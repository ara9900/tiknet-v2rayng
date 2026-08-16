package com.v2ray.ang.tiknet

import android.content.Context
import android.content.SharedPreferences

/**
 * Session + bootstrap prefs for TikNet panel (ported from Flutter TikNet).
 */
object TikNetPrefs {
    private const val PREFS = "tiknet_prefs"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_BASE_URL = "base_url"
    const val KEY_USERNAME = "username"
    const val KEY_PANEL_URLS_CACHE = "panel_urls_json"
    const val KEY_SUB_GUID = "tiknet_sub_guid"
    const val TIKNET_SUB_GUID = "tiknet-personal"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_ENTITLEMENT_NOTIF_PREFIX = "entitlement_notif_"
    private const val KEY_PINNED_SERVERS = "pinned_server_guids"
    private const val KEY_WIDGET_MODE = "widget_connect_mode"
    private const val KEY_WIDGET_SERVER = "widget_server_guid"
    private const val KEY_WIDGET_CONNECTING = "widget_connecting"
    private const val KEY_IRAN_DIRECT = "iran_direct_routing_enabled"

    const val WIDGET_MODE_CURRENT = "current"
    const val WIDGET_MODE_SMART = "smart"
    const val WIDGET_MODE_FIXED = "fixed"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isLoggedIn(ctx: Context): Boolean =
        !getAccessToken(ctx).isNullOrBlank() && !getBaseUrl(ctx).isNullOrBlank()

