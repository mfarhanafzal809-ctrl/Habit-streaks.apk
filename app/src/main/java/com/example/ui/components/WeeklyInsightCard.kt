package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.glassmorphicCard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitCompletion
import com.example.domain.StreakEngine
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyInsightCard(
    allCompletions: List<HabitCompletion>,
    today: LocalDate = com.example.util.GreetingHelper.getNow().toLocalDate(),
    modifier: Modifier = Modifier
) {
    val last7Days = remember(today) {
        (6 downTo 0).map { today.minusDays(it.toLong()) }
    }

    val completionsByDate = remember(allCompletions) {
        allCompletions.groupBy { it.completionDate }
    }

    val weeklyData = remember(last7Days, completionsByDate) {
        last7Days.map { date ->
            val dateStr = StreakEngine.formatDate(date)
            val count = completionsByDate[dateStr]?.size ?: 0
            date to count
        }
    }

    val weeklyTotal = remember(weeklyData) { weeklyData.sumOf { it.second } }
    val weeklyAvg = remember(weeklyTotal) { String.format(Locale.getDefault(), "%.1f", weeklyTotal / 7f) }

    val entries = remember(weeklyData) {
        weeklyData.mapIndexed { index, data ->
            FloatEntry(x = index.toFloat(), y = data.second.toFloat())
        }
    }

    val chartEntryModel = remember(entries) { entryModelOf(entries) }
    val isGlass = com.example.ui.theme.LocalGlassmorphismEnabled.current
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isGlass) {
                    Modifier.glassmorphicCard(shape = RoundedCornerShape(20.dp), borderWidth = 1.6.dp)
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = com.example.ui.theme.glassmorphicCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = com.example.ui.theme.glassmorphicCardElevation(defaultElevation = 2.dp),
        border = if (isGlass) null else com.example.ui.theme.glassmorphicCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weekly Insight",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isGlass && isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Last 7 days completion trend",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "$weeklyTotal total",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "$weeklyAvg/day",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Chart(
                chart = columnChart(),
                model = chartEntryModel,
                startAxis = rememberStartAxis(
                    valueFormatter = { value, _ -> value.toInt().toString() }
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { value, _ ->
                        val index = value.toInt()
                        if (index in weeklyData.indices) {
                            weeklyData[index].first.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        } else ""
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            if (weeklyTotal == 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Complete your first habit today to light up your 7-day momentum chart.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
