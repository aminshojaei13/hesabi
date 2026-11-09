package com.braveboy.hesabi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.domain.logic.EndOfMonthForecast
import com.braveboy.hesabi.domain.logic.ForecastStatus
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

/** "پیش‌بینی پایان ماه" card: one number and one honest sentence. */
@Composable
fun ForecastCard(
    forecast: EndOfMonthForecast,
    modifier: Modifier = Modifier,
) {
    val color = when (forecast.status) {
        ForecastStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
        ForecastStatus.TIGHT -> MaterialTheme.colorScheme.tertiary
        ForecastStatus.AT_RISK, ForecastStatus.CRITICAL -> MaterialTheme.colorScheme.error
    }
    HesabiCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "پیش‌بینی پایان ${forecast.period.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = forecast.headline(),
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            androidx.compose.material3.Icon(
                Icons.Rounded.Insights,
                contentDescription = null,
                tint = color,
            )
        }
        AmountText(
            amountToman = forecast.projectedRemainingBalanceToman,
            style = MaterialTheme.typography.headlineSmall,
            hidden = null,
            modifier = Modifier,
        )
        Text(
            text = "با میانگین خرج روزانهٔ " + com.braveboy.hesabi.core.money.Money.format(
                forecast.averageDailyExpenseToman,
            ) + " و " + Digits.toPersian(forecast.daysRemaining.toString()) + " روز باقی‌مانده",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (forecast.shortfallToman > 0) {
            Text(
                text = "کمبود تخمینی: " + com.braveboy.hesabi.core.money.Money.format(forecast.shortfallToman),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(name = "پیش‌بینی پایان ماه")
@Composable
private fun ForecastPreview() {
    HesabiTheme {
        ForecastCard(
            forecast = EndOfMonthForecast(
                period = MonthPeriod(1405, 8),
                daysElapsed = 20,
                daysRemaining = 10,
                spentSoFarToman = 8_400_000,
                incomeSoFarToman = 25_000_000,
                currentBalanceToman = 18_000_000,
                averageDailyExpenseToman = 420_000,
                projectedExpenseToman = 12_600_000,
                upcomingCommitmentsToman = 2_000_000,
                projectedRemainingBalanceToman = 3_400_000,
                shortfallToman = 0,
                safeDailyAfterCommitmentsToman = 340_000,
                status = ForecastStatus.TIGHT,
            ),
        )
    }
}
