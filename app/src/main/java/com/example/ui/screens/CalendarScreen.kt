package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.ui.components.CalendarView
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(start = 46.dp)) {
                Text(
                    text = "Habit Calendar",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Inspect and complete your habits across past and current dates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            CalendarView(
                selectedMonth = selectedMonth,
                selectedDate = selectedDate,
                allHabits = allHabits,
                allCompletions = allCompletions,
                onMonthChange = onMonthChange,
                onDateSelected = onDateSelected,
                onToggleHabitOnDate = onToggleHabitOnDate,
                onEditNote = onEditNote
            )
        }
    }
}
