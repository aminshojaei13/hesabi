package com.braveboy.hesabi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.logic.BurnRateStatus
import com.braveboy.hesabi.domain.logic.LiquidityForecast
import com.braveboy.hesabi.domain.model.ThemeMode
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme
import com.braveboy.hesabi.ui.theme.LocalHesabiLumina

/**
 * Modern fintech Liquidity Forecast card.
 * Informs the user about their remaining disposable liquidity runway after obligations,
 * days remaining until payday, and daily budget resilience.
 */
@Composable
fun MonthlySalaryLiquidityCard(
    forecast: LiquidityForecast?,
    onSetupSalary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lumina = LocalHesabiLumina.current

    if (forecast == null || forecast.monthlySalaryToman <= 0) {
        // Banner prompting user to configure monthly salary
        GlassCard(
            modifier = modifier.fillMaxWidth(),
            onClick = onSetupSalary,
            glowColor = lumina.glowSecondary,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "کنترل نقدینگی و ثبت حقوق ماهانه",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "با ثبت حقوق ماهانه، تاب‌آوری مالی و سقف مجاز روزانه تا سررسید بعدی محاسبه می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(HesabiSpacing.md))

            ElevatedButton(
                onClick = onSetupSalary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HesabiRadius.input),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    Icons.Rounded.Payments,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(HesabiSpacing.xs))
                Text(
                    text = "تنظیم حقوق ماهیانه",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    } else {
        val visual = when (forecast.runwayStatus) {
            BurnRateStatus.EXCELLENT -> LiquidityStatusVisual(
                lumina.safeBudgetGreen,
                lumina.glowPrimary,
                Icons.Rounded.CheckCircle,
                "وضعیت عالی",
            )
            BurnRateStatus.NORMAL -> LiquidityStatusVisual(
                lumina.voiceWaveformSecondary,
                lumina.glowSecondary,
                Icons.Rounded.CheckCircle,
                "متعادل",
            )
            BurnRateStatus.CAUTION -> LiquidityStatusVisual(
                lumina.cautionBudgetYellow,
                lumina.cautionBudgetYellow.copy(alpha = 0.2f),
                Icons.Rounded.Warning,
                "احتیاط در نقدینگی",
            )
            BurnRateStatus.DANGER -> LiquidityStatusVisual(
                lumina.dangerBudgetRed,
                lumina.dangerBudgetRed.copy(alpha = 0.2f),
                Icons.Rounded.Info,
                "کسری نقدینگی",
            )
        }

        val remainingFraction = if (forecast.monthlySalaryToman > 0) {
            (forecast.netDisposableToman.toFloat() / forecast.monthlySalaryToman).coerceIn(0f, 1f)
        } else 0f

        val animatedProgress by animateFloatAsState(
            targetValue = remainingFraction,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            label = "liquidityProgress",
        )

        GlassCard(
            modifier = modifier.fillMaxWidth(),
            onClick = onSetupSalary,
            glowColor = visual.glow,
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "کنترل نقدینگی ماهانه",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "${Digits.toPersian(forecast.daysRemaining.toString())} روز تا حقوق بعدی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = visual.color.copy(alpha = 0.15f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            visual.icon,
                            contentDescription = null,
                            tint = visual.color,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = visual.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = visual.color,
                        )
                    }
                }
            }

            Spacer(Modifier.height(HesabiSpacing.md))

            // Main Metric: Net Disposable Cash
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "نقدینگی خالص در دسترس",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = Money.format(forecast.netDisposableToman, withUnit = false),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (forecast.netDisposableToman > 0) MaterialTheme.colorScheme.onSurface else lumina.dangerBudgetRed,
                    )
                    Text(
                        text = "تومان",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(HesabiSpacing.md))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = visual.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )

            Spacer(Modifier.height(HesabiSpacing.md))

            // 3-Column Metrics Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricColumn(
                    title = "حقوق ماهانه",
                    amountToman = forecast.monthlySalaryToman,
                    modifier = Modifier.weight(1f),
                )
                MetricColumn(
                    title = "تعهدات و پس‌انداز",
                    amountToman = forecast.fixedCommitmentsToman,
                    modifier = Modifier.weight(1f),
                )
                MetricColumn(
                    title = "تاب‌آوری روزانه",
                    amountToman = forecast.dailyRunwayToman,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    amountToman: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = Money.format(amountToman, withUnit = true),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private data class LiquidityStatusVisual(
    val color: Color,
    val glow: Color,
    val icon: ImageVector,
    val label: String,
)

@Preview(name = "کارت نقدینگی - فعال (تاریک)", showBackground = true)
@Composable
private fun MonthlySalaryLiquidityCardActiveDarkPreview() {
    HesabiTheme(themeMode = ThemeMode.DARK) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                MonthlySalaryLiquidityCard(
                    forecast = LiquidityForecast(
                        monthlySalaryToman = 25_000_000L,
                        fixedCommitmentsToman = 6_500_000L,
                        netDisposableToman = 14_200_000L,
                        daysRemaining = 18,
                        dailyRunwayToman = 788_000L,
                        runwayStatus = BurnRateStatus.NORMAL,
                    ),
                    onSetupSalary = {},
                )
            }
        }
    }
}

@Preview(name = "کارت نقدینگی - فعال (روشن)", showBackground = true)
@Composable
private fun MonthlySalaryLiquidityCardActiveLightPreview() {
    HesabiTheme(themeMode = ThemeMode.LIGHT) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                MonthlySalaryLiquidityCard(
                    forecast = LiquidityForecast(
                        monthlySalaryToman = 20_000_000L,
                        fixedCommitmentsToman = 4_000_000L,
                        netDisposableToman = 16_000_000L,
                        daysRemaining = 24,
                        dailyRunwayToman = 666_000L,
                        runwayStatus = BurnRateStatus.EXCELLENT,
                    ),
                    onSetupSalary = {},
                )
            }
        }
    }
}

@Preview(name = "کارت نقدینگی - ثبت نشده", showBackground = true)
@Composable
private fun MonthlySalaryLiquidityCardEmptyPreview() {
    HesabiTheme(themeMode = ThemeMode.DARK) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                MonthlySalaryLiquidityCard(
                    forecast = null,
                    onSetupSalary = {},
                )
            }
        }
    }
}
