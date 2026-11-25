import re

file_path = "app/src/main/java/com/example/native/NotificationHelper.kt"
with open(file_path, "r") as f:
    content = f.read()

# I will replace `fun showHabitNotification` entirely.
match = re.search(r'fun showHabitNotification\(habit: Habit\) \{.*', content, flags=re.DOTALL)
if match:
    before = content[:match.start()]
    
new_method = """fun showHabitNotification(habit: Habit) {
        val channelId = when (habit.ringMode) {
            RingMode.SOUND_AND_VIBRATE -> {
                val uriStr = habit.ringtoneUri ?: "default"
                val safeUriHash = uriStr.hashCode().toString().replace("-", "N")
                val dynamicChannelId = "habit_channel_${habit.id}_${safeUriHash}"
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val existingChannel = notificationManager.getNotificationChannel(dynamicChannelId)
                    if (existingChannel == null) {
                        try {
                            // Clean up old channels for this specific habit to avoid clutter
                            notificationManager.notificationChannels.forEach { channel ->
                                if (channel.id.startsWith("habit_channel_${habit.id}_") && channel.id != dynamicChannelId) {
                                    notificationManager.deleteNotificationChannel(channel.id)
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore cleanup errors
                        }
                        
                        val audioAttributes = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                        
                        val soundUri = if (habit.ringtoneUri.isNullOrBlank()) {
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        } else {
                            Uri.parse(habit.ringtoneUri)
                        }
                        
                        val newChannel = NotificationChannel(
                            dynamicChannelId,
                            "Alarm: ${habit.name}",
                            NotificationManager.IMPORTANCE_HIGH
                        ).apply {
                            description = "Alarm/Reminder for ${habit.name}"
                            enableVibration(habit.vibrateEnabled)
                            if (habit.vibrateEnabled) {
                                vibrationPattern = longArrayOf(0, 300, 200, 300)
                            }
                            setSound(soundUri, audioAttributes)
                            setShowBadge(true)
                            setBypassDnd(true) // Attempt to bypass DND for alarms
                        }
                        notificationManager.createNotificationChannel(newChannel)
                    }
                }
                dynamicChannelId
            }
            RingMode.VIBRATE_ONLY -> CHANNEL_ID_VIBRATE_ONLY
            RingMode.SILENT -> CHANNEL_ID_SILENT
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

        val notificationId = habit.id.toInt()
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(habit.name)
            .setContentText("Time for your habit! Keep your streak alive today.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Don't break your streak! Take a moment to complete '${habit.name}' and stay consistent.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "✓ Mark Done",
                markDonePendingIntent
            )

        if (habit.ringMode == RingMode.SOUND_AND_VIBRATE) {
            if (!habit.ringtoneUri.isNullOrBlank()) {
                builder.setSound(Uri.parse(habit.ringtoneUri))
            } else {
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            }
            if (habit.vibrateEnabled) {
                builder.setVibrate(longArrayOf(0, 300, 200, 300))
            }
        } else if (habit.ringMode == RingMode.VIBRATE_ONLY) {
            builder.setVibrate(longArrayOf(0, 250, 150, 250))
            builder.setSound(null)
        } else {
            builder.setSilent(true)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelNotification(habitId: Long) {
        notificationManager.cancel(habitId.toInt())
    }
}
"""

with open(file_path, "w") as f:
    f.write(before + new_method)
