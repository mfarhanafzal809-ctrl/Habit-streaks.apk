package com.example.ui.components

import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdConfig
import com.example.ads.AdInitState
import com.example.ads.AdLogUtility
import com.example.ads.AdManager
import com.example.util.NetworkUtils
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Real Google AdMob Banner Ad component.
 * Loads and displays live Google AdMob ads using AdConfig.current.bannerAdUnitId
 * (with fallback to adaptive banner unit).
 * Keeps AdView VISIBLE during loading so Android measures valid non-zero dimensions
 * required by Google Mobile Ads SDK, and hides/pauses only when Ad-Free mode is active.
 */
@Composable
fun AdBannerPlaceholder(
    modifier: Modifier = Modifier
) {
    // If ad-free mode is active or in preview inspection, do not display banner
    if (AdManager.isAdFreeActive() || LocalInspectionMode.current) {
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val initState by AdManager.initState.collectAsStateWithLifecycle()
    val isConsentResolved by AdManager.isConsentResolved.collectAsStateWithLifecycle()
    val networkStatus by remember {
        com.example.util.ConnectivityMonitor.getInstance(context).networkStatus
    }.collectAsStateWithLifecycle()

    var isAdLoaded by remember { mutableStateOf(false) }
    var currentAdUnitId by remember { mutableStateOf(AdConfig.current.adaptiveBannerAdUnitId) }
    var hasSwitchedFallbackUnit by remember { mutableStateOf(false) }
    var retryCount by remember(currentAdUnitId) { mutableIntStateOf(0) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    // When connectivity returns, reset retryCount so banner can recover cleanly
    LaunchedEffect(networkStatus) {
        if (networkStatus is com.example.util.NetworkStatus.ValidatedOnline && !isAdLoaded) {
            retryCount = 0
            hasSwitchedFallbackUnit = false
        }
    }

    // When MobileAds is initialized, consent is resolved, and network is online, load the banner ad
    LaunchedEffect(initState, isConsentResolved, networkStatus, adViewRef, currentAdUnitId) {
        if (initState is AdInitState.Initialized &&
            isConsentResolved &&
            !isAdLoaded &&
            NetworkUtils.isNetworkAvailable(context) &&
            AdManager.canRequestAds()
        ) {
            adViewRef?.let { adView ->
                try {
                    val isAdaptive = (currentAdUnitId == AdConfig.current.adaptiveBannerAdUnitId)
                    val label = if (isAdaptive) "Adaptive Banner Ad" else "Standard Banner Ad"
                    val adRequest = AdRequest.Builder().build()
                    AdLogUtility.logAdRequestDetails(context, label, currentAdUnitId, adRequest)
                    adView.loadAd(adRequest)
                } catch (t: Throwable) {
                    Log.w("AdBanner", "Error requesting banner ad: ${t.message}")
                }
            }
        }
    }

    // Manage AdView lifecycle (pause, resume, destroy)
    DisposableEffect(lifecycleOwner, adViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adViewRef?.pause()
                Lifecycle.Event.ON_RESUME -> {
                    adViewRef?.resume()
                    if (!isAdLoaded &&
                        isConsentResolved &&
                        AdManager.canRequestAds() &&
                        NetworkUtils.isNetworkAvailable(context) &&
                        AdManager.initState.value is AdInitState.Initialized
                    ) {
                        retryCount = 0
                        try {
                            adViewRef?.loadAd(AdRequest.Builder().build())
                        } catch (_: Throwable) {}
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        adViewRef?.destroy()
                    } catch (_: Throwable) {}
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                adViewRef?.destroy()
            } catch (_: Throwable) {}
            adViewRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (isAdLoaded) 4.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        // Real Google Mobile Ads SDK AdView
        androidx.compose.runtime.key(currentAdUnitId) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                factory = { ctx ->
                    try {
                        AdView(ctx).apply {
                            val isAdaptive = (currentAdUnitId == AdConfig.current.adaptiveBannerAdUnitId)
                            if (isAdaptive) {
                                val displayMetrics = ctx.resources.displayMetrics
                                val widthDp = (displayMetrics.widthPixels / displayMetrics.density)
                                    .toInt()
                                    .coerceAtLeast(320)
                                val adaptiveSize = try {
                                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp)
                                } catch (_: Throwable) {
                                    AdSize.BANNER
                                }
                                setAdSize(adaptiveSize)
                            } else {
                                setAdSize(AdSize.BANNER)
                            }

                            adUnitId = currentAdUnitId
                            // IMPORTANT: Must remain VISIBLE so View measurement provides non-zero dimensions
                            visibility = View.VISIBLE
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    super.onAdLoaded()
                                    isAdLoaded = true
                                    retryCount = 0
                                    AdManager.setBannerLoaded(true)
                                    val formatLabel = if (isAdaptive) "Adaptive Banner Ad" else "Standard Banner Ad"
                                    AdLogUtility.logAdLoaded(formatLabel, adUnitId)
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    Log.d("AdBanner", "Banner impression recorded ($adUnitId)")
                                }

                                override fun onAdClicked() {
                                    super.onAdClicked()
                                    Log.d("AdBanner", "Banner ad clicked ($adUnitId)")
                                }

                                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                    super.onAdFailedToLoad(loadAdError)
                                    isAdLoaded = false
                                    AdManager.setBannerLoaded(false)
                                    val formatLabel = if (isAdaptive) "Adaptive Banner Ad" else "Standard Banner Ad"
                                    AdLogUtility.logAdFailed(formatLabel, adUnitId, loadAdError)

                                    // If adaptive banner fails with No Fill (Error Code 3), fall back once to standard banner unit
                                    if (!hasSwitchedFallbackUnit && loadAdError.code == AdRequest.ERROR_CODE_NO_FILL) {
                                        hasSwitchedFallbackUnit = true
                                        postDelayed({
                                            if (!isAdLoaded && AdManager.canRequestAds() && isAttachedToWindow) {
                                                currentAdUnitId = if (isAdaptive) {
                                                    AdConfig.current.bannerAdUnitId
                                                } else {
                                                    AdConfig.current.adaptiveBannerAdUnitId
                                                }
                                            }
                                        }, 1000L)
                                    } else if (retryCount < 3) {
                                        retryCount++
                                        val delayMs = retryCount * 4000L
                                        postDelayed({
                                            if (!isAdLoaded &&
                                                AdManager.canRequestAds() &&
                                                NetworkUtils.isNetworkAvailable(ctx) &&
                                                isAttachedToWindow
                                            ) {
                                                try {
                                                    loadAd(AdRequest.Builder().build())
                                                } catch (_: Throwable) {}
                                            }
                                        }, delayMs)
                                    }
                                }
                            }
                            adViewRef = this
                        }
                    } catch (t: Throwable) {
                        Log.e("AdBanner", "Could not initialize AdView", t)
                        View(ctx)
                    }
                },
                update = { view ->
                    if (view is AdView) {
                        if (AdManager.isAdFreeActive()) {
                            view.visibility = View.GONE
                            view.pause()
                        } else {
                            view.visibility = View.VISIBLE
                            view.resume()
                        }
                    }
                },
                onRelease = { view ->
                    if (view is AdView) {
                        try {
                            view.destroy()
                        } catch (_: Throwable) {}
                        if (adViewRef === view) {
                            adViewRef = null
                        }
                    }
                }
            )
        }
    }
}
