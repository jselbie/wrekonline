package com.selbie.wrek.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utility for detecting network connection type.
 * Used to implement AUTO bitrate selection (WiFi → 320kbps, Mobile → 128kbps).
 */
class NetworkMonitor(private val context: Context) {

    /**
     * Checks if the device is connected via WiFi or Ethernet (unmetered network).
     *
     * @return true if connected to WiFi/Ethernet, false if mobile data or no connection
     */
    fun isOnWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
