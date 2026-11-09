package com.braveboy.hesabi.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.components.EmptyState
import com.braveboy.hesabi.ui.components.FilterChipRow
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.components.LoadingSkeleton
import com.braveboy.hesabi.ui.components.SearchField
import com.braveboy.hesabi.ui.components.StatTile
import com.braveboy.hesabi.ui.components.TransactionDayHeader
import com.braveboy.hesabi.ui.components.TransactionRow
import com.braveboy.hesabi.ui.components.UndoBar
import com.braveboy.hesabi.ui.components.groupByDay
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme
import com.braveboy.hesabi.ui.theme.LocalHesabiSemantic

@Composable
fun TransactionsScreen(
    container: AppContainer,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TransactionsViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionsContent(
        state = state,
        onQueryChange = viewModel::setQuery,
        onTypeChange = viewModel::setType,
        onAccountChange = viewModel::setAccount,
        onCategoryChange = viewModel::setCategory,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onToggleAll = viewModel::toggleAllPeriods,
        onCurrentMonth = viewModel::currentMonth,
        onTransactionClick = onTransactionClick,
        onDelete = viewModel::delete,
        onUndo = viewModel::undoDelete,
        onDismissUndo = viewModel::dismissUndo,
        modifier = modifier,
    )
}

@Composable
fun TransactionsContent(
    state: TransactionsUiState,
    onQueryChange: (String) -> Unit,
    onTypeChange: (TransactionType?) -> Unit,
    onAccountChange: (String?) -> Unit = {},
    onCategoryChange: (String?) -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleAll: () -> Unit,
    onCurrentMonth: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onDelete: (com.braveboy.hesabi.domain.model.Transaction) -> Unit,
    onUndo: () -> Unit,
    onDismissUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember(state.transactions) { groupByDay(state.transactions) }
    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(horizontal = HesabiSpacing.lg)) {
                Text(
                    text = "تراکنش‌ها",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = HesabiSpacing.md, bottom = HesabiSpacing.sm),
                )
                SearchField(value = state.filter.query, onValueChange = onQueryChange)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = HesabiSpacing.lg,
                end = HesabiSpacing.lg,
                top = HesabiSpacing.sm,
                bottom = 108.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
        ) {
            item {
                MonthFilterRow(
                    state = state,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onToggleAll = onToggleAll,
                    onCurrentMonth = onCurrentMonth,
                )
            }
            item {
                FilterChipRow(
                    options = listOf<TransactionType?>(null, TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER),
                    selected = state.filter.type,
                    onSelect = onTypeChange,
                    label = { it?.label ?: "همه" },
                )
            }
            item {
                val accounts = remember(state.accounts) { state.accounts.values.sortedBy { it.sortOrder } }
                FilterChipRow(
                    options = listOf<com.braveboy.hesabi.domain.model.Account?>(null) + accounts,
                    selected = state.filter.accountId?.let { state.accounts[it] },
                    onSelect = { onAccountChange(it?.id) },
                    label = { it?.name ?: "همهٔ حساب‌ها" },
                )
            }
            item {
                val categories = remember(state.categories, state.filter.type) {
                    state.categories.values
                        .filter { category ->
                            state.filter.type == null ||
                                (state.filter.type == TransactionType.EXPENSE && category.kind == com.braveboy.hesabi.domain.model.CategoryKind.EXPENSE) ||
                                (state.filter.type == TransactionType.INCOME && category.kind == com.braveboy.hesabi.domain.model.CategoryKind.INCOME)
                        }
                        .sortedBy { it.sortOrder }
                }
                FilterChipRow(
                    options = listOf<com.braveboy.hesabi.domain.model.Category?>(null) + categories,
                    selected = state.filter.categoryId?.let { state.categories[it] },
                    onSelect = { onCategoryChange(it?.id) },
                    label = { it?.name ?: "همهٔ دسته‌ها" },
                )
            }
            if (!state.loading) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                        StatTile(
                            label = "هزینه",
                            value = Money.compact(state.totalExpenseToman),
                            modifier = Modifier.weight(1f),
                            caption = "${state.transactions.count { it.type == TransactionType.EXPENSE }} مورد",
                            valueColor = LocalHesabiSemantic.current.expense,
                        )
                        StatTile(
                            label = "درآمد",
                            value = Money.compact(state.totalIncomeToman),
                            modifier = Modifier.weight(1f),
                            caption = "${state.transactions.count { it.type == TransactionType.INCOME }} مورد",
                            valueColor = LocalHesabiSemantic.current.income,
                        )
                    }
                }
            }
            if (state.loading) {
                item { LoadingSkeleton(rows = 5) }
            } else if (groups.isEmpty()) {
                item {
                    EmptyState(
                        title = if (state.filter.query.isNotBlank()) "چیزی پیدا نشد" else "تراکنشی نیست",
                        message = if (state.filter.query.isNotBlank()) {
                            "عبارت دیگری را جست‌وجو کن یا فیلترها را بردار."
                        } else {
                            "در این بازه تراکنشی ثبت نشده است."
                        },
                        icon = Icons.Rounded.ReceiptLong,
                        actionLabel = if (state.filter.query.isNotBlank()) "پاک کردن جست‌وجو" else null,
                        onAction = if (state.filter.query.isNotBlank()) {
                            { onQueryChange("") }
                        } else {
                            null
                        },
                    )
                }
            } else {
                groups.forEach { (epochDay, dayTransactions) ->
                    item(key = "day-$epochDay") {
                        val expense = dayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountToman }
                        val income = dayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountToman }
                        TransactionDayHeader(
                            epochDay = epochDay,
                            expenseToman = expense,
                            incomeToman = income,
                        )
                    }
                    items(dayTransactions, key = { it.id }) { transaction ->
                        val category = state.categories[transaction.categoryId]
                        val account = state.accounts[transaction.accountId]
                        val toAccount = state.accounts[transaction.toAccountId]
                        TransactionRow(
                            transaction = transaction,
                            categoryName = category?.name ?: "بدون دسته",
                            iconKey = category?.iconKey,
                            accountName = account?.name ?: "—",
                            toAccountName = toAccount?.name,
                            onClick = { onTransactionClick(transaction.id) },
                            onLongClick = { onDelete(transaction) },
                        )
                    }
                }
            }
            item {
                Text(
                    text = "برای ویرایش ضربه بزن • برای حذف نگه دار",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = HesabiSpacing.md),
                )
            }
        }
        state.lastDeleted?.let {
            UndoBar(
                message = "تراکنش حذف شد.",
                onUndo = onUndo,
                modifier = Modifier.padding(HesabiSpacing.md),
            )
            androidx.compose.runtime.LaunchedEffect(it.id) {
                kotlinx.coroutines.delay(6_000)
                onDismissUndo()
            }
        }
    }
}

