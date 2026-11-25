package com.example.ui.screens

import com.example.ui.HabitSortOrder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextOverflow
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.core.content.ContextCompat
import com.example.native.AlarmScheduler
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.data.model.HabitWithStats
import com.example.domain.StreakEngine
import com.example.ui.HabitFilter
import com.example.ui.components.HabitCard
import com.example.ui.components.PremiumFeatureBadge
import com.example.ui.theme.HabitAmber
import com.example.ui.theme.glassmorphicCard
import com.example.ui.theme.glassmorphicCardColors
import com.example.ui.theme.glassmorphicCardElevation
import com.example.ui.theme.glassmorphicCardBorder
import com.example.ui.theme.pulsingGlow
import com.example.util.GreetingHelper
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    habitsWithStats: List<HabitWithStats>,
    overallStats: com.example.data.model.OverallStats,
    currentFilter: HabitFilter,
    searchQuery: String,
    onFilterChange: (HabitFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleHabitDone: (Long, Boolean) -> Unit,
    onAddHabitClick: () -> Unit,
    onEditHabitClick: (Habit) -> Unit,
    onDeleteHabitClick: (Long, String) -> Unit,
    onArchiveHabit: (Habit) -> Unit,
    onSortOrderChange: (HabitSortOrder) -> Unit,
    onShareHabit: (HabitWithStats) -> Unit,
    onMilestoneClick: ((com.example.data.model.StreakMilestone, String?, Int?) -> Unit)? = null,
    isAdFreeActive: Boolean = false,
    adFreeTimeRemainingText: String = "",
    onOpenAdFreeClick: () -> Unit = {},
    habitsFeedState: com.example.ui.HabitsFeedState = com.example.ui.HabitsFeedState.Success(habitsWithStats),
    allHabitsForMetrics: List<HabitWithStats> = habitsWithStats,
    onEditTodayNote: ((HabitWithStats) -> Unit)? = null,
    onDateChanged: (() -> Unit)? = null,
    is24HourFormat: Boolean = false,
    isNotificationsEnabled: Boolean = true,
    userName: String = "Champion",
    onUserNameChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {

    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val coroutineScope = rememberCoroutineScope()

    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var isHoldingGreeting by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }

    // Live exact current time & date state
    var currentDateTime by remember { mutableStateOf(GreetingHelper.getNow()) }
    LaunchedEffect(Unit) {
        var lastDayOfYear = currentDateTime.dayOfYear
        while (true) {
            // Check time periodically to keep AM/PM and minutes synchronized without continuous 1-second recomposition churn
            delay(15_000L)
            val updated = GreetingHelper.getNow()
            currentDateTime = updated
            if (updated.dayOfYear != lastDayOfYear) {
                lastDayOfYear = updated.dayOfYear
                onDateChanged?.invoke()
            }
        }
    }

    val formattedDate = remember(currentDateTime.dayOfYear) {
        GreetingHelper.formatFullHeaderDate(currentDateTime)
    }
    val formattedTime = remember(currentDateTime.hour, currentDateTime.minute, is24HourFormat) {
        if (is24HourFormat) {
            String.format(Locale.getDefault(), "%02d:%02d", currentDateTime.hour, currentDateTime.minute)
        } else {
            GreetingHelper.formatTimeWithAmPm(currentDateTime)
        }
    }
    val dynamicGreetingText = remember(currentDateTime.hour, userName) {
        GreetingHelper.getGreetingText(currentDateTime.hour, userName = userName, withEmoji = true)
    }

    val greetingView: @Composable (Color) -> Unit = { textColor ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isHoldingGreeting = true
                            holdProgress = 0f
                            val job = coroutineScope.launch {
                                val start = System.currentTimeMillis()
                                val duration = 3000L
                                while (isActive) {
                                    val elapsed = System.currentTimeMillis() - start
                                    holdProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                                    if (elapsed >= duration) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        showEditUsernameDialog = true
                                        break
                                    }
                                    delay(40L)
                                }
                            }
                            try {
                                awaitRelease()
                            } finally {
                                job.cancel()
                                isHoldingGreeting = false
                                holdProgress = 0f
                            }
                        }
                    )
                }
        ) {
            Column {
                Text(
                    text = dynamicGreetingText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    ),
                    color = textColor,
                    modifier = Modifier.testTag("dynamic_greeting_text")
                )
                if (isHoldingGreeting) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { holdProgress },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = primaryColor
                    )
                    Text(
                        text = "Hold for 3s to edit username...",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor
                    )
                }
            }
        }
    }

    if (showEditUsernameDialog) {
        var tempName by remember { mutableStateOf(userName) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = { Text("Change Username") },
            text = {
                Column {
                    Text("Enter your new username (hold greeting 3s anytime to change):")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    val finalName = tempName.trim().ifBlank { "Champion" }
                    onUserNameChange(finalName)
                    showEditUsernameDialog = false
                    android.widget.Toast.makeText(context, "Username updated to $finalName!", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showEditUsernameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    val (totalCompletions, bestStreak, activeHabits) = overallStats

    // Advanced Stats for today (keyed on current date and full habit list, unaffected by active filter/search)
    val activeToday = currentDateTime.toLocalDate()
    val todayHabits = remember(allHabitsForMetrics, activeToday) {
        allHabitsForMetrics.filter { !it.habit.isArchived && StreakEngine.isDueOn(it.habit.frequency, activeToday) }
    }
    val todayCompletedCount = remember(todayHabits) {
        todayHabits.count { it.isCompletedToday }
    }
    val todayTotalCount = todayHabits.size
    val todayProgress = remember(todayCompletedCount, todayTotalCount) {
        if (todayTotalCount > 0) todayCompletedCount.toFloat() / todayTotalCount.toFloat() else 0f
    }

    val activeTotalCount = remember(allHabitsForMetrics) { allHabitsForMetrics.count { !it.habit.isArchived } }
    val activePendingCount = remember(allHabitsForMetrics) { allHabitsForMetrics.count { !it.habit.isArchived && !it.isCompletedToday } }
    val activeCompletedCount = remember(allHabitsForMetrics) { allHabitsForMetrics.count { !it.habit.isArchived && it.isCompletedToday } }
    val archivedCount = remember(allHabitsForMetrics) { allHabitsForMetrics.count { it.habit.isArchived } }

    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gradientBrush = remember(primaryColor, secondaryColor) {
        Brush.linearGradient(
            colors = listOf(primaryColor, secondaryColor)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 46.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Enterprise Hero Section
            // Enterprise Hero Section (Natural Atmospheric Floating Greeting + Floating 0/0 Tracker)
            item {
                val isGlass = com.example.ui.theme.LocalGlassmorphismEnabled.current
                val isDark = com.example.ui.theme.LocalIsDarkTheme.current

                if (isGlass) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Crystal-Clear Convex Glass Hero Greeting Card
                        val vfxConfig = com.example.ui.theme.LocalGlassVfxConfig.current
                        val (vfxPrimary, vfxSecondary) = vfxConfig.resolveGlowColors(
                            themePrimary = primaryColor,
                            themeSecondary = MaterialTheme.colorScheme.secondary,
                            isDark = isDark
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicCard(shape = RoundedCornerShape(26.dp), borderWidth = 1.6.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = glassmorphicCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = glassmorphicCardElevation(defaultElevation = 0.dp),
                            border = null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                // Top Suite Status Row matching reference hero header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Current Time: $formattedTime (${currentDateTime.zone.id})",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(vfxPrimary.copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = vfxPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$formattedDate • $formattedTime".uppercase(Locale.getDefault()),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.8.sp
                                            ),
                                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                                        )
                                    }

                                    // Radiant Circular Progress Ring
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(58.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { 1f },
                                            modifier = Modifier.fillMaxSize(),
                                            color = vfxPrimary.copy(alpha = 0.18f),
                                            strokeWidth = 5.5.dp,
                                        )
                                        CircularProgressIndicator(
                                            progress = { todayProgress },
                                            modifier = Modifier.fillMaxSize(),
                                            color = vfxPrimary,
                                            strokeWidth = 5.5.dp,
                                        )
                                        Text(
                                            text = "$todayCompletedCount/$todayTotalCount",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
                                            ),
                                            color = if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                greetingView(if (isDark) Color.White else Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Hold greeting for 3s to change name",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = if (isDark) Color(0xFFCBD5E1).copy(alpha = 0.72f) else Color(0xFF475569)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) Color(0xFF061322).copy(alpha = 0.56f) else Color.White.copy(alpha = 0.65f),
                                    border = BorderStroke(
                                        1.dp,
                                        vfxPrimary.copy(alpha = if (isDark) 0.30f else 0.45f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TipsAndUpdates,
                                            contentDescription = null,
                                            tint = HabitAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = com.example.domain.QuoteEngine.getDailyQuote(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                fontSize = 12.sp
                                            ),
                                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Floating Glassmorphic "Go Ad-Free" Card matching reference pictures
                        if (isAdFreeActive) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphicCard(shape = RoundedCornerShape(20.dp), borderWidth = 1.6.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onOpenAdFreeClick()
                                    }
                                    .testTag("ad_free_status_banner"),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981).copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VerifiedUser,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Ad-Free Mode Active 🛡️",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.25f)
                                                ) {
                                                    Text(
                                                        text = "BLOCKER ON",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 9.sp
                                                        ),
                                                        color = Color(0xFF10B981),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (adFreeTimeRemainingText.isNotBlank()) "$adFreeTimeRemainingText remaining • All ads blocked" else "Active • Zero ads across app",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF00E5FF) else Color(0xFF2563EB),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        } else {
                            val themeSecondary = MaterialTheme.colorScheme.secondary
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphicCard(shape = RoundedCornerShape(20.dp), borderWidth = 1.6.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onOpenAdFreeClick()
                                    }
                                    .testTag("go_ad_free_button"),
                                color = Color.Transparent
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    primaryColor.copy(alpha = if (isDark) 0.55f else 0.40f),
                                                    themeSecondary.copy(alpha = if (isDark) 0.40f else 0.28f)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 16.dp, vertical = 13.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bolt,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Go Ad-Free (6h / 12h)",
                                                        style = MaterialTheme.typography.titleSmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 15.sp
                                                        ),
                                                        color = Color.White
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    PremiumFeatureBadge(
                                                        requiredAdsLabel = "",
                                                        isAdFreeActive = false,
                                                        compact = true,
                                                        onDarkSurface = true,
                                                        customPillText = "PREMIUM PASS",
                                                        showAdCount = false
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Block all banners & popups",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                    color = Color.White.copy(alpha = 0.90f)
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = com.example.ui.theme.glassmorphicCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = com.example.ui.theme.glassmorphicCardElevation(defaultElevation = 4.dp),
                        border = com.example.ui.theme.glassmorphicCardBorder()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(gradientBrush, alpha = 0.15f)
                            )
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Current Time: $formattedTime (${currentDateTime.zone.id})",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "$formattedDate • $formattedTime",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = primaryColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        greetingView(MaterialTheme.colorScheme.onBackground)
                                    }
                                    
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { 1f },
                                            modifier = Modifier.fillMaxSize(),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            strokeWidth = 6.dp,
                                        )
                                        CircularProgressIndicator(
                                            progress = { todayProgress },
                                            modifier = Modifier.fillMaxSize(),
                                            color = primaryColor,
                                            strokeWidth = 6.dp,
                                        )
                                        Text(
                                            text = "$todayCompletedCount/$todayTotalCount",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TipsAndUpdates,
                                            contentDescription = null,
                                            tint = HabitAmber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = com.example.domain.QuoteEngine.getDailyQuote(),
                                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (isAdFreeActive) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .pulsingGlow(Color(0xFF10B981))
                                            .clickable {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                onOpenAdFreeClick()
                                            }
                                            .testTag("ad_free_status_banner"),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.16f),
                                        border = BorderStroke(1.5.dp, Color(0xFF10B981))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF10B981).copy(alpha = 0.25f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VerifiedUser,
                                                        contentDescription = null,
                                                        tint = Color(0xFF10B981),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "Ad-Free Mode Active 🛡️",
                                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                                            color = Color(0xFF10B981)
                                                        )
                                                    }
                                                    Text(
                                                        text = if (adFreeTimeRemainingText.isNotBlank()) "$adFreeTimeRemainingText remaining" else "Active",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else {
                                    val themeSecondary = MaterialTheme.colorScheme.secondary
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .pulsingGlow(primaryColor)
                                            .clickable {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                onOpenAdFreeClick()
                                            }
                                            .testTag("go_ad_free_button"),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.Transparent
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(primaryColor, themeSecondary)
                                                    )
                                                )
                                                .padding(horizontal = 14.dp, vertical = 11.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(38.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.22f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Bolt,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = "Go Ad-Free (6h / 12h)",
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                                                color = Color.White
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            PremiumFeatureBadge(
                                                                requiredAdsLabel = "",
                                                                isAdFreeActive = false,
                                                                compact = true,
                                                                onDarkSurface = true,
                                                                customPillText = "PREMIUM PASS",
                                                                showAdCount = false
                                                            )
                                                        }
                                                        Text(
                                                            text = "Block all banners & popups",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                            color = Color.White.copy(alpha = 0.90f)
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Stats Banner
            item {
                val isGlass = com.example.ui.theme.LocalGlassmorphismEnabled.current
                val isDark = com.example.ui.theme.LocalIsDarkTheme.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f).glassmorphicCard(shape = RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = glassmorphicCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = glassmorphicCardElevation(0.dp),
                        border = null
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$bestStreak",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = primaryColor
                            )
                            Text(
                                "Best Streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f).glassmorphicCard(shape = RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = glassmorphicCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = glassmorphicCardElevation(0.dp),
                        border = null
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = HabitAmber, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$totalCompletions",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isGlass && isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Completions",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Next Milestone Target Radar Card
            val nextMilestone = StreakEngine.getNextMilestone(bestStreak)
            if (nextMilestone != null) {
                item {
                    val isGlass = com.example.ui.theme.LocalGlassmorphismEnabled.current
                    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
                    val remaining = nextMilestone.days - bestStreak
                    val radarColor = runCatching {
                        Color(android.graphics.Color.parseColor(nextMilestone.primaryColorHex))
                    }.getOrDefault(HabitAmber)
                    val bestHabit = allHabitsForMetrics.filter { !it.habit.isArchived }.maxByOrNull { it.currentStreak }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isGlass) {
                                    Modifier.glassmorphicCard(shape = RoundedCornerShape(18.dp), borderWidth = 1.6.dp)
                                } else Modifier
                            )
                            .then(if (onMilestoneClick != null) Modifier.clickable {
                                onMilestoneClick.invoke(nextMilestone, bestHabit?.habit?.name, bestStreak)
                            } else Modifier),
                        shape = RoundedCornerShape(18.dp),
                        colors = com.example.ui.theme.glassmorphicCardColors(containerColor = radarColor.copy(alpha = 0.12f)),
                        elevation = com.example.ui.theme.glassmorphicCardElevation(0.dp),
                        border = if (isGlass) null else androidx.compose.foundation.BorderStroke(1.2.dp, radarColor.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(radarColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = radarColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "NEXT UNLOCK: ${nextMilestone.title.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                                        color = primaryColor
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = primaryColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$remaining DAYS TO GO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = primaryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Keep your streak unbroken to unlock the ${nextMilestone.badgeTier} badge and claim your celebration! 🎉",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isGlass) (if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)) else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val progressRatio = (bestStreak.toFloat() / nextMilestone.days.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = primaryColor,
                                trackColor = if (isGlass) (if (isDark) Color(0xFF08182B) else Color(0xFFE2E8F0)) else MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Current: $bestStreak Days",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Target: ${nextMilestone.days} Days • Tap to preview 🎯",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }
            }
            
            // Upcoming Alarm & Readiness Card
            item {
                val isGlass = com.example.ui.theme.LocalGlassmorphismEnabled.current
                val isDark = com.example.ui.theme.LocalIsDarkTheme.current
                val scheduler = remember(context) { AlarmScheduler(context) }
                val activeMetricHabits = remember(allHabitsForMetrics) {
                    allHabitsForMetrics.filter { !it.habit.isArchived }
                }
                val completedIds = remember(activeMetricHabits) {
                    activeMetricHabits.filter { it.isCompletedToday }.map { it.habit.id }.toSet()
                }
                val nextAlarm = remember(activeMetricHabits, currentDateTime.minute, currentDateTime.hour, isNotificationsEnabled) {
                    AlarmScheduler.updateGlobalNotificationsCache(isNotificationsEnabled)
                    if (isNotificationsEnabled) {
                        scheduler.getNextScheduledAlarm(activeMetricHabits.map { it.habit }, completedIds)
                    } else null
                }
                val hasNotifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true
                val canExact = scheduler.canScheduleExactAlarms()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isGlass) {
                                Modifier.glassmorphicCard(shape = RoundedCornerShape(18.dp), borderWidth = 1.6.dp)
                            } else Modifier
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = com.example.ui.theme.glassmorphicCardColors(
                        containerColor = if (nextAlarm != null && (!hasNotifPerm || !canExact)) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        } else {
                            primaryColor.copy(alpha = 0.12f)
                        }
                    ),
                    elevation = com.example.ui.theme.glassmorphicCardElevation(0.dp),
                    border = null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    primaryColor.copy(alpha = if (isGlass && isDark) 0.22f else 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    nextAlarm != null && (!hasNotifPerm || !canExact) -> Icons.Default.Warning
                                    nextAlarm != null -> Icons.Default.Alarm
                                    else -> Icons.Default.NotificationsOff
                                },
                                contentDescription = "Upcoming Alarm",
                                tint = if (nextAlarm != null && (!hasNotifPerm || !canExact)) MaterialTheme.colorScheme.error else primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (nextAlarm != null) {
                                val nextTimeStr = if (is24HourFormat) {
                                    String.format(Locale.getDefault(), "%02d:%02d", nextAlarm.hour, nextAlarm.minute)
                                } else {
                                    val amPm = if (nextAlarm.hour >= 12) "PM" else "AM"
                                    val h12 = if (nextAlarm.hour % 12 == 0) 12 else nextAlarm.hour % 12
                                    String.format(Locale.getDefault(), "%02d:%02d %s", h12, nextAlarm.minute, amPm)
                                }
                                val diffMins = ((nextAlarm.triggerTimeMillis - System.currentTimeMillis()) / 60000L).coerceAtLeast(1L)
                                val countdown = when {
                                    diffMins >= 1440L -> "${diffMins / 1440L}d ${(diffMins % 1440L) / 60L}h"
                                    diffMins >= 60L -> "${diffMins / 60L}h ${diffMins % 60L}m"
                                    else -> "${diffMins}m"
                                }
                                Text(
                                    text = "Next Alarm • $nextTimeStr (in $countdown)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = primaryColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = nextAlarm.habit.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isGlass) (if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap to enable Exact Alarms for lock-screen precision",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.clickable {
                                            try {
                                                context.startActivity(
                                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                        data = Uri.parse("package:${context.packageName}")
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                )
                                            } catch (_: Exception) {}
                                        }
                                    )
                                }
                            } else if (!isNotificationsEnabled) {
                                Text(
                                    text = "Habit Reminders Paused",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Global Habit Reminders are turned OFF in Settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isGlass) (if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Alarm Engine Ready",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = primaryColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Enable reminders on any habit for clock-exact sound & vibration alerts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isGlass) (if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Search & Filter Header
            item {
                val isGlass = com.example.ui.theme.LocalGlassmorphismEnabled.current
                val isDark = com.example.ui.theme.LocalIsDarkTheme.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your Habits",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isGlass) (if (isDark) Color.White else Color(0xFF0F172A)) else MaterialTheme.colorScheme.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            IconButton(
                                onClick = { isSortMenuExpanded = true }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort Habits",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = isSortMenuExpanded,
                                onDismissRequest = { isSortMenuExpanded = false }
                            ) {
                                HabitSortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            onSortOrderChange(order)
                                            isSortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) onSearchChange("")
                            }
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "Search Habits",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Search Bar (Animated)
            item {
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Filter habits by name or category...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                            focusedPlaceholderColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            unfocusedPlaceholderColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            focusedLeadingIconColor = if (isDark) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                            unfocusedLeadingIconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            focusedBorderColor = if (isDark) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Filter Chips
            item {
                val primaryColor = MaterialTheme.colorScheme.primary
                val isDark = com.example.ui.theme.LocalIsDarkTheme.current
                val isGlass = com.example.ui.theme.LocalGlassmorphismEnabled.current
                val vfxConfig = com.example.ui.theme.LocalGlassVfxConfig.current
                val (vfxGlow, _) = vfxConfig.resolveGlowColors(
                    themePrimary = primaryColor,
                    themeSecondary = MaterialTheme.colorScheme.secondary,
                    isDark = isDark
                )
                val chipSelectedContainer = if (isGlass && isDark) vfxGlow else primaryColor
                val chipSelectedLabel = if (isGlass && isDark) Color(0xFF04121F) else MaterialTheme.colorScheme.onPrimary
                val chipUnselectedLabel = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                val chipUnselectedContainer = if (isGlass && isDark) Color(0xFF0A1220).copy(alpha = 0.56f) else Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        HabitFilter.ALL to "All ($activeTotalCount)",
                        HabitFilter.PENDING to "Pending ($activePendingCount)",
                        HabitFilter.COMPLETED to "Completed ($activeCompletedCount)",
                        HabitFilter.ARCHIVED to "Archived ($archivedCount)"
                    ).forEach { (filter, title) ->
                        val isSelected = currentFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterChange(filter) },
                            label = {
                                Text(
                                    title,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(50),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isGlass && isDark) vfxGlow.copy(alpha = 0.30f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = chipSelectedContainer
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = chipUnselectedContainer,
                                labelColor = chipUnselectedLabel,
                                selectedContainerColor = chipSelectedContainer,
                                selectedLabelColor = chipSelectedLabel
                            )
                        )
                    }
                }
            }

            // Habit Cards List with distinct Loading, Error, Empty, and Success states
            when (habitsFeedState) {
                is com.example.ui.HabitsFeedState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                                .testTag("habits_loading_indicator"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading your habit streaks...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                is com.example.ui.HabitsFeedState.Error -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                                .testTag("habits_error_state"),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Unable to load habits",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = habitsFeedState.errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                is com.example.ui.HabitsFeedState.Success -> {
                    val displayedHabits = habitsFeedState.habits
                    if (displayedHabits.isEmpty()) {
                        item {
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val emptyTitle: String
                            val emptySubtitle: String
                            val emptyIcon = when {
                                searchQuery.isNotBlank() -> Icons.Default.Search
                                currentFilter == HabitFilter.PENDING && activeTotalCount > 0 -> Icons.Default.CheckCircle
                                currentFilter == HabitFilter.ARCHIVED -> Icons.Default.Archive
                                else -> Icons.Default.LocalFireDepartment
                            }
                            when {
                                searchQuery.isNotBlank() -> {
                                    emptyTitle = "No matching habits found"
                                    emptySubtitle = "Try a different keyword or clear your search filter."
                                }
                                currentFilter == HabitFilter.PENDING && activeTotalCount > 0 -> {
                                    emptyTitle = "All caught up for today! 🎉"
                                    emptySubtitle = "Every active habit has been completed today."
                                }
                                currentFilter == HabitFilter.COMPLETED -> {
                                    emptyTitle = "No completions yet today"
                                    emptySubtitle = "Switch to Pending or All to check off your first habit."
                                }
                                currentFilter == HabitFilter.ARCHIVED -> {
                                    emptyTitle = "No archived habits"
                                    emptySubtitle = "Archived habits appear here whenever you pause one."
                                }
                                else -> {
                                    emptyTitle = "No habits yet!"
                                    emptySubtitle = "Pick a science-backed template or create a custom habit to ignite your streak."
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp)
                                    .testTag("habits_empty_state"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = emptyIcon,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = emptyTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = emptySubtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (searchQuery.isNotBlank() || currentFilter != HabitFilter.ALL) {
                                        OutlinedButton(
                                            onClick = {
                                                if (searchQuery.isNotBlank()) onSearchChange("")
                                                if (currentFilter != HabitFilter.ALL) onFilterChange(HabitFilter.ALL)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Reset Filters")
                                        }
                                    } else {
                                        Button(
                                            onClick = onAddHabitClick,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Create Your First Habit", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(displayedHabits, key = { it.habit.id }) { habitStats ->
                            HabitCard(
                                habitWithStats = habitStats,
                                onToggleDone = { isDone ->
                                    if (!isDone) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onToggleHabitDone(habitStats.habit.id, isDone)
                                },
                                onEdit = { onEditHabitClick(habitStats.habit) },
                                onDelete = { onDeleteHabitClick(habitStats.habit.id, habitStats.habit.name) },
                                onArchive = { onArchiveHabit(habitStats.habit) },
                                onShare = { onShareHabit(habitStats) },
                                onEditTodayNote = onEditTodayNote?.let { cb -> { cb(habitStats) } },
                                is24Hour = is24HourFormat,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(300),
                                    fadeOutSpec = tween(300),
                                    placementSpec = tween(300)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button with gradient and pristine circular shape
        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onAddHabitClick()
            },
            containerColor = Color.Transparent,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .background(gradientBrush, CircleShape)
                .testTag("add_habit_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Habit", modifier = Modifier.size(28.dp))
        }
    }
}
