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
 * TikNet defaults: Iran routing whitelist + Iran geo rule-set source.
 * Refreshes geo assets quietly on each app open.
 */
object TikNetBootstrap {
    private const val IRAN_GEO_SOURCE = "Chocolate4U/Iran-v2ray-rules"

    fun applyDefaults(context: Context) {
        runCatching {
            SettingsManager.resetRoutingRulesetsFromPresets(context, RoutingType.WHITE_IRAN)
            MmkvManager.encodeSettings(AppConfig.PREF_GEO_FILES_SOURCES, IRAN_GEO_SOURCE)
        }.onFailure {
            LogUtil.e(AppConfig.TAG, "TikNetBootstrap defaults failed", it)
        }
    }

    fun refreshGeoAssets(context: Context) {
        runCatching {
            val extDir = File(Utils.userAssetPath(context))
            if (!extDir.exists()) extDir.mkdirs()
            val source = MmkvManager.decodeSettingsString(AppConfig.PREF_GEO_FILES_SOURCES)
                ?: IRAN_GEO_SOURCE
            val files = listOf(AppConfig.GEOSITE_DAT, AppConfig.GEOIP_DAT)
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
