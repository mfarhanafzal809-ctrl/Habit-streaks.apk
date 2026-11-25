package com.example

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.glassmorphicCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.ui.components.AppDrawer
import com.example.ui.screens.ContactScreen
import com.example.ui.components.ThemeCustomizationBottomSheet
import com.example.util.findActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Habit
import com.example.ui.HabitViewModel
import com.example.ui.components.AddEditHabitDialog
import com.example.ui.components.AddHabitConfirmDialog
import com.example.ui.components.ProFeatureUnlockDialog
import com.example.ui.components.OfflineAdNoticeDialog
import com.example.ui.components.AdBannerPlaceholder
import com.example.ui.components.AdFreeModeSelectionDialog
import com.example.ui.components.AdFreePlaybackOverlay
import com.example.ui.components.AdFreeCelebrationDialog
import com.example.ui.components.BackupDialog
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.InitialUserNameDialog
import com.example.ui.components.MilestoneCelebrationDialog
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.HabitStreaksTheme

import com.example.ui.components.GlassMorphineVfxBottomSheet
import com.example.ui.components.QuickLauncherDialog
import com.example.ui.screens.HabitGuideScreen

enum class MainTab {
    HOME,
    CALENDAR,
    STATS,
    SETTINGS
}

enum class SubScreen {
    NONE,
    PRIVACY_POLICY,
    ABOUT,
    CONTACT,
    GUIDE
}

private sealed interface PendingProFeature {
    data object AddHabit : PendingProFeature
    data object QuickLauncher : PendingProFeature
    data class BuiltInTheme(
        val template: String,
        val darkMode: Boolean,
        val dynamicColor: Boolean
    ) : PendingProFeature
    data class CustomTheme(
        val template: String,
        val darkMode: Boolean,
        val dynamicColor: Boolean
    ) : PendingProFeature
    data object DynamicRainbow : PendingProFeature
    data class GlassVfx(val config: com.example.ui.theme.GlassVfxConfig) : PendingProFeature
    data object EnableGlassmorphism : PendingProFeature
    data object EnableWidget : PendingProFeature
    data class BackupExport(val proceed: () -> Unit) : PendingProFeature
    data object OpenThemeCustomizer : PendingProFeature
    data object OpenGlassVfxStudio : PendingProFeature
}

class MainActivity : ComponentActivity() {

