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
)

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

    fun getMe(baseUrl: String, token: String): TikNetUserInfo {
        val req = Request.Builder()
            .url("${root(baseUrl)}/api/customer/me")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        return executeJson(req, TikNetUserInfo::class.java)
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
