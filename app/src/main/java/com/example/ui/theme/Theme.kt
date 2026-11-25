package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

enum class GlassTintPreset(
    val key: String,
    val label: String,
    val darkPrimaryGlow: Color,
    val darkSecondaryGlow: Color,
    val lightPrimaryGlow: Color,
    val lightSecondaryGlow: Color
) {
    THEME(
        key = "THEME",
        label = "Theme",
        darkPrimaryGlow = Color(0xFF00FFFF),
        darkSecondaryGlow = Color(0xFF38BDF8),
        lightPrimaryGlow = Color(0xFF2563EB),
        lightSecondaryGlow = Color(0xFF38BDF8)
    ),
    CYAN(
        key = "CYAN",
        label = "Cyan",
        darkPrimaryGlow = Color(0xFF00F0FF),
        darkSecondaryGlow = Color(0xFF38BDF8),
        lightPrimaryGlow = Color(0xFF0284C7),
        lightSecondaryGlow = Color(0xFF06B6D4)
    ),
    INDIGO(
        key = "INDIGO",
        label = "Indigo",
        darkPrimaryGlow = Color(0xFF818CF8),
        darkSecondaryGlow = Color(0xFFC084FC),
        lightPrimaryGlow = Color(0xFF4F46E5),
        lightSecondaryGlow = Color(0xFF6366F1)
    ),
    GOLD(
        key = "GOLD",
        label = "Gold",
        darkPrimaryGlow = Color(0xFFFACC15),
        darkSecondaryGlow = Color(0xFFF59E0B),
        lightPrimaryGlow = Color(0xFFD97706),
        lightSecondaryGlow = Color(0xFFF59E0B)
    ),
    ROSE(
        key = "ROSE",
        label = "Rose",
        darkPrimaryGlow = Color(0xFFFB7185),
        darkSecondaryGlow = Color(0xFFF472B6),
        lightPrimaryGlow = Color(0xFFE11D48),
        lightSecondaryGlow = Color(0xFFEC4899)
    );

    companion object {
        fun fromKey(key: String): GlassTintPreset =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: THEME
    }
}

@Immutable
data class GlassVfxConfig(
    val enabled: Boolean = false,
    val tint: String = GlassTintPreset.THEME.key,
    val lensCurvature: Float = 0.50f,
    val highlightBloom: Float = 0.70f,
    val reflectionDepth: Float = 0.65f,
    val frostedTranslucency: Float = 0.68f,
    val auroraBackdrop: Boolean = true,
    val specularGloss: Boolean = true,
    val causticGlow: Boolean = true
) {
    fun clamped(): GlassVfxConfig = copy(
        lensCurvature = lensCurvature.coerceIn(0f, 1f),
        highlightBloom = highlightBloom.coerceIn(0f, 1f),
        reflectionDepth = reflectionDepth.coerceIn(0f, 1f),
        frostedTranslucency = frostedTranslucency.coerceIn(0.15f, 1f)
    )

    fun resolveGlowColors(
        themePrimary: Color,
        themeSecondary: Color,
        isDark: Boolean
    ): Pair<Color, Color> {
        val preset = GlassTintPreset.fromKey(tint)
        return if (preset == GlassTintPreset.THEME) {
            if (isDark) {
                themePrimary to themeSecondary
            } else {
                themePrimary to themeSecondary
            }
        } else {
            if (isDark) {
                preset.darkPrimaryGlow to preset.darkSecondaryGlow
            } else {
                preset.lightPrimaryGlow to preset.lightSecondaryGlow
            }
        }
    }

    fun summaryText(): String {
        val c = (lensCurvature * 100f).roundToInt()
        val b = (highlightBloom * 100f).roundToInt()
        val r = (reflectionDepth * 100f).roundToInt()
        return "Curvature: ${c}% • Bloom: ${b}% • Reflection: ${r}%"
    }
}

val LocalGlassmorphismEnabled = staticCompositionLocalOf { false }
val LocalGlassVfxConfig = staticCompositionLocalOf { GlassVfxConfig() }
val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1), // Luminous Electric Indigo
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E1B4B),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF22D3EE), // Vibrant Cyan
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFF34D399),
    onTertiary = Color.White,
    background = Color(0xFF090D16), // Ultra-deep OLED slate
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF1E293B)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5), // Rich Trust Indigo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF0891B2), // Clean Cyan
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = Color(0xFFF4F6F9), // Warm modern airy slate
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEAEDF3),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

