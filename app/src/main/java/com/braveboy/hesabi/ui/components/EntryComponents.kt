package com.braveboy.hesabi.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.LocalHesabiDisplay
import com.braveboy.hesabi.ui.theme.LocalHesabiSemantic

@Composable
fun TransactionTypeToggle(
    type: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalHesabiSemantic.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(HesabiSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
    ) {
        TransactionType.entries.forEach { option ->
            val selected = option == type
            val accent = when (option) {
                TransactionType.EXPENSE -> semantic.expense
                TransactionType.INCOME -> semantic.income
                TransactionType.TRANSFER -> semantic.transfer
            }
            val background by animateColorAsState(
                targetValue = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent,
                label = "typeBackground",
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "typeContent",
            )
            Surface(
                onClick = { onTypeChange(option) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = MaterialTheme.shapes.small,
                color = background,
                border = if (selected) BorderStroke(1.dp, accent.copy(alpha = 0.6f)) else null,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
fun AmountEntryDisplay(
    amountToman: Long,
    type: TransactionType,
    modifier: Modifier = Modifier,
) {
    val display = LocalHesabiDisplay.current
    val semantic = LocalHesabiSemantic.current
    val accent = when (type) {
        TransactionType.EXPENSE -> semantic.expense
        TransactionType.INCOME -> semantic.income
        TransactionType.TRANSFER -> semantic.transfer
    }
    val label = when (type) {
        TransactionType.EXPENSE -> "مبلغ هزینه"
        TransactionType.INCOME -> "مبلغ درآمد"
        TransactionType.TRANSFER -> "مبلغ انتقال"
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.xxs),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
            Text(
                text = if (amountToman <= 0L) "۰" else Money.format(
                    amountToman = amountToman,
                    currency = display.currency,
                    withUnit = false,
                ),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (amountToman > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = display.currency.labelFa,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}

/**
 * Calculator-style keypad. The grid is forced LTR, matching every Android dialer,
 * while the labels stay Persian.
 */
@Composable
fun AmountKeypad(
    onDigit: (Int) -> Unit,
    onTripleZero: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "⌫"),
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                    row.forEach { key ->
                        Surface(
                            onClick = {
                                when (key) {
                                    "000" -> onTripleZero()
                                    "⌫" -> onBackspace()
                                    else -> key.toIntOrNull()?.let(onDigit)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "⌫") {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = "پاک کردن",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                } else {
                                    Text(
                                        text = Digits.toPersian(key),
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateField(
    date: JalaliDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = JalaliDate.today()
    val relative = when (date) {
        today -> "امروز"
        today.plusDays(-1) -> "دیروز"
        else -> null
    }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HesabiSpacing.md, vertical = HesabiSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date.formatLong(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (relative != null) {
                    Text(
                        text = relative,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun AccountChips(
    accounts: List<Account>,
    selectedId: String?,
    onSelect: (Account) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
        accounts.forEach { account ->
            val selected = account.id == selectedId
            Surface(
                onClick = { onSelect(account) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = if (selected) {
                    Color(account.colorArgb).copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) Color(account.colorArgb) else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = HesabiSpacing.md, vertical = HesabiSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    AccountDot(account.colorArgb, size = 12)
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = account.kind.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryGrid(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
        categories.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                row.forEach { category ->
                    val selected = category.id == selectedId
                    val color = Color(category.colorArgb)
                    Surface(
                        onClick = { onSelect(category) },
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) color.copy(alpha = 0.14f) else Color.Transparent,
                        border = if (selected) BorderStroke(1.5.dp, color) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = HesabiSpacing.sm, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                        ) {
                            Surface(shape = CircleShape, color = color.copy(alpha = 0.16f), modifier = Modifier.size(34.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = categoryIcon(category.iconKey),
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        if (categories.isEmpty()) {
            Text(
                text = "دسته‌ای برای این نوع وجود ندارد. از «بیشتر ← دسته‌ها» اضافه کن.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        content()
    }
}

@Composable
fun CategoryChipRow(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
        categories.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
                row.forEach { category ->
                    FilterChip(
                        selected = category.id == selectedId,
                        onClick = { onSelect(category) },
                        label = { Text(category.name, maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                categoryIcon(category.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(category.colorArgb),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Preview(name = "ورودی مبلغ", showBackground = true)
@Composable
private fun EntryPreview() {
    HesabiPreview(spacing = HesabiSpacing.md) {
        TransactionTypeToggle(type = TransactionType.EXPENSE, onTypeChange = {})
        AmountEntryDisplay(amountToman = 1_250_000, type = TransactionType.EXPENSE)
        AmountKeypad(onDigit = {}, onTripleZero = {}, onBackspace = {})
        DateField(date = PreviewData.today, onClick = {})
        AccountChips(accounts = PreviewData.accounts, selectedId = "acc_card", onSelect = {})
        CategoryGrid(
            categories = PreviewData.categories.filter { it.kind == CategoryKind.EXPENSE },
            selectedId = "cat_food",
            onSelect = {},
        )
    }
}

@Preview(name = "انتخاب دسته - تیره", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryPreview() {
    HesabiPreview(dark = true) {
        TransactionTypeToggle(type = TransactionType.INCOME, onTypeChange = {})
        AmountEntryDisplay(amountToman = 24_000_000, type = TransactionType.INCOME)
        FormSection("دستهٔ درآمد") {
            CategoryChipRow(
                categories = PreviewData.categories.filter { it.kind == CategoryKind.INCOME },
                selectedId = "cat_salary",
                onSelect = {},
            )
        }
    }
}
