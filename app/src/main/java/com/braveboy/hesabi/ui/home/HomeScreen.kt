package com.braveboy.hesabi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.logic.ReportCalculator
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.components.AddEditGoalDialog
import com.braveboy.hesabi.ui.components.BalanceHero
import com.braveboy.hesabi.ui.components.BudgetProgressCard
import com.braveboy.hesabi.ui.components.DailySafeToSpendWidget
import com.braveboy.hesabi.ui.components.EmptyState
import com.braveboy.hesabi.ui.components.ForecastCard
import com.braveboy.hesabi.ui.components.GoalDepositWithdrawDialog
import com.braveboy.hesabi.ui.components.MonthSwitcher
import com.braveboy.hesabi.ui.components.MonthlySalaryLiquidityCard
import com.braveboy.hesabi.ui.components.PendingSmsBanner
import com.braveboy.hesabi.ui.components.SavingsGoalsSection
import com.braveboy.hesabi.ui.components.SectionHeader
import com.braveboy.hesabi.ui.components.TransactionRow
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.settings.SalarySetupDialog
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme
import com.braveboy.hesabi.ui.theme.LocalHesabiSemantic

@Composable
fun HomeScreen(
    container: AppContainer,
    onAdd: (TransactionType) -> Unit,
    onSeeAll: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onOpenSms: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVoice: () -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshIfMonthChanged()
        onPauseOrDispose { }
    }

    var activeGoalForDeposit by remember { mutableStateOf<SavingsGoal?>(null) }
    var activeGoalForEdit by remember { mutableStateOf<SavingsGoal?>(null) }
    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var showSalaryDialog by remember { mutableStateOf(false) }

    HomeContent(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onGoToCurrentMonth = viewModel::goToCurrentMonth,
        onAdd = onAdd,
        onSeeAll = onSeeAll,
        onTransactionClick = onTransactionClick,
        onOpenSms = onOpenSms,
        onOpenBudget = onOpenBudget,
        onOpenSettings = onOpenSettings,
        onOpenVoice = onOpenVoice,
        onOpenAssistant = onOpenAssistant,
        onOpenSalarySetup = { showSalaryDialog = true },
        onAddGoal = { showCreateGoalDialog = true },
        onGoalClick = { goal -> activeGoalForDeposit = goal },
        modifier = modifier,
    )

    if (showSalaryDialog) {
        SalarySetupDialog(
            initialSalaryToman = state.monthlySalaryToman,
            initialPayday = state.paydayDayOfMonth,
            initialAccountId = state.salaryAccountId,
            initialAutoDeposit = state.autoDepositSalary,
            accounts = state.balances.map { it.account },
            onDismiss = { showSalaryDialog = false },
            onSave = { salary, payday, accId, autoDeposit ->
                viewModel.updateSalarySettings(salary, payday, accId, autoDeposit)
                showSalaryDialog = false
            },
        )
    }

    if (activeGoalForDeposit != null) {
        GoalDepositWithdrawDialog(
            goal = activeGoalForDeposit!!,
            onDeposit = { amount -> viewModel.depositToGoal(activeGoalForDeposit!!.id, amount) },
            onWithdraw = { amount -> viewModel.withdrawFromGoal(activeGoalForDeposit!!.id, amount) },
            onEdit = {
                activeGoalForEdit = activeGoalForDeposit
            },
            onDismiss = { activeGoalForDeposit = null },
        )
    }

    if (activeGoalForEdit != null) {
        AddEditGoalDialog(
            goal = activeGoalForEdit,
            onSave = { updated -> viewModel.saveGoal(updated) },
            onDelete = { id -> viewModel.deleteGoal(id) },
            onDismiss = { activeGoalForEdit = null },
        )
    }

    if (showCreateGoalDialog) {
        AddEditGoalDialog(
            goal = null,
            onSave = { newGoal -> viewModel.saveGoal(newGoal) },
            onDismiss = { showCreateGoalDialog = false },
        )
    }
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToCurrentMonth: () -> Unit,
    onAdd: (TransactionType) -> Unit,
    onSeeAll: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onOpenSms: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVoice: () -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    onOpenSalarySetup: () -> Unit = {},
    onAddGoal: () -> Unit = {},
    onGoalClick: (SavingsGoal) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val report = state.report
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HesabiSpacing.lg,
            end = HesabiSpacing.lg,
            top = HesabiSpacing.md,
            bottom = 108.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.lg),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = HesabiSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "👋",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "سلام، خوش‌آمدید",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "حسابی",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.height(38.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = JalaliDate.today().let { Digits.toPersian("${it.day} ${it.monthName()}") },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    if (state.pendingSmsCount > 0) {
                        Surface(
                            onClick = onOpenSms,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                BadgedBox(
                                    badge = { Badge { Text(state.pendingSmsCount.toString()) } }
                                ) {
                                    Icon(
                                        Icons.Rounded.Notifications,
                                        contentDescription = "پیامک‌ها",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        onClick = onOpenAssistant,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = "دستیار مالی",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenSettings,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = "تنظیمات",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            MonthSwitcher(
                period = state.period,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onGoToCurrentMonth = onGoToCurrentMonth,
                currentPeriod = MonthPeriod.current(),
            )
        }
        item {
            BalanceHero(
                balanceToman = state.totalBalanceToman,
                monthExpenseToman = report?.totalExpenseToman ?: 0,
                monthIncomeToman = report?.totalIncomeToman ?: 0,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                QuickAction(
                    label = "ثبت صوتی",
                    icon = Icons.Rounded.Mic,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onOpenVoice,
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    label = "هزینه",
                    icon = Icons.AutoMirrored.Rounded.TrendingDown,
                    color = LocalHesabiSemantic.current.expense,
                    onClick = { onAdd(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    label = "درآمد",
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    color = LocalHesabiSemantic.current.income,
                    onClick = { onAdd(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    label = "انتقال",
                    icon = Icons.Rounded.SwapHoriz,
                    color = LocalHesabiSemantic.current.transfer,
                    onClick = { onAdd(TransactionType.TRANSFER) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            MonthlySalaryLiquidityCard(
                forecast = state.liquidityForecast,
                onSetupSalary = onOpenSalarySetup,
            )
        }
        if (state.safeToSpend != null) {
            item {
                DailySafeToSpendWidget(
                    safeToSpend = state.safeToSpend,
                    onClick = onOpenBudget,
                )
            }
        }
        state.forecast?.let { forecast ->
            item {
                ForecastCard(forecast = forecast)
            }
        }
        if (state.pendingSmsCount > 0) {
            item {
                PendingSmsBanner(count = state.pendingSmsCount, onClick = onOpenSms)
            }
        }
        item {
            SavingsGoalsSection(
                goals = state.savingsGoals,
                onAddGoal = onAddGoal,
                onGoalClick = onGoalClick,
            )
        }
        if (report != null) {
            item {
                BudgetProgressCard(
                    spentToman = report.budgetSpentToman,
                    limitToman = report.budgetLimitToman,
                    title = "بودجهٔ ${report.period.label}",
                    onEdit = onOpenBudget,
                )
            }
        }
        item {
            SectionHeader(
                title = "تراکنش‌های اخیر",
                actionLabel = if (state.recent.isNotEmpty()) "همه" else null,
                onAction = if (state.recent.isNotEmpty()) onSeeAll else null,
            )
        }
        if (state.recent.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    title = "شروع کن!",
                    message = "اولین هزینه یا درآمدت را ثبت کن تا گزارش ماه ساخته شود.",
                    actionLabel = "ثبت تراکنش",
                    onAction = { onAdd(TransactionType.EXPENSE) },
                )
            }
        } else {
            items(count = state.recent.size, key = { state.recent[it].id }) { index ->
                val transaction = state.recent[index]
                val category = state.categories[transaction.categoryId]
                val account =
                    state.balances.firstOrNull { it.account.id == transaction.accountId }?.account
                val toAccount =
                    state.balances.firstOrNull { it.account.id == transaction.toAccountId }?.account
                TransactionRow(
                    transaction = transaction,
                    categoryName = category?.name ?: "بدون دسته",
                    iconKey = category?.iconKey,
                    accountName = account?.name ?: "—",
                    toAccountName = toAccount?.name,
                    onClick = { onTransactionClick(transaction.id) },
                    showDay = true,
                )
            }
        }
        item {
            Text(
                text = "همهٔ داده‌ها فقط روی همین گوشی ذخیره می‌شوند.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(76.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.1f),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(30.dp)) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(HesabiSpacing.xs))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(name = "خانه", heightDp = 950, showBackground = true)
@Composable
private fun HomePreview() {
    val report = remember {
        val period = MonthPeriod.of(PreviewData.today)
        ReportCalculator.build(
            period = period,
            transactions = PreviewData.transactions,
            categories = PreviewData.categories,
            previousTransactions = PreviewData.transactions.map {
                it.copy(occurredOnEpochDay = it.occurredOnEpochDay - 30)
            },
            budgets = emptyList(),
        ).copy(budgetLimitToman = 6_000_000, budgetSpentToman = 3_200_000)
    }
    val state = HomeUiState(
        period = MonthPeriod.of(PreviewData.today),
        report = report,
        recent = PreviewData.transactions.take(5),
        categories = PreviewData.categories.associateBy { it.id },
        balances = PreviewData.balances,
        pendingSmsCount = 2,
        loading = false,
        monthlySalaryToman = 25_000_000L,
        liquidityForecast = com.braveboy.hesabi.domain.logic.LiquidityForecast(
            monthlySalaryToman = 25_000_000L,
            fixedCommitmentsToman = 5_000_000L,
            netDisposableToman = 16_800_000L,
            daysRemaining = 18,
            dailyRunwayToman = 933_000L,
            runwayStatus = com.braveboy.hesabi.domain.logic.BurnRateStatus.NORMAL,
        ),
    )
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.LIGHT) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                HomeContent(
                    state = state,
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onGoToCurrentMonth = {},
                    onAdd = {},
                    onSeeAll = {},
                    onTransactionClick = {},
                    onOpenSms = {},
                    onOpenBudget = {},
                    onOpenSettings = {},
                    onOpenSalarySetup = {},
                )
            }
        }
    }
}

@Preview(
    name = "خانه - خالی",
    heightDp = 900,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun HomeEmptyPreview() {
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                HomeContent(
                    state = HomeUiState(
                        period = MonthPeriod.current(),
                        recent = emptyList(),
                        categories = PreviewData.categories.filter { it.kind == CategoryKind.EXPENSE }
                            .associateBy { it.id },
                        balances = PreviewData.balances,
                        pendingSmsCount = 0,
                        loading = false,
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onGoToCurrentMonth = {},
                    onAdd = {},
                    onSeeAll = {},
                    onTransactionClick = {},
                    onOpenSms = {},
                    onOpenBudget = {},
                    onOpenSettings = {},
                    onOpenSalarySetup = {},
                )
            }
        }
    }
}
