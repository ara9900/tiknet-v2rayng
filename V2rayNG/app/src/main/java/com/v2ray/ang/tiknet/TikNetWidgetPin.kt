package com.v2ray.ang.tiknet

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.v2ray.ang.receiver.WidgetProvider
import com.v2ray.ang.ui.tiknet.TikNetMainActivity

/**
 * Pins the TikNet home-screen widget via the system pin flow (API 26+).
 */
object TikNetWidgetPin {
    fun isSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return runCatching {
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
        }.getOrDefault(false)
    }

    /**
     * @return true if the pin request was handed to the launcher.
     */
    fun requestPin(context: Context): Boolean {
        if (!isSupported(context)) return false
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, WidgetProvider::class.java)
        val successIntent = Intent(context, TikNetMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_PINNED, true)
        }
        val successPending = PendingIntent.getActivity(
            context,
            REQUEST_PIN_WIDGET,
            successIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return runCatching {
            appWidgetManager.requestPinAppWidget(provider, null, successPending)
        }.getOrDefault(false)
    }

    const val EXTRA_WIDGET_PINNED = "tiknet_widget_pinned"
    private const val REQUEST_PIN_WIDGET = 44021
}