@Composable
private fun MonthFilterRow(
    state: TransactionsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleAll: () -> Unit,
    onCurrentMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "بازهٔ قبل")
            }
            Text(
                text = if (state.filter.allPeriods) "همهٔ ماه‌ها" else state.filter.period.label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = HesabiSpacing.xs),
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "بازهٔ بعد")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.filter.allPeriods) {
                FilterChip(selected = false, onClick = onCurrentMonth, label = { Text("این ماه") })
            } else {
                FilterChip(selected = true, onClick = onToggleAll, label = { Text("همه") })
            }
        }
    }
}

@Preview(name = "تراکنش‌ها", heightDp = 900, showBackground = true)
@Composable
private fun TransactionsPreview() {
    val state = TransactionsUiState(
        filter = TransactionFilter(period = MonthPeriod.of(PreviewData.today)),
        transactions = PreviewData.transactions,
        categories = PreviewData.categories.associateBy { it.id },
        accounts = PreviewData.accounts.associateBy { it.id },
        totalExpenseToman = 3_200_000,
        totalIncomeToman = 24_000_000,
        loading = false,
    )
    HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                TransactionsContent(
                    state = state,
                    onQueryChange = {},
                    onTypeChange = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onToggleAll = {},
                    onCurrentMonth = {},
                    onTransactionClick = {},
                    onDelete = {},
                    onUndo = {},
                    onDismissUndo = {},
                )
            }
        }
    }
}

@Preview(
    name = "تراکنش‌ها - خالی",
    heightDp = 800,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun TransactionsEmptyPreview() {
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                TransactionsContent(
                    state = TransactionsUiState(loading = false),
                    onQueryChange = {},
                    onTypeChange = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onToggleAll = {},
                    onCurrentMonth = {},
                    onTransactionClick = {},
                    onDelete = {},
                    onUndo = {},
                    onDismissUndo = {},
                )
            }
        }
    }
}
