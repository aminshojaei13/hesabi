package com.braveboy.hesabi.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.AmountExpressionEvaluator
import com.braveboy.hesabi.domain.logic.DuplicateDetector
import com.braveboy.hesabi.domain.logic.SmartCategorySuggester
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.QuickTemplate
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionSource
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.QuickTemplateRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class QuickEntryUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val expression: String = "",
    val amountToman: Long = 0L,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val templates: List<QuickTemplate> = emptyList(),
    val accountId: String = "",
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val note: String = "",
    val occurredOnEpochDay: Long = JalaliDate.today().toEpochDay(),
    val suggestion: String? = null,
    val errors: List<String> = emptyList(),
    val saving: Boolean = false,
    val finished: Boolean = false,
    val loading: Boolean = true,
    val isEditing: Boolean = false,
) {
    val filteredCategories: List<Category>
        get() = when (type) {
            TransactionType.EXPENSE -> categories.filter { it.kind == CategoryKind.EXPENSE && !it.archived }
            TransactionType.INCOME -> categories.filter { it.kind == CategoryKind.INCOME && !it.archived }
            TransactionType.TRANSFER -> emptyList()
        }
    val canSave: Boolean get() = amountToman > 0 && accountId.isNotBlank()
    val displayExpression: String get() = expression.ifBlank { "0" }
}

/**
 * Two-tap entry: the amount pad and one category are enough to save. The calculator
 * lives inside the amount field and accepts `+ - × ÷ %`, and frequent entries are one
 * tap away through templates.
 */
class QuickEntryViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val quickTemplateRepository: QuickTemplateRepository,
    private val settingsRepository: SettingsRepository,
    private val initialType: TransactionType? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(QuickEntryUiState(type = initialType ?: TransactionType.EXPENSE, loading = true))
    val state: StateFlow<QuickEntryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = accountRepository.getAll()
            val categories = categoryRepository.getAll()
            val templates = quickTemplateRepository.getAll()
            val settings = settingsRepository.current()
            val type = initialType ?: TransactionType.EXPENSE
            val accountId = settings.defaultAccountId?.takeIf { id -> accounts.any { it.id == id } }
                ?: accounts.firstOrNull()?.id.orEmpty()
            _state.update {
                it.copy(
                    type = type,
                    accounts = accounts,
                    categories = categories,
                    templates = templates,
                    accountId = accountId,
                    toAccountId = accounts.firstOrNull { account -> account.id != accountId }?.id,
                    categoryId = defaultCategory(categories, type, accountId = null),
                    loading = false,
                )
            }
        }
    }

    fun append(text: String) {
        _state.update { current ->
            val next = (current.expression + text).take(32)
            current.copy(
                expression = next,
                amountToman = evaluate(next),
                errors = emptyList(),
            )
        }
    }

    fun backspace() {
        _state.update { current ->
            val next = current.expression.dropLast(1)
            current.copy(expression = next, amountToman = evaluate(next), errors = emptyList())
        }
    }

    fun clearAmount() {
        _state.update { it.copy(expression = "", amountToman = 0L, errors = emptyList()) }
    }

    fun setType(type: TransactionType) {
        _state.update { current ->
            current.copy(
                type = type,
                categoryId = defaultCategory(current.categories, type, current.categoryId),
                toAccountId = if (type == TransactionType.TRANSFER) {
                    current.toAccountId ?: current.accounts.firstOrNull { it.id != current.accountId }?.id
                } else {
                    null
                },
                errors = emptyList(),
            )
        }
    }

    fun setAccount(account: Account) {
        _state.update { current ->
            current.copy(
                accountId = account.id,
                toAccountId = current.toAccountId?.takeIf { it != account.id }
                    ?: current.accounts.firstOrNull { it.id != account.id }?.id,
                errors = emptyList(),
            )
        }
    }

    fun setToAccount(account: Account) {
        _state.update { it.copy(toAccountId = account.id) }
    }

    fun setCategory(category: Category) {
        _state.update { it.copy(categoryId = category.id, suggestion = null, errors = emptyList()) }
    }

    fun setNote(note: String) {
        _state.update { current ->
            val suggestion = if (current.type == TransactionType.TRANSFER) {
                null
            } else {
                SmartCategorySuggester.suggest(
                    text = note,
                    history = emptyList(),
                    categories = current.categories,
                    kind = if (current.type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE,
                )?.takeIf { it.categoryId != current.categoryId }?.let {
                    "دستهٔ «${it.categoryName}» پیشنهاد می‌شود."
                }
            }
            current.copy(note = note.take(200), suggestion = suggestion)
        }
    }

    fun applySuggestedCategory() {
        val current = _state.value
        val suggestion = SmartCategorySuggester.suggest(
            text = current.note,
            history = emptyList(),
            categories = current.categories,
            kind = if (current.type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE,
        ) ?: return
        setCategory(current.categories.first { it.id == suggestion.categoryId })
    }

    fun applyTemplate(template: QuickTemplate) {
        _state.update { current ->
            val category = template.categoryId?.let { id -> current.categories.firstOrNull { it.id == id } }
            val account = template.accountId?.let { id -> current.accounts.firstOrNull { it.id == id } }
            current.copy(
                type = template.type,
                expression = template.amountToman.takeIf { it > 0 }?.toString() ?: current.expression,
                amountToman = template.amountToman.takeIf { it > 0 } ?: current.amountToman,
                categoryId = category?.id ?: current.categoryId,
                accountId = account?.id ?: current.accountId,
                note = template.note.ifBlank { current.note },
                suggestion = null,
                errors = emptyList(),
            )
        }
        viewModelScope.launch { quickTemplateRepository.markUsed(template.id) }
    }

    fun saveCurrentAsTemplate(label: String) {
        val current = _state.value
        if (label.isBlank()) return
        viewModelScope.launch {
            val template = QuickTemplate(
                id = UUID.randomUUID().toString(),
                label = label.take(24),
                type = current.type,
                amountToman = current.amountToman,
                categoryId = current.categoryId,
                accountId = current.accountId,
                toAccountId = current.toAccountId,
                note = current.note,
                sortOrder = current.templates.size,
            )
            quickTemplateRepository.save(template)
            _state.update { it.copy(templates = it.templates + template) }
        }
    }

    fun deleteTemplate(template: QuickTemplate) {
        viewModelScope.launch {
            quickTemplateRepository.delete(template.id)
            _state.update { it.copy(templates = it.templates.filterNot { item -> item.id == template.id }) }
        }
    }

    fun save() {
        val snapshot = _state.value
        if (snapshot.saving) return
        val errors = buildList {
            if (snapshot.amountToman <= 0) add("مبلغ را وارد کن.")
            if (snapshot.accountId.isBlank()) add("حساب را انتخاب کن.")
            if (snapshot.type == TransactionType.TRANSFER) {
                if (snapshot.toAccountId.isNullOrBlank()) add("حساب مقصد را انتخاب کن.")
                if (snapshot.toAccountId == snapshot.accountId) add("حساب مبدأ و مقصد نباید یکی باشند.")
            } else if (snapshot.categoryId.isNullOrBlank()) {
                add("دسته را انتخاب کن.")
            }
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        _state.update { it.copy(saving = true, errors = emptyList()) }
        viewModelScope.launch {
            val duplicate = if (snapshot.type == TransactionType.TRANSFER) null else {
                DuplicateDetector.findDuplicate(
                    amountToman = snapshot.amountToman,
                    type = snapshot.type,
                    accountId = snapshot.accountId,
                    epochDay = snapshot.occurredOnEpochDay,
                    existing = transactionRepository.getAll(),
                )
            }
            if (duplicate != null) {
                _state.update {
                    it.copy(
                        saving = false,
                        errors = listOf("تراکنشی با همین مبلغ و حساب امروز ثبت شده است. اگر تکراری نیست، دوباره ثبت کن."),
                    )
                }
                return@launch
            }
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                type = snapshot.type,
                amountToman = snapshot.amountToman,
                accountId = snapshot.accountId,
                toAccountId = snapshot.toAccountId.takeIf { snapshot.type == TransactionType.TRANSFER },
                categoryId = snapshot.categoryId.takeIf { snapshot.type != TransactionType.TRANSFER },
                occurredOnEpochDay = snapshot.occurredOnEpochDay,
                note = snapshot.note.trim(),
                source = TransactionSource.QUICK_REPEAT,
            )
            runCatching { transactionRepository.save(transaction) }
                .onSuccess { _state.update { it.copy(saving = false, finished = true) } }
                .onFailure { error ->
                    _state.update {
                        it.copy(saving = false, errors = listOf("ثبت نشد؛ دوباره تلاش کن. (${error.message.orEmpty()})"))
                    }
                }
        }
    }

    private fun evaluate(expression: String): Long {
        if (expression.isBlank()) return 0
        return when (val result = AmountExpressionEvaluator.evaluate(expression)) {
            is AmountExpressionEvaluator.Result.Value -> result.tonoman
            is AmountExpressionEvaluator.Result.Invalid -> partialValue(expression)
        }
    }

    /** While the user is typing "125+", keep showing the last complete number. */
    private fun partialValue(expression: String): Long {
        val lastNumber = Regex("""\d+""").findAll(expression).lastOrNull()?.value
        return lastNumber?.toLongOrNull() ?: 0L
    }

    private fun defaultCategory(
        categories: List<Category>,
        type: TransactionType,
        accountId: String?,
    ): String? {
        val kind = when (type) {
            TransactionType.EXPENSE -> CategoryKind.EXPENSE
            TransactionType.INCOME -> CategoryKind.INCOME
            TransactionType.TRANSFER -> return null
        }
        return categories.firstOrNull { it.kind == kind && !it.archived }?.id
    }
}
