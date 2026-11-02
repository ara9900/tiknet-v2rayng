package com.v2ray.ang.tiknet

import android.content.Context
import android.os.Build
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.AppConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Reports real-ping / urltest results to the panel (`link_quality`) so admins can
 * see which configs work on which Iranian operators — no raw IP stored server-side.
 */
object TikNetTelemetry {
    private val io = Executors.newSingleThreadExecutor { r ->
        Thread(r, "tiknet-telemetry").apply { isDaemon = true }
    }
    private val lastRefreshMs = AtomicLong(0)
    private val lastReportMs = AtomicLong(0)

    @Volatile
    private var tagByHostPort: Map<String, TagInfo> = emptyMap()

    data class TagInfo(val tag: String, val kind: String)

    fun refreshTagMapAsync(ctx: Context) {
        val app = ctx.applicationContext
        io.execute {
            runCatching { refreshTagMap(app) }
                .onFailure { LogUtil.e(AppConfig.TAG, "TikNetTelemetry refresh tags failed", it) }
        }
    }

    fun refreshTagMap(ctx: Context) {
        if (!TikNetPrefs.isLoggedIn(ctx)) return
        val base = TikNetPrefs.getBaseUrl(ctx) ?: return
        val token = TikNetPrefs.getAccessToken(ctx) ?: return
        val lines = TikNetApi.getSubscriptionLines(base, token)
        val map = LinkedHashMap<String, TagInfo>()
        for (line in lines) {
            val host = line.host?.lowercase()?.trim().orEmpty()
            val port = line.port
            val tag = line.tag.trim()
            if (tag.isEmpty()) continue
            if (host.isNotEmpty() && port != null && port > 0) {
                map["$host:$port"] = TagInfo(tag, line.kind)
            }
            // Also index by dial host parsed from URI when present
            val fromUri = parseHostPort(line.uri)
            if (fromUri != null) {
                map["${fromUri.first}:${fromUri.second}"] = TagInfo(tag, line.kind)
            }
        }
        tagByHostPort = map
        lastRefreshMs.set(System.currentTimeMillis())
        LogUtil.i(AppConfig.TAG, "TikNetTelemetry: cached ${map.size} tagged endpoints")
    }

    /** Fire-and-forget after a real-ping / TCP test result. */
    fun reportDelayAsync(ctx: Context, guid: String, delayMillis: Long) {
        val app = ctx.applicationContext
        io.execute {
            runCatching { reportDelay(app, guid, delayMillis) }
                .onFailure { LogUtil.e(AppConfig.TAG, "TikNetTelemetry report failed", it) }
        }
    }

    private fun reportDelay(ctx: Context, guid: String, delayMillis: Long) {
        if (!TikNetPrefs.isLoggedIn(ctx)) return
        // Soft rate-limit bursts from "test all"
        val now = System.currentTimeMillis()
        if (now - lastReportMs.get() < 40) {
            Thread.sleep(40)
        }
        lastReportMs.set(System.currentTimeMillis())

        if (tagByHostPort.isEmpty() || now - lastRefreshMs.get() > 30 * 60_000L) {
            runCatching { refreshTagMap(ctx) }
        }

        val profile = MmkvManager.decodeServerConfig(guid) ?: return
        // Only personal TikNet sub + catalog (skip manual unrelated profiles)
        val subId = profile.subscriptionId
        if (subId.isNotBlank() &&
            subId != TikNetPrefs.TIKNET_SUB_GUID &&
            !subId.startsWith("tiknet-cat-")
        ) {
            return
        }

        val host = profile.server?.trim().orEmpty()
        val port = profile.serverPort?.toIntOrNull()
        if (host.isEmpty() && port == null) return

        val key = if (port != null) "${host.lowercase()}:$port" else host.lowercase()
        val info = tagByHostPort[key]
        val ok = delayMillis >= 0
        val latency = if (ok) delayMillis.toInt().coerceAtMost(120_000) else null

        val payload = mutableMapOf<String, Any?>(
            "ok" to ok,
            "protocol" to profile.configType.name.lowercase(),
            "remark" to profile.remarks.take(120),
        )
        if (!info?.tag.isNullOrBlank()) {
            payload["tag"] = info!!.tag
            if (info.kind.isNotBlank()) payload["kind"] = info.kind
        } else if (host.isNotEmpty()) {
            payload["host"] = host.lowercase()
            if (port != null) payload["port"] = port
            payload["kind"] = if (profile.remarks.contains("تونل")) "tunnel" else "direct"
        } else {
            return
        }
        if (latency != null) payload["latency_ms"] = latency
        if (!ok) payload["error"] = "ping_failed"

        val base = TikNetPrefs.getBaseUrl(ctx) ?: return
        val clean = payload.filterValues { it != null }.mapValues { it.value as Any }
        TikNetApi.postTelemetry(
            baseUrl = base,
            event = "link_quality",
            appVersion = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            platform = "android",
            deviceModel = Build.MODEL ?: "",
            androidSdk = Build.VERSION.SDK_INT,
            payload = clean,
        )
    }

    private fun parseHostPort(uri: String): Pair<String, Int>? {
        // vless://uuid@host:port?...  or ss://...@host:port#...
        val at = uri.indexOf('@')
        if (at < 0) return null
        var rest = uri.substring(at + 1)
        val cut = rest.indexOfAny(charArrayOf('/', '?', '#'))
        if (cut >= 0) rest = rest.substring(0, cut)
        // [ipv6]:port
        if (rest.startsWith("[")) {
            val end = rest.indexOf(']')
            if (end < 0) return null
            val host = rest.substring(1, end).lowercase()
            val port = rest.substring(end + 1).removePrefix(":").toIntOrNull() ?: return null
            return host to port
        }
        val colon = rest.lastIndexOf(':')
        if (colon <= 0) return null
        val host = rest.substring(0, colon).lowercase()
        val port = rest.substring(colon + 1).toIntOrNull() ?: return null
        return host to port
    }
}
