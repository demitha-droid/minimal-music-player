package com.minimal.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryViolet,
    secondary = SecondaryCyan,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceElevated,
    onPrimary = BgDark,
    onSecondary = BgDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder
)

@Composable
fun MinimalMusicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MinimalTypography,
        content = content
    )
}
