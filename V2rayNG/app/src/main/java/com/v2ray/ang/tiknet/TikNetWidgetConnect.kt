package com.v2ray.ang.tiknet

import android.content.Context
import com.v2ray.ang.handler.MmkvManager

/**
 * Resolves which server the home-screen widget should connect with.
 */
object TikNetWidgetConnect {
    /**
     * @return selected server guid, or null if none available.
     */
    fun resolveServerGuid(ctx: Context): String? {
        return when (TikNetPrefs.getWidgetMode(ctx)) {
            TikNetPrefs.WIDGET_MODE_SMART -> pickBestGuid(ctx) ?: MmkvManager.getSelectServer()
            TikNetPrefs.WIDGET_MODE_FIXED -> {
                val fixed = TikNetPrefs.getWidgetServerGuid(ctx)
                if (!fixed.isNullOrBlank() && MmkvManager.decodeServerConfig(fixed) != null) {
                    fixed
                } else {
                    MmkvManager.getSelectServer()
                }
            }
            else -> MmkvManager.getSelectServer()
        }
    }

    private fun pickBestGuid(ctx: Context): String? {
        val guids = linkedSetOf<String>()
        guids.addAll(MmkvManager.decodeServerList(TikNetPrefs.TIKNET_SUB_GUID))
        MmkvManager.decodeSubsList().forEach { subId ->
            if (subId.startsWith("tiknet-")) {
                guids.addAll(MmkvManager.decodeServerList(subId))
            }
        }
        if (guids.isEmpty()) guids.addAll(MmkvManager.decodeAllServerList())
        val scored = guids.mapNotNull { guid ->
            val cfg = MmkvManager.decodeServerConfig(guid) ?: return@mapNotNull null
            val ping = MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis
                ?.takeIf { it > 0 && it < 65_000 }
            Triple(guid, cfg.remarks, ping)
        }
        if (scored.isEmpty()) return null
        val withPing = scored.filter { it.third != null }.minByOrNull { it.third!! }
        return withPing?.first ?: scored.first().first
    }
}
