package com.braveboy.hesabi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.logic.ReportCalculator
import com.braveboy.hesabi.domain.model.AccountWithBalance
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.MonthReport
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.BudgetRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.SmsRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val period: MonthPeriod = MonthPeriod.current(),
    val report: MonthReport? = null,
    val recent: List<Transaction> = emptyList(),
    val categories: Map<String, Category> = emptyMap(),
    val balances: List<AccountWithBalance> = emptyList(),
    val pendingSmsCount: Int = 0,
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
) : ViewModel() {

    private val period = MutableStateFlow(MonthPeriod.current())

    private data class Core(
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val balances: List<AccountWithBalance>,
        val budgets: List<com.braveboy.hesabi.domain.model.Budget>,
    )

    private val core = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        accountRepository.observeWithBalances(),
        period.flatMapLatest { budgetRepository.observeForPeriod(it.key) },
    ) { transactions, categories, balances, budgets ->
        Core(transactions, categories, balances, budgets)
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
        HomeUiState(
            period = current,
            report = report,
            recent = data.transactions
                .sortedWith(compareByDescending<Transaction> { it.occurredOnEpochDay }.thenByDescending { it.createdAtMillis })
                .take(6),
            categories = data.categories.associateBy { it.id },
            balances = data.balances,
            pendingSmsCount = pendingSms,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun previousMonth() {
        period.value = period.value.previous()
    }

    fun nextMonth() {
        period.value = period.value.next()
    }

    fun goToCurrentMonth() {
        period.value = MonthPeriod.current()
    }
}
