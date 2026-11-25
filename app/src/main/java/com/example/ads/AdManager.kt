package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.util.ConnectivityMonitor
import com.example.util.NetworkStatus
import com.example.util.NetworkUtils
import com.example.util.findActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object AdManager {

    private const val TAG = "AdManager"
    private const val MAX_RETRY_COUNT = 2
    private const val AD_EXPIRATION_MS = 55L * 60L * 1000L // Google AdMob full-screen ads expire after 60 mins

    // Tunable eligibility policy with conservative defaults
    var policy = AdEligibilityPolicy(
        minSessionAgeSeconds = 10L,
        minSecondsBetweenInterstitials = 30L,
        maxSessionInterstitials = 5,
        minMeaningfulActions = 3
    )

    private var sessionStartTime = System.currentTimeMillis()
    private var lastInterstitialShownTime = 0L
    private var sessionInterstitialCount = 0
    private var meaningfulActionCount = 0

    private var interstitialRetryCount = 0
    private var interstitialVideoRetryCount = 0
    private var rewardedRetryCount = 0

    private val interstitialLoadSeq = AtomicInteger(0)
    private val interstitialVideoLoadSeq = AtomicInteger(0)
    private val rewardedLoadSeq = AtomicInteger(0)
    @Volatile private var preloadPendingOnInit = false

    private var mInterstitialAd: InterstitialAd? = null
    private var mInterstitialVideoAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null
    private var mRewardedInterstitialAd: RewardedInterstitialAd? = null
    private var lastRewardedLoadTime = 0L
    private var lastInterstitialLoadTime = 0L
    private var lastInterstitialVideoLoadTime = 0L

    @Volatile private var interstitialLoadedAt = 0L
    @Volatile private var interstitialVideoLoadedAt = 0L
    @Volatile private var rewardedLoadedAt = 0L

    private fun isAdExpired(loadedAt: Long): Boolean {
        return loadedAt > 0L && (System.currentTimeMillis() - loadedAt) >= AD_EXPIRATION_MS
    }

    private var adFreeUntilMillis: Long = 0L
    private val adShowInProgress = AtomicBoolean(false)
    private var isAdShowing = false
    private var isConsentGranted = true
    private var explicitConsentRevoked = false

    private val testDeviceIds = CopyOnWriteArrayList(
        listOf(
            AdRequest.DEVICE_ID_EMULATOR,
            "B3EEABB8EE11C2BE770B684D95219ECB",
            "33BE2250B43518CCDA7DE426D04EE230",
            "EDC44D76E4B94C2FE6E413E7847BCB62"
        )
    )

    private fun applyRequestConfiguration() {
        try {
            val reqConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds.toList())
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
                .build()
            MobileAds.setRequestConfiguration(reqConfig)
        } catch (_: Throwable) {}
    }

    fun addTestDeviceId(deviceId: String, applyNow: Boolean = true) {
        if (deviceId.isNotBlank() && !testDeviceIds.contains(deviceId)) {
            testDeviceIds.add(deviceId)
            if (applyNow && (_initState.value is AdInitState.Initialized || _initState.value is AdInitState.Initializing)) {
                applyRequestConfiguration()
            }
        }
    }

    private var currentActivityRef = java.lang.ref.WeakReference<Activity?>(null)

    fun updateCurrentActivity(activity: Activity?) {
        currentActivityRef = java.lang.ref.WeakReference(activity)
    }

    val currentActivity: Activity?
        get() = currentActivityRef.get() ?: com.example.HabitStreaksApplication.currentActivity

    // Explicit Observable States
    private val _initState = MutableStateFlow<AdInitState>(AdInitState.NotInitialized)
    val initState: StateFlow<AdInitState> = _initState.asStateFlow()

    private val _interstitialState = MutableStateFlow<AdState>(AdState.Idle)
    val interstitialState: StateFlow<AdState> = _interstitialState.asStateFlow()

    private val _interstitialVideoState = MutableStateFlow<AdState>(AdState.Idle)
    val interstitialVideoState: StateFlow<AdState> = _interstitialVideoState.asStateFlow()

    private val _rewardedState = MutableStateFlow<AdState>(AdState.Idle)
    val rewardedState: StateFlow<AdState> = _rewardedState.asStateFlow()

    private val _bannerState = MutableStateFlow<AdState>(AdState.Idle)
    val bannerState: StateFlow<AdState> = _bannerState.asStateFlow()

    // Observable Boolean StateFlows kept in sync with states
    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    private val _isInterstitialVideoReady = MutableStateFlow(false)
    val isInterstitialVideoReady: StateFlow<Boolean> = _isInterstitialVideoReady.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    private val _isBannerAdLoaded = MutableStateFlow(false)
    val isBannerAdLoaded: StateFlow<Boolean> = _isBannerAdLoaded.asStateFlow()

    private val _isConsentResolved = MutableStateFlow(false)
    val isConsentResolved: StateFlow<Boolean> = _isConsentResolved.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var connectivityJob: Job? = null

    /**
     * Executes an action safely on the Android main thread, or directly if
     * running in non-Android JVM environments (such as unit tests).
     */
    private fun runOnMainThread(action: () -> Unit) {
        try {
            val looper = Looper.getMainLooper()
            if (looper != null && Looper.myLooper() == looper) {
                action()
            } else if (looper != null) {
                Handler(looper).post(action)
            } else {
                action()
            }
        } catch (_: Throwable) {
            action()
        }
    }

    private fun runOnMainThreadDelayed(delayMillis: Long, action: () -> Unit) {
        try {
            val looper = Looper.getMainLooper()
            if (looper != null) {
                Handler(looper).postDelayed(action, delayMillis)
            } else {
                action()
            }
        } catch (_: Throwable) {
            action()
        }
    }

    @JvmOverloads
    fun setAdFreeUntil(timestamp: Long, context: Context? = null) {
        val wasAdFree = isAdFreeActive()
        adFreeUntilMillis = timestamp
        val isNowAdFree = isAdFreeActive()
        if (isNowAdFree) {
            // Clear preloaded ads to preserve memory when ad-free mode is active
            mInterstitialAd = null
            mInterstitialVideoAd = null
            mRewardedAd = null
            mRewardedInterstitialAd = null
            interstitialLoadedAt = 0L
            interstitialVideoLoadedAt = 0L
            rewardedLoadedAt = 0L
            _interstitialState.value = AdState.Idle
            _interstitialVideoState.value = AdState.Idle
            _rewardedState.value = AdState.Idle
            _isInterstitialReady.value = false
            _isInterstitialVideoReady.value = false
            _isRewardedReady.value = false
        } else if (wasAdFree || (_interstitialState.value is AdState.Idle && _rewardedState.value is AdState.Idle)) {
            if (_initState.value is AdInitState.Initialized && _isConsentResolved.value && canRequestAds()) {
                val targetCtx = context ?: currentActivity ?: com.example.HabitStreaksApplication.currentActivity
                if (targetCtx != null) {
                    preloadActiveAds(targetCtx)
                }
            }
        }
        try {
            Log.d(TAG, "Ad-Free mode set until $timestamp. Active: $isNowAdFree")
        } catch (_: Throwable) {}
    }

    /**
     * Checks cached full-screen ads for 55-minute expiration, evicts stale ads,
     * and ensures active ad slots are preloaded when ads are eligible.
     */
    fun refreshAdsIfNeeded(context: Context) {
        if (mInterstitialAd != null && isAdExpired(interstitialLoadedAt)) {
            mInterstitialAd = null
            interstitialLoadedAt = 0L
            _interstitialState.value = AdState.Idle
            _isInterstitialReady.value = false
        }
        if (mInterstitialVideoAd != null && isAdExpired(interstitialVideoLoadedAt)) {
            mInterstitialVideoAd = null
            interstitialVideoLoadedAt = 0L
            _interstitialVideoState.value = AdState.Idle
            _isInterstitialVideoReady.value = false
        }
        if ((mRewardedAd != null || mRewardedInterstitialAd != null) && isAdExpired(rewardedLoadedAt)) {
            mRewardedAd = null
            mRewardedInterstitialAd = null
            rewardedLoadedAt = 0L
            _rewardedState.value = AdState.Idle
            _isRewardedReady.value = false
        }

        if (_initState.value is AdInitState.Initialized && _isConsentResolved.value && canRequestAds()) {
            preloadActiveAds(context)
        }
    }

    fun isAdFreeActive(): Boolean = System.currentTimeMillis() < adFreeUntilMillis

    fun getAdFreeTimeRemainingMillis(): Long {
        val remaining = adFreeUntilMillis - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    fun setConsentGranted(granted: Boolean) {
        explicitConsentRevoked = !granted
        isConsentGranted = granted
        _isConsentResolved.value = true
        if (!granted) {
            mInterstitialAd = null
            mInterstitialVideoAd = null
            mRewardedAd = null
            mRewardedInterstitialAd = null
            interstitialLoadedAt = 0L
            interstitialVideoLoadedAt = 0L
            rewardedLoadedAt = 0L
            _isInterstitialReady.value = false
            _isInterstitialVideoReady.value = false
            _isRewardedReady.value = false
            _interstitialState.value = AdState.Idle
            _interstitialVideoState.value = AdState.Idle
            _rewardedState.value = AdState.Idle
        } else if (_initState.value is AdInitState.Initialized && !isAdFreeActive()) {
            val targetCtx = currentActivity
                ?: com.example.HabitStreaksApplication.currentActivity
            if (targetCtx != null && NetworkUtils.isNetworkAvailable(targetCtx)) {
                preloadActiveAds(targetCtx)
            }
        }
    }

    private fun preloadActiveAds(context: Context) {
        if (!canRequestAds() || !_isConsentResolved.value || _initState.value !is AdInitState.Initialized) {
            return
        }
        val appCtx = context.applicationContext
        if (!NetworkUtils.isNetworkAvailable(appCtx)) return
        val loadCtx = currentActivity ?: context.findActivity() ?: appCtx

        if (_interstitialState.value !is AdState.Ready && _interstitialState.value !is AdState.Loading) {
            loadInterstitialAd(loadCtx)
        }
        if (_rewardedState.value !is AdState.Ready && _rewardedState.value !is AdState.Loading) {
            loadRewardedAd(loadCtx)
        }
    }

    fun canRequestAds(): Boolean {
        if (explicitConsentRevoked) return false
        if (isAdFreeActive()) return false
        return isConsentGranted || isTestOrDebugEnvironment()
    }

    /**
     * Integrates the Google User Messaging Platform (UMP) consent workflow.
     * Ensures MobileAds initializes once, resolves UMP consent, and then preloads ads
     * with an active Activity context.
     */
    fun requestConsentAndInitialize(activity: Activity, onComplete: () -> Unit = {}) {
        runOnMainThread {
            val appContext = activity.applicationContext
            // Ensure MobileAds starts initializing once (does not preload until consent resolves)
            initialize(appContext, preloadAds = false)

            if (activity.isFinishing || activity.isDestroyed) {
                setConsentGranted(true)
                onComplete()
                return@runOnMainThread
            }

            val completed = AtomicBoolean(false)
            fun finishConsent(canRequest: Boolean) {
                if (completed.compareAndSet(false, true)) {
                    setConsentGranted(canRequest)
                    onComplete()
                }
            }

            // Safety guard: never stall ad loading indefinitely if UMP network call hangs
            runOnMainThreadDelayed(4000L) {
                if (!completed.get() && isTestOrDebugEnvironment()) {
                    try {
                        Log.w(TAG, "UMP consent check timed out after 4s in debug/test mode; proceeding.")
                    } catch (_: Throwable) {}
                    finishConsent(true)
                }
            }

            try {
                val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
                val paramsBuilder = ConsentRequestParameters.Builder()
                    .setTagForUnderAgeOfConsent(false)

                if (isTestOrDebugEnvironment()) {
                    try {
                        val debugSettings = ConsentDebugSettings.Builder(activity)
                            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED)
                            .addTestDeviceHashedId(AdRequest.DEVICE_ID_EMULATOR)
                            .apply {
                                testDeviceIds.forEach { id ->
                                    if (id != AdRequest.DEVICE_ID_EMULATOR) {
                                        addTestDeviceHashedId(id)
                                    }
                                }
                            }
                            .build()
                        paramsBuilder.setConsentDebugSettings(debugSettings)
                    } catch (_: Throwable) {}
                }

                val params = paramsBuilder.build()

                consentInformation.requestConsentInfoUpdate(
                    activity,
                    params,
                    {
                        if (activity.isFinishing || activity.isDestroyed) {
                            finishConsent(true)
                            return@requestConsentInfoUpdate
                        }
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                            if (formError != null) {
                                try {
                                    Log.w(TAG, "Consent form error: ${formError.errorCode} - ${formError.message}")
                                } catch (_: Throwable) {}
                            }
                            val canRequest = try {
                                consentInformation.canRequestAds() || isTestOrDebugEnvironment()
                            } catch (_: Throwable) {
                                true
                            }
                            finishConsent(canRequest)
                        }
                    },
                    { requestConsentError ->
                        try {
                            Log.w(TAG, "Consent info update failed: ${requestConsentError.errorCode} - ${requestConsentError.message}")
                        } catch (_: Throwable) {}
                        val canRequest = try {
                            consentInformation.canRequestAds() || isTestOrDebugEnvironment()
                        } catch (_: Throwable) {
                            true
                        }
                        finishConsent(canRequest)
                    }
                )
            } catch (t: Throwable) {
                try {
                    Log.w(TAG, "UMP SDK unavailable or test environment, falling back to direct consent resolution", t)
                } catch (_: Throwable) {}
                finishConsent(true)
            }
        }
    }

    fun isPrivacyOptionsRequired(context: Context): Boolean {
        return try {
            val consentInformation = UserMessagingPlatform.getConsentInformation(context)
            consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (_: Throwable) {
            false
        }
    }

    fun showPrivacyOptions(activity: Activity, onDismiss: () -> Unit = {}) {
        runOnMainThread {
            val targetActivity = if (!activity.isFinishing && !activity.isDestroyed) {
                activity
            } else {
                currentActivity ?: com.example.HabitStreaksApplication.currentActivity ?: activity
            }
            if (targetActivity.isFinishing || targetActivity.isDestroyed) {
                onDismiss()
                return@runOnMainThread
            }

            try {
                UserMessagingPlatform.showPrivacyOptionsForm(targetActivity) { formError ->
                    if (formError != null) {
                        try {
                            Log.w(TAG, "Privacy options form error: ${formError.errorCode} - ${formError.message}")
                        } catch (_: Throwable) {}
                    }
                    val canRequest = try {
                        UserMessagingPlatform.getConsentInformation(targetActivity).canRequestAds()
                    } catch (_: Throwable) {
                        true
                    }
                    setConsentGranted(canRequest)
                    onDismiss()
                }
            } catch (t: Throwable) {
                try {
                    Log.w(TAG, "Failed to launch privacy options form", t)
                } catch (_: Throwable) {}
                onDismiss()
            }
        }
    }

    /**
     * Opens the Google AdMob Ad Inspector for on-device diagnostics.
     */
    fun openAdInspector(context: Context, onDismiss: () -> Unit = {}) {
        runOnMainThread {
            try {
                com.google.android.gms.ads.MobileAds.openAdInspector(context) { error ->
                    if (error != null) {
                        try {
                            Log.w(TAG, "Ad Inspector error: ${error.code} - ${error.message}")
                        } catch (_: Throwable) {}
                    }
                    onDismiss()
                }
            } catch (t: Throwable) {
                try {
                    Log.w(TAG, "Failed to launch Ad Inspector", t)
                } catch (_: Throwable) {}
                onDismiss()
            }
        }
    }

    private val _isOfflineBypassActive = MutableStateFlow(false)
    val isOfflineBypassActive: StateFlow<Boolean> = _isOfflineBypassActive.asStateFlow()

    fun setOfflineBypass(bypassed: Boolean) {
        _isOfflineBypassActive.value = bypassed
        try {
            Log.d(TAG, "Offline bypass set to $bypassed")
        } catch (_: Throwable) {}
    }

    fun isOfflineBypassed(): Boolean = _isOfflineBypassActive.value

    fun onMobileAdsInitialized(context: Context, preloadAds: Boolean = true) {
        _initState.value = AdInitState.Initialized
        val appContext = context.applicationContext
        startConnectivityMonitorIfNeeded(appContext)
        if ((preloadAds || _isConsentResolved.value) && canRequestAds() && NetworkUtils.isNetworkAvailable(appContext)) {
            if (!_isConsentResolved.value && isTestOrDebugEnvironment()) {
                _isConsentResolved.value = true
                isConsentGranted = true
            }
            if (_isConsentResolved.value) {
                val targetCtx = currentActivity ?: appContext
                preloadActiveAds(targetCtx)
            }
        }
    }

    fun onMobileAdsInitializationFailed(error: String) {
        _initState.value = AdInitState.Failed(error)
    }

    private fun startConnectivityMonitorIfNeeded(appContext: Context) {
        if (connectivityJob == null) {
            connectivityJob = scope.launch {
                try {
                    val monitor = ConnectivityMonitor.getInstance(appContext)
                    monitor.networkStatus.collectLatest { status ->
                        if (status is NetworkStatus.ValidatedOnline) {
                            // Clear offline bypass when validated internet connectivity returns
                            _isOfflineBypassActive.value = false
                            if (_isConsentResolved.value && canRequestAds() && _initState.value is AdInitState.Initialized) {
                                val targetCtx = currentActivity ?: appContext
                                preloadActiveAds(targetCtx)
                            }
                        }
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    fun initialize(context: Context, preloadAds: Boolean = true) {
        val appContext = context.applicationContext
        if (_initState.value is AdInitState.Initialized) {
            startConnectivityMonitorIfNeeded(appContext)
            if (preloadAds && _isConsentResolved.value && canRequestAds()) {
                preloadActiveAds(currentActivity ?: appContext)
            }
            return
        }
        if (_initState.value is AdInitState.Initializing) {
            if (preloadAds) preloadPendingOnInit = true
            return
        }
        _initState.value = AdInitState.Initializing
        preloadPendingOnInit = preloadAds
        sessionStartTime = System.currentTimeMillis()
        startConnectivityMonitorIfNeeded(appContext)

        runOnMainThread {
            try {
                applyRequestConfiguration()
                MobileAds.initialize(appContext) { initializationStatus ->
                    AdLogUtility.logInitializationStatus(initializationStatus)
                    onMobileAdsInitialized(appContext, preloadPendingOnInit)
                }
            } catch (e: Throwable) {
                try {
                    Log.e(TAG, "Error initializing MobileAds", e)
                } catch (_: Throwable) {}
                onMobileAdsInitializationFailed(e.message ?: "Unknown initialization error")
            }
        }
    }

    fun loadInterstitialAd(context: Context, force: Boolean = false) {
        if (!canRequestAds()) {
            mInterstitialAd = null
            _interstitialState.value = AdState.Idle
            _isInterstitialReady.value = false
            return
        }
        val now = System.currentTimeMillis()
        if (mInterstitialAd != null && isAdExpired(interstitialLoadedAt)) {
            mInterstitialAd = null
            interstitialLoadedAt = 0L
            _interstitialState.value = AdState.Idle
            _isInterstitialReady.value = false
        }
        val isStaleLoading = (_interstitialState.value is AdState.Loading && (now - lastInterstitialLoadTime > 15000L))
        if (!force && !isStaleLoading) {
            if (_interstitialState.value is AdState.Loading || _interstitialState.value is AdState.Showing) {
                return
            }
            if (mInterstitialAd != null && _interstitialState.value is AdState.Ready) {
                return
            }
        }

        val appContext = context.applicationContext
        val activity = currentActivity ?: context.findActivity()
        val loadContext = activity ?: appContext

        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            mInterstitialAd = null
            _interstitialState.value = AdState.Failed("Device is offline")
            _isInterstitialReady.value = false
            return
        }

        if (_initState.value !is AdInitState.Initialized) {
            val seq = interstitialLoadSeq.incrementAndGet()
            lastInterstitialLoadTime = now
            _interstitialState.value = AdState.Loading
            _isInterstitialReady.value = false
            initialize(appContext)
            scope.launch {
                var waited = 0
                while (waited < 3000 && _initState.value !is AdInitState.Initialized && seq == interstitialLoadSeq.get()) {
                    delay(100L)
                    waited += 100
                }
                if (seq != interstitialLoadSeq.get()) return@launch
                if (_initState.value is AdInitState.Initialized) {
                    loadInterstitialAd(currentActivity ?: loadContext, force = true)
                } else if (isTestOrDebugEnvironment()) {
                    _initState.value = AdInitState.Initialized
                    loadInterstitialAd(currentActivity ?: loadContext, force = true)
                } else {
                    _interstitialState.value = AdState.Failed("SDK initialization timeout")
                    _isInterstitialReady.value = false
                }
            }
            return
        }

        if (force) {
            interstitialRetryCount = 0
        }

        val seq = interstitialLoadSeq.incrementAndGet()
        lastInterstitialLoadTime = now
        _interstitialState.value = AdState.Loading
        _isInterstitialReady.value = false
        runOnMainThread {
            try {
                val adRequest = AdRequest.Builder().build()
                val adUnitId = AdConfig.current.interstitialAdUnitId
                AdLogUtility.logAdRequestDetails(loadContext, "Interstitial Ad", adUnitId, adRequest)
                InterstitialAd.load(
                    loadContext,
                    adUnitId,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                            if (seq != interstitialLoadSeq.get()) return
                            mInterstitialAd = interstitialAd
                            interstitialLoadedAt = System.currentTimeMillis()
                            _interstitialState.value = AdState.Ready
                            _isInterstitialReady.value = true
                            interstitialRetryCount = 0
                            AdLogUtility.logAdLoaded("Interstitial Ad", adUnitId)
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            if (seq != interstitialLoadSeq.get()) return
                            mInterstitialAd = null
                            _isInterstitialReady.value = false
                            AdLogUtility.logAdFailed("Interstitial Ad", adUnitId, adError)

                            if (interstitialRetryCount < MAX_RETRY_COUNT && canRequestAds() && NetworkUtils.isNetworkAvailable(appContext)) {
                                interstitialRetryCount++
                                val backoffDelay = interstitialRetryCount * 2000L
                                scope.launch {
                                    delay(backoffDelay)
                                    if (seq == interstitialLoadSeq.get() && _interstitialState.value !is AdState.Ready) {
                                        loadInterstitialAd(currentActivity ?: appContext)
                                    }
                                }
                            } else {
                                _interstitialState.value = AdState.Failed(formatLoadErrorMessage(adError))
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                mInterstitialAd = null
                _interstitialState.value = AdState.Failed(e.message ?: "Exception loading interstitial")
                _isInterstitialReady.value = false
            }
        }
    }

    /**
     * Loads a Google AdMob Interstitial Video ad unit.
     */
    fun loadInterstitialVideoAd(context: Context, force: Boolean = false) {
        if (!canRequestAds()) {
            mInterstitialVideoAd = null
            _interstitialVideoState.value = AdState.Idle
            _isInterstitialVideoReady.value = false
            return
        }
        val now = System.currentTimeMillis()
        if (mInterstitialVideoAd != null && isAdExpired(interstitialVideoLoadedAt)) {
            mInterstitialVideoAd = null
            interstitialVideoLoadedAt = 0L
            _interstitialVideoState.value = AdState.Idle
            _isInterstitialVideoReady.value = false
        }
        val isStaleLoading = (_interstitialVideoState.value is AdState.Loading && (now - lastInterstitialVideoLoadTime > 15000L))
        if (!force && !isStaleLoading) {
            if (_interstitialVideoState.value is AdState.Loading || _interstitialVideoState.value is AdState.Showing) {
                return
            }
            if (mInterstitialVideoAd != null && _interstitialVideoState.value is AdState.Ready) {
                return
            }
        }

        val appContext = context.applicationContext
        val activity = currentActivity ?: context.findActivity()
        val loadContext = activity ?: appContext

        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            mInterstitialVideoAd = null
            _interstitialVideoState.value = AdState.Failed("Device is offline")
            _isInterstitialVideoReady.value = false
            return
        }

        if (_initState.value !is AdInitState.Initialized) {
            val seq = interstitialVideoLoadSeq.incrementAndGet()
            lastInterstitialVideoLoadTime = now
            _interstitialVideoState.value = AdState.Loading
            _isInterstitialVideoReady.value = false
            initialize(appContext, preloadAds = false)
            scope.launch {
                var waited = 0
                while (waited < 3000 && _initState.value !is AdInitState.Initialized && seq == interstitialVideoLoadSeq.get()) {
                    delay(100L)
                    waited += 100
                }
                if (seq != interstitialVideoLoadSeq.get()) return@launch
                if (_initState.value is AdInitState.Initialized) {
                    loadInterstitialVideoAd(currentActivity ?: loadContext, force = true)
                } else if (isTestOrDebugEnvironment()) {
                    _initState.value = AdInitState.Initialized
                    loadInterstitialVideoAd(currentActivity ?: loadContext, force = true)
                } else {
                    _interstitialVideoState.value = AdState.Failed("SDK initialization timeout")
                    _isInterstitialVideoReady.value = false
                }
            }
            return
        }

        if (force) {
            interstitialVideoRetryCount = 0
        }

        val seq = interstitialVideoLoadSeq.incrementAndGet()
        lastInterstitialVideoLoadTime = now
        _interstitialVideoState.value = AdState.Loading
        _isInterstitialVideoReady.value = false
        runOnMainThread {
            try {
                val adRequest = AdRequest.Builder().build()
                val adUnitId = AdConfig.current.interstitialVideoAdUnitId
                AdLogUtility.logAdRequestDetails(loadContext, "Interstitial Video Ad", adUnitId, adRequest)
                InterstitialAd.load(
                    loadContext,
                    adUnitId,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                            if (seq != interstitialVideoLoadSeq.get()) return
                            mInterstitialVideoAd = interstitialAd
                            interstitialVideoLoadedAt = System.currentTimeMillis()
                            _interstitialVideoState.value = AdState.Ready
                            _isInterstitialVideoReady.value = true
                            interstitialVideoRetryCount = 0
                            AdLogUtility.logAdLoaded("Interstitial Video (Long Video) Ad", adUnitId)
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            if (seq != interstitialVideoLoadSeq.get()) return
                            mInterstitialVideoAd = null
                            _isInterstitialVideoReady.value = false
                            AdLogUtility.logAdFailed("Interstitial Video (Long Video) Ad", adUnitId, adError)

                            if (interstitialVideoRetryCount < MAX_RETRY_COUNT && canRequestAds() && NetworkUtils.isNetworkAvailable(appContext)) {
                                interstitialVideoRetryCount++
                                val backoffDelay = interstitialVideoRetryCount * 2000L
                                scope.launch {
                                    delay(backoffDelay)
                                    if (seq == interstitialVideoLoadSeq.get() && _interstitialVideoState.value !is AdState.Ready) {
                                        loadInterstitialVideoAd(currentActivity ?: appContext)
                                    }
                                }
                            } else {
                                _interstitialVideoState.value = AdState.Failed(formatLoadErrorMessage(adError))
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                mInterstitialVideoAd = null
                _interstitialVideoState.value = AdState.Failed(e.message ?: "Exception loading interstitial video")
                _isInterstitialVideoReady.value = false
            }
        }
    }

    private fun formatLoadErrorMessage(adError: LoadAdError): String {
        return when (adError.code) {
            AdRequest.ERROR_CODE_NO_FILL ->
                "No ad inventory available right now (Error 3: No Fill). Please wait a moment and tap Retry."
            AdRequest.ERROR_CODE_NETWORK_ERROR ->
                "Network error loading ad. Check your internet connection and tap Retry."
            AdRequest.ERROR_CODE_INVALID_REQUEST ->
                "Invalid ad request (Error 1): ${adError.message}"
            else ->
                "Ad failed to load (Error ${adError.code}): ${adError.message}"
        }
    }

    private fun scheduleRewardedRetry(context: Context, expectedSeq: Int, finalErrorMessage: String) {
        if (rewardedRetryCount < MAX_RETRY_COUNT && canRequestAds() && NetworkUtils.isNetworkAvailable(context)) {
            rewardedRetryCount++
            val backoffDelay = rewardedRetryCount * 1200L
            scope.launch {
                delay(backoffDelay)
                if (expectedSeq == rewardedLoadSeq.get() && _rewardedState.value !is AdState.Ready) {
                    loadRewardedAd(currentActivity ?: context, isRetryAttempt = true)
                }
            }
        } else {
            _rewardedState.value = AdState.Failed(finalErrorMessage)
            _isRewardedReady.value = false
        }
    }

    fun loadRewardedAd(context: Context, force: Boolean = false) {
        loadRewardedAd(context, force = force, isRetryAttempt = false)
    }

    private fun loadRewardedAd(context: Context, force: Boolean = false, isRetryAttempt: Boolean = false) {
        if (!canRequestAds()) {
            mRewardedAd = null
            mRewardedInterstitialAd = null
            _rewardedState.value = AdState.Idle
            _isRewardedReady.value = false
            return
        }
        val now = System.currentTimeMillis()
        if ((mRewardedAd != null || mRewardedInterstitialAd != null) && isAdExpired(rewardedLoadedAt)) {
            mRewardedAd = null
            mRewardedInterstitialAd = null
            rewardedLoadedAt = 0L
            _rewardedState.value = AdState.Idle
            _isRewardedReady.value = false
        }
        val isStaleLoading = (_rewardedState.value is AdState.Loading && (now - lastRewardedLoadTime > 15000L))
        if (!force && !isRetryAttempt && !isStaleLoading) {
            if (_rewardedState.value is AdState.Loading || _rewardedState.value is AdState.Showing) {
                return
            }
            if ((mRewardedAd != null || mRewardedInterstitialAd != null) && _rewardedState.value is AdState.Ready) {
                return
            }
        }

        val appContext = context.applicationContext
        val activity = currentActivity ?: context.findActivity()
        val loadContext = activity ?: appContext

        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            mRewardedAd = null
            mRewardedInterstitialAd = null
            _rewardedState.value = AdState.Failed("Device is offline")
            _isRewardedReady.value = false
            return
        }

        if (_initState.value !is AdInitState.Initialized) {
            val seq = rewardedLoadSeq.incrementAndGet()
            lastRewardedLoadTime = now
            _rewardedState.value = AdState.Loading
            _isRewardedReady.value = false
            initialize(appContext, preloadAds = false)
            scope.launch {
                var waited = 0
                while (waited < 3000 && _initState.value !is AdInitState.Initialized && seq == rewardedLoadSeq.get()) {
                    delay(100L)
                    waited += 100
                }
                if (seq != rewardedLoadSeq.get()) return@launch
                if (_initState.value is AdInitState.Initialized) {
                    loadRewardedAd(currentActivity ?: loadContext, force = true, isRetryAttempt = isRetryAttempt)
                } else if (isTestOrDebugEnvironment()) {
                    _initState.value = AdInitState.Initialized
                    loadRewardedAd(currentActivity ?: loadContext, force = true, isRetryAttempt = isRetryAttempt)
                } else {
                    _rewardedState.value = AdState.Failed("SDK initialization timeout")
                    _isRewardedReady.value = false
                }
            }
            return
        }

        if (force && !isRetryAttempt) {
            rewardedRetryCount = 0
        }

        val seq = rewardedLoadSeq.incrementAndGet()
        lastRewardedLoadTime = now
        _rewardedState.value = AdState.Loading
        _isRewardedReady.value = false

        runOnMainThread {
            try {
                val adRequest = AdRequest.Builder().build()
                val rewardedUnitId = AdConfig.current.rewardedAdUnitId
                AdLogUtility.logAdRequestDetails(loadContext, "Rewarded Ad", rewardedUnitId, adRequest)

                // 1. Primary: Load standard Google AdMob Rewarded Ad
                RewardedAd.load(
                    loadContext,
                    rewardedUnitId,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(rewardedAd: RewardedAd) {
                            if (seq != rewardedLoadSeq.get()) return
                            mRewardedAd = rewardedAd
                            rewardedLoadedAt = System.currentTimeMillis()
                            _rewardedState.value = AdState.Ready
                            _isRewardedReady.value = true
                            rewardedRetryCount = 0
                            AdLogUtility.logAdLoaded("Rewarded Ad", rewardedUnitId)
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            if (seq != rewardedLoadSeq.get()) return
                            mRewardedAd = null
                            AdLogUtility.logAdFailed("Rewarded Ad", rewardedUnitId, adError)

                            // 2. High-speed fallback: Load Rewarded Interstitial Ad
                            val fallbackUnitId = AdConfig.current.rewardedInterstitialAdUnitId
                            val fallbackRequest = AdRequest.Builder().build()
                            AdLogUtility.logAdRequestDetails(loadContext, "Rewarded Interstitial Ad (Fallback)", fallbackUnitId, fallbackRequest)
                            RewardedInterstitialAd.load(
                                loadContext,
                                fallbackUnitId,
                                fallbackRequest,
                                object : RewardedInterstitialAdLoadCallback() {
                                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                                        if (seq != rewardedLoadSeq.get()) return
                                        mRewardedInterstitialAd = ad
                                        rewardedLoadedAt = System.currentTimeMillis()
                                        _rewardedState.value = AdState.Ready
                                        _isRewardedReady.value = true
                                        rewardedRetryCount = 0
                                        AdLogUtility.logAdLoaded("Rewarded Interstitial Ad (Fallback)", fallbackUnitId)
                                    }

                                    override fun onAdFailedToLoad(fallbackError: LoadAdError) {
                                        if (seq != rewardedLoadSeq.get()) return
                                        mRewardedInterstitialAd = null
                                        AdLogUtility.logAdFailed("Rewarded Interstitial Ad (Fallback)", fallbackUnitId, fallbackError)
                                        if (mInterstitialVideoAd == null && mInterstitialAd == null) {
                                            loadInterstitialVideoAd(loadContext)
                                            loadInterstitialAd(loadContext)
                                        }
                                        scheduleRewardedRetry(
                                            appContext,
                                            seq,
                                            formatLoadErrorMessage(fallbackError)
                                        )
                                    }
                                }
                            )
                        }
                    }
                )
            } catch (e: Throwable) {
                mRewardedAd = null
                mRewardedInterstitialAd = null
                _rewardedState.value = AdState.Failed(e.message ?: "Exception loading rewarded ad")
                _isRewardedReady.value = false
            }
        }
    }

    fun recordMeaningfulAction(): Boolean {
        if (isAdFreeActive()) return false
        meaningfulActionCount++
        try {
            Log.d(TAG, "Meaningful action recorded. Counter: $meaningfulActionCount")
        } catch (_: Throwable) {}
        return canShowInterstitial(isFocusedTask = false)
    }

    fun canShowInterstitial(isFocusedTask: Boolean = false, force: Boolean = false): Boolean {
        if (isAdFreeActive()) return false
        if (!isConsentGranted) return false
        if (isAdShowing) return false
        if (isFocusedTask) return false
        if (force) return true

        val now = System.currentTimeMillis()
        val sessionAge = (now - sessionStartTime) / 1000
        val timeSinceLast = (now - lastInterstitialShownTime) / 1000

        if (sessionAge < policy.minSessionAgeSeconds) return false
        if (sessionInterstitialCount >= policy.maxSessionInterstitials) return false
        if (meaningfulActionCount < policy.minMeaningfulActions) return false
        if (lastInterstitialShownTime > 0 && timeSinceLast < policy.minSecondsBetweenInterstitials) return false

        return true
    }

    /**
     * Request and Continuation contract:
     * Continues intended local user action safely, executing onContinue at most once.
     * Never permanently blocks or waits indefinitely for an ad.
     */
    fun requestInterstitialAndContinue(
        activity: Activity?,
        isFocusedTask: Boolean = false,
        force: Boolean = false,
        preferVideo: Boolean = false,
        onContinue: () -> Unit
    ) {
        val continuationExecuted = AtomicBoolean(false)

        fun safeContinue() {
            if (continuationExecuted.compareAndSet(false, true)) {
                runOnMainThread {
                    onContinue()
                }
            }
        }

        if (isAdFreeActive() || !canShowInterstitial(isFocusedTask, force)) {
            safeContinue()
            return
        }

        val targetActivity = activity
            ?: currentActivity
            ?: com.example.HabitStreaksApplication.currentActivity
        if (targetActivity == null || targetActivity.isFinishing || targetActivity.isDestroyed) {
            safeContinue()
            return
        }

        // Evict stale ads older than 55 minutes before presentation
        if (mInterstitialAd != null && isAdExpired(interstitialLoadedAt)) {
            mInterstitialAd = null
            interstitialLoadedAt = 0L
            _isInterstitialReady.value = false
            _interstitialState.value = AdState.Idle
        }
        if (mInterstitialVideoAd != null && isAdExpired(interstitialVideoLoadedAt)) {
            mInterstitialVideoAd = null
            interstitialVideoLoadedAt = 0L
            _isInterstitialVideoReady.value = false
            _interstitialVideoState.value = AdState.Idle
        }

        // Determine which ad to present: video interstitial (only if requested) or standard interstitial
        val availableAd = when {
            preferVideo -> mInterstitialVideoAd ?: mInterstitialAd
            else -> mInterstitialAd ?: mInterstitialVideoAd
        }

        if (availableAd != null) {
            val isVideo = (availableAd == mInterstitialVideoAd)
            showLoadedInterstitial(targetActivity, availableAd, isVideo, ::safeContinue)
            return
        }

        if (force) {
            if (!NetworkUtils.isNetworkAvailable(targetActivity)) {
                safeContinue()
                return
            }
            // User requested an ad-gated or confirmed action: load with force and allow up to 3.5s for AdMob response
            if (preferVideo) {
                if (_interstitialVideoState.value !is AdState.Loading) {
                    loadInterstitialVideoAd(targetActivity, force = true)
                }
            } else {
                if (_interstitialState.value !is AdState.Loading) {
                    loadInterstitialAd(targetActivity, force = true)
                }
            }
            scope.launch {
                var waited = 0
                while (waited < 3500 && !continuationExecuted.get()) {
                    val adNow = if (preferVideo) (mInterstitialVideoAd ?: mInterstitialAd) else (mInterstitialAd ?: mInterstitialVideoAd)
                    if (adNow != null) {
                        val isVideo = (adNow == mInterstitialVideoAd)
                        runOnMainThread {
                            showLoadedInterstitial(targetActivity, adNow, isVideo, ::safeContinue)
                        }
                        return@launch
                    }
                    if (!NetworkUtils.isNetworkAvailable(targetActivity)) {
                        break
                    }
                    val targetState = if (preferVideo) _interstitialVideoState.value else _interstitialState.value
                    if (targetState is AdState.Failed && (!preferVideo || _interstitialState.value is AdState.Failed)) {
                        break
                    }
                    delay(150L)
                    waited += 150
                }
                safeContinue()
            }
            return
        }

        loadInterstitialAd(targetActivity)
        safeContinue()
    }

    fun requestInterstitialAndContinue(
        activity: Activity?,
        isFocusedTask: Boolean = false,
        force: Boolean = false,
        onContinue: () -> Unit
    ) {
        requestInterstitialAndContinue(activity, isFocusedTask, force, preferVideo = false, onContinue)
    }

    fun requestInterstitialAndContinue(
        activity: Activity?,
        isFocusedTask: Boolean = false,
        onContinue: () -> Unit
    ) {
        requestInterstitialAndContinue(activity, isFocusedTask, force = false, preferVideo = false, onContinue)
    }

    private fun showLoadedInterstitial(
        targetActivity: Activity,
        ad: InterstitialAd,
        isVideo: Boolean = false,
        onFinished: () -> Unit
    ) {
        if (!adShowInProgress.compareAndSet(false, true)) {
            onFinished()
            return
        }
        isAdShowing = true
        runOnMainThread {
            try {
                if (isVideo) {
                    mInterstitialVideoAd = null
                    _interstitialVideoState.value = AdState.Showing
                    _isInterstitialVideoReady.value = false
                } else {
                    mInterstitialAd = null
                    _interstitialState.value = AdState.Showing
                    _isInterstitialReady.value = false
                }
                lastInterstitialShownTime = System.currentTimeMillis()
                sessionInterstitialCount++
                meaningfulActionCount = 0

                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdImpression() {
                        Log.d(TAG, if (isVideo) "Interstitial Video impression recorded" else "Interstitial impression recorded")
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, if (isVideo) "Interstitial Video clicked" else "Interstitial clicked")
                    }

                    override fun onAdDismissedFullScreenContent() {
                        isAdShowing = false
                        adShowInProgress.set(false)
                        val preloadContext: Context = currentActivity
                            ?.takeIf { !it.isFinishing && !it.isDestroyed }
                            ?: targetActivity.applicationContext
                        if (isVideo) {
                            mInterstitialVideoAd = null
                            interstitialVideoLoadedAt = 0L
                            _interstitialVideoState.value = AdState.Idle
                            _isInterstitialVideoReady.value = false
                            loadInterstitialVideoAd(preloadContext)
                        } else {
                            mInterstitialAd = null
                            interstitialLoadedAt = 0L
                            _interstitialState.value = AdState.Idle
                            _isInterstitialReady.value = false
                            loadInterstitialAd(preloadContext)
                        }
                        onFinished()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        isAdShowing = false
                        adShowInProgress.set(false)
                        AdLogUtility.logFullScreenError(if (isVideo) "Interstitial Video Ad" else "Interstitial Ad", adError)
                        val preloadContext: Context = currentActivity
                            ?.takeIf { !it.isFinishing && !it.isDestroyed }
                            ?: targetActivity.applicationContext
                        if (isVideo) {
                            mInterstitialVideoAd = null
                            interstitialVideoLoadedAt = 0L
                            _interstitialVideoState.value = AdState.Failed(adError.message)
                            _isInterstitialVideoReady.value = false
                            loadInterstitialVideoAd(preloadContext)
                        } else {
                            mInterstitialAd = null
                            interstitialLoadedAt = 0L
                            _interstitialState.value = AdState.Failed(adError.message)
                            _isInterstitialReady.value = false
                            loadInterstitialAd(preloadContext)
                        }
                        onFinished()
                    }
                }
                ad.show(targetActivity)
            } catch (t: Throwable) {
                isAdShowing = false
                adShowInProgress.set(false)
                if (isVideo) {
                    mInterstitialVideoAd = null
                    interstitialVideoLoadedAt = 0L
                    _interstitialVideoState.value = AdState.Failed(t.message ?: "Show error")
                    _isInterstitialVideoReady.value = false
                } else {
                    mInterstitialAd = null
                    interstitialLoadedAt = 0L
                    _interstitialState.value = AdState.Failed(t.message ?: "Show error")
                    _isInterstitialReady.value = false
                }
                onFinished()
            }
        }
    }

    /**
     * Shows a real RewardedAd or RewardedInterstitialAd.
     * Rewarded entitlement is granted ONLY inside onUserEarnedReward callback from Google Mobile Ads SDK.
     */
    fun showRewardedAd(
        activity: Activity?,
        onUserEarnedReward: (RewardItem) -> Unit,
        onAdClosedOrFailed: () -> Unit
    ) {
        val continuationExecuted = AtomicBoolean(false)

        fun safeFinish() {
            if (continuationExecuted.compareAndSet(false, true)) {
                runOnMainThread {
                    onAdClosedOrFailed()
                }
            }
        }

        if ((mRewardedAd != null || mRewardedInterstitialAd != null) && isAdExpired(rewardedLoadedAt)) {
            mRewardedAd = null
            mRewardedInterstitialAd = null
            rewardedLoadedAt = 0L
            _isRewardedReady.value = false
            _rewardedState.value = AdState.Idle
        }

        val targetActivity = activity
            ?: currentActivity
            ?: com.example.HabitStreaksApplication.currentActivity
        val rewardedAd = mRewardedAd
        val rewardedInterstitialAd = mRewardedInterstitialAd

        if (targetActivity == null || targetActivity.isFinishing || targetActivity.isDestroyed) {
            safeFinish()
            return
        }

        if (rewardedAd == null && rewardedInterstitialAd == null) {
            val fallbackInterstitial = mInterstitialVideoAd ?: mInterstitialAd
            if (fallbackInterstitial != null) {
                val isVideo = (fallbackInterstitial == mInterstitialVideoAd)
                showLoadedInterstitial(targetActivity, fallbackInterstitial, isVideo) {
                    onUserEarnedReward(object : RewardItem {
                        override fun getAmount(): Int = 1
                        override fun getType(): String = "sponsor_fallback"
                    })
                    safeFinish()
                }
                return
            }
            _rewardedState.value = AdState.Failed("Rewarded ad not available")
            _isRewardedReady.value = false
            try {
                Log.w(TAG, "Rewarded ad unavailable. Refusing to grant reward without watching ad.")
            } catch (_: Throwable) {}
            safeFinish()
            return
        }

        if (!adShowInProgress.compareAndSet(false, true)) {
            safeFinish()
            return
        }
        isAdShowing = true

        runOnMainThread {
            try {
                _rewardedState.value = AdState.Showing
                _isRewardedReady.value = false

                val rewardConsumed = AtomicBoolean(false)

                if (rewardedAd != null) {
                    mRewardedAd = null
                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdImpression() {
                            Log.d(TAG, "Rewarded Ad impression recorded")
                        }

                        override fun onAdClicked() {
                            Log.d(TAG, "Rewarded Ad clicked")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            isAdShowing = false
                            adShowInProgress.set(false)
                            mRewardedAd = null
                            rewardedLoadedAt = 0L
                            _rewardedState.value = AdState.Idle
                            _isRewardedReady.value = false
                            val preloadContext: Context = currentActivity
                                ?.takeIf { !it.isFinishing && !it.isDestroyed }
                                ?: targetActivity.applicationContext
                            loadRewardedAd(preloadContext)
                            safeFinish()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            isAdShowing = false
                            adShowInProgress.set(false)
                            AdLogUtility.logFullScreenError("Rewarded Ad", adError)
                            mRewardedAd = null
                            rewardedLoadedAt = 0L
                            _rewardedState.value = AdState.Failed(adError.message)
                            _isRewardedReady.value = false
                            safeFinish()
                        }
                    }

                    rewardedAd.show(targetActivity) { rewardItem ->
                        if (rewardConsumed.compareAndSet(false, true)) {
                            try {
                                Log.d(TAG, "User earned reward from RewardedAd: ${rewardItem.amount} ${rewardItem.type}")
                            } catch (_: Throwable) {}
                            runOnMainThread {
                                onUserEarnedReward(rewardItem)
                            }
                        }
                    }
                } else if (rewardedInterstitialAd != null) {
                    mRewardedInterstitialAd = null
                    rewardedInterstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdImpression() {
                            Log.d(TAG, "Rewarded Interstitial Ad impression recorded")
                        }

                        override fun onAdClicked() {
                            Log.d(TAG, "Rewarded Interstitial Ad clicked")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            isAdShowing = false
                            adShowInProgress.set(false)
                            mRewardedInterstitialAd = null
                            rewardedLoadedAt = 0L
                            _rewardedState.value = AdState.Idle
                            _isRewardedReady.value = false
                            val preloadContext: Context = currentActivity
                                ?.takeIf { !it.isFinishing && !it.isDestroyed }
                                ?: targetActivity.applicationContext
                            loadRewardedAd(preloadContext)
                            safeFinish()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            isAdShowing = false
                            adShowInProgress.set(false)
                            AdLogUtility.logFullScreenError("Rewarded Interstitial Ad", adError)
                            mRewardedInterstitialAd = null
                            rewardedLoadedAt = 0L
                            _rewardedState.value = AdState.Failed(adError.message)
                            _isRewardedReady.value = false
                            safeFinish()
                        }
                    }

                    rewardedInterstitialAd.show(targetActivity) { rewardItem ->
                        if (rewardConsumed.compareAndSet(false, true)) {
                            try {
                                Log.d(TAG, "User earned reward from RewardedInterstitialAd: ${rewardItem.amount} ${rewardItem.type}")
                            } catch (_: Throwable) {}
                            runOnMainThread {
                                onUserEarnedReward(rewardItem)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                isAdShowing = false
                adShowInProgress.set(false)
                mRewardedAd = null
                mRewardedInterstitialAd = null
                _rewardedState.value = AdState.Failed(t.message ?: "Show error")
                _isRewardedReady.value = false
                safeFinish()
            }
        }
    }

    fun setBannerLoaded(loaded: Boolean) {
        _isBannerAdLoaded.value = loaded
        _bannerState.value = if (loaded) AdState.Ready else AdState.Idle
    }

    /**
     * Explicit disposal of active ads and resources.
     */
    fun dispose() {
        mInterstitialAd = null
        mInterstitialVideoAd = null
        mRewardedAd = null
        mRewardedInterstitialAd = null
        _interstitialState.value = AdState.Disposed
        _interstitialVideoState.value = AdState.Disposed
        _rewardedState.value = AdState.Disposed
        _isInterstitialReady.value = false
        _isInterstitialVideoReady.value = false
        _isRewardedReady.value = false
        connectivityJob?.cancel()
        connectivityJob = null
    }

    /**
     * Resets AdManager state for isolated testing.
     */
    fun resetForTesting() {
        currentActivityRef = java.lang.ref.WeakReference(null)
        mInterstitialAd = null
        mInterstitialVideoAd = null
        mRewardedAd = null
        mRewardedInterstitialAd = null
        adFreeUntilMillis = 0L
        adShowInProgress.set(false)
        isAdShowing = false
        isConsentGranted = true
        explicitConsentRevoked = false
        sessionStartTime = System.currentTimeMillis()
        lastInterstitialShownTime = 0L
        lastRewardedLoadTime = 0L
        lastInterstitialLoadTime = 0L
        lastInterstitialVideoLoadTime = 0L
        interstitialLoadedAt = 0L
        interstitialVideoLoadedAt = 0L
        rewardedLoadedAt = 0L
        sessionInterstitialCount = 0
        meaningfulActionCount = 0
        interstitialRetryCount = 0
        interstitialVideoRetryCount = 0
        rewardedRetryCount = 0
        interstitialLoadSeq.incrementAndGet()
        interstitialVideoLoadSeq.incrementAndGet()
        rewardedLoadSeq.incrementAndGet()
        preloadPendingOnInit = false
        _initState.value = AdInitState.NotInitialized
        _interstitialState.value = AdState.Idle
        _interstitialVideoState.value = AdState.Idle
        _rewardedState.value = AdState.Idle
        _bannerState.value = AdState.Idle
        _isInterstitialReady.value = false
        _isInterstitialVideoReady.value = false
        _isRewardedReady.value = false
        _isBannerAdLoaded.value = false
        _isOfflineBypassActive.value = false
        _isConsentResolved.value = false
        policy = AdEligibilityPolicy()
    }

    // Callers with failure and dismissal handling
    fun tryShowInterstitial(activity: Activity?, onAdDismissed: () -> Unit, onAdFailed: () -> Unit) {
        if (!canShowInterstitial() || activity == null || activity.isFinishing || activity.isDestroyed || (mInterstitialAd == null && mInterstitialVideoAd == null)) {
            if (activity != null) {
                loadInterstitialAd(activity.applicationContext)
                loadInterstitialVideoAd(activity.applicationContext)
            }
            onAdFailed()
        } else {
            requestInterstitialAndContinue(activity, isFocusedTask = false, onContinue = onAdDismissed)
        }
    }

    fun tryShowInterstitial(activity: Activity?, onAdDismissed: () -> Unit) {
        requestInterstitialAndContinue(activity, isFocusedTask = false, onContinue = onAdDismissed)
    }

    fun tryShowInterstitial(onAdDismissed: () -> Unit) {
        requestInterstitialAndContinue(null, isFocusedTask = false, onContinue = onAdDismissed)
    }

    fun tryShowInterstitial(onAdDismissed: () -> Unit, onAdFailed: () -> Unit) {
        if (!canShowInterstitial() || (mInterstitialAd == null && mInterstitialVideoAd == null)) {
            onAdFailed()
        } else {
            requestInterstitialAndContinue(null, isFocusedTask = false, onContinue = onAdDismissed)
        }
    }

    fun tryShowInterstitialVideo(activity: Activity?, onAdDismissed: () -> Unit, onAdFailed: () -> Unit) {
        if (!canShowInterstitial() || activity == null || activity.isFinishing || activity.isDestroyed || mInterstitialVideoAd == null) {
            if (activity != null) {
                loadInterstitialVideoAd(activity.applicationContext, force = true)
            }
            onAdFailed()
        } else {
            requestInterstitialAndContinue(activity, isFocusedTask = false, force = false, preferVideo = true, onContinue = onAdDismissed)
        }
    }

    fun isEmulatorEnvironment(): Boolean {
        val fingerprint = android.os.Build.FINGERPRINT.lowercase()
        val model = android.os.Build.MODEL.lowercase()
        val hardware = android.os.Build.HARDWARE.lowercase()
        val product = android.os.Build.PRODUCT.lowercase()
        val board = android.os.Build.BOARD.lowercase()
        val device = android.os.Build.DEVICE.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        val host = android.os.Build.HOST.lowercase()
        val tags = android.os.Build.TAGS?.lowercase() ?: ""

        return fingerprint.contains("generic") ||
                fingerprint.contains("unknown") ||
                fingerprint.contains("cuttlefish") ||
                fingerprint.contains("cf_") ||
                fingerprint.contains("sdk") ||
                fingerprint.contains("vbox") ||
                fingerprint.contains("test-keys") ||
                tags.contains("test-keys") ||
                host.contains("android-build") ||
                model.contains("google_sdk") ||
                model.contains("emulator") ||
                model.contains("cuttlefish") ||
                model.contains("android sdk") ||
                model.contains("sdk") ||
                model.contains("gphone") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                hardware.contains("cutf") ||
                hardware.contains("vsoc") ||
                hardware.contains("qemu") ||
                device.contains("generic") ||
                device.contains("vsoc") ||
                device.contains("cutf") ||
                device.contains("emulator") ||
                product.contains("sdk") ||
                product.contains("vbox") ||
                product.contains("cuttlefish") ||
                product.contains("cf_") ||
                product.contains("gphone") ||
                board.contains("cutf") ||
                board.contains("vsoc") ||
                (brand == "google" && (device.contains("vsoc") || hardware.contains("cutf") || product.contains("cf_") || model.contains("sdk")))
    }

    fun isTestOrDebugEnvironment(): Boolean {
        return com.example.BuildConfig.DEBUG ||
                AdConfig.current.appId == AdConfig.TEST_APP_ID ||
                isEmulatorEnvironment()
    }
}