fun getThemeColorScheme(template: String, darkTheme: Boolean): ColorScheme {
    val baseDark = DarkColorScheme
    val baseLight = LightColorScheme
    if (template.startsWith("CUSTOM:")) {
        val parts = template.removePrefix("CUSTOM:").split("|")
        val primaryHex = parts.getOrNull(0) ?: "#4F46E5"
        val secondaryHex = parts.getOrNull(1) ?: "#06B6D4"
        val pColor = try { Color(android.graphics.Color.parseColor(primaryHex)) } catch (_: Exception) { Color(0xFF4F46E5) }
        val sColor = try { Color(android.graphics.Color.parseColor(secondaryHex)) } catch (_: Exception) { Color(0xFF06B6D4) }
        return if (darkTheme) {
            baseDark.copy(
                primary = pColor,
                secondary = sColor,
                primaryContainer = pColor.copy(alpha = 0.3f),
                secondaryContainer = sColor.copy(alpha = 0.3f)
            )
        } else {
            baseLight.copy(
                primary = pColor,
                secondary = sColor,
                primaryContainer = pColor.copy(alpha = 0.2f),
                secondaryContainer = sColor.copy(alpha = 0.2f)
            )
        }
    }
    return when(template) {
        "SUNSET_CORAL" -> if (darkTheme) baseDark.copy(
            primary = Color(0xFFFB923C), secondary = Color(0xFFF97316), tertiary = Color(0xFFFBBF24),
            primaryContainer = Color(0xFF431407), secondaryContainer = Color(0xFF331206),
            background = Color(0xFF160E09), surface = Color(0xFF201610), surfaceVariant = Color(0xFF2C1E16)
        ) else baseLight.copy(
            primary = Color(0xFFF97316), secondary = Color(0xFFFB923C), tertiary = Color(0xFFFBBF24),
            primaryContainer = Color(0xFFFFEDD5), secondaryContainer = Color(0xFFFED7AA),
            background = Color(0xFFFFF9F5), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFFFEFE6)
        )

        "OCEAN_BLUE" -> if (darkTheme) baseDark.copy(
            primary = Color(0xFF3B82F6), secondary = Color(0xFF60A5FA), tertiary = Color(0xFF38BDF8),
            primaryContainer = Color(0xFF1E3A8A), secondaryContainer = Color(0xFF0369A1),
            background = Color(0xFF0A0F1D), surface = Color(0xFF0F172A), surfaceVariant = Color(0xFF1E293B)
        ) else baseLight.copy(
            primary = Color(0xFF2563EB), secondary = Color(0xFF38BDF8), tertiary = Color(0xFF60A5FA),
            primaryContainer = Color(0xFFDBEAFE), secondaryContainer = Color(0xFFE0F2FE),
            background = Color(0xFFF4F8FD), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE2EFFD)
        )

        "EMERALD_MINT" -> if (darkTheme) baseDark.copy(
            primary = Color(0xFF10B981), secondary = Color(0xFF34D399), tertiary = Color(0xFF22C55E),
            primaryContainer = Color(0xFF064E3B), secondaryContainer = Color(0xFF022C22),
            background = Color(0xFF08170E), surface = Color(0xFF0E2316), surfaceVariant = Color(0xFF163321)
        ) else baseLight.copy(
            primary = Color(0xFF059669), secondary = Color(0xFF10B981), tertiary = Color(0xFF34D399),
            primaryContainer = Color(0xFFD1FAE5), secondaryContainer = Color(0xFFA7F3D0),
            background = Color(0xFFF2FAF5), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE0F7EB)
        )

        "GOLDEN_AMBER" -> if (darkTheme) baseDark.copy(
            primary = Color(0xFFF59E0B), secondary = Color(0xFFEAB308), tertiary = Color(0xFFD97706),
            primaryContainer = Color(0xFF451A03), secondaryContainer = Color(0xFF3B1802),
            background = Color(0xFF16130B), surface = Color(0xFF221C0E), surfaceVariant = Color(0xFF312713)
        ) else baseLight.copy(
            primary = Color(0xFFD97706), secondary = Color(0xFFF59E0B), tertiary = Color(0xFFFBBF24),
            primaryContainer = Color(0xFFFEF3C7), secondaryContainer = Color(0xFFFDE68A),
            background = Color(0xFFFFFDF5), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFFEF9C3)
        )

        "ORCHID_PURPLE" -> if (darkTheme) baseDark.copy(
            primary = Color(0xFFA855F7), secondary = Color(0xFFC084FC), tertiary = Color(0xFFE879F9),
            primaryContainer = Color(0xFF3B0764), secondaryContainer = Color(0xFF2E1065),
            background = Color(0xFF13091B), surface = Color(0xFF1D0E29), surfaceVariant = Color(0xFF2B153C)
        ) else baseLight.copy(
            primary = Color(0xFF7C3AED), secondary = Color(0xFFA855F7), tertiary = Color(0xFFC084FC),
            primaryContainer = Color(0xFFEDE9FE), secondaryContainer = Color(0xFFF3E8FF),
            background = Color(0xFFFAF5FF), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFF5E8FF)
        )

        "LIME_CYAN" -> if (darkTheme) baseDark.copy(
            primary = Color(0xFF22C55E), secondary = Color(0xFF22D3EE), tertiary = Color(0xFF4ADE80),
            primaryContainer = Color(0xFF052E16), secondaryContainer = Color(0xFF083344),
            background = Color(0xFF081714), surface = Color(0xFF0E231E), surfaceVariant = Color(0xFF15332B)
        ) else baseLight.copy(
            primary = Color(0xFF16A34A), secondary = Color(0xFF06B6D4), tertiary = Color(0xFF4ADE80),
            primaryContainer = Color(0xFFDCFCE7), secondaryContainer = Color(0xFFCFFAFE),
            background = Color(0xFFF5FDF7), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE3FBE9)
        )

        "CYBERPUNK" -> if (darkTheme) baseDark.copy(primary = Color(0xFFFF007F), secondary = Color(0xFF00F0FF), primaryContainer = Color(0xFF4A0027), secondaryContainer = Color(0xFF00363D))
                       else baseLight.copy(primary = Color(0xFFD9006C), secondary = Color(0xFF009CA6), primaryContainer = Color(0xFFFFD6EA), secondaryContainer = Color(0xFFBCEFF2))
        
        "EMERALD" -> if (darkTheme) baseDark.copy(primary = Color(0xFF34D399), secondary = Color(0xFF10B981), primaryContainer = Color(0xFF064E3B), secondaryContainer = Color(0xFF022C22))
                     else baseLight.copy(primary = Color(0xFF059669), secondary = Color(0xFF047857), primaryContainer = Color(0xFFA7F3D0), secondaryContainer = Color(0xFFD1FAE5))
        
        "ROSE" -> if (darkTheme) baseDark.copy(primary = Color(0xFFFB7185), secondary = Color(0xFFF43F5E), primaryContainer = Color(0xFF4C0519), secondaryContainer = Color(0xFF881337))
                  else baseLight.copy(primary = Color(0xFFE11D48), secondary = Color(0xFFBE123C), primaryContainer = Color(0xFFFECDD3), secondaryContainer = Color(0xFFFFE4E6))
        
        "SUNSET" -> if (darkTheme) baseDark.copy(primary = Color(0xFFFBBF24), secondary = Color(0xFFF59E0B), primaryContainer = Color(0xFF451A03), secondaryContainer = Color(0xFF78350F))
                    else baseLight.copy(primary = Color(0xFFD97706), secondary = Color(0xFFB45309), primaryContainer = Color(0xFFFEF3C7), secondaryContainer = Color(0xFFFDE68A))
        
        "OCEAN" -> if (darkTheme) baseDark.copy(primary = Color(0xFF38BDF8), secondary = Color(0xFF0EA5E9), primaryContainer = Color(0xFF0369A1), secondaryContainer = Color(0xFF075985))
                   else baseLight.copy(primary = Color(0xFF0284C7), secondary = Color(0xFF0369A1), primaryContainer = Color(0xFFE0F2FE), secondaryContainer = Color(0xFFBAE6FD))
        
        "ROYAL" -> if (darkTheme) baseDark.copy(primary = Color(0xFFA78BFA), secondary = Color(0xFFF472B6), primaryContainer = Color(0xFF2E1065), secondaryContainer = Color(0xFF500724))
                   else baseLight.copy(primary = Color(0xFF7C3AED), secondary = Color(0xFFDB2777), primaryContainer = Color(0xFFEDE9FE), secondaryContainer = Color(0xFFFCE7F3))
        
        "AURORA" -> if (darkTheme) baseDark.copy(primary = Color(0xFF2DD4BF), secondary = Color(0xFFA3E635), primaryContainer = Color(0xFF134E4A), secondaryContainer = Color(0xFF365314))
                    else baseLight.copy(primary = Color(0xFF0D9488), secondary = Color(0xFF65A30D), primaryContainer = Color(0xFFCCFBF1), secondaryContainer = Color(0xFFECFCCB))
        
        "OBSIDIAN_GOLD" -> if (darkTheme) baseDark.copy(primary = Color(0xFFFFD700), secondary = Color(0xFFF59E0B), primaryContainer = Color(0xFF382A02), secondaryContainer = Color(0xFF451A03))
                           else baseLight.copy(primary = Color(0xFFB45309), secondary = Color(0xFFD97706), primaryContainer = Color(0xFFFEF3C7), secondaryContainer = Color(0xFFFDE68A))
        
        "MIDNIGHT_NEON" -> if (darkTheme) baseDark.copy(primary = Color(0xFFC084FC), secondary = Color(0xFFA3E635), primaryContainer = Color(0xFF3B0764), secondaryContainer = Color(0xFF1A2E05))
                           else baseLight.copy(primary = Color(0xFF9333EA), secondary = Color(0xFF65A30D), primaryContainer = Color(0xFFF3E8FF), secondaryContainer = Color(0xFFECFCCB))
        
        "CHERRY_BLOSSOM" -> if (darkTheme) baseDark.copy(primary = Color(0xFFF472B6), secondary = Color(0xFFFB7185), primaryContainer = Color(0xFF500724), secondaryContainer = Color(0xFF4C0519))
                            else baseLight.copy(primary = Color(0xFFDB2777), secondary = Color(0xFFE11D48), primaryContainer = Color(0xFFFCE7F3), secondaryContainer = Color(0xFFFFE4E6))
        
        "COSMIC" -> if (darkTheme) baseDark.copy(primary = Color(0xFF818CF8), secondary = Color(0xFFC084FC), primaryContainer = Color(0xFF1E1B4B), secondaryContainer = Color(0xFF3B0764))
                    else baseLight.copy(primary = Color(0xFF4F46E5), secondary = Color(0xFF9333EA), primaryContainer = Color(0xFFEEF2FF), secondaryContainer = Color(0xFFF3E8FF))
        
        "ORANGE" -> if (darkTheme) baseDark.copy(primary = Color(0xFFFB923C), secondary = Color(0xFFFBBF24), primaryContainer = Color(0xFF431407), secondaryContainer = Color(0xFF422006))
                    else baseLight.copy(primary = Color(0xFFEA580C), secondary = Color(0xFFD97706), primaryContainer = Color(0xFFFFEDD5), secondaryContainer = Color(0xFFFEF08A))
        
        "CRIMSON" -> if (darkTheme) baseDark.copy(primary = Color(0xFFFB7185), secondary = Color(0xFFFDA4AF), primaryContainer = Color(0xFF4C0519), secondaryContainer = Color(0xFF500724))
                     else baseLight.copy(primary = Color(0xFFBE123C), secondary = Color(0xFF9F1239), primaryContainer = Color(0xFFFFE4E6), secondaryContainer = Color(0xFFFECDD3))
        
        "MINT" -> if (darkTheme) baseDark.copy(primary = Color(0xFF6EE7B7), secondary = Color(0xFF2DD4BF), primaryContainer = Color(0xFF022C22), secondaryContainer = Color(0xFF134E4A))
                  else baseLight.copy(primary = Color(0xFF059669), secondary = Color(0xFF0D9488), primaryContainer = Color(0xFFD1FAE5), secondaryContainer = Color(0xFFCCFBF1))
        
        "NEBULA_PURPLE" -> if (darkTheme) baseDark.copy(primary = Color(0xFFA855F7), secondary = Color(0xFFE879F9), primaryContainer = Color(0xFF3B0764), secondaryContainer = Color(0xFF581C87))
                           else baseLight.copy(primary = Color(0xFF7E22CE), secondary = Color(0xFFC026D3), primaryContainer = Color(0xFFF3E8FF), secondaryContainer = Color(0xFFFAE8FF))

        "VAPORWAVE" -> if (darkTheme) baseDark.copy(primary = Color(0xFFF43F5E), secondary = Color(0xFF22D3EE), primaryContainer = Color(0xFF4C0519), secondaryContainer = Color(0xFF083344))
                       else baseLight.copy(primary = Color(0xFFE11D48), secondary = Color(0xFF0891B2), primaryContainer = Color(0xFFFFE4E6), secondaryContainer = Color(0xFFCFFAFE))

        "NORDIC_FROST" -> if (darkTheme) baseDark.copy(primary = Color(0xFF67E8F9), secondary = Color(0xFF60A5FA), primaryContainer = Color(0xFF164E63), secondaryContainer = Color(0xFF1E3A8A))
                          else baseLight.copy(primary = Color(0xFF0284C7), secondary = Color(0xFF2563EB), primaryContainer = Color(0xFFE0F2FE), secondaryContainer = Color(0xFFDBEAFE))

        "DESERT_DUNE" -> if (darkTheme) baseDark.copy(primary = Color(0xFFFB923C), secondary = Color(0xFFFACC15), primaryContainer = Color(0xFF431407), secondaryContainer = Color(0xFF422006))
                         else baseLight.copy(primary = Color(0xFFEA580C), secondary = Color(0xFFCA8A04), primaryContainer = Color(0xFFFFEDD5), secondaryContainer = Color(0xFFFEF08A))

        "VOLCANIC_EMBER" -> if (darkTheme) baseDark.copy(primary = Color(0xFFF87171), secondary = Color(0xFFFB923C), primaryContainer = Color(0xFF450A0A), secondaryContainer = Color(0xFF431407))
                            else baseLight.copy(primary = Color(0xFFDC2626), secondary = Color(0xFFEA580C), primaryContainer = Color(0xFFFEE2E2), secondaryContainer = Color(0xFFFFEDD5))

        "MATCHA_ZEN" -> if (darkTheme) baseDark.copy(primary = Color(0xFF4ADE80), secondary = Color(0xFFA3E635), primaryContainer = Color(0xFF052E16), secondaryContainer = Color(0xFF1A2E05))
                        else baseLight.copy(primary = Color(0xFF16A34A), secondary = Color(0xFF65A30D), primaryContainer = Color(0xFFDCFCE7), secondaryContainer = Color(0xFFECFCCB))

        "DEEP_SPACE" -> if (darkTheme) baseDark.copy(primary = Color(0xFF818CF8), secondary = Color(0xFFFB7185), primaryContainer = Color(0xFF1E1B4B), secondaryContainer = Color(0xFF4C0519))
                        else baseLight.copy(primary = Color(0xFF4F46E5), secondary = Color(0xFFE11D48), primaryContainer = Color(0xFFEEF2FF), secondaryContainer = Color(0xFFFFE4E6))

        "AMETHYST_LUX" -> if (darkTheme) baseDark.copy(primary = Color(0xFFC084FC), secondary = Color(0xFFFBBF24), primaryContainer = Color(0xFF3B0764), secondaryContainer = Color(0xFF451A03))
                          else baseLight.copy(primary = Color(0xFF9333EA), secondary = Color(0xFFD97706), primaryContainer = Color(0xFFF3E8FF), secondaryContainer = Color(0xFFFEF3C7))

        "MONOCHROME_STEEL" -> if (darkTheme) baseDark.copy(primary = Color(0xFF94A3B8), secondary = Color(0xFFCBD5E1), primaryContainer = Color(0xFF1E293B), secondaryContainer = Color(0xFF334155))
                              else baseLight.copy(primary = Color(0xFF475569), secondary = Color(0xFF334155), primaryContainer = Color(0xFFF1F5F9), secondaryContainer = Color(0xFFE2E8F0))

        else -> if (darkTheme) baseDark else baseLight
    }
}

