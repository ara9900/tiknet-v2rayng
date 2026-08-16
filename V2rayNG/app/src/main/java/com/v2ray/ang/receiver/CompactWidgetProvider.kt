package com.v2ray.ang.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.tiknet.TikNetPrefs
import com.v2ray.ang.tiknet.TikNetWidgetConnect

/**
 * Compact 1×1 icon-only TikNet widget (no text).
 */
class CompactWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgets(context, appWidgetManager, appWidgetIds, resolvePhase(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            AppConfig.BROADCAST_ACTION_WIDGET_CLICK -> handleClick(context)
            AppConfig.BROADCAST_ACTION_ACTIVITY -> {
                when (intent.getIntExtra("key", 0)) {
                    AppConfig.MSG_STATE_RUNNING, AppConfig.MSG_STATE_START_SUCCESS -> {
                        TikNetPrefs.setWidgetConnecting(context, false)
                        refreshAll(context, Phase.Connected)
                    }
                    AppConfig.MSG_STATE_NOT_RUNNING,
                    AppConfig.MSG_STATE_START_FAILURE,
                    AppConfig.MSG_STATE_STOP_SUCCESS,
                    -> {
                        TikNetPrefs.setWidgetConnecting(context, false)
                        refreshAll(context, Phase.Disconnected)
                    }
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (ids != null && ids.isNotEmpty()) {
                    updateWidgets(context, AppWidgetManager.getInstance(context), ids, resolvePhase(context))
                }
            }
        }
    }

    private fun handleClick(context: Context) {
        val phase = resolvePhase(context)
        if (phase == Phase.Connecting) return
        if (phase == Phase.Connected || CoreServiceManager.isRunning()) {
            TikNetPrefs.setWidgetConnecting(context, false)
            refreshAll(context, Phase.Disconnected)
            WidgetProvider.refreshAll(context)
            LauncherManager.stopService(context)
            return
        }
        val guid = TikNetWidgetConnect.resolveServerGuid(context)
        if (!guid.isNullOrBlank()) MmkvManager.setSelectServer(guid)
        TikNetPrefs.setWidgetConnecting(context, true)
        refreshAll(context, Phase.Connecting)
        WidgetProvider.refreshAll(context)
        val started = LauncherManager.startServiceFromToggle(context)
        if (!started) {
            TikNetPrefs.setWidgetConnecting(context, false)
            refreshAll(context, Phase.Disconnected)
            WidgetProvider.refreshAll(context)
        }
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        phase: Phase,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_compact)
        val click = Intent(context, CompactWidgetProvider::class.java).apply {
            action = AppConfig.BROADCAST_ACTION_WIDGET_CLICK
        }
        views.setOnClickPendingIntent(
            R.id.layout_switch,
            PendingIntent.getBroadcast(
                context,
                R.id.layout_switch + 17,
                click,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        when (phase) {
            Phase.Connected -> {
                views.setInt(R.id.layout_switch, "setBackgroundResource", R.drawable.tiknet_widget_bg_online)
                views.setInt(R.id.layout_background, "setBackgroundResource", R.drawable.tiknet_widget_icon_ring_online)
                views.setViewVisibility(R.id.image_switch, View.VISIBLE)
                views.setViewVisibility(R.id.pb_widget_connecting, View.GONE)
            }
            Phase.Connecting -> {
                views.setInt(R.id.layout_switch, "setBackgroundResource", R.drawable.tiknet_widget_bg_connecting)
                views.setInt(R.id.layout_background, "setBackgroundResource", R.drawable.tiknet_widget_icon_ring_connecting)
                views.setViewVisibility(R.id.image_switch, View.INVISIBLE)
                views.setViewVisibility(R.id.pb_widget_connecting, View.VISIBLE)
            }
            Phase.Disconnected -> {
                views.setInt(R.id.layout_switch, "setBackgroundResource", R.drawable.tiknet_widget_bg_offline)
                views.setInt(R.id.layout_background, "setBackgroundResource", R.drawable.tiknet_widget_icon_ring_offline)
                views.setViewVisibility(R.id.image_switch, View.VISIBLE)
                views.setViewVisibility(R.id.pb_widget_connecting, View.GONE)
            }
        }
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, views)
    }

    private fun resolvePhase(context: Context): Phase = when {
        CoreServiceManager.isRunning() -> Phase.Connected
        TikNetPrefs.isWidgetConnecting(context) -> Phase.Connecting
        else -> Phase.Disconnected
    }

    private enum class Phase { Disconnected, Connecting, Connected }

    companion object {
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            val ids = mgr.getAppWidgetIds(ComponentName(context, CompactWidgetProvider::class.java))
            if (ids.isEmpty()) return
            CompactWidgetProvider().updateWidgets(context, mgr, ids, CompactWidgetProvider().resolvePhase(context))
        }

        private fun refreshAll(context: Context, phase: Phase) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            val ids = mgr.getAppWidgetIds(ComponentName(context, CompactWidgetProvider::class.java))
            if (ids.isEmpty()) return
            CompactWidgetProvider().updateWidgets(context, mgr, ids, phase)
        }

        fun requestUpdate(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            val ids = mgr.getAppWidgetIds(ComponentName(context, CompactWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, CompactWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                },
            )
        }
    }
}