    private val viewModel: HabitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.ads.AdManager.updateCurrentActivity(this@MainActivity)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.local.HabitDatabase.getInstance(applicationContext)
                db.habitDao().getAllHabitsList()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error warming up database", e)
            }
        }

        setContent {
            LaunchedEffect(Unit) {
                com.example.ads.AdManager.requestConsentAndInitialize(this@MainActivity)
            }

            val isDarkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val isDynamicColorEnabled by viewModel.isDynamicColorEnabled.collectAsStateWithLifecycle()
            val isDynamicRainbowEnabled by viewModel.isDynamicRainbowEnabled.collectAsStateWithLifecycle()
            val themeTemplate by viewModel.themeTemplate.collectAsStateWithLifecycle()
            val isGlassmorphismEnabled by viewModel.isGlassmorphismEnabled.collectAsStateWithLifecycle()
            val glassVfxConfig by viewModel.glassVfxConfig.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = isDarkModePref ?: systemDark

            DisposableEffect(darkTheme) {
                val barStyle = if (darkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.argb(0x66, 0x1B, 0x1B, 0x1B)
                    )
                }
                enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        window.isNavigationBarContrastEnforced = false
                    } catch (_: Throwable) {}
                }
                onDispose {}
            }

            HabitStreaksTheme(
                darkTheme = darkTheme,
                dynamicColor = isDynamicColorEnabled,
                dynamicRainbowEnabled = isDynamicRainbowEnabled,
                themeTemplate = themeTemplate,
                glassmorphismEnabled = isGlassmorphismEnabled,
                glassVfxConfig = glassVfxConfig
            ) {
                HabitStreaksApp(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setAppForeground(true)
    }

    override fun onResume() {
        super.onResume()
        com.example.ads.AdManager.updateCurrentActivity(this@MainActivity)
        com.example.ads.AdManager.refreshAdsIfNeeded(this@MainActivity)
        viewModel.refreshCurrentDate()
        viewModel.refreshAdFreeState()
    }

    override fun onStop() {
        super.onStop()
        viewModel.setAppForeground(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (com.example.ads.AdManager.currentActivity == this) {
            com.example.ads.AdManager.updateCurrentActivity(null)
        }
    }
}

@Composable
fun HabitStreaksApp(viewModel: HabitViewModel) {
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var isThemeDialogOpen by remember { mutableStateOf(false) }
    var isGlassVfxDialogOpen by remember { mutableStateOf(false) }
    var isQuickLauncherOpen by remember { mutableStateOf(false) }

    var isAddEditOpen by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var habitToDelete by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var isBackupDialogOpen by remember { mutableStateOf(false) }
    var showAddHabitConfirmDialog by remember { mutableStateOf(false) }
    var showOfflineNoticeDialog by remember { mutableStateOf(false) }
    var offlineNoticeErrorMessage by remember { mutableStateOf<String?>(null) }
    var isAdFreeSelectionOpen by remember { mutableStateOf(false) }
    var activeAdFreeSequence by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var pendingOfflineFeature by remember { mutableStateOf<PendingProFeature?>(null) }
    var pendingRewardedFeature by remember { mutableStateOf<PendingProFeature?>(null) }
    var pendingUnlockFeature by remember { mutableStateOf<PendingProFeature?>(null) }
    var celebrationHours by remember { mutableStateOf<Int?>(null) }
    var editingNoteTarget by remember { mutableStateOf<Triple<Habit, java.time.LocalDate, String>?>(null) }

    // Observed flows
    val isAdFreeActive by viewModel.isAdFreeActive.collectAsStateWithLifecycle()
    val adFreeRemainingText by viewModel.adFreeRemainingFormatted.collectAsStateWithLifecycle()
    val habitsFeedState by viewModel.habitsFeedState.collectAsStateWithLifecycle()
    val habitsWithStats by viewModel.habitsWithStats.collectAsStateWithLifecycle()
    val allActiveHabitsWithStats by viewModel.allActiveHabitsWithStats.collectAsStateWithLifecycle()
    val allHabitsWithStats by viewModel.allHabitsWithStats.collectAsStateWithLifecycle()
    val allCompletions by viewModel.allCompletions.collectAsStateWithLifecycle()
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val overallStats by viewModel.overallStats.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val effectiveDarkTheme = isDarkMode ?: systemDark
    val isDynamicColorEnabled by viewModel.isDynamicColorEnabled.collectAsStateWithLifecycle()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsStateWithLifecycle()
    val themeTemplate by viewModel.themeTemplate.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val isGlassmorphismEnabled by viewModel.isGlassmorphismEnabled.collectAsStateWithLifecycle()
    val glassVfxConfig by viewModel.glassVfxConfig.collectAsStateWithLifecycle()
    val isWidgetEnabled by viewModel.isWidgetEnabled.collectAsStateWithLifecycle()
    val isWidgetUrgentHighlightEnabled by viewModel.isWidgetUrgentHighlightEnabled.collectAsStateWithLifecycle()
    val isWidgetShowCompleted by viewModel.isWidgetShowCompleted.collectAsStateWithLifecycle()
    val isWidgetQuickControlsEnabled by viewModel.isWidgetQuickControlsEnabled.collectAsStateWithLifecycle()
    val is24HourFormat by viewModel.is24HourFormat.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val hasStoredUserName by viewModel.hasStoredUserName.collectAsStateWithLifecycle()
    var showFirstLaunchNameDialog by remember(hasCompletedOnboarding, hasStoredUserName) {
        mutableStateOf(hasCompletedOnboarding && !hasStoredUserName)
    }
    val celebratingMilestone by viewModel.celebratingMilestone.collectAsStateWithLifecycle()

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    if (!hasCompletedOnboarding) {
        OnboardingScreen(onFinish = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            viewModel.setOnboardingCompleted(true)
        })
        return
    }

    fun commitProFeature(feature: PendingProFeature) {
        when (feature) {
            PendingProFeature.AddHabit -> {
                editingHabit = null
                isAddEditOpen = true
            }
            PendingProFeature.QuickLauncher -> {
                isQuickLauncherOpen = true
            }
            is PendingProFeature.BuiltInTheme -> {
                viewModel.setDarkMode(feature.darkMode)
                viewModel.setDynamicColorEnabled(feature.dynamicColor)
                viewModel.setThemeTemplate(feature.template)
                isThemeDialogOpen = false
            }
            is PendingProFeature.CustomTheme -> {
                viewModel.setDarkMode(feature.darkMode)
                viewModel.setDynamicColorEnabled(feature.dynamicColor)
                viewModel.setThemeTemplate(feature.template)
                isThemeDialogOpen = false
            }
            PendingProFeature.DynamicRainbow -> {
                viewModel.setDynamicRainbowEnabled(true)
            }
            is PendingProFeature.GlassVfx -> {
                viewModel.saveGlassVfxConfig(feature.config)
                isGlassVfxDialogOpen = false
            }
            PendingProFeature.EnableGlassmorphism -> {
                viewModel.setGlassmorphismEnabled(true)
            }
            PendingProFeature.EnableWidget -> {
                viewModel.setWidgetEnabled(true)
            }
            is PendingProFeature.BackupExport -> {
                feature.proceed()
            }
            PendingProFeature.OpenThemeCustomizer -> {
                isThemeDialogOpen = true
            }
            PendingProFeature.OpenGlassVfxStudio -> {
                isGlassVfxDialogOpen = true
            }
        }
    }

    fun runWithInterstitialGateDirect(feature: PendingProFeature) {
        if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
            pendingOfflineFeature = feature
            offlineNoticeErrorMessage = null
            showOfflineNoticeDialog = true
        } else {
            val activity = context.findActivity() ?: com.example.ads.AdManager.currentActivity
            com.example.ads.AdManager.requestInterstitialAndContinue(activity, isFocusedTask = false, force = true) {
                commitProFeature(feature)
            }
        }
    }

    fun runWithRewardedGateDirect(feature: PendingProFeature) {
        if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
            pendingOfflineFeature = feature
            offlineNoticeErrorMessage = null
            showOfflineNoticeDialog = true
        } else {
            pendingRewardedFeature = feature
            activeAdFreeSequence = Pair(0, 2)
        }
    }

    fun runWithInterstitialGate(feature: PendingProFeature) {
        if (isAdFreeActive) {
            commitProFeature(feature)
        } else {
            pendingUnlockFeature = feature
        }
    }

    fun runWithRewardedGate(feature: PendingProFeature) {
        if (isAdFreeActive) {
            commitProFeature(feature)
        } else {
            pendingUnlockFeature = feature
        }
    }

    LaunchedEffect(Unit) {
        viewModel.interstitialTrigger.collect {
            val activity = context.findActivity()
            com.example.ads.AdManager.requestInterstitialAndContinue(activity, isFocusedTask = false, force = false) {}
        }
    }


    // Handle back button for sub screens
    BackHandler(enabled = currentSubScreen != SubScreen.NONE || drawerState.isOpen) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            currentSubScreen = SubScreen.NONE
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentTab = currentTab,
                currentSubScreen = currentSubScreen,
                onTabSelected = { tab ->
                    currentTab = tab
                    currentSubScreen = SubScreen.NONE
                    scope.launch { drawerState.close() }
                },
                onNavigateToSubScreen = { subScreen ->
                    currentSubScreen = subScreen
                    scope.launch { drawerState.close() }
                },
                themeTemplate = themeTemplate,
                onThemeCustomizeClick = {
                    scope.launch { drawerState.close() }
                    runWithRewardedGate(PendingProFeature.OpenThemeCustomizer)
                },
                onGlassVfxClick = {
                    scope.launch { drawerState.close() }
                    runWithRewardedGate(PendingProFeature.OpenGlassVfxStudio)
                },
                onQuickLauncherClick = {
                    scope.launch { drawerState.close() }
                    runWithInterstitialGate(PendingProFeature.QuickLauncher)
                },
                isAdFreeActive = isAdFreeActive,
                onOpenAdFreeClick = {
                    scope.launch { drawerState.close() }
                    if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                        com.example.util.NetworkUtils.showOfflineToast(context)
                        scope.launch {
                            snackbarHostState.showSnackbar(com.example.util.NetworkUtils.OFFLINE_MESSAGE)
                        }
                    } else {
                        isAdFreeSelectionOpen = true
                    }
                },
                isGlassVfxEnabled = isGlassmorphismEnabled
            )
        }
    ) {
        if (currentSubScreen == SubScreen.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onBack = { currentSubScreen = SubScreen.NONE })
        } else if (currentSubScreen == SubScreen.ABOUT) {
            AboutScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
        } else if (currentSubScreen == SubScreen.CONTACT) {
            ContactScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
        } else if (currentSubScreen == SubScreen.GUIDE) {
            HabitGuideScreen(
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onBack = { currentSubScreen = SubScreen.NONE }
            )
        } else {
            com.example.ui.theme.GlassmorphicBackdrop {
                val isDark = com.example.ui.theme.LocalIsDarkTheme.current
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (isGlassmorphismEnabled) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.background,
                    contentColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        val navPrimary = MaterialTheme.colorScheme.primary
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Fixed Ad Banner (Blocked when Ad-Free mode is active)
                            if (!isAdFreeActive) {
                                AdBannerPlaceholder()
                            }
                            // Navigation Bar
                            if (isGlassmorphismEnabled) {
                                val isDark = com.example.ui.theme.LocalIsDarkTheme.current
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .glassmorphicCard(shape = RoundedCornerShape(26.dp), borderWidth = 1.6.dp)
                                ) {
                                    NavigationBar(
                                        containerColor = Color.Transparent,
                                        tonalElevation = 0.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        NavigationBarItem(
                                            selected = currentTab == MainTab.HOME,
                                            onClick = { 
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                currentTab = MainTab.HOME 
                                            },
                                            icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = "Habits") },
                                            label = { Text("Habits", fontWeight = if (currentTab == MainTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = navPrimary,
                                                selectedTextColor = navPrimary,
                                                unselectedIconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                unselectedTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                indicatorColor = navPrimary.copy(alpha = 0.22f)
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == MainTab.CALENDAR,
                                            onClick = { 
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                currentTab = MainTab.CALENDAR 
                                            },
                                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                                            label = { Text("Calendar", fontWeight = if (currentTab == MainTab.CALENDAR) FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = navPrimary,
                                                selectedTextColor = navPrimary,
                                                unselectedIconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                unselectedTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                indicatorColor = navPrimary.copy(alpha = 0.22f)
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == MainTab.STATS,
                                            onClick = { 
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                currentTab = MainTab.STATS 
                                            },
                                            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Streaks") },
                                            label = { Text("Streaks", fontWeight = if (currentTab == MainTab.STATS) FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = navPrimary,
                                                selectedTextColor = navPrimary,
                                                unselectedIconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                unselectedTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                indicatorColor = navPrimary.copy(alpha = 0.22f)
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == MainTab.SETTINGS,
                                            onClick = { 
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                currentTab = MainTab.SETTINGS 
                                            },
                                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                            label = { Text("Settings", fontWeight = if (currentTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = navPrimary,
                                                selectedTextColor = navPrimary,
                                                unselectedIconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                unselectedTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                indicatorColor = navPrimary.copy(alpha = 0.22f)
                                            )
                                        )
                                    }
                                }
                            } else {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 6.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.HOME,
                                        onClick = { 
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            currentTab = MainTab.HOME 
                                        },
                                        icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = "Habits") },
                                        label = { Text("Habits") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = navPrimary,
                                            selectedTextColor = navPrimary,
                                            indicatorColor = navPrimary.copy(alpha = 0.15f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.CALENDAR,
                                        onClick = { 
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            currentTab = MainTab.CALENDAR 
                                        },
                                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                                        label = { Text("Calendar") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = navPrimary,
                                            selectedTextColor = navPrimary,
                                            indicatorColor = navPrimary.copy(alpha = 0.15f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.STATS,
                                        onClick = { 
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            currentTab = MainTab.STATS 
                                        },
                                        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Streaks") },
                                        label = { Text("Streaks") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = navPrimary,
                                            selectedTextColor = navPrimary,
                                            indicatorColor = navPrimary.copy(alpha = 0.15f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.SETTINGS,
                                        onClick = { 
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            currentTab = MainTab.SETTINGS 
                                        },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = navPrimary,
                                            selectedTextColor = navPrimary,
                                            indicatorColor = navPrimary.copy(alpha = 0.15f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(if (isGlassmorphismEnabled) Color.Transparent else MaterialTheme.colorScheme.background)
                ) {
                    when (currentTab) {
                        MainTab.HOME -> {
                            HomeScreen(
                                habitsWithStats = habitsWithStats,
                                overallStats = overallStats,
                                currentFilter = currentFilter,
                                searchQuery = searchQuery,
                                onFilterChange = { viewModel.setFilter(it) },
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onToggleHabitDone = { habitId, isDone ->
                                    viewModel.toggleHabitDoneToday(habitId, isDone)
                                    if (!isDone) {
                                        scope.launch {
                                            val res = snackbarHostState.showSnackbar(
                                                message = "Habit marked as complete! 🔥",
                                                actionLabel = "Undo"
                                            )
                                            if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                viewModel.toggleHabitDoneToday(habitId, true)
                                            }
                                        }
                                    }
                                },
                                habitsFeedState = habitsFeedState,
                                allHabitsForMetrics = allHabitsWithStats,
                                onEditTodayNote = { stats ->
                                    val todayDate = viewModel.today.value
                                    editingNoteTarget = Triple(stats.habit, todayDate, stats.todayNote)
                                },
                                onDateChanged = { viewModel.refreshCurrentDate() },
                                onSortOrderChange = { viewModel.setSortOrder(it) },
                                userName = userName,
                                onUserNameChange = { viewModel.setUserName(it) },

                                onAddHabitClick = {
                                    editingHabit = null
                                    if (isAdFreeActive || com.example.ads.AdManager.isOfflineBypassed()) {
                                        // Ad-free mode active or user already selected offline bypass
                                        isAddEditOpen = true
                                    } else if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                                        // Device is offline: display compact, accessible notice
                                        pendingOfflineFeature = PendingProFeature.AddHabit
                                        offlineNoticeErrorMessage = null
                                        showOfflineNoticeDialog = true
                                    } else {
                                        // Online and ad-eligible: show creation transition confirmation
                                        showAddHabitConfirmDialog = true
                                    }
                                },
                                onEditHabitClick = { habit ->
                                    editingHabit = habit
                                    isAddEditOpen = true
                                },
                                onDeleteHabitClick = { id, name ->
                                    habitToDelete = Pair(id, name)
                                },
                                onArchiveHabit = { habit ->
                                    viewModel.toggleArchiveHabit(habit)
                                },
                                onShareHabit = { stats ->
                                    viewModel.shareHabitStreak(stats)
                                },
                                onMilestoneClick = { milestone, bestHabitName, bestStreak ->
                                    viewModel.openMilestonePreview(milestone, bestHabitName, bestStreak)
                                },
                                isAdFreeActive = isAdFreeActive,
                                adFreeTimeRemainingText = adFreeRemainingText,
                                onOpenAdFreeClick = {
                                    if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                                        com.example.util.NetworkUtils.showOfflineToast(context)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(com.example.util.NetworkUtils.OFFLINE_MESSAGE)
                                        }
                                    } else {
                                        isAdFreeSelectionOpen = true
                                    }
                                },
                                is24HourFormat = is24HourFormat,
                                isNotificationsEnabled = isNotificationsEnabled
                            )

                        }
                        MainTab.CALENDAR -> {
                            CalendarScreen(
                                selectedMonth = selectedMonth,
                                selectedDate = selectedDate,
                                allHabits = allHabits,
                                allCompletions = allCompletions,
                                onMonthChange = { viewModel.setSelectedMonth(it) },
                                onDateSelected = { viewModel.setSelectedDate(it) },
                                onToggleHabitOnDate = { habitId, date, isDone ->
                                    viewModel.toggleHabitDoneOnDate(habitId, date, isDone)
                                    if (!isDone) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Marked done for ${date.month.name.lowercase()} ${date.dayOfMonth}!")
                                        }
                                    }
                                },
                                onEditNote = { habit, date, existingNote ->
                                    editingNoteTarget = Triple(habit, date, existingNote)
                                }
                            )

                        }
                        MainTab.STATS -> {
                            StatsScreen(
                                habitsWithStats = allActiveHabitsWithStats,
                                overallStats = overallStats,
                                allCompletions = allCompletions,
                                onMilestoneClick = { milestone, bestHabitName, bestStreak ->
                                    viewModel.openMilestonePreview(milestone, bestHabitName, bestStreak)
                                },
                                onShareReport = { report ->
                                    viewModel.shareText(report)
                                }
                            )
                        }

                        MainTab.SETTINGS -> {
                            SettingsScreen(
                                isDarkMode = effectiveDarkTheme,
                                isGlassmorphismEnabled = isGlassmorphismEnabled,
                                isDynamicColorEnabled = isDynamicColorEnabled,
                                isNotificationsEnabled = isNotificationsEnabled,
                                isVibrationEnabled = isVibrationEnabled,
                                isWidgetEnabled = isWidgetEnabled,
                                isWidgetUrgentHighlightEnabled = isWidgetUrgentHighlightEnabled,
                                isWidgetShowCompleted = isWidgetShowCompleted,
                                isWidgetQuickControlsEnabled = isWidgetQuickControlsEnabled,
                                onDarkModeToggle = { viewModel.setDarkMode(it) },
                                onGlassmorphismToggle = { enabled ->
                                    if (enabled) {
                                        runWithInterstitialGate(PendingProFeature.EnableGlassmorphism)
                                    } else {
                                        viewModel.setGlassmorphismEnabled(false)
                                    }
                                },
                                onDynamicColorToggle = { viewModel.setDynamicColorEnabled(it) },
                                onNotificationsToggle = { viewModel.setNotificationsEnabled(it) },
                                onVibrationToggle = { viewModel.setVibrationEnabled(it) },
                                onWidgetEnabledToggle = { enabled ->
                                    if (enabled) {
                                        runWithInterstitialGate(PendingProFeature.EnableWidget)
                                    } else {
                                        viewModel.setWidgetEnabled(false)
                                    }
                                },
                                onWidgetUrgentHighlightToggle = { viewModel.setWidgetUrgentHighlightEnabled(it) },
                                onWidgetShowCompletedToggle = { viewModel.setWidgetShowCompleted(it) },
                                onWidgetQuickControlsToggle = { viewModel.setWidgetQuickControlsEnabled(it) },
                                onSyncWidget = { viewModel.syncWidgetNow() },
                                onOpenBackupDialog = { isBackupDialogOpen = true },
                                onOpenPrivacyPolicy = { currentSubScreen = SubScreen.PRIVACY_POLICY },
                                onOpenAbout = { currentSubScreen = SubScreen.ABOUT },
                                onTestSound = { viewModel.previewSound(null, true) },
                                onTestFullAlarm = { viewModel.triggerTestFullAlarm() },
                                onOpenGlassVfx = { runWithRewardedGate(PendingProFeature.OpenGlassVfxStudio) },
                                isAdFreeActive = isAdFreeActive
                            )
                        }
                    }
                    
                    androidx.compose.material3.IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier
                            .padding(start = 9.dp, top = 2.dp)
                            .align(androidx.compose.ui.Alignment.TopStart),
                        colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            }
        }
    }

    if (isAddEditOpen) {
        val availableRingtones = remember(isAddEditOpen) { viewModel.getAvailableRingtones() }
        AddEditHabitDialog(
            initialHabit = editingHabit,
            availableRingtones = availableRingtones,
            onDismiss = {
                isAddEditOpen = false
                editingHabit = null
                viewModel.stopSoundPreview()
            },
            onSave = { name, desc, cat, color, icon, rem, hr, min, ring, uri, rName, vib, freq, additionalTimes ->
                if (editingHabit == null) {
                    viewModel.createHabit(name, desc, cat, color, icon, rem, hr, min, ring, uri, rName, vib, freq, additionalTimes)
                } else {
                    viewModel.updateHabit(
                        editingHabit!!.copy(
                            name = name,
                            description = desc,
                            category = cat,
                            colorHex = color,
                            iconName = icon,
                            reminderEnabled = rem,
                            reminderHour = hr,
                            reminderMinute = min,
                            ringMode = ring,
                            ringtoneUri = uri,
                            ringtoneName = rName,
                            vibrateEnabled = vib,
                            frequency = freq,
                            additionalReminderTimes = additionalTimes
                        )
                    )
                }
                isAddEditOpen = false
                editingHabit = null
                viewModel.stopSoundPreview()
            },
            is24Hour = is24HourFormat,
            isVibrationEnabled = isVibrationEnabled,
            isAdFreeActive = isAdFreeActive,
            onPreviewSound = { uri, vib ->
                viewModel.previewSound(uri, vib)
            },
            onStopPreview = {
                viewModel.stopSoundPreview()
            }
        )
    }

    if (habitToDelete != null) {
        ConfirmationDialog(
            title = "Delete Habit",
            message = "Are you sure you want to delete '${habitToDelete!!.second}'? This will permanently erase all its history and streaks.",
            onConfirm = {
                viewModel.deleteHabit(habitToDelete!!.first)
                habitToDelete = null
            },
            onDismiss = { habitToDelete = null }
        )
    }

    if (isBackupDialogOpen) {
        BackupDialog(
            onExportFile = { viewModel.exportBackupFile() },
            onExportCsvFile = { viewModel.exportCsvFile() },
            onImportStream = { stream, replace -> viewModel.importBackup(stream, replace) },
            isAdFreeActive = isAdFreeActive,
            onRequestExportAd = { proceed ->
                runWithInterstitialGate(PendingProFeature.BackupExport(proceed))
            },
            onDismiss = { isBackupDialogOpen = false }
        )
    }

    if (isThemeDialogOpen) {
        val savedCustomThemes by viewModel.savedCustomThemes.collectAsStateWithLifecycle()
        val dynamicColorSource by viewModel.dynamicColorSource.collectAsStateWithLifecycle()
        val isDynamicRainbowEnabled by viewModel.isDynamicRainbowEnabled.collectAsStateWithLifecycle()
        ThemeCustomizationBottomSheet(
            currentTemplate = themeTemplate,
            isDarkMode = effectiveDarkTheme,
            isDynamicColor = isDynamicColorEnabled,
            isDynamicRainbow = isDynamicRainbowEnabled,
            savedCustomThemes = savedCustomThemes,
            dynamicColorSource = dynamicColorSource,
            isAdFreeActive = isAdFreeActive,
            onDarkModeToggle = { viewModel.setDarkMode(it) },
            onDynamicColorToggle = { viewModel.setDynamicColorEnabled(it) },
            onDynamicRainbowToggle = { enabled ->
                if (enabled) {
                    runWithRewardedGate(PendingProFeature.DynamicRainbow)
                } else {
                    viewModel.setDynamicRainbowEnabled(false)
                }
            },
            onSaveCustomTheme = { themeStr -> viewModel.saveCustomTheme(themeStr) },
            onDeleteCustomTheme = { themeStr -> viewModel.deleteCustomTheme(themeStr) },
            onSetDynamicColorSource = { source -> viewModel.setDynamicColorSource(source) },
            onTemplateSelect = { template ->
                if (template.startsWith("CUSTOM:")) {
                    runWithRewardedGate(
                        PendingProFeature.CustomTheme(
                            template = template,
                            darkMode = effectiveDarkTheme,
                            dynamicColor = isDynamicColorEnabled
                        )
                    )
                } else {
                    runWithInterstitialGate(
                        PendingProFeature.BuiltInTheme(
                            template = template,
                            darkMode = effectiveDarkTheme,
                            dynamicColor = isDynamicColorEnabled
                        )
                    )
                }
            },
            onApplyThemeWithModes = { template, darkMode, dynamicColor ->
                if (template.startsWith("CUSTOM:")) {
                    runWithRewardedGate(PendingProFeature.CustomTheme(template, darkMode, dynamicColor))
                } else {
                    runWithInterstitialGate(PendingProFeature.BuiltInTheme(template, darkMode, dynamicColor))
                }
            },
            onDismiss = { isThemeDialogOpen = false },
            onOpenGlassVfx = { runWithRewardedGate(PendingProFeature.OpenGlassVfxStudio) }
        )
    }

    if (isGlassVfxDialogOpen) {
        GlassMorphineVfxBottomSheet(
            currentConfig = glassVfxConfig,
            isAdFreeActive = isAdFreeActive,
            onSaveConfig = { updated ->
                if (updated.enabled) {
                    runWithRewardedGate(PendingProFeature.GlassVfx(updated))
                } else {
                    viewModel.saveGlassVfxConfig(updated)
                    isGlassVfxDialogOpen = false
                }
            },
            onDismiss = { isGlassVfxDialogOpen = false }
        )
    }

    if (isQuickLauncherOpen) {
        QuickLauncherDialog(
            habitsWithStats = allActiveHabitsWithStats,
            isAdFreeActive = isAdFreeActive,
            onToggleHabitDone = { habitId, isDone ->
                viewModel.toggleHabitDoneToday(habitId, isDone)
                if (!isDone) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Habit marked as complete! 🔥")
                    }
                }
            },
            onAddHabit = {
                editingHabit = null
                if (isAdFreeActive || com.example.ads.AdManager.isOfflineBypassed()) {
                    isAddEditOpen = true
                } else if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                    pendingOfflineFeature = PendingProFeature.AddHabit
                    offlineNoticeErrorMessage = null
                    showOfflineNoticeDialog = true
                } else {
                    showAddHabitConfirmDialog = true
                }
            },
            onOpenCalendar = {
                currentSubScreen = SubScreen.NONE
                currentTab = MainTab.CALENDAR
            },
            onOpenStats = {
                currentSubScreen = SubScreen.NONE
                currentTab = MainTab.STATS
            },
            onOpenThemes = {
                isQuickLauncherOpen = false
                runWithRewardedGate(PendingProFeature.OpenThemeCustomizer)
            },
            onOpenGlassVfx = {
                isQuickLauncherOpen = false
                runWithRewardedGate(PendingProFeature.OpenGlassVfxStudio)
            },
            onDismiss = { isQuickLauncherOpen = false }
        )
    }

    celebratingMilestone?.let { celebration ->
        MilestoneCelebrationDialog(
            celebration = celebration,
            onDismiss = { viewModel.dismissCelebration() },
            onShare = { text ->
                viewModel.shareText(text)
            }
        )
    }

    if (showOfflineNoticeDialog) {
        OfflineAdNoticeDialog(
            errorMessage = offlineNoticeErrorMessage,
            onNoInternet = {
                val targetFeature = pendingOfflineFeature ?: PendingProFeature.AddHabit
                showOfflineNoticeDialog = false
                offlineNoticeErrorMessage = null
                pendingOfflineFeature = null
                com.example.ads.AdManager.setOfflineBypass(true)
                commitProFeature(targetFeature)
            },
            onHaveInternet = {
                // Recheck actual current connectivity state
                if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                    offlineNoticeErrorMessage = "Please turn on Wi-Fi or mobile data, then try again."
                } else {
                    val targetFeature = pendingOfflineFeature ?: PendingProFeature.AddHabit
                    showOfflineNoticeDialog = false
                    offlineNoticeErrorMessage = null
                    pendingOfflineFeature = null
                    com.example.ads.AdManager.setOfflineBypass(false)
                    when (targetFeature) {
                        PendingProFeature.AddHabit -> {
                            showAddHabitConfirmDialog = true
                        }
                        is PendingProFeature.CustomTheme,
                        PendingProFeature.DynamicRainbow,
                        is PendingProFeature.GlassVfx,
                        PendingProFeature.OpenThemeCustomizer,
                        PendingProFeature.OpenGlassVfxStudio -> {
                            pendingRewardedFeature = targetFeature
                            activeAdFreeSequence = Pair(0, 2)
                        }
                        else -> {
                            val activity = context.findActivity() ?: com.example.ads.AdManager.currentActivity
                            com.example.ads.AdManager.requestInterstitialAndContinue(activity, isFocusedTask = false, force = true) {
                                commitProFeature(targetFeature)
                            }
                        }
                    }
                }
            },
            onDismiss = {
                showOfflineNoticeDialog = false
                offlineNoticeErrorMessage = null
                pendingOfflineFeature = null
            }
        )
    }

    if (showAddHabitConfirmDialog) {
        AddHabitConfirmDialog(
            onConfirm = {
                showAddHabitConfirmDialog = false
                val activity = context.findActivity() ?: com.example.ads.AdManager.currentActivity
                com.example.ads.AdManager.requestInterstitialAndContinue(activity, isFocusedTask = false, force = true) {
                    editingHabit = null
                    isAddEditOpen = true
                }
            },
            onDismiss = {
                showAddHabitConfirmDialog = false
            }
        )
    }

    pendingUnlockFeature?.let { feature ->
        val featureName = when (feature) {
            PendingProFeature.AddHabit -> "Habit Creator Studio"
            PendingProFeature.QuickLauncher -> "Quick Launcher"
            is PendingProFeature.BuiltInTheme -> "Built-in Theme Palette"
            is PendingProFeature.CustomTheme -> "Custom Theme Creator"
            PendingProFeature.DynamicRainbow -> "Dynamic Rainbow Transition"
            is PendingProFeature.GlassVfx -> "Glass Morphine & VFX Studio"
            PendingProFeature.EnableGlassmorphism -> "Glassmorphism Effect"
            PendingProFeature.EnableWidget -> "Home Screen Widget"
            is PendingProFeature.BackupExport -> "Backup & Restore Data"
            PendingProFeature.OpenThemeCustomizer -> "Customize Theme"
            PendingProFeature.OpenGlassVfxStudio -> "Glass Morphine & VFX Studio"
        }
        val requiredAds = when (feature) {
            is PendingProFeature.CustomTheme,
            PendingProFeature.DynamicRainbow,
            is PendingProFeature.GlassVfx,
            PendingProFeature.OpenThemeCustomizer,
            PendingProFeature.OpenGlassVfxStudio -> 2
            else -> 1
        }

        ProFeatureUnlockDialog(
            featureName = featureName,
            requiredAdsCount = requiredAds,
            onWatchAdToUnlock = {
                pendingUnlockFeature = null
                when (requiredAds) {
                    2 -> runWithRewardedGateDirect(feature)
                    else -> runWithInterstitialGateDirect(feature)
                }
            },
            onGoAdFreeClick = {
                pendingUnlockFeature = null
                isAdFreeSelectionOpen = true
            },
            onDismiss = {
                pendingUnlockFeature = null
            }
        )
    }

    if (isAdFreeSelectionOpen) {
        AdFreeModeSelectionDialog(
            isCurrentlyActive = isAdFreeActive,
            remainingTimeText = adFreeRemainingText,
            onConfirmSelection = { hours, requiredAds ->
                if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                    isAdFreeSelectionOpen = false
                    activeAdFreeSequence = null
                    com.example.util.NetworkUtils.showOfflineToast(context)
                    scope.launch {
                        snackbarHostState.showSnackbar(com.example.util.NetworkUtils.OFFLINE_MESSAGE)
                    }
                } else {
                    isAdFreeSelectionOpen = false
                    pendingRewardedFeature = null
                    activeAdFreeSequence = Pair(hours, requiredAds)
                }
            },
            onDismiss = {
                isAdFreeSelectionOpen = false
            }
        )
    }

    activeAdFreeSequence?.let { (hours, requiredAds) ->
        val rewardedFeature = pendingRewardedFeature
        AdFreePlaybackOverlay(
            totalAds = requiredAds,
            targetHours = hours,
            featureTitle = when (rewardedFeature) {
                is PendingProFeature.CustomTheme -> "Unlocking Custom Theme"
                PendingProFeature.DynamicRainbow -> "Unlocking Dynamic Rainbow"
                is PendingProFeature.GlassVfx -> "Unlocking Glass Morphine & VFX"
                PendingProFeature.OpenThemeCustomizer -> "Unlocking Customize Theme"
                PendingProFeature.OpenGlassVfxStudio -> "Unlocking Glass Morphine & VFX Studio"
                null -> when (hours) {
                    6 -> "Unlocking 6 Hours Ad-Free"
                    12 -> "Unlocking 12 Hours Ad-Free"
                    else -> "Unlocking $hours Hours Ad-Free"
                }
                else -> "Unlocking Pro Feature"
            },
            onAllAdsCompleted = {
                val targetFeature = pendingRewardedFeature
                pendingRewardedFeature = null
                activeAdFreeSequence = null
                if (targetFeature != null) {
                    commitProFeature(targetFeature)
                } else {
                    val completedHours = hours
                    viewModel.activateAdFree(completedHours)
                    viewModel.playCelebrationSound()
                    celebrationHours = completedHours
                }
            },
            onCancel = {
                activeAdFreeSequence = null
                pendingRewardedFeature = null
            }
        )
    }

    celebrationHours?.let { hours ->
        AdFreeCelebrationDialog(
            hours = hours,
            onDismiss = {
                celebrationHours = null
            }
        )
    }

    if (showFirstLaunchNameDialog && hasCompletedOnboarding && !hasStoredUserName) {
        InitialUserNameDialog(
            onConfirm = { chosenName ->
                viewModel.setUserName(chosenName)
                showFirstLaunchNameDialog = false
            },
            onDismiss = {
                viewModel.setUserName("Champion")
                showFirstLaunchNameDialog = false
            }
        )
    }

    editingNoteTarget?.let { (habit, targetDate, initialNote) ->
        var noteText by remember(habit.id, targetDate, initialNote) { mutableStateOf(initialNote) }
        val dateStr = com.example.domain.StreakEngine.formatDate(targetDate)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editingNoteTarget = null },
            title = {
                Text(
                    text = "Reflection Note",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "${habit.name} • $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.Spacer(
                        modifier = androidx.compose.ui.Modifier.padding(top = 10.dp)
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = noteText,
                        onValueChange = { if (it.length <= 300) noteText = it },
                        label = { Text("How did it go?") },
                        placeholder = { Text("Capture a quick thought or win...") },
                        maxLines = 4,
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.addCompletionNote(habit.id, dateStr, noteText.trim())
                        editingNoteTarget = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Reflection saved!")
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { editingNoteTarget = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    com.example.ui.components.EnterpriseErrorDialogHost()
}
