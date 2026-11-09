package com.braveboy.hesabi.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.LocalHesabiSemantic

/** Human day label used by every list: امروز / دیروز / ۲ آبان / ۱۲ آبان ۱۴۰۴. */
fun transactionDayLabel(epochDay: Long, today: JalaliDate = JalaliDate.today()): String {
    val date = JalaliDate.fromEpochDay(epochDay)
    return when {
        date == today -> "امروز"
        date == today.plusDays(-1) -> "دیروز"
        date.year == today.year -> date.formatDayMonth()
        else -> date.formatLong()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    transaction: Transaction,
    categoryName: String,
    iconKey: String?,
    accountName: String,
    toAccountName: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDay: Boolean = false,
    hidden: Boolean? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val semantic = LocalHesabiSemantic.current
    val isTransfer = transaction.type == TransactionType.TRANSFER
    val accent = when (transaction.type) {
        TransactionType.EXPENSE -> semantic.expense
        TransactionType.INCOME -> semantic.income
        TransactionType.TRANSFER -> semantic.transfer
    }
    val title = if (isTransfer) "انتقال" else categoryName
    val subtitle = when {
        isTransfer && transaction.note.isNotBlank() -> transaction.note
        isTransfer -> "$accountName ← ${toAccountName ?: "—"}"
        transaction.note.isNotBlank() -> transaction.note
        else -> accountName
    }

    val spokenAmount = if (com.braveboy.hesabi.ui.theme.LocalHesabiDisplay.current.hideAmounts) {
        "مبلغ پنهان"
    } else {
        Money.format(transaction.amountToman)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$title، ${transaction.type.label}، $spokenAmount، $subtitle"
            }
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier.clickable(onClick = onClick)
                },
            )
            .padding(horizontal = HesabiSpacing.xs, vertical = HesabiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
    ) {
        Surface(shape = CircleShape, color = accent.copy(alpha = 0.14f), modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isTransfer) {
                        Icons.Rounded.SwapHoriz
                    } else {
                        categoryIcon(iconKey ?: "")
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (transaction.source == com.braveboy.hesabi.domain.model.TransactionSource.SMS) {
                    Spacer(Modifier.width(HesabiSpacing.xs))
                    Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text = "پیامک",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                if (!transaction.receiptImagePath.isNullOrBlank()) {
                    Spacer(Modifier.width(HesabiSpacing.xs))
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = "دارای تصویر فاکتور",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showDay) {
                Text(
                    text = transactionDayLabel(transaction.occurredOnEpochDay),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
        AmountText(
            amountToman = transaction.amountToman,
            type = transaction.type,
            showSign = true,
            style = MaterialTheme.typography.bodyLarge,
            hidden = hidden,
        )
    }
}

@Composable
fun TransactionDayHeader(
    epochDay: Long,
    expenseToman: Long,
    incomeToman: Long,
    modifier: Modifier = Modifier,
    today: JalaliDate = JalaliDate.today(),
) {
    Column(modifier = modifier.padding(top = HesabiSpacing.md, bottom = HesabiSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = transactionDayLabel(epochDay, today),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.md)) {
                if (expenseToman > 0) {
                    Text(
                        text = "−" + Money.compact(expenseToman),
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalHesabiSemantic.current.expense,
                    )
                }
                if (incomeToman > 0) {
                    Text(
                        text = "+" + Money.compact(incomeToman),
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalHesabiSemantic.current.income,
                    )
                }
            }
        }
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(top = HesabiSpacing.xs),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
fun AccountDot(colorArgb: Long, modifier: Modifier = Modifier, size: Int = 10) {
    Surface(
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = Color(colorArgb),
        content = {},
    )
}

@Composable
fun TypePill(type: TransactionType, modifier: Modifier = Modifier) {
    val semantic = LocalHesabiSemantic.current
    val (label, color) = when (type) {
        TransactionType.EXPENSE -> "هزینه" to semantic.expense
        TransactionType.INCOME -> "درآمد" to semantic.income
        TransactionType.TRANSFER -> "انتقال" to semantic.transfer
    }
    Surface(shape = MaterialTheme.shapes.extraSmall, color = color.copy(alpha = 0.14f), modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** Groups transactions by Persian day, newest first. */
fun groupByDay(transactions: List<Transaction>): List<Pair<Long, List<Transaction>>> =
    transactions
        .sortedWith(compareByDescending<Transaction> { it.occurredOnEpochDay }.thenByDescending { it.createdAtMillis })
        .groupBy { it.occurredOnEpochDay }
        .toList()

@Preview(name = "ردیف تراکنش", showBackground = true)
@Composable
private fun TransactionRowPreview() {
    val categories = PreviewData.categories.associateBy { it.id }
    val accounts = PreviewData.accounts.associateBy { it.id }
    HesabiPreview {
        PreviewData.transactions.take(3).forEach { tx ->
            val category = categories[tx.categoryId]
            TransactionRow(
                transaction = tx,
                categoryName = category?.name ?: "بدون دسته",
                iconKey = category?.iconKey,
                accountName = accounts[tx.accountId]?.name ?: "—",
                toAccountName = accounts[tx.toAccountId]?.name,
                onClick = {},
            )
        }
        TransactionDayHeader(
            epochDay = PreviewData.today.toEpochDay(),
            expenseToman = 247_000,
            incomeToman = 0,
            today = PreviewData.today,
        )
        TransactionDayHeader(
            epochDay = PreviewData.today.plusDays(-1).toEpochDay(),
            expenseToman = 0,
            incomeToman = 1_500_000,
            today = PreviewData.today,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
            TransactionType.entries.forEach { TypePill(it) }
        }
    }
}

@Preview(name = "ردیف تراکنش - تیره", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionRowDarkPreview() {
    val tx = PreviewData.transactions.first()
    val category: Category? = PreviewData.food
    val account: Account = PreviewData.accounts[1]
    HesabiPreview(dark = true) {
        TransactionRow(
            transaction = tx,
            categoryName = category?.name ?: "خوراک",
            iconKey = category?.iconKey,
            accountName = account.name,
            onClick = {},
            showDay = true,
        )
        TransactionRow(
            transaction = PreviewData.transactions[3],
            categoryName = "انتقال",
            iconKey = null,
            accountName = "کارت بانکی",
            toAccountName = "نقدی",
            onClick = {},
        )
    }
}

@Preview(name = "ردیف تراکنش - حالت پنهان")
@Composable
private fun TransactionRowHiddenPreview() {
    val tx = PreviewData.transactions.first()
    HesabiPreview {
        TransactionRow(
            transaction = tx,
            categoryName = "خوراک",
            iconKey = "restaurant",
            accountName = "کارت بانکی",
            onClick = {},
            hidden = true,
        )
    }
}
