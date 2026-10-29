package com.minimal.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkColors.primary,
    secondary = DarkColors.secondary,
    background = DarkColors.background,
    surface = DarkColors.surface,
    surfaceVariant = DarkColors.surfaceElevated,
    onPrimary = DarkColors.background,
    onSecondary = DarkColors.background,
    onBackground = DarkColors.textPrimary,
    onSurface = DarkColors.textPrimary,
    onSurfaceVariant = DarkColors.textSecondary,
    outline = DarkColors.surfaceBorder
)

private val AmoledColorScheme = darkColorScheme(
    primary = AmoledColors.primary,
    secondary = AmoledColors.secondary,
    background = AmoledColors.background,
    surface = AmoledColors.surface,
    surfaceVariant = AmoledColors.surfaceElevated,
    onPrimary = AmoledColors.background,
    onSecondary = AmoledColors.background,
    onBackground = AmoledColors.textPrimary,
    onSurface = AmoledColors.textPrimary,
    onSurfaceVariant = AmoledColors.textSecondary,
    outline = AmoledColors.surfaceBorder
)

private val LightColorScheme = lightColorScheme(
    primary = LightColors.primary,
    secondary = LightColors.secondary,
    background = LightColors.background,
    surface = LightColors.surface,
    surfaceVariant = LightColors.surfaceElevated,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightColors.textPrimary,
    onSurface = LightColors.textPrimary,
    onSurfaceVariant = LightColors.textSecondary,
    outline = LightColors.surfaceBorder
)

@Composable
fun MinimalMusicTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val customColors = when (themeMode) {
        ThemeMode.DARK -> DarkColors
        ThemeMode.AMOLED -> AmoledColors
        ThemeMode.LIGHT -> LightColors
    }

    val colorScheme = when (themeMode) {
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.AMOLED -> AmoledColorScheme
        ThemeMode.LIGHT -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MinimalTypography,
            content = content
        )
    }
}
