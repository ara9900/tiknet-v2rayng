package com.v2ray.ang.tiknet

import android.content.Context
import java.nio.charset.StandardCharsets
import android.util.Base64
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager

object TikNetSync {
    /**
     * Download personal subscription from panel and import into v2rayNG profiles
     * under a fixed subscription id so refresh replaces the same set.
     */
    fun syncPersonalSubscription(ctx: Context): Int {
        val base = TikNetPrefs.getBaseUrl(ctx) ?: throw TikNetApiException("no base url")
        val token = TikNetPrefs.getAccessToken(ctx) ?: throw TikNetApiException("not logged in")
        val bytes = TikNetApi.getSubscriptionConfigBytes(base, token)
        if (bytes.isEmpty()) throw TikNetApiException("empty subscription")

        ensureSubscriptionShell()
        val payload = decodePayload(bytes)
        val (count, _) = AngConfigManager.importBatchConfig(payload, TikNetPrefs.TIKNET_SUB_GUID, append = false)
        val sub = MmkvManager.decodeSubscription(TikNetPrefs.TIKNET_SUB_GUID) ?: SubscriptionItem()
        sub.remarks = "TikNet"
        sub.enabled = true
        sub.lastUpdated = System.currentTimeMillis()
        MmkvManager.encodeSubscription(TikNetPrefs.TIKNET_SUB_GUID, sub)
        return count
    }

    fun importCatalogServer(ctx: Context, serverId: Int, name: String): Int {
        val base = TikNetPrefs.getBaseUrl(ctx) ?: throw TikNetApiException("no base url")
        val token = TikNetPrefs.getAccessToken(ctx) ?: throw TikNetApiException("not logged in")
        val bytes = TikNetApi.getServerConfigBytes(base, token, serverId)
        if (bytes.isEmpty()) throw TikNetApiException("empty server config")
        val subId = "tiknet-cat-$serverId"
        val sub = SubscriptionItem(
            remarks = name.ifBlank { "Catalog $serverId" },
            enabled = true,
            lastUpdated = System.currentTimeMillis(),
        )
        MmkvManager.encodeSubscription(subId, sub)
        val payload = decodePayload(bytes)
        val (count, _) = AngConfigManager.importBatchConfig(payload, subId, append = false)
        return count
    }

    private fun ensureSubscriptionShell() {
        val existing = MmkvManager.decodeSubscription(TikNetPrefs.TIKNET_SUB_GUID)
        if (existing == null) {
            MmkvManager.encodeSubscription(
                TikNetPrefs.TIKNET_SUB_GUID,
                SubscriptionItem(remarks = "TikNet", enabled = true),
            )
        }
    }

    private fun decodePayload(bytes: ByteArray): String {
        // Panel may return plain share-links, base64 subscription body, or JSON.
        val asText = runCatching { String(bytes, StandardCharsets.UTF_8) }.getOrDefault("")
        val trimmed = asText.trim()
        if (trimmed.startsWith("vless://") ||
            trimmed.startsWith("vmess://") ||
            trimmed.startsWith("ss://") ||
            trimmed.startsWith("trojan://") ||
            trimmed.startsWith("wireguard://") ||
            trimmed.startsWith("hy2://") ||
            trimmed.startsWith("hysteria2://") ||
            trimmed.startsWith("{") ||
            trimmed.startsWith("[")
        ) {
            return trimmed
        }
        // Try base64 decode (subscription style).
        val decoded = runCatching {
            String(Base64.decode(trimmed, Base64.DEFAULT), StandardCharsets.UTF_8).trim()
        }.getOrNull()
        if (!decoded.isNullOrBlank()) return decoded
        return trimmed
    }
}
