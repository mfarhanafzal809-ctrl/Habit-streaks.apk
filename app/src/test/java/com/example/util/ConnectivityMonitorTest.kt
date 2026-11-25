package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectivityMonitorTest {

    @Test
    fun networkStatus_properties_areConsistent() {
        val unknown = NetworkStatus.Unknown
        assertFalse(unknown.isOnline)
        assertFalse(unknown.isOffline)

        val offline = NetworkStatus.Offline
        assertFalse(offline.isOnline)
        assertTrue(offline.isOffline)

        val online = NetworkStatus.ValidatedOnline
        assertTrue(online.isOnline)
        assertFalse(online.isOffline)
    }

    @Test
    fun connectivityMonitor_singleton_returnsSameInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instance1 = ConnectivityMonitor.getInstance(context)
        val instance2 = ConnectivityMonitor.getInstance(context)

        assertNotNull(instance1)
        assertSame("ConnectivityMonitor must be a singleton to avoid duplicate monitor instances", instance1, instance2)
    }

    @Test
    fun connectivityMonitor_networkStatus_isObservableAndNonNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val monitor = ConnectivityMonitor.getInstance(context)
        val status = monitor.networkStatus.value

        assertNotNull(status)
        assertTrue(status == NetworkStatus.Unknown || status == NetworkStatus.Offline || status == NetworkStatus.ValidatedOnline)
    }

    @Test
    fun connectivityMonitor_stopMonitoring_isIdempotent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val monitor = ConnectivityMonitor.getInstance(context)

        // Multiple calls to stopMonitoring should never throw or crash
        monitor.stopMonitoring()
        monitor.stopMonitoring()
    }
}
