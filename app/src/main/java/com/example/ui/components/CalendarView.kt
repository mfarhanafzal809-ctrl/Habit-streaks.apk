package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.HabitFrequency
import com.example.domain.StreakEngine
import com.example.ui.theme.PrimaryIndigo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView(
    selectedMonth: YearMonth,
    selectedDate: LocalDate,
    allHabits: List<Habit>,
    allCompletions: List<HabitCompletion>,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onToggleHabitOnDate: (Long, LocalDate, Boolean) -> Unit,
    onEditNote: ((Habit, LocalDate, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val today = com.example.util.GreetingHelper.getNow().toLocalDate()
    val currentMonth = YearMonth.from(today)
    val monthTitle = selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + selectedMonth.year
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val validHabitIds = remember(allHabits) { allHabits.map { it.id }.toSet() }
    val completionsByDate = remember(allCompletions, validHabitIds) {
        allCompletions
            .filter { it.habitId in validHabitIds }
            .groupBy { it.completionDate }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = com.example.ui.theme.glassmorphicCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = com.example.ui.theme.glassmorphicCardElevation(defaultElevation = 2.dp),
        border = com.example.ui.theme.glassmorphicCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Header with arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onMonthChange(selectedMonth.minusMonths(1)) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month"
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        onMonthChange(currentMonth)
                        onDateSelected(today)
                    }
                ) {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (selectedMonth != currentMonth) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Current Month",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onMonthChange(selectedMonth.plusMonths(1)) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Weekday labels
            val weekdays = listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekdays.forEach { dayOfWeek ->
                    Text(
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calculate calendar grid days
            val firstDayOfMonth = selectedMonth.atDay(1)
            val dayOfWeekValue = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            val paddingDays = dayOfWeekValue - 1
            val daysInMonth = selectedMonth.lengthOfMonth()

            val totalGridCells = paddingDays + daysInMonth
            val rows = (totalGridCells + 6) / 7

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - paddingDays + 1

                            if (dayNumber in 1..daysInMonth) {
                                val cellDate = selectedMonth.atDay(dayNumber)
                                val dateStr = StreakEngine.formatDate(cellDate)
                                val completedCount = completionsByDate[dateStr]?.size ?: 0
                                val isSelected = cellDate == selectedDate
                                val isToday = cellDate == today
                                val isFutureCell = cellDate.isAfter(today)
                                val calendarPrimary = MaterialTheme.colorScheme.primary

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .alpha(if (isFutureCell && !isSelected) 0.50f else 1f)
                                        .background(
                                            when {
                                                isSelected -> calendarPrimary
                                                completedCount > 0 -> calendarPrimary.copy(alpha = 0.18f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                            color = if (isToday && !isSelected) calendarPrimary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            onDateSelected(cellDate)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            ),
                                            color = when {
                                                isSelected -> Color.White
                                                isToday -> calendarPrimary
                                                completedCount > 0 -> calendarPrimary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        if (completedCount > 0 && !isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(calendarPrimary)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.size(38.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Date Summary Box
            val selectedDateStr = StreakEngine.formatDate(selectedDate)
            val isFutureDate = selectedDate.isAfter(today)
            val selectedDayRecords = completionsByDate[selectedDateStr].orEmpty()
            val selectedDayCompletionMap = remember(selectedDayRecords) {
                selectedDayRecords.associateBy { it.habitId }
            }
            val displayedHabits = remember(allHabits, selectedDayCompletionMap) {
                allHabits.filter { !it.isArchived || selectedDayCompletionMap.containsKey(it.id) }
            }
            val dueHabitsCount = remember(displayedHabits, selectedDate) {
                displayedHabits.count { !it.isArchived && StreakEngine.isDueOn(it.frequency, selectedDate) }
            }
            val completedOnDayCount = remember(displayedHabits, selectedDayCompletionMap) {
                displayedHabits.count { selectedDayCompletionMap.containsKey(it.id) }
            }
            val formattedSelectedDate = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                ", " + selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + selectedDate.dayOfMonth

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formattedSelectedDate,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when {
                                    isFutureDate -> "Future date • Check-ins unlock on this day"
                                    dueHabitsCount > 0 -> "$completedOnDayCount of $dueHabitsCount due habits completed"
                                    else -> "$completedOnDayCount completed"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isFutureDate) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = "Future date locked",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (displayedHabits.isEmpty()) {
                        Text(
                            text = "No active habits yet. Create a habit to start tracking!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        displayedHabits.forEach { habit ->
                            val completion = selectedDayCompletionMap[habit.id]
                            val isDoneOnDay = completion != null
                            val isDueOnDay = StreakEngine.isDueOn(habit.frequency, selectedDate)
                            val existingNote = completion?.note.orEmpty()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = habit.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (habit.isArchived) {
                                            Text(
                                                text = "Archived",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else if (habit.frequency != HabitFrequency.DAILY && !isDueOnDay) {
                                            Text(
                                                text = "Rest day (${habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() }})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isDoneOnDay && !isFutureDate && onEditNote != null) {
                                            IconButton(
                                                onClick = { onEditNote(habit, selectedDate, existingNote) },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.EditNote,
                                                    contentDescription = "Edit reflection note for ${habit.name}",
                                                    tint = if (existingNote.isNotBlank()) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .alpha(if (isFutureDate) 0.35f else 1f)
                                                .background(if (isDoneOnDay) PrimaryIndigo else MaterialTheme.colorScheme.surface)
                                                .border(
                                                    1.dp,
                                                    if (isDoneOnDay) PrimaryIndigo else MaterialTheme.colorScheme.outline,
                                                    CircleShape
                                                )
                                                .clickable(
                                                    enabled = !isFutureDate,
                                                    onClickLabel = if (isDoneOnDay) "Undo completion" else "Mark completed"
                                                ) {
                                                    if (!isDoneOnDay) {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                    }
                                                    onToggleHabitOnDate(habit.id, selectedDate, isDoneOnDay)
                                                }
                                                .semantics {
                                                    contentDescription = when {
                                                        isFutureDate -> "Future date locked for ${habit.name}"
                                                        isDoneOnDay -> "Completed ${habit.name}"
                                                        else -> "Not completed ${habit.name}"
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isDoneOnDay) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (existingNote.isNotBlank()) {
                                    Text(
                                        text = "📝 $existingNote",
                                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp, end = 40.dp)
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
