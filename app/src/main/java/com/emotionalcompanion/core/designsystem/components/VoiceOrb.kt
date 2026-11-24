package com.emotionalcompanion.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.emotionalcompanion.core.designsystem.Orchid
import com.emotionalcompanion.core.designsystem.Pearl
import com.emotionalcompanion.core.designsystem.RoyalPurple
import com.emotionalcompanion.core.designsystem.SoftRose
import com.emotionalcompanion.domain.conversation.ConversationState

private data class OrbPalette(val highlight: Color, val core: Color, val edge: Color)

private fun paletteFor(state: ConversationState): OrbPalette = when (state) {
    ConversationState.Idle -> OrbPalette(SoftRose, Orchid, RoyalPurple)
    ConversationState.Connecting,
    ConversationState.Reconnecting -> OrbPalette(Pearl, Orchid, RoyalPurple)
    ConversationState.Listening -> OrbPalette(Pearl, SoftRose, Orchid)
    ConversationState.Thinking -> OrbPalette(Orchid, RoyalPurple, Color(0xFF3F2468))
    ConversationState.Speaking -> OrbPalette(Pearl, SoftRose, RoyalPurple)
    ConversationState.Interrupted -> OrbPalette(SoftRose, RoyalPurple, Color(0xFF3A2456))
    ConversationState.Offline -> OrbPalette(Color(0xFF9A90A6), Color(0xFF5D5568), Color(0xFF2E2A36))
    ConversationState.Error -> OrbPalette(Color(0xFFFFC2BA), Color(0xFFE5737A), Color(0xFF8C1D2B))
}

/**
 * The emotional voice orb.
 *
 * [state] must come from the real conversation state, and [amplitude] (0..1) from real
 * microphone or playback level. The orb never invents activity: in Idle it only breathes
 * slowly, and in Listening/Speaking its size follows [amplitude] alone.
 * With [reducedMotion], every looping animation stops and only color changes remain.
 */
@Composable
fun VoiceOrb(
    state: ConversationState,
    amplitude: Float,
    reducedMotion: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val palette = paletteFor(state)
    val highlight by animateColorAsState(palette.highlight, tween(500), label = "orbHighlight")
    val core by animateColorAsState(palette.core, tween(500), label = "orbCore")
    val edge by animateColorAsState(palette.edge, tween(500), label = "orbEdge")

    val transition = rememberInfiniteTransition(label = "orb")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "spin",
    )
    val level by animateFloatAsState(amplitude.coerceIn(0f, 1f), tween(90), label = "level")

    val stateScale = when (state) {
        ConversationState.Interrupted -> 0.9f
        ConversationState.Offline, ConversationState.Error -> 0.94f
        else -> 1f
    }
    val scale by animateFloatAsState(stateScale, tween(300), label = "stateScale")

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val baseRadius = size.minDimension / 2f * 0.70f
        val motion = if (reducedMotion) {
            1f
        } else {
            when (state) {
                ConversationState.Listening, ConversationState.Speaking -> 1f + 0.14f * level
                ConversationState.Idle -> 1f + 0.025f * breath
                ConversationState.Thinking -> 1f + 0.04f * breath
                else -> 1f
            }
        }
        val radius = baseRadius * scale * motion

        // Soft outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(core.copy(alpha = 0.38f), Color.Transparent),
                center = center,
                radius = radius * 1.42f,
            ),
            radius = radius * 1.42f,
        )
        // Orb body, lit slightly from the upper left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(highlight, core, edge),
                center = Offset(center.x - radius * 0.22f, center.y - radius * 0.26f),
                radius = radius * 1.25f,
            ),
            radius = radius,
        )

        val showsRing = state == ConversationState.Connecting ||
            state == ConversationState.Reconnecting ||
            state == ConversationState.Thinking
        if (showsRing) {
            val ringRadius = radius * 1.16f
            val strokePx = size.minDimension * 0.012f
            val sweep = if (state == ConversationState.Thinking) 70f else 110f
            drawArc(
                color = highlight.copy(alpha = 0.9f),
                startAngle = if (reducedMotion) -90f else spin,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = Size(ringRadius * 2, ringRadius * 2),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            if (state == ConversationState.Reconnecting) {
                drawArc(
                    color = highlight.copy(alpha = 0.5f),
                    startAngle = (if (reducedMotion) -90f else spin) + 180f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
    }
}
