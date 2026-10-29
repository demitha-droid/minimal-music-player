package com.minimal.music.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CustomColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceBorder: Color,
    val primary: Color,
    val primaryAccent: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val heartActive: Color,
    val equalizerGreen: Color,
    val glassSurface: Color,
    val isLight: Boolean = false
)

// --- Obsidian Dark Palette ---
val DarkColors = CustomColors(
    background = Color(0xFF0C0C0E),
    surface = Color(0xFF141418),
    surfaceElevated = Color(0xFF1D1D24),
    surfaceBorder = Color(0xFF282834),
    primary = Color(0xFFA076F9),
    primaryAccent = Color(0xFF7C4DFF),
    secondary = Color(0xFF65D8FF),
    textPrimary = Color(0xFFF7F7F8),
    textSecondary = Color(0xFF9494A6),
    textMuted = Color(0xFF5E5E70),
    heartActive = Color(0xFFFF4B6E),
    equalizerGreen = Color(0xFF00E676),
    glassSurface = Color(0xCC141418),
    isLight = false
)

// --- AMOLED Pure Black Palette ---
val AmoledColors = CustomColors(
    background = Color(0xFF000000),      // True 100% black for AMOLED
    surface = Color(0xFF080808),         // Pure deep tone
    surfaceElevated = Color(0xFF121212), // Subtle elevated card
    surfaceBorder = Color(0xFF222222),   // Crisp contrast outline
    primary = Color(0xFFA78BFA),         // Bright vivid violet
    primaryAccent = Color(0xFF8B5CF6),
    secondary = Color(0xFF38BDF8),       // Electric cyan
    textPrimary = Color(0xFFFFFFFF),     // Pure white for highest contrast
    textSecondary = Color(0xFFA1A1AA),
    textMuted = Color(0xFF71717A),
    heartActive = Color(0xFFFF3366),
    equalizerGreen = Color(0xFF00E676),
    glassSurface = Color(0xE6000000),
    isLight = false
)

// --- Minimal Light Palette ---
val LightColors = CustomColors(
    background = Color(0xFFF8F9FA),      // Clean ivory-tinted white
    surface = Color(0xFFFFFFFF),         // Crisp pure white cards
    surfaceElevated = Color(0xFFF1F3F5), // Soft elevated background
    surfaceBorder = Color(0xFFE5E7EB),   // Light border outline
    primary = Color(0xFF7C3AED),         // Deep royal violet
    primaryAccent = Color(0xFF6D28D9),
    secondary = Color(0xFF0284C7),       // Clean sky blue
    textPrimary = Color(0xFF111827),     // Deep charcoal/black
    textSecondary = Color(0xFF4B5563),   // Slate grey
    textMuted = Color(0xFF9CA3AF),       // Muted slate
    heartActive = Color(0xFFE11D48),
    equalizerGreen = Color(0xFF059669),
    glassSurface = Color(0xEEFFFFFF),
    isLight = true
)

val LocalCustomColors = staticCompositionLocalOf { DarkColors }

// Top-level dynamic getters so every screen/component automatically updates with theme changes
val BgDark: Color @Composable get() = LocalCustomColors.current.background
val SurfaceDark: Color @Composable get() = LocalCustomColors.current.surface
val SurfaceElevated: Color @Composable get() = LocalCustomColors.current.surfaceElevated
val SurfaceBorder: Color @Composable get() = LocalCustomColors.current.surfaceBorder

val PrimaryViolet: Color @Composable get() = LocalCustomColors.current.primary
val PrimaryAccent: Color @Composable get() = LocalCustomColors.current.primaryAccent
val SecondaryCyan: Color @Composable get() = LocalCustomColors.current.secondary

val TextPrimary: Color @Composable get() = LocalCustomColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalCustomColors.current.textSecondary
val TextMuted: Color @Composable get() = LocalCustomColors.current.textMuted

val HeartActive: Color @Composable get() = LocalCustomColors.current.heartActive
val EqualizerGreen: Color @Composable get() = LocalCustomColors.current.equalizerGreen
val GlassSurface: Color @Composable get() = LocalCustomColors.current.glassSurface
