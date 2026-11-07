package com.braveboy.hesabi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.logic.BurnRateStatus
import com.braveboy.hesabi.domain.logic.DailySafeToSpend
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.LocalHesabiLumina

/**
 * Modern Dribbble-inspired Daily Safe-to-Spend radial gauge widget.
 * Visually conveys whether the user is on track, in caution, or overspending today.
 */
@Composable
fun DailySafeToSpendWidget(
    safeToSpend: DailySafeToSpend,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val lumina = LocalHesabiLumina.current

    val (statusColor, statusGlow, statusIcon, statusLabel) = when (safeToSpend.burnRateStatus) {
        BurnRateStatus.EXCELLENT -> Quad(
            lumina.safeBudgetGreen,
            lumina.glowPrimary,
            Icons.Rounded.CheckCircle,
            "وضعیت عالی",
        )
        BurnRateStatus.NORMAL -> Quad(
            lumina.voiceWaveformSecondary,
            lumina.glowSecondary,
            Icons.Rounded.CheckCircle,
            "متعادل",
        )
        BurnRateStatus.CAUTION -> Quad(
            lumina.cautionBudgetYellow,
            lumina.cautionBudgetYellow.copy(alpha = 0.2f),
            Icons.Rounded.Warning,
            "احتیاط در خرج",
        )
        BurnRateStatus.DANGER -> Quad(
            lumina.dangerBudgetRed,
            lumina.dangerBudgetRed.copy(alpha = 0.2f),
            Icons.Rounded.Info,
            "هشدار بودجه",
        )
    }

    val progressFraction = if (safeToSpend.safeToSpendTodayToman > 0) {
        (safeToSpend.spentTodayToman.toFloat() / safeToSpend.safeToSpendTodayToman).coerceIn(0f, 1f)
    } else {
        1f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "radialProgress",
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        glowColor = statusGlow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "خرج امن امروز",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Status chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                        )
                    }
                }
            }

            Spacer(Modifier.height(HesabiSpacing.lg))

            // Central Glowing Radial Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp),
            ) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = size.width - strokeWidth
                    val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)

                    // Track background (dim arc, 240 degrees)
                    drawArc(
                        color = statusColor.copy(alpha = 0.12f),
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )

                    // Active progress sweep (gradient)
                    if (animatedProgress > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    statusColor.copy(alpha = 0.6f),
                                    statusColor,
                                ),
                            ),
                            startAngle = 150f,
                            sweepAngle = 240f * animatedProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }

                // Inner content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Money.format(safeToSpend.safeToSpendTodayToman, withUnit = false),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "تومان",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(HesabiSpacing.md))

            // Footer info: Days remaining till next payday
            Surface(
                shape = RoundedCornerShape(HesabiRadius.chip),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = HesabiSpacing.md, vertical = HesabiSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${Digits.toPersian(safeToSpend.daysRemaining.toString())} روز تا دریافت حقوق بعدی",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Text(
                        text = "خرج امروز: ${Money.format(safeToSpend.spentTodayToman)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
