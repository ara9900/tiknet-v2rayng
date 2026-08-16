package com.v2ray.ang.tiknet

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.enums.RoutingType
import com.v2ray.ang.extension.concatUrl
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import java.io.File

/**
 * TikNet defaults: optional Iran+LAN direct routing + Iran geo rule-set source.
 * Geo assets refresh at most once per [GEO_REFRESH_TTL_MS] to avoid multi‑MB downloads
 * on every cold start (that competed with VPN bring‑up for ~30s on Android 16).
 */
object TikNetBootstrap {
    private const val IRAN_GEO_SOURCE = "Chocolate4U/Iran-v2ray-rules"
    private const val PREF_ROUTING_SEEDED = "tiknet_routing_iran_seeded"
    private const val PREF_GEO_LAST_OK_MS = "tiknet_geo_last_ok_ms"
    private const val GEO_REFRESH_TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    private const val MIN_GEO_BYTES = 100_000L

    fun applyDefaults(context: Context) {
        runCatching {
            val iranOn = TikNetPrefs.isIranDirectEnabled(context)
            if (iranOn) {
                if (MmkvManager.decodeSettingsBool(PREF_ROUTING_SEEDED) != true) {
                    SettingsManager.resetRoutingRulesetsFromPresets(context, RoutingType.WHITE_IRAN)
                    MmkvManager.encodeSettings(PREF_ROUTING_SEEDED, true)
                    LogUtil.i(AppConfig.TAG, "TikNetBootstrap: Iran routing seeded")
                }
                MmkvManager.encodeSettings(AppConfig.PREF_GEO_FILES_SOURCES, IRAN_GEO_SOURCE)
            }
        }.onFailure {
            LogUtil.e(AppConfig.TAG, "TikNetBootstrap defaults failed", it)
        }
    }

    /**
     * Enable/disable Iran domains+IPs and keep LAN/private direct.
     * Off → global proxy preset (still bypasses LAN).
     */
    fun setIranDirectRouting(context: Context, enabled: Boolean) {
        TikNetPrefs.setIranDirectEnabled(context, enabled)
        if (enabled) {
            SettingsManager.resetRoutingRulesetsFromPresets(context, RoutingType.WHITE_IRAN)
            MmkvManager.encodeSettings(PREF_ROUTING_SEEDED, true)
            MmkvManager.encodeSettings(AppConfig.PREF_GEO_FILES_SOURCES, IRAN_GEO_SOURCE)
            // Force a geo refresh attempt so category-ir / geoip:ir work.
            MmkvManager.encodeSettings(PREF_GEO_LAST_OK_MS, "0")
            refreshGeoAssets(context)
            LogUtil.i(AppConfig.TAG, "TikNetBootstrap: Iran direct ON")
        } else {
            SettingsManager.resetRoutingRulesetsFromPresets(context, RoutingType.GLOBAL)
            MmkvManager.encodeSettings(PREF_ROUTING_SEEDED, false)
            LogUtil.i(AppConfig.TAG, "TikNetBootstrap: Iran direct OFF (global + LAN)")
        }
    }

    fun refreshGeoAssets(context: Context) {
        runCatching {
            val extDir = File(Utils.userAssetPath(context))
            if (!extDir.exists()) extDir.mkdirs()

            val lastOk = MmkvManager.decodeSettingsString(PREF_GEO_LAST_OK_MS)?.toLongOrNull() ?: 0L
            val files = listOf(AppConfig.GEOSITE_DAT, AppConfig.GEOIP_DAT)
            val allPresent = files.all { name ->
                val f = File(extDir, name)
                f.isFile && f.length() >= MIN_GEO_BYTES
            }
            val fresh = (System.currentTimeMillis() - lastOk) < GEO_REFRESH_TTL_MS
            if (allPresent && fresh) {
                LogUtil.i(AppConfig.TAG, "TikNetBootstrap: geo assets fresh, skip download")
                return
            }

            val source = MmkvManager.decodeSettingsString(AppConfig.PREF_GEO_FILES_SOURCES)
                ?: IRAN_GEO_SOURCE
            for (name in files) {
                val url = String.format(AppConfig.GITHUB_DOWNLOAD_URL, source).concatUrl(name)
                downloadBinary(url, File(extDir, name))
            }
            runCatching {
                downloadBinary(
                    AppConfig.GEOIP_ONLY_CN_PRIVATE_URL,
                    File(extDir, AppConfig.GEOIP_ONLY_CN_PRIVATE_DAT),
                )
            }
            MmkvManager.encodeSettings(PREF_GEO_LAST_OK_MS, System.currentTimeMillis().toString())
            LogUtil.i(AppConfig.TAG, "TikNetBootstrap: geo assets refreshed from $source")
        }.onFailure {
            LogUtil.e(AppConfig.TAG, "TikNetBootstrap geo refresh failed", it)
        }
    }

    private fun downloadBinary(url: String, target: File) {
        val tmp = File(target.absolutePath + "_tmp")
        try {
            val conn = java.net.URL(url).openConnection()
            conn.connectTimeout = 15_000
            conn.readTimeout = 45_000
            conn.getInputStream().use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            if (tmp.length() > 1024) {
                if (target.exists()) target.delete()
                tmp.renameTo(target)
            } else {
                tmp.delete()
            }
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
    }
}
