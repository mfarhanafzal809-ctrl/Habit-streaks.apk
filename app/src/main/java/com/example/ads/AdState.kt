package com.example.ads

sealed class AdState {
    object Idle : AdState()
    object Loading : AdState()
    object Ready : AdState()
    object Showing : AdState()
    data class Failed(val reason: String, val timestamp: Long = System.currentTimeMillis()) : AdState()
    object Disposed : AdState()

    val isReady: Boolean get() = this is Ready
    val isLoading: Boolean get() = this is Loading
    val isShowing: Boolean get() = this is Showing
    val isFailed: Boolean get() = this is Failed
    val isDisposed: Boolean get() = this is Disposed
}

sealed class AdInitState {
    object NotInitialized : AdInitState()
    object Initializing : AdInitState()
    object Initialized : AdInitState()
    data class Failed(val reason: String) : AdInitState()

    val isInitialized: Boolean get() = this is Initialized
}

data class AdEligibilityPolicy(
    val minSessionAgeSeconds: Long = 10L,
    val minSecondsBetweenInterstitials: Long = 30L,
    val maxSessionInterstitials: Int = 5,
    val minMeaningfulActions: Int = 3
)
