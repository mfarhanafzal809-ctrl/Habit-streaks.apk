package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassTintPreset
import com.example.ui.theme.GlassVfxConfig
import com.example.ui.theme.LocalGlassVfxConfig
import com.example.ui.theme.LocalGlassmorphismEnabled
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.glassmorphicCard
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassMorphineVfxBottomSheet(
    currentConfig: GlassVfxConfig,
    isAdFreeActive: Boolean = false,
    onSaveConfig: (GlassVfxConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(currentConfig) { mutableStateOf(currentConfig.clamped()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = LocalIsDarkTheme.current
    val haptic = LocalHapticFeedback.current

    val sheetBg = if (isDark) Color(0xFF0A0F1A) else MaterialTheme.colorScheme.surface
    val cardBg = if (isDark) Color(0xFF0F1624) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val cardBorderColor = if (isDark) Color(0xFF1E293B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.onSurface
    val subColor = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant

    val (resolvedPrimary, resolvedSecondary) = draft.resolveGlowColors(
        themePrimary = MaterialTheme.colorScheme.primary,
        themeSecondary = MaterialTheme.colorScheme.secondary,
        isDark = isDark
    )
    val accentCyan = if (draft.tint == GlassTintPreset.THEME.key) {
        if (isDark) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary
    } else {
        resolvedPrimary
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = titleColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Glass Morphine & VFX",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = titleColor
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    PremiumFeatureBadge(
                        requiredAdsLabel = "2 Ads",
                        isAdFreeActive = isAdFreeActive,
                        compact = true
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Custom Lens Curvature, Highlight Bloom & Reflection Depth",
                        style = MaterialTheme.typography.bodySmall,
                        color = subColor
                    )
                }

                val requiresRewardedAds = draft.enabled && !isAdFreeActive
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSaveConfig(draft.clamped())
                        onDismiss()
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("vfx_save_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentCyan,
                        contentColor = if (isDark) Color(0xFF04121F) else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        if (requiresRewardedAds) {
                            Text(
                                text = "2 Ads",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp
                                )
                            )
                        }
                    }
                }
            }

            // Live 3D Glass Preview Card driven by draft state in real time
            CompositionLocalProvider(
                LocalGlassmorphismEnabled provides true,
                LocalGlassVfxConfig provides draft.copy(enabled = true)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphicCard(shape = RoundedCornerShape(20.dp), borderWidth = 1.8.dp)
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live 3D Glass Preview",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = titleColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = draft.summaryText(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFE2E8F0).copy(alpha = 0.85f) else subColor
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Live 3D Glass Preview",
                            tint = accentCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Master Toggle Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Enable Glass Morphine System",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = titleColor
                        )
                        Text(
                            text = "Frosted glass card filters and specular edge glows",
                            style = MaterialTheme.typography.bodySmall,
                            color = subColor
                        )
                    }
                    Switch(
                        checked = draft.enabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            draft = draft.copy(enabled = it)
                        },
                        modifier = Modifier.testTag("vfx_enable_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isDark) Color(0xFF04121F) else Color.White,
                            checkedTrackColor = accentCyan,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // Glass Refraction Color Tint Chips
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Glass Refraction Color Tint",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = titleColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GlassTintPreset.entries.forEach { preset ->
                            val selected = draft.tint.equals(preset.key, ignoreCase = true)
                            val chipBg = if (selected) {
                                accentCyan
                            } else {
                                if (isDark) Color(0xFF141C2B) else Color(0xFFE2E8F0).copy(alpha = 0.65f)
                            }
                            val chipText = if (selected) {
                                if (isDark) Color(0xFF04121F) else Color.White
                            } else {
                                titleColor.copy(alpha = 0.85f)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) accentCyan else cardBorderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        draft = draft.copy(tint = preset.key)
                                    }
                                    .testTag("vfx_tint_${preset.key.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    ),
                                    color = chipText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 4 Interactive VFX Sliders
            VfxSliderCard(
                title = "Lens Curvature Intensity",
                subtitle = "Bulge depth & optical convex dome radius",
                value = draft.lensCurvature,
                onValueChange = { draft = draft.copy(lensCurvature = it) },
                accentColor = accentCyan,
                secondaryColor = resolvedSecondary,
                cardBg = cardBg,
                borderColor = cardBorderColor,
                titleColor = titleColor,
                subColor = subColor,
                isDark = isDark,
                testTag = "vfx_slider_curvature"
            )

            VfxSliderCard(
                title = "Highlight Bloom Strength",
                subtitle = "Specular gloss radiance & internal refraction volume",
                value = draft.highlightBloom,
                onValueChange = { draft = draft.copy(highlightBloom = it) },
                accentColor = accentCyan,
                secondaryColor = resolvedSecondary,
                cardBg = cardBg,
                borderColor = cardBorderColor,
                titleColor = titleColor,
                subColor = subColor,
                isDark = isDark,
                testTag = "vfx_slider_bloom"
            )

            VfxSliderCard(
                title = "Reflection Depth",
                subtitle = "Specular top rim lines, caustic arcs & border stroke depth",
                value = draft.reflectionDepth,
                onValueChange = { draft = draft.copy(reflectionDepth = it) },
                accentColor = accentCyan,
                secondaryColor = resolvedSecondary,
                cardBg = cardBg,
                borderColor = cardBorderColor,
                titleColor = titleColor,
                subColor = subColor,
                isDark = isDark,
                testTag = "vfx_slider_reflection"
            )

            VfxSliderCard(
                title = "Frosted Translucency (Opacity)",
                subtitle = "Frosted backdrop fill density & ambient tint translucency",
                value = draft.frostedTranslucency,
                valueRange = 0.15f..1f,
                onValueChange = { draft = draft.copy(frostedTranslucency = it) },
                accentColor = accentCyan,
                secondaryColor = resolvedSecondary,
                cardBg = cardBg,
                borderColor = cardBorderColor,
                titleColor = titleColor,
                subColor = subColor,
                isDark = isDark,
                testTag = "vfx_slider_translucency"
            )

            // Atmospheric Aurora Backdrop Toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Atmospheric Aurora Backdrop",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = titleColor
                        )
                        Text(
                            text = "Floating nebula gradient orbs behind main screens",
                            style = MaterialTheme.typography.bodySmall,
                            color = subColor
                        )
                    }
                    Switch(
                        checked = draft.auroraBackdrop,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            draft = draft.copy(auroraBackdrop = it)
                        },
                        modifier = Modifier.testTag("vfx_aurora_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isDark) Color(0xFF04121F) else Color.White,
                            checkedTrackColor = accentCyan,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // Bottom 2 Toggle Pills: Specular Gloss & Caustic Glow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VfxFeatureTogglePill(
                    label = "Specular Gloss",
                    icon = Icons.Default.Flare,
                    selected = draft.specularGloss,
                    accentColor = MaterialTheme.colorScheme.primary,
                    glowAccent = accentCyan,
                    isDark = isDark,
                    cardBorderColor = cardBorderColor,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        draft = draft.copy(specularGloss = !draft.specularGloss)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vfx_gloss_toggle")
                )

                VfxFeatureTogglePill(
                    label = "Caustic Glow",
                    icon = Icons.Default.GridOn,
                    selected = draft.causticGlow,
                    accentColor = MaterialTheme.colorScheme.primary,
                    glowAccent = accentCyan,
                    isDark = isDark,
                    cardBorderColor = cardBorderColor,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        draft = draft.copy(causticGlow = !draft.causticGlow)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vfx_caustic_toggle")
                )
            }

            // Reset Defaults action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        draft = GlassVfxConfig(enabled = draft.enabled)
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("vfx_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = subColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset Optical Defaults",
                        style = MaterialTheme.typography.labelMedium,
                        color = subColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun VfxSliderCard(
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    secondaryColor: Color,
    cardBg: Color,
    borderColor: Color,
    titleColor: Color,
    subColor: Color,
    isDark: Boolean,
    testTag: String,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    val pct = (value * 100f).roundToInt()
    val norm = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = titleColor
                )
                Text(
                    text = "${pct}%",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    val trackHeight = 14.dp.toPx()
                    val trackY = (size.height - trackHeight) / 2f
                    val trackRadius = trackHeight / 2f
                    val thumbWidth = 5.dp.toPx()
                    val usableWidth = (size.width - thumbWidth).coerceAtLeast(1f)
                    val thumbX = (usableWidth * norm).coerceIn(0f, usableWidth)

                    // Inactive track
                    drawRoundRect(
                        color = if (isDark) Color(0xFF1F1D3A) else Color(0xFFE2E8F0),
                        topLeft = Offset(0f, trackY),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(trackRadius, trackRadius)
                    )

                    // Active glowing track
                    if (thumbX > 2f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    accentColor,
                                    secondaryColor.copy(alpha = 0.9f),
                                    accentColor
                                )
                            ),
                            topLeft = Offset(0f, trackY),
                            size = Size(thumbX.coerceAtLeast(trackHeight), trackHeight),
                            cornerRadius = CornerRadius(trackRadius, trackRadius)
                        )
                    }

                    // Right end indicator dot
                    drawCircle(
                        color = accentColor.copy(alpha = 0.7f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(size.width - 7.dp.toPx(), size.height / 2f)
                    )

                    // Vertical capsule thumb matching screenshot
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(thumbX, 0f),
                        size = Size(thumbWidth, size.height),
                        cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
                    )
                }

                Slider(
                    value = value.coerceIn(valueRange.start, valueRange.endInclusive),
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
private fun VfxFeatureTogglePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accentColor: Color,
    glowAccent: Color,
    isDark: Boolean,
    cardBorderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) {
        if (isDark) Color(0xFF281C47) else accentColor.copy(alpha = 0.16f)
    } else {
        if (isDark) Color(0xFF0F1624) else Color.Transparent
    }
    val borderColor = if (selected) {
        if (isDark) Color(0xFF7C3AED) else accentColor
    } else {
        cardBorderColor
    }
    val textColor = if (selected) {
        if (isDark) Color(0xFFF8FAFC) else accentColor
    } else {
        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    }
    val iconColor = if (selected) {
        if (isDark) Color(0xFFD8B4FE) else accentColor
    } else {
        if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    }

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
