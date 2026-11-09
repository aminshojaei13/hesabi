package com.braveboy.hesabi.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.LocalHesabiDisplay
import com.braveboy.hesabi.ui.theme.LocalHesabiSemantic

@Composable
fun AmountText(
    amountToman: Long,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
    style: TextStyle? = null,
    showSign: Boolean = false,
    hidden: Boolean? = null,
) {
    val display = LocalHesabiDisplay.current
    val semantic = LocalHesabiSemantic.current
    val shouldHide = hidden ?: display.hideAmounts
    val color = when (type) {
        TransactionType.EXPENSE -> semantic.expense
        TransactionType.INCOME -> semantic.income
        TransactionType.TRANSFER -> semantic.transfer
        null -> MaterialTheme.colorScheme.onSurface
    }
    val sign = when {
        !showSign -> ""
        type == TransactionType.EXPENSE -> "−"
        type == TransactionType.INCOME -> "+"
        type == TransactionType.TRANSFER -> "⇄ "
        else -> ""
    }
    val text = if (shouldHide) {
        "••••••"
    } else {
        sign + Money.format(
            amountToman = amountToman,
            currency = display.currency,
            withUnit = true,
            persian = true,
        )
    }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style ?: MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
    )
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    icon: @Composable (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier.padding(HesabiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
            ) {
                if (icon != null) icon()
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun MonthSwitcher(
    period: MonthPeriod,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoToCurrentMonth: () -> Unit,
    modifier: Modifier = Modifier,
    currentPeriod: MonthPeriod = MonthPeriod.current(),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "ماه قبل")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = period.label, style = MaterialTheme.typography.titleMedium)
            if (period != currentPeriod) {
                Text(
                    modifier = Modifier.clickable(onClick = onGoToCurrentMonth),
                    text = "برگشت به ماه جاری",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "ماه بعد")
        }
    }
}

@Composable
fun BalanceHero(
    balanceToman: Long,
    monthExpenseToman: Long,
    monthIncomeToman: Long,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalHesabiSemantic.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Column(
            modifier = Modifier.padding(HesabiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md)
        ) {
            Text(
                text = "موجودی کل",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            )
            AmountText(
                amountToman = balanceToman,
                style = MaterialTheme.typography.displaySmall,
                hidden = null,
                modifier = Modifier,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xl)) {
                Column {
                    Text(
                        "هزینه این ماه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                    )
                    Text(
                        text = Money.format(monthExpenseToman, withUnit = false),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                    )
                }
                Column {
                    Text(
                        "درآمد این ماه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                    )
                    Text(
                        text = Money.format(monthIncomeToman, withUnit = false),
                        style = MaterialTheme.typography.titleSmall,
                        color = semantic.income,
                    )
                }
            }
        }
    }
}

@Preview(name = "مبالغ")
@Composable
private fun AmountPreview() {
    HesabiPreview {
        Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.lg)) {
            AmountText(185_000, type = TransactionType.EXPENSE, showSign = true)
            AmountText(24_000_000, type = TransactionType.INCOME, showSign = true)
        }
        BalanceHero(
            balanceToman = 15_250_000,
            monthExpenseToman = 3_200_000,
            monthIncomeToman = 24_000_000
        )
        Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
            StatTile("امروز", "۱۲۵٬۰۰۰", modifier = Modifier.weight(1f), caption = "۳ تراکنش")
            StatTile(
                "مانده ماه",
                "۲۰٬۸۰۰٬۰۰۰",
                modifier = Modifier.weight(1f),
                caption = "مثبت",
                valueColor = LocalHesabiSemantic.current.income
            )
        }
        MonthSwitcher(
            period = MonthPeriod(1405, 8),
            onPrevious = {},
            onNext = {},
            onGoToCurrentMonth = {}
        )
    }
}

@Preview(name = "مبالغ پنهان", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HiddenAmountPreview() {
    HesabiPreview(dark = true) {
        Surface(shape = CircleShape, color = Color.Transparent) { Box(Modifier.size(1.dp)) }
        AmountText(15_250_000, hidden = true, style = MaterialTheme.typography.displaySmall)
        Text("حالت پنهان‌کردن مبالغ", style = MaterialTheme.typography.bodySmall)
    }
}
