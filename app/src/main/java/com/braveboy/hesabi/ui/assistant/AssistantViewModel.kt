package com.braveboy.hesabi.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.assistant.AssistantEntry
import com.braveboy.hesabi.core.assistant.FinancialContext
import com.braveboy.hesabi.core.assistant.FinancialQueryEngine
import com.braveboy.hesabi.core.assistant.PersianTransactionParser
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.logic.PaydayCycleCalculator
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AiMode
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionSource
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.BudgetRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.SavingsGoalRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromUser: Boolean,
    val text: String,
    val transactionIds: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
)

data class AssistantUiState(
    val messages: List<AssistantMessage> = listOf(
        AssistantMessage(
            fromUser = false,
            text = "سلام! می‌توانی هزینه را با جمله بگویی («دیروز ۲۵۰ هزار تومان تاکسی دادم») " +
                "یا یک سؤال مالی بپرسی («این ماه چقدر خرج کردم؟»).",
            suggestions = listOf(
                "این ماه چقدر خرج کردم؟",
                "بیشترین هزینهٔ این ماه کدام دسته بود؟",
                "امروز چقدر می‌توانم خرج کنم؟",
            ),
        ),
    ),
    val input: String = "",
    val pendingEntry: AssistantEntry? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val aiMode: AiMode = AiMode.LOCAL,
    val busy: Boolean = false,
    val saving: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
)

/**
 * Local-first assistant. Parsing and answering happen in pure Kotlin on the device;
 * the cloud switch only declares intent for a future release and never uploads text.
 */
class AssistantViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = accountRepository.getAll()
            val categories = categoryRepository.getAll()
            val settings = settingsRepository.current()
            _state.update {
                it.copy(accounts = accounts, categories = categories, aiMode = settings.aiMode, loading = false)
            }
        }
    }

    fun setInput(text: String) {
        _state.update { it.copy(input = text.take(500)) }
    }

    fun setAiMode(mode: AiMode) {
        viewModelScope.launch { settingsRepository.setAiMode(mode) }
        _state.update { it.copy(aiMode = mode) }
    }

    fun submit(text: String = _state.value.input) {
        val question = text.trim()
        if (question.isBlank() || _state.value.busy) return
        _state.update {
            it.copy(
                input = "",
                busy = true,
                error = null,
                messages = it.messages + AssistantMessage(fromUser = true, text = question),
            )
        }
        viewModelScope.launch {
            runCatching {
                val allTransactions = transactionRepository.getAll()
                val categories = categoryRepository.getAll()
                val accounts = accountRepository.getAll()
                val entry = PersianTransactionParser.parse(
                    text = question,
                    categories = categories,
                    history = allTransactions,
                    today = JalaliDate.today(),
                )
                if (entry != null && entry.amountToman > 0) {
                    _state.update { current ->
                        current.copy(
                            pendingEntry = entry,
                            accounts = accounts,
                            categories = categories,
                            busy = false,
                            messages = current.messages + AssistantMessage(
                                fromUser = false,
                                text = "این پیشنهاد را از جملهٔ تو ساختم. تأیید کن تا ثبت شود:\n" +
                                    entry.explanation.joinToString("\n• ", prefix = "• "),
                            ),
                        )
                    }
                } else {
                    val context = buildContext(allTransactions, categories, accounts)
                    val answer = FinancialQueryEngine.ask(question, context)
                    _state.update { current ->
                        current.copy(
                            busy = false,
                            accounts = accounts,
                            categories = categories,
                            messages = current.messages + AssistantMessage(
                                fromUser = false,
                                text = answer.text,
                                transactionIds = answer.transactionIds,
                                suggestions = answer.suggestions,
                            ),
                        )
                    }
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(busy = false, error = "پردازش انجام نشد: ${error.message.orEmpty()}")
                }
            }
        }
    }

    fun quickSuggestion(text: String) = submit(text)

    fun updatePendingAmount(text: String) {
        val amount = Money.parseToman(text) ?: return
        _state.update { current ->
            current.copy(pendingEntry = current.pendingEntry?.copy(amountToman = amount))
        }
    }

    fun updatePendingType(type: TransactionType) {
        _state.update { current ->
            val entry = current.pendingEntry ?: return@update current
            val category = if (type == TransactionType.TRANSFER) {
                null
            } else {
                current.categories.firstOrNull {
                    it.kind == if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
                }?.id
            }
            current.copy(pendingEntry = entry.copy(type = type, categoryId = category))
        }
    }

    fun updatePendingCategory(category: Category) {
        _state.update { current ->
            current.copy(
                pendingEntry = current.pendingEntry?.copy(
                    categoryId = category.id,
                    categoryName = category.name,
                ),
            )
        }
    }

    fun updatePendingDate(date: JalaliDate) {
        _state.update { current ->
            current.copy(pendingEntry = current.pendingEntry?.copy(occurredOnEpochDay = date.toEpochDay()))
        }
    }

    fun dismissPending() {
        _state.update { it.copy(pendingEntry = null) }
    }

    fun confirmPending(accountId: String, toAccountId: String?) {
        val entry = _state.value.pendingEntry ?: return
        if (_state.value.saving) return
        if (entry.amountToman <= 0) {
            _state.update { it.copy(error = "مبلغ معتبر نیست.") }
            return
        }
        if (entry.type == TransactionType.TRANSFER && (toAccountId.isNullOrBlank() || toAccountId == accountId)) {
            _state.update { it.copy(error = "برای انتقال، دو حساب متفاوت لازم است.") }
            return
        }
        if (entry.type != TransactionType.TRANSFER && entry.categoryId.isNullOrBlank()) {
            _state.update { it.copy(error = "دسته را انتخاب کن.") }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                type = entry.type,
                amountToman = entry.amountToman,
                accountId = accountId,
                toAccountId = toAccountId.takeIf { entry.type == TransactionType.TRANSFER },
                categoryId = entry.categoryId.takeIf { entry.type != TransactionType.TRANSFER },
                occurredOnEpochDay = entry.occurredOnEpochDay,
                note = entry.note.take(200),
                source = TransactionSource.ASSISTANT,
            )
            runCatching { transactionRepository.save(transaction) }
                .onSuccess {
                    val message = "ثبت شد: ${entry.type.label} ${Money.format(entry.amountToman)}" +
                        " در دستهٔ «${entry.categoryName ?: "بدون دسته"}»."
                    _state.update { current ->
                        current.copy(
                            saving = false,
                            pendingEntry = null,
                            messages = current.messages + AssistantMessage(fromUser = false, text = message),
                        )
                    }
                    settingsRepository.setLastAssistantUseAt(System.currentTimeMillis())
                }
                .onFailure { error ->
                    _state.update { it.copy(saving = false, error = "ثبت نشد: ${error.message.orEmpty()}") }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun buildContext(
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>,
    ): FinancialContext {
        val settings = settingsRepository.current()
        val period = MonthPeriod.current()
        val today = JalaliDate.today()
        val budgets = budgetRepository.getForPeriod(period.key)
        val goals = savingsGoalRepository.getAll()
        val balances = accountRepository.observeWithBalances().first()
        val totalBalance = balances.filterNot { it.account.archived }.sumOf { it.balanceToman }
        val cycle = PaydayCycleCalculator.calculateCycle(today, settings.paydayDayOfMonth)
        val cycleExpenses = transactions
            .filter {
                it.type == TransactionType.EXPENSE &&
                    it.occurredOnEpochDay >= cycle.startEpochDay &&
                    it.occurredOnEpochDay < cycle.endEpochDayExclusive
            }
            .sumOf { it.amountToman }
        val spentToday = transactions
            .filter { it.type == TransactionType.EXPENSE && it.occurredOnEpochDay == today.toEpochDay() }
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
        return FinancialContext(
            period = period,
            today = today,
            transactions = transactions,
            categories = categories,
            totalBalanceToman = totalBalance,
            safeToSpend = safe,
            budgets = budgets,
            goals = goals,
        )
    }
}
