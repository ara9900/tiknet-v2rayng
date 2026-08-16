package com.v2ray.ang.tiknet

import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

/** Parsed QR / deep-link payload for TikNet login (mirrors Flutter 4.2.77). */
sealed class TikNetQrLoginPayload

data class TikNetQrCredentials(
    val username: String,
    val password: String,
    val panelUrl: String? = null,
) : TikNetQrLoginPayload()

data class TikNetQrLoginToken(
    val token: String,
    val panelUrl: String? = null,
) : TikNetQrLoginPayload()

data class TikNetQrSubscriptionLink(val url: String) : TikNetQrLoginPayload()

object TikNetQrLogin {
    fun parse(raw: String): TikNetQrLoginPayload? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        if (text.startsWith("{")) {
            runCatching {
                val map = JSONObject(text)
                val token = (map.optString("token").ifBlank { map.optString("login_token") }).trim()
                val panel = listOf("panel_url", "panel", "base_url")
                    .map { map.optString(it).trim() }
                    .firstOrNull { it.isNotEmpty() }
                if (token.isNotEmpty()) {
                    return TikNetQrLoginToken(token = token, panelUrl = panel)
                }
                val user = listOf("username", "user", "u")
                    .map { map.optString(it).trim() }
                    .firstOrNull { it.isNotEmpty() }
                    .orEmpty()
                val pass = listOf("password", "pass", "p")
                    .map { if (map.has(it) && !map.isNull(it)) map.optString(it) else "" }
                    .firstOrNull { it.isNotEmpty() }
                    .orEmpty()
                if (user.isNotEmpty() && pass.isNotEmpty()) {
                    return TikNetQrCredentials(username = user, password = pass, panelUrl = panel)
                }
            }
        }

        val uri = runCatching { URI(text) }.getOrNull()
        if (uri != null && uri.scheme.equals("tiknet", ignoreCase = true)) {
            val token = (queryParam(uri, "token") ?: queryParam(uri, "login_token")).orEmpty()
            val panel = queryParam(uri, "panel") ?: queryParam(uri, "panel_url")
            if (token.isNotEmpty()) {
                return TikNetQrLoginToken(token = token, panelUrl = panel?.trim()?.takeIf { it.isNotEmpty() })
            }
            val user = (queryParam(uri, "username") ?: queryParam(uri, "user")).orEmpty()
            val pass = (queryParam(uri, "password") ?: queryParam(uri, "pass")).orEmpty()
            if (user.isNotEmpty() && pass.isNotEmpty()) {
                return TikNetQrCredentials(username = user, password = pass, panelUrl = panel?.trim()?.takeIf { it.isNotEmpty() })
            }
        }

        if (uri != null && (uri.scheme.equals("http", true) || uri.scheme.equals("https", true))) {
            val path = uri.path.orEmpty().lowercase()
            val token = (queryParam(uri, "token") ?: queryParam(uri, "login_token")).orEmpty().trim()
            if (token.isNotEmpty() && (path.contains("/app/login") || path.endsWith("/login"))) {
                val panel = buildString {
                    append(uri.scheme).append("://").append(uri.host)
                    if (uri.port != -1) append(':').append(uri.port)
                }
                return TikNetQrLoginToken(token = token, panelUrl = panel)
            }
            return TikNetQrSubscriptionLink(text)
        }

        val colon = text.indexOf(':')
        if (colon > 0 && colon < text.length - 1 && !text.startsWith("http")) {
            val user = text.substring(0, colon).trim()
            val pass = text.substring(colon + 1)
            if (user.isNotEmpty() && pass.isNotEmpty() && !user.contains(' ')) {
                return TikNetQrCredentials(username = user, password = pass)
            }
        }

        return null
    }

    fun isLoginDeepLink(link: String): Boolean {
        val uri = runCatching { URI(link.trim()) }.getOrNull() ?: return false
        if (uri.scheme.equals("tiknet", true)) return true
        if (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
            val path = uri.path.orEmpty().lowercase()
            val token = queryParam(uri, "token").orEmpty().trim()
            return token.isNotEmpty() && (path.contains("/app/login") || path.endsWith("/login"))
        }
        return false
    }

    private fun queryParam(uri: URI, key: String): String? {
        val rawQuery = uri.rawQuery ?: return null
        val match = rawQuery.split('&')
            .firstOrNull { part -> part.substringBefore('=') == key }
            ?: return null
        val rawValue = match.substringAfter('=', "")
        if (rawValue.isEmpty()) return ""
        return runCatching {
            URLDecoder.decode(rawValue, Charsets.UTF_8.name()).trim()
        }.getOrDefault(rawValue.trim())
    }
}
