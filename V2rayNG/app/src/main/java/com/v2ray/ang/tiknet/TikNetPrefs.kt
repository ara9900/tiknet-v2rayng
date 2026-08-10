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
}
