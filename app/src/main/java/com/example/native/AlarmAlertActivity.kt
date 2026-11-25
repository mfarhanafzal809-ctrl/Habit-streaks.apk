package com.example.native

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import com.example.data.local.HabitDatabase
import com.example.data.repository.SettingsRepository
import com.example.ui.theme.HabitStreaksTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Full-screen native alarm clock alert activity shown over the lock screen
 * when an exact habit reminder alarm fires.
 */
class AlarmAlertActivity : ComponentActivity() {

    private var habitIdState by mutableLongStateOf(-1L)
    private var habitNameState by mutableStateOf("Habit Reminder")
    private var habitColorState by mutableStateOf("#6366F1")
    private var habitStreakState by mutableIntStateOf(0)
    private var isTestAlarmState by mutableStateOf(false)

    private val alarmStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AlarmRingingService.ACTION_ALARM_STATE_CHANGED) {
                val ringing = intent.getBooleanExtra(AlarmRingingService.EXTRA_RINGING, true)
                val targetHabitId = intent.getLongExtra(NotificationHelper.EXTRA_HABIT_ID, -1L)
                if (!ringing && (targetHabitId == -1L || targetHabitId == habitIdState)) {
                    finishSafely()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                window.isNavigationBarContrastEnforced = false
            } catch (_: Throwable) {}
        }
        configureLockScreenWindow()
        readIntentExtras(intent)

        val filter = IntentFilter(AlarmRingingService.ACTION_ALARM_STATE_CHANGED)
        ContextCompat.registerReceiver(
            this,
            alarmStoppedReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            val is24Hour by remember {
                SettingsRepository(HabitDatabase.getInstance(applicationContext).appSettingDao()).is24HourFormat
            }.collectAsState(initial = false)

            HabitStreaksTheme(darkTheme = true) {
                BackHandler {
                    dispatchAlarmAction(NotificationHelper.ACTION_DISMISS_ALARM)
                }

                AlarmAlertScreen(
                    habitName = habitNameState,
                    habitColorHex = habitColorState,
                    currentStreak = habitStreakState,
                    isTestAlarm = isTestAlarmState,
                    is24Hour = is24Hour,
                    onMarkDone = {
                        dispatchAlarmAction(NotificationHelper.ACTION_MARK_DONE)
                    },
                    onSnooze = { minutes ->
                        dispatchAlarmAction(NotificationHelper.ACTION_SNOOZE, snoozeMinutes = minutes)
                    },
                    onDismiss = {
                        dispatchAlarmAction(NotificationHelper.ACTION_DISMISS_ALARM)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntentExtras(intent)
    }

    private fun readIntentExtras(intent: Intent?) {
        habitIdState = intent?.getLongExtra(NotificationHelper.EXTRA_HABIT_ID, -1L) ?: -1L
        habitNameState = intent?.getStringExtra(NotificationHelper.EXTRA_HABIT_NAME) ?: "Habit Reminder"
        habitColorState = intent?.getStringExtra(NotificationHelper.EXTRA_HABIT_COLOR) ?: "#6366F1"
        habitStreakState = intent?.getIntExtra(NotificationHelper.EXTRA_HABIT_STREAK, 0) ?: 0
        isTestAlarmState = intent?.getBooleanExtra(NotificationHelper.EXTRA_IS_TEST_ALARM, false) ?: false
    }

    private fun configureLockScreenWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun dispatchAlarmAction(action: String, snoozeMinutes: Int = 10) {
        val actionIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = action
            setPackage(packageName)
            putExtra(NotificationHelper.EXTRA_HABIT_ID, habitIdState)
            putExtra(NotificationHelper.EXTRA_HABIT_NAME, habitNameState)
            putExtra(NotificationHelper.EXTRA_IS_TEST_ALARM, isTestAlarmState)
            putExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        sendBroadcast(actionIntent)
        AlarmRingingService.stop(applicationContext, habitIdState)
        finishSafely()
    }

    private fun finishSafely() {
        if (!isFinishing && !isDestroyed) {
            finish()
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(alarmStoppedReceiver)
        } catch (_: Throwable) {}
        super.onDestroy()
    }
}

@Composable
private fun AlarmAlertScreen(
    habitName: String,
    habitColorHex: String,
    currentStreak: Int,
    isTestAlarm: Boolean,
    is24Hour: Boolean,
    onMarkDone: () -> Unit,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = remember(habitColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(habitColorHex)) }
            .getOrDefault(Color(0xFF6366F1))
    }

    var now by remember { mutableStateOf(com.example.util.GreetingHelper.getNow().toLocalDateTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = com.example.util.GreetingHelper.getNow().toLocalDateTime()
            delay(1000L)
        }
    }

    val timeFormatted = remember(now, is24Hour) {
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        now.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }
    val dateFormatted = remember(now.toLocalDate()) {
        now.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    }

    val pulseTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val outerPulse by pulseTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_ring"
    )
    val innerPulse by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inner_ring"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF060B18),
                        accentColor.copy(alpha = 0.22f),
                        Color(0xFF080E1E)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top badge & Live Clock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = if (isTestAlarm) "TEST ALARM ACTIVE" else "HABIT ALARM",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 48.sp,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )

                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFFCBD5E1)
                )
            }

            // Center pulsing icon + Habit details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(164.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(152.dp)
                            .scale(outerPulse)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f))
                    )
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .scale(innerPulse)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.30f))
                    )
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Ringing habit alarm",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = habitName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (currentStreak > 0) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(
                        1.dp,
                        if (currentStreak > 0) Color(0xFFF59E0B).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentStreak > 0) {
                                "Protect your $currentStreak-day streak!"
                            } else {
                                "Time to build consistency!"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom Action Controls (>= 48dp touch targets)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onMarkDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("alarm_mark_done_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isTestAlarm) "Complete Test Alarm" else "Mark Habit Done",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                }

                // Quick Snooze choices (5m, 10m, 15m)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15).forEach { mins ->
                        OutlinedButton(
                            onClick = { onSnooze(mins) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("alarm_snooze_${mins}m_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${mins}m",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("alarm_dismiss_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF43F5E).copy(alpha = 0.14f),
                        contentColor = Color(0xFFFDA4AF)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dismiss Alarm",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
