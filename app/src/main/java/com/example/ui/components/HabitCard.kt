package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.data.model.HabitFrequency
import com.example.data.model.HabitWithStats
import com.example.data.model.RingMode
import com.example.domain.StreakEngine
import com.example.ui.theme.HabitAmber
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.glassmorphicCard
import java.time.LocalDate
import java.util.Locale

@Composable
fun HabitCard(
    habitWithStats: HabitWithStats,
    onToggleDone: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onShare: () -> Unit,
    onEditTodayNote: (() -> Unit)? = null,
    is24Hour: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val habit = habitWithStats.habit
    val isDone = habitWithStats.isCompletedToday
    var menuExpanded by remember { mutableStateOf(false) }

    val habitColor = runCatching {
        Color(android.graphics.Color.parseColor(habit.colorHex))
    }.getOrDefault(PrimaryIndigo)

    val scale by animateFloatAsState(
        targetValue = if (isDone) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    val checkBgColor by animateColorAsState(
        targetValue = if (isDone) habitColor else Color.Transparent,
        label = "check_bg"
    )

    val isGlass = com.example.ui.theme.LocalGlassmorphismEnabled.current
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (isGlass) {
                    Modifier.glassmorphicCard(shape = RoundedCornerShape(20.dp), borderWidth = 1.6.dp)
                } else Modifier
            )
            .testTag("habit_item_card_${habit.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = com.example.ui.theme.glassmorphicCardColors(
            containerColor = if (isDone) {
                habitColor.copy(alpha = if (isGlass) 0.15f else 0.06f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = com.example.ui.theme.glassmorphicCardElevation(defaultElevation = if (isDone) 1.dp else 4.dp),
        border = if (isGlass) null else (
            if (isDone) androidx.compose.foundation.BorderStroke(1.2.dp, habitColor.copy(alpha = 0.50f))
            else androidx.compose.foundation.BorderStroke(1.2.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.70f) else Color(0xFF94A3B8).copy(alpha = 0.65f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Streak badge & Category
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGlass) habitColor.copy(alpha = 0.18f) else habitColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = habit.category.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = if (isGlass && isDark) habitColor.copy(alpha = 0.95f) else habitColor
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (habit.frequency != HabitFrequency.DAILY) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val isDueToday = remember(habit.frequency) {
                            StreakEngine.isDueOn(habit.frequency, com.example.util.GreetingHelper.getNow().toLocalDate())
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = when {
                                    !isDueToday && !isDone -> "REST DAY"
                                    habit.frequency == HabitFrequency.WEEKDAYS -> "WEEKDAYS"
                                    else -> "WEEKENDS"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Flame Streak pill
                    if (habitWithStats.currentStreak > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFF3D00), Color(0xFFFF9100))
                                    )
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${habitWithStats.currentStreak}d Streak",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Start Streak ⚡",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (habitWithStats.longestStreak > habitWithStats.currentStreak && habitWithStats.longestStreak > 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HabitAmber.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = HabitAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Best ${habitWithStats.longestStreak}d",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = HabitAmber
                                    )
                                )
                            }
                        }
                    }
                }

                // Options Menu
                Box {
                    IconButton(
                        onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            menuExpanded = true 
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Habit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (habit.isArchived) "Unarchive Habit" else "Archive Habit") },
                            leadingIcon = { Icon(if (habit.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                menuExpanded = false
                                onArchive()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Streak") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                menuExpanded = false
                                onShare()
                            }
                        )
                        if (onEditTodayNote != null) {
                            DropdownMenuItem(
                                text = { Text(if (habitWithStats.todayNote.isNotBlank()) "Edit Today's Note" else "Add Today's Note") },
                                leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    menuExpanded = false
                                    onEditTodayNote()
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("Delete Habit", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                menuExpanded = false
                                onDelete()
                            }
                        )

                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Habit Icon, Name and Action Buttons Row
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(habitColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getHabitIconVector(habit.iconName),
                            contentDescription = getHabitIconLabel(habit.iconName),
                            tint = habitColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = if (isGlass) (if (isDark) Color.White else Color(0xFF0F172A)) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (habit.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 7-Day Streak Chain Visualizer
                val today = remember(habitWithStats) { com.example.util.GreetingHelper.getNow().toLocalDate() }
                val pastWeekDays = remember(today) {
                    (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isGlass) {
                                if (isDark) Color(0xFF08182B).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.65f)
                            } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                        .border(
                            width = if (isGlass) 1.dp else 0.dp,
                            color = if (isGlass) {
                                if (isDark) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0xFF93C5FD).copy(alpha = 0.40f)
                            } else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pastWeekDays.forEach { date ->
                        val dateStr = StreakEngine.formatDate(date)
                        val isCompletedOnDate = habitWithStats.recentCompletions.contains(dateStr)
                        val isToday = date == today
                        val dayLetter = date.dayOfWeek.name.take(1)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = dayLetter,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isToday) FontWeight.Black else FontWeight.Medium
                                ),
                                color = if (isToday) {
                                    if (isGlass && isDark) Color(0xFF00E5FF) else PrimaryIndigo
                                } else {
                                    if (isGlass) {
                                        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCompletedOnDate -> habitColor
                                            isToday -> habitColor.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        }
                                    )
                                    .then(
                                        if (isToday && !isCompletedOnDate) {
                                            Modifier.border(1.5.dp, habitColor, CircleShape)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompletedOnDate) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Powerful Done / Undo Action Buttons
                if (isDone) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onToggleDone(isDone) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("habit_toggle_button_${habit.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("COMPLETED ✓ (Undo)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                        if (onEditTodayNote != null) {
                            FilledTonalIconButton(
                                onClick = onEditTodayNote,
                                modifier = Modifier.size(48.dp).testTag("habit_note_button_${habit.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = if (habitWithStats.todayNote.isNotBlank()) "Edit reflection note" else "Add reflection note",
                                    tint = if (habitWithStats.todayNote.isNotBlank()) habitColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (habitWithStats.todayNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (onEditTodayNote != null) Modifier.clickable { onEditTodayNote() } else Modifier),
                            shape = RoundedCornerShape(10.dp),
                            color = habitColor.copy(alpha = 0.09f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, habitColor.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "📝 ${habitWithStats.todayNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isGlass) (if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { onToggleDone(isDone) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("habit_toggle_button_${habit.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = habitColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COMPLETE TODAY 🔥", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Footer row: reminder info + monthly count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Reminder time
                if (habit.reminderEnabled) {
                    val allTimes = remember(habit.reminderHour, habit.reminderMinute, habit.additionalReminderTimes) {
                        habit.getAllReminderTimes()
                    }
                    val timeLabel = remember(habit.safeReminderHour, habit.safeReminderMinute, is24Hour) {
                        if (is24Hour) {
                            String.format(Locale.getDefault(), "%02d:%02d", habit.safeReminderHour, habit.safeReminderMinute)
                        } else {
                            val amPm = if (habit.safeReminderHour >= 12) "PM" else "AM"
                            val hour12 = if (habit.safeReminderHour % 12 == 0) 12 else habit.safeReminderHour % 12
                            String.format(Locale.getDefault(), "%02d:%02d %s", hour12, habit.safeReminderMinute, amPm)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (isGlass) {
                                    if (isDark) Color(0xFF08182B).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.65f)
                                } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (isGlass) 1.dp else 0.dp,
                                color = if (isGlass) {
                                    if (isDark) Color(0xFF00E5FF).copy(alpha = 0.20f) else Color(0xFFBFDBFE).copy(alpha = 0.45f)
                                } else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Reminder",
                            tint = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (allTimes.size > 1) "$timeLabel (+${allTimes.size - 1})" else timeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val modeIcon = when (habit.ringMode) {
                            RingMode.SOUND_AND_VIBRATE -> Icons.AutoMirrored.Filled.VolumeUp
                            RingMode.VIBRATE_ONLY -> Icons.Default.Vibration
                            RingMode.SILENT -> Icons.AutoMirrored.Filled.VolumeOff
                        }
                        Icon(
                            imageVector = modeIcon,
                            contentDescription = null,
                            tint = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Completion count
                Text(
                    text = "${habitWithStats.totalCompletions} total completions",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
