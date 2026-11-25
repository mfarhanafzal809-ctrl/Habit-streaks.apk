package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ClockSelectionMode {
    HOUR,
    MINUTE
}

@Composable
fun AnimatedClockPickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onTimeConfirmed: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val safeInitialHour = initialHour.coerceIn(0, 23)

    // Initialize based on format
    var isPm by remember { mutableStateOf(if (is24Hour) false else safeInitialHour >= 12) }
    var selectedHour by remember {
        mutableIntStateOf(
            if (is24Hour) {
                safeInitialHour
            } else {
                when {
                    safeInitialHour == 0 -> 12
                    safeInitialHour > 12 -> safeInitialHour - 12
                    else -> safeInitialHour
                }
            }
        )
    }
    var selectedMinute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    var selectionMode by remember { mutableStateOf(ClockSelectionMode.HOUR) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                border = com.example.ui.theme.glassmorphicCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Set Alarm Time",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Big Animated Digital Display Box
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Hour Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selectionMode == ClockSelectionMode.HOUR) primaryColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (selectionMode == ClockSelectionMode.HOUR) androidx.compose.foundation.BorderStroke(2.dp, primaryColor) else null,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable {
                                    selectionMode = ClockSelectionMode.HOUR
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                }
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", selectedHour),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 44.sp
                                ),
                                color = if (selectionMode == ClockSelectionMode.HOUR) primaryColor else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        Text(
                            text = ":",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 40.sp
                            ),
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Minute Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selectionMode == ClockSelectionMode.MINUTE) primaryColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (selectionMode == ClockSelectionMode.MINUTE) androidx.compose.foundation.BorderStroke(2.dp, primaryColor) else null,
                            modifier = Modifier.clickable {
                                selectionMode = ClockSelectionMode.MINUTE
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", selectedMinute),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 44.sp
                                ),
                                color = if (selectionMode == ClockSelectionMode.MINUTE) primaryColor else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // AM / PM Toggle Stack - Only show in 12h mode
                        if (!is24Hour) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (!isPm) primaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .clickable {
                                            isPm = false
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "AM",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                            color = if (!isPm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isPm) primaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .clickable {
                                            isPm = true
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "PM",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                            color = if (isPm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Clock Face Dial with Pointer (supports 12h single ring + 24h dual ring 00..23)
                    val outerRingDp = 92f
                    val innerRingDp = 60f
                    val isInnerHourSelected = is24Hour && selectionMode == ClockSelectionMode.HOUR && (selectedHour == 0 || selectedHour > 12)
                    val animatedRadiusFraction by animateFloatAsState(
                        targetValue = if (isInnerHourSelected) (innerRingDp / outerRingDp) else 1f,
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                        label = "dial_hand_radius"
                    )

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val angleDeg = remember(selectionMode, selectedHour, selectedMinute) {
                            if (selectionMode == ClockSelectionMode.HOUR) {
                                (selectedHour % 12) * 30f
                            } else {
                                selectedMinute * 6f
                            }
                        }

                        val animatedAngle by animateFloatAsState(
                            targetValue = angleDeg,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                            label = "dial_hand"
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(selectionMode, is24Hour) {
                                    detectTapGestures { offset ->
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val dx = offset.x - center.x
                                        val dy = offset.y - center.y
                                        val distFromCenter = hypot(dx, dy)
                                        var angleRad = atan2(dy.toDouble(), dx.toDouble()) + (PI / 2.0)
                                        if (angleRad < 0) angleRad += 2.0 * PI
                                        val fraction = (angleRad / (2.0 * PI)).toFloat()

                                        if (selectionMode == ClockSelectionMode.HOUR) {
                                            val step = (fraction * 12).roundToInt() % 12
                                            val innerThresholdPx = ((outerRingDp + innerRingDp) / 2f).dp.toPx()
                                            selectedHour = if (is24Hour && distFromCenter < innerThresholdPx) {
                                                if (step == 0) 0 else step + 12
                                            } else {
                                                if (step == 0) 12 else step
                                            }
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            selectionMode = ClockSelectionMode.MINUTE
                                        } else {
                                            var min = (fraction * 60).roundToInt()
                                            if (min >= 60) min = 0
                                            selectedMinute = min.coerceIn(0, 59)
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                        ) {
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val outerRadiusPx = outerRingDp.dp.toPx()
                            val activeRadius = outerRadiusPx * animatedRadiusFraction

                            // Center dot
                            drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(centerX, centerY))

                            // Pointer line
                            val rad = (animatedAngle - 90f) * (PI / 180f)
                            val handEnd = Offset(
                                x = (centerX + activeRadius * cos(rad)).toFloat(),
                                y = (centerY + activeRadius * sin(rad)).toFloat()
                            )

                            drawLine(
                                color = primaryColor,
                                start = Offset(centerX, centerY),
                                end = handEnd,
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Pointer target circle
                            drawCircle(
                                color = primaryColor,
                                radius = if (isInnerHourSelected) 14.dp.toPx() else 16.dp.toPx(),
                                center = handEnd
                            )
                        }

                        // Number markers
                        if (selectionMode == ClockSelectionMode.HOUR) {
                            // Outer ring: 1..12
                            for (i in 1..12) {
                                val rad = (i * 30.0 - 90.0) * (PI / 180.0)
                                val xOffset = (outerRingDp * cos(rad)).dp
                                val yOffset = (outerRingDp * sin(rad)).dp

                                Box(
                                    modifier = Modifier
                                        .offset(x = xOffset, y = yOffset)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            selectedHour = i
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            selectionMode = ClockSelectionMode.MINUTE
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$i",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selectedHour == i) FontWeight.Black else FontWeight.Medium
                                        ),
                                        color = if (selectedHour == i) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Inner ring in 24h mode: 00, 13..23
                            if (is24Hour) {
                                for (step in 0..11) {
                                    val hour24 = if (step == 0) 0 else step + 12
                                    val rad = (step * 30.0 - 90.0) * (PI / 180.0)
                                    val xOffset = (innerRingDp * cos(rad)).dp
                                    val yOffset = (innerRingDp * sin(rad)).dp

                                    Box(
                                        modifier = Modifier
                                            .offset(x = xOffset, y = yOffset)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                selectedHour = hour24
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                selectionMode = ClockSelectionMode.MINUTE
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = String.format(Locale.getDefault(), "%02d", hour24),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = if (selectedHour == hour24) FontWeight.Black else FontWeight.Medium
                                            ),
                                            color = if (selectedHour == hour24) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            // Minutes dial markers (every 5 mins: 00, 05, 10 ... 55)
                            for (m in 0..11) {
                                val minVal = m * 5
                                val rad = (m * 30.0 - 90.0) * (PI / 180.0)
                                val xOffset = (outerRingDp * cos(rad)).dp
                                val yOffset = (outerRingDp * sin(rad)).dp

                                Box(
                                    modifier = Modifier
                                        .offset(x = xOffset, y = yOffset)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            selectedMinute = minVal
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d", minVal),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selectedMinute == minVal) FontWeight.Black else FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = if (selectedMinute == minVal) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Precision Minute Controls: -5m, -1m, +1m, +5m
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedMinute = (selectedMinute - 5 + 60) % 60
                                selectionMode = ClockSelectionMode.MINUTE
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text("-5m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedMinute = (selectedMinute - 1 + 60) % 60
                                selectionMode = ClockSelectionMode.MINUTE
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text("-1m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedMinute = (selectedMinute + 1) % 60
                                selectionMode = ClockSelectionMode.MINUTE
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text("+1m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedMinute = (selectedMinute + 5) % 60
                                selectionMode = ClockSelectionMode.MINUTE
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text("+5m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Final Confirmation Action Button
                    Button(
                        onClick = {
                            val finalHour = if (is24Hour) {
                                selectedHour.coerceIn(0, 23)
                            } else {
                                when {
                                    !isPm && selectedHour == 12 -> 0
                                    isPm && selectedHour < 12 -> selectedHour + 12
                                    else -> selectedHour
                                }
                            }
                            onTimeConfirmed(finalHour, selectedMinute)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        val formattedDisplay = if (is24Hour) {
                            String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                selectedHour,
                                selectedMinute
                            )
                        } else {
                            String.format(
                                Locale.getDefault(),
                                "%02d:%02d %s",
                                selectedHour,
                                selectedMinute,
                                if (isPm) "PM" else "AM"
                            )
                        }
                        Text("Confirm Time ($formattedDisplay)", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
