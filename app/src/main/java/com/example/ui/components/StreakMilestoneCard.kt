package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StreakMilestone
import com.example.ui.theme.HabitAmber
import com.example.ui.theme.PrimaryIndigo

@Composable
fun StreakMilestoneCard(
    milestone: StreakMilestone,
    progressStreak: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val progress = (progressStreak.toFloat() / milestone.days.toFloat()).coerceIn(0f, 1f)

    val primaryColor = runCatching {
        Color(android.graphics.Color.parseColor(milestone.primaryColorHex))
    }.getOrDefault(HabitAmber)

    val secondaryColor = runCatching {
        Color(android.graphics.Color.parseColor(milestone.secondaryColorHex))
    }.getOrDefault(PrimaryIndigo)

    val badgeIcon: ImageVector = when (milestone.badgeIconName) {
        "Flame" -> Icons.Default.LocalFireDepartment
        "Bolt" -> Icons.Default.FlashOn
        "Target" -> Icons.Default.Adjust
        "Eco" -> Icons.Default.Spa
        "Crown" -> Icons.Default.MilitaryTech
        "Shield" -> Icons.Default.Shield
        "Rocket" -> Icons.Default.RocketLaunch
        "Trophy" -> Icons.Default.EmojiEvents
        "Star" -> Icons.Default.AutoAwesome
        else -> Icons.Default.EmojiEvents
    }

    val isGlass = com.example.ui.theme.LocalGlassmorphismEnabled.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = com.example.ui.theme.glassmorphicCardColors(
            containerColor = if (milestone.isUnlocked) {
                primaryColor.copy(alpha = if (isGlass) 0.18f else 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = com.example.ui.theme.glassmorphicCardElevation(defaultElevation = if (milestone.isUnlocked) 2.dp else 1.dp),
        border = com.example.ui.theme.glassmorphicCardBorder() ?: if (milestone.isUnlocked) androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.4f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unique Tier Badge Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (milestone.isUnlocked) {
                            Brush.linearGradient(listOf(primaryColor, secondaryColor))
                        } else {
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                        }
                    )
                    .then(
                        if (milestone.isUnlocked) Modifier.border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (milestone.isUnlocked) badgeIcon else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (milestone.isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        // Badge Tier Label
                        Text(
                            text = "${milestone.badgeTier} • ${milestone.days}D",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp,
                                color = if (milestone.isUnlocked) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                        Text(
                            text = milestone.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (milestone.isUnlocked) primaryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (milestone.isUnlocked) "UNLOCKED 🏆" else "$progressStreak/${milestone.days}d",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (milestone.isUnlocked) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (milestone.achievedCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents, 
                            contentDescription = null, 
                            tint = primaryColor, 
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Achieved ${milestone.achievedCount} time${if (milestone.achievedCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = primaryColor
                        )
                    }
                }

                if (!milestone.isUnlocked) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = primaryColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (onClick != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (milestone.isUnlocked) "Tap to inspect badge & replay celebration 🎉" else "Tap to preview badge & reward 🎯",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }
    }
}
