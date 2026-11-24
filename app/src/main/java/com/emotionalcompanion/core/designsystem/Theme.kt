package com.emotionalcompanion.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.emotionalcompanion.data.settings.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Orchid,
    onPrimary = Midnight,
    primaryContainer = RoyalPurple,
    onPrimaryContainer = Pearl,
    secondary = SoftRose,
    onSecondary = Midnight,
    secondaryContainer = DeepSurfaceHigh,
    onSecondaryContainer = Pearl,
    tertiary = RoyalPurple,
    onTertiary = Pearl,
    background = Midnight,
    onBackground = Pearl,
    surface = Midnight,
    onSurface = Pearl,
    surfaceVariant = DeepSurface,
    onSurfaceVariant = PearlMuted,
    surfaceContainerLowest = Midnight,
    surfaceContainerLow = DeepSurface,
    surfaceContainer = DeepSurface,
    surfaceContainerHigh = DeepSurfaceHigh,
    surfaceContainerHighest = DeepSurfaceHigh,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorRose,
)

private val LightColors = lightColorScheme(
    primary = RoyalPurple,
    onPrimary = Pearl,
    primaryContainer = Pearl,
    onPrimaryContainer = InkPurple,
    secondary = Orchid,
    onSecondary = InkPurple,
    tertiary = SoftRose,
    onTertiary = InkPurple,
    background = LightBackground,
    onBackground = InkPurple,
    surface = LightBackground,
    onSurface = InkPurple,
    surfaceVariant = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceHigh,
)

/** True when the user turned on "Reduce motion" in Settings. Animations must check it. */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun EmotionalCompanionTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }
    EmotionalCompanionTheme(darkTheme = dark, content = content)
}

/** Dynamic color is intentionally not used, to keep the brand palette. */
@Composable
fun EmotionalCompanionTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
