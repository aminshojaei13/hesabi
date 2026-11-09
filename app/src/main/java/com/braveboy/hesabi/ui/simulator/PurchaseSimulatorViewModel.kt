package com.braveboy.hesabi.ui.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.logic.DailySafeToSpend
import com.braveboy.hesabi.domain.logic.PaydayCycleCalculator
import com.braveboy.hesabi.domain.logic.PurchaseImpact
import com.braveboy.hesabi.domain.logic.PurchaseSimulator
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.BudgetRepository
import com.braveboy.hesabi.domain.repository.SavingsGoalRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimulatorUiState(
    val amountText: String = "",
    val amountToman: Long = 0L,
    val impact: PurchaseImpact? = null,
    val safeToSpend: DailySafeToSpend? = null,
    val liquidBalanceToman: Long = 0L,
    val goals: List<SavingsGoal> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class PurchaseSimulatorViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SimulatorUiState())
    val state: StateFlow<SimulatorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
    }

    fun setAmount(text: String) {
        val amount = Money.parseToman(text) ?: 0L
        _state.update { current ->
            current.copy(
                amountText = text.take(18),
                amountToman = amount,
                impact = current.safeToSpend?.let {
                    PurchaseSimulator.simulate(
                        amountToman = amount,
                        safeToSpend = it,
                        liquidBalanceToman = current.liquidBalanceToman,
                        activeGoals = current.goals,
                    )
                },
            )
        }
    }

    fun clearAmount() {
        _state.update { it.copy(amountText = "", amountToman = 0L, impact = null) }
    }

    private suspend fun refresh() {
        runCatching {
            val settings = settingsRepository.current()
            val transactions = transactionRepository.getAll()
            val balances = accountRepository.observeWithBalances().first()
            val goals = savingsGoalRepository.getAll()
            val period = com.braveboy.hesabi.core.date.MonthPeriod.current()
            val budgets = budgetRepository.getForPeriod(period.key)
            val totalBalance = balances.filterNot { it.account.archived }.sumOf { it.balanceToman }
            val cycle = PaydayCycleCalculator.calculateCycle(JalaliDate.today(), settings.paydayDayOfMonth)
            val cycleExpenses = transactions
                .filter {
                    it.type == TransactionType.EXPENSE &&
                        it.occurredOnEpochDay >= cycle.startEpochDay &&
                        it.occurredOnEpochDay < cycle.endEpochDayExclusive
                }
                .sumOf { it.amountToman }
            val spentToday = transactions
                .filter {
                    it.type == TransactionType.EXPENSE &&
                        it.occurredOnEpochDay == JalaliDate.today().toEpochDay()
                }
                .sumOf { it.amountToman }
            val overallBudget = budgets.firstOrNull { it.categoryId.isBlank() }
            val safe = PaydayCycleCalculator.calculateSafeToSpend(
                totalLiquidBalanceToman = totalBalance,
                totalCycleExpenseToman = cycleExpenses,
                spentTodayToman = spentToday,
                cycle = cycle,
                monthlyBudgetLimitToman = overallBudget?.limitToman,
                savingsGoalsAllocatedToman = goals.sumOf { it.savedAmountToman },
            )
            Triple(safe, totalBalance, goals)
        }.onSuccess { (safe, balance, goals) ->
            _state.update { current ->
                current.copy(
                    safeToSpend = safe,
                    liquidBalanceToman = balance,
                    goals = goals,
                    loading = false,
                    impact = if (current.amountToman > 0) {
                        PurchaseSimulator.simulate(
                            amountToman = current.amountToman,
                            safeToSpend = safe,
                            liquidBalanceToman = balance,
                            activeGoals = goals,
                        )
                    } else {
                        null
                    },
                )
            }
        }.onFailure { error ->
            _state.update { it.copy(loading = false, error = error.message ?: "محاسبه انجام نشد.") }
        }
    }
}
