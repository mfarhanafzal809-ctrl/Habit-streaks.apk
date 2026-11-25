package com.example.native

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.HabitStreaksApplication
import com.example.data.local.HabitDatabase
import com.example.domain.StreakEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Foreground Service guaranteeing exact, unthrottled alarm ringing and vibration
 * on Android's USAGE_ALARM stream until the user marks done, snoozes, dismisses,
 * or the 60-second safety timeout completes.
 */
class AlarmRingingService : Service() {

    companion object {
        private const val TAG = "AlarmRingingService"
        const val ACTION_START_ALARM = "com.example.habitstreak.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.habitstreak.ACTION_STOP_ALARM"
        const val ACTION_ALARM_STATE_CHANGED = "com.example.habitstreak.ACTION_ALARM_STATE_CHANGED"

        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
        const val EXTRA_PLAY_SOUND = "extra_play_sound"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_RINGING = "extra_ringing"

        private const val ALARM_TIMEOUT_MS = 60_000L
        private const val WAKE_LOCK_TIMEOUT_MS = 75_000L
        private const val NOTIFICATION_ID_OFFSET = 50000

        @Volatile
        var activeAlarmHabitId: Long? = null
            private set

        fun start(
            context: Context,
            habitId: Long,
            habitName: String,
            habitColor: String,
            currentStreak: Int,
            ringtoneUri: String?,
            playSound: Boolean,
            vibrate: Boolean,
            isTestAlarm: Boolean = false
        ) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(NotificationHelper.EXTRA_HABIT_ID, habitId)
                putExtra(NotificationHelper.EXTRA_HABIT_NAME, habitName)
                putExtra(NotificationHelper.EXTRA_HABIT_COLOR, habitColor)
                putExtra(NotificationHelper.EXTRA_HABIT_STREAK, currentStreak)
                putExtra(EXTRA_RINGTONE_URI, ringtoneUri)
                putExtra(EXTRA_PLAY_SOUND, playSound)
                putExtra(EXTRA_VIBRATE, vibrate)
                putExtra(NotificationHelper.EXTRA_IS_TEST_ALARM, isTestAlarm)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context, habitId: Long = -1L) {
            val active = activeAlarmHabitId
            if (habitId != -1L && active != null && active != habitId) {
                return
            }
            activeAlarmHabitId = null
            try {
                context.stopService(Intent(context, AlarmRingingService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping AlarmRingingService", e)
            }
            broadcastAlarmState(context, ringing = false, habitId = habitId)
        }

        private fun broadcastAlarmState(context: Context, ringing: Boolean, habitId: Long) {
            try {
                val stateIntent = Intent(ACTION_ALARM_STATE_CHANGED).apply {
                    setPackage(context.packageName)
                    putExtra(EXTRA_RINGING, ringing)
                    putExtra(NotificationHelper.EXTRA_HABIT_ID, habitId)
                }
                context.sendBroadcast(stateIntent)
            } catch (_: Throwable) {}
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var soundPlayer: SoundPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var timeoutJob: Job? = null

    private var currentHabitId: Long = -1L
    private var currentHabitName: String = "Habit Reminder"
    private var currentStreak: Int = 0
    private var currentIsTestAlarm: Boolean = false
    private var isStoppedCleanly: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        soundPlayer = SoundPlayer(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP_ALARM) {
            stopAlarmAndSelf(postMissedNotification = false)
            return START_NOT_STICKY
        }

        val habitId = intent.getLongExtra(NotificationHelper.EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(NotificationHelper.EXTRA_HABIT_NAME) ?: "Habit Reminder"
        val habitColor = intent.getStringExtra(NotificationHelper.EXTRA_HABIT_COLOR) ?: "#6366F1"
        val streak = intent.getIntExtra(NotificationHelper.EXTRA_HABIT_STREAK, 0)
        val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI)
        val playSound = intent.getBooleanExtra(EXTRA_PLAY_SOUND, true)
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val isTestAlarm = intent.getBooleanExtra(NotificationHelper.EXTRA_IS_TEST_ALARM, false)

        if (habitId == -1L && !isTestAlarm) {
            stopAlarmAndSelf(postMissedNotification = false)
            return START_NOT_STICKY
        }

        // If another real habit alarm is already actively ringing at the same minute,
        // preserve it with a persistent high-priority notification so it isn't silently dropped.
        val displacedHabitId = currentHabitId
        val displacedStreak = currentStreak
        val displacedWasTest = currentIsTestAlarm
        if (!isStoppedCleanly && displacedHabitId > 0 && !displacedWasTest && displacedHabitId != habitId) {
            postMissedOrConcurrentNotificationAsync(displacedHabitId, displacedStreak)
        }

        isStoppedCleanly = false
        currentHabitId = habitId
        currentHabitName = habitName
        currentStreak = streak
        currentIsTestAlarm = isTestAlarm
        activeAlarmHabitId = habitId

        acquireWakeLock()

        val notificationHelper = NotificationHelper(applicationContext)
        val notification = notificationHelper.buildActiveAlarmNotification(
            habitId = habitId,
            habitName = habitName,
            habitColor = habitColor,
            currentStreak = streak,
            isTestAlarm = isTestAlarm
        )
        val notificationId = habitId.toInt() + NOTIFICATION_ID_OFFSET

        try {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
            ServiceCompat.startForeground(this, notificationId, notification, fgsType)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enter foreground mode for alarm", e)
            stopAlarmAndSelf(postMissedNotification = !isTestAlarm)
            return START_NOT_STICKY
        }

        broadcastAlarmState(applicationContext, ringing = true, habitId = habitId)

        // Start looped USAGE_ALARM audio & rhythmic vibration
        try {
            soundPlayer?.playAlarmLoop(ringtoneUri, playSound = playSound, vibrate = vibrate)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm loop", e)
        }

        // Launch full-screen UI directly when in foreground or unlocked (lockscreen relies on fullScreenIntent)
        launchAlertActivityIfAllowed(
            habitId = habitId,
            habitName = habitName,
            habitColor = habitColor,
            streak = streak,
            isTestAlarm = isTestAlarm
        )

        // Safety auto-stop timer so alarm does not loop indefinitely if unattended
        timeoutJob?.cancel()
        timeoutJob = serviceScope.launch {
            delay(ALARM_TIMEOUT_MS)
            Log.i(TAG, "Alarm auto-timeout reached for habitId=$habitId ($habitName)")
            stopAlarmAndSelf(postMissedNotification = !isTestAlarm)
        }

        return START_NOT_STICKY
    }

    private fun launchAlertActivityIfAllowed(
        habitId: Long,
        habitName: String,
        habitColor: String,
        streak: Int,
        isTestAlarm: Boolean
    ) {
        val canDirectLaunch = isTestAlarm || HabitStreaksApplication.currentActivity != null
        if (!canDirectLaunch) return

        try {
            val alertIntent = Intent(applicationContext, AlarmAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(NotificationHelper.EXTRA_HABIT_ID, habitId)
                putExtra(NotificationHelper.EXTRA_HABIT_NAME, habitName)
                putExtra(NotificationHelper.EXTRA_HABIT_COLOR, habitColor)
                putExtra(NotificationHelper.EXTRA_HABIT_STREAK, streak)
                putExtra(NotificationHelper.EXTRA_IS_TEST_ALARM, isTestAlarm)
            }
            startActivity(alertIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Direct AlertActivity launch blocked by OS (relying on fullScreenIntent)", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HabitStreaks:AlarmRingingWakeLock").apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to acquire AlarmRingingService WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Throwable) {}
        wakeLock = null
    }

    private fun stopAlarmAndSelf(postMissedNotification: Boolean) {
        if (isStoppedCleanly) return
        isStoppedCleanly = true

        timeoutJob?.cancel()
        timeoutJob = null
        activeAlarmHabitId = null

        try {
            soundPlayer?.stopAlarmLoop()
        } catch (_: Throwable) {}

        broadcastAlarmState(applicationContext, ringing = false, habitId = currentHabitId)

        if (postMissedNotification && currentHabitId > 0) {
            postMissedOrConcurrentNotificationAsync(currentHabitId, currentStreak)
        }

        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {}
        releaseWakeLock()
        stopSelf()
    }

    override fun onDestroy() {
        timeoutJob?.cancel()
        activeAlarmHabitId = null
        try {
            soundPlayer?.stopAlarmLoop()
            soundPlayer?.release()
        } catch (_: Throwable) {}
        soundPlayer = null
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun postMissedOrConcurrentNotificationAsync(habitId: Long, streak: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = HabitDatabase.getInstance(applicationContext)
                val habit = db.habitDao().getHabitById(habitId)
                val todayStr = StreakEngine.formatDate(LocalDate.now())
                val isDone = db.habitCompletionDao().getCompletion(habitId, todayStr) != null
                if (habit != null && !habit.isArchived && !isDone) {
                    val globalVibrate = db.appSettingDao().getSettingValue("global_vibration")?.toBoolean() ?: true
                    NotificationHelper(applicationContext).showHabitNotification(
                        habit = habit,
                        currentStreak = streak,
                        playChannelAlert = false,
                        globalVibrationEnabled = globalVibrate
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error posting missed/concurrent reminder notification", e)
            }
        }
    }
}