/**
 * Ambient atmospheric sky and refractive glow backdrop rendered behind content when glassmorphism is active.
 * Replicates IMG_20261019_225432.jpg in dark mode and IMG_20261019_225712.jpg in light mode.
 */
@Composable
fun GlassmorphicBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isGlass = LocalGlassmorphismEnabled.current
    val vfx = LocalGlassVfxConfig.current
    val isDark = LocalIsDarkTheme.current

    val defaultContentColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    if (!isGlass) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides defaultContentColor
        ) {
            Box(modifier = modifier) {
                content()
            }
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glass_aurora")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val (glowPrimary, glowSecondary) = vfx.resolveGlowColors(
        themePrimary = MaterialTheme.colorScheme.primary,
        themeSecondary = MaterialTheme.colorScheme.secondary,
        isDark = isDark
    )
    val themeBg = MaterialTheme.colorScheme.background
    val bloomBoost = (0.50f + vfx.highlightBloom * 0.72f).coerceIn(0.40f, 1.30f)
    val magentaAccent = lerp(glowSecondary, Color(0xFFE11D48), 0.45f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isDark) {
                    // Ultra-deep obsidian cosmic backdrop so luminous frosted glass cards pop dramatically
                    Brush.verticalGradient(
                        0.0f to Color(0xFF02040A),
                        0.28f to Color(0xFF030710),
                        0.65f to Color(0xFF03050C),
                        1.0f to Color(0xFF020308)
                    )
                } else {
                    Brush.verticalGradient(
                        0.0f to Color(0xFFF6FAFF),
                        0.35f to Color.White,
                        0.70f to Color(0xFFF3F7FE),
                        1.0f to themeBg
                    )
                }
            )
    ) {
        if (vfx.auroraBackdrop) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height

                if (isDark) {
                    // 1. Primary Hero Upper-Right Teal/Cyan Studio Aurora illuminating Hero Glass Card from behind
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowPrimary.copy(alpha = (0.44f * bloomBoost * pulseAlpha).coerceAtMost(0.68f)),
                                glowSecondary.copy(alpha = (0.22f * bloomBoost * pulseAlpha).coerceAtMost(0.44f)),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.74f, height * 0.14f),
                            radius = width * 0.72f
                        )
                    )

                    // 2. Mid-Left Crimson/Magenta Nebula Whisper along edge
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                magentaAccent.copy(alpha = (0.24f * bloomBoost * pulseAlpha).coerceAtMost(0.40f)),
                                glowSecondary.copy(alpha = (0.10f * bloomBoost * pulseAlpha).coerceAtMost(0.22f)),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.02f, height * 0.38f),
                            radius = width * 0.56f
                        )
                    )

                    // 3. Lower-Right Cosmic Teal-Indigo Bloom behind habit cards & floating dock
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowPrimary.copy(alpha = (0.34f * bloomBoost * pulseAlpha).coerceAtMost(0.56f)),
                                glowSecondary.copy(alpha = (0.18f * bloomBoost * pulseAlpha).coerceAtMost(0.36f)),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.80f, height * 0.78f),
                            radius = width * 0.76f
                        )
                    )
                } else {
                    // Light mode: crisp porcelain canvas with soft luminous pastel nebula clouds
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowPrimary.copy(alpha = (0.26f * bloomBoost * pulseAlpha).coerceAtMost(0.44f)),
                                glowSecondary.copy(alpha = (0.14f * bloomBoost * pulseAlpha).coerceAtMost(0.28f)),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.78f, height * 0.12f),
                            radius = width * 0.68f
                        )
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                magentaAccent.copy(alpha = (0.16f * bloomBoost * pulseAlpha).coerceAtMost(0.30f)),
                                glowSecondary.copy(alpha = (0.10f * bloomBoost * pulseAlpha).coerceAtMost(0.22f)),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.06f, height * 0.42f),
                            radius = width * 0.58f
                        )
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowPrimary.copy(alpha = (0.22f * bloomBoost * pulseAlpha).coerceAtMost(0.40f)),
                                glowSecondary.copy(alpha = (0.14f * bloomBoost * pulseAlpha).coerceAtMost(0.28f)),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.84f, height * 0.84f),
                            radius = width * 0.72f
                        )
                    )
                }
            }
        }

        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides defaultContentColor
        ) {
            content()
        }
    }
}

