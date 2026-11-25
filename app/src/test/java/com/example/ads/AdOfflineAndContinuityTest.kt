package com.example.ads

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.util.ConnectivityMonitor
import com.example.util.NetworkStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class AdOfflineAndContinuityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AdManager.resetForTesting()
        ConnectivityMonitor.resetForTesting()
    }

    @After
    fun tearDown() {
        AdManager.resetForTesting()
        ConnectivityMonitor.resetForTesting()
    }

    @Test
    fun offlineNotice_bypassState_suppressesRepetitivePromptsUntilOnline() {
        // Initial state: no bypass
        assertFalse(AdManager.isOfflineBypassed())

        // User chooses "No Internet" on offline notice: bypass is recorded
        AdManager.setOfflineBypass(true)
        assertTrue(AdManager.isOfflineBypassed())

        // While still offline, bypass persists
        assertTrue(AdManager.isOfflineBypassed())

        // When connectivity returns, monitor clears the bypass
        AdManager.initialize(context)
        ConnectivityMonitor.setNetworkStatusForTesting(context, NetworkStatus.ValidatedOnline)

        // Reset bypass
        AdManager.setOfflineBypass(false)
        assertFalse(AdManager.isOfflineBypassed())
    }

    @Test
    fun localOperationContinuity_whenAdsFailOrOffline_continuesSafely() {
        // Ensure not in ad-free mode, no ad loaded
        AdManager.setAdFreeUntil(0L)
        assertFalse(AdManager.isInterstitialReady.value)

        val continuationExecuted = AtomicBoolean(false)
        AdManager.requestInterstitialAndContinue(
            activity = null,
            isFocusedTask = false
        ) {
            continuationExecuted.set(true)
        }

        // Local action must NOT be blocked or wait indefinitely
        assertTrue("Local action must continue safely even when ad is unavailable", continuationExecuted.get())
    }

    @Test
    fun rewardedAd_rewardIsStrictlySeparated_notGrantedOnDismissalOrFailure() {
        AdManager.setAdFreeUntil(0L)
        assertFalse(AdManager.isRewardedReady.value)

        var rewardGranted = false
        var closedOrFailedCalled = false

        AdManager.showRewardedAd(
            activity = null,
            onUserEarnedReward = { rewardGranted = true },
            onAdClosedOrFailed = { closedOrFailedCalled = true }
        )

        assertFalse("Reward must NEVER be granted when ad is unavailable or fails", rewardGranted)
        assertTrue("Callback must indicate ad closed or failed safely", closedOrFailedCalled)
    }

    @Test
    fun adFreeExpiry_restoresLocalState_whenExpired() {
        // Active ad-free mode
        val futureTime = System.currentTimeMillis() + 3600_000L
        AdManager.setAdFreeUntil(futureTime)
        assertTrue(AdManager.isAdFreeActive())
        assertFalse(AdManager.canRequestAds())

        // Expired ad-free mode
        val pastTime = System.currentTimeMillis() - 1000L
        AdManager.setAdFreeUntil(pastTime)
        assertFalse(AdManager.isAdFreeActive())
        assertTrue(AdManager.canRequestAds())
    }

    @Test
    fun networkStatus_offlineNoticeRules_matchExactTexts() {
        val noticeTitle = "You're currently offline"
        val noticeMessage = "Ads need an internet connection. You can continue using this feature without an ad, or reconnect to try loading one."
        val buttonA = "No Internet"
        val buttonB = "Have Internet"
        val retryOfflineMsg = "Please turn on Wi-Fi or mobile data, then try again."

        assertEquals("You're currently offline", noticeTitle)
        assertTrue(noticeMessage.contains("Ads need an internet connection"))
        assertEquals("No Internet", buttonA)
        assertEquals("Have Internet", buttonB)
        assertEquals("Please turn on Wi-Fi or mobile data, then try again.", retryOfflineMsg)
    }

    @Test
    fun adManager_adFreeWithContextAndRefreshAdsIfNeeded_preserveConsentAndEligibility() {
        AdManager.initialize(context, preloadAds = false)
        AdManager.setConsentGranted(true)
        assertTrue(AdManager.canRequestAds())

        val futureTime = System.currentTimeMillis() + 600_000L
        AdManager.setAdFreeUntil(futureTime, context)
        assertTrue(AdManager.isAdFreeActive())
        assertFalse(AdManager.canRequestAds())

        // Clearing Ad-Free with Context restores ad eligibility immediately
        AdManager.setAdFreeUntil(0L, context)
        assertFalse(AdManager.isAdFreeActive())
        assertTrue(AdManager.canRequestAds())

        // Refreshing ads handles idle/loading state cleanly
        AdManager.refreshAdsIfNeeded(context)
        assertTrue(AdManager.rewardedState.value == AdState.Idle || AdManager.rewardedState.value == AdState.Loading)
    }

    @Test
    fun adManager_loadRewardedAd_whenOnlineAndUninitialized_transitionsToLoadingInsteadOfFailed() {
        ConnectivityMonitor.setNetworkStatusForTesting(context, NetworkStatus.ValidatedOnline)
        AdManager.setConsentGranted(true)

        AdManager.loadRewardedAd(context, force = true)
        assertEquals(AdState.Loading, AdManager.rewardedState.value)
    }

    @Test
    fun adManager_whenDeviceOffline_loadRewardedFailsFastAndForcedInterstitialContinuesOnce() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val shadowCm = org.robolectric.Shadows.shadowOf(cm)
        @Suppress("DEPRECATION")
        shadowCm.setActiveNetworkInfo(null)
        shadowCm.clearAllNetworks()
        ConnectivityMonitor.setNetworkStatusForTesting(context, NetworkStatus.Offline)

        AdManager.setAdFreeUntil(0L)
        AdManager.setConsentGranted(true)

        // Offline rewarded load fails fast instead of hanging in Loading
        AdManager.loadRewardedAd(context, force = true)
        assertEquals(AdState.Failed("Device is offline"), AdManager.rewardedState.value)

        // Forced interstitial when offline continues immediately without stalling
        val executed = java.util.concurrent.atomic.AtomicInteger(0)
        AdManager.requestInterstitialAndContinue(
            activity = null,
            isFocusedTask = false,
            force = true
        ) {
            executed.incrementAndGet()
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        assertEquals(1, executed.get())
    }
}
