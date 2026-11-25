package com.example.native

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.os.UserManagerCompat
import androidx.glance.appwidget.updateAll
import com.example.widget.HabitWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        private const val WAKE_LOCK_TIMEOUT_MS = 15_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_USER_UNLOCKED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.TIME_SET" ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            if (!UserManagerCompat.isUserUnlocked(context)) {
                Log.i(TAG, "User credential storage still locked ($action); deferring reminder reconciliation until unlock.")
                return
            }

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = try {
                powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HabitStreaks:BootReceiver")?.apply {
                    setReferenceCounted(false)
                    acquire(WAKE_LOCK_TIMEOUT_MS)
                }
            } catch (_: Throwable) {
                null
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val scheduler = AlarmScheduler(context)
                    // Suspend and await until all alarms are fully scheduled/reconciled
                    scheduler.reconcileAllReminders()

                    // Refresh widgets following reboot, permission change, or system time change
                    try {
                        HabitWidget().updateAll(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to refresh widget in BootReceiver", e)
                    }

                    Log.d(TAG, "Successfully reconciled all habit reminders and widgets for action: $action")
                } catch (e: Exception) {
                    Log.e(TAG, "Error reconciling reminders in BootReceiver for action: $action", e)
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock.release()
                        }
                    } catch (_: Throwable) {}
                    pendingResult.finish()
                }
            }
        }
    }
}
