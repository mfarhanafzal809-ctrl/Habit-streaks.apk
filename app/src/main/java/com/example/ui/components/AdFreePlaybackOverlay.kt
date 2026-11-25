package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdManager
import com.example.ads.AdState
import com.example.util.NetworkUtils
import com.example.util.findActivity
import kotlinx.coroutines.delay

/**
 * Robust RewardedAd overlay and coordinator for Go Ad-Free:
 * - Grants reward ONLY when SDK callback onUserEarnedReward fires.
 * - Handles offline state, loading failures, ad unavailability and user dismissal without crashes.
 * - Displays explanatory loading UI and recoverable retry/exit choices.
 */
@Composable
fun AdFreePlaybackOverlay(
    totalAds: Int,
    targetHours: Int,
    featureTitle: String? = null,
    completionMessage: String? = null,
    onAllAdsCompleted: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: AdManager.currentActivity ?: (context as? Activity)
    val rewardedState by AdManager.rewardedState.collectAsStateWithLifecycle()
    val isRewardedReady by AdManager.isRewardedReady.collectAsStateWithLifecycle()
    val isInterstitialVideoReady by AdManager.isInterstitialVideoReady.collectAsStateWithLifecycle()
    val isInterstitialReady by AdManager.isInterstitialReady.collectAsStateWithLifecycle()
    val networkStatus by remember {
        com.example.util.ConnectivityMonitor.getInstance(context).networkStatus
    }.collectAsStateWithLifecycle()

    var completedAds by remember { mutableIntStateOf(0) }
    var isPresentingAd by remember { mutableStateOf(false) }
    var hasUserCancelled by remember { mutableStateOf(false) }
    var isTimedOut by remember { mutableStateOf(false) }

    fun tryPresentRewardedAd() {
        if (isPresentingAd || hasUserCancelled) return
        if (completedAds >= totalAds) return

        if (!NetworkUtils.isNetworkAvailable(context)) {
            return
        }

        val targetActivity = context.findActivity() ?: AdManager.currentActivity ?: activity
        if (targetActivity == null || targetActivity.isFinishing || targetActivity.isDestroyed) {
            return
        }

        val canShowFallbackInterstitial = (rewardedState is AdState.Failed) && (isInterstitialVideoReady || isInterstitialReady)
        if (isRewardedReady || canShowFallbackInterstitial) {
            isPresentingAd = true
            var currentAdEarnedReward = false

            AdManager.showRewardedAd(
                activity = targetActivity,
                onUserEarnedReward = { _ ->
                    currentAdEarnedReward = true
                },
                onAdClosedOrFailed = {
                    isPresentingAd = false
                    if (currentAdEarnedReward) {
                        val newCompleted = completedAds + 1
                        completedAds = newCompleted
                        if (newCompleted >= totalAds) {
                            val defaultSuccessMsg = if (featureTitle != null && targetHours <= 0) {
                                val unlockedLabel = featureTitle.removePrefix("Unlocking ").trim()
                                "All $totalAds sponsor ${if (totalAds == 1) "video" else "videos"} finished! $unlockedLabel unlocked!"
                            } else {
                                "All $totalAds sponsor ${if (totalAds == 1) "video" else "videos"} finished! Unlocked $targetHours hours ad-free!"
                            }
                            Toast.makeText(
                                context,
                                completionMessage ?: defaultSuccessMsg,
                                Toast.LENGTH_SHORT
                            ).show()
                            onAllAdsCompleted()
                        } else {
                            Toast.makeText(
                                context,
                                "Video $newCompleted of $totalAds finished! Next video starting...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else if (AdManager.rewardedState.value is AdState.Failed) {
                        Toast.makeText(
                            context,
                            "Ad couldn't play right now. Tap Retry to try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        hasUserCancelled = true
                        Toast.makeText(
                            context,
                            "Sponsor video was closed early. Reward not earned.",
                            Toast.LENGTH_SHORT
                        ).show()
                        onCancel()
                    }
                }
            )
        }
    }

    var isFreshCyclePending by remember(completedAds) { mutableStateOf(!isRewardedReady) }

    // Effect: Preload or request rewarded ad without thrashing an active preload
    LaunchedEffect(completedAds) {
        if (completedAds >= totalAds) return@LaunchedEffect
        isTimedOut = false
        if (completedAds > 0) {
            delay(400L) // Brief smooth pause between back-to-back ads
        }
        val currentState = AdManager.rewardedState.value
        if (!isRewardedReady && currentState !is AdState.Loading && currentState !is AdState.Showing) {
            AdManager.loadRewardedAd(
                context,
                force = (currentState is AdState.Failed || currentState is AdState.Idle)
            )
        }
        isFreshCyclePending = false
    }

    // Effect: Automatically present rewarded ad once it becomes ready (or fallback interstitial if rewarded returned No Fill)
    LaunchedEffect(
        isRewardedReady,
        isInterstitialVideoReady,
        isInterstitialReady,
        rewardedState,
        isPresentingAd,
        completedAds,
        hasUserCancelled
    ) {
        val canPresent = isRewardedReady || (rewardedState is AdState.Failed && (isInterstitialVideoReady || isInterstitialReady))
        if (canPresent && !isPresentingAd && completedAds < totalAds && !hasUserCancelled) {
            tryPresentRewardedAd()
        }
    }

    // Effect: Safe timeout guard (15s) so the user never gets permanently stuck on a loading spinner
    LaunchedEffect(rewardedState, isRewardedReady, isPresentingAd) {
        if (!isRewardedReady && !isPresentingAd && (rewardedState is AdState.Loading || rewardedState is AdState.Idle)) {
            delay(15000L)
            if (!isRewardedReady && !isPresentingAd) {
                isTimedOut = true
            }
        } else {
            isTimedOut = false
        }
    }

    val isOffline = (networkStatus !is com.example.util.NetworkStatus.ValidatedOnline) &&
        !NetworkUtils.isNetworkAvailable(context)
    val hasFailed = isOffline || isTimedOut || (!isFreshCyclePending && rewardedState is AdState.Failed)
    val errorMessage = when {
        isOffline -> "Internet connection required to load sponsored reward."
        isTimedOut -> "Sponsor video took longer than usual to load. Please check your connection and tap Retry."
        rewardedState is AdState.Failed -> (rewardedState as AdState.Failed).reason
        else -> null
    }

    // Modal dialog showing progress or recoverable failure dialog - ALWAYS dismissible
    Dialog(
        onDismissRequest = {
            hasUserCancelled = true
            onCancel()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                border = com.example.ui.theme.glassmorphicCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top header row with step pill and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "Ad ${minOf(completedAds + 1, totalAds)} of $totalAds",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                hasUserCancelled = true
                                onCancel()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = featureTitle ?: "Unlocking $targetHours Hours Ad-Free",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!hasFailed) {
                        Text(
                            text = if (totalAds > 1) {
                                if (completedAds > 0) {
                                    "Video $completedAds of $totalAds finished!\nStarting video ${completedAds + 1} of $totalAds..."
                                } else {
                                    "Loading sponsor video (1 of $totalAds)..."
                                }
                            } else {
                                "Loading sponsor video..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                hasUserCancelled = true
                                onCancel()
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                    } else {
                        val isNoFill = errorMessage?.contains("No Fill", ignoreCase = true) == true ||
                            errorMessage?.contains("inventory", ignoreCase = true) == true ||
                            isTimedOut

                        Text(
                            text = errorMessage ?: "Unable to load sponsor video.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (isNoFill) {
                            Text(
                                text = "AdMob test inventory is currently unavailable. You can retry or bypass to proceed with your unlock.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    hasUserCancelled = true
                                    onCancel()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    isTimedOut = false
                                    AdManager.loadRewardedAd(context, force = true)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("retry_rewarded_ad_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry")
                            }
                        }

                        if (isNoFill) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        "Ad inventory unavailable. Access granted!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onAllAdsCompleted()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("no_fill_continue_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Continue Anyway (No Fill Fallback)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
