package com.v2ray.ang.tiknet

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.util.LogUtil
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

enum class TikNetDiagStatus { Ok, Fail, Warn, Info }

/**
 * @param autoFix battery | restart_vpn | sync | ensure_assets | open_settings
 */
data class TikNetDiagItem(
    val id: String,
    val title: String,
    val detail: String,
    val status: TikNetDiagStatus,
    val settingsAction: String? = null,
    val autoFix: String? = null,
) {
    val ok: Boolean get() = status == TikNetDiagStatus.Ok || status == TikNetDiagStatus.Info
}

data class TikNetAutoFixResult(
    val messages: List<String>,
    val restartedVpn: Boolean = false,
    val requestedBattery: Boolean = false,
)

object TikNetDiagnostics {
    private const val PROBE_HOST = "www.gstatic.com"
    private const val PROBE_URL = "https://www.gstatic.com/generate_204"

    fun collect(ctx: Context, tikNetVpnActive: Boolean): List<TikNetDiagItem> {
        val list = mutableListOf<TikNetDiagItem>()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val caps = net?.let { cm.getNetworkCapabilities(it) }
        val link = net?.let { cm.getLinkProperties(it) }

        val airplane = Settings.Global.getInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        list += TikNetDiagItem(
            id = "airplane",
            title = "حالت هواپیما",
            detail = if (airplane) "حالت هواپیما روشن است و اینترنت قطع می‌شود." else "حالت هواپیما خاموش است.",
            status = if (airplane) TikNetDiagStatus.Fail else TikNetDiagStatus.Ok,
            settingsAction = "airplane",
        )

        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val cell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val eth = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val vpnTransport = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val transports = buildList {
            if (wifi) add("وای‌فای")
            if (cell) add("دیتای موبایل")
            if (eth) add("اترنت")
            if (vpnTransport) add("VPN")
        }
        list += TikNetDiagItem(
            id = "network",
            title = "اتصال شبکه",
            detail = when {
                !hasInternet -> "سیستم شبکه فعالی با قابلیت اینترنت گزارش نکرد."
                validated -> "شبکه فعال است (${transports.ifEmpty { listOf("نامشخص") }.joinToString(" + ")})."
                else -> "شبکه هست ولی هنوز validate نشده (احتمال captive portal یا قطع موقت)."
            },
            status = when {
                !hasInternet -> TikNetDiagStatus.Fail
                validated -> TikNetDiagStatus.Ok
                else -> TikNetDiagStatus.Warn
            },
            settingsAction = "wireless",
            autoFix = if (!hasInternet || !validated) "restart_vpn" else null,
        )

        val dnsServers = link?.dnsServers?.mapNotNull { it.hostAddress }?.filter { it.isNotBlank() }.orEmpty()
        if (dnsServers.isNotEmpty()) {
            list += TikNetDiagItem(
                id = "dns_servers",
                title = "سرورهای DNS فعلی",
                detail = dnsServers.joinToString(", "),
                status = TikNetDiagStatus.Info,
                settingsAction = "private_dns",
            )
        }

        val autoTime = Settings.Global.getInt(ctx.contentResolver, Settings.Global.AUTO_TIME, 0) == 1
        val autoTz = Settings.Global.getInt(ctx.contentResolver, Settings.Global.AUTO_TIME_ZONE, 0) == 1
        list += TikNetDiagItem(
            id = "auto_time",
            title = "ساعت خودکار",
            detail = when {
                autoTime && autoTz -> "ساعت و منطقه زمانی خودکار روشن است."
                autoTime -> "ساعت خودکار روشن است (منطقه زمانی دستی)."
                else -> "ساعت خودکار خاموش است؛ اختلاف ساعت باعث خطای SSL می‌شود."
            },
            status = if (autoTime) TikNetDiagStatus.Ok else TikNetDiagStatus.Fail,
            settingsAction = "date",
        )

        val privateDnsMode = runCatching {
            Settings.Global.getString(ctx.contentResolver, "private_dns_mode")
        }.getOrNull().orEmpty().ifBlank { "off" }
        val privateDnsHost = runCatching {
            Settings.Global.getString(ctx.contentResolver, "private_dns_specifier")
        }.getOrNull().orEmpty()
        val modeLower = privateDnsMode.lowercase(Locale.US)
        val privateDnsOff = modeLower == "off" || modeLower == "unknown"
        val privateDnsOk = privateDnsOff || modeLower == "opportunistic"
        list += TikNetDiagItem(
            id = "private_dns",
            title = "DNS خصوصی",
            detail = when {
                privateDnsOff -> "DNS خصوصی خاموش / خودکار است."
                modeLower == "hostname" || modeLower == "provider" ->
                    "DNS خصوصی روی «${privateDnsHost.ifBlank { modeLower }}» تنظیم شده؛ اگر فیلتر/مسدود باشد اینترنت مختل می‌شود."
                else -> "حالت DNS: $privateDnsMode${if (privateDnsHost.isNotBlank()) " ($privateDnsHost)" else ""}"
            },
            status = if (privateDnsOk) TikNetDiagStatus.Ok else TikNetDiagStatus.Warn,
            settingsAction = "private_dns",
        )

        val coreRunning = runCatching { CoreServiceManager.isRunning() }.getOrDefault(false)
        when {
            vpnTransport && (tikNetVpnActive || coreRunning) -> list += TikNetDiagItem(
                id = "vpn_active",
                title = "VPN فعال",
                detail = "VPN فعال مربوط به خود TikNet است — طبیعی است.",
                status = TikNetDiagStatus.Ok,
                settingsAction = "vpn",
            )
            vpnTransport -> list += TikNetDiagItem(
                id = "vpn_active",
                title = "VPN فعال",
                detail = "یک VPN دیگر روی گوشی فعال است. اگر «همیشه روشن» باشد ممکن است TikNet نتواند وصل شود.",
                status = TikNetDiagStatus.Warn,
                settingsAction = "vpn",
            )
            else -> list += TikNetDiagItem(
                id = "vpn_active",
                title = "VPN فعال",
                detail = "VPN دیگری روی شبکهٔ فعلی دیده نشد.",
                status = TikNetDiagStatus.Ok,
                settingsAction = "vpn",
            )
        }

        list += alwaysOnVpnItem(ctx)
        list += dnsLookupItem()
        list += httpAndClockItem(cell)
        list += batteryItem(ctx)

        list += TikNetDiagItem(
            id = "apn",
            title = "پروکسی APN سیم‌کارت",
            detail = "اپ نمی‌تواند فیلد پروکسی APN را بخواند. در تنظیمات APN مطمئن شوید فیلد Proxy خالی است؛ پر بودن آن اینترنت را قطع می‌کند.",
            status = TikNetDiagStatus.Info,
            settingsAction = "apn",
        )

        list += TikNetDiagItem(
            id = "core",
            title = "هسته TikNet",
            detail = if (coreRunning) "هسته VPN در حال اجرا است." else "هسته VPN الان متوقف است.",
            status = if (coreRunning) TikNetDiagStatus.Ok else TikNetDiagStatus.Info,
            autoFix = if (!coreRunning) "restart_vpn" else null,
            settingsAction = "vpn",
        )

        return list
    }

