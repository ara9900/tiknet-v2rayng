package com.v2ray.ang.tiknet

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.dto.TestServiceMessage
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.receiver.CompactWidgetProvider
import com.v2ray.ang.receiver.WidgetProvider
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Widget connect resolver + smart ping-then-connect (same idea as in-app smart mode).
 */
object TikNetWidgetConnect {
    private const val SMART_PING_TIMEOUT_MS = 12_000L
    private val smartFinishLock = Any()
    private val timeoutPosted = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * @return selected server guid, or null if none available.
     * For SMART mode this only reads last ping results (used for labels); connect path uses [beginSmartConnect].
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

    fun listServerGuids(): List<String> {
        val guids = linkedSetOf<String>()
        guids.addAll(MmkvManager.decodeServerList(TikNetPrefs.TIKNET_SUB_GUID))
        MmkvManager.decodeSubsList().forEach { subId ->
            if (subId.startsWith("tiknet-")) {
                guids.addAll(MmkvManager.decodeServerList(subId))
            }
        }
        if (guids.isEmpty()) guids.addAll(MmkvManager.decodeAllServerList())
        return guids.filter { MmkvManager.decodeServerConfig(it) != null }
    }

    fun pickBestGuid(ctx: Context): String? {
        val scored = listServerGuids().mapNotNull { guid ->
            val ping = MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis
                ?.takeIf { it > 0 && it < 65_000 }
            Triple(guid, ping, MmkvManager.decodeServerConfig(guid)?.remarks)
        }
        if (scored.isEmpty()) return null
        val withPing = scored.filter { it.second != null }.minByOrNull { it.second!! }
        return withPing?.first ?: scored.first().first
    }

    /**
     * Toggle from either home widget.
     * @return true if a connect/disconnect action was started.
     */
    fun toggleFromWidget(ctx: Context): Boolean {
        val app = ctx.applicationContext
        if (com.v2ray.ang.core.CoreServiceManager.isRunning()) {
            clearSmartPending(app)
            TikNetPrefs.setWidgetConnecting(app, false)
            WidgetProvider.refreshAll(app)
            CompactWidgetProvider.refreshAll(app)
            LauncherManager.stopService(app)
            return true
        }
        if (TikNetPrefs.isWidgetConnecting(app) || TikNetPrefs.isWidgetSmartPending(app)) {
            // Debounce / in-progress: ignore extra taps (user can open app to cancel).
            return false
        }

        return when (TikNetPrefs.getWidgetMode(app)) {
            TikNetPrefs.WIDGET_MODE_SMART -> beginSmartConnect(app)
            else -> beginDirectConnect(app)
        }
    }

    private fun beginDirectConnect(ctx: Context): Boolean {
        val guid = resolveServerGuid(ctx)
        if (!guid.isNullOrBlank()) MmkvManager.setSelectServer(guid)
        TikNetPrefs.setWidgetConnecting(ctx, true)
        WidgetProvider.refreshAll(ctx)
        CompactWidgetProvider.refreshAll(ctx)
        val started = LauncherManager.startServiceFromToggle(ctx)
        if (!started) {
            TikNetPrefs.setWidgetConnecting(ctx, false)
            WidgetProvider.refreshAll(ctx)
            CompactWidgetProvider.refreshAll(ctx)
        }
        return started
    }

    /** Smart connect: reuse fresh ping cache, else real-ping then connect. */
    fun beginSmartConnect(ctx: Context): Boolean {
        val app = ctx.applicationContext
        val guids = listServerGuids()
        if (guids.isEmpty()) {
            return beginDirectConnect(app)
        }
        if (TikNetPingCache.isFresh(app)) {
            val best = pickBestGuid(app)
            if (!best.isNullOrBlank()) {
                MmkvManager.setSelectServer(best)
                return beginDirectConnect(app)
            }
        }
        TikNetPrefs.setWidgetSmartPending(app, true)
        TikNetPrefs.setWidgetConnecting(app, true)
        WidgetProvider.refreshAll(app)
        CompactWidgetProvider.refreshAll(app)
        MessageHelper.sendMsg2TestService(
            app,
            TestServiceMessage(
                key = AppConfig.MSG_MEASURE_CONFIG_START,
                subscriptionId = TikNetPrefs.TIKNET_SUB_GUID,
                serverGuids = guids,
                onlyTcp = false,
            ),
        )
        scheduleSmartTimeout(app)
        return true
    }

    /** Called when CoreTestService finishes measuring (or timeout). */
    fun onSmartPingFinished(ctx: Context) {
        val app = ctx.applicationContext
        synchronized(smartFinishLock) {
            if (!TikNetPrefs.isWidgetSmartPending(app)) return
            TikNetPrefs.setWidgetSmartPending(app, false)
        }
        timeoutPosted.set(false)
        val best = pickBestGuid(app)
        if (best.isNullOrBlank()) {
            TikNetPrefs.setWidgetConnecting(app, false)
            WidgetProvider.refreshAll(app)
            CompactWidgetProvider.refreshAll(app)
            return
        }
        TikNetPingCache.rememberSuccessfulBatch(app)
        MmkvManager.setSelectServer(best)
        TikNetPrefs.setWidgetConnecting(app, true)
        WidgetProvider.refreshAll(app)
        CompactWidgetProvider.refreshAll(app)
        val started = LauncherManager.startServiceFromToggle(app)
        if (!started) {
            TikNetPrefs.setWidgetConnecting(app, false)
            WidgetProvider.refreshAll(app)
            CompactWidgetProvider.refreshAll(app)
        }
    }

    fun clearSmartPending(ctx: Context) {
        TikNetPrefs.setWidgetSmartPending(ctx.applicationContext, false)
        timeoutPosted.set(false)
    }

    private fun scheduleSmartTimeout(ctx: Context) {
        if (!timeoutPosted.compareAndSet(false, true)) return
        val app = ctx.applicationContext
        mainHandler.postDelayed({
            if (TikNetPrefs.isWidgetSmartPending(app)) {
                onSmartPingFinished(app)
            } else {
                timeoutPosted.set(false)
            }
        }, SMART_PING_TIMEOUT_MS)
    }
}
