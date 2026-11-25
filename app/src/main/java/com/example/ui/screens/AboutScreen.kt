package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HabitAmber
import com.example.ui.theme.HabitCoral
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onOpenDrawer: () -> Unit) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = SecondaryCyan,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "About Habit Streaks",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Version 1.0.0 • Enterprise Edition",
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryCyan,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // About Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Our Core Philosophy & Architecture",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Habit Streaks is engineered on a strict local-first, privacy-respecting architecture. We believe personal productivity and habit data belongs entirely to you. All habit records, streaks, notes, and completion histories are securely stored in your device's sandboxed local SQLite database with zero cloud tracking or telemetry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "• 100% Offline-First: All core habit tracking, reminders, and calendar analytics function completely without internet access.\n• Privacy & UMP Consent: Fully compliant with Google User Messaging Platform (UMP) standards for transparent advertising choices.\n• Data Sovereignty: Full JSON backup and restore with zero cloud lock-in.\n• Zero Third-Party Analytics: No hidden crash trackers, telemetry SDKs, or background identifiers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // User Guide Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = HabitAmber, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Complete User Guide & Feature Manual",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ⭐ STAR FEATURE: AD-FREE MODE GUIDANCE (PROMINENT & VERY NOTICEABLE)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.12f)),
                border = BorderStroke(2.dp, PrimaryIndigo)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(PrimaryIndigo, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = HabitAmber.copy(alpha = 0.2f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = " ⭐ PREMIUM REWARD FEATURE ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = HabitAmber
                                )
                            }
                            Text(
                                text = "HOW TO GO ADS-FREE",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = PrimaryIndigo
                            )
                            Text(
                                text = "Seamless, Uninterrupted Productivity Hours",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Enjoy complete uninterrupted focus without banners, popups, or interstitial ads! Here is how our automated ad-free playback works:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GuideBulletPoint(
                        title = "1. Select Duration (6h or 12h)",
                        description = "Tap the 'Go Ad-Free' badge on the Home Screen header or in Settings to choose your desired ad-free duration (6 hours or 12 hours)."
                    )
                    GuideBulletPoint(
                        title = "2. Autonomous Sponsor Video Chain",
                        description = "Our system plays the required sponsor moments in sequence automatically 1 after 1! You do NOT need to tap 'Reward Granted' or click dismiss buttons after each ad — the app automatically handles the sequence."
                    )
                    GuideBulletPoint(
                        title = "3. Instant Ad-Free Activation",
                        description = "As soon as the sponsor sequence finishes, your ad-free pass activates instantly. All bottom banners disappear, creation flows open without transitions, and premium customizations unlock seamlessly."
                    )
                    GuideBulletPoint(
                        title = "4. Remaining Time Counter",
                        description = "A live countdown badge on the Home Screen displays your remaining ad-free hours and minutes so you always know your active status."
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Guide Card: Habits & Scheduling
            GuideSectionCard(
                icon = Icons.Default.LocalFireDepartment,
                iconTint = HabitCoral,
                title = "1. Habits & Flexible Frequency Rules",
                description = "Configure habits for Daily, Weekday-only, or Weekend-only routines. Set custom color labels, categories (Health, Mindset, Fitness, etc.), and targets. Mark habits complete with one tap from the checklist."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Guide Card: Smart Reminders & Media Ringtone Import
            GuideSectionCard(
                icon = Icons.Default.NotificationsActive,
                iconTint = HabitAmber,
                title = "2. Smart Reminders & Custom Ringtone Imports",
                description = "Schedule multiple reminder times per habit with exact alarm support. Choose from pre-bundled musical chimes or import your own audio/video files — our zero-dependency audio extractor automatically extracts the sound and sets it as your notification ringtone."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Guide Card: Calendar & History
            GuideSectionCard(
                icon = Icons.Default.CalendarMonth,
                iconTint = SecondaryCyan,
                title = "3. Calendar & Heatmap Tracking",
                description = "Inspect historical consistency across any month. Tap past calendar days to retroactively log completed habits or make adjustments while keeping streak calculations accurate and reliable."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Guide Card: Themes & Glassmorphism
            GuideSectionCard(
                icon = Icons.Default.AutoAwesome,
                iconTint = PrimaryIndigo,
                title = "4. 24+ Themes & Glassmorphic UI",
                description = "Choose from 24+ professionally curated themes including Cyberpunk Neon, Electric Indigo, Midnight Rose, Obsidian Gold, and Emerald. Enable Material You dynamic colors or modern glassmorphic translucent effects."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Guide Card: Widgets & Quick Controls
            GuideSectionCard(
                icon = Icons.Default.Widgets,
                iconTint = HabitAmber,
                title = "5. Home Screen Glance Widgets",
                description = "Add our adaptive Home Screen widget. The widget dynamically resizes its height when habits are completed or when desktop space is tight, offering 1-tap completion toggles right from your launcher."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Guide Card: Backup & Restore
            GuideSectionCard(
                icon = Icons.Default.CloudDownload,
                iconTint = PrimaryIndigo,
                title = "6. Secure Backup, Export & Restore",
                description = "Export all your habit records and completion history into a secure JSON backup file stored safely in your device storage. Easily restore your data anytime or migrate seamlessly between Android devices."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Guide Card: Offline Mode & Privacy
            GuideSectionCard(
                icon = Icons.Default.Security,
                iconTint = SecondaryCyan,
                title = "7. Offline Mode & Continuity Guarantee",
                description = "When no internet is available, tapping 'No Internet' opens the feature immediately without blocking. Your core habit creation, alarms, and widgets are never dependent on an ad server or network connection."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Guide Card: Premium Features
            GuideSectionCard(
                icon = Icons.Default.AutoAwesome,
                iconTint = HabitCoral,
                title = "👑 8. Premium Features Guide",
                description = "• Custom Color Studio: Create, save, and manage custom color combinations using plain language or hex codes (2 Ads).\n• Dynamic Rainbow Auto-Transition: Continuously and smoothly cycles colors across all built-in and custom palettes (2 Ads).\n• Ad-Free Mode: Enjoy complete uninterrupted app streaming."
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Floating Drawer Menu Button
        IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun GuideSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GuideBulletPoint(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = PrimaryIndigo
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
