package com.braveboy.hesabi.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.ui.components.BudgetProgressCard
import com.braveboy.hesabi.ui.components.BudgetRow
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.MonthSwitcher
import com.braveboy.hesabi.ui.components.SectionHeader
import com.braveboy.hesabi.ui.components.categoryIcon
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

@Composable
fun BudgetScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: BudgetViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editOverall by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<BudgetLine?>(null) }

    BudgetContent(
        state = state,
        onBack = onBack,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onCurrentMonth = viewModel::currentMonth,
        onEditOverall = { editOverall = true },
        onEditCategory = { editCategory = it },
        modifier = modifier,
    )

    if (editOverall) {
        BudgetEditorDialog(
            title = "بودجهٔ کل ${state.period.label}",
            initial = state.overallBudget,
            onDismiss = { editOverall = false },
            onSave = { amount ->
                viewModel.saveOverall(amount)
                editOverall = false
            },
            onRemove = state.overallBudget?.let {
                {
                    viewModel.removeOverall()
                    editOverall = false
                }
            },
        )
    }

    editCategory?.let { line ->
        BudgetEditorDialog(
            title = "بودجهٔ ${line.category.name}",
            initial = line.budget,
            onDismiss = { editCategory = null },
            onSave = { amount ->
                viewModel.saveCategory(line.category.id, amount)
                editCategory = null
            },
            onRemove = line.budget?.let {
                {
                    viewModel.removeCategory(line.category.id)
                    editCategory = null
                }
            },
        )
    }
}

@Composable
fun BudgetContent(
    state: BudgetUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onEditOverall: () -> Unit,
    onEditCategory: (BudgetLine) -> Unit,
    modifier: Modifier = Modifier,
) {
    val withBudget = state.lines.filter { it.budget != null }
    val withoutBudget = state.lines.filter { it.budget == null }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HesabiSpacing.lg,
            end = HesabiSpacing.lg,
            top = HesabiSpacing.sm,
            bottom = HesabiSpacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
    ) {
        item {
            HesabiTopBar(title = "بودجه", subtitle = "سقف خرج برای هر ماه", onBack = onBack)
        }
        item {
            MonthSwitcher(
                period = state.period,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onGoToCurrentMonth = onCurrentMonth,
                currentPeriod = MonthPeriod.current(),
            )
        }
        item {
            BudgetProgressCard(
                spentToman = state.overallSpentToman,
                limitToman = state.overallBudget?.limitToman,
                title = "بودجهٔ کل ماه",
                onEdit = onEditOverall,
            )
        }
        item {
            InfoBanner(
                text = "بودجه فقط شامل هزینه‌هاست؛ انتقال بین حساب‌ها و درآمد در آن حساب نمی‌شود.",
            )
        }
        if (withBudget.isNotEmpty()) {
            item { SectionHeader("بودجهٔ دسته‌ها") }
            items(withBudget, key = { it.category.id }) { line ->
                CategoryBudgetCard(line = line, onClick = { onEditCategory(line) })
            }
        }
        if (withoutBudget.isNotEmpty()) {
            item { SectionHeader("بدون بودجه") }
            items(withoutBudget, key = { "no-${it.category.id}" }) { line ->
                CategoryBudgetCard(line = line, onClick = { onEditCategory(line) })
            }
        }
        item {
            Text(
                text = "برای هر دسته سقف جدا تعیین کن؛ مجموع بودجه‌های دسته‌ها در گزارش ماه دیده می‌شود.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryBudgetCard(line: BudgetLine, onClick: () -> Unit) {
    val color = Color(line.category.colorArgb)
    val budget = line.budget
    HesabiCard(onClick = onClick, contentPadding = PaddingValues(HesabiSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        ) {
            androidx.compose.material3.Icon(
                imageVector = categoryIcon(line.category.iconKey),
                contentDescription = null,
                tint = color,
            )
            Text(line.category.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = if (budget == null) "تعیین بودجه" else Money.compact(budget.limitToman),
                style = MaterialTheme.typography.labelMedium,
                color = if (budget == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (budget != null) {
            BudgetRow(
                title = "",
                spentToman = line.spentToman,
                limitToman = budget.limitToman,
                color = color,
            )
        }
    }
}

@Composable
private fun BudgetEditorDialog(
    title: String,
    initial: Budget?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    onRemove: (() -> Unit)?,
) {
    var text by remember {
        mutableStateOf(
            initial?.limitToman?.takeIf { it > 0 }?.let { Money.format(it, withUnit = false) }.orEmpty(),
        )
    }
    val parsed = Money.parseToman(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                Text(
                    "مبلغ بودجه را به تومان وارد کن. خالی گذاشتن یعنی حذف بودجه.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HesabiTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "سقف بودجه (تومان)",
                    placeholder = "۵٬۰۰۰٬۰۰۰",
                    error = if (text.isNotBlank() && parsed == null) "مبلغ قابل خواندن نیست." else null,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsed ?: 0L) },
                enabled = text.isBlank() || parsed != null,
            ) { Text(if (text.isBlank()) "حذف بودجه" else "ذخیره") }
        },
        dismissButton = {
            Row {
                if (onRemove != null) {
                    TextButton(onClick = onRemove) {
                        Text("حذف", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        },
    )
}

@Preview(name = "بودجه", heightDp = 850, showBackground = true)
@Composable
private fun BudgetPreview() {
    val lines = PreviewData.categories
        .filter { it.kind == com.braveboy.hesabi.domain.model.CategoryKind.EXPENSE }
        .take(5)
        .mapIndexed { index, category ->
            BudgetLine(
                category = category,
                budget = if (index < 3) {
                    Budget("b$index", category.id, "1405-08", 1_000_000L * (index + 1))
                } else {
                    null
                },
                spentToman = 620_000L * (index + 1),
            )
        }
    val state = BudgetUiState(
        period = MonthPeriod.of(PreviewData.today),
        overallBudget = Budget("overall", "", "1405-08", 6_000_000),
        overallSpentToman = 3_200_000,
        lines = lines,
        loading = false,
    )
    HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                BudgetContent(state, {}, {}, {}, {}, {}, {})
            }
        }
    }
}

@Preview(
    name = "بودجه - تیره",
    heightDp = 850,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun BudgetDarkPreview() {
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                BudgetContent(
                    BudgetUiState(period = MonthPeriod.current(), loading = false),
                    {}, {}, {}, {}, {}, {},
                )
            }
        }
    }
}
