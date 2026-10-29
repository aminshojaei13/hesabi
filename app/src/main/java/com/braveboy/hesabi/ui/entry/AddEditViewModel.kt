package com.braveboy.hesabi.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.domain.logic.DuplicateDetector
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionDraft
import com.braveboy.hesabi.domain.model.TransactionSource
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AddEditUiState(
    val draft: TransactionDraft = TransactionDraft(),
    val amountDigits: String = "",
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val editingId: String? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val errors: List<String> = emptyList(),
    val duplicateOf: Transaction? = null,
    val finished: Boolean = false,
    val deleted: Boolean = false,
    val source: TransactionSource = TransactionSource.MANUAL,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val externalRef: String? = null,
) {
    val isEditing: Boolean get() = editingId != null
    val filteredCategories: List<Category>
        get() = when (draft.type) {
            TransactionType.EXPENSE -> categories.filter { it.kind == CategoryKind.EXPENSE }
            TransactionType.INCOME -> categories.filter { it.kind == CategoryKind.INCOME }
            TransactionType.TRANSFER -> emptyList()
        }
    val canSave: Boolean get() = draft.amountToman > 0 && accounts.isNotEmpty()
}

class AddEditViewModel(
    private val transactionId: String?,
    private val initialType: TransactionType? = null,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditUiState(loading = true))
    val state: StateFlow<AddEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = accountRepository.getAll()
            val categories = categoryRepository.getAll()
            val settings = settingsRepository.current()
            val existing = transactionId?.let { transactionRepository.getById(it) }
            val draft = if (existing != null) {
                TransactionDraft(
                    type = existing.type,
                    amountToman = existing.amountToman,
                    accountId = existing.accountId,
                    toAccountId = existing.toAccountId,
                    categoryId = existing.categoryId,
                    occurredOnEpochDay = existing.occurredOnEpochDay,
                    note = existing.note,
                    source = existing.source,
                )
            } else {
                val type = initialType ?: TransactionType.EXPENSE
                val accountId = settings.defaultAccountId ?: accounts.firstOrNull()?.id.orEmpty()
                val kind = when (type) {
                    TransactionType.EXPENSE -> CategoryKind.EXPENSE
                    TransactionType.INCOME -> CategoryKind.INCOME
                    TransactionType.TRANSFER -> null
                }
                val category = kind?.let { wanted -> categories.firstOrNull { it.kind == wanted } }
                TransactionDraft(
                    type = type,
                    accountId = accountId,
                    toAccountId = if (type == TransactionType.TRANSFER) {
                        accounts.firstOrNull { it.id != accountId }?.id
                    } else {
                        null
                    },
                    categoryId = category?.id,
                    occurredOnEpochDay = JalaliDate.today().toEpochDay(),
                )
            }
            _state.value = AddEditUiState(
                draft = draft,
                amountDigits = draft.amountToman.takeIf { it > 0 }?.toString().orEmpty(),
                accounts = accounts,
                categories = categories,
                editingId = existing?.id,
                loading = false,
                source = existing?.source ?: TransactionSource.MANUAL,
                createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
                externalRef = existing?.externalRef,
            )
        }
    }

    fun setType(type: TransactionType) {
        _state.update { current ->
            val categories = current.categories
            val kind = when (type) {
                TransactionType.EXPENSE -> CategoryKind.EXPENSE
                TransactionType.INCOME -> CategoryKind.INCOME
                TransactionType.TRANSFER -> null
            }
            val categoryId = when {
                kind == null -> null
                categories.any { it.id == current.draft.categoryId && it.kind == kind } -> current.draft.categoryId
                else -> categories.firstOrNull { it.kind == kind }?.id
            }
            current.copy(
                draft = current.draft.copy(type = type, categoryId = categoryId),
                errors = emptyList(),
            )
        }
    }

    fun appendDigit(digit: Int) {
        _state.update { current ->
            val next = when {
                current.amountDigits.isEmpty() && digit == 0 -> ""
                current.amountDigits.length >= 13 -> current.amountDigits
                else -> current.amountDigits + digit
            }
            current.copy(amountDigits = next, draft = current.draft.copy(amountToman = next.toLongOrNull() ?: 0L), errors = emptyList())
        }
    }

    fun appendTripleZero() {
        _state.update { current ->
            if (current.amountDigits.isEmpty() || current.amountDigits.length > 10) return@update current
            val next = current.amountDigits + "000"
            current.copy(amountDigits = next, draft = current.draft.copy(amountToman = next.toLongOrNull() ?: 0L))
        }
    }

    fun backspace() {
        _state.update { current ->
            val next = current.amountDigits.dropLast(1)
            current.copy(amountDigits = next, draft = current.draft.copy(amountToman = next.toLongOrNull() ?: 0L))
        }
    }

    fun clearAmount() {
        _state.update { it.copy(amountDigits = "", draft = it.draft.copy(amountToman = 0L)) }
    }

    fun setAccount(account: Account) {
        _state.update { current ->
            val toAccount = if (current.draft.toAccountId == account.id) null else current.draft.toAccountId
            current.copy(draft = current.draft.copy(accountId = account.id, toAccountId = toAccount), errors = emptyList())
        }
    }

    fun setToAccount(account: Account) {
        _state.update { it.copy(draft = it.draft.copy(toAccountId = account.id), errors = emptyList()) }
    }

    fun setCategory(category: Category) {
        _state.update { it.copy(draft = it.draft.copy(categoryId = category.id), errors = emptyList()) }
    }

    fun setDate(date: JalaliDate) {
        _state.update { it.copy(draft = it.draft.copy(occurredOnEpochDay = date.toEpochDay())) }
    }

    fun setNote(note: String) {
        _state.update { it.copy(draft = it.draft.copy(note = note.take(200))) }
    }

    fun dismissDuplicate() {
        _state.update { it.copy(duplicateOf = null) }
    }

    fun save(force: Boolean = false) {
        val snapshot = _state.value
        if (snapshot.saving) return
        val errors = snapshot.draft.validate()
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        if (snapshot.duplicateOf != null && !force) return
        _state.update { it.copy(saving = true, errors = emptyList()) }
        viewModelScope.launch {
            if (snapshot.editingId == null && snapshot.duplicateOf == null && !force) {
                val existing = transactionRepository.getAll()
                val duplicate = DuplicateDetector.findDuplicate(snapshot.draft, existing)
                if (duplicate != null) {
                    _state.update { it.copy(saving = false, duplicateOf = duplicate) }
                    return@launch
                }
            }
            val draft = snapshot.draft
            val transaction = Transaction(
                id = snapshot.editingId ?: UUID.randomUUID().toString(),
                type = draft.type,
                amountToman = draft.amountToman,
                accountId = draft.accountId,
                toAccountId = if (draft.type == TransactionType.TRANSFER) draft.toAccountId else null,
                categoryId = if (draft.type == TransactionType.TRANSFER) null else draft.categoryId,
                occurredOnEpochDay = draft.occurredOnEpochDay,
                note = draft.note.trim(),
                source = snapshot.source,
                externalRef = snapshot.externalRef,
                createdAtMillis = snapshot.createdAtMillis,
                updatedAtMillis = System.currentTimeMillis(),
            )
            transactionRepository.save(transaction)
            _state.update { it.copy(saving = false, finished = true, duplicateOf = null) }
        }
    }

    fun delete() {
        val id = _state.value.editingId ?: return
        viewModelScope.launch {
            transactionRepository.delete(id)
            _state.update { it.copy(deleted = true, finished = true) }
        }
    }

    fun clearErrors() {
        _state.update { it.copy(errors = emptyList()) }
    }
}