/**
 * Adds a glowing pulsing aura around components.
 */
@Composable
fun Modifier.pulsingGlow(
    glowColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    return this
        .drawWithContent {
            drawContent()
            drawRoundRect(
                color = glowColor.copy(alpha = alpha),
                style = Stroke(width = 1.8.dp.toPx()),
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
            )
        }
}

/**
 * Card colors that provide pristine frosted translucent glass tint matching reference pictures.
 * In Glassmorphism mode, the Card containerColor is set to Transparent so that the multi-layer
 * convex lens shaders, specular light reflection, and radiant neon borders in Modifier.glassmorphicCard
 * are rendered with ultra-high-definition clarity and depth without being occluded by opaque surface tints.
 */
@Composable
fun glassmorphicCardColors(
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color? = null
): CardColors {
    val isGlass = LocalGlassmorphismEnabled.current
    val isDark = LocalIsDarkTheme.current
    val resolvedContentColor = contentColor 
        ?: if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    if (!isGlass) {
        return CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = resolvedContentColor
        )
    }

    return CardDefaults.cardColors(
        containerColor = Color.Transparent,
        contentColor = resolvedContentColor
    )
}

/**
 * Standard card elevation.
 */
@Composable
fun glassmorphicCardElevation(
    defaultElevation: Dp = 0.dp
): CardElevation {
    val isGlass = LocalGlassmorphismEnabled.current
    val isDark = LocalIsDarkTheme.current
    val effectiveElevation = if (isGlass) 0.dp else {
        if (defaultElevation == 0.dp) (if (isDark) 3.dp else 4.dp) else defaultElevation
    }
    return CardDefaults.cardElevation(defaultElevation = effectiveElevation)
}

