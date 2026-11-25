package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.data.local.HabitDatabase
import com.example.data.model.Habit
import com.example.data.repository.HabitRepository
import com.example.domain.StreakEngine
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.SizeMode
import androidx.glance.LocalSize

class HabitWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = HabitDatabase.getInstance(context)
        val repository = HabitRepository(db.habitDao(), db.habitCompletionDao())
        val settingsRepo = SettingsRepository(db.appSettingDao())

        val isWidgetEnabled = settingsRepo.isWidgetEnabled.first()
        val isUrgentHighlightEnabled = settingsRepo.isWidgetUrgentHighlightEnabled.first()
        val isShowCompleted = settingsRepo.isWidgetShowCompleted.first()
        val isQuickControls = settingsRepo.isWidgetQuickControlsEnabled.first()
        val bgOpacity = settingsRepo.widgetBgOpacity.first()
        val isGlassmorphism = settingsRepo.isGlassmorphismEnabled.first()
        val is24Hour = settingsRepo.is24HourFormat.first()

        if (!isWidgetEnabled) {
            provideContent {
                GlanceTheme {
                    WidgetDisabledContent()
                }
            }
            return
        }
        
        val validActiveHabits = repository.allHabits.first().filter { !it.isArchived }
        val activeHabitIds = validActiveHabits.map { it.id }.toSet()
        val allCompletions = repository.allCompletions.first()
        val todayStr = StreakEngine.getTodayString()
        
        val now = LocalDateTime.now()
        val todayActiveCompletions = allCompletions.filter { it.completionDate == todayStr && it.habitId in activeHabitIds }

        val habitsWithStats = validActiveHabits.map { habit ->
            val isDone = todayActiveCompletions.any { it.habitId == habit.id }
            val allTimes = habit.getAllReminderTimes()

            // Calculate next reminder time across all times and habit frequency
            val reminderTime: LocalDateTime? = if (habit.reminderEnabled && allTimes.isNotEmpty()) {
                var earliestUpcoming: LocalDateTime? = null
                val isDueToday = StreakEngine.isDueOn(habit.frequency, now.toLocalDate())

                // 1. Check upcoming times today if today is a due day
                if (isDueToday) {
                    for ((h, m) in allTimes) {
                        try {
                            val candidateTime = LocalDateTime.of(
                                now.toLocalDate(),
                                LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59))
                            )
                            if (candidateTime.isAfter(now)) {
                                if (earliestUpcoming == null || candidateTime.isBefore(earliestUpcoming)) {
                                    earliestUpcoming = candidateTime
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // 2. If no upcoming times today, find earliest time on next valid frequency day
                if (earliestUpcoming == null) {
                    var nextValidDate = now.toLocalDate().plusDays(1)
                    while (!StreakEngine.isDueOn(habit.frequency, nextValidDate)) {
                        nextValidDate = nextValidDate.plusDays(1)
                    }
                    for ((h, m) in allTimes) {
                        try {
                            val candidateTime = LocalDateTime.of(
                                nextValidDate,
                                LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59))
                            )
                            if (earliestUpcoming == null || candidateTime.isBefore(earliestUpcoming)) {
                                earliestUpcoming = candidateTime
                            }
                        } catch (_: Exception) {}
                    }
                }

                earliestUpcoming
            } else null

            val isNear = if (isUrgentHighlightEnabled && !isDone) {
                reminderTime?.let {
                    val diff = Duration.between(now, it).toMinutes()
                    diff in 0..15 && !it.isBefore(now)
                } ?: false
            } else false

            val reminderSummary: String? = reminderTime?.let { rt ->
                val timeStr = com.example.util.TimeFormatUtils.formatTime(rt.hour, rt.minute, is24Hour)
                val extraCount = allTimes.size - 1
                val extraSuffix = if (extraCount > 0) " (+$extraCount)" else ""
                val prefix = when {
                    rt.toLocalDate() == now.toLocalDate() -> if (isNear) "⏰ 15m ALERT: " else "Today "
                    rt.toLocalDate() == now.toLocalDate().plusDays(1) -> "Tomorrow "
                    else -> rt.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() } + " "
                }
                "$prefix$timeStr$extraSuffix"
            }

            HabitWidgetData(habit, isDone, reminderTime, isNear, reminderSummary)
        }.filter { if (!isShowCompleted) !it.isDone else true }
        .sortedWith(compareBy<HabitWidgetData> { it.isDone }
            .thenBy { it.isNear.not() }
            .thenBy { it.nextReminder ?: LocalDateTime.MAX }
        )

        val todayLocalDate = now.toLocalDate()
        val habitsDueToday = validActiveHabits.filter { StreakEngine.isDueOn(it.frequency, todayLocalDate) }
        val totalDueToday = habitsDueToday.size
        val completedDueToday = habitsDueToday.count { habit -> todayActiveCompletions.any { it.habitId == habit.id } }
        val progress = if (totalDueToday > 0) (completedDueToday.toFloat() / totalDueToday).coerceIn(0f, 1f) else if (validActiveHabits.isEmpty()) 0f else 1f
        val adFreeUntil = settingsRepo.adFreeUntilTimestamp.first()
        val isAdFree = adFreeUntil > System.currentTimeMillis()

        provideContent {
            GlanceTheme {
                WidgetContent(
                    habits = habitsWithStats,
                    totalActiveHabitsCount = validActiveHabits.size,
                    dailyProgress = progress,
                    isQuickControlsEnabled = isQuickControls,
                    isAdFree = isAdFree,
                    bgOpacity = bgOpacity,
                    isGlassmorphism = isGlassmorphism
                )
            }
        }
    }

    data class HabitWidgetData(
        val habit: Habit,
        val isDone: Boolean,
        val nextReminder: LocalDateTime?,
        val isNear: Boolean,
        val reminderSummary: String? = null
    )

    @Composable
    private fun WidgetDisabledContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Habit Streaks Widget",
                style = TextStyle(
                    color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.size(8.dp))
            Text(
                text = "Widget is turned OFF in Settings.\nOpen app to turn on.",
                style = TextStyle(
                    color = androidx.glance.color.ColorProvider(day = Color(0xFF94A3B8), night = Color(0xFF94A3B8)),
                    fontSize = 12.sp,
                    textAlign = androidx.glance.text.TextAlign.Center
                )
            )
        }
    }

    @Composable
    private fun WidgetContent(
        habits: List<HabitWidgetData>,
        totalActiveHabitsCount: Int,
        dailyProgress: Float,
        isQuickControlsEnabled: Boolean,
        isAdFree: Boolean,
        bgOpacity: Float,
        isGlassmorphism: Boolean
    ) {
        val size = LocalSize.current
        val baseColor = Color(0xFF0F172A)
        val bgColor = baseColor.copy(alpha = bgOpacity.coerceIn(0.1f, 1.0f))
        val hasData = habits.isNotEmpty()
        val isCompactHeight = size.height < 110.dp

        if (!hasData) {
            // Automatically slightly smaller in height when there is no data to show
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .cornerRadius(16.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Habit Streaks 🔥",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (totalActiveHabitsCount == 0) "No active habits yet" else "All caught up for today!",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = Color(0xFF94A3B8), night = Color(0xFF94A3B8)),
                                fontSize = 11.sp
                            )
                        )
                    }

                    Text(
                        text = "${(dailyProgress * 100).toInt()}%",
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(day = Color(0xFF38BDF8), night = Color(0xFF38BDF8)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                androidx.glance.appwidget.LinearProgressIndicator(
                    progress = dailyProgress,
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = androidx.glance.color.ColorProvider(day = Color(0xFF38BDF8), night = Color(0xFF38BDF8)),
                    backgroundColor = androidx.glance.color.ColorProvider(day = Color(0xFF334155), night = Color(0xFF334155))
                )

                // Compact status/celebration bar with shortcut to open app
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(Color(0xFF1E293B)))
                        .cornerRadius(10.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .clickable(androidx.glance.action.actionStartActivity<com.example.MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (totalActiveHabitsCount == 0) "✨ Tap to create your first habit" else "🎉 All habits completed! Tap to open app",
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = "↗",
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(day = Color(0xFF38BDF8), night = Color(0xFF38BDF8)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        } else if (isCompactHeight) {
            // Compact single-row layout when resized small on home screen
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgColor)
                    .cornerRadius(16.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Habit Streaks 🔥",
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = "${habits.count { it.isDone }}/${habits.size}",
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(day = Color(0xFF38BDF8), night = Color(0xFF38BDF8)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                HabitWidgetRow(habits.first(), isQuickControlsEnabled)
            }
        } else {
            // Full expanded layout when multiple habits are present
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgColor)
                    .cornerRadius(16.dp)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Habit Streaks 🔥",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Completed: ${habits.count { it.isDone }}/${habits.size}",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(day = Color(0xFF94A3B8), night = Color(0xFF94A3B8)),
                                fontSize = 11.sp
                            )
                        )
                    }

                    Text(
                        text = "${(dailyProgress * 100).toInt()}%",
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(day = Color(0xFF38BDF8), night = Color(0xFF38BDF8)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                androidx.glance.appwidget.LinearProgressIndicator(
                    progress = dailyProgress,
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = androidx.glance.color.ColorProvider(day = Color(0xFF38BDF8), night = Color(0xFF38BDF8)),
                    backgroundColor = androidx.glance.color.ColorProvider(day = Color(0xFF334155), night = Color(0xFF334155))
                )

                LazyColumn(modifier = GlanceModifier.defaultWeight()) {
                    items(habits) { data ->
                        HabitWidgetRow(data, isQuickControlsEnabled)
                    }
                }

                if (!habits.any { it.isNear } && size.height >= 170.dp) {
                    Spacer(modifier = GlanceModifier.size(4.dp))
                    WidgetMotivationCard()
                }
            }
        }
    }

    @Composable
    private fun WidgetMotivationCard() {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color(0xFF1E293B)))
                .cornerRadius(12.dp)
                .padding(8.dp)
                .clickable(androidx.glance.action.actionStartActivity<com.example.MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .background(androidx.glance.ImageProvider(com.example.R.drawable.bg_widget_ad_badge))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PRO",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.Black, night = Color.Black), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Keep your momentum going • Tap to open app",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Build consistent habits daily with smart tracking",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF38BDF8), night = Color(0xFF38BDF8)), fontSize = 8.sp)
                )
            }
            Text(
                text = "↗",
                style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF38BDF8), night = Color(0xFF38BDF8)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }
    }

    @Composable
    private fun HabitWidgetRow(data: HabitWidgetData, isQuickControlsEnabled: Boolean) {
        val habit = data.habit
        val isDone = data.isDone
        val isNear = data.isNear
        
        val colorStr = habit.colorHex.removePrefix("#")
        val habitColor = colorStr.toLongOrNull(16)?.let { if (colorStr.length == 6) it or 0xFF000000 else it }?.toInt() ?: 0xFF4F46E5.toInt()

        val actionParamKey = ActionParameters.Key<Long>("habitId")
        val isDoneParamKey = ActionParameters.Key<Boolean>("isDone")
        val toggleKey = ActionParameters.Key<String>("toggleType")

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .background(if (isNear) Color(0xFF78350F) else Color(0xFF1E293B))
                .cornerRadius(10.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Checkbox
            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .background(if (isDone) Color(habitColor) else Color(0xFF334155))
                    .cornerRadius(8.dp)
                    .clickable(
                        onClick = actionRunCallback<ToggleHabitAction>(
                            actionParametersOf(actionParamKey to habit.id, isDoneParamKey to isDone)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
               if (isDone) {
                   Text("✓", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 18.sp, fontWeight = FontWeight.Bold))
               }
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            // Info Column
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = habit.name,
                    style = TextStyle(
                        color = androidx.glance.color.ColorProvider(day = if (isDone) Color.Gray else Color.White, night = if (isDone) Color.Gray else Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isDone) androidx.glance.text.TextDecoration.LineThrough else null
                    ),
                    maxLines = 1
                )
                if (habit.reminderEnabled && data.reminderSummary != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isNear) {
                            Text(
                                text = "✨",
                                style = TextStyle(fontSize = 12.sp)
                            )
                            Spacer(modifier = GlanceModifier.width(2.dp))
                        }
                        Text(
                            text = data.reminderSummary,
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(
                                    day = if (isNear) Color(0xFFFDE047) else Color.LightGray, 
                                    night = if (isNear) Color(0xFFFDE047) else Color.LightGray
                                ),
                                fontSize = 11.sp,
                                fontWeight = if (isNear) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            // Quick Controls
            if (isQuickControlsEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reminder Toggle
                    WidgetControlButton(
                        icon = if (habit.reminderEnabled) "🔔" else "🔕",
                        isActive = habit.reminderEnabled,
                        onClick = actionRunCallback<HabitControlAction>(
                            actionParametersOf(actionParamKey to habit.id, toggleKey to "reminder")
                        )
                    )
                    
                    // Sound Toggle
                    WidgetControlButton(
                        icon = if (habit.ringMode == com.example.data.model.RingMode.SOUND_AND_VIBRATE) "🔊" else "🔇",
                        isActive = habit.ringMode == com.example.data.model.RingMode.SOUND_AND_VIBRATE,
                        onClick = actionRunCallback<HabitControlAction>(
                            actionParametersOf(actionParamKey to habit.id, toggleKey to "sound")
                        )
                    )
                    
                    // Vibration Toggle
                    WidgetControlButton(
                        icon = if (habit.vibrateEnabled) "📳" else "📵",
                        isActive = habit.vibrateEnabled,
                        onClick = actionRunCallback<HabitControlAction>(
                            actionParametersOf(actionParamKey to habit.id, toggleKey to "vibrate")
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun WidgetControlButton(
        icon: String,
        isActive: Boolean,
        onClick: androidx.glance.action.Action
    ) {
        Box(
            modifier = GlanceModifier
                .size(30.dp)
                .padding(2.dp)
                .background(if (isActive) Color(0xFF334155) else Color.Transparent)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 14.sp)
            )
        }
    }
}
