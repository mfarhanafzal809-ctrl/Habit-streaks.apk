package com.example.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.initialization.InitializationStatus

/**
 * Dedicated AdMob diagnostic logging utility to capture and dissect SDK initialization,
 * test-device verification, ad load events, failure codes (such as No Fill Error Code 3),
 * mediation adapter responses, and error domains.
 */
object AdLogUtility {
    private const val TAG = "AdMobDiagnostics"

    fun logInitializationStatus(status: InitializationStatus) {
        try {
            Log.i(TAG, "========================================")
            Log.i(TAG, " [MOBILE ADS INITIALIZED]")
            status.adapterStatusMap.forEach { (adapterClass, adapterStatus) ->
                Log.i(
                    TAG,
                    " Adapter: $adapterClass | State: ${adapterStatus.initializationState} | " +
                        "Latency: ${adapterStatus.latency}ms | Desc: ${adapterStatus.description}"
                )
            }
            Log.i(TAG, "========================================")
        } catch (_: Throwable) {}
    }

    fun logAdRequestDetails(context: Context, format: String, adUnitId: String, adRequest: AdRequest) {
        try {
            val isTest = adRequest.isTestDevice(context)
            Log.d(
                TAG,
                " -> Requesting $format | Unit: $adUnitId | isTestDevice: $isTest"
            )
        } catch (_: Throwable) {}
    }

    fun logAdLoaded(format: String, adUnitId: String) {
        try {
            Log.i(TAG, "========================================")
            Log.i(TAG, " [AD SUCCESS] $format Loaded Successfully")
            Log.i(TAG, " Ad Unit ID: $adUnitId")
            Log.i(TAG, " Timestamp: ${System.currentTimeMillis()}")
            Log.i(TAG, "========================================")
        } catch (_: Throwable) {}
    }

    fun logAdFailed(format: String, adUnitId: String, loadAdError: LoadAdError) {
        try {
            val errorCode = loadAdError.code
            val errorMessage = loadAdError.message
            val errorDomain = loadAdError.domain
            val responseInfo = loadAdError.responseInfo
            val cause = loadAdError.cause

            val errorDescription = when (errorCode) {
                AdRequest.ERROR_CODE_INTERNAL_ERROR -> "INTERNAL_ERROR (The ad server encountered an internal error)"
                AdRequest.ERROR_CODE_INVALID_REQUEST -> "INVALID_REQUEST (The ad request was invalid, e.g. incorrect ad unit ID)"
                AdRequest.ERROR_CODE_NETWORK_ERROR -> "NETWORK_ERROR (The ad request was unsuccessful due to network connectivity)"
                AdRequest.ERROR_CODE_NO_FILL -> "NO_FILL (The ad request was successful, but no ad inventory was returned)"
                AdRequest.ERROR_CODE_APP_ID_MISSING -> "APP_ID_MISSING (Application ID was missing in AndroidManifest.xml)"
                AdRequest.ERROR_CODE_REQUEST_ID_MISMATCH -> "REQUEST_ID_MISMATCH"
                AdRequest.ERROR_CODE_INVALID_AD_STRING -> "INVALID_AD_STRING"
                else -> "ERROR_CODE_$errorCode"
            }

            Log.w(TAG, "========================================")
            Log.w(TAG, " ❌ [AD FAILED TO LOAD] $format")
            Log.w(TAG, " Ad Unit ID  : $adUnitId")
            Log.w(TAG, " Error Code  : $errorCode -> $errorDescription")
            Log.w(TAG, " Error Msg   : $errorMessage")
            Log.w(TAG, " Error Domain: $errorDomain")
            if (cause != null) {
                Log.w(TAG, " Cause       : ${cause.code} - ${cause.message} (${cause.domain})")
            }
            if (responseInfo != null) {
                Log.w(TAG, " Response ID : ${responseInfo.responseId ?: "N/A"}")
                Log.w(TAG, " Adapter Resp: ${responseInfo.adapterResponses.size} adapters")
                responseInfo.adapterResponses.forEach { adapterResp ->
                    Log.w(
                        TAG,
                        "   * Adapter=${adapterResp.adapterClassName}, Latency=${adapterResp.latencyMillis}ms, " +
                            "AdError=${adapterResp.adError?.code}:${adapterResp.adError?.message}"
                    )
                }
                responseInfo.loadedAdapterResponseInfo?.let {
                    Log.w(TAG, " Loaded Adapter: ${it.adapterClassName} (Latency: ${it.latencyMillis}ms)")
                }
            }
            Log.w(TAG, "========================================")
        } catch (_: Throwable) {}
    }

    fun logFullScreenError(format: String, adError: AdError) {
        try {
            Log.e(TAG, "❌ [AD SHOW FAILED] $format: Code ${adError.code} - ${adError.message} (Domain: ${adError.domain})")
        } catch (_: Throwable) {}
    }
}
