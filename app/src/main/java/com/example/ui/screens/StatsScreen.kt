package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.example.ui.theme.glassmorphicCard
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Share
import com.example.ui.theme.PrimaryIndigo
import com.example.data.model.HabitWithStats
import com.example.domain.StreakEngine
import com.example.ui.components.StreakMilestoneCard
import com.example.ui.theme.HabitAmber
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StatsScreen(
    habitsWithStats: List<HabitWithStats>,
    overallStats: com.example.data.model.OverallStats,
    allCompletions: List<com.example.data.model.HabitCompletion> = emptyList(),
    onMilestoneClick: ((com.example.data.model.StreakMilestone, String?, Int?) -> Unit)? = null,
    onShareReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    val totalCompletions = overallStats.totalCompletions
    val bestStreak = overallStats.bestStreakEver
    val currentBestStreak = overallStats.currentBestStreak
    val activeHabits = overallStats.activeHabits
    val habitFrequencies = remember(habitsWithStats) {
        habitsWithStats.associate { it.habit.id to it.habit.frequency }
    }
    val completionsByHabit = remember(allCompletions) { allCompletions.groupBy { it.habitId } }
    val milestones = remember(currentBestStreak, totalCompletions, completionsByHabit, habitFrequencies) {
        StreakEngine.getMilestones(currentBestStreak, totalCompletions, completionsByHabit, habitFrequencies)
    }
    val today = com.example.util.GreetingHelper.getNow().toLocalDate()
    val weeklyData = remember(allCompletions, today) {
        val completionsByDate = allCompletions.groupingBy { it.completionDate }.eachCount()
        (6 downTo 0).map { i ->
            val d = today.minusDays(i.toLong())
            val dateStr = StreakEngine.formatDate(d)
            val count = completionsByDate[dateStr] ?: 0
            Pair(d.dayOfWeek.name.take(3), count)
        }
    }
    val currentMonth = YearMonth.from(today)
    val todayDay = today.dayOfMonth

    // Calculate monthly rate respecting each active habit's due schedule (Daily / Weekdays / Weekends)
    val monthStart = currentMonth.atDay(1)
    val monthlyCompletionsSum = habitsWithStats.sumOf { it.monthlyCompletions }
    val expectedMonthCompletions = remember(habitsWithStats, monthStart, today) {
        if (habitsWithStats.isEmpty()) {
            (activeHabits * todayDay).coerceAtLeast(1)
        } else {
            habitsWithStats.sumOf {
                StreakEngine.countDueDaysInRange(it.habit.frequency, monthStart, today)
            }.coerceAtLeast(1)
        }
    }
    val monthlyRate = ((monthlyCompletionsSum.toFloat() / expectedMonthCompletions.toFloat()) * 100f).coerceIn(0f, 100f)

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gradientBrush = Brush.linearGradient(
        colors = listOf(primaryColor, secondaryColor)
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(start = 46.dp)) {
                Text(
                    text = "Insights & Streaks",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Track your long-term consistency, milestones, and personal records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary Hero Card with Gradient
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().background(gradientBrush)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monthly Consistency",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${monthlyRate.toInt()}%",
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Text(
                                text = "$monthlyCompletionsSum completed",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Premium Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(monthlyRate / 100f)
                                    .height(8.dp)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
        
        // 2x2 Grid for Core Metrics
        item {
            val isGlass = com.example.ui.theme.LocalGlassmorphismEnabled.current
            val isDark = com.example.ui.theme.LocalIsDarkTheme.current
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Total Completions
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
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
                        modifier = Modifier.padding(16.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(HabitAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HabitAmber, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$totalCompletions",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = if (isGlass && isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total Check-ins",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Best Streak
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
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
                        modifier = Modifier.padding(16.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryIndigo.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = if (isGlass && isDark) Color(0xFF00E5FF) else PrimaryIndigo, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$bestStreak d",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = if (isGlass && isDark) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Record Streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section Title: Weekly Insight
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Weekly Insight",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        item {
            com.example.ui.components.WeeklyInsightCard(
                allCompletions = allCompletions,
                today = today
            )
        }

        // Next Milestone Radar Card
        val nextMilestone = StreakEngine.getNextMilestone(bestStreak)
        if (nextMilestone != null) {
            item {
                val remaining = nextMilestone.days - bestStreak
                val radarPrimary = runCatching {
                    Color(android.graphics.Color.parseColor(nextMilestone.primaryColorHex))
                }.getOrDefault(HabitAmber)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (onMilestoneClick != null) Modifier.clickable {
                            onMilestoneClick.invoke(nextMilestone, habitsWithStats.maxByOrNull { it.currentStreak }?.habit?.name, bestStreak)
                        } else Modifier),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = radarPrimary.copy(alpha = 0.10f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, radarPrimary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = radarPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NEXT UNLOCK RADAR",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                    color = radarPrimary
                                )
                            }
                            Text(
                                text = "$remaining DAYS TO GO",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = radarPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${nextMilestone.title} (${nextMilestone.days} Days)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Maintain your daily streak to unlock the ${nextMilestone.badgeTier} badge and claim your trophy!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val progressRatio = (bestStreak.toFloat() / nextMilestone.days.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = radarPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // Section Title: Milestones
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = HabitAmber, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Trophies & Milestones",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        items(milestones, key = { it.days }) { milestone ->
            StreakMilestoneCard(
                milestone = milestone,
                progressStreak = bestStreak,
                onClick = {
                    onMilestoneClick?.invoke(
                        milestone,
                        habitsWithStats.maxByOrNull { it.longestStreak }?.habit?.name,
                        bestStreak
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val summary = habitsWithStats.joinToString("\n") { 
                        "• ${it.habit.name}: ${it.currentStreak} day streak"
                    }
                    val report = "📊 Habit Streaks Progress Report\n\n$summary\n\nTotal Completions: $totalCompletions\nOverall Record: $bestStreak days\n\nKeep going! 🚀"
                    onShareReport(report)
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Share Full Progress Report", fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

}

