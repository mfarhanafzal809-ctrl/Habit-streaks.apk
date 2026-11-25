package com.example.native

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.MainActivity
import com.example.data.model.Habit
import com.example.data.model.RingMode
import java.io.File

class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        const val CHANNEL_ID_SOUND_VIBRATE = "habit_streaks_sound_vibrate_v2"
        const val CHANNEL_ID_VIBRATE_ONLY = "habit_streaks_vibrate_only_v2"
        const val CHANNEL_ID_SILENT = "habit_streaks_silent_v2"
        const val CHANNEL_ID_MISSED_REMINDER = "habit_streaks_missed_high_v2"
        const val CHANNEL_ID_ALARM_RINGING = "habit_streaks_alarm_ringing_v2"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
        const val EXTRA_HABIT_COLOR = "extra_habit_color"
        const val EXTRA_HABIT_STREAK = "extra_habit_streak"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_IS_TEST_ALARM = "extra_is_test_alarm"
        const val EXTRA_IS_SNOOZE_TRIGGER = "extra_is_snooze_trigger"
        const val ACTION_MARK_DONE = "com.example.habitstreak.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.example.habitstreak.ACTION_SNOOZE"
        const val ACTION_DISMISS_ALARM = "com.example.habitstreak.ACTION_DISMISS_ALARM"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    fun canUseFullScreenIntent(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                notificationManager.canUseFullScreenIntent()
            } catch (_: Throwable) {
                false
            }
        } else {
            true
        }
    }

    /**
     * Resolves a stored ringtone URI or built-in key to a NotificationManager-readable URI.
     * Converts private file:// URIs to FileProvider content:// URIs with read permission for SystemUI,
     * falling back safely to the system alarm/notification ringtone if the file cannot be shared.
     */
    fun resolveNotificationSoundUri(rawUriString: String?): Uri? {
        val defaultAlarmOrNotif = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (rawUriString.isNullOrBlank() || rawUriString == "default" || rawUriString == "chime") {
            return defaultAlarmOrNotif
        }

        fun toShareableContentUri(file: File): Uri? {
            if (!file.exists() || file.length() <= 0L) return null
            return try {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                try {
                    context.grantUriPermission(
                        "com.android.systemui",
                        contentUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Throwable) {}
                contentUri
            } catch (e: Exception) {
                Log.w(TAG, "Unable to expose sound file via FileProvider: ${file.absolutePath}", e)
                null
            }
        }

        return try {
            val parsed = Uri.parse(rawUriString)
            when (parsed.scheme) {
                "file" -> {
                    val file = File(parsed.path ?: "")
                    toShareableContentUri(file) ?: run {
                        val fallbackBuiltin = File(context.filesDir, "musical_ringtones/${file.nameWithoutExtension}.wav")
                        toShareableContentUri(fallbackBuiltin) ?: defaultAlarmOrNotif
                    }
                }
                "content", "android.resource" -> {
                    try {
                        context.grantUriPermission(
                            "com.android.systemui",
                            parsed,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Throwable) {}
                    parsed
                }
                else -> {
                    val key = rawUriString.removePrefix("musical_").removeSuffix(".wav")
                    val builtinFile = File(context.filesDir, "musical_ringtones/$key.wav")
                    toShareableContentUri(builtinFile) ?: defaultAlarmOrNotif
                }
            }
        } catch (_: Throwable) {
            defaultAlarmOrNotif
        }
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Remove legacy v1 channels so stale OS-cached settings don't duplicate or mute alerts
            listOf(
                "habit_streaks_sound_vibrate",
                "habit_streaks_vibrate_only",
                "habit_streaks_silent",
                "habit_streaks_alarm_ringing"
            ).forEach { legacyId ->
                try {
                    if (notificationManager.getNotificationChannel(legacyId) != null) {
                        notificationManager.deleteNotificationChannel(legacyId)
                    }
                } catch (_: Throwable) {}
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // 1. Active Alarm Ringing Channel (silent channel because AlarmRingingService manages looped USAGE_ALARM audio/vibration directly without double-ringing)
            val activeAlarmChannel = NotificationChannel(
                CHANNEL_ID_ALARM_RINGING,
                "Active Habit Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen and lock-screen active habit alarm alerts"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setShowBadge(true)
            }

            // 2. Sound + Vibrate Channel (used for missed/fallback notifications)
            val soundChannel = NotificationChannel(
                CHANNEL_ID_SOUND_VIBRATE,
                "Habit Reminders (Sound & Vibrate)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders with sound and vibration to keep your streaks alive"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(defaultSoundUri, audioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setShowBadge(true)
            }

            // 3. Vibrate Only Channel (IMPORTANCE_HIGH for heads-up alerts)
            val vibrateChannel = NotificationChannel(
                CHANNEL_ID_VIBRATE_ONLY,
                "Habit Reminders (Vibrate Only)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Vibration-only reminders for habits"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            // 4. Silent Channel
            val silentChannel = NotificationChannel(
                CHANNEL_ID_SILENT,
                "Habit Reminders (Silent)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Discreet silent notifications for habits"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            }

            // 5. High-Priority Silent Missed/Concurrent Alarm Channel (keeps missed alerts prominent without duplicating audio)
            val missedReminderChannel = NotificationChannel(
                CHANNEL_ID_MISSED_REMINDER,
                "Habit Reminders (Missed & Concurrent)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-visibility notifications for timed-out or concurrent habit alarms"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(activeAlarmChannel)
            notificationManager.createNotificationChannel(soundChannel)
            notificationManager.createNotificationChannel(vibrateChannel)
            notificationManager.createNotificationChannel(silentChannel)
            notificationManager.createNotificationChannel(missedReminderChannel)
        }
    }

    /**
     * Builds the high-priority ongoing Alarm notification used by AlarmRingingService.
     */
    fun buildActiveAlarmNotification(
        habitId: Long,
        habitName: String,
        habitColor: String,
        currentStreak: Int,
        isTestAlarm: Boolean = false
    ): Notification {
        val alertIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_HABIT_COLOR, habitColor)
            putExtra(EXTRA_HABIT_STREAK, currentStreak)
            putExtra(EXTRA_IS_TEST_ALARM, isTestAlarm)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            habitId.toInt() + 25000,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_IS_TEST_ALARM, isTestAlarm)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt() + 10000,
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_SNOOZE_MINUTES, 10)
            putExtra(EXTRA_IS_TEST_ALARM, isTestAlarm)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt() + 15000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_IS_TEST_ALARM, isTestAlarm)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt() + 20000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val streakText = if (currentStreak > 0) {
            "Protect your $currentStreak-day streak! Tap to respond."
        } else {
            "Time for your scheduled habit alarm."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALARM_RINGING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $habitName")
            .setContentText(streakText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setContentIntent(fullScreenPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, if (isTestAlarm) "✓ Test Done" else "✓ Done", markDonePendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "⏰ Snooze 10m", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        if (canUseFullScreenIntent()) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        return builder.build()
    }

    fun showHabitNotification(
        habit: Habit,
        currentStreak: Int = 0,
        playChannelAlert: Boolean = true,
        globalVibrationEnabled: Boolean = true
    ) {
        val effectiveVibrate = habit.vibrateEnabled && globalVibrationEnabled
        val channelId = when {
            !playChannelAlert -> if (habit.ringMode == RingMode.SILENT) CHANNEL_ID_SILENT else CHANNEL_ID_MISSED_REMINDER
            habit.ringMode == RingMode.SILENT -> CHANNEL_ID_SILENT
            habit.ringMode == RingMode.VIBRATE_ONLY -> if (effectiveVibrate) CHANNEL_ID_VIBRATE_ONLY else CHANNEL_ID_SILENT
            habit.ringMode == RingMode.SOUND_AND_VIBRATE -> {
                val uriStr = habit.ringtoneUri ?: "default"
                if (uriStr == "default" || habit.ringtoneUri.isNullOrBlank()) {
                    CHANNEL_ID_SOUND_VIBRATE
                } else {
                    val soundUri = resolveNotificationSoundUri(habit.ringtoneUri)
                    val safeUriHash = uriStr.hashCode().toString().replace("-", "N")
                    val vibFlag = if (effectiveVibrate) "v1" else "v0"
                    val dynamicChannelId = "habit_channel_v2_${habit.id}_${safeUriHash}_$vibFlag"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        cleanupHabitNotificationChannels(habit.id, keepChannelId = dynamicChannelId)
                        val existingChannel = notificationManager.getNotificationChannel(dynamicChannelId)
                        if (existingChannel == null) {
                            val audioAttributes = AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .build()

                            val newChannel = NotificationChannel(
                                dynamicChannelId,
                                "Habit Alert: ${habit.name}",
                                NotificationManager.IMPORTANCE_HIGH
                            ).apply {
                                description = "Custom sound reminder for ${habit.name}"
                                enableVibration(effectiveVibrate)
                                if (effectiveVibrate) {
                                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                                }
                                setSound(soundUri, audioAttributes)
                                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                                setBypassDnd(true)
                                setShowBadge(true)
                            }
                            notificationManager.createNotificationChannel(newChannel)
                        }
                    }
                    dynamicChannelId
                }
            }
            else -> CHANNEL_ID_SILENT
        }

        // Tap content intent -> Open MainActivity
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_HABIT_ID, habit.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            habit.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark Done
        val markDoneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_HABIT_ID, habit.id)
            putExtra(EXTRA_HABIT_NAME, habit.name)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.toInt() + 10000,
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 10 minutes
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_HABIT_ID, habit.id)
            putExtra(EXTRA_HABIT_NAME, habit.name)
            putExtra(EXTRA_SNOOZE_MINUTES, 10)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.toInt() + 15000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Habit Reminder")
            .setContentText("You have a habit scheduled for today")
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = habit.id.toInt()
        val priority = when {
            !playChannelAlert -> NotificationCompat.PRIORITY_HIGH
            habit.ringMode == RingMode.SOUND_AND_VIBRATE -> NotificationCompat.PRIORITY_HIGH
            habit.ringMode == RingMode.VIBRATE_ONLY -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_LOW
        }

        val detailText = if (currentStreak > 0) {
            "Consistency is key! Keep your $currentStreak-day streak for '${habit.name}' alive."
        } else {
            "Consistency is key! Take a moment to complete '${habit.name}' and start your streak."
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(habit.name)
            .setContentText("Time for your habit: ${habit.name}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailText))
            .setPriority(priority)
            .setSilent(!playChannelAlert || habit.ringMode == RingMode.SILENT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "✓ Mark Done",
                markDonePendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_reminder,
                "⏰ Snooze 10m",
                snoozePendingIntent
            )
            .setTicker("Habit Reminder: ${habit.name}")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (playChannelAlert && habit.ringMode == RingMode.SOUND_AND_VIBRATE) {
                val preOSoundUri = resolveNotificationSoundUri(habit.ringtoneUri)
                if (preOSoundUri != null) {
                    builder.setSound(preOSoundUri)
                }
                if (effectiveVibrate) {
                    builder.setVibrate(longArrayOf(0, 300, 200, 300))
                }
            } else if (playChannelAlert && habit.ringMode == RingMode.VIBRATE_ONLY && effectiveVibrate) {
                builder.setVibrate(longArrayOf(0, 300, 200, 300))
                builder.setSound(null)
            } else {
                builder.setSilent(true)
            }
        }

        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelNotification(habitId: Long) {
        notificationManager.cancel(habitId.toInt())
        notificationManager.cancel(habitId.toInt() + 50000)
    }

    fun cleanupHabitNotificationChannels(habitId: Long, keepChannelId: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val prefix = "habit_channel_v2_${habitId}_"
            try {
                notificationManager.notificationChannels.orEmpty().forEach { channel ->
                    val id = channel.id
                    if (id.startsWith(prefix) && id != keepChannelId) {
                        notificationManager.deleteNotificationChannel(id)
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
