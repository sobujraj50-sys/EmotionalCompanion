package com.emotionalcompanion.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Draws recent real audio levels (0..1), newest on the right.
 * With no levels (microphone and playback off) it draws a flat, dim baseline.
 * It never generates its own motion.
 */
@Composable
fun AmplitudeWaveform(
    levels: List<Float>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    barCount: Int = 36,
) {
    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val slot = size.width / barCount
        val barWidth = slot * 0.5f
        val minHeight = 3.dp.toPx()
        val recent = levels.takeLast(barCount)
        val offset = barCount - recent.size
        for (i in 0 until barCount) {
            val level = if (i >= offset) recent[i - offset].coerceIn(0f, 1f) else 0f
            val h = (minHeight + level * (size.height - minHeight))
            val x = i * slot + (slot - barWidth) / 2f
            drawRoundRect(
                color = if (level > 0f) active else idle,
                topLeft = Offset(x, (size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