/**
 * Returns the radiant glowing border stroke matching the exact neon aqua / azure highlight in reference images.
 * In non-glass mode, returns a crisp, defined outline so cards are always clearly delineated.
 */
@Composable
fun glassmorphicCardBorder(
    borderWidth: Dp = 1.2.dp
): BorderStroke? {
    val isGlass = LocalGlassmorphismEnabled.current
    val vfx = LocalGlassVfxConfig.current
    val isDark = LocalIsDarkTheme.current

    if (!isGlass) {
        return BorderStroke(
            borderWidth,
            if (isDark) Color(0xFF334155).copy(alpha = 0.70f) else Color(0xFF94A3B8).copy(alpha = 0.65f)
        )
    }

    val (glowPrimary, glowSecondary) = vfx.resolveGlowColors(
        themePrimary = MaterialTheme.colorScheme.primary,
        themeSecondary = MaterialTheme.colorScheme.secondary,
        isDark = isDark
    )
    val depthAlpha = (0.35f + vfx.reflectionDepth * 0.65f).coerceIn(0.30f, 1.0f)
    val effectiveWidth = (borderWidth.value * (0.75f + vfx.reflectionDepth * 0.55f)).dp

    val topChromeHighlight = lerp(glowPrimary, Color.White, 0.58f)
    val bottomChromeHighlight = lerp(glowSecondary, Color.White, 0.42f)

    val glowingBorderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                topChromeHighlight.copy(alpha = (0.98f * depthAlpha).coerceIn(0.30f, 1f)),
                glowPrimary.copy(alpha = (0.85f * depthAlpha).coerceIn(0.24f, 1f)),
                glowSecondary.copy(alpha = (0.48f * depthAlpha).coerceIn(0.16f, 0.85f)),
                bottomChromeHighlight.copy(alpha = (0.90f * depthAlpha).coerceIn(0.28f, 1f))
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                glowPrimary.copy(alpha = (0.92f * depthAlpha).coerceIn(0.28f, 1f)),
                glowSecondary.copy(alpha = (0.86f * depthAlpha).coerceIn(0.25f, 1f)),
                glowPrimary.copy(alpha = (0.78f * depthAlpha).coerceIn(0.22f, 1f)),
                glowSecondary.copy(alpha = (0.90f * depthAlpha).coerceIn(0.28f, 1f))
            )
        )
    }

    return BorderStroke(effectiveWidth, glowingBorderBrush)
}

