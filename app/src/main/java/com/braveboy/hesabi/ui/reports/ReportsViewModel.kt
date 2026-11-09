package com.braveboy.hesabi.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.logic.ReportCalculator
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.MonthReport
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.BudgetRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class MonthPoint(
    val label: String,
    val expenseToman: Long,
    val incomeToman: Long,
)

data class ReportsUiState(
    val period: MonthPeriod = MonthPeriod.current(),
    val report: MonthReport? = null,
    val trend: List<MonthPoint> = emptyList(),
    val transactionCount: Int = 0,
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val period = MutableStateFlow(MonthPeriod.current())

    private data class Inputs(
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val budgets: List<Budget>,
        val accounts: List<Account>,
    )

    private val inputs = combine(
        combine(
            transactionRepository.observeAll(),
            categoryRepository.observeAll(),
            period.flatMapLatest { budgetRepository.observeForPeriod(it.key) },
        ) { transactions, categories, budgets -> Triple(transactions, categories, budgets) },
        accountRepository.observeAll(),
    ) { (transactions, categories, budgets), accounts ->
        Inputs(transactions, categories, budgets, accounts)
    }

    val state: StateFlow<ReportsUiState> = combine(period, inputs) { current, data ->
        val previous = current.previous()
        val report = ReportCalculator.build(
            period = current,
            transactions = data.transactions,
            categories = data.categories,
            previousTransactions = data.transactions.filter { previous.contains(it.occurredOnEpochDay) },
            budgets = data.budgets,
        )
        val trend = (5 downTo 0).map { offset ->
            val month = if (offset == 0) current else {
                var value = current
                repeat(offset) { value = value.previous() }
                value
            }
            val expense = data.transactions
                .filter { it.type == TransactionType.EXPENSE && month.contains(it.occurredOnEpochDay) }
                .sumOf { it.amountToman }
            val income = data.transactions
                .filter { it.type == TransactionType.INCOME && month.contains(it.occurredOnEpochDay) }
                .sumOf { it.amountToman }
            MonthPoint(label = month.label, expenseToman = expense, incomeToman = income)
        }
        ReportsUiState(
            period = current,
            report = report,
            trend = trend,
            transactionCount = data.transactions.count { current.contains(it.occurredOnEpochDay) },
            transactions = data.transactions.filter { current.contains(it.occurredOnEpochDay) },
            categories = data.categories,
            accounts = data.accounts,
            budgets = data.budgets,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun previousMonth() {
        period.value = period.value.previous()
    }

    fun nextMonth() {
        period.value = period.value.next()
    }

    fun currentMonth() {
        period.value = MonthPeriod.current()
    }
}
