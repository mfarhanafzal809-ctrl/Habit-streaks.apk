package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Premium Brand Core Colors - Highly Professional & Modern
val PrimaryIndigo = Color(0xFF4F46E5) // Sleek, high-trust Indigo
val PrimaryIndigoLight = Color(0xFF818CF8)
val SecondaryCyan = Color(0xFF06B6D4) // Vibrant Cyan accent
val TertiaryEmerald = Color(0xFF10B981) // Success/Growth Emerald

// Elegant Dark Palette (Deep Slate / OLED friendly)
val DarkBg = Color(0xFF0B0F19) // Very deep slate/black
val DarkSurface = Color(0xFF111827) // Elevated slate
val DarkSurfaceVariant = Color(0xFF1F2937)
val DarkCard = Color(0xFF111827)
val DarkBorder = Color(0xFF374151)

// Clean Light Palette (Minimalist, Apple/Linear inspired)
val LightBg = Color(0xFFF8FAFC) // Very light slate
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightCard = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE2E8F0)

// Accent Category Colors for Habits
val HabitCoral = Color(0xFFF43F5E)
val HabitEmerald = Color(0xFF10B981)
val HabitSky = Color(0xFF0EA5E9)
val HabitIndigo = Color(0xFF6366F1)
val HabitViolet = Color(0xFF8B5CF6)
val HabitAmber = Color(0xFFF59E0B)

// Premium Gradients
val PremiumGradientLight = Brush.linearGradient(
    colors = listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))
)
val PremiumGradientDark = Brush.linearGradient(
    colors = listOf(Color(0xFF818CF8), Color(0xFF22D3EE))
)
val CardGlowGradient = Brush.verticalGradient(
    colors = listOf(Color(0x1A4F46E5), Color(0x004F46E5))
)

// Glassmorphism (Frosted Glass) Styling Tokens
val GlassDarkSurface = Color(0x401E293B) // 25% opacity slate
val GlassDarkSurfaceElevated = Color(0x55334155) // 33% opacity
val GlassDarkBorder = Color(0x2EFFFFFF) // 18% white rim
val GlassLightSurface = Color(0xD9FFFFFF) // 85% translucent white
val GlassLightSurfaceElevated = Color(0xEEF8FAFC)
val GlassLightBorder = Color(0x66FFFFFF) // 40% white edge highlight

// Glowing Streak Fire Colors
val StreakFlameOrange = Color(0xFFFF5722)
val StreakFlameViolet = Color(0xFF8B5CF6)
val StreakFlameGold = Color(0xFFFFB300)
val StreakFlameDiamond = Color(0xFF00E5FF)
val StreakFlameRose = Color(0xFFFF1744)

val FieryStreakGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFF5722), Color(0xFFFFB300))
)
val LegendStreakGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFFFD700))
)
val GlassAmbientGradientDark = Brush.radialGradient(
    colors = listOf(Color(0x264F46E5), Color(0x000B0F19)),
    radius = 1200f
)
val GlassAmbientGradientLight = Brush.radialGradient(
    colors = listOf(Color(0x1F818CF8), Color(0x00F8FAFC)),
    radius = 1200f
)

