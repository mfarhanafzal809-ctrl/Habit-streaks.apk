package com.example.ads

import com.example.BuildConfig

data class AdUnitConfig(
    val appId: String,
    val bannerAdUnitId: String,
    val interstitialAdUnitId: String,
    val rewardedAdUnitId: String,
    val rewardedInterstitialAdUnitId: String = "ca-app-pub-3940256099942544/5354046379",
    val interstitialVideoAdUnitId: String = "ca-app-pub-3940256099942544/8691691433",
    val adaptiveBannerAdUnitId: String = "ca-app-pub-3940256099942544/9214589741"
)

object AdConfig {
    // Official Google AdMob sample test ad unit IDs (up to date for 2026 / GMA SDK v24.x)
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_ADAPTIVE_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_INTERSTITIAL_VIDEO_AD_UNIT_ID = "ca-app-pub-3940256099942544/8691691433"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"

    val testConfig = AdUnitConfig(
        appId = TEST_APP_ID,
        bannerAdUnitId = TEST_BANNER_AD_UNIT_ID,
        interstitialAdUnitId = TEST_INTERSTITIAL_AD_UNIT_ID,
        rewardedAdUnitId = TEST_REWARDED_AD_UNIT_ID,
        rewardedInterstitialAdUnitId = TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID,
        interstitialVideoAdUnitId = TEST_INTERSTITIAL_VIDEO_AD_UNIT_ID,
        adaptiveBannerAdUnitId = TEST_ADAPTIVE_BANNER_AD_UNIT_ID
    )

    private var customProductionConfig: AdUnitConfig? = null

    fun setProductionConfig(config: AdUnitConfig?) {
        customProductionConfig = config
    }

    private fun resolveFromBuildConfig(): AdUnitConfig? {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            fun getSecret(name: String): String? {
                return try {
                    val field = clazz.getField(name)
                    val value = field.get(null) as? String
                    if (!value.isNullOrBlank() && value.startsWith("ca-app-pub-")) value else null
                } catch (_: Throwable) {
                    null
                }
            }
            val appId = getSecret("ADMOB_APP_ID")
            val bannerId = getSecret("ADMOB_BANNER_AD_UNIT_ID") ?: getSecret("BANNER_AD_UNIT_ID")
            val adaptiveBannerId = getSecret("ADMOB_ADAPTIVE_BANNER_AD_UNIT_ID")
            val interstitialId = getSecret("ADMOB_INTERSTITIAL_AD_UNIT_ID") ?: getSecret("INTERSTITIAL_AD_UNIT_ID")
            val rewardedId = getSecret("ADMOB_REWARDED_AD_UNIT_ID") ?: getSecret("REWARDED_AD_UNIT_ID")
            val rewardedInterstitialId = getSecret("ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID")

            if (appId != null || bannerId != null || interstitialId != null || rewardedId != null) {
                AdUnitConfig(
                    appId = appId ?: TEST_APP_ID,
                    bannerAdUnitId = bannerId ?: TEST_BANNER_AD_UNIT_ID,
                    interstitialAdUnitId = interstitialId ?: TEST_INTERSTITIAL_AD_UNIT_ID,
                    rewardedAdUnitId = rewardedId ?: TEST_REWARDED_AD_UNIT_ID,
                    rewardedInterstitialAdUnitId = rewardedInterstitialId ?: TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID,
                    interstitialVideoAdUnitId = TEST_INTERSTITIAL_VIDEO_AD_UNIT_ID,
                    adaptiveBannerAdUnitId = adaptiveBannerId ?: TEST_ADAPTIVE_BANNER_AD_UNIT_ID
                )
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    val current: AdUnitConfig
        get() {
            customProductionConfig?.let { return it }
            resolveFromBuildConfig()?.let { return it }
            return testConfig
        }
}
