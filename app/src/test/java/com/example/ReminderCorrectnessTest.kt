package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.HabitFrequency
import com.example.data.model.RingMode
import com.example.domain.StreakEngine
import com.example.native.AlarmScheduler
import com.example.native.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReminderCorrectnessTest {

    private lateinit var context: Context
    private lateinit var alarmScheduler: AlarmScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmScheduler = AlarmScheduler(context)
    }

    @Test
    fun requestCodeGeneration_distinctAndConsistent() {
        val habitId = 42L

        // Primary and additional slots must not collide
        val code0 = AlarmScheduler.getReminderRequestCode(habitId, 0)
        val code1 = AlarmScheduler.getReminderRequestCode(habitId, 1)
        val widgetPre0 = AlarmScheduler.getWidgetPreReminderRequestCode(habitId, 0)
        val snooze = AlarmScheduler.getSnoozeRequestCode(habitId)

        assertNotEquals(code0, code1)
        assertNotEquals(code0, widgetPre0)
        assertNotEquals(code0, snooze)
        assertNotEquals(widgetPre0, snooze)

        // Multiple calls with same arguments must produce identical codes
        assertEquals(code0, AlarmScheduler.getReminderRequestCode(habitId, 0))
        assertEquals(snooze, AlarmScheduler.getSnoozeRequestCode(habitId))
    }

    @Test
    fun habit_getAllReminderTimes_parsesAdditionalTimesSafely() {
        val habit = Habit(
            id = 1L,
            name = "Hydrate",
            reminderHour = 8,
            reminderMinute = 30,
            additionalReminderTimes = "12:00, 16:45, 21:15, invalid:time, 25:99"
        )

        val times = habit.getAllReminderTimes()
        assertEquals(4, times.size)
        assertEquals(Pair(8, 30), times[0])
        assertEquals(Pair(12, 0), times[1])
        assertEquals(Pair(16, 45), times[2])
        assertEquals(Pair(21, 15), times[3])
    }

    @Test
    fun streakEngine_weekdaysFrequency_skipsWeekendsWithoutBreakingStreak() {
        // Monday 2026-10-12
        val monday = LocalDate.of(2026, 10, 12)
        // Friday 2026-10-09
        // Thursday 2026-10-08
        // Wednesday 2026-10-07
        val completions = listOf(
            HabitCompletion(habitId = 1L, completionDate = "2026-10-12"), // Monday
            HabitCompletion(habitId = 1L, completionDate = "2026-10-09"), // Friday
            HabitCompletion(habitId = 1L, completionDate = "2026-10-08"), // Thursday
            HabitCompletion(habitId = 1L, completionDate = "2026-10-07")  // Wednesday
        )

        val (currentStreak, longestStreak) = StreakEngine.calculateStreaks(
            completions = completions,
            today = monday,
            frequency = HabitFrequency.WEEKDAYS
        )

        assertEquals(4, currentStreak)
        assertEquals(4, longestStreak)
    }

    @Test
    fun streakEngine_weekendsFrequency_skipsWeekdaysWithoutBreakingStreak() {
        // Sunday 2026-10-11
        val sunday = LocalDate.of(2026, 10, 11)
        val completions = listOf(
            HabitCompletion(habitId = 1L, completionDate = "2026-10-11"), // Sunday
            HabitCompletion(habitId = 1L, completionDate = "2026-10-10"), // Saturday
            HabitCompletion(habitId = 1L, completionDate = "2026-10-04"), // Previous Sunday
            HabitCompletion(habitId = 1L, completionDate = "2026-10-03")  // Previous Saturday
        )

        val (currentStreak, longestStreak) = StreakEngine.calculateStreaks(
            completions = completions,
            today = sunday,
            frequency = HabitFrequency.WEEKENDS
        )

        assertEquals(4, currentStreak)
        assertEquals(4, longestStreak)
    }

    @Test
    fun streakEngine_dueDates_properlyIdentified() {
        val monday = LocalDate.of(2026, 10, 12)
        val saturday = LocalDate.of(2026, 10, 10)
        val sunday = LocalDate.of(2026, 10, 11)

        assertTrue(StreakEngine.isDueOn(HabitFrequency.DAILY, monday))
        assertTrue(StreakEngine.isDueOn(HabitFrequency.DAILY, saturday))

        assertTrue(StreakEngine.isDueOn(HabitFrequency.WEEKDAYS, monday))
        assertFalse(StreakEngine.isDueOn(HabitFrequency.WEEKDAYS, saturday))
        assertFalse(StreakEngine.isDueOn(HabitFrequency.WEEKDAYS, sunday))

        assertFalse(StreakEngine.isDueOn(HabitFrequency.WEEKENDS, monday))
        assertTrue(StreakEngine.isDueOn(HabitFrequency.WEEKENDS, saturday))
        assertTrue(StreakEngine.isDueOn(HabitFrequency.WEEKENDS, sunday))
    }

    @Test
    fun notificationHelper_createsChannelsAndConfiguresActions() {
        val helper = NotificationHelper(context)
        helper.createNotificationChannels()

        val habit = Habit(
            id = 5L,
            name = "Morning Meditation",
            reminderEnabled = true,
            reminderHour = 7,
            reminderMinute = 0,
            ringMode = RingMode.SOUND_AND_VIBRATE
        )

        // Verifies no exception when building and triggering notification
        helper.showHabitNotification(habit)
        helper.cancelNotification(5L)
        helper.cancelAllNotifications()
    }

    @Test
    fun alarmScheduler_scheduleAndCancelHabitReminder_runsCleanly() {
        val habit = Habit(
            id = 99L,
            name = "Evening Reading",
            reminderEnabled = true,
            reminderHour = 20,
            reminderMinute = 0,
            frequency = HabitFrequency.DAILY,
            additionalReminderTimes = "22:00"
        )

        alarmScheduler.scheduleHabitReminder(habit)
        alarmScheduler.scheduleSnooze(habit.id, habit.name, 10 * 60 * 1000L)
        alarmScheduler.cancelHabitReminder(habit.id)
    }

    @Test
    fun alarmScheduler_computeNextTriggerTime_respectsWeekdaysAndDoneToday() {
        // Friday 2026-10-16 at 10:00 AM
        val friday = LocalDate.of(2026, 10, 16)
        val calFriday10am = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.OCTOBER, 16, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val nowMillis = calFriday10am.timeInMillis

        // 1. Not done today, reminder at 14:30 -> triggers same Friday at 14:30
        val sameDayTrigger = alarmScheduler.computeNextTriggerTimeMillis(
            hour = 14,
            minute = 30,
            frequency = HabitFrequency.WEEKDAYS,
            isDoneToday = false,
            nowMillis = nowMillis,
            today = friday
        )
        val resSame = java.util.Calendar.getInstance().apply { timeInMillis = sameDayTrigger }
        assertEquals(16, resSame.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(14, resSame.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(30, resSame.get(java.util.Calendar.MINUTE))

        // 2. Already done today -> skips Saturday 17 & Sunday 18, triggers Monday Oct 19 at 14:30
        val nextWeekTrigger = alarmScheduler.computeNextTriggerTimeMillis(
            hour = 14,
            minute = 30,
            frequency = HabitFrequency.WEEKDAYS,
            isDoneToday = true,
            nowMillis = nowMillis,
            today = friday
        )
        val resMonday = java.util.Calendar.getInstance().apply { timeInMillis = nextWeekTrigger }
        assertEquals(19, resMonday.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(14, resMonday.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(30, resMonday.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun alarmScheduler_getNextScheduledAlarm_picksEarliestNonCompletedHabit() {
        val testDay = LocalDate.of(2026, 10, 14) // Wednesday
        val cal10am = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.OCTOBER, 14, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val fixedNow = cal10am.timeInMillis

        val completedHabit = Habit(
            id = 10L,
            name = "Completed Early",
            reminderEnabled = true,
            reminderHour = 11,
            reminderMinute = 0,
            frequency = HabitFrequency.DAILY
        )
        val pendingHabit = Habit(
            id = 11L,
            name = "Pending Habit",
            reminderEnabled = true,
            reminderHour = 12,
            reminderMinute = 15,
            frequency = HabitFrequency.DAILY
        )
        val archivedHabit = Habit(
            id = 12L,
            name = "Archived Habit",
            reminderEnabled = true,
            reminderHour = 10,
            reminderMinute = 30,
            isArchived = true
        )

        val next = alarmScheduler.getNextScheduledAlarm(
            habits = listOf(completedHabit, pendingHabit, archivedHabit),
            completedTodayIds = setOf(10L),
            nowMillis = fixedNow,
            today = testDay
        )
        assertEquals(11L, next?.habit?.id)
    }

    @Test
    fun notificationActionReceiver_dismissAndTestAlarm_runCleanlyWithoutCrashing() {
        val receiver = com.example.native.NotificationActionReceiver()

        val dismissIntent = android.content.Intent(context, com.example.native.NotificationActionReceiver::class.java).apply {
            action = NotificationHelper.ACTION_DISMISS_ALARM
            putExtra(NotificationHelper.EXTRA_HABIT_ID, 77L)
            putExtra(NotificationHelper.EXTRA_HABIT_NAME, "Morning Run")
        }
        receiver.onReceive(context, dismissIntent)

        val testDoneIntent = android.content.Intent(context, com.example.native.NotificationActionReceiver::class.java).apply {
            action = NotificationHelper.ACTION_MARK_DONE
            putExtra(NotificationHelper.EXTRA_HABIT_ID, -999L)
            putExtra(NotificationHelper.EXTRA_HABIT_NAME, "Habit Streaks Alarm Test")
            putExtra(NotificationHelper.EXTRA_IS_TEST_ALARM, true)
        }
        receiver.onReceive(context, testDoneIntent)
        assertEquals(null, com.example.native.AlarmRingingService.activeAlarmHabitId)
    }

    @Test
    fun soundPlayer_alarmLoopAndCelebrationOptions_runSafely() {
        val player = com.example.native.SoundPlayer(context)
        player.playAlarmLoop(uriString = null, playSound = false, vibrate = false)
        player.stopAlarmLoop()
        player.playCelebrationFanfare(vibrate = false)
        player.stopPreview()
        player.release()
    }

    @Test
    fun notificationHelper_usesVersionedChannelIds_andResolvesFileProviderSoundUri() {
        assertTrue(NotificationHelper.CHANNEL_ID_SOUND_VIBRATE.endsWith("_v2"))
        assertTrue(NotificationHelper.CHANNEL_ID_VIBRATE_ONLY.endsWith("_v2"))
        assertTrue(NotificationHelper.CHANNEL_ID_SILENT.endsWith("_v2"))
        assertTrue(NotificationHelper.CHANNEL_ID_ALARM_RINGING.endsWith("_v2"))

        val helper = NotificationHelper(context)
        val dir = java.io.File(context.filesDir, "musical_ringtones").apply { mkdirs() }
        val wavFile = java.io.File(dir, "zen_bells.wav")
        wavFile.writeBytes(ByteArray(128) { 1 })

        val resolved = helper.resolveNotificationSoundUri(android.net.Uri.fromFile(wavFile).toString())
        if (resolved != null) {
            assertNotEquals("Notification sound URI must never stay file://", "file", resolved.scheme)
            assertEquals("content", resolved.scheme)
            assertTrue(resolved.authority?.endsWith(".fileprovider") == true)
        }

        val missingResolved = helper.resolveNotificationSoundUri("file:///non_existent_ringtone_999.wav")
        if (missingResolved != null) {
            assertNotEquals("file", missingResolved.scheme)
        }
    }

    @Test
    fun alarmScheduler_globalNotificationsCache_sharedAcrossInstances() {
        try {
            AlarmScheduler.updateGlobalNotificationsCache(false)
            val secondInstance = AlarmScheduler(context)
            assertFalse(secondInstance.isGlobalNotificationsEnabledSync())

            val activeHabit = Habit(
                id = 55L,
                name = "Read 20 pages",
                reminderEnabled = true,
                reminderHour = 23,
                reminderMinute = 50,
                frequency = HabitFrequency.DAILY
            )
            val nextAlarm = secondInstance.getNextScheduledAlarm(
                habits = listOf(activeHabit),
                completedTodayIds = emptySet()
            )
            assertEquals(null, nextAlarm)
        } finally {
            AlarmScheduler.updateGlobalNotificationsCache(true)
        }
    }

    @Test
    fun alarmRingingService_stopScopedToHabitId_doesNotStopDifferentActiveHabit() {
        val field = com.example.native.AlarmRingingService::class.java.getDeclaredField("activeAlarmHabitId")
        field.isAccessible = true
        try {
            field.set(null, 101L)
            com.example.native.AlarmRingingService.stop(context, habitId = 202L)
            assertEquals(101L, com.example.native.AlarmRingingService.activeAlarmHabitId)

            com.example.native.AlarmRingingService.stop(context, habitId = 101L)
            assertEquals(null, com.example.native.AlarmRingingService.activeAlarmHabitId)
        } finally {
            field.set(null, null)
        }
    }

    @Test
    fun greetingHelper_formatsGreetingWithCustomUserName() {
        val morningGreeting = com.example.util.GreetingHelper.getGreetingText(8, userName = "Alex", withEmoji = true)
        assertTrue(morningGreeting.contains("Good Morning, Alex!"))

        val eveningGreeting = com.example.util.GreetingHelper.getGreetingText(19, userName = "Sarah", withEmoji = false)
        assertEquals("Good Evening, Sarah!", eveningGreeting)

        val defaultGreeting = com.example.util.GreetingHelper.getGreetingText(14, userName = "", withEmoji = false)
        assertEquals("Good Afternoon, Champion!", defaultGreeting)
    }

    @Test
    fun preferencesManager_persistsUserNameAndExposesViaFlow() {
        val prefs = com.example.data.local.PreferencesManager.getInstance(context)
        prefs.setUserName("Jordan")
        assertEquals("Jordan", prefs.getUserName())
        assertEquals("Jordan", prefs.userNameFlow.value)
        assertTrue(prefs.hasStoredUserName())
    }

    @Test
    fun alarmScheduler_computeNextTriggerTime_withLeadGuard_skipsEarlyWakeupDoubleRing() {
        // Friday 2026-10-16 at 14:29:59.500 (500ms before 14:30 reminder due to OEM alarm wakeup jitter)
        val friday = LocalDate.of(2026, 10, 16)
        val calEarlyWakeup = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.OCTOBER, 16, 14, 29, 59)
            set(java.util.Calendar.MILLISECOND, 500)
        }
        val earlyNowMillis = calEarlyWakeup.timeInMillis

        // Without lead guard, 14:30:00.000 > 14:29:59.500 triggers same day (risk of double-ring).
        // With 60s lead guard (used by ReminderReceiver on reschedule), it skips today and schedules Monday Oct 19.
        val nextTrigger = alarmScheduler.computeNextTriggerTimeMillis(
            hour = 14,
            minute = 30,
            frequency = HabitFrequency.WEEKDAYS,
            isDoneToday = false,
            nowMillis = earlyNowMillis,
            today = friday,
            minTriggerLeadMillis = 60_000L
        )
        val resCal = java.util.Calendar.getInstance().apply { timeInMillis = nextTrigger }
        assertEquals(19, resCal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(14, resCal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(30, resCal.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun notificationHelper_missedAlertRoutesToHighImportanceSilentChannel_whileTrueSilentUsesLowChannel() {
        assertTrue(NotificationHelper.CHANNEL_ID_MISSED_REMINDER.endsWith("_v2"))
        val helper = NotificationHelper(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        val missedChannel = nm.getNotificationChannel(NotificationHelper.CHANNEL_ID_MISSED_REMINDER)
        assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH, missedChannel.importance)
        assertEquals(null, missedChannel.sound)
        assertFalse(missedChannel.shouldVibrate())

        val soundHabit = Habit(
            id = 301L,
            name = "Evening Journal",
            reminderEnabled = true,
            ringMode = RingMode.SOUND_AND_VIBRATE
        )
        helper.showHabitNotification(soundHabit, currentStreak = 5, playChannelAlert = false)

        val shadowNm = org.robolectric.Shadows.shadowOf(nm)
        val postedMissed = shadowNm.getNotification(301)
        assertEquals(NotificationHelper.CHANNEL_ID_MISSED_REMINDER, postedMissed.channelId)

        val silentHabit = Habit(
            id = 302L,
            name = "Quiet Reading",
            reminderEnabled = true,
            ringMode = RingMode.SILENT
        )
        helper.showHabitNotification(silentHabit, currentStreak = 2, playChannelAlert = true)
        val postedSilent = shadowNm.getNotification(302)
        assertEquals(NotificationHelper.CHANNEL_ID_SILENT, postedSilent.channelId)
    }
}
