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
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.tiknet.TikNetPrefs
import com.v2ray.ang.tiknet.TikNetWidgetConnect

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgets(context, appWidgetManager, appWidgetIds, resolvePhase(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            AppConfig.BROADCAST_ACTION_WIDGET_CLICK -> {
                TikNetWidgetConnect.toggleFromWidget(context)
            }
            AppConfig.BROADCAST_ACTION_ACTIVITY -> {
                when (intent.getIntExtra("key", 0)) {
                    AppConfig.MSG_STATE_RUNNING, AppConfig.MSG_STATE_START_SUCCESS -> {
                        TikNetWidgetConnect.clearSmartPending(context)
                        TikNetPrefs.setWidgetConnecting(context, false)
                        refreshAll(context, Phase.Connected)
                    }
                    AppConfig.MSG_STATE_NOT_RUNNING,
                    AppConfig.MSG_STATE_START_FAILURE,
                    AppConfig.MSG_STATE_STOP_SUCCESS,
                    -> {
                        TikNetWidgetConnect.clearSmartPending(context)
                        TikNetPrefs.setWidgetConnecting(context, false)
                        refreshAll(context, Phase.Disconnected)
                    }
                    AppConfig.MSG_MEASURE_CONFIG_FINISH -> {
                        if (TikNetPrefs.isWidgetSmartPending(context)) {
                            TikNetWidgetConnect.onSmartPingFinished(context)
                        }
                    }
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (ids != null && ids.isNotEmpty()) {
                    val mgr = AppWidgetManager.getInstance(context)
                    updateWidgets(context, mgr, ids, resolvePhase(context))
                }
            }
        }
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        phase: Phase,
    ) {
        val remoteViews = buildViews(context, phase)
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private fun buildViews(context: Context, phase: Phase): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_switch)
        val click = Intent(context, WidgetProvider::class.java).apply {
            action = AppConfig.BROADCAST_ACTION_WIDGET_CLICK
        }
        val pending = PendingIntent.getBroadcast(
            context,
            R.id.layout_switch,
            click,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.layout_switch, pending)

        val mode = TikNetPrefs.getWidgetMode(context)
        val modeLabel = when (mode) {
            TikNetPrefs.WIDGET_MODE_SMART -> context.getString(R.string.tiknet_widget_mode_smart)
            TikNetPrefs.WIDGET_MODE_FIXED -> context.getString(R.string.tiknet_widget_mode_fixed)
            else -> context.getString(R.string.tiknet_widget_mode_current)
        }

        val serverName = when (phase) {
            Phase.Connected -> {
                CoreServiceManager.getRunningServerName().ifBlank {
                    resolveDisplayServerName(context)
                }
            }
            Phase.Connecting -> resolveDisplayServerName(context)
            Phase.Disconnected -> ""
        }

        views.setTextViewText(R.id.tv_widget_title, context.getString(R.string.app_name))

        when (phase) {
            Phase.Connected -> {
                views.setInt(R.id.layout_switch, "setBackgroundResource", R.drawable.tiknet_widget_bg_online)
                views.setInt(R.id.layout_background, "setBackgroundResource", R.drawable.tiknet_widget_icon_ring_online)
                views.setInt(R.id.image_switch, "setImageResource", R.drawable.ic_stat_tiknet)
                views.setViewVisibility(R.id.image_switch, View.VISIBLE)
                views.setViewVisibility(R.id.pb_widget_connecting, View.GONE)
                views.setTextViewText(R.id.tv_widget_status, context.getString(R.string.tiknet_widget_status_on))
                views.setTextColor(R.id.tv_widget_status, 0xFF22C55E.toInt())
                val detail = if (serverName.isNotBlank()) {
                    context.getString(R.string.tiknet_widget_detail_connected, modeLabel, serverName)
                } else {
                    modeLabel
                }
                views.setTextViewText(R.id.tv_widget_mode, detail)
            }
            Phase.Connecting -> {
                views.setInt(R.id.layout_switch, "setBackgroundResource", R.drawable.tiknet_widget_bg_connecting)
                views.setInt(R.id.layout_background, "setBackgroundResource", R.drawable.tiknet_widget_icon_ring_connecting)
                views.setViewVisibility(R.id.image_switch, View.INVISIBLE)
                views.setViewVisibility(R.id.pb_widget_connecting, View.VISIBLE)
                views.setTextViewText(R.id.tv_widget_status, context.getString(R.string.tiknet_widget_status_connecting))
                views.setTextColor(R.id.tv_widget_status, 0xFFF59E0B.toInt())
                val detail = if (mode == TikNetPrefs.WIDGET_MODE_SMART) {
                    context.getString(R.string.tiknet_widget_detail_smart_connecting)
                } else if (serverName.isNotBlank()) {
                    context.getString(R.string.tiknet_widget_detail_connecting, modeLabel, serverName)
                } else {
                    modeLabel
                }
                views.setTextViewText(R.id.tv_widget_mode, detail)
            }
            Phase.Disconnected -> {
                views.setInt(R.id.layout_switch, "setBackgroundResource", R.drawable.tiknet_widget_bg_offline)
                views.setInt(R.id.layout_background, "setBackgroundResource", R.drawable.tiknet_widget_icon_ring_offline)
                views.setInt(R.id.image_switch, "setImageResource", R.drawable.ic_stat_tiknet)
                views.setViewVisibility(R.id.image_switch, View.VISIBLE)
                views.setViewVisibility(R.id.pb_widget_connecting, View.GONE)
                views.setTextViewText(R.id.tv_widget_status, context.getString(R.string.tiknet_widget_status_off))
                views.setTextColor(R.id.tv_widget_status, 0xFFA5B4FC.toInt())
                views.setTextViewText(
                    R.id.tv_widget_mode,
                    context.getString(R.string.tiknet_widget_detail_ready, modeLabel),
                )
            }
        }
        return views
    }

    private fun resolveDisplayServerName(context: Context): String {
        val guid = TikNetWidgetConnect.resolveServerGuid(context) ?: return ""
        return MmkvManager.decodeServerConfig(guid)?.remarks.orEmpty()
    }

    private fun resolvePhase(context: Context): Phase {
        return when {
            CoreServiceManager.isRunning() -> Phase.Connected
            TikNetPrefs.isWidgetConnecting(context) -> Phase.Connecting
            else -> Phase.Disconnected
        }
    }

    private enum class Phase {
        Disconnected,
        Connecting,
        Connected,
    }

    companion object {
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            val ids = mgr.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
            if (ids.isEmpty()) return
            val provider = WidgetProvider()
            provider.updateWidgets(context, mgr, ids, provider.resolvePhase(context))
        }

        private fun refreshAll(context: Context, phase: Phase) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            val ids = mgr.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
            if (ids.isEmpty()) return
            WidgetProvider().updateWidgets(context, mgr, ids, phase)
        }

        /** Ask home launcher / widget host to redraw after settings change (cross-process). */
        fun requestUpdate(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            val ids = mgr.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, WidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
            CompactWidgetProvider.requestUpdate(context)
        }
    }
}
