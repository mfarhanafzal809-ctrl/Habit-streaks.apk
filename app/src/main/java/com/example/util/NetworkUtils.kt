package com.example.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

object NetworkUtils {

    const val OFFLINE_MESSAGE = "You are offline, please turn on Wi-Fi or mobile data."

    /**
     * Checks if the device has a valid internet connection via Wi-Fi or Mobile Data.
     * Prevents false negatives on emulators or transitioning network states.
     */
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        return try {
            val monitor = ConnectivityMonitor.getInstance(context)
            if (monitor.networkStatus.value is NetworkStatus.ValidatedOnline) {
                return true
            }
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true // Fail open to allow SDK network attempts
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    return true
                }
            }
            false
        } catch (_: Throwable) {
            true // Fail open so network requests can be attempted
        }
    }

    /**
     * Shows a toast notification informing the user that they are offline.
     */
    fun showOfflineToast(context: Context?) {
        if (context == null) return
        try {
            Toast.makeText(context, OFFLINE_MESSAGE, Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) {}
    }
}
