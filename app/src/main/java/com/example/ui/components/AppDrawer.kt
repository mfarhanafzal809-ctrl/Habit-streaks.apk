package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainTab
import com.example.SubScreen
import com.example.ui.theme.LocalIsDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    currentTab: MainTab,
    currentSubScreen: SubScreen,
    onTabSelected: (MainTab) -> Unit,
    onNavigateToSubScreen: (SubScreen) -> Unit,
    themeTemplate: String,
    onThemeCustomizeClick: () -> Unit,
    onGlassVfxClick: () -> Unit = {},
    onQuickLauncherClick: () -> Unit = { onTabSelected(MainTab.HOME) },
    isAdFreeActive: Boolean = false,
    onOpenAdFreeClick: () -> Unit = {},
    isGlassVfxEnabled: Boolean = false
) {
    val isDark = LocalIsDarkTheme.current
    val haptic = LocalHapticFeedback.current

    val drawerBg = if (isDark) Color(0xFF060912) else MaterialTheme.colorScheme.background
    val titleAccent = if (isDark) Color(0xFF22D3EE) else MaterialTheme.colorScheme.primary
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.onBackground
    val textMuted = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant

    ModalDrawerSheet(
        drawerContainerColor = drawerBg,
        drawerContentColor = textPrimary,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Branding + PRO / AD-FREE Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "HABIT STREAKS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = titleAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "PREMIUM PRODUCTIVITY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.8.sp,
                            fontSize = 10.sp
                        ),
                        color = textMuted
                    )
                }

                // Gold PRO / AD-FREE Pill (48dp touch height for a11y) with ad-count micro-text
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenAdFreeClick()
                        }
                        .testTag("drawer_pro_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isAdFreeActive) Color(0xFF10B981) else Color(0xFFFACC15),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = if (isAdFreeActive) "Ad-Free Active" else "Unlock Ad-Free",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAdFreeActive) "AD-FREE" else "PREMIUM PASS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.6.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                        Text(
                            text = if (isAdFreeActive) "0 Ads • Active" else "Ad-Free Pass",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
                            ),
                            color = if (isAdFreeActive) Color(0xFF10B981) else if (isDark) Color(0xFFFDE047) else Color(0xFF92400E),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Launcher Glowing Glass Card
            val quickGradient = if (isDark) {
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0D283B).copy(alpha = 0.92f),
                        Color(0xFF0A1B2E).copy(alpha = 0.88f),
                        Color(0xFF10243C).copy(alpha = 0.92f)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(quickGradient)
                    .border(
                        width = 1.2.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                titleAccent.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.60f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onQuickLauncherClick()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("drawer_quick_launcher")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF00E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF04121F),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Quick Launcher",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            PremiumFeatureBadge(
                                requiredAdsLabel = "1 Ad",
                                isAdFreeActive = isAdFreeActive,
                                compact = true
                            )
                        }
                        Text(
                            text = "Search & toggle habits in seconds",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = titleAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "NAVIGATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                ),
                color = textMuted.copy(alpha = 0.78f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            DrawerItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentTab == MainTab.HOME && currentSubScreen == SubScreen.NONE,
                onClick = { onTabSelected(MainTab.HOME) },
                testTag = "drawer_nav_home"
            )
            DrawerItem(
                icon = Icons.Default.CalendarMonth,
                label = "Calendar",
                isSelected = currentTab == MainTab.CALENDAR && currentSubScreen == SubScreen.NONE,
                onClick = { onTabSelected(MainTab.CALENDAR) },
                testTag = "drawer_nav_calendar"
            )
            DrawerItem(
                icon = Icons.Default.EmojiEvents,
                label = "Streaks & Stats",
                isSelected = currentTab == MainTab.STATS && currentSubScreen == SubScreen.NONE,
                onClick = { onTabSelected(MainTab.STATS) },
                testTag = "drawer_nav_stats"
            )
            DrawerItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "Guide",
                isSelected = currentSubScreen == SubScreen.GUIDE,
                onClick = { onNavigateToSubScreen(SubScreen.GUIDE) },
                testTag = "drawer_nav_guide"
            )
            DrawerItem(
                icon = Icons.Default.Email,
                label = "Contact",
                isSelected = currentSubScreen == SubScreen.CONTACT,
                onClick = { onNavigateToSubScreen(SubScreen.CONTACT) },
                testTag = "drawer_nav_contact"
            )
            DrawerItem(
                icon = Icons.Default.Language,
                label = "About",
                isSelected = currentSubScreen == SubScreen.ABOUT,
                onClick = { onNavigateToSubScreen(SubScreen.ABOUT) },
                testTag = "drawer_nav_about"
            )
            DrawerItem(
                icon = Icons.Default.PrivacyTip,
                label = "Privacy Policy",
                isSelected = currentSubScreen == SubScreen.PRIVACY_POLICY,
                onClick = { onNavigateToSubScreen(SubScreen.PRIVACY_POLICY) },
                testTag = "drawer_nav_privacy"
            )
            DrawerItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentTab == MainTab.SETTINGS && currentSubScreen == SubScreen.NONE,
                onClick = { onTabSelected(MainTab.SETTINGS) },
                testTag = "drawer_nav_settings"
            )

            Spacer(modifier = Modifier.height(18.dp))
            Spacer(modifier = Modifier.weight(1f, fill = false))

            Text(
                text = "THEME",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                ),
                color = textMuted.copy(alpha = 0.78f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Customize Theme Card
            DrawerThemeActionCard(
                title = "Customize Theme",
                subtitle = "30+ Palettes & Live Preview",
                requiredAdsLabel = "1–2 Ads",
                isAdFreeActive = isAdFreeActive,
                subtitleColor = if (isDark) Color(0xFF22D3EE) else MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Palette,
                iconBadgeColor = Color(0xFF00E5FF),
                iconTint = Color(0xFF04121F),
                isDark = isDark,
                borderBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF22D3EE).copy(alpha = 0.75f),
                        Color(0xFF38BDF8).copy(alpha = 0.45f)
                    )
                ),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onThemeCustomizeClick()
                },
                testTag = "drawer_customize_theme"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Glass Morphine Effects Card
            DrawerThemeActionCard(
                title = "Glass Morphine Effects",
                subtitle = if (isGlassVfxEnabled) "UHD Aurora, Gloss & Caustic VFX • ON" else "UHD Aurora, Gloss & Caustic VFX",
                requiredAdsLabel = "2 Ads",
                isAdFreeActive = isAdFreeActive,
                subtitleColor = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED),
                icon = Icons.Default.AutoAwesome,
                iconBadgeColor = Color(0xFFC084FC),
                iconTint = Color(0xFF1E0B36),
                isDark = isDark,
                borderBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFC084FC).copy(alpha = 0.78f),
                        Color(0xFF818CF8).copy(alpha = 0.50f)
                    )
                ),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onGlassVfxClick()
                },
                testTag = "drawer_glass_vfx"
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DrawerThemeActionCard(
    title: String,
    subtitle: String,
    requiredAdsLabel: String,
    isAdFreeActive: Boolean,
    subtitleColor: Color,
    icon: ImageVector,
    iconBadgeColor: Color,
    iconTint: Color,
    isDark: Boolean,
    borderBrush: Brush,
    onClick: () -> Unit,
    testTag: String
) {
    val cardBg = if (isDark) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF161D2C).copy(alpha = 0.92f),
                Color(0xFF121826).copy(alpha = 0.88f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                MaterialTheme.colorScheme.surface
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.2.dp, borderBrush, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBadgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                PremiumFeatureBadge(
                    requiredAdsLabel = requiredAdsLabel,
                    isAdFreeActive = isAdFreeActive,
                    compact = true
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = subtitleColor.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    val isDark = LocalIsDarkTheme.current
    val bgColor = if (isSelected) {
        if (isDark) Color(0xFF28184B) else MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (isSelected) {
        if (isDark) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        if (isDark) Color(0xFFE2E8F0).copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    }

    val itemModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(bgColor)
        .then(
            if (isSelected) {
                Modifier.border(
                    1.dp,
                    if (isDark) Color(0xFF6D28D9).copy(alpha = 0.65f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    RoundedCornerShape(16.dp)
                )
            } else Modifier
        )
        .clickable { onClick() }
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }

    Row(
        modifier = itemModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected && !isDark) MaterialTheme.colorScheme.primary else contentColor,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp
            ),
            color = contentColor
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}
