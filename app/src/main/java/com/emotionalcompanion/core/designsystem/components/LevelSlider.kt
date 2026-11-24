package com.emotionalcompanion.core.designsystem.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.emotionalcompanion.R
import com.emotionalcompanion.ai.personality.Level
import kotlin.math.roundToInt

@StringRes
fun Level.labelRes(): Int = when (this) {
    Level.VeryLow -> R.string.level_very_low
    Level.Low -> R.string.level_low
    Level.Medium -> R.string.level_medium
    Level.High -> R.string.level_high
    Level.VeryHigh -> R.string.level_very_high
}

/** Five-step slider for a personality or voice-delivery preference. */
@Composable
fun LevelSlider(
    @StringRes title: Int,
    level: Level,
    onChange: (Level) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(level.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = level.ordinal.toFloat(),
            onValueChange = { v ->
                val next = Level.entries[v.roundToInt().coerceIn(0, Level.entries.lastIndex)]
                if (next != level) onChange(next)
            },
            valueRange = 0f..Level.entries.lastIndex.toFloat(),
            steps = Level.entries.size - 2,
        )
    }
}
