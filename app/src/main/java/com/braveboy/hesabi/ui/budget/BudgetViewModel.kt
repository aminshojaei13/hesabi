package com.braveboy.hesabi.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.logic.EnvelopeBudgetCalculator
import com.braveboy.hesabi.domain.logic.EnvelopeStatus
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType
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
import kotlinx.coroutines.launch
import java.util.UUID

data class BudgetLine(
    val category: Category,
    val budget: Budget?,
    val spentToman: Long,
    val envelope: EnvelopeStatus? = null,
)

data class BudgetUiState(
    val period: MonthPeriod = MonthPeriod.current(),
    val overallBudget: Budget? = null,
    val overallSpentToman: Long = 0,
    val overallEnvelope: EnvelopeStatus? = null,
    val lines: List<BudgetLine> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val period = MutableStateFlow(MonthPeriod.current())

    private data class Inputs(
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val budgets: List<Budget>,
    )

    private val inputs = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        period.flatMapLatest { budgetRepository.observeForPeriod(it.key) },
    ) { transactions, categories, budgets -> Inputs(transactions, categories, budgets) }

    val state: StateFlow<BudgetUiState> = combine(period, inputs) { current, data ->
        val inMonth = data.transactions.filter { current.contains(it.occurredOnEpochDay) }
        val overallSpent = inMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountToman }
        val expenseCategories = data.categories
            .filter { it.kind == CategoryKind.EXPENSE && !it.archived }
            .sortedBy { it.sortOrder }
        val lines = expenseCategories.map { category ->
            val budget = data.budgets.firstOrNull { it.categoryId == category.id }
            BudgetLine(
                category = category,
                budget = budget,
                spentToman = inMonth
                    .filter { it.type == TransactionType.EXPENSE && it.categoryId == category.id }
                    .sumOf { it.amountToman },
                envelope = budget?.let {
                    EnvelopeBudgetCalculator.status(it, data.transactions, current)
                },
            )
        }
        val overallBudget = data.budgets.firstOrNull { it.categoryId.isBlank() }
        BudgetUiState(
            period = current,
            overallBudget = overallBudget,
            overallSpentToman = overallSpent,
            overallEnvelope = overallBudget?.let {
                EnvelopeBudgetCalculator.status(it, data.transactions, current)
            },
            lines = lines,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun previousMonth() {
        period.value = period.value.previous()
    }

    fun nextMonth() {
        period.value = period.value.next()
    }

    fun currentMonth() {
        period.value = MonthPeriod.current()
    }

    fun saveOverall(limitToman: Long, rolloverEnabled: Boolean = false) {
        val current = state.value
        viewModelScope.launch {
            budgetRepository.save(
                Budget(
                    id = current.overallBudget?.id ?: UUID.randomUUID().toString(),
                    categoryId = "",
                    periodKey = current.period.key,
                    limitToman = limitToman.coerceAtLeast(0),
                    rolloverEnabled = rolloverEnabled,
                ),
            )
        }
    }

    fun removeOverall() {
        val budget = state.value.overallBudget ?: return
        viewModelScope.launch { budgetRepository.delete(budget.id) }
    }

    fun saveCategory(categoryId: String, limitToman: Long, rolloverEnabled: Boolean = false) {
        val current = state.value
        val existing = current.lines.firstOrNull { it.category.id == categoryId }?.budget
        viewModelScope.launch {
            if (limitToman <= 0) {
                existing?.let { budgetRepository.delete(it.id) }
            } else {
                budgetRepository.save(
                    Budget(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        categoryId = categoryId,
                        periodKey = current.period.key,
                        limitToman = limitToman,
                        rolloverEnabled = rolloverEnabled,
                    ),
                )
            }
        }
    }

    fun toggleRollover(categoryId: String) {
        val line = state.value.lines.firstOrNull { it.category.id == categoryId } ?: return
        val budget = line.budget ?: return
        viewModelScope.launch {
            budgetRepository.save(budget.copy(rolloverEnabled = !budget.rolloverEnabled))
        }
    }

    fun toggleOverallRollover() {
        val budget = state.value.overallBudget ?: return
        viewModelScope.launch {
            budgetRepository.save(budget.copy(rolloverEnabled = !budget.rolloverEnabled))
        }
    }

    fun removeCategory(categoryId: String) = saveCategory(categoryId, 0)

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch { budgetRepository.delete(budget.id) }
    }
}
