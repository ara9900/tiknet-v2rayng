package com.v2ray.ang.tiknet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.v2ray.ang.R
import com.v2ray.ang.ui.tiknet.TikNetMainActivity
import java.util.Calendar

enum class TikNetEntitlementKind { Expired, ExpiringSoon, LowTraffic }

data class TikNetEntitlementAlert(
    val kind: TikNetEntitlementKind,
    val message: String,
    val severe: Boolean,
)

/**
 * Local entitlement warnings (expiry / traffic) shown in-app and optionally as a system notification.
 */
object TikNetEntitlementAlerts {
    private const val CHANNEL_ID = "tiknet_account_alerts"
    private const val NOTIF_ID = 43140
    private const val EXPIRY_DAYS = 3
    private const val TRAFFIC_RATIO = 0.80

    fun evaluate(user: TikNetUserInfo?): TikNetEntitlementAlert? {
        if (user == null) return null
        if (user.isExpired == true) {
            return TikNetEntitlementAlert(
                kind = TikNetEntitlementKind.Expired,
                message = "اشتراک شما منقضی شده است. برای ادامه، تمدید کنید.",
                severe = true,
            )
        }
        if (user.hasSubscription) {
            val days = user.daysRemaining
            if (days != null && days in 0..EXPIRY_DAYS) {
                val dayLabel = TikNetJalali.toPersianDigits(days.toString())
                return TikNetEntitlementAlert(
                    kind = TikNetEntitlementKind.ExpiringSoon,
                    message = when (days) {
                        0 -> "اشتراک امروز منقضی می‌شود. همین حالا تمدید کنید."
                        1 -> "فقط ۱ روز تا انقضای اشتراک باقی مانده است."
                        else -> "فقط $dayLabel روز تا انقضای اشتراک باقی مانده است."
                    },
                    severe = days <= 1,
                )
            }
            val used = user.trafficUsedBytes
            val limit = user.trafficLimitBytes
            if (used != null && limit != null && limit > 0) {
                val ratio = used.toDouble() / limit.toDouble()
                if (ratio >= TRAFFIC_RATIO) {
                    val pct = TikNetJalali.toPersianDigits(((ratio * 100).toInt().coerceAtMost(100)).toString())
                    return TikNetEntitlementAlert(
                        kind = TikNetEntitlementKind.LowTraffic,
                        message = "حدود $pct٪ از حجم اشتراک مصرف شده است.",
                        severe = ratio >= 0.95,
                    )
                }
            }
        }
        return null
    }

    /** At most one system notification per calendar day per alert kind. */
    fun maybeNotify(ctx: Context, alert: TikNetEntitlementAlert?) {
        if (alert == null) return
        if (alert.kind == TikNetEntitlementKind.Expired && !alert.severe) return
        val dayKey = todayKey()
        val last = TikNetPrefs.getEntitlementNotifDay(ctx, alert.kind.name)
        if (last == dayKey) return
        ensureChannel(ctx)
        val intent = Intent(ctx, TikNetMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx,
            NOTIF_ID + alert.kind.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = when (alert.kind) {
            TikNetEntitlementKind.Expired -> "اشتراک منقضی"
            TikNetEntitlementKind.ExpiringSoon -> "انقضای نزدیک"
            TikNetEntitlementKind.LowTraffic -> "حجم رو به اتمام"
        }
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tiknet)
            .setContentTitle(title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching {
            NotificationManagerCompat.from(ctx).notify(NOTIF_ID + alert.kind.ordinal, notif)
            TikNetPrefs.saveEntitlementNotifDay(ctx, alert.kind.name, dayKey)
        }
    }

    private fun todayKey(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "هشدار حساب تیک‌نت",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "انقضا و اتمام حجم اشتراک"
            },
        )
    }
}
