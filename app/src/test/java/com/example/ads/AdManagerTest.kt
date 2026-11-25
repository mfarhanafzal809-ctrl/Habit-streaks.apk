package com.example.ads

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AdManagerTest {

    @Before
    fun setUp() {
        AdManager.resetForTesting()
    }

    @After
    fun tearDown() {
        AdManager.resetForTesting()
    }

    @Test
    fun adManager_initialStates_areExplicitAndIdle() {
        assertEquals(AdInitState.NotInitialized, AdManager.initState.value)
        assertEquals(AdState.Idle, AdManager.interstitialState.value)
        assertEquals(AdState.Idle, AdManager.rewardedState.value)
        assertEquals(AdState.Idle, AdManager.bannerState.value)

        assertFalse(AdManager.isInterstitialReady.value)
        assertFalse(AdManager.isRewardedReady.value)
        assertFalse(AdManager.isBannerAdLoaded.value)
    }

    @Test
    fun adManager_dispose_transitionsToDisposedState() {
        AdManager.dispose()
        assertEquals(AdState.Disposed, AdManager.interstitialState.value)
        assertEquals(AdState.Disposed, AdManager.rewardedState.value)
        assertTrue(AdManager.interstitialState.value.isDisposed)
        assertTrue(AdManager.rewardedState.value.isDisposed)
        assertFalse(AdManager.isInterstitialReady.value)
        assertFalse(AdManager.isRewardedReady.value)
    }

    @Test
    fun adManager_adFreeMode_blocksAdRequestsAndInterstitials() {
        val future = System.currentTimeMillis() + 3600_000L
        AdManager.setAdFreeUntil(future)

        assertTrue(AdManager.isAdFreeActive())
        assertFalse(AdManager.canRequestAds())
        assertFalse(AdManager.canShowInterstitial(isFocusedTask = false))
        assertTrue(AdManager.getAdFreeTimeRemainingMillis() > 0)

        // Safe continuation contract executes onContinue immediately without blocking
        val executed = AtomicBoolean(false)
        AdManager.requestInterstitialAndContinue(null, isFocusedTask = false) {
            executed.set(true)
        }
        assertTrue("Continuation must be invoked safely when in ad-free mode", executed.get())
    }

    @Test
    fun adManager_userConsent_revocationBlocksAdRequests() {
        AdManager.setConsentGranted(false)
        assertFalse(AdManager.canRequestAds())
        assertFalse(AdManager.canShowInterstitial())

        val executed = AtomicBoolean(false)
        AdManager.requestInterstitialAndContinue(null) {
            executed.set(true)
        }
        assertTrue(executed.get())
    }

    @Test
    fun adManager_focusedTask_preventsInterstitialInterruption() {
        // Even if other eligibility requirements might pass, a focused task (active form, audio extraction) must never be interrupted
        AdManager.policy = AdEligibilityPolicy(
            minSessionAgeSeconds = 0L,
            minSecondsBetweenInterstitials = 0L,
            maxSessionInterstitials = 10,
            minMeaningfulActions = 0
        )
        assertFalse("Interstitial must never interrupt an active focused task", AdManager.canShowInterstitial(isFocusedTask = true))
        assertTrue(AdManager.canShowInterstitial(isFocusedTask = false))
    }

    @Test
    fun adManager_policy_enforcesConservativeDefaultsAndTunability() {
        // Default policy
        assertEquals(10L, AdManager.policy.minSessionAgeSeconds)
        assertEquals(30L, AdManager.policy.minSecondsBetweenInterstitials)
        assertEquals(5, AdManager.policy.maxSessionInterstitials)
        assertEquals(3, AdManager.policy.minMeaningfulActions)

        // Tune policy
        AdManager.policy = AdEligibilityPolicy(
            minSessionAgeSeconds = 0L,
            minSecondsBetweenInterstitials = 5L,
            maxSessionInterstitials = 2,
            minMeaningfulActions = 1
        )
        assertEquals(0L, AdManager.policy.minSessionAgeSeconds)
        assertEquals(5L, AdManager.policy.minSecondsBetweenInterstitials)
        assertEquals(2, AdManager.policy.maxSessionInterstitials)
        assertEquals(1, AdManager.policy.minMeaningfulActions)
    }

    @Test
    fun adManager_meaningfulAction_incrementsCounterAndRegulatesEligibility() {
        AdManager.policy = AdEligibilityPolicy(
            minSessionAgeSeconds = 0L,
            minSecondsBetweenInterstitials = 0L,
            maxSessionInterstitials = 10,
            minMeaningfulActions = 3
        )

        assertFalse(AdManager.canShowInterstitial())
        AdManager.recordMeaningfulAction() // count = 1
        assertFalse(AdManager.canShowInterstitial())
        AdManager.recordMeaningfulAction() // count = 2
        assertFalse(AdManager.canShowInterstitial())
        AdManager.recordMeaningfulAction() // count = 3
        assertTrue(AdManager.canShowInterstitial())
    }

    @Test
    fun adManager_requestInterstitialAndContinue_executesExactlyOnce() {
        val executionCount = AtomicInteger(0)
        AdManager.requestInterstitialAndContinue(null, isFocusedTask = false) {
            executionCount.incrementAndGet()
        }
        assertEquals(1, executionCount.get())
    }

    @Test
    fun adManager_rewardedAndInterstitial_remainStrictlyDistinct() {
        // A normal interstitial call must never invoke rewarded entitlement
        var rewardEarned = false
        AdManager.requestInterstitialAndContinue(null) {
            // Dismissal / continue of interstitial
        }
        assertFalse("Interstitial completion must NEVER grant a rewarded entitlement", rewardEarned)

        // Rewarded ad call with null/offline activity safely calls onAdClosedOrFailed, NEVER onUserEarnedReward
        var closedOrFailedCalled = false
        AdManager.showRewardedAd(
            activity = null,
            onUserEarnedReward = { rewardEarned = true },
            onAdClosedOrFailed = { closedOrFailedCalled = true }
        )
        assertFalse("Reward must NOT be granted when ad fails or is unavailable", rewardEarned)
        assertTrue("Closed or failed callback must be invoked safely", closedOrFailedCalled)
    }

    @Test
    fun adConfig_current_usesOfficialAdMobSampleIdsInDebug() {
        val config = AdConfig.current
        assertNotNull(config)
        assertEquals(AdConfig.TEST_APP_ID, config.appId)
        assertEquals(AdConfig.TEST_BANNER_AD_UNIT_ID, config.bannerAdUnitId)
        assertEquals(AdConfig.TEST_INTERSTITIAL_AD_UNIT_ID, config.interstitialAdUnitId)
        assertEquals(AdConfig.TEST_INTERSTITIAL_VIDEO_AD_UNIT_ID, config.interstitialVideoAdUnitId)
        assertEquals(AdConfig.TEST_REWARDED_AD_UNIT_ID, config.rewardedAdUnitId)
        assertEquals(AdConfig.TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID, config.rewardedInterstitialAdUnitId)
        assertEquals(AdConfig.TEST_ADAPTIVE_BANNER_AD_UNIT_ID, config.adaptiveBannerAdUnitId)
    }

    @Test
    fun adManager_interstitialVideo_statesAndDisposal() {
        assertEquals(AdState.Idle, AdManager.interstitialVideoState.value)
        assertFalse(AdManager.isInterstitialVideoReady.value)

        // Try show video ad with null activity calls onAdFailed safely
        var failedCalled = false
        AdManager.tryShowInterstitialVideo(
            activity = null,
            onAdDismissed = {},
            onAdFailed = { failedCalled = true }
        )
        assertTrue(failedCalled)

        // Disposal transitions video ad state to Disposed
        AdManager.dispose()
        assertEquals(AdState.Disposed, AdManager.interstitialVideoState.value)
        assertFalse(AdManager.isInterstitialVideoReady.value)
    }
}
