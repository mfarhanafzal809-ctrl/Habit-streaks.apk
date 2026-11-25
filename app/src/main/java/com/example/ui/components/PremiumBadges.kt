package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIsDarkTheme

/**
 * Unified Premium ("PRO") badge with tiny micro-text displaying the exact number
 * of ads required to unlock the feature (or "Ad-Free" when an Ad-Free Pass is active).
 */
@Composable
fun PremiumFeatureBadge(
    requiredAdsLabel: String,
    isAdFreeActive: Boolean = false,
    compact: Boolean = false,
    inlineAdCount: Boolean = true,
    onDarkSurface: Boolean = false,
    customPillText: String? = null,
    showAdCount: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current || onDarkSurface

    val pillColor = if (isAdFreeActive) Color(0xFF10B981) else Color(0xFFFACC15)
    val pillBorder = if (isAdFreeActive) Color(0xFF059669) else Color(0xFFEAB308)
    val pillContentColor = Color(0xFF0F172A)

    val adText = if (isAdFreeActive) "0 Ads • Ad-Free" else requiredAdsLabel
    val adTextColor = when {
        isAdFreeActive -> Color(0xFF10B981)
        isDark -> Color(0xFFFDE047)
        else -> Color(0xFF92400E)
    }
    val adChipBg = when {
        isAdFreeActive -> Color(0xFF10B981).copy(alpha = if (isDark) 0.18f else 0.14f)
        isDark -> Color(0xFFFACC15).copy(alpha = 0.16f)
        else -> Color(0xFFFEF3C7)
    }
    val adChipBorder = when {
        isAdFreeActive -> Color(0xFF10B981).copy(alpha = 0.55f)
        else -> Color(0xFFF59E0B).copy(alpha = 0.55f)
    }

    if (!showAdCount) {
        ProPill(
            isAdFreeActive = isAdFreeActive,
            pillColor = pillColor,
            pillBorder = pillBorder,
            contentColor = pillContentColor,
            compact = compact,
            customPillText = customPillText,
            modifier = modifier
        )
        return
    }

    if (inlineAdCount) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ProPill(
                isAdFreeActive = isAdFreeActive,
                pillColor = pillColor,
                pillBorder = pillBorder,
                contentColor = pillContentColor,
                compact = compact,
                customPillText = customPillText
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = adChipBg,
                border = BorderStroke(0.8.dp, adChipBorder)
            ) {
                Text(
                    text = adText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 8.5.sp else 9.sp,
                        lineHeight = 10.sp
                    ),
                    color = adTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        horizontal = if (compact) 5.dp else 6.dp,
                        vertical = if (compact) 1.5.dp else 2.dp
                    )
                )
            }
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ProPill(
                isAdFreeActive = isAdFreeActive,
                pillColor = pillColor,
                pillBorder = pillBorder,
                contentColor = pillContentColor,
                compact = compact,
                customPillText = customPillText
            )
            Text(
                text = adText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp,
                    lineHeight = 10.sp
                ),
                color = adTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProPill(
    isAdFreeActive: Boolean,
    pillColor: Color,
    pillBorder: Color,
    contentColor: Color,
    compact: Boolean,
    customPillText: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = pillColor,
        border = BorderStroke(0.8.dp, pillBorder)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 3.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAdFreeActive) Icons.Default.CheckCircle else Icons.Default.WorkspacePremium,
                contentDescription = if (isAdFreeActive) "Unlocked Ad-Free" else "Premium Feature",
                tint = contentColor,
                modifier = Modifier.size(if (compact) 10.dp else 12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = customPillText ?: (if (isAdFreeActive) "UNLOCKED" else "PRO"),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = if (compact) 8.5.sp else 9.5.sp,
                    letterSpacing = 0.4.sp,
                    lineHeight = 10.sp
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
