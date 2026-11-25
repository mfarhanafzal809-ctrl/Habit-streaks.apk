package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.native.SoundPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicalSoundPickerBottomSheet(
    currentUri: String?,
    currentName: String,
    isVibrationEnabled: Boolean = true,
    isAdFreeActive: Boolean = false,
    onSoundApplied: (uri: String, name: String) -> Unit,
    onRequestCustomSoundImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val soundPlayer = remember { SoundPlayer(context.applicationContext) }
    val sounds = remember { soundPlayer.getAvailableMusicalSounds() }

    var selectedSound by remember {
        mutableStateOf(
            sounds.find { it.uriString == currentUri } ?: sounds.firstOrNull() ?: SoundPlayer.MusicalSoundOption(
                id = "default",
                name = currentName,
                category = "General",
                description = "Standard Alert",
                uriString = currentUri ?: ""
            )
        )
    }

    var currentlyPlayingUri by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.release()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = {
            soundPlayer.release()
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Musical Alarm Sounds",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Stress-reducing melodies & positive habit anchors",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        soundPlayer.release()
                        onDismiss()
                    },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Advance Feature: Import Custom Audio / Video Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        soundPlayer.stopPreview()
                        onRequestCustomSoundImport()
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.10f)),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Import MP3 / Video",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                PremiumFeatureBadge(
                                    requiredAdsLabel = "1 Ad",
                                    isAdFreeActive = isAdFreeActive,
                                    compact = true
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Select audio (MP3, WAV) or extract audio from video",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "Import media",
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sound List Grouped by Category
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val grouped = sounds.groupBy { it.category }
                grouped.forEach { (category, soundList) ->
                    item {
                        Text(
                            text = category.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = primaryColor,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }

                    items(soundList, key = { it.id }) { sound ->
                        val isSelected = selectedSound.id == sound.id
                        val isPlaying = currentlyPlayingUri == sound.uriString

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSound = sound
                                    if (isPlaying) {
                                        soundPlayer.stopPreview()
                                        currentlyPlayingUri = null
                                    } else {
                                        val targetUri = sound.uriString
                                        currentlyPlayingUri = targetUri
                                        soundPlayer.playPreview(targetUri, vibrate = isVibrationEnabled) {
                                            if (currentlyPlayingUri == targetUri) {
                                                currentlyPlayingUri = null
                                            }
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Play/Pause Wave Button
                                    IconButton(
                                        onClick = {
                                            selectedSound = sound
                                            if (isPlaying) {
                                                soundPlayer.stopPreview()
                                                currentlyPlayingUri = null
                                            } else {
                                                val targetUri = sound.uriString
                                                currentlyPlayingUri = targetUri
                                                soundPlayer.playPreview(targetUri, vibrate = isVibrationEnabled) {
                                                    if (currentlyPlayingUri == targetUri) {
                                                        currentlyPlayingUri = null
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                            contentDescription = "Preview Sound",
                                            tint = if (isPlaying || isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = sound.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = sound.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = primaryColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Apply This Sound" Button
            Button(
                onClick = {
                    soundPlayer.release()
                    onSoundApplied(selectedSound.uriString, selectedSound.name)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Apply: ${selectedSound.name}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