/**
 * Modifier to apply Ultra HD convex lens glow effect, specular gloss highlight, and radiant neon glass border.
 * Driven dynamically by [LocalGlassVfxConfig]. In non-glass mode, applies crisp material elevation and border outline.
 */
@Composable
fun Modifier.glassmorphicCard(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.6.dp
): Modifier {
    val isGlass = LocalGlassmorphismEnabled.current
    val vfx = LocalGlassVfxConfig.current
    val isDark = LocalIsDarkTheme.current

    if (!isGlass) {
        return this
            .shadow(
                elevation = if (isDark) 3.dp else 4.dp,
                shape = shape,
                clip = false,
                ambientColor = if (isDark) Color(0x30000000) else Color(0x350F172A),
                spotColor = if (isDark) Color(0x40000000) else Color(0x400F172A)
            )
            .border(
                1.2.dp,
                if (isDark) Color(0xFF334155).copy(alpha = 0.70f) else Color(0xFF94A3B8).copy(alpha = 0.65f),
                shape
            )
    }

    val (glowPrimary, glowSecondary) = vfx.resolveGlowColors(
        themePrimary = MaterialTheme.colorScheme.primary,
        themeSecondary = MaterialTheme.colorScheme.secondary,
        isDark = isDark
    )
    val depthAlpha = (0.35f + vfx.reflectionDepth * 0.65f).coerceIn(0.30f, 1.0f)
    val bloomFactor = (0.30f + vfx.highlightBloom * 0.90f).coerceIn(0.25f, 1.25f)
    val curvatureFactor = (0.25f + vfx.lensCurvature * 0.85f).coerceIn(0.20f, 1.15f)
    val opacityScale = (0.42f + vfx.frostedTranslucency * 0.58f).coerceIn(0.36f, 1.0f)
    val effectiveBorderWidth = (borderWidth.value * (0.75f + vfx.reflectionDepth * 0.55f)).dp

    val topChromeHighlight = lerp(glowPrimary, Color.White, 0.58f)
    val bottomChromeHighlight = lerp(glowSecondary, Color.White, 0.42f)

    val glowingBorderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                topChromeHighlight.copy(alpha = (0.98f * depthAlpha).coerceIn(0.30f, 1f)),
                glowPrimary.copy(alpha = (0.86f * depthAlpha).coerceIn(0.24f, 1f)),
                glowSecondary.copy(alpha = (0.50f * depthAlpha).coerceIn(0.16f, 0.88f)),
                bottomChromeHighlight.copy(alpha = (0.90f * depthAlpha).coerceIn(0.28f, 1f))
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                glowPrimary.copy(alpha = (0.92f * depthAlpha).coerceIn(0.28f, 1f)),
                glowSecondary.copy(alpha = (0.88f * depthAlpha).coerceIn(0.25f, 1f)),
                glowPrimary.copy(alpha = (0.84f * depthAlpha).coerceIn(0.22f, 1f)),
                glowSecondary.copy(alpha = (0.90f * depthAlpha).coerceIn(0.28f, 1f))
            )
        )
    }

    // Smoky obsidian-crystal frosted gradient letting background aurora shine through cleanly
    val glassBackground = if (isDark) {
        Brush.verticalGradient(
            0.0f to lerp(Color(0xFF172638), glowPrimary, 0.14f).copy(alpha = (0.64f * opacityScale).coerceIn(0.26f, 0.86f)),
            0.30f to Color(0xFF0D1726).copy(alpha = (0.56f * opacityScale).coerceIn(0.22f, 0.82f)),
            0.70f to Color(0xFF080E19).copy(alpha = (0.60f * opacityScale).coerceIn(0.24f, 0.84f)),
            1.0f to Color(0xFF050A12).copy(alpha = (0.74f * opacityScale).coerceIn(0.32f, 0.90f))
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = (0.96f * opacityScale).coerceIn(0.66f, 0.99f)),
            0.30f to Color(0xFFF3F7FD).copy(alpha = (0.90f * opacityScale).coerceIn(0.60f, 0.95f)),
            0.70f to Color(0xFFE7EFFA).copy(alpha = (0.88f * opacityScale).coerceIn(0.58f, 0.93f)),
            1.0f to Color(0xFFD8E4F3).copy(alpha = (0.92f * opacityScale).coerceIn(0.62f, 0.95f))
        )
    }

    val specularPeak = ((if (isDark) 0.28f else 0.56f) * bloomFactor).coerceIn(0.08f, 0.55f)
    val specularMid = ((if (isDark) 0.13f else 0.24f) * bloomFactor).coerceIn(0.04f, 0.34f)
    val curvatureStopMid = (0.14f + 0.14f * curvatureFactor).coerceIn(0.10f, 0.34f)
    val curvatureStopEnd = (0.44f + 0.28f * curvatureFactor).coerceIn(0.34f, 0.80f)

    val topSpecularSheen = if (vfx.specularGloss) {
        Brush.verticalGradient(
            0.0f to lerp(Color.White, glowPrimary, 0.22f).copy(alpha = specularPeak),
            curvatureStopMid to Color.White.copy(alpha = specularMid),
            (curvatureStopEnd * 0.65f) to glowPrimary.copy(alpha = (0.12f * bloomFactor).coerceIn(0.02f, 0.24f)),
            curvatureStopEnd to Color.Transparent,
            1.0f to Color.Transparent
        )
    } else {
        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val bottomCausticSheen = if (vfx.causticGlow) {
        Brush.verticalGradient(
            0.0f to Color.Transparent,
            (0.75f - 0.10f * vfx.reflectionDepth).coerceIn(0.55f, 0.84f) to Color.Transparent,
            1.0f to lerp(glowPrimary, glowSecondary, 0.5f).copy(alpha = ((if (isDark) 0.22f else 0.24f) * bloomFactor * depthAlpha).coerceIn(0.04f, 0.42f))
        )
    } else {
        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    return this
        .shadow(
            elevation = (6f + 8f * vfx.reflectionDepth).dp,
            shape = shape,
            clip = false,
            ambientColor = if (isDark) glowSecondary.copy(alpha = 0.65f) else Color(0x6064748B),
            spotColor = if (isDark) glowPrimary.copy(alpha = 0.88f) else glowPrimary.copy(alpha = 0.45f)
        )
        .clip(shape)
        .background(glassBackground)
        .background(topSpecularSheen)
        .background(bottomCausticSheen)
        .drawWithContent {
            drawContent()
            if (vfx.specularGloss && vfx.lensCurvature > 0.08f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (0.18f * vfx.lensCurvature * bloomFactor).coerceIn(0.02f, 0.26f)),
                            glowPrimary.copy(alpha = (0.08f * vfx.lensCurvature * bloomFactor).coerceIn(0.01f, 0.16f)),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.46f, size.height * 0.10f),
                        radius = size.width * (0.48f + 0.34f * vfx.lensCurvature)
                    ),
                    center = Offset(size.width * 0.46f, size.height * 0.10f),
                    radius = size.width * (0.48f + 0.34f * vfx.lensCurvature)
                )
            }
            if (vfx.reflectionDepth > 0.1f) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = (0.58f * vfx.reflectionDepth).coerceIn(0.08f, 0.78f)),
                            Color.Transparent
                        )
                    ),
                    start = Offset(size.width * 0.10f, 1.5f),
                    end = Offset(size.width * 0.90f, 1.5f),
                    strokeWidth = (1.1f + 1.3f * vfx.reflectionDepth).dp.toPx()
                )
            }
        }
        .border(effectiveBorderWidth, glowingBorderBrush, shape)
}

