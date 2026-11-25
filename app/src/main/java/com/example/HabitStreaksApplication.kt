package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.example.ads.AdManager
import java.lang.ref.WeakReference

class HabitStreaksApplication : Application() {

    companion object {
        private const val TAG = "HabitStreaksApp"
        private var currentActivityRef = WeakReference<Activity?>(null)

        val currentActivity: Activity?
            get() = currentActivityRef.get()
    }

    override fun onCreate() {
        super.onCreate()
        com.example.util.EnterpriseErrorHandler.initGlobalHandler()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private fun isInternalAdActivity(activity: Activity): Boolean {
                return activity.javaClass.name.contains("AdActivity")
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (!isInternalAdActivity(activity)) {
                    currentActivityRef = WeakReference(activity)
                    AdManager.updateCurrentActivity(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {
                if (!isInternalAdActivity(activity)) {
                    currentActivityRef = WeakReference(activity)
                    AdManager.updateCurrentActivity(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                if (!isInternalAdActivity(activity)) {
                    currentActivityRef = WeakReference(activity)
                    AdManager.updateCurrentActivity(activity)
                }
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                if (!isInternalAdActivity(activity) && currentActivityRef.get() == activity && activity.isFinishing) {
                    currentActivityRef = WeakReference(null)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (!isInternalAdActivity(activity) && currentActivityRef.get() == activity) {
                    currentActivityRef = WeakReference(null)
                }
            }
        })

        // Register dynamic test device ID and initialize Google Mobile Ads SDK once via AdManager
        // (ads wait for UMP consent resolution + Activity context in MainActivity before loading)
        getDynamicHashedDeviceId()?.let { dynamicId ->
            Log.i(TAG, "Registering dynamic AdMob test device ID: $dynamicId")
            AdManager.addTestDeviceId(dynamicId, applyNow = false)
        }
        AdManager.initialize(this, preloadAds = false)
    }

    private fun getDynamicHashedDeviceId(): String? {
        return try {
            val androidId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            if (androidId.isNullOrEmpty()) return null
            val digest = java.security.MessageDigest.getInstance("MD5")
            digest.update(androidId.toByteArray(Charsets.UTF_8))
            val bytes = digest.digest()
            val sb = StringBuilder()
            for (b in bytes) {
                val hex = Integer.toHexString(0xFF and b.toInt())
                if (hex.length < 2) sb.append('0')
                sb.append(hex)
            }
            sb.toString().uppercase()
        } catch (_: Throwable) {
            null
        }
    }
}
