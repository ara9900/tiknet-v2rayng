package com.v2ray.ang.tiknet

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class TikNetApiException(message: String, val statusCode: Int? = null) : Exception(message)

data class TikNetLoginResponse(
    @SerializedName("access_token") val accessToken: String = "",
    @SerializedName("expires_in") val expiresIn: Int = 0,
    @SerializedName("subscription_url") val subscriptionUrl: String? = null,
)

data class TikNetUserInfo(
    val username: String = "",
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("expire_date") val expireDate: String? = null,
    @SerializedName("has_subscription") val hasSubscription: Boolean = false,
    @SerializedName("subscription_url") val subscriptionUrl: String? = null,
    @SerializedName("plan_name") val planName: String? = null,
    @SerializedName("is_expired") val isExpired: Boolean? = null,
    @SerializedName("days_remaining") val daysRemaining: Int? = null,
    @SerializedName("traffic_used_bytes") val trafficUsedBytes: Long? = null,
    @SerializedName("traffic_limit_bytes") val trafficLimitBytes: Long? = null,
    @SerializedName("shop_enabled") val shopEnabled: Boolean? = null,
    @SerializedName("support_telegram") val supportTelegram: String? = null,
)

data class TikNetUsagePoint(
    val t: String? = null,
    @SerializedName("used_gb") val usedGb: Double = 0.0,
    @SerializedName("limit_gb") val limitGb: Double = 0.0,
)

data class TikNetUsageHistory(
    @SerializedName("order_id") val orderId: Int = 0,
    @SerializedName("used_gb") val usedGb: Double = 0.0,
    @SerializedName("limit_gb") val limitGb: Double = 0.0,
    val points: List<TikNetUsagePoint> = emptyList(),
)

data class TikNetSession(
    val id: Int = 0,
    @SerializedName("ip_address") val ipAddress: String? = null,
    @SerializedName("user_agent") val userAgent: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("is_current") val isCurrent: Boolean = false,
)

data class TikNetPublicConfig(
    val shopEnabled: Boolean = false,
    val shopUrl: String? = null,
    val shopLabel: String? = null,
) {
    /** Same rules as Flutter [TikNetPublicConfig.showTelegramShop]. */
    val showShop: Boolean
        get() {
            if (!shopEnabled) return false
            val url = shopUrl?.trim().orEmpty()
            if (url.isEmpty()) return false
            val scheme = url.substringBefore(':', missingDelimiterValue = "")
                .lowercase()
                .trim()
            return scheme == "http" || scheme == "https" || scheme == "tg"
        }
}

data class TikNetAppUpdateInfo(
    val enabled: Boolean = false,
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val force: Boolean = false,
    val changelog: String = "",
    val sha256: String = "",
)

data class TikNetCatalogServer(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("country_code") val countryCode: String? = null,
    val tier: String? = null,
)

data class TikNetAnnouncement(
    val show: Boolean = false,
    val type: String = "info",
    val text: String = "",
)

