package com.braveboy.hesabi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.domain.logic.DailySafeToSpend
import com.braveboy.hesabi.ui.theme.LocalHesabiLumina
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern Dribbble-inspired Daily Safe-to-Spend radial gauge widget with perimeter ticks
 * and neon glow indicator cap. Dynamically adapts to Light and Dark Lumina themes.
 */
@Composable
fun DailySafeToSpendWidget(
    safeToSpend: DailySafeToSpend,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val lumina = LocalHesabiLumina.current

    val progressFraction = if (safeToSpend.safeToSpendTodayToman > 0) {
        (safeToSpend.spentTodayToman.toFloat() / safeToSpend.safeToSpendTodayToman).coerceIn(0.15f, 1f)
    } else {
        0.6f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "radialProgress",
    )

    val percentInt = (animatedProgress * 100).toInt()

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Action Icons Row (3-dots on left, info on right in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = null,
                    tint = lumina.textSecondary,
                    modifier = Modifier.size(20.dp),
                )

                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = lumina.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(6.dp))

            // Central Glowing Radial Gauge Dial with Perimeter Ticks
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp),
            ) {
                Canvas(modifier = Modifier.size(210.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val strokeWidth = 14.dp.toPx()
                    val arcRadius = (size.width - strokeWidth - 28.dp.toPx()) / 2f
                    val startAngle = 140f
                    val sweepAngle = 260f

                    // 1. Draw Perimeter Ticks (outer radiating lines)
                    val tickCount = 44
                    val tickOuterRadius = arcRadius + 18.dp.toPx()
                    val tickInnerRadius = arcRadius + 11.dp.toPx()

                    for (i in 0..tickCount) {
                        val tickFraction = i.toFloat() / tickCount
                        val tickAngle = startAngle + tickFraction * sweepAngle
                        val rad = Math.toRadians(tickAngle.toDouble())

                        val startX = center.x + tickInnerRadius * cos(rad).toFloat()
                        val startY = center.y + tickInnerRadius * sin(rad).toFloat()
                        val endX = center.x + tickOuterRadius * cos(rad).toFloat()
                        val endY = center.y + tickOuterRadius * sin(rad).toFloat()

                        val isActive = tickFraction <= animatedProgress
                        val tickColor = if (isActive) lumina.gaugeTickActive else lumina.gaugeTickInactive

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }

                    val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                    val arcDiameter = arcRadius * 2f

                    // 2. Track Background Arc (dim glass)
                    drawArc(
                        color = lumina.gaugeTrack,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = Size(arcDiameter, arcDiameter),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )

                    // 3. Active Gradient Progress Arc (Cyan -> Emerald Green -> Violet)
                    if (animatedProgress > 0f) {
                        drawArc(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF10B981), // Emerald
                                    Color(0xFF06B6D4), // Cyan
                                    Color(0xFF8B5CF6), // Violet
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height),
                            ),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle * animatedProgress,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = Size(arcDiameter, arcDiameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )

                        // 4. End Cap Glowing Knob Circle
                        val knobAngle = startAngle + sweepAngle * animatedProgress
                        val knobRad = Math.toRadians(knobAngle.toDouble())
                        val knobX = center.x + arcRadius * cos(knobRad).toFloat()
                        val knobY = center.y + arcRadius * sin(knobRad).toFloat()

                        // Outer Cyan Glow Halo
                        drawCircle(
                            color = Color(0xFF06B6D4).copy(alpha = 0.45f),
                            radius = 9.dp.toPx(),
                            center = Offset(knobX, knobY),
                        )
                        // Inner White Dot
                        drawCircle(
                            color = Color.White,
                            radius = 3.5.dp.toPx(),
                            center = Offset(knobX, knobY),
                        )
                    }
                }

                // Dial Inner Center Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "خرج امن روزانه",
                        style = MaterialTheme.typography.labelSmall,
                        color = lumina.textSecondary,
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "${Digits.toPersian(percentInt.toString())}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = lumina.textPrimary,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "${Digits.toPersian(safeToSpend.daysRemaining.toString())} باقی روز",
                        style = MaterialTheme.typography.labelMedium,
                        color = lumina.textSecondary,
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(Modifier.height(8.dp))

                    // Bottom Pill Badge inside Dial ("۱۳ باید")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = lumina.gaugeCenterPillBg,
                    ) {
                        Text(
                            text = "${Digits.toPersian((safeToSpend.daysRemaining + 1).toString())} باید",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = lumina.textPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }
    }
}
