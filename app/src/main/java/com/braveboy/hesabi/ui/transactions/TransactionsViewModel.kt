package com.braveboy.hesabi.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionFilter(
    val period: MonthPeriod = MonthPeriod.current(),
    val allPeriods: Boolean = false,
    val type: TransactionType? = null,
    val query: String = "",
)

data class TransactionsUiState(
    val filter: TransactionFilter = TransactionFilter(),
    val transactions: List<Transaction> = emptyList(),
    val categories: Map<String, Category> = emptyMap(),
    val accounts: Map<String, Account> = emptyMap(),
    val totalExpenseToman: Long = 0,
    val totalIncomeToman: Long = 0,
    val lastDeleted: Transaction? = null,
    val loading: Boolean = true,
) {
    val hasAnyTransaction: Boolean get() = transactions.isNotEmpty()
}

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter())
    private val lastDeleted = MutableStateFlow<Transaction?>(null)

    private data class Inputs(
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val accounts: List<Account>,
    )

    private val inputs = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        accountRepository.observeAll(),
    ) { transactions, categories, accounts -> Inputs(transactions, categories, accounts) }

    val state: StateFlow<TransactionsUiState> = combine(
        filter,
        inputs,
        lastDeleted,
    ) { currentFilter, data, deleted ->
        val categoryById = data.categories.associateBy { it.id }
        val accountById = data.accounts.associateBy { it.id }
        val query = Digits.toAscii(currentFilter.query).trim()
        val filtered = data.transactions.filter { tx ->
            val periodOk = currentFilter.allPeriods || currentFilter.period.contains(tx.occurredOnEpochDay)
            val typeOk = currentFilter.type == null || tx.type == currentFilter.type
            val queryOk = query.isEmpty() || run {
                val categoryName = categoryById[tx.categoryId]?.name.orEmpty()
                val accountName = accountById[tx.accountId]?.name.orEmpty()
                val toName = accountById[tx.toAccountId]?.name.orEmpty()
                val haystack = Digits.toAscii("${tx.note} $categoryName $accountName $toName").lowercase()
                haystack.contains(query.lowercase())
            }
            periodOk && typeOk && queryOk
        }.sortedWith(
            compareByDescending<Transaction> { it.occurredOnEpochDay }.thenByDescending { it.createdAtMillis },
        )
        TransactionsUiState(
            filter = currentFilter,
            transactions = filtered,
            categories = categoryById,
            accounts = accountById,
            totalExpenseToman = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountToman },
            totalIncomeToman = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amountToman },
            lastDeleted = deleted,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun setQuery(query: String) {
        filter.value = filter.value.copy(query = query.take(60))
    }

    fun setType(type: TransactionType?) {
        filter.value = filter.value.copy(type = type)
    }

    fun toggleAllPeriods() {
        filter.value = filter.value.copy(allPeriods = !filter.value.allPeriods)
    }

    fun previousMonth() {
        filter.value = filter.value.copy(period = filter.value.period.previous(), allPeriods = false)
    }

    fun nextMonth() {
        filter.value = filter.value.copy(period = filter.value.period.next(), allPeriods = false)
    }

    fun currentMonth() {
        filter.value = TransactionFilter(period = MonthPeriod.current())
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.delete(transaction.id)
            lastDeleted.value = transaction
        }
    }

    fun undoDelete() {
        val transaction = lastDeleted.value ?: return
        viewModelScope.launch {
            transactionRepository.save(transaction)
            lastDeleted.value = null
        }
    }

    fun dismissUndo() {
        lastDeleted.value = null
    }
}