data class TikNetNotificationItem(
    val id: Int = 0,
    val title: String = "",
    val body: String = "",
    val type: String = "info",
    val read: Boolean = true,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class TikNetFaqItem(
    val id: Int = 0,
    val category: String? = null,
    val question: String = "",
    val answer: String = "",
)

/**
 * Panel HTTP client — API shapes mirrored from Flutter TikNet.
 */
object TikNetApi {
    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "TikNet/${com.v2ray.ang.BuildConfig.VERSION_NAME} Android")
                .build()
            chain.proceed(req)
        }
        .build()

    private fun root(baseUrl: String): String = baseUrl.trim().trimEnd('/')

    fun login(baseUrl: String, username: String, password: String): TikNetLoginResponse {
        val body = gson.toJson(mapOf("username" to username.trim(), "password" to password))
            .toRequestBody(jsonMedia)
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/login")
            .post(body)
            .header("Accept", "application/json")
            .build()
        return executeJson(req, TikNetLoginResponse::class.java)
    }

    /** POST /api/customer/login/token — one-time deep-link / QR login from Telegram bot. */
    fun loginWithToken(baseUrl: String, token: String): TikNetLoginResponse {
        val body = gson.toJson(mapOf("token" to token.trim()))
            .toRequestBody(jsonMedia)
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/login/token")
            .post(body)
            .header("Accept", "application/json")
            .build()
        return executeJson(req, TikNetLoginResponse::class.java)
    }

    fun getMe(baseUrl: String, token: String): TikNetUserInfo {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/me")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        return executeJson(req, TikNetUserInfo::class.java)
    }

    /**
     * GET /me with tolerant JSON parsing (avoids Gson/Kotlin data-class edge cases)
     * and nested brand.support_telegram.
     */
    fun enrichMe(baseUrl: String, token: String): Pair<TikNetUserInfo, String?> {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/me")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val detail = runCatching {
                    JsonParser.parseString(text).asJsonObject.get("detail")?.asString
                }.getOrNull()
                throw TikNetApiException(detail ?: "HTTP ${resp.code}", resp.code)
            }
            if (text.isBlank()) throw TikNetApiException("Empty response")
            val rootEl = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
                ?: throw TikNetApiException("Invalid JSON")
            val me = parseUserInfo(rootEl)
            fun jsonString(el: com.google.gson.JsonElement?): String? {
                if (el == null || el.isJsonNull || !el.isJsonPrimitive) return null
                return el.asString?.trim()?.takeIf { it.isNotEmpty() }
            }
            val brandTg = jsonString(rootEl.getAsJsonObject("brand")?.get("support_telegram"))
            val topTg = jsonString(rootEl.get("support_telegram"))
            val supportTg = brandTg ?: topTg ?: me.supportTelegram
            val shopEl = rootEl.get("shop_enabled")
            val shopEnabled = if (shopEl != null && !shopEl.isJsonNull && shopEl.isJsonPrimitive && shopEl.asJsonPrimitive.isBoolean) {
                shopEl.asBoolean
            } else {
                me.shopEnabled
            }
            val enriched = me.copy(
                supportTelegram = supportTg,
                shopEnabled = shopEnabled,
            )
            return enriched to supportTg
        }
    }

    /** Tolerant /me parser — numbers may be int/double; booleans may be missing. */
    fun parseUserInfo(rootEl: com.google.gson.JsonObject): TikNetUserInfo {
        fun str(vararg keys: String): String? {
            for (k in keys) {
                val v = rootEl.get(k) ?: continue
                if (v.isJsonNull || !v.isJsonPrimitive) continue
                val s = v.asString?.trim()
                if (!s.isNullOrEmpty()) return s
            }
            return null
        }
        fun bool(vararg keys: String): Boolean? {
            for (k in keys) {
                val v = rootEl.get(k) ?: continue
                if (v.isJsonNull || !v.isJsonPrimitive) continue
                val p = v.asJsonPrimitive
                if (p.isBoolean) return p.asBoolean
            }
            return null
        }
        fun longOrNull(vararg keys: String): Long? {
            for (k in keys) {
                val v = rootEl.get(k) ?: continue
                if (v.isJsonNull || !v.isJsonPrimitive) continue
                val p = v.asJsonPrimitive
                if (p.isNumber) return p.asLong
            }
            return null
        }
        fun intOrNull(vararg keys: String): Int? = longOrNull(*keys)?.toInt()

        return TikNetUserInfo(
            username = str("username") ?: "",
            fullName = str("full_name", "fullName"),
            expireDate = str("expire_date", "expireDate"),
            hasSubscription = bool("has_subscription", "hasSubscription") ?: false,
            subscriptionUrl = str("subscription_url", "subscriptionUrl"),
            planName = str("plan_name", "planName"),
            isExpired = bool("is_expired", "isExpired"),
            daysRemaining = intOrNull("days_remaining", "daysRemaining"),
            trafficUsedBytes = longOrNull("traffic_used_bytes", "trafficUsedBytes"),
            trafficLimitBytes = longOrNull("traffic_limit_bytes", "trafficLimitBytes"),
            shopEnabled = bool("shop_enabled", "shopEnabled"),
            supportTelegram = str("support_telegram", "supportTelegram"),
        )
    }

    /** GET /api/customer/public-config — no auth (shop flags / buy-renew URL). */
    fun getPublicConfig(baseUrl: String): TikNetPublicConfig {
        return try {
            val req = Request.Builder()
                .url("${root(baseUrl)}/api/customer/public-config")
                .get()
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return TikNetPublicConfig()
                val text = resp.body?.string().orEmpty()
                parsePublicConfigJson(text)
            }
        } catch (_: Exception) {
            TikNetPublicConfig()
        }
    }

    /**
     * Parse public-config JSON.
     * For Telegram shop links (`t.me` / `tg:`), [telegram_shop_enabled] is authoritative:
     * if that flag is missing/false, the buy button stays hidden even if a URL is present.
     */
    fun parsePublicConfigJson(text: String): TikNetPublicConfig {
        if (text.isBlank()) return TikNetPublicConfig()
        val rootEl = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
            ?: return TikNetPublicConfig()
        val shop = rootEl.getAsJsonObject("shop")
            ?: rootEl.getAsJsonObject("buy_renew")
            ?: rootEl.getAsJsonObject("app_shop")

        fun readLooseBool(el: com.google.gson.JsonElement?): Boolean? {
            if (el == null || el.isJsonNull || !el.isJsonPrimitive) return null
            val p = el.asJsonPrimitive
            if (p.isBoolean) return p.asBoolean
            if (p.isNumber) return p.asInt != 0
            if (p.isString) {
                return when (p.asString.trim().lowercase()) {
                    "1", "true", "yes", "on" -> true
                    "0", "false", "no", "off" -> false
                    else -> null
                }
            }
            return null
        }

        fun pickBool(vararg keys: String): Boolean? {
            for (k in keys) {
                val v = readLooseBool(rootEl.get(k) ?: shop?.get(k))
                if (v != null) return v
            }
            return null
        }

        fun pickString(vararg keys: String): String? {
            for (k in keys) {
                val el = rootEl.get(k) ?: shop?.get(k) ?: continue
                if (el.isJsonNull || !el.isJsonPrimitive) continue
                val s = el.asString?.trim()
                if (!s.isNullOrEmpty()) return s
            }
            return null
        }

        val url = pickString(
            "telegram_shop_url",
            "shop_url",
            "app_shop_url",
            "buy_url",
            "buy_renew_url",
            "renew_url",
            "telegram_bot_url",
        ) ?: pickString("url", "link")

        val label = pickString(
            "telegram_shop_label",
            "shop_label",
            "buy_label",
            "buy_renew_label",
        ) ?: pickString("label", "title")

        val telegramFlag = pickBool("telegram_shop_enabled")
        val looksLikeTelegram = !url.isNullOrBlank() && (
            url.contains("t.me/", ignoreCase = true) ||
                url.contains("telegram.me/", ignoreCase = true) ||
                url.startsWith("tg:", ignoreCase = true)
            )

        // Any explicit false among known flags wins (panel off must hide the button).
        val explicitOff = listOf(
            "telegram_shop_enabled",
            "shop_enabled",
            "app_shop_enabled",
            "buy_enabled",
            "buy_renew_enabled",
            "show_shop_button",
            "show_buy_button",
        ).any { pickBool(it) == false } || readLooseBool(shop?.get("enabled")) == false

        val enabled = when {
            explicitOff -> false
            looksLikeTelegram -> telegramFlag == true
            else -> pickBool(
                "shop_enabled",
                "app_shop_enabled",
                "buy_enabled",
                "buy_renew_enabled",
                "show_shop_button",
                "show_buy_button",
            ) ?: readLooseBool(shop?.get("enabled")) ?: false
        }

        return TikNetPublicConfig(
            shopEnabled = enabled,
            shopUrl = url,
            shopLabel = label,
        )
    }

    fun getSubscriptionConfigBytes(baseUrl: String, token: String): ByteArray {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/subscription/config")
            .get()
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw TikNetApiException("subscription config HTTP ${resp.code}", resp.code)
            }
            return resp.body?.bytes() ?: ByteArray(0)
        }
    }

    fun getServerCatalog(baseUrl: String, token: String): List<TikNetCatalogServer> {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/servers")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw TikNetApiException("servers HTTP ${resp.code}", resp.code)
            }
            val text = resp.body?.string().orEmpty()
            val rootEl = JsonParser.parseString(text).asJsonObject
            val arr = rootEl.getAsJsonArray("servers") ?: return emptyList()
            return arr.mapNotNull {
                runCatching { gson.fromJson(it, TikNetCatalogServer::class.java) }.getOrNull()
            }.filter { it.id > 0 }
        }
    }

    fun getServerConfigBytes(baseUrl: String, token: String, serverId: Int): ByteArray {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/servers/$serverId/config")
            .get()
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw TikNetApiException("server config HTTP ${resp.code}", resp.code)
            }
            return resp.body?.bytes() ?: ByteArray(0)
        }
    }

    fun getAppUpdate(baseUrl: String): TikNetAppUpdateInfo {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/app-update")
            .get()
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return TikNetAppUpdateInfo()
            val text = resp.body?.string().orEmpty()
            val rootEl = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
                ?: return TikNetAppUpdateInfo()
            val update = rootEl.getAsJsonObject("update") ?: return TikNetAppUpdateInfo()
            val enabled = update.get("enabled")?.asBoolean ?: false
            if (!enabled) return TikNetAppUpdateInfo()
            val versionCode = update.get("version_code")?.asInt ?: 0
            val apkUrl = update.get("apk_url")?.asString?.trim().orEmpty()
            if (versionCode <= 0 || apkUrl.isEmpty()) return TikNetAppUpdateInfo()
            return TikNetAppUpdateInfo(
                enabled = true,
                versionCode = versionCode,
                versionName = update.get("version_name")?.asString?.trim().orEmpty(),
                apkUrl = apkUrl,
                force = update.get("force")?.asBoolean ?: false,
                changelog = update.get("changelog")?.asString?.trim().orEmpty(),
                sha256 = update.get("sha256")?.asString?.trim()?.lowercase().orEmpty(),
            )
        }
    }

    fun getAnnouncement(baseUrl: String?, token: String?): TikNetAnnouncement {
        // Panel first
        if (!baseUrl.isNullOrBlank()) {
            runCatching {
                val b = Request.Builder()
                    .url("${root(baseUrl)}/api/customer/announcement")
                    .get()
                    .header("Accept", "application/json")
                if (!token.isNullOrBlank()) b.header("Authorization", "Bearer $token")
                client.newCall(b.build()).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val text = resp.body?.string().orEmpty()
                        parseAnnouncement(text)?.let { return it }
                    }
                }
            }
        }
        // GitHub fallback
        runCatching {
            val req = Request.Builder()
                .url("https://ara9900.github.io/app-config/config.json")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val text = resp.body?.string().orEmpty()
                parseAnnouncement(text)?.let { return it }
            }
        }
        return TikNetAnnouncement(
            show = true,
            type = "success",
            text = "تیکنت، مسیری مطمئن به دنیای اینترنت",
        )
    }

    private fun parseAnnouncement(text: String): TikNetAnnouncement? {
        val rootEl = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull() ?: return null
        val msg = rootEl.getAsJsonObject("message") ?: return null
        val show = msg.get("show")?.asBoolean ?: false
        val body = msg.get("text")?.asString?.trim().orEmpty()
        if (!show || body.isEmpty()) return TikNetAnnouncement(show = false)
        return TikNetAnnouncement(
            show = true,
            type = msg.get("type")?.asString?.trim().orEmpty().ifBlank { "info" },
            text = body,
        )
    }

    fun getNotifications(baseUrl: String, token: String): Pair<List<TikNetNotificationItem>, Int> {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/notifications")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw TikNetApiException("notifications HTTP ${resp.code}", resp.code)
            val text = resp.body?.string().orEmpty()
            val rootEl = JsonParser.parseString(text).asJsonObject
            val arr = rootEl.getAsJsonArray("notifications") ?: return emptyList<TikNetNotificationItem>() to 0
            val list = arr.mapNotNull {
                runCatching { gson.fromJson(it, TikNetNotificationItem::class.java) }.getOrNull()
            }
            val unread = rootEl.get("unread_count")?.asInt ?: list.count { !it.read }
            return list to unread
        }
    }

    fun markNotificationRead(baseUrl: String, token: String, id: Int) {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/notifications/$id/read")
            .post("{}".toRequestBody(jsonMedia))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw TikNetApiException("read HTTP ${resp.code}", resp.code)
        }
    }

    fun getFaq(baseUrl: String): List<TikNetFaqItem> {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/faq")
            .get()
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw TikNetApiException("faq HTTP ${resp.code}", resp.code)
            val text = resp.body?.string().orEmpty()
            val rootEl = JsonParser.parseString(text).asJsonObject
            val arr = rootEl.getAsJsonArray("items") ?: return emptyList()
            return arr.mapNotNull {
                runCatching { gson.fromJson(it, TikNetFaqItem::class.java) }.getOrNull()
            }
        }
    }

    fun healthOk(baseUrl: String): Boolean {
        return try {
            val req = Request.Builder()
                .url("${root(baseUrl)}/api/health")
                .get()
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    fun getReferral(baseUrl: String, token: String): TikNetReferralInfo {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/referral")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val detail = runCatching {
                    JsonParser.parseString(text).asJsonObject.get("detail")?.asString
                }.getOrNull()
                throw TikNetApiException(detail ?: "referral HTTP ${resp.code}", resp.code)
            }
            return TikNetReferralParser.parse(text)
        }
    }

    fun attachReferral(baseUrl: String, token: String, referralCode: String): TikNetReferralAttachResult {
        val code = referralCode.trim()
        val body = gson.toJson(mapOf("referral_code" to code)).toRequestBody(jsonMedia)
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/referral/attach")
            .post(body)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val detail = runCatching {
                    JsonParser.parseString(text).asJsonObject.get("detail")?.asString
                }.getOrNull()
                throw TikNetApiException(detail ?: "referral attach HTTP ${resp.code}", resp.code)
            }
            return TikNetReferralParser.parseAttach(text, code)
        }
    }

    fun getUsageHistory(baseUrl: String, token: String, days: Int = 14): TikNetUsageHistory {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/usage-history?days=$days")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val detail = runCatching {
                    JsonParser.parseString(text).asJsonObject.get("detail")?.asString
                }.getOrNull()
                throw TikNetApiException(detail ?: "usage HTTP ${resp.code}", resp.code)
            }
            val rootEl = JsonParser.parseString(text).asJsonObject
            val points = rootEl.getAsJsonArray("points")?.mapNotNull { el ->
                if (!el.isJsonObject) return@mapNotNull null
                val o = el.asJsonObject
                TikNetUsagePoint(
                    t = o.get("t")?.takeIf { !it.isJsonNull }?.asString,
                    usedGb = o.get("used_gb")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0,
                    limitGb = o.get("limit_gb")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0,
                )
            } ?: emptyList()
            return TikNetUsageHistory(
                orderId = rootEl.get("order_id")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                usedGb = rootEl.get("used_gb")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0,
                limitGb = rootEl.get("limit_gb")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0,
                points = points,
            )
        }
    }

    fun listSessions(baseUrl: String, token: String): List<TikNetSession> {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/sessions")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val detail = runCatching {
                    JsonParser.parseString(text).asJsonObject.get("detail")?.asString
                }.getOrNull()
                throw TikNetApiException(detail ?: "sessions HTTP ${resp.code}", resp.code)
            }
            val arr = JsonParser.parseString(text).asJsonObject.getAsJsonArray("sessions")
                ?: return emptyList()
            return arr.mapNotNull {
                runCatching { gson.fromJson(it, TikNetSession::class.java) }.getOrNull()
            }
        }
    }

    fun revokeSession(baseUrl: String, token: String, sessionId: Int) {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/sessions/$sessionId")
            .delete()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val text = resp.body?.string().orEmpty()
                val detail = runCatching {
                    JsonParser.parseString(text).asJsonObject.get("detail")?.asString
                }.getOrNull()
                throw TikNetApiException(detail ?: "revoke HTTP ${resp.code}", resp.code)
            }
        }
    }

    fun registerDevice(
        baseUrl: String,
        token: String,
        deviceId: String,
        deviceModel: String = "",
        appVersion: String = "",
        versionCode: Int? = null,
        androidSdk: Int? = null,
        platform: String = "android",
    ) {
        val payload = mutableMapOf<String, Any>(
            "device_id" to deviceId,
            "platform" to platform,
            "device_model" to deviceModel,
            "app_version" to appVersion,
        )
        if (versionCode != null) payload["version_code"] = versionCode
        if (androidSdk != null) payload["android_sdk"] = androidSdk
        val body = gson.toJson(payload).toRequestBody(jsonMedia)
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/device/register")
            .post(body)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            // Soft-fail: older panels may not implement this endpoint.
            if (!resp.isSuccessful && resp.code != 404 && resp.code != 501) {
                throw TikNetApiException("device register HTTP ${resp.code}", resp.code)
            }
        }
    }

    private fun <T> executeJson(req: Request, clazz: Class<T>): T {
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val detail = runCatching {
                    JsonParser.parseString(text).asJsonObject.get("detail")?.asString
                }.getOrNull()
                throw TikNetApiException(detail ?: "HTTP ${resp.code}", resp.code)
            }
            return gson.fromJson(text, clazz)
                ?: throw TikNetApiException("Empty response")
        }
    }

    /** Resolve panel base URL like Flutter ConfigService. */
    fun resolveBaseUrl(ctx: Context): String {
        val sources = listOf(
            "https://ara9900.github.io/app-config/config.json",
            "https://panel.tikn.ir/static/config.json",
        )
        for (url in sources) {
            val urls = fetchApiUrls(url)
            if (urls.isNotEmpty()) {
                TikNetPrefs.savePanelUrlsCache(ctx, urls)
                for (u in urls) {
                    if (healthOk(u)) return u.trim().trimEnd('/')
                }
                return urls.first().trim().trimEnd('/')
            }
        }
        for (u in TikNetPrefs.getPanelUrlsCache(ctx)) {
            if (healthOk(u)) return u
        }
        val cached = TikNetPrefs.getBaseUrl(ctx)
        if (!cached.isNullOrBlank()) return cached
        return "https://panel.tikn.ir"
    }

    private fun fetchApiUrls(configUrl: String): List<String> {
        return try {
            val req = Request.Builder().url(configUrl).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val text = resp.body?.string().orEmpty()
                val rootEl = JsonParser.parseString(text).asJsonObject
                val arr = rootEl.getAsJsonArray("api_urls") ?: return emptyList()
                arr.mapNotNull { it.asString?.trim()?.takeIf { s -> s.startsWith("http") } }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