    fun getAccessToken(ctx: Context): String? =
        prefs(ctx).getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun getBaseUrl(ctx: Context): String? =
        prefs(ctx).getString(KEY_BASE_URL, null)?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }

    fun getUsername(ctx: Context): String? =
        prefs(ctx).getString(KEY_USERNAME, null)

    fun saveSession(ctx: Context, baseUrl: String, accessToken: String, username: String?) {
        prefs(ctx).edit()
            .putString(KEY_BASE_URL, baseUrl.trim().trimEnd('/'))
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun clearSession(ctx: Context) {
        prefs(ctx).edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_USERNAME)
            // keep base_url / panel cache for faster next login
            .apply()
    }

    fun savePanelUrlsCache(ctx: Context, urls: List<String>) {
        if (urls.isEmpty()) return
        val json = urls.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]") {
            it.replace("\"", "")
        }
        prefs(ctx).edit().putString(KEY_PANEL_URLS_CACHE, json).apply()
    }

    fun getPanelUrlsCache(ctx: Context): List<String> {
        val raw = prefs(ctx).getString(KEY_PANEL_URLS_CACHE, null) ?: return emptyList()
        return raw.trim()
            .removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map { it.trim().trim('"') }
            .filter { it.startsWith("http") }
    }

    private const val KEY_CACHED_PROFILE = "cached_profile_json"

    fun saveCachedProfile(ctx: Context, user: TikNetUserInfo) {
        val json = org.json.JSONObject()
            .put("username", user.username)
            .put("full_name", user.fullName)
            .put("expire_date", user.expireDate)
            .put("has_subscription", user.hasSubscription)
            .put("plan_name", user.planName)
            .put("is_expired", user.isExpired)
            .put("days_remaining", user.daysRemaining)
            .put("traffic_used_bytes", user.trafficUsedBytes)
            .put("traffic_limit_bytes", user.trafficLimitBytes)
            .toString()
        prefs(ctx).edit().putString(KEY_CACHED_PROFILE, json).apply()
    }

    fun getCachedProfile(ctx: Context): TikNetUserInfo? {
        val raw = prefs(ctx).getString(KEY_CACHED_PROFILE, null) ?: return null
        return runCatching {
            val o = org.json.JSONObject(raw)
            TikNetUserInfo(
                username = o.optString("username"),
                fullName = o.optString("full_name").takeIf { it.isNotBlank() },
                expireDate = o.optString("expire_date").takeIf { it.isNotBlank() },
                hasSubscription = o.optBoolean("has_subscription"),
                planName = o.optString("plan_name").takeIf { it.isNotBlank() },
                isExpired = if (o.has("is_expired") && !o.isNull("is_expired")) o.optBoolean("is_expired") else null,
                daysRemaining = if (o.has("days_remaining") && !o.isNull("days_remaining")) o.optInt("days_remaining") else null,
                trafficUsedBytes = if (o.has("traffic_used_bytes") && !o.isNull("traffic_used_bytes")) o.optLong("traffic_used_bytes") else null,
                trafficLimitBytes = if (o.has("traffic_limit_bytes") && !o.isNull("traffic_limit_bytes")) o.optLong("traffic_limit_bytes") else null,
            )
        }.getOrNull()
    }

    fun getDeviceId(ctx: Context): String? =
        prefs(ctx).getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }

    fun saveDeviceId(ctx: Context, id: String) {
        prefs(ctx).edit().putString(KEY_DEVICE_ID, id).apply()
    }

    fun getEntitlementNotifDay(ctx: Context, kind: String): String? =
        prefs(ctx).getString(KEY_ENTITLEMENT_NOTIF_PREFIX + kind, null)

    fun saveEntitlementNotifDay(ctx: Context, kind: String, dayKey: String) {
        prefs(ctx).edit().putString(KEY_ENTITLEMENT_NOTIF_PREFIX + kind, dayKey).apply()
    }

    fun getPinnedServers(ctx: Context): Set<String> {
        val raw = prefs(ctx).getString(KEY_PINNED_SERVERS, null) ?: return emptySet()
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    fun togglePinnedServer(ctx: Context, guid: String): Set<String> {
        val cur = getPinnedServers(ctx).toMutableSet()
        if (!cur.add(guid)) cur.remove(guid)
        prefs(ctx).edit()
            .putString(KEY_PINNED_SERVERS, cur.joinToString(","))
            .apply()
        return cur
    }

    fun getWidgetMode(ctx: Context): String {
        // MMKV is multi-process — widget runs in the VPN daemon process.
        val mmkv = com.v2ray.ang.handler.MmkvManager.decodeSettingsString(KEY_WIDGET_MODE)
        if (!mmkv.isNullOrBlank()) return mmkv
        val legacy = prefs(ctx).getString(KEY_WIDGET_MODE, WIDGET_MODE_CURRENT) ?: WIDGET_MODE_CURRENT
        com.v2ray.ang.handler.MmkvManager.encodeSettings(KEY_WIDGET_MODE, legacy)
        return legacy
    }

    fun setWidgetMode(ctx: Context, mode: String) {
        com.v2ray.ang.handler.MmkvManager.encodeSettings(KEY_WIDGET_MODE, mode)
        prefs(ctx).edit().putString(KEY_WIDGET_MODE, mode).apply()
    }

    fun getWidgetServerGuid(ctx: Context): String? {
        val mmkv = com.v2ray.ang.handler.MmkvManager.decodeSettingsString(KEY_WIDGET_SERVER)
        if (!mmkv.isNullOrBlank()) return mmkv
        val legacy = prefs(ctx).getString(KEY_WIDGET_SERVER, null)?.takeIf { it.isNotBlank() }
        if (!legacy.isNullOrBlank()) {
            com.v2ray.ang.handler.MmkvManager.encodeSettings(KEY_WIDGET_SERVER, legacy)
        }
        return legacy
    }

    fun setWidgetServerGuid(ctx: Context, guid: String?) {
        com.v2ray.ang.handler.MmkvManager.encodeSettings(KEY_WIDGET_SERVER, guid)
        prefs(ctx).edit().putString(KEY_WIDGET_SERVER, guid).apply()
    }

    fun isWidgetConnecting(ctx: Context): Boolean =
        com.v2ray.ang.handler.MmkvManager.decodeSettingsBool(KEY_WIDGET_CONNECTING, false)

    fun setWidgetConnecting(ctx: Context, connecting: Boolean) {
        com.v2ray.ang.handler.MmkvManager.encodeSettings(KEY_WIDGET_CONNECTING, connecting)
    }

    /** Default true — Iran + LAN direct routing is TikNet's recommended mode. */
    fun isIranDirectEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_IRAN_DIRECT, true)

    fun setIranDirectEnabled(ctx: Context, enabled: Boolean) {
        // commit() so a following read cannot see a stale value (apply() is async).
        prefs(ctx).edit().putBoolean(KEY_IRAN_DIRECT, enabled).commit()
    }
}
