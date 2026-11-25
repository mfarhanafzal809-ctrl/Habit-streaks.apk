package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdManager
import com.example.data.local.HabitDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.HabitFrequency
import com.example.data.model.HabitWithStats
import com.example.data.model.RingMode
import com.example.data.model.StreakMilestone
import com.example.data.model.MilestoneCelebration
import com.example.data.repository.HabitRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.BackupManager
import com.example.domain.StreakEngine
import androidx.glance.appwidget.updateAll
import com.example.widget.HabitWidget
import com.example.native.AlarmScheduler
import com.example.native.SoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.time.YearMonth

enum class HabitFilter {
    ALL,
    PENDING,
    COMPLETED,
    ARCHIVED
}

enum class HabitSortOrder {
    DEFAULT,
    STREAK_DESCENDING,
    NAME_ASCENDING,
    TIME_DUE
}

sealed interface HabitsFeedState {
    object Loading : HabitsFeedState
    data class Success(val habits: List<HabitWithStats>) : HabitsFeedState
    data class Error(val errorMessage: String) : HabitsFeedState
}


class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy { HabitDatabase.getInstance(application) }
    private val repository by lazy { HabitRepository(db.habitDao(), db.habitCompletionDao()) }
    private val settingsRepo by lazy { SettingsRepository(db.appSettingDao()) }
    private val backupManager by lazy { BackupManager(application, repository) }
    private val alarmScheduler by lazy { AlarmScheduler(application) }
    private val soundPlayer by lazy { SoundPlayer(application) }
    private val preferencesManager by lazy { com.example.data.local.PreferencesManager.getInstance(application) }

    // Current date state
    private val _today = MutableStateFlow(com.example.util.GreetingHelper.getNow().toLocalDate())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    // Calendar selected month and date
    private val _selectedMonth = MutableStateFlow(YearMonth.from(com.example.util.GreetingHelper.getNow().toLocalDate()))
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(com.example.util.GreetingHelper.getNow().toLocalDate())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Filter
    private val _currentFilter = MutableStateFlow(HabitFilter.ALL)
    val currentFilter: StateFlow<HabitFilter> = _currentFilter.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sort order
    private val _currentSortOrder = MutableStateFlow(HabitSortOrder.DEFAULT)
    val currentSortOrder: StateFlow<HabitSortOrder> = _currentSortOrder.asStateFlow()



    // Settings
    val isDarkMode: StateFlow<Boolean?> = settingsRepo.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isNotificationsEnabled: StateFlow<Boolean> = settingsRepo.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isVibrationEnabled: StateFlow<Boolean> = settingsRepo.isVibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDynamicColorEnabled: StateFlow<Boolean> = settingsRepo.isDynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDynamicRainbowEnabled: StateFlow<Boolean> = settingsRepo.isDynamicRainbowEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val savedCustomThemes: StateFlow<String> = settingsRepo.savedCustomThemes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val dynamicColorSource: StateFlow<String> = settingsRepo.dynamicColorSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BOTH")

    val themeTemplate: StateFlow<String> = settingsRepo.themeTemplate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INDIGO")

    val hasCompletedOnboarding: StateFlow<Boolean> = settingsRepo.hasCompletedOnboarding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true) // Default true to avoid flash

    val isGlassmorphismEnabled: StateFlow<Boolean> = settingsRepo.isGlassmorphismEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val glassVfxConfig: StateFlow<com.example.ui.theme.GlassVfxConfig> = combine(
        combine(
            settingsRepo.isGlassmorphismEnabled,
            settingsRepo.glassTint,
            settingsRepo.glassCurvature,
            settingsRepo.glassBloom,
            settingsRepo.glassReflection
        ) { enabled, tint, curvature, bloom, reflection ->
            com.example.ui.theme.GlassVfxConfig(
                enabled = enabled,
                tint = tint,
                lensCurvature = curvature,
                highlightBloom = bloom,
                reflectionDepth = reflection
            )
        },
        combine(
            settingsRepo.glassTranslucency,
            settingsRepo.glassAuroraEnabled,
            settingsRepo.glassSpecularGloss,
            settingsRepo.glassCausticGlow
        ) { translucency, aurora, gloss, caustic ->
            listOf(translucency, aurora, gloss, caustic)
        }
    ) { base, extras ->
        base.copy(
            frostedTranslucency = extras[0] as Float,
            auroraBackdrop = extras[1] as Boolean,
            specularGloss = extras[2] as Boolean,
            causticGlow = extras[3] as Boolean
        ).clamped()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.ui.theme.GlassVfxConfig()
    )

    val isWidgetEnabled: StateFlow<Boolean> = settingsRepo.isWidgetEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val is24HourFormat: StateFlow<Boolean> = settingsRepo.is24HourFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userNameFlow: StateFlow<String> = preferencesManager.userNameFlow
    val userName: StateFlow<String> = preferencesManager.userNameFlow

    private val _hasStoredUserName = MutableStateFlow(preferencesManager.hasStoredUserName())
    val hasStoredUserName: StateFlow<Boolean> = _hasStoredUserName.asStateFlow()

    fun hasStoredUserName(): Boolean = preferencesManager.hasStoredUserName()

    fun setUserName(name: String) {
        val cleanName = name.trim().ifBlank { "Champion" }
        preferencesManager.setUserName(cleanName)
        _hasStoredUserName.value = true
        viewModelScope.launch {
            settingsRepo.setUserName(cleanName)
        }
    }

    val isWidgetUrgentHighlightEnabled: StateFlow<Boolean> = settingsRepo.isWidgetUrgentHighlightEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isWidgetShowCompleted: StateFlow<Boolean> = settingsRepo.isWidgetShowCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isWidgetQuickControlsEnabled: StateFlow<Boolean> = settingsRepo.isWidgetQuickControlsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val adFreeUntilTimestamp: StateFlow<Long> = settingsRepo.adFreeUntilTimestamp
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private val _isAdFreeActive = MutableStateFlow(false)
    val isAdFreeActive: StateFlow<Boolean> = _isAdFreeActive.asStateFlow()

    private val _adFreeRemainingFormatted = MutableStateFlow("")
    val adFreeRemainingFormatted: StateFlow<String> = _adFreeRemainingFormatted.asStateFlow()

    private var adFreeCountdownJob: Job? = null

    private val _isAppInForeground = MutableStateFlow(true)

    fun setAppForeground(inForeground: Boolean) {
        _isAppInForeground.value = inForeground
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            alarmScheduler.isGlobalNotificationsEnabled()
        }
        viewModelScope.launch {
            adFreeUntilTimestamp.collect { timestamp ->
                scheduleAdFreeExpiry(timestamp)
            }
        }
        viewModelScope.launch {
            var dynamicJob: Job? = null
            combine(isDynamicRainbowEnabled, _isAppInForeground) { enabled, inForeground ->
                enabled && inForeground
            }.collect { shouldCycle ->
                dynamicJob?.cancel()
                if (shouldCycle) {
                    dynamicJob = launch(Dispatchers.IO) {
                        val builtIns = listOf(
                            "SUNSET_CORAL", "OCEAN_BLUE", "EMERALD_MINT", "GOLDEN_AMBER", "ORCHID_PURPLE", "LIME_CYAN",
                            "INDIGO", "CYBERPUNK", "EMERALD", "ROSE", "SUNSET", "OCEAN",
                            "ROYAL", "AURORA", "OBSIDIAN_GOLD", "MIDNIGHT_NEON", "CHERRY_BLOSSOM", "COSMIC",
                            "ORANGE", "CRIMSON", "MINT", "NEBULA_PURPLE", "VAPORWAVE", "NORDIC_FROST",
                            "DESERT_DUNE", "VOLCANIC_EMBER", "MATCHA_ZEN", "DEEP_SPACE"
                        )
                        delay(2500L)
                        while (isActive) {
                            val source = dynamicColorSource.value
                            val customStr = savedCustomThemes.value
                            val customList = if (customStr.isBlank()) emptyList() else customStr.split(";;;")

                            val isDyn = isDynamicColorEnabled.value
                            val pool = mutableListOf<String>()

                            // When dynamic colors is turned on alongside rainbow transitions,
                            // the UI also transforms in device wallpaper themes fetched by the dynamic colors feature
                            if (isDyn) {
                                pool.add("DYNAMIC_WALLPAPER_PRIMARY")
                                pool.add("DYNAMIC_WALLPAPER_HARMONY")
                                pool.add("DYNAMIC_WALLPAPER_VIBRANT")
                            }

                            if (source == "BUILTIN" || source == "BOTH") {
                                pool.addAll(builtIns)
                            }
                            if (source == "CUSTOM" || source == "BOTH") {
                                customList.forEach { item ->
                                    val raw = if (item.contains(":")) item.substringAfterLast(":") else item
                                    val cleaned = raw.removePrefix("CUSTOM:")
                                    if (cleaned.contains("|")) {
                                        pool.add("CUSTOM:$cleaned")
                                    } else if (raw.startsWith("CUSTOM:")) {
                                        pool.add(raw)
                                    }
                                }
                            }
                            if (pool.isEmpty()) {
                                pool.addAll(builtIns)
                            }

                            val current = themeTemplate.value
                            val nextIdx = (pool.indexOf(current) + 1).let { if (it in pool.indices) it else 0 }
                            settingsRepo.setThemeTemplate(pool[nextIdx])
                            delay(7000L)
                        }
                    }
                }
            }
        }
    }

    fun refreshCurrentDate() {
        val now = com.example.util.GreetingHelper.getNow().toLocalDate()
        if (_today.value != now) {
            _today.value = now
            _selectedMonth.value = YearMonth.from(now)
            _selectedDate.value = now
        }
    }

    fun refreshAdFreeState() {
        scheduleAdFreeExpiry(adFreeUntilTimestamp.value)
    }

    private fun scheduleAdFreeExpiry(untilTimestamp: Long) {
        adFreeCountdownJob?.cancel()
        val now = System.currentTimeMillis()
        if (untilTimestamp <= now) {
            _isAdFreeActive.value = false
            _adFreeRemainingFormatted.value = ""
            AdManager.setAdFreeUntil(0L, getApplication())
            return
        }

        _isAdFreeActive.value = true
        AdManager.setAdFreeUntil(untilTimestamp, getApplication())
        updateAdFreeFormattedString(untilTimestamp, now)

        adFreeCountdownJob = viewModelScope.launch {
            while (isActive) {
                val current = System.currentTimeMillis()
                if (current >= untilTimestamp) {
                    _isAdFreeActive.value = false
                    _adFreeRemainingFormatted.value = ""
                    AdManager.setAdFreeUntil(0L, getApplication())
                    break
                }
                updateAdFreeFormattedString(untilTimestamp, current)
                val remainingMillis = untilTimestamp - current
                val millisToNextMinute = (current % 60000L).let { 60000L - it }.coerceIn(1000L, 60000L)
                val delayTime = minOf(remainingMillis, millisToNextMinute)
                delay(delayTime)
            }
        }
    }

    private fun updateAdFreeFormattedString(untilTimestamp: Long, now: Long) {
        val remainingMillis = untilTimestamp - now
        if (remainingMillis <= 0) {
            _adFreeRemainingFormatted.value = ""
            return
        }
        val totalMinutes = (remainingMillis / 60000).coerceAtLeast(1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        _adFreeRemainingFormatted.value = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun activateAdFree(hours: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val baseTime = maxOf(now, adFreeUntilTimestamp.value)
            val targetTime = baseTime + (hours.toLong() * 3600L * 1000L)
            settingsRepo.setAdFreeUntil(targetTime)
            withContext(Dispatchers.Main) {
                scheduleAdFreeExpiry(targetTime)
            }
        }
    }

    fun playCelebrationSound() {
        soundPlayer.playCelebrationFanfare(vibrate = isVibrationEnabled.value)
    }

    private val _celebratingMilestone = MutableStateFlow<MilestoneCelebration?>(null)
    val celebratingMilestone: StateFlow<MilestoneCelebration?> = _celebratingMilestone.asStateFlow()

    fun dismissCelebration() {
        _celebratingMilestone.value = null
    }

    fun openMilestonePreview(milestone: StreakMilestone, bestHabitName: String? = null, bestStreak: Int? = null) {
        val next = StreakEngine.getNextMilestone(milestone.days)
        _celebratingMilestone.value = MilestoneCelebration(
            habitName = bestHabitName ?: "Habit Journey",
            streakDays = bestStreak ?: milestone.days,
            milestone = milestone,
            isNewUnlock = milestone.isUnlocked,
            nextMilestoneDays = next?.days,
            nextMilestoneTitle = next?.title
        )
        soundPlayer.playCelebrationFanfare(vibrate = isVibrationEnabled.value)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setHasCompletedOnboarding(completed)
        }
    }

    private val _interstitialTrigger = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val interstitialTrigger: kotlinx.coroutines.flow.SharedFlow<Unit> = _interstitialTrigger.asSharedFlow()

    private fun onMeaningfulActionDone() {
        if (AdManager.recordMeaningfulAction()) {
            _interstitialTrigger.tryEmit(Unit)
        }
    }

    // Habits feed state with distinct Loading, Success, and Error states
    val habitsFeedState: StateFlow<HabitsFeedState> = combine(
        repository.allHabits,
        repository.allCompletions,
        _today,
        _searchQuery,
        combine(_currentFilter, _currentSortOrder) { f, s -> Pair(f, s) }
    ) { habits, completions, todayDate, query, (filter, sortOrder) ->
        try {
            val completionsByHabit = completions.groupBy { it.habitId }
            val todayStr = StreakEngine.formatDate(todayDate)
            val currentMonthStr = todayDate.format(StreakEngine.MONTH_FORMATTER)

            val list = habits.map { habit ->
                val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
                StreakEngine.buildHabitWithStats(habit, habitCompletions, todayDate, todayStr, currentMonthStr)
            }

            val filteredByQuery = if (query.isBlank()) list else {
                list.filter { it.habit.name.contains(query, ignoreCase = true) || it.habit.category.contains(query, ignoreCase = true) }
            }

            val filtered = when (filter) {
                HabitFilter.ALL -> filteredByQuery.filter { !it.habit.isArchived }
                HabitFilter.PENDING -> filteredByQuery.filter { !it.habit.isArchived && !it.isCompletedToday }
                HabitFilter.COMPLETED -> filteredByQuery.filter { !it.habit.isArchived && it.isCompletedToday }
                HabitFilter.ARCHIVED -> filteredByQuery.filter { it.habit.isArchived }
            }

            val sorted = when (sortOrder) {
                HabitSortOrder.DEFAULT -> filtered
                HabitSortOrder.STREAK_DESCENDING -> filtered.sortedByDescending { it.currentStreak }
                HabitSortOrder.NAME_ASCENDING -> filtered.sortedBy { it.habit.name.lowercase() }
                HabitSortOrder.TIME_DUE -> filtered.sortedBy { it.habit.reminderHour * 60 + it.habit.reminderMinute }
            }
            HabitsFeedState.Success(sorted)
        } catch (e: Exception) {
            HabitsFeedState.Error(e.message ?: "Failed to compute habits")
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsFeedState.Loading)

    // Derived habits list for backward compatibility with UI feed
    val habitsWithStats: StateFlow<List<HabitWithStats>> = habitsFeedState
        .map { state ->
            if (state is HabitsFeedState.Success) state.habits else emptyList()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All active habits with stats, unaffected by feed search query or current filter
    val allActiveHabitsWithStats: StateFlow<List<HabitWithStats>> = combine(
        repository.allHabits,
        repository.allCompletions,
        _today
    ) { habits, completions, todayDate ->
        val activeHabits = habits.filter { !it.isArchived }
        val completionsByHabit = completions.groupBy { it.habitId }
        val todayStr = StreakEngine.formatDate(todayDate)
        val currentMonthStr = todayDate.format(StreakEngine.MONTH_FORMATTER)
        activeHabits.map { habit ->
            val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
            StreakEngine.buildHabitWithStats(habit, habitCompletions, todayDate, todayStr, currentMonthStr)
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All habits including archived with stats
    val allHabitsWithStats: StateFlow<List<HabitWithStats>> = combine(
        repository.allHabits,
        repository.allCompletions,
        _today
    ) { habits, completions, todayDate ->
        val completionsByHabit = completions.groupBy { it.habitId }
        val todayStr = StreakEngine.formatDate(todayDate)
        val currentMonthStr = todayDate.format(StreakEngine.MONTH_FORMATTER)
        habits.map { habit ->
            val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
            StreakEngine.buildHabitWithStats(habit, habitCompletions, todayDate, todayStr, currentMonthStr)
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All habits including archived
    val allHabits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All completions
    val allCompletions: StateFlow<List<HabitCompletion>> = repository.allCompletions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall summary stats, resilient against orphan completions and independent of feed filters
    val overallStats: StateFlow<com.example.data.model.OverallStats> = combine(
        repository.allHabits,
        repository.allCompletions,
        _today
    ) { habits, completions, todayDate ->
        val validHabitIds = habits.map { it.id }.toSet()
        val nonOrphanCompletions = completions.filter { it.habitId in validHabitIds }
        val activeHabits = habits.filter { !it.isArchived }

        val completionsByHabit = nonOrphanCompletions.groupBy { it.habitId }
        val todayStr = StreakEngine.formatDate(todayDate)
        val currentMonthStr = todayDate.format(StreakEngine.MONTH_FORMATTER)

        val activeStats = activeHabits.map { habit ->
            val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
            StreakEngine.buildHabitWithStats(habit, habitCompletions, todayDate, todayStr, currentMonthStr)
        }

        val allStats = habits.map { habit ->
            val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
            StreakEngine.buildHabitWithStats(habit, habitCompletions, todayDate, todayStr, currentMonthStr)
        }

        val totalCompletions = nonOrphanCompletions.size
        val bestStreakEver = allStats.maxOfOrNull { it.longestStreak } ?: 0
        val currentBestStreak = activeStats.maxOfOrNull { it.currentStreak } ?: 0
        val activeCount = activeHabits.size

        com.example.data.model.OverallStats(
            totalCompletions = totalCompletions,
            bestStreakEver = bestStreakEver,
            currentBestStreak = currentBestStreak,
            activeHabits = activeCount
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.model.OverallStats(0, 0, 0, 0))

    // Gamification & Habit Mastery Level calculation
    val habitXpAndLevel: StateFlow<com.example.data.model.HabitMasteryInfo> = combine(
        overallStats,
        repository.allCompletions
    ) { stats, _ ->
        val totalCompletions = stats.totalCompletions
        val bestStreak = stats.bestStreakEver
        // XP Formula: 25 XP per completion + 15 XP per best streak day
        val totalXp = (totalCompletions * 25) + (bestStreak * 15)
        val xpPerLevel = 200
        val level = (totalXp / xpPerLevel) + 1
        val currentLevelXp = totalXp % xpPerLevel
        val progress = currentLevelXp.toFloat() / xpPerLevel.toFloat()
        val title = when {
            level >= 25 -> "Mythic Grandmaster 👑"
            level >= 15 -> "Discipline Master ⚡"
            level >= 10 -> "Consistency Champion 🏆"
            level >= 6 -> "Streak Warrior ⚔️"
            level >= 3 -> "Habit Apprentice 🌟"
            else -> "Novice Explorer 🌱"
        }
        com.example.data.model.HabitMasteryInfo(
            level = level,
            title = title,
            totalXp = totalXp,
            currentLevelXp = currentLevelXp,
            xpToNextLevel = xpPerLevel,
            progress = progress
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.model.HabitMasteryInfo())

    init {
        // seedSampleDataIfEmpty() // disabled by request
    }

    private fun seedSampleDataIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getAllHabitsSync()
            if (existing.isEmpty()) {
                val sample1 = Habit(
                    name = "Morning Water & Hydration",
                    description = "Drink 500ml water right after waking up",
                    category = "Health",
                    colorHex = "#0EA5E9",
                    iconName = "WaterDrop",
                    reminderEnabled = true,
                    reminderHour = 8,
                    reminderMinute = 0,
                    ringMode = RingMode.SOUND_AND_VIBRATE
                )
                val sample2 = Habit(
                    name = "Read 15 Pages",
                    description = "Focus on reading books daily",
                    category = "Learning",
                    colorHex = "#8B5CF6",
                    iconName = "Book",
                    reminderEnabled = true,
                    reminderHour = 21,
                    reminderMinute = 0,
                    ringMode = RingMode.SOUND_AND_VIBRATE
                )
                val sample3 = Habit(
                    name = "Daily Workout & Stretch",
                    description = "20-minute bodyweight or stretching session",
                    category = "Fitness",
                    colorHex = "#FF5722",
                    iconName = "FitnessCenter",
                    reminderEnabled = true,
                    reminderHour = 18,
                    reminderMinute = 30,
                    ringMode = RingMode.SOUND_AND_VIBRATE
                )
                val id1 = repository.insertHabit(sample1)
                val id2 = repository.insertHabit(sample2)
                val id3 = repository.insertHabit(sample3)

                // Add some initial streak completions for a great first-launch experience
                val currentToday = com.example.util.GreetingHelper.getNow().toLocalDate()
                val yestStr = StreakEngine.formatDate(currentToday.minusDays(1))
                val prev2Str = StreakEngine.formatDate(currentToday.minusDays(2))
                val prev3Str = StreakEngine.formatDate(currentToday.minusDays(3))

                repository.markHabitDone(id1, yestStr)
                repository.markHabitDone(id1, prev2Str)
                repository.markHabitDone(id1, prev3Str)

                repository.markHabitDone(id2, yestStr)
                repository.markHabitDone(id2, prev2Str)

                alarmScheduler.scheduleHabitReminder(sample1.copy(id = id1))
                alarmScheduler.scheduleHabitReminder(sample2.copy(id = id2))
                alarmScheduler.scheduleHabitReminder(sample3.copy(id = id3))
            }
        }
    }

    fun setFilter(filter: HabitFilter) {
        _currentFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: HabitSortOrder) {
        _currentSortOrder.value = order
    }

    fun setSelectedMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleHabitDoneToday(habitId: Long, isDone: Boolean) {
        val currentToday = com.example.util.GreetingHelper.getNow().toLocalDate()
        _today.value = currentToday
        val todayStr = StreakEngine.formatDate(currentToday)
        viewModelScope.launch(Dispatchers.IO) {
            if (isDone) {
                repository.undoHabitDone(habitId, todayStr)
            } else {
                repository.markHabitDone(habitId, todayStr)
                val habit = repository.getHabitById(habitId)
                val completions = repository.getCompletionsListForHabit(habitId)
                val (currentStreak, _) = StreakEngine.calculateStreaks(
                    completions = completions,
                    today = currentToday,
                    frequency = habit?.frequency ?: HabitFrequency.DAILY
                )

                val milestone = StreakEngine.findMilestoneForStreak(currentStreak)
                if (milestone != null && habit != null) {
                    val celebratedKey = "milestone_celebrated_${habitId}_${currentStreak}"
                    val alreadyCelebrated = settingsRepo.getSetting(celebratedKey, "") == todayStr
                    if (!alreadyCelebrated) {
                        settingsRepo.setSetting(celebratedKey, todayStr)
                        val next = StreakEngine.getNextMilestone(currentStreak)
                        _celebratingMilestone.value = MilestoneCelebration(
                            habitName = habit.name,
                            streakDays = currentStreak,
                            milestone = milestone,
                            isNewUnlock = true,
                            nextMilestoneDays = next?.days,
                            nextMilestoneTitle = next?.title
                        )
                        soundPlayer.playCelebrationFanfare(vibrate = isVibrationEnabled.value)
                    } else {
                        soundPlayer.playPreview("chime", isVibrationEnabled.value)
                    }
                } else {
                    soundPlayer.playPreview("chime", isVibrationEnabled.value)
                }
                onMeaningfulActionDone()
            }
            try {
                if (!isDone) {
                    com.example.native.AlarmRingingService.stop(getApplication(), habitId)
                    com.example.native.NotificationHelper(getApplication()).cancelNotification(habitId)
                }
                val habit = repository.getHabitById(habitId)
                if (habit != null && habit.reminderEnabled && !habit.isArchived) {
                    alarmScheduler.scheduleHabitReminder(habit, isDoneTodayOverride = !isDone)
                }
                HabitWidget().updateAll(getApplication<Application>())
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleHabitDoneOnDate(habitId: Long, date: LocalDate, isDone: Boolean) {
        val dateStr = StreakEngine.formatDate(date)
        val currentToday = com.example.util.GreetingHelper.getNow().toLocalDate()
        _today.value = currentToday
        viewModelScope.launch(Dispatchers.IO) {
            if (isDone) {
                repository.undoHabitDone(habitId, dateStr)
            } else {
                repository.markHabitDone(habitId, dateStr)
                val habit = repository.getHabitById(habitId)
                val completions = repository.getCompletionsListForHabit(habitId)
                val (currentStreak, _) = StreakEngine.calculateStreaks(
                    completions = completions,
                    today = currentToday,
                    frequency = habit?.frequency ?: HabitFrequency.DAILY
                )

                val milestone = StreakEngine.findMilestoneForStreak(currentStreak)
                if (milestone != null && habit != null) {
                    val celebratedKey = "milestone_celebrated_${habitId}_${currentStreak}"
                    val todayStr = StreakEngine.formatDate(currentToday)
                    val alreadyCelebrated = settingsRepo.getSetting(celebratedKey, "") == todayStr
                    if (!alreadyCelebrated) {
                        settingsRepo.setSetting(celebratedKey, todayStr)
                        val next = StreakEngine.getNextMilestone(currentStreak)
                        _celebratingMilestone.value = MilestoneCelebration(
                            habitName = habit.name,
                            streakDays = currentStreak,
                            milestone = milestone,
                            isNewUnlock = true,
                            nextMilestoneDays = next?.days,
                            nextMilestoneTitle = next?.title
                        )
                        soundPlayer.playCelebrationFanfare(vibrate = isVibrationEnabled.value)
                    } else {
                        soundPlayer.playPreview("chime", isVibrationEnabled.value)
                    }
                } else {
                    soundPlayer.playPreview("chime", isVibrationEnabled.value)
                }
                onMeaningfulActionDone()
            }
            try {
                if (date == currentToday) {
                    if (!isDone) {
                        com.example.native.AlarmRingingService.stop(getApplication(), habitId)
                        com.example.native.NotificationHelper(getApplication()).cancelNotification(habitId)
                    }
                    val habit = repository.getHabitById(habitId)
                    if (habit != null && habit.reminderEnabled && !habit.isArchived) {
                        alarmScheduler.scheduleHabitReminder(habit, isDoneTodayOverride = !isDone)
                    }
                }
                HabitWidget().updateAll(getApplication<Application>())
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createHabit(
        name: String,
        description: String,
        category: String,
        colorHex: String,
        iconName: String,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        ringMode: RingMode,
        ringtoneUri: String?,
        ringtoneName: String,
        vibrateEnabled: Boolean,
        frequency: HabitFrequency,
        additionalReminderTimes: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val safeHour = reminderHour.coerceIn(0, 23)
            val safeMinute = reminderMinute.coerceIn(0, 59)
            val habit = Habit(
                name = name.trim(),
                description = description.trim(),
                category = category,
                colorHex = colorHex,
                iconName = iconName,
                reminderEnabled = reminderEnabled,
                reminderHour = safeHour,
                reminderMinute = safeMinute,
                ringMode = ringMode,
                ringtoneUri = ringtoneUri,
                ringtoneName = ringtoneName,
                vibrateEnabled = vibrateEnabled,
                frequency = frequency,
                additionalReminderTimes = additionalReminderTimes
            )
            val id = repository.insertHabit(habit)
            val savedHabit = habit.copy(id = id)
            if (reminderEnabled) {
                alarmScheduler.scheduleHabitReminder(savedHabit)
            }
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch (e: Exception) { e.printStackTrace() }
            onMeaningfulActionDone()
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch(Dispatchers.IO) {
            val safeHabit = habit.copy(
                reminderHour = habit.reminderHour.coerceIn(0, 23),
                reminderMinute = habit.reminderMinute.coerceIn(0, 59)
            )
            repository.updateHabit(safeHabit)
            val notificationHelper = com.example.native.NotificationHelper(getApplication())
            if (safeHabit.reminderEnabled && !safeHabit.isArchived) {
                alarmScheduler.scheduleHabitReminder(safeHabit)
                if (safeHabit.ringMode != RingMode.SOUND_AND_VIBRATE || safeHabit.ringtoneUri.isNullOrBlank()) {
                    notificationHelper.cleanupHabitNotificationChannels(safeHabit.id)
                }
            } else {
                com.example.native.AlarmRingingService.stop(getApplication(), safeHabit.id)
                alarmScheduler.cancelHabitReminder(safeHabit.id)
                notificationHelper.cancelNotification(safeHabit.id)
                notificationHelper.cleanupHabitNotificationChannels(safeHabit.id)
            }
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch (e: Exception) { e.printStackTrace() }
            onMeaningfulActionDone()
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.native.AlarmRingingService.stop(getApplication(), habitId)
            alarmScheduler.cancelHabitReminder(habitId)
            val notificationHelper = com.example.native.NotificationHelper(getApplication())
            notificationHelper.cancelNotification(habitId)
            notificationHelper.cleanupHabitNotificationChannels(habitId)
            repository.deleteHabit(habitId)
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch (e: Exception) { e.printStackTrace() }
            onMeaningfulActionDone()
        }
    }

    fun toggleArchiveHabit(habit: Habit) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = habit.copy(isArchived = !habit.isArchived)
            repository.updateHabit(updated)
            if (updated.isArchived) {
                com.example.native.AlarmRingingService.stop(getApplication(), habit.id)
                com.example.native.NotificationHelper(getApplication()).cancelNotification(habit.id)
                alarmScheduler.cancelHabitReminder(habit.id)
            } else if (updated.reminderEnabled) {
                alarmScheduler.scheduleHabitReminder(updated)
            }
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch(e: Exception) { e.printStackTrace() }
        }
    }

    fun shareHabitStreak(habitWithStats: HabitWithStats) {
        val habit = habitWithStats.habit
        val text = """
            🔥 I'm on a ${habitWithStats.currentStreak}-day streak for '${habit.name}'!
            
            Total completions: ${habitWithStats.totalCompletions}
            Best streak: ${habitWithStats.longestStreak} days
            
            Tracked with Habit Streaks App. Keep the flame alive! 🚀
        """.trimIndent()

        val sendIntent: android.content.Intent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share your progress")
        shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            getApplication<Application>().startActivity(shareIntent)
        } catch (_: Exception) {
            android.widget.Toast.makeText(
                getApplication<Application>(),
                "No app available to share right now.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun shareText(text: String) {
        val sendIntent: android.content.Intent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share")
        shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            getApplication<Application>().startActivity(shareIntent)
        } catch (_: Exception) {
            android.widget.Toast.makeText(
                getApplication<Application>(),
                "No app available to share right now.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }



    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setDarkMode(enabled)
        }
    }

    fun setGlassmorphismEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setGlassmorphismEnabled(enabled)
        }
    }

    fun saveGlassVfxConfig(config: com.example.ui.theme.GlassVfxConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.saveGlassVfxConfig(config)
        }
    }

    fun set24HourFormat(is24H: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.set24HourFormat(is24H)
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun setWidgetEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setWidgetEnabled(enabled)
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch(e: Exception) { e.printStackTrace() }
        }
    }

    fun setWidgetUrgentHighlightEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setWidgetUrgentHighlightEnabled(enabled)
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch(e: Exception) { e.printStackTrace() }
        }
    }

    fun setWidgetShowCompleted(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setWidgetShowCompleted(enabled)
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch(e: Exception) { e.printStackTrace() }
        }
    }

    fun setWidgetQuickControlsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setWidgetQuickControlsEnabled(enabled)
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch(e: Exception) { e.printStackTrace() }
        }
    }

    fun syncWidgetNow() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch(e: Exception) { e.printStackTrace() }
        }
    }

    fun setThemeTemplate(template: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setThemeTemplate(template)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        AlarmScheduler.updateGlobalNotificationsCache(enabled)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setNotificationsEnabled(enabled)
            if (enabled) {
                alarmScheduler.reconcileAllReminders()
            } else {
                com.example.native.AlarmRingingService.stop(getApplication())
                alarmScheduler.cancelAllReminders()
                com.example.native.NotificationHelper(getApplication()).cancelAllNotifications()
            }
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setVibrationEnabled(enabled)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setDynamicColorEnabled(enabled)
        }
    }

    fun setDynamicRainbowEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setDynamicRainbowEnabled(enabled)
        }
    }

    fun saveCustomTheme(themeStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = savedCustomThemes.value
            val list = if (current.isBlank()) mutableListOf() else current.split(";;;").toMutableList()
            if (!list.contains(themeStr)) {
                list.add(themeStr)
                settingsRepo.setSavedCustomThemes(list.joinToString(";;;"))
            }
        }
    }

    fun deleteCustomTheme(themeStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = savedCustomThemes.value
            val list = current.split(";;;").toMutableList()
            list.remove(themeStr)
            settingsRepo.setSavedCustomThemes(list.joinToString(";;;"))
        }
    }

    fun setDynamicColorSource(source: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.setDynamicColorSource(source)
        }
    }


    // Sound & Preview
    fun getAvailableRingtones(): List<SoundPlayer.MusicalSoundOption> = soundPlayer.getAvailableRingtones()

    fun previewSound(uriString: String?, vibrate: Boolean = true) {
        soundPlayer.playPreview(uriString, vibrate && isVibrationEnabled.value)
    }

    fun stopSoundPreview() {
        soundPlayer.stopPreview()
    }

    fun triggerTestFullAlarm() {
        com.example.native.AlarmRingingService.start(
            context = getApplication(),
            habitId = -999L,
            habitName = "Habit Streaks Alarm Test",
            habitColor = "#6366F1",
            currentStreak = overallStats.value.currentBestStreak,
            ringtoneUri = null,
            playSound = true,
            vibrate = isVibrationEnabled.value,
            isTestAlarm = true
        )
    }

    fun saveCustomRingtone(inputStream: InputStream, fileName: String): Result<Pair<String, String>> {
        return soundPlayer.saveCustomRingtone(inputStream, fileName)
    }

    // Backup & Restore
    suspend fun exportBackupFile(): File = withContext(Dispatchers.IO) {
        backupManager.exportToFile()
    }

    suspend fun exportCsvFile(): File = withContext(Dispatchers.IO) {
        backupManager.exportCsvToFile()
    }

    fun addCompletionNote(habitId: Long, dateStr: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCompletionNote(habitId, dateStr, note)
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch (_: Exception) {}
        }
    }

    suspend fun importBackup(inputStream: InputStream, replaceExisting: Boolean): Result<String> {
        if (replaceExisting) {
            alarmScheduler.cancelAllReminders()
        }
        val result = backupManager.importBackup(inputStream, replaceExisting)
        if (result.isSuccess) {
            alarmScheduler.reconcileAllReminders()
            try {
                HabitWidget().updateAll(getApplication<Application>())
            } catch (_: Exception) {}
        } else if (replaceExisting) {
            alarmScheduler.reconcileAllReminders()
        }
        return result
    }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.release()
    }
}
