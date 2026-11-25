package com.example

import com.example.data.model.Habit
import com.example.data.model.HabitFrequency
import com.example.data.model.RingMode
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun habit_singleReminderTime_returnsSinglePair() {
        val habit = Habit(
            name = "Morning Water",
            reminderEnabled = true,
            reminderHour = 7,
            reminderMinute = 30,
            additionalReminderTimes = ""
        )
        val allTimes = habit.getAllReminderTimes()
        assertEquals(1, allTimes.size)
        assertEquals(Pair(7, 30), allTimes[0])
    }

    @Test
    fun habit_multipleReminderTimes_returnsAllDistinctTimes() {
        val habit = Habit(
            name = "Hydration Alarm",
            reminderEnabled = true,
            reminderHour = 8,
            reminderMinute = 0,
            additionalReminderTimes = "12:30,16:45,20:00"
        )
        val allTimes = habit.getAllReminderTimes()
        assertEquals(4, allTimes.size)
        assertEquals(Pair(8, 0), allTimes[0])
        assertEquals(Pair(12, 30), allTimes[1])
        assertEquals(Pair(16, 45), allTimes[2])
        assertEquals(Pair(20, 0), allTimes[3])
    }

    @Test
    fun habit_multipleReminderTimes_handlesDuplicatesAndWhitespace() {
        val habit = Habit(
            name = "Focus Habit",
            reminderEnabled = true,
            reminderHour = 9,
            reminderMinute = 15,
            additionalReminderTimes = " 09:15 , 14:00, 18:30 , invalid "
        )
        val allTimes = habit.getAllReminderTimes()
        assertEquals(3, allTimes.size)
        assertEquals(Pair(9, 15), allTimes[0])
        assertEquals(Pair(14, 0), allTimes[1])
        assertEquals(Pair(18, 30), allTimes[2])
    }

    @Test
    fun habit_defaults_haveValidMusicalAndModeSettings() {
        val habit = Habit(name = "Meditation")
        assertEquals(RingMode.SOUND_AND_VIBRATE, habit.ringMode)
        assertTrue(habit.vibrateEnabled)
        assertEquals(HabitFrequency.DAILY, habit.frequency)
    }

    @Test
    fun adManager_adFreeMode_blocksAdsWhenActive() {
        val futureTime = System.currentTimeMillis() + 6 * 3600 * 1000L
        com.example.ads.AdManager.setAdFreeUntil(futureTime)
        assertTrue(com.example.ads.AdManager.isAdFreeActive())
        assertFalse(com.example.ads.AdManager.canRequestAds())
        assertFalse(com.example.ads.AdManager.canShowInterstitial())

        var callbackCalled = false
        com.example.ads.AdManager.tryShowInterstitial {
            callbackCalled = true
        }
        assertTrue(callbackCalled)
    }

    @Test
    fun networkUtils_offlineMessage_isDefined() {
        assertEquals("You are offline, please turn on Wi-Fi or mobile data.", com.example.util.NetworkUtils.OFFLINE_MESSAGE)
    }

    @Test
    fun networkUtils_nullContext_returnsFalse() {
        assertFalse(com.example.util.NetworkUtils.isNetworkAvailable(null))
    }

    @Test
    fun adManager_tryShowInterstitial_callsOnAdFailed_whenNotReady() {
        // Ensure not in ad-free mode
        com.example.ads.AdManager.setAdFreeUntil(0L)
        var dismissedCalled = false
        var failedCalled = false
        com.example.ads.AdManager.tryShowInterstitial(
            onAdDismissed = { dismissedCalled = true },
            onAdFailed = { failedCalled = true }
        )
        assertFalse(dismissedCalled)
        assertTrue(failedCalled)
    }

    @Test
    fun greetingHelper_timePeriods_matchExactHoursAndAmPm() {
        // Morning (3:00 AM - 11:59 AM)
        assertEquals(com.example.util.GreetingHelper.TimePeriod.MORNING, com.example.util.GreetingHelper.getTimePeriod(3))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.MORNING, com.example.util.GreetingHelper.getTimePeriod(5))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.MORNING, com.example.util.GreetingHelper.getTimePeriod(8))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.MORNING, com.example.util.GreetingHelper.getTimePeriod(11))
        assertTrue(com.example.util.GreetingHelper.getGreetingText(3).startsWith("Good Morning"))
        assertTrue(com.example.util.GreetingHelper.getGreetingText(9).startsWith("Good Morning"))

        // Afternoon (12:00 PM - 4:59 PM)
        assertEquals(com.example.util.GreetingHelper.TimePeriod.AFTERNOON, com.example.util.GreetingHelper.getTimePeriod(12))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.AFTERNOON, com.example.util.GreetingHelper.getTimePeriod(14))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.AFTERNOON, com.example.util.GreetingHelper.getTimePeriod(16))
        assertTrue(com.example.util.GreetingHelper.getGreetingText(12).startsWith("Good Afternoon"))
        assertTrue(com.example.util.GreetingHelper.getGreetingText(15).startsWith("Good Afternoon"))

        // Evening (5:00 PM - 8:59 PM)
        assertEquals(com.example.util.GreetingHelper.TimePeriod.EVENING, com.example.util.GreetingHelper.getTimePeriod(17))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.EVENING, com.example.util.GreetingHelper.getTimePeriod(19))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.EVENING, com.example.util.GreetingHelper.getTimePeriod(20))
        assertTrue(com.example.util.GreetingHelper.getGreetingText(18).startsWith("Good Evening"))

        // Night (9:00 PM - 2:59 AM)
        assertEquals(com.example.util.GreetingHelper.TimePeriod.NIGHT, com.example.util.GreetingHelper.getTimePeriod(21))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.NIGHT, com.example.util.GreetingHelper.getTimePeriod(22))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.NIGHT, com.example.util.GreetingHelper.getTimePeriod(23))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.NIGHT, com.example.util.GreetingHelper.getTimePeriod(0))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.NIGHT, com.example.util.GreetingHelper.getTimePeriod(1))
        assertEquals(com.example.util.GreetingHelper.TimePeriod.NIGHT, com.example.util.GreetingHelper.getTimePeriod(2))
        assertTrue(com.example.util.GreetingHelper.getGreetingText(21).startsWith("Good Night"))
        assertTrue(com.example.util.GreetingHelper.getGreetingText(2).startsWith("Good Night"))
    }
}

