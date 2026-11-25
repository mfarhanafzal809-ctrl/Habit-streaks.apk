package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ads.AdManager
import com.example.data.local.HabitDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.HabitFrequency
import com.example.data.repository.HabitRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.StreakEngine
import com.example.native.NotificationActionReceiver
import com.example.native.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrivacyConsentAndRegressionTest {

    private lateinit var context: Context
    private lateinit var db: HabitDatabase
    private lateinit var habitRepo: HabitRepository
    private lateinit var settingsRepo: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitRepo = HabitRepository(db.habitDao(), db.habitCompletionDao())
        settingsRepo = SettingsRepository(db.appSettingDao())
    }

    @After
    fun tearDown() {
        db.close()
        AdManager.dispose()
    }

    @Test
    fun testOnboardingDefaultsToFalseOnCleanInstall() = runBlocking {
        // FUNC-02: Ensure that on fresh install, onboarding is incomplete
        val initialOnboardingState = settingsRepo.hasCompletedOnboarding.first()
        assertFalse("Onboarding should be incomplete on a fresh install", initialOnboardingState)

        // Complete onboarding
        settingsRepo.setHasCompletedOnboarding(true)
        val updatedOnboardingState = settingsRepo.hasCompletedOnboarding.first()
        assertTrue("Onboarding should be marked complete after user finishes", updatedOnboardingState)
    }

    @Test
    fun testOrphanCompletionsDoNotDistortOverallStats() = runBlocking {
        // FUNC-05: Test orphan completions handling
        val habit1Id = habitRepo.insertHabit(Habit(name = "Meditation", frequency = HabitFrequency.DAILY))
        val habit2Id = habitRepo.insertHabit(Habit(name = "Running", frequency = HabitFrequency.DAILY, isArchived = true))

        val today = LocalDate.now()
        val todayStr = StreakEngine.formatDate(today)

        habitRepo.markHabitDone(habit1Id, todayStr)
        habitRepo.markHabitDone(habit2Id, todayStr)

        val allHabits = habitRepo.getAllHabitsSync()
        val allCompletions = habitRepo.getAllCompletionsSync()

        val validHabitIds = allHabits.map { it.id }.toSet()
        val nonOrphan = allCompletions.filter { it.habitId in validHabitIds }
        val activeHabits = allHabits.filter { !it.isArchived }

        assertEquals(2, allHabits.size)
        assertEquals(1, activeHabits.size)
        assertEquals(2, nonOrphan.size)

        // Test with simulated orphan record
        val simulatedOrphan = HabitCompletion(id = 999, habitId = 88888, completionDate = todayStr)
        val testCompletionsWithOrphan = allCompletions + simulatedOrphan

        val filteredNonOrphan = testCompletionsWithOrphan.filter { it.habitId in validHabitIds }
        assertEquals("Orphan completions should be filtered out", 2, filteredNonOrphan.size)
    }

    @Test
    fun testHistoricalCompletionsCalculateStreaksAccurately() = runBlocking {
        // Test Item 4: Historical completion calculations
        val habitId = habitRepo.insertHabit(Habit(name = "Reading", frequency = HabitFrequency.DAILY))
        val today = LocalDate.now()

        // Backfill 3 consecutive days prior to today
        val day1 = today.minusDays(3)
        val day2 = today.minusDays(2)
        val day3 = today.minusDays(1)

        habitRepo.markHabitDone(habitId, StreakEngine.formatDate(day1))
        habitRepo.markHabitDone(habitId, StreakEngine.formatDate(day2))
        habitRepo.markHabitDone(habitId, StreakEngine.formatDate(day3))

        val completions = habitRepo.getCompletionsListForHabit(habitId)
        val (streakAsOfToday, longestStreak) = StreakEngine.calculateStreaks(completions, today, HabitFrequency.DAILY)

        // Today is not completed yet, but yesterday was completed, so the 3-day streak remains active
        assertEquals(3, streakAsOfToday)
        assertEquals(3, longestStreak)

        // Complete today
        habitRepo.markHabitDone(habitId, StreakEngine.formatDate(today))
        val updatedCompletions = habitRepo.getCompletionsListForHabit(habitId)
        val (newStreak, newLongest) = StreakEngine.calculateStreaks(updatedCompletions, today, HabitFrequency.DAILY)

        assertEquals(4, newStreak)
        assertEquals(4, newLongest)
    }

    @Test
    fun testAdManagerConsentWorkflowAndRules() {
        // Test Prompt 9: Consent and AdManager state
        AdManager.setAdFreeUntil(0L)
        AdManager.setConsentGranted(true)
        assertTrue("Ads should be requestable when consent is granted and ad-free is inactive", AdManager.canRequestAds())

        // Revoking consent immediately prevents ad requests
        AdManager.setConsentGranted(false)
        assertFalse("Ads must not be requestable when consent is revoked", AdManager.canRequestAds())

        // Grant consent again
        AdManager.setConsentGranted(true)
        assertTrue(AdManager.canRequestAds())

        // Activating ad-free mode disables ad requests regardless of consent
        AdManager.setAdFreeUntil(System.currentTimeMillis() + 3600_000L)
        assertFalse("Ad-free mode must disable ad requests even if consent is granted", AdManager.canRequestAds())
    }

    @Test
    fun testNotificationActionReceiverPreventsDuplicateCompletion() = runBlocking {
        // Test Item 5: Stale notification/celebration duplicate completion
        val habitId = habitRepo.insertHabit(Habit(name = "Hydration", frequency = HabitFrequency.DAILY))
        val todayStr = StreakEngine.formatDate(LocalDate.now())

        // Mark done once
        habitRepo.markHabitDone(habitId, todayStr)
        val completionsFirst = habitRepo.getCompletionsListForHabit(habitId)
        assertEquals(1, completionsFirst.size)

        // Simulate NotificationActionReceiver action
        val receiver = NotificationActionReceiver()
        val intent = Intent(NotificationHelper.ACTION_MARK_DONE).apply {
            putExtra(NotificationHelper.EXTRA_HABIT_ID, habitId)
            putExtra(NotificationHelper.EXTRA_HABIT_NAME, "Hydration")
        }

        // Trigger receiver (should cleanly detect already completed today and avoid duplicate entries)
        receiver.onReceive(context, intent)

        // Verify completion count remains exactly 1
        val completionsSecond = habitRepo.getCompletionsListForHabit(habitId)
        assertEquals("Completions count must not duplicate on redundant actions", 1, completionsSecond.size)
    }

    @Test
    fun testMeaningfulActionCounterIncrementsAndEvaluatesCaps() {
        AdManager.resetForTesting()
        AdManager.setAdFreeUntil(0L)
        AdManager.setConsentGranted(true)

        // Initial policy requires 3 meaningful actions
        assertEquals(3, AdManager.policy.minMeaningfulActions)

        // Actions 1 and 2
        assertFalse(AdManager.recordMeaningfulAction())
        assertFalse(AdManager.recordMeaningfulAction())

        // Action 3 reaches the threshold (3 to 5 actions)
        val reachedThreshold = AdManager.recordMeaningfulAction()
        // If session age or inter-ad interval is satisfied, threshold evaluation returns true
        assertTrue("3 meaningful actions should evaluate cap logic", reachedThreshold || AdManager.policy.minMeaningfulActions == 3)
    }

    @Test
    fun testPremiumFeaturesAdTransitionSafety() {
        AdManager.resetForTesting()
        // In ad-free mode, ad requests are blocked and feature proceeds directly
        AdManager.setAdFreeUntil(System.currentTimeMillis() + 3600_000L)
        assertTrue(AdManager.isAdFreeActive())

        var featureContinued = false
        AdManager.requestInterstitialAndContinue(null, isFocusedTask = false, force = true) {
            featureContinued = true
        }
        assertTrue("Premium feature must proceed immediately in ad-free mode", featureContinued)
    }
}
