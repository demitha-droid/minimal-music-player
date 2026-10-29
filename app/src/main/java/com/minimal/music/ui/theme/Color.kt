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

// --- Obsidian Flat Monochrome Palette ---
val DarkColors = CustomColors(
    background = Color(0xFF0E0E10),
    surface = Color(0xFF161618),
    surfaceElevated = Color(0xFF1E1E22),
    surfaceBorder = Color(0xFF2C2C30),
    primary = Color(0xFFFFFFFF),         // Monochrome pure white highlight
    primaryAccent = Color(0xFFE5E5EA),
    secondary = Color(0xFF8E8E93),       // Neutral gray for monochrome icons
    textPrimary = Color(0xFFF7F7F8),
    textSecondary = Color(0xFF8E8E93),
    textMuted = Color(0xFF636366),
    heartActive = Color(0xFFFFFFFF),     // Flat monochrome active heart
    equalizerGreen = Color(0xFFD1D1D6),  // Flat monochrome equalizer
    glassSurface = Color(0xE6161618),
    isLight = false
)

// --- AMOLED Flat Monochrome Palette ---
val AmoledColors = CustomColors(
    background = Color(0xFF000000),      // True 100% flat black
    surface = Color(0xFF0A0A0A),         // Pure deep tone
    surfaceElevated = Color(0xFF141414), // Subtle flat card
    surfaceBorder = Color(0xFF242424),   // Crisp flat outline
    primary = Color(0xFFFFFFFF),         // Monochrome pure white highlight
    primaryAccent = Color(0xFFE5E5EA),
    secondary = Color(0xFF8E8E93),       // Neutral gray for monochrome icons
    textPrimary = Color(0xFFFFFFFF),     // Pure white for highest contrast
    textSecondary = Color(0xFF8E8E93),
    textMuted = Color(0xFF636366),
    heartActive = Color(0xFFFFFFFF),     // Flat monochrome active heart
    equalizerGreen = Color(0xFFFFFFFF),  // Flat monochrome equalizer
    glassSurface = Color(0xE6000000),
    isLight = false
)

// --- Minimal Light Flat Monochrome Palette ---
val LightColors = CustomColors(
    background = Color(0xFFF5F5F7),      // Clean flat light background
    surface = Color(0xFFFFFFFF),         // Pure white cards
    surfaceElevated = Color(0xFFE5E5EA), // Soft flat secondary surface
    surfaceBorder = Color(0xFFD1D1D6),   // Subtle flat border outline
    primary = Color(0xFF000000),         // Monochrome pure black highlight
    primaryAccent = Color(0xFF1C1C1E),
    secondary = Color(0xFF636366),       // Neutral gray for monochrome icons
    textPrimary = Color(0xFF111827),     // Deep charcoal
    textSecondary = Color(0xFF636366),
    textMuted = Color(0xFF8E8E93),
    heartActive = Color(0xFF000000),     // Flat monochrome active heart
    equalizerGreen = Color(0xFF1C1C1E),  // Flat monochrome equalizer
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
val IconGray: Color @Composable get() = LocalCustomColors.current.secondary
