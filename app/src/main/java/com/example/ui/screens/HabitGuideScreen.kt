package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassmorphicBackdrop
import com.example.ui.theme.LocalGlassmorphismEnabled
import com.example.ui.theme.glassmorphicCard
import com.example.ui.theme.glassmorphicCardBorder
import com.example.ui.theme.glassmorphicCardColors
import com.example.ui.theme.glassmorphicCardElevation

private data class GuideTopic(
    val title: String,
    val badge: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun HabitGuideScreen(
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit
) {
    val isGlass = LocalGlassmorphismEnabled.current
    val topics = listOf(
        GuideTopic(
            title = "Build Unbreakable Daily Streaks",
            badge = "STREAKS",
            description = "Start with a 2-minute version of each habit so even busy days keep your chain alive. Tap any habit card to view streak milestones, heatmaps, and consistency rates.",
            icon = Icons.Default.LocalFireDepartment,
            accent = Color(0xFFF59E0B)
        ),
        GuideTopic(
            title = "Exact Alarm & Reminder Reliability",
            badge = "ALARMS",
            description = "Choose between Full-Screen Ringing, High-Priority Heads-Up Alerts, or Notification mode per habit. Enable Exact Alarms and Battery Optimization Exemption in Settings for zero-delay wakeups.",
            icon = Icons.Default.AlarmOn,
            accent = Color(0xFF22D3EE)
        ),
        GuideTopic(
            title = "Custom Ringtones & Vibration",
            badge = "AUDIO",
            description = "Pick built-in alert chimes, device alarms, or import your own MP3/WAV track inside the habit editor. You can test both alarm audio and full-screen ringing anytime in Settings.",
            icon = Icons.Default.MusicNote,
            accent = Color(0xFF34D399)
        ),
        GuideTopic(
            title = "Glass Morphine & 30+ Themes",
            badge = "VISUALS",
            description = "Open Customize Theme for 30+ curated palettes, Dynamic Material You colors, and Custom Studio. Use Glass Morphine Effects in the menu to tune Lens Curvature, Bloom, Reflection Depth, and Aurora orbs.",
            icon = Icons.Default.AutoAwesome,
            accent = Color(0xFFA855F7)
        ),
        GuideTopic(
            title = "Quick Launcher & Personalization",
            badge = "PRO TIPS",
            description = "Tap Quick Launcher in the side menu to search and complete habits in one tap. Long-press your home greeting for 3 seconds anytime to personalize your name, and add the Home Widget for lock-free tracking.",
            icon = Icons.Default.Bolt,
            accent = Color(0xFF6366F1)
        ),
        GuideTopic(
            title = "Home Widget & Backups",
            badge = "SYNC",
            description = "Configure widget urgency highlights and transparency in Settings. Export JSON backups or CSV history spreadsheets anytime to safeguard your streaks.",
            icon = Icons.Default.Widgets,
            accent = Color(0xFFFB7185)
        )
    )

    GlassmorphicBackdrop {
        Scaffold(
            containerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenDrawer, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Habit & VFX Guide",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = "Tips for mastery & reliability",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(topics) { topic ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicCard(shape = RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = glassmorphicCardColors(),
                        elevation = glassmorphicCardElevation(),
                        border = glassmorphicCardBorder()
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = topic.icon,
                                        contentDescription = null,
                                        tint = topic.accent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = topic.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = topic.accent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = topic.badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = topic.accent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = topic.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
