package com.braveboy.hesabi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.logic.DailySafeToSpend
import com.braveboy.hesabi.domain.logic.EndOfMonthForecast
import com.braveboy.hesabi.domain.logic.MonthForecastCalculator
import com.braveboy.hesabi.domain.logic.PaydayCycle
import com.braveboy.hesabi.domain.logic.PaydayCycleCalculator
import com.braveboy.hesabi.domain.logic.ReportCalculator
import com.braveboy.hesabi.domain.model.AccountWithBalance
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.MonthReport
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.BudgetRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.SavingsGoalRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import com.braveboy.hesabi.domain.repository.SmsRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val period: MonthPeriod = MonthPeriod.current(),
    val report: MonthReport? = null,
    val recent: List<Transaction> = emptyList(),
    val categories: Map<String, Category> = emptyMap(),
    val balances: List<AccountWithBalance> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val safeToSpend: DailySafeToSpend? = null,
    val paydayCycle: PaydayCycle? = null,
    val paydayDayOfMonth: Int = 1,
    val monthlySalaryToman: Long = 0L,
    val salaryAccountId: String? = null,
    val autoDepositSalary: Boolean = false,
    val liquidityForecast: com.braveboy.hesabi.domain.logic.LiquidityForecast? = null,
    val pendingSmsCount: Int = 0,
    val forecast: EndOfMonthForecast? = null,
    val loading: Boolean = true,
) {
    val totalBalanceToman: Long get() = balances.sumOf { it.balanceToman }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    budgetRepository: BudgetRepository,
    smsRepository: SmsRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val period = MutableStateFlow(MonthPeriod.current())
    private var followsCurrentMonth = true

    private data class Core(
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val balances: List<AccountWithBalance>,
        val budgets: List<com.braveboy.hesabi.domain.model.Budget>,
        val goals: List<SavingsGoal>,
        val paydayDay: Int,
        val monthlySalaryToman: Long = 0L,
        val salaryAccountId: String? = null,
        val autoDepositSalary: Boolean = false,
    )

    private val core = combine(
        combine(
            transactionRepository.observeAll(),
            categoryRepository.observeAll(),
            accountRepository.observeWithBalances(),
            period.flatMapLatest { budgetRepository.observeForPeriod(it.key) },
            savingsGoalRepository.observeActive(),
        ) { transactions, categories, balances, budgets, goals ->
            Core(transactions, categories, balances, budgets, goals, paydayDay = 1, monthlySalaryToman = 0L)
        },
        settingsRepository.settings,
    ) { coreData, settings ->
        coreData.copy(
            paydayDay = settings.paydayDayOfMonth,
            monthlySalaryToman = settings.monthlySalaryToman,
            salaryAccountId = settings.salaryAccountId,
            autoDepositSalary = settings.autoDepositSalary,
        )
    }

    val state: StateFlow<HomeUiState> = combine(
        period,
        core,
        smsRepository.observePendingCount(),
    ) { current, data, pendingSms ->
        val previous = current.previous()
        val report = ReportCalculator.build(
            period = current,
            transactions = data.transactions,
            categories = data.categories,
            previousTransactions = data.transactions.filter { previous.contains(it.occurredOnEpochDay) },
            budgets = data.budgets,
        )

        val today = JalaliDate.today()
        val todayEpochDay = today.toEpochDay()
        val cycle = PaydayCycleCalculator.calculateCycle(today = today, paydayDay = data.paydayDay)

        val cycleExpenses = data.transactions
            .filter { it.type == TransactionType.EXPENSE && it.occurredOnEpochDay >= cycle.startEpochDay && it.occurredOnEpochDay < cycle.endEpochDayExclusive }
            .sumOf { it.amountToman }

        val spentToday = data.transactions
            .filter { it.type == TransactionType.EXPENSE && it.occurredOnEpochDay == todayEpochDay }
            .sumOf { it.amountToman }

        val totalBalance = data.balances.filterNot { it.account.archived }.sumOf { it.balanceToman }
        val goalsAllocated = data.goals.sumOf { it.savedAmountToman }

        val safeToSpend = PaydayCycleCalculator.calculateSafeToSpend(
            totalLiquidBalanceToman = totalBalance,
            totalCycleExpenseToman = cycleExpenses,
            spentTodayToman = spentToday,
            cycle = cycle,
            monthlyBudgetLimitToman = report.budgetLimitToman,
            savingsGoalsAllocatedToman = goalsAllocated,
        )

        val liquidityForecast = if (data.monthlySalaryToman > 0) {
            PaydayCycleCalculator.calculateLiquidityForecast(
                monthlySalaryToman = data.monthlySalaryToman,
                fixedCommitmentsToman = goalsAllocated,
                currentCycleExpenseToman = cycleExpenses,
                cycle = cycle,
            )
        } else null

        val forecast = MonthForecastCalculator.forecast(
            period = current,
            today = today,
            transactions = data.transactions,
            currentBalanceToman = totalBalance,
            monthlyBudgetLimitToman = report.budgetLimitToman,
        )

        HomeUiState(
            period = current,
            report = report,
            recent = data.transactions
                .sortedWith(compareByDescending<Transaction> { it.occurredOnEpochDay }.thenByDescending { it.createdAtMillis })
                .take(6),
            categories = data.categories.associateBy { it.id },
            balances = data.balances,
            savingsGoals = data.goals,
            safeToSpend = safeToSpend,
            paydayCycle = cycle,
            paydayDayOfMonth = data.paydayDay,
            monthlySalaryToman = data.monthlySalaryToman,
            salaryAccountId = data.salaryAccountId,
            autoDepositSalary = data.autoDepositSalary,
            liquidityForecast = liquidityForecast,
            pendingSmsCount = pendingSms,
            forecast = forecast,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun previousMonth() {
        followsCurrentMonth = false
        period.value = period.value.previous()
    }

    fun nextMonth() {
        followsCurrentMonth = false
        period.value = period.value.next()
    }

    fun goToCurrentMonth() {
        followsCurrentMonth = true
        period.value = MonthPeriod.current()
    }

    /** Called on resume so crossing midnight into a new month refreshes the card (M29). */
    fun refreshIfMonthChanged() {
        if (followsCurrentMonth) {
            period.value = MonthPeriod.current()
        }
    }

    fun updateSalarySettings(
        salaryToman: Long,
        payday: Int,
        accountId: String?,
        autoDeposit: Boolean,
    ) {
        viewModelScope.launch {
            settingsRepository.setMonthlySalary(salaryToman)
            settingsRepository.setPaydayDayOfMonth(payday)
            settingsRepository.setSalaryAccountId(accountId)
            settingsRepository.setAutoDepositSalary(autoDeposit)
        }
    }

    fun saveGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsGoalRepository.save(goal)
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            savingsGoalRepository.delete(id)
        }
    }

    fun depositToGoal(goalId: String, amountToman: Long) {
        viewModelScope.launch {
            savingsGoalRepository.deposit(goalId, amountToman)
        }
    }

    fun withdrawFromGoal(goalId: String, amountToman: Long) {
        viewModelScope.launch {
            savingsGoalRepository.withdraw(goalId, amountToman)
        }
    }
}
