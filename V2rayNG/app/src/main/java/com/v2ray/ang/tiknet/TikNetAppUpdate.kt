package com.v2ray.ang.tiknet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.v2ray.ang.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class TikNetAppUpdateState {
    object Idle : TikNetAppUpdateState()
    object Checking : TikNetAppUpdateState()
    object UpToDate : TikNetAppUpdateState()
    data class Available(val info: TikNetAppUpdateInfo) : TikNetAppUpdateState()
    data class Downloading(val info: TikNetAppUpdateInfo, val progress: Int) : TikNetAppUpdateState()
    data class Error(val message: String, val info: TikNetAppUpdateInfo? = null) : TikNetAppUpdateState()
}

object TikNetAppUpdateController {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun check(ctx: Context): TikNetAppUpdateState = withContext(Dispatchers.IO) {
        val base = TikNetPrefs.getBaseUrl(ctx)
            ?: runCatching { TikNetApi.resolveBaseUrl(ctx) }.getOrNull()
            ?: return@withContext TikNetAppUpdateState.Idle
        try {
            val info = TikNetApi.getAppUpdate(base)
            if (info.enabled && info.versionCode > BuildConfig.VERSION_CODE) {
                TikNetAppUpdateState.Available(info)
            } else {
                TikNetAppUpdateState.UpToDate
            }
        } catch (error: Exception) {
            TikNetAppUpdateState.Error(TikNetErrors.message(error))
        }
    }

    suspend fun downloadAndInstall(
        ctx: Context,
        info: TikNetAppUpdateInfo,
        onProgress: (Int) -> Unit = {},
    ): Boolean = withContext(Dispatchers.IO) {
        if (info.apkUrl.isBlank()) throw TikNetApiException("apk url is empty")

        val apk = File(ctx.cacheDir, "tiknet-update-${info.versionCode}.apk")
        apk.parentFile?.mkdirs()
        if (apk.exists()) apk.delete()

        onProgress(0)
        val req = Request.Builder().url(info.apkUrl).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw TikNetApiException("apk download HTTP ${resp.code}", resp.code)
            }
            val body = resp.body ?: throw TikNetApiException("Empty apk response")
            val total = body.contentLength()
            var lastProgress = -1
            var downloaded = 0L
            body.byteStream().use { input ->
                FileOutputStream(apk).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val progress = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }
            if (lastProgress < 100) {
                onProgress(100)
            }
        }

        if (info.sha256.isNotBlank()) {
            verifySha256(apk, info.sha256)
        }

        val installStarted = withContext(Dispatchers.Main) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !ctx.packageManager.canRequestPackageInstalls()
            ) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${ctx.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(settingsIntent)
                false
            } else {
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.cache", apk)
                val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
                ctx.startActivity(installIntent)
                true
            }
        }
        installStarted
    }

    private fun verifySha256(file: File, expected: String) {
        val normalized = expected.trim().lowercase()
        if (normalized.isEmpty()) return

        val actual = MessageDigest.getInstance("SHA-256").let { digest ->
            file.inputStream().use { input ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }

        if (actual != normalized) {
            throw TikNetApiException("SHA-256 mismatch")
        }
    }
}
