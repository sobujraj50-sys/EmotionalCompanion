package com.emotionalcompanion.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Orchid,
    onPrimary = Midnight,
    secondary = SoftRose,
    onSecondary = Midnight,
    tertiary = RoyalPurple,
    onTertiary = Pearl,
    background = Midnight,
    onBackground = Pearl,
    surface = DeepSurface,
    onSurface = Pearl,
    onSurfaceVariant = PearlMuted,
)

private val LightColors = lightColorScheme(
    primary = RoyalPurple,
    onPrimary = Pearl,
    secondary = Orchid,
    onSecondary = InkPurple,
    tertiary = SoftRose,
    onTertiary = InkPurple,
    background = LightBackground,
    onBackground = InkPurple,
    surface = LightSurface,
    onSurface = InkPurple,
)

/**
 * Dark mode is the primary design. Light mode follows the system unless
 * [darkTheme] is overridden (a user setting arrives in Milestone B).
 * Dynamic color is intentionally not used, to keep the brand palette.
 */
@Composable
fun EmotionalCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