    /**
     * Safe auto-remediation. Returns human-readable actions taken.
     * Caller should re-run [collect] afterwards.
     */
    @SuppressLint("BatteryLife")
    fun autoFixAll(
        ctx: Context,
        items: List<TikNetDiagItem>,
        onRestartVpn: () -> Unit,
        onSync: () -> Unit,
    ): TikNetAutoFixResult {
        val messages = mutableListOf<String>()
        var restarted = false
        var battery = false

        val needs = items.filter {
            it.status == TikNetDiagStatus.Fail || it.status == TikNetDiagStatus.Warn
        }.mapNotNull { it.autoFix }.toMutableSet()

        // Also pick fixes from known ids even if autoFix null
        items.forEach { item ->
            when {
                item.id == "battery" && item.status == TikNetDiagStatus.Warn -> needs += "battery"
                item.id == "http" && item.status != TikNetDiagStatus.Ok -> needs += "restart_vpn"
                item.id == "dns" && item.status == TikNetDiagStatus.Fail -> needs += "restart_vpn"
                item.id == "network" && item.status != TikNetDiagStatus.Ok -> needs += "restart_vpn"
            }
        }

        if ("battery" in needs) {
            if (requestIgnoreBatteryOptimizations(ctx)) {
                battery = true
                messages += "درخواست رفع محدودیت باتری ارسال شد"
            } else {
                openSettings(ctx, "battery")
                messages += "صفحه تنظیمات باتری باز شد"
            }
        }

        runCatching {
            TikNetBootstrap.applyDefaults(ctx)
            // Force a freshness check (skip if recent — refreshGeoAssets already TTL-gated)
            TikNetBootstrap.refreshGeoAssets(ctx)
            messages += "مسیریابی ایران / فایل‌های geo بررسی شد"
            needs -= "ensure_assets"
        }.onFailure {
            LogUtil.w(AppConfig.TAG, "diag ensure_assets failed: ${it.message}")
        }

        if ("sync" in needs || items.any { it.id == "http" && it.status == TikNetDiagStatus.Fail }) {
            runCatching {
                onSync()
                messages += "همگام‌سازی اشتراک شروع شد"
            }
        }

        if ("restart_vpn" in needs) {
            runCatching {
                onRestartVpn()
                restarted = true
                messages += "راه‌اندازی مجدد اتصال VPN"
            }
        }

        // Open settings for issues we cannot change programmatically
        val openOnce = linkedSetOf<String>()
        items.forEach { item ->
            if (item.status == TikNetDiagStatus.Fail || item.status == TikNetDiagStatus.Warn) {
                when (item.id) {
                    "airplane" -> openOnce += "airplane"
                    "auto_time" -> openOnce += "date"
                    "private_dns" -> if (item.status == TikNetDiagStatus.Warn) openOnce += "private_dns"
                    "always_on" -> if (item.status == TikNetDiagStatus.Warn) openOnce += "vpn"
                }
            }
        }
        // Prefer one settings screen: airplane > date > private_dns > vpn
        val first = listOf("airplane", "date", "private_dns", "vpn").firstOrNull { it in openOnce }
        if (first != null && messages.none { it.contains("باتری") }) {
            openSettings(ctx, first)
            messages += "تنظیمات مرتبط ($first) باز شد — لطفاً دستی اصلاح کنید"
        }

        if (messages.isEmpty()) {
            messages += "مورد قابل رفع خودکار پیدا نشد"
        }
        return TikNetAutoFixResult(messages = messages, restartedVpn = restarted, requestedBattery = battery)
    }