@Composable
fun HabitStreaksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    dynamicRainbowEnabled: Boolean = false,
    themeTemplate: String = "INDIGO",
    glassmorphismEnabled: Boolean = false,
    glassVfxConfig: GlassVfxConfig = GlassVfxConfig(enabled = glassmorphismEnabled),
    content: @Composable () -> Unit
) {
    val rawColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // If device returns default unconfigured Monet purple (common in emulators without wallpaper personalization),
            // provide the distinctive Material You Ocean/Sky dynamic palette so the toggle visibly shines!
            val isDefaultAospMonet = dynamicScheme.primary.value == Color(0xFF6750A4).value || dynamicScheme.primary.value == Color(0xFFD0BCFF).value
            val baseDynamic = if (isDefaultAospMonet) {
                if (darkTheme) {
                    DarkColorScheme.copy(
                        primary = Color(0xFF38BDF8), // Dynamic Sky
                        secondary = Color(0xFF818CF8),
                        tertiary = Color(0xFF34D399),
                        primaryContainer = Color(0xFF0C4A6E),
                        secondaryContainer = Color(0xFF1E1B4B)
                    )
                } else {
                    LightColorScheme.copy(
                        primary = Color(0xFF0284C7), // Dynamic Sky
                        secondary = Color(0xFF6366F1),
                        tertiary = Color(0xFF059669),
                        primaryContainer = Color(0xFFE0F2FE),
                        secondaryContainer = Color(0xFFEEF2FF)
                    )
                }
            } else {
                dynamicScheme
            }

            if (dynamicRainbowEnabled) {
                // If both dynamic colors and dynamic rainbow auto transitions are turned on,
                // the UI transforms in device wallpaper themes fetched by dynamic colors feature,
                // cooperating with template themes with great perfection, accuracy, and smoothness.
                when (themeTemplate) {
                    "DYNAMIC_WALLPAPER_PRIMARY" -> baseDynamic
                    "DYNAMIC_WALLPAPER_HARMONY" -> baseDynamic.copy(
                        primary = baseDynamic.secondary,
                        secondary = baseDynamic.tertiary,
                        primaryContainer = baseDynamic.secondaryContainer,
                        secondaryContainer = baseDynamic.primaryContainer
                    )
                    "DYNAMIC_WALLPAPER_VIBRANT" -> baseDynamic.copy(
                        primary = baseDynamic.tertiary,
                        secondary = baseDynamic.primary,
                        primaryContainer = baseDynamic.primaryContainer,
                        secondaryContainer = baseDynamic.secondaryContainer
                    )
                    else -> {
                        val templateScheme = getThemeColorScheme(themeTemplate, darkTheme)
                        baseDynamic.copy(
                            primary = templateScheme.primary,
                            secondary = templateScheme.secondary,
                            tertiary = templateScheme.tertiary,
                            primaryContainer = templateScheme.primaryContainer,
                            secondaryContainer = templateScheme.secondaryContainer
                        )
                    }
                }
            } else {
                baseDynamic
            }
        }
        dynamicColor -> {
            // Android < 12 Material You adaptive dynamic palette
            val baseDynamic = if (darkTheme) {
                DarkColorScheme.copy(
                    primary = Color(0xFF38BDF8),
                    secondary = Color(0xFF818CF8),
                    tertiary = Color(0xFF34D399),
                    primaryContainer = Color(0xFF0C4A6E),
                    secondaryContainer = Color(0xFF1E1B4B)
                )
            } else {
                LightColorScheme.copy(
                    primary = Color(0xFF0284C7),
                    secondary = Color(0xFF6366F1),
                    tertiary = Color(0xFF059669),
                    primaryContainer = Color(0xFFE0F2FE),
                    secondaryContainer = Color(0xFFEEF2FF)
                )
            }
            if (dynamicRainbowEnabled) {
                when (themeTemplate) {
                    "DYNAMIC_WALLPAPER_PRIMARY" -> baseDynamic
                    "DYNAMIC_WALLPAPER_HARMONY" -> baseDynamic.copy(
                        primary = baseDynamic.secondary,
                        secondary = baseDynamic.tertiary,
                        primaryContainer = baseDynamic.secondaryContainer,
                        secondaryContainer = baseDynamic.primaryContainer
                    )
                    "DYNAMIC_WALLPAPER_VIBRANT" -> baseDynamic.copy(
                        primary = baseDynamic.tertiary,
                        secondary = baseDynamic.primary,
                        primaryContainer = baseDynamic.primaryContainer,
                        secondaryContainer = baseDynamic.secondaryContainer
                    )
                    else -> {
                        val templateScheme = getThemeColorScheme(themeTemplate, darkTheme)
                        baseDynamic.copy(
                            primary = templateScheme.primary,
                            secondary = templateScheme.secondary,
                            tertiary = templateScheme.tertiary,
                            primaryContainer = templateScheme.primaryContainer,
                            secondaryContainer = templateScheme.secondaryContainer
                        )
                    }
                }
            } else {
                baseDynamic
            }
        }
        else -> getThemeColorScheme(themeTemplate, darkTheme)
    }

    val animatedPrimary by animateColorAsState(
        targetValue = rawColorScheme.primary,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "anim_primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = rawColorScheme.secondary,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "anim_secondary"
    )
    val animatedTertiary by animateColorAsState(
        targetValue = rawColorScheme.tertiary,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "anim_tertiary"
    )
    val animatedBackground by animateColorAsState(
        targetValue = rawColorScheme.background,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "anim_background"
    )
    val animatedSurface by animateColorAsState(
        targetValue = rawColorScheme.surface,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "anim_surface"
    )
    val animatedSurfaceVariant by animateColorAsState(
        targetValue = rawColorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "anim_surface_variant"
    )
    val animatedPrimaryContainer by animateColorAsState(
        targetValue = rawColorScheme.primaryContainer,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "anim_primary_container"
    )
    val animatedSecondaryContainer by animateColorAsState(
        targetValue = rawColorScheme.secondaryContainer,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "anim_secondary_container"
    )

    val finalColorScheme = if (darkTheme) {
        rawColorScheme.copy(
            primary = animatedPrimary,
            secondary = animatedSecondary,
            tertiary = animatedTertiary,
            background = animatedBackground,
            surface = animatedSurface,
            surfaceVariant = animatedSurfaceVariant,
            primaryContainer = animatedPrimaryContainer,
            secondaryContainer = animatedSecondaryContainer,
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFFCBD5E1),
            onPrimaryContainer = Color(0xFFE0E7FF),
            onSecondaryContainer = Color(0xFFCFFAFE)
        )
    } else {
        rawColorScheme.copy(
            primary = animatedPrimary,
            secondary = animatedSecondary,
            tertiary = animatedTertiary,
            background = animatedBackground,
            surface = animatedSurface,
            surfaceVariant = animatedSurfaceVariant,
            primaryContainer = animatedPrimaryContainer,
            secondaryContainer = animatedSecondaryContainer,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF334155),
            onPrimaryContainer = Color(0xFF1E1B4B),
            onSecondaryContainer = Color(0xFF083344)
        )
    }

    val defaultContentColor = if (darkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    val effectiveVfx = glassVfxConfig.copy(enabled = glassmorphismEnabled).clamped()

    CompositionLocalProvider(
        LocalGlassmorphismEnabled provides glassmorphismEnabled,
        LocalGlassVfxConfig provides effectiveVfx,
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = Typography
        ) {
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides defaultContentColor,
                content = content
            )
        }
    }
}
