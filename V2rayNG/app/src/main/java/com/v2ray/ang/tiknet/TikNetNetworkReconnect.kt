package com.v2ray.ang.tiknet

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper

/**
 * Fires when a non-VPN underlay network with internet becomes available.
 * Initial callback after register is ignored so we only react to changes.
 */
class TikNetNetworkReconnect(
    context: Context,
    private val onUnderlayAvailable: () -> Unit,
    private val onUnderlayLost: () -> Unit = {},
) {
    private val app = context.applicationContext
    private val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val main = Handler(Looper.getMainLooper())
    private var registered = false
    private var skipNext = true

    private val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        .build()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (skipNext) {
                skipNext = false
                return
            }
            main.removeCallbacks(fire)
            main.postDelayed(fire, DEBOUNCE_MS)
        }

        override fun onLost(network: Network) {
            main.removeCallbacks(fire)
            main.post { onUnderlayLost() }
        }
    }

    private val fire = Runnable { onUnderlayAvailable() }

    fun register() {
        if (registered) return
        try {
            skipNext = true
            connectivity.registerNetworkCallback(request, callback)
            registered = true
        } catch (_: Exception) {
            registered = false
        }
    }

    fun unregister() {
        main.removeCallbacks(fire)
        if (!registered) return
        registered = false
        try {
            connectivity.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 2000L
    }
}
