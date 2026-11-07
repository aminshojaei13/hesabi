package com.braveboy.hesabi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.ui.theme.LocalHesabiLumina
import kotlin.math.PI
import kotlin.math.sin

/**
 * A neon-glowing audio waveform visualizer drawn with Compose Canvas.
 * When [isRecording] is true, it shows animated sine waves that respond
 * to [amplitudes]. When idle, it shows a flat line with a subtle pulse.
 */
@Composable
fun AudioWaveformVisualizer(
    isRecording: Boolean,
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
) {
    val lumina = LocalHesabiLumina.current
    val primaryColor = lumina.voiceWaveform
    val secondaryColor = lumina.voiceWaveformSecondary

    // Animation for the wave phase shift (continuous scrolling)
    val infiniteTransition = rememberInfiniteTransition(label = "wavePhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    // Pulse animation for idle state
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        if (!isRecording) {
            // Idle state: pulsing flat line
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0f),
                        primaryColor.copy(alpha = pulseAlpha),
                        primaryColor.copy(alpha = 0f),
                    ),
                ),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
            return@Canvas
        }

        // Active state: animated multi-layered sine waveform
        val barCount = amplitudes.size.coerceAtLeast(1)

        // Draw 2 layered wave paths with different frequencies
        for (layer in 0..1) {
            val path = Path()
            val freq = if (layer == 0) 3f else 5f
            val layerAlpha = if (layer == 0) 0.9f else 0.4f
            val layerColor = if (layer == 0) primaryColor else secondaryColor

            val segmentCount = 100
            for (i in 0..segmentCount) {
                val x = width * i / segmentCount
                val progress = i.toFloat() / segmentCount

                val ampIndex = (progress * (barCount - 1)).toInt().coerceIn(0, barCount - 1)
                val amp = if (amplitudes.isNotEmpty()) amplitudes[ampIndex] else 0.3f

                val wave = sin(freq * progress * PI.toFloat() + phase + layer * 0.5f)
                val envelope = sin(progress * PI.toFloat())
                val y = centerY + wave * amp * height * 0.35f * envelope

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Glow line (thicker, translucent)
            drawPath(
                path = path,
                color = layerColor.copy(alpha = layerAlpha * 0.3f),
                style = Stroke(width = 8f, cap = StrokeCap.Round),
            )
            // Core line
            drawPath(
                path = path,
                color = layerColor.copy(alpha = layerAlpha),
                style = Stroke(width = 3f, cap = StrokeCap.Round),
            )
        }
    }
}
