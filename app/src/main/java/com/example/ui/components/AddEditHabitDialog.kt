package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Habit
import com.example.data.model.HabitFrequency
import com.example.data.model.RingMode
import com.example.native.AlarmScheduler
import com.example.native.AudioExtractionHelper
import com.example.native.SoundPlayer
import com.example.util.findActivity
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AddEditHabitDialog(
    initialHabit: Habit? = null,
    availableRingtones: List<SoundPlayer.MusicalSoundOption>,
    is24Hour: Boolean,
    isVibrationEnabled: Boolean = true,
    isAdFreeActive: Boolean = com.example.ads.AdManager.isAdFreeActive(),
    onPreviewSound: (String?, Boolean) -> Unit,
    onStopPreview: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        description: String,
        category: String,
        colorHex: String,
        iconName: String,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        ringMode: RingMode,
        ringtoneUri: String?,
        ringtoneName: String,
        vibrateEnabled: Boolean,
        frequency: HabitFrequency,
        additionalReminderTimes: String
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary

    var name by remember { mutableStateOf(initialHabit?.name ?: "") }
    var description by remember { mutableStateOf(initialHabit?.description ?: "") }
    var category by remember { mutableStateOf(initialHabit?.category ?: "Health") }
    var colorHex by remember { mutableStateOf(initialHabit?.colorHex ?: "#0284C7") }
    var iconName by remember { mutableStateOf(initialHabit?.iconName?.ifBlank { "Flame" } ?: "Flame") }
    var reminderEnabled by remember { mutableStateOf(initialHabit?.reminderEnabled ?: false) }

    // List of (Hour, Minute) reminder times
    val reminderTimes = remember {
        val list = mutableStateListOf<Pair<Int, Int>>()
        if (initialHabit != null) {
            val all = initialHabit.getAllReminderTimes()
            if (all.isNotEmpty()) {
                list.addAll(all)
            } else {
                list.add(Pair(initialHabit.reminderHour, initialHabit.reminderMinute))
            }
        } else {
            val now = java.time.LocalTime.now()
            list.add(Pair(now.hour, now.minute))
        }
        list
    }

    var ringMode by remember { mutableStateOf(initialHabit?.ringMode ?: RingMode.SOUND_AND_VIBRATE) }
    var ringtoneUri by remember { mutableStateOf(initialHabit?.ringtoneUri) }
    var ringtoneName by remember {
        mutableStateOf(
            initialHabit?.ringtoneName?.takeIf { it.isNotBlank() && it != "Default Bell" }
                ?: "Kyoto Temple Bronze Bell"
        )
    }
    var vibrateEnabled by remember { mutableStateOf(initialHabit?.vibrateEnabled ?: true) }
    var frequency by remember { mutableStateOf(initialHabit?.frequency ?: HabitFrequency.DAILY) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Permission readiness checks when enabling alarms
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var canScheduleExact by remember {
        mutableStateOf(AlarmScheduler(context).canScheduleExactAlarms())
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    // State for Animated Clock Picker Dialog
    var showClockPicker by remember { mutableStateOf(false) }
    var clockPickerEditIndex by remember { mutableIntStateOf(-1) } // -1 means adding new time

    // State for Musical Sound Picker Bottom Sheet
    var showSoundPickerSheet by remember { mutableStateOf(false) }
    var isExtractingAudio by remember { mutableStateOf(false) }
    var pendingMediaImportUri by remember { mutableStateOf<Uri?>(null) }
    var showMediaOfflineNotice by remember { mutableStateOf(false) }
    var mediaOfflineError by remember { mutableStateOf<String?>(null) }

    fun proceedWithMediaExtraction(uri: Uri) {
        val isVideo = AudioExtractionHelper.isVideoUri(context, uri)
        if (isVideo) {
            coroutineScope.launch {
                isExtractingAudio = true
                val result = AudioExtractionHelper.extractAudioFromVideo(context, uri)
                isExtractingAudio = false
                if (result.isSuccess) {
                    val (extractedUri, extractedName) = result.getOrThrow()
                    ringtoneUri = extractedUri.toString()
                    ringtoneName = extractedName
                    Toast.makeText(context, "Voice extracted and set as ringtone! 🎵", Toast.LENGTH_SHORT).show()
                    onPreviewSound(ringtoneUri, vibrateEnabled)
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Audio extraction failed"
                    Toast.makeText(context, "Extraction error: $err", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            coroutineScope.launch {
                isExtractingAudio = true
                val result = AudioExtractionHelper.saveAudioToInternalStorage(context, uri)
                isExtractingAudio = false
                if (result.isSuccess) {
                    val (savedUri, savedName) = result.getOrThrow()
                    ringtoneUri = savedUri.toString()
                    ringtoneName = savedName
                    Toast.makeText(context, "Audio set as ringtone: $savedName", Toast.LENGTH_SHORT).show()
                    onPreviewSound(ringtoneUri, vibrateEnabled)
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Audio import failed"
                    Toast.makeText(context, "Import error: $err", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // System file picker for audio/video (Open from Audio/Video)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val activity = context.findActivity() ?: com.example.ads.AdManager.currentActivity
            val effectiveAdFree = isAdFreeActive || com.example.ads.AdManager.isAdFreeActive()

            if (effectiveAdFree || com.example.ads.AdManager.isOfflineBypassed()) {
                proceedWithMediaExtraction(uri)
            } else if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                pendingMediaImportUri = uri
                mediaOfflineError = null
                showMediaOfflineNotice = true
            } else {
                com.example.ads.AdManager.requestInterstitialAndContinue(activity, isFocusedTask = false, force = true) {
                    proceedWithMediaExtraction(uri)
                }
            }
        }
    }

    val categories = listOf("Health", "Mindset", "Fitness", "Focus", "Routine", "Learning", "Social")
    val presetColors = listOf(
        "#0284C7", // Calming Ocean Azure
        "#10B981", // Encouraging Emerald
        "#F59E0B", // Dopamine Amber
        "#6366F1", // Electric Indigo
        "#8B5CF6", // Lavender Dream
        "#F43F5E", // Rose Coral
        "#0D9488", // Deep Teal
        "#EC4899"  // Joyful Pink
    )

    DisposableEffect(Unit) {
        onDispose {
            onStopPreview()
        }
    }

    // Centered Animated Clock Picker Dialog
    if (showClockPicker) {
        val (initH, initM) = if (clockPickerEditIndex in reminderTimes.indices) {
            reminderTimes[clockPickerEditIndex]
        } else {
            Pair(9, 0)
        }

        AnimatedClockPickerDialog(
            initialHour = initH,
            initialMinute = initM,
            is24Hour = is24Hour,
            onTimeConfirmed = { h, m ->
                val newTime = Pair(h, m)
                if (clockPickerEditIndex in reminderTimes.indices) {
                    reminderTimes[clockPickerEditIndex] = newTime
                } else if (!reminderTimes.contains(newTime)) {
                    reminderTimes.add(newTime)
                }
                showClockPicker = false
                clockPickerEditIndex = -1
            },
            onDismiss = {
                showClockPicker = false
                clockPickerEditIndex = -1
            }
        )
    }

    // Musical Sound Picker Bottom Sheet
    if (showSoundPickerSheet) {
        MusicalSoundPickerBottomSheet(
            currentUri = ringtoneUri,
            currentName = ringtoneName,
            isVibrationEnabled = isVibrationEnabled && vibrateEnabled,
            isAdFreeActive = isAdFreeActive,
            onSoundApplied = { uri, name ->
                ringtoneUri = uri
                ringtoneName = name
                showSoundPickerSheet = false
            },
            onRequestCustomSoundImport = {
                showSoundPickerSheet = false
                try {
                    mediaPickerLauncher.launch(arrayOf("audio/*", "video/*"))
                } catch (_: Exception) {
                    try {
                        mediaPickerLauncher.launch(arrayOf("*/*"))
                    } catch (_: Exception) {
                        Toast.makeText(context, "No compatible media picker found on this device", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = {
                showSoundPickerSheet = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (initialHabit == null) Icons.Default.AddCircle else Icons.Default.Edit,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialHabit == null) "Create New Habit" else "Edit Habit",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (initialHabit == null) {
                    Text(
                        text = "Quick-Start Templates",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.data.model.HabitTemplateLibrary.templates.forEach { template ->
                            val isApplied = name.equals(template.name, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isApplied) primaryColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isApplied) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        name = template.name
                                        description = template.description
                                        category = when (template.category) {
                                            "Mind" -> "Mindset"
                                            "Productivity" -> "Focus"
                                            else -> if (categories.contains(template.category)) template.category else "Routine"
                                        }
                                        colorHex = template.colorHex
                                        iconName = normalizeHabitIconName(template.iconName)
                                        frequency = template.frequency
                                        reminderTimes.clear()
                                        reminderTimes.add(Pair(template.recommendedHour, template.recommendedMinute))
                                        reminderEnabled = true
                                        errorMessage = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = template.emoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = template.name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isApplied) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isApplied) primaryColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Habit Name *") },
                    placeholder = { Text("e.g., Morning Meditation, Deep Work") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Why do you want to build this habit?") },
                    placeholder = { Text("e.g., Enhances cognitive clarity & inner calm") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Chips
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                                selectedLabelColor = primaryColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Frequency Selector (Daily / Weekdays / Weekends)
                Text(
                    text = "Schedule Cadence",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val freqOptions = listOf(
                        HabitFrequency.DAILY to "Daily",
                        HabitFrequency.WEEKDAYS to "Weekdays",
                        HabitFrequency.WEEKENDS to "Weekends"
                    )
                    freqOptions.forEach { (freq, label) ->
                        val selected = frequency == freq
                        FilterChip(
                            selected = selected,
                            onClick = { frequency = freq },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                                selectedLabelColor = primaryColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Habit Icon Picker
                Text(
                    text = "Habit Icon",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    habitIconOptions.forEach { opt ->
                        val isSelected = iconName.equals(opt.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) primaryColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor) else null,
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { iconName = opt.name }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = opt.label,
                                    tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Swatches
                Text(
                    text = "Theme Accent Color",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(CircleShape)
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected color",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Premium Reminder Settings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Exact Habit Alarm",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Clock-precision sound, vibration & lockscreen alert",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { enabled ->
                                    reminderEnabled = enabled
                                    canScheduleExact = AlarmScheduler(context).canScheduleExactAlarms()
                                    if (enabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                            )
                        }

                        if (reminderEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))

                            if (!hasNotificationPermission || !canScheduleExact) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Grant alarm permissions for on-the-second alerts",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                TextButton(
                                                    onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                                    modifier = Modifier.heightIn(min = 48.dp)
                                                ) {
                                                    Text("Allow Notifications", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                TextButton(
                                                    onClick = {
                                                        try {
                                                            context.startActivity(
                                                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                                    data = Uri.parse("package:${context.packageName}")
                                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                            )
                                                        } catch (_: Exception) {}
                                                    },
                                                    modifier = Modifier.heightIn(min = 48.dp)
                                                ) {
                                                    Text("Allow Exact Alarms", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Interactive Times Section (Supports multiple reminder times)
                            Text(
                                text = "Alarm Times (Tap to set clock):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                reminderTimes.forEachIndexed { index, (hour, min) ->
                                    val timeStr = if (is24Hour) {
                                        String.format(Locale.getDefault(), "%02d:%02d", hour, min)
                                    } else {
                                        val amPm = if (hour >= 12) "PM" else "AM"
                                        val hour12 = if (hour % 12 == 0) 12 else hour % 12
                                        String.format(Locale.getDefault(), "%02d:%02d %s", hour12, min, amPm)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = primaryColor.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 48.dp)
                                                    .clickable {
                                                        clockPickerEditIndex = index
                                                        showClockPicker = true
                                                    }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccessTime,
                                                    contentDescription = null,
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (index == 0) "Primary: $timeStr" else "Time #${index + 1}: $timeStr",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = primaryColor
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TextButton(
                                                    onClick = {
                                                        clockPickerEditIndex = index
                                                        showClockPicker = true
                                                    },
                                                    modifier = Modifier.heightIn(min = 48.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }

                                                if (index > 0) {
                                                    IconButton(
                                                        onClick = { reminderTimes.removeAt(index) },
                                                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Delete Time",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        clockPickerEditIndex = -1
                                        showClockPicker = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+ Add Another Reminder Time",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Alert Mode Selector
                            Text(
                                text = "Alert Mode",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val modes = listOf(
                                    RingMode.SOUND_AND_VIBRATE to "Sound + Vibe",
                                    RingMode.VIBRATE_ONLY to "Vibrate",
                                    RingMode.SILENT to "Silent"
                                )
                                modes.forEach { (mode, label) ->
                                    val isSelected = ringMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                ringMode = mode
                                                if (mode == RingMode.VIBRATE_ONLY) vibrateEnabled = true
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                ),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            if (ringMode != RingMode.SILENT) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Vibrate on Alarm",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Switch(
                                        checked = vibrateEnabled,
                                        onCheckedChange = { vibrateEnabled = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                                    )
                                }
                            }

                            if (ringMode == RingMode.SOUND_AND_VIBRATE) {
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Alarm Melody",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable { showSoundPickerSheet = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = ringtoneName,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "Tap to choose musical sound or import audio",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Choose alarm melody",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter a habit name"
                        return@Button
                    }
                    onStopPreview()

                    val sortedUnique = reminderTimes
                        .distinct()
                        .sortedBy { it.first * 60 + it.second }
                    val primaryTime = sortedUnique.firstOrNull() ?: Pair(9, 0)
                    val additionalTimesStr = sortedUnique.drop(1).joinToString(",") {
                        String.format(Locale.getDefault(), "%02d:%02d", it.first, it.second)
                    }

                    onSave(
                        name,
                        description,
                        category,
                        colorHex,
                        iconName,
                        reminderEnabled,
                        primaryTime.first,
                        primaryTime.second,
                        ringMode,
                        ringtoneUri,
                        ringtoneName,
                        vibrateEnabled,
                        frequency,
                        additionalTimesStr
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("save_habit_button")
            ) {
                Text(if (initialHabit == null) "Create Habit" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onStopPreview()
                    onDismiss()
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )

    if (showMediaOfflineNotice && pendingMediaImportUri != null) {
        OfflineAdNoticeDialog(
            errorMessage = mediaOfflineError,
            onNoInternet = {
                val targetUri = pendingMediaImportUri
                showMediaOfflineNotice = false
                mediaOfflineError = null
                pendingMediaImportUri = null
                com.example.ads.AdManager.setOfflineBypass(true)
                if (targetUri != null) {
                    proceedWithMediaExtraction(targetUri)
                }
            },
            onHaveInternet = {
                if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                    mediaOfflineError = "Please turn on Wi-Fi or mobile data, then try again."
                } else {
                    val targetUri = pendingMediaImportUri
                    showMediaOfflineNotice = false
                    mediaOfflineError = null
                    pendingMediaImportUri = null
                    com.example.ads.AdManager.setOfflineBypass(false)
                    if (targetUri != null) {
                        val activity = context.findActivity() ?: com.example.ads.AdManager.currentActivity
                        com.example.ads.AdManager.requestInterstitialAndContinue(activity, isFocusedTask = false, force = true) {
                            proceedWithMediaExtraction(targetUri)
                        }
                    }
                }
            },
            onDismiss = {
                showMediaOfflineNotice = false
                mediaOfflineError = null
                pendingMediaImportUri = null
            }
        )
    }

    if (isExtractingAudio) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { /* Modal during extraction */ },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = primaryColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Processing Media...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Extracting audio track off the main thread",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
