package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

sealed class NetworkStatus {
    object Unknown : NetworkStatus()
    object Offline : NetworkStatus()
    object ValidatedOnline : NetworkStatus()

    val isOnline: Boolean get() = this is ValidatedOnline
    val isOffline: Boolean get() = this is Offline
}

/**
 * Lifecycle-safe, application-scoped ConnectivityMonitor observing
 * NET_CAPABILITY_INTERNET and NET_CAPABILITY_VALIDATED.
 * Never performs blocking network queries.
 */
class ConnectivityMonitor private constructor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Unknown)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val isRegistered = AtomicBoolean(false)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            checkCurrentNetworkState()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            _networkStatus.value = if (hasInternet) {
                NetworkStatus.ValidatedOnline
            } else {
                NetworkStatus.Offline
            }
        }

        override fun onLost(network: Network) {
            checkCurrentNetworkState()
        }

        override fun onUnavailable() {
            _networkStatus.value = NetworkStatus.Offline
        }
    }

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        val cm = connectivityManager ?: run {
            _networkStatus.value = NetworkStatus.Offline
            return
        }

        if (isRegistered.compareAndSet(false, true)) {
            try {
                // Initialize current state immediately without blocking
                checkCurrentNetworkState()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cm.registerDefaultNetworkCallback(networkCallback)
                } else {
                    val request = NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                    cm.registerNetworkCallback(request, networkCallback)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register network callback", e)
                _networkStatus.value = NetworkStatus.Offline
            }
        }
    }

    private fun checkCurrentNetworkState() {
        val cm = connectivityManager ?: run {
            _networkStatus.value = NetworkStatus.Offline
            return
        }
        val activeNetwork = cm.activeNetwork
        if (activeNetwork == null) {
            _networkStatus.value = NetworkStatus.Offline
            return
        }
        val caps = cm.getNetworkCapabilities(activeNetwork)
        if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            _networkStatus.value = NetworkStatus.ValidatedOnline
        } else {
            _networkStatus.value = NetworkStatus.Offline
        }
    }

    fun stopMonitoring() {
        if (isRegistered.compareAndSet(true, false)) {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister network callback", e)
            }
        }
    }

    companion object {
        private const val TAG = "ConnectivityMonitor"

        @Volatile
        private var INSTANCE: ConnectivityMonitor? = null

        fun getInstance(context: Context): ConnectivityMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConnectivityMonitor(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        fun setNetworkStatusForTesting(context: Context, status: NetworkStatus) {
            val instance = getInstance(context)
            instance._networkStatus.value = status
        }

        fun resetForTesting() {
            INSTANCE?.stopMonitoring()
            INSTANCE = null
        }
    }
}
