package com.v2ray.ang.tiknet

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.v2ray.ang.handler.MmkvManager

/**
 * Smart-connect ping cache: reuse last real-ping results when still fresh on the
 * **same underlay network** (Wi‑Fi / cellular), ignoring our own VPN interface.
 *
 * Safety rails:
 * - No location permission required (no SSID/BSSID) → Wi‑Fi A→B may look the same;
 *   fail-over after a failed connect covers that case.
 * - VPN up/down must NOT invalidate the cache (compare underlay only).
 * - TTL prevents forever-stale rankings.
 */
object TikNetPingCache {
    const val TTL_MS: Long = 10L * 60L * 1000L // 10 minutes

    private const val KEY_AT = "tiknet_ping_cache_at"
    private const val KEY_NET = "tiknet_ping_cache_net"
    private const val KEY_FAILOVER = "tiknet_ping_failover_used"

    fun underlayFingerprint(ctx: Context): String {
        return runCatching {
            val cm = ctx.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val underlay = findUnderlayNetwork(cm) ?: return "none"
            val caps = cm.getNetworkCapabilities(underlay) ?: return "unknown"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    // Prefer a stable-ish wifi signal without location permission.
                    val extra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        runCatching {
                            val info = caps.transportInfo
                            info?.javaClass?.methods
                                ?.firstOrNull { it.name == "getBSSID" && it.parameterCount == 0 }
                                ?.invoke(info) as? String
                        }.getOrNull()?.takeIf { !it.isNullOrBlank() && it != "02:00:00:00:00:00" }
                    } else null
                    if (!extra.isNullOrBlank()) "wifi:$extra" else "wifi"
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cell"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "eth"
                else -> "other"
            }
        }.getOrDefault("unknown")
    }

    private fun findUnderlayNetwork(cm: ConnectivityManager): android.net.Network? {
        // Prefer non-VPN networks; activeNetwork is often the VPN while connected.
        val networks = cm.allNetworks
        for (n in networks) {
            val caps = cm.getNetworkCapabilities(n) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            ) {
                return n
            }
        }
        val active = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(active) ?: return active
        return if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) null else active
    }

    fun hasUsablePings(): Boolean {
        val guids = linkedSetOf<String>()
        guids.addAll(MmkvManager.decodeServerList(TikNetPrefs.TIKNET_SUB_GUID))
        if (guids.isEmpty()) {
            guids.addAll(MmkvManager.decodeAllServerList().take(40))
        }
        return guids.any { guid ->
            val ms = MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis ?: 0L
            ms > 0 && ms < 65_000
        }
    }

    /** True → smart connect may skip a full re-ping and use last results. */
    fun isFresh(ctx: Context): Boolean {
        val at = MmkvManager.decodeSettingsLong(KEY_AT, 0L)
        if (at <= 0L) return false
        if (System.currentTimeMillis() - at > TTL_MS) return false
        val net = MmkvManager.decodeSettingsString(KEY_NET).orEmpty()
        if (net.isBlank()) return false
        if (net != underlayFingerprint(ctx)) return false
        return hasUsablePings()
    }

    fun rememberSuccessfulBatch(ctx: Context) {
        if (!hasUsablePings()) return
        MmkvManager.encodeSettings(KEY_AT, System.currentTimeMillis())
        MmkvManager.encodeSettings(KEY_NET, underlayFingerprint(ctx))
        // Do NOT clear fail-over here — that would allow a re-ping/connect loop
        // if the post-fail-over start also fails. Budget resets on success/disconnect.
    }

    fun invalidate(ctx: Context) {
        MmkvManager.encodeSettings(KEY_AT, 0L)
        MmkvManager.encodeSettings(KEY_NET, "")
    }

    fun isFailoverUsed(ctx: Context): Boolean =
        MmkvManager.decodeSettingsBool(KEY_FAILOVER, false)

    fun markFailoverUsed(ctx: Context) {
        MmkvManager.encodeSettings(KEY_FAILOVER, true)
    }

    fun clearFailover(ctx: Context) {
        MmkvManager.encodeSettings(KEY_FAILOVER, false)
    }
}
