package com.v2ray.ang.tiknet

/**
 * Dual product modes on the same APK (`com.tik.net`):
 * - [KIND_VPN]: full filter-bypass / classic configs
 * - [KIND_GAME_DNS]: Cafe Bazaar / Myket-facing game accelerator (WG allowlist only)
 *
 * Mode comes from panel `/api/customer/me` → `product_kind` after login.
 */
object TikNetProduct {
    const val KIND_VPN = "vpn"
    const val KIND_GAME_DNS = "game_dns"

    fun normalize(raw: String?): String {
        val v = raw?.trim()?.lowercase().orEmpty()
        return when (v) {
            KIND_GAME_DNS, "game-dns", "dns", "dns_game", "gamedns" -> KIND_GAME_DNS
            else -> KIND_VPN
        }
    }

    fun isGameDns(kind: String?): Boolean = normalize(kind) == KIND_GAME_DNS

    fun isGameDns(user: TikNetUserInfo?): Boolean = isGameDns(user?.productKind)

    fun subscriptionRemarks(kind: String?): String =
        if (isGameDns(kind)) "TikNet DNS" else "TikNet"

    fun connectSubtitle(kind: String?): String =
        if (isGameDns(kind)) "مسیر بازی‌ها از طریق DNS گیم"
        else "ترافیک شما از طریق تونل امن عبور می‌کند"

    fun syncLabel(kind: String?): String =
        if (isGameDns(kind)) "بروزرسانی کانفیگ DNS"
        else "بروزرسانی اشتراک"

    private val PROXY_SCHEMES = listOf(
        "vless://", "vmess://", "ss://", "trojan://", "hy2://", "hysteria2://",
        "tuic://", "hysteria://", "socks://", "http://", "https://",
    )

    /**
     * For game_dns accounts keep WireGuard only so VLESS/etc never appear
     * even if the panel feed is misconfigured.
     */
    fun filterSubscriptionPayload(payload: String, kind: String?): String {
        if (!isGameDns(kind)) return payload
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return ""
        // Clash / sing-box JSON profiles are full-tunnel — never import for game DNS.
        if ((trimmed.startsWith("{") || trimmed.startsWith("[")) &&
            !trimmed.contains("[Interface]", ignoreCase = true)
        ) {
            return ""
        }
        val lines = trimmed.lineSequence().map { it.trimEnd() }.toList()
        val wgLinks = lines.filter { it.trim().lowercase().startsWith("wireguard://") }
        if (wgLinks.isNotEmpty()) {
            return wgLinks.joinToString("\n")
        }
        // Plain WireGuard .conf body
        if (trimmed.contains("[Interface]", ignoreCase = true) &&
            trimmed.contains("[Peer]", ignoreCase = true)
        ) {
            return trimmed
        }
        // Strip known proxy share-links; keep anything else only if no proxy schemes remain.
        val cleaned = lines.filter { line ->
            val lower = line.trim().lowercase()
            if (lower.isEmpty()) return@filter false
            PROXY_SCHEMES.none { lower.startsWith(it) }
        }
        return cleaned.joinToString("\n").trim()
    }
}
