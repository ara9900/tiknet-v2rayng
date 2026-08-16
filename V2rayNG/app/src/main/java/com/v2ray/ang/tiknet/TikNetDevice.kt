package com.v2ray.ang.tiknet

import android.content.Context
import android.os.Build
import com.v2ray.ang.BuildConfig
import java.util.UUID

/**
 * Persistent device id + silent panel register (best-effort, never blocks UI).
 */
object TikNetDevice {
    fun getOrCreateDeviceId(ctx: Context): String {
        val existing = TikNetPrefs.getDeviceId(ctx)
        if (!existing.isNullOrBlank()) return existing
        val id = UUID.randomUUID().toString()
        TikNetPrefs.saveDeviceId(ctx, id)
        return id
    }

    fun registerIfLoggedIn(ctx: Context) {
        runCatching {
            val base = TikNetPrefs.getBaseUrl(ctx) ?: return
            val token = TikNetPrefs.getAccessToken(ctx) ?: return
            val deviceId = getOrCreateDeviceId(ctx)
            TikNetApi.registerDevice(
                baseUrl = base,
                token = token,
                deviceId = deviceId,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                appVersion = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                androidSdk = Build.VERSION.SDK_INT,
            )
        }
    }
}