    fun autoFixOne(ctx: Context, item: TikNetDiagItem, onRestartVpn: () -> Unit, onSync: () -> Unit): String {
        val key = item.autoFix ?: item.id
        return when (key) {
            "battery" -> {
                if (requestIgnoreBatteryOptimizations(ctx)) "درخواست رفع محدودیت باتری ارسال شد"
                else {
                    openSettings(ctx, "battery")
                    "تنظیمات باتری باز شد"
                }
            }
            "restart_vpn", "core", "http", "network", "dns" -> {
                onRestartVpn()
                "اتصال VPN راه‌اندازی مجدد شد"
            }
            "sync" -> {
                onSync()
                "همگام‌سازی اشتراک شروع شد"
            }
            "ensure_assets" -> {
                TikNetBootstrap.applyDefaults(ctx)
                TikNetBootstrap.refreshGeoAssets(ctx)
                "فایل‌های مسیریابی بررسی شد"
            }
            else -> {
                openSettings(ctx, item.settingsAction)
                "تنظیمات مربوطه باز شد"
            }
        }
    }

    fun openSettings(ctx: Context, target: String?) {
        val intent = when (target) {
            "date" -> Intent(Settings.ACTION_DATE_SETTINGS)
            "vpn" -> Intent("android.net.vpn.SETTINGS").takeIf {
                it.resolveActivity(ctx.packageManager) != null
            } ?: Intent(Settings.ACTION_SETTINGS)
            "apn" -> Intent(Settings.ACTION_APN_SETTINGS)
            "private_dns", "wireless" -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
            "battery" -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).takeIf {
                it.resolveActivity(ctx.packageManager) != null
            } ?: Intent(Settings.ACTION_SETTINGS)
            "airplane" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).takeIf {
                it.resolveActivity(ctx.packageManager) != null
            } ?: Intent(Settings.ACTION_WIRELESS_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(intent) }
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        if (pm.isIgnoringBatteryOptimizations(ctx.packageName)) return true
        return try {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${ctx.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "battery opt request failed: ${e.message}")
            false
        }
    }

    private fun alwaysOnVpnItem(ctx: Context): TikNetDiagItem {
        val app = runCatching {
            Settings.Secure.getString(ctx.contentResolver, "always_on_vpn_app")
        }.getOrNull().orEmpty()
        val lockdown = runCatching {
            Settings.Secure.getInt(ctx.contentResolver, "always_on_vpn_lockdown", 0) == 1
        }.getOrDefault(false)

        if (app.isNotBlank()) {
            val ours = app == ctx.packageName || app.endsWith(".tik.net")
            if (ours) {
                return TikNetDiagItem(
                    id = "always_on",
                    title = "VPN همیشه روشن",
                    detail = "Always-on مربوط به TikNet است ($app) — اگر عمداً روشن کرده‌اید مشکلی نیست.",
                    status = TikNetDiagStatus.Info,
                    settingsAction = "vpn",
                )
            }
            return TikNetDiagItem(
                id = "always_on",
                title = "VPN همیشه روشن",
                detail = if (lockdown) {
                    "Always-on VPN با قفل فعال است ($app) و ممکن است مانع اتصال TikNet شود."
                } else {
                    "Always-on VPN روی $app تنظیم شده؛ اگر مال اپ دیگری است آن را خاموش کنید."
                },
                status = TikNetDiagStatus.Warn,
                settingsAction = "vpn",
            )
        }
        return TikNetDiagItem(
            id = "always_on",
            title = "VPN همیشه روشن",
            detail = "اگر اپ دیگری Always-on VPN دارد، TikNet نمی‌تواند وصل شود. در تنظیمات VPN گوشی بررسی کنید.",
            status = TikNetDiagStatus.Info,
            settingsAction = "vpn",
        )
    }

    private fun dnsLookupItem(): TikNetDiagItem {
        return try {
            val addrs = InetAddress.getAllByName(PROBE_HOST)
            if (addrs.isNullOrEmpty()) {
                TikNetDiagItem(
                    id = "dns",
                    title = "DNS / رزولوشن دامنه",
                    detail = "نام دامنه resolve نشد.",
                    status = TikNetDiagStatus.Fail,
                    settingsAction = "private_dns",
                    autoFix = "restart_vpn",
                )
            } else {
                TikNetDiagItem(
                    id = "dns",
                    title = "DNS / رزولوشن دامنه",
                    detail = "دامنه $PROBE_HOST با موفقیت resolve شد (${addrs.size} آدرس).",
                    status = TikNetDiagStatus.Ok,
                )
            }
        } catch (e: Exception) {
            TikNetDiagItem(
                id = "dns",
                title = "DNS / رزولوشن دامنه",
                detail = "خطا در resolve دامنه: ${e.message ?: e.javaClass.simpleName}",
                status = TikNetDiagStatus.Fail,
                settingsAction = "private_dns",
                autoFix = "restart_vpn",
            )
        }
    }

    private fun httpAndClockItem(isCellular: Boolean): TikNetDiagItem {
        return try {
            val started = System.currentTimeMillis()
            val c = (URL(PROBE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = false
                requestMethod = "GET"
            }
            c.connect()
            val code = c.responseCode
            val dateHeader = c.getHeaderField("Date")
            c.disconnect()
            val elapsed = System.currentTimeMillis() - started

            var clockDetail = ""
            if (!dateHeader.isNullOrBlank()) {
                val serverMs = parseHttpDate(dateHeader)
                if (serverMs != null) {
                    val skewSec = TimeUnit.MILLISECONDS.toSeconds(
                        kotlin.math.abs(System.currentTimeMillis() - serverMs),
                    )
                    if (skewSec > 120) {
                        return TikNetDiagItem(
                            id = "http",
                            title = "دسترسی اینترنت و ساعت",
                            detail = "اینترنت پاسخ داد (${elapsed}ms) ولی اختلاف ساعت حدود ${skewSec}s است — SSL ممکن است خراب شود.",
                            status = TikNetDiagStatus.Fail,
                            settingsAction = "date",
                        )
                    }
                    clockDetail = " · اختلاف ساعت OK"
                }
            }

            val simHint = if (isCellular) " (شبکه موبایل)" else ""
            when {
                code == 204 || code in 200..399 -> TikNetDiagItem(
                    id = "http",
                    title = "دسترسی اینترنت",
                    detail = "پینگ HTTP به دامنهٔ تست موفق بود (${elapsed}ms)$simHint$clockDetail.",
                    status = TikNetDiagStatus.Ok,
                )
                else -> TikNetDiagItem(
                    id = "http",
                    title = "دسترسی اینترنت",
                    detail = "پاسخ غیرمنتظره از دامنهٔ تست (کد $code).",
                    status = TikNetDiagStatus.Warn,
                    settingsAction = "wireless",
                    autoFix = "restart_vpn",
                )
            }
        } catch (e: Exception) {
            TikNetDiagItem(
                id = "http",
                title = "دسترسی اینترنت",
                detail = "نتوانستیم به دامنهٔ تست وصل شویم. دیتای سیم‌کارت/وای‌فای را بررسی کنید. (${e.message ?: e.javaClass.simpleName})",
                status = TikNetDiagStatus.Fail,
                settingsAction = "wireless",
                autoFix = "restart_vpn",
            )
        }
    }

    private fun batteryItem(ctx: Context): TikNetDiagItem {
        val ignoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(ctx.packageName) == true
        } else {
            true
        }
        return TikNetDiagItem(
            id = "battery",
            title = "بهینه‌سازی باتری",
            detail = if (ignoring) {
                "محدودیت بهینه‌سازی باتری برای TikNet برداشته شده (یا لازم نیست)."
            } else {
                "بهینه‌سازی باتری ممکن است سرویس VPN را در پس‌زمینه قطع کند."
            },
            status = if (ignoring) TikNetDiagStatus.Ok else TikNetDiagStatus.Warn,
            settingsAction = "battery",
            autoFix = if (ignoring) null else "battery",
        )
    }

    private fun parseHttpDate(raw: String): Long? {
        return runCatching {
            val fmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("GMT")
            fmt.parse(raw)?.time
        }.getOrNull()
    }
}
