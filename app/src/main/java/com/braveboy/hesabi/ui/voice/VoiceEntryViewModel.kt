package com.braveboy.hesabi.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.voice.VoiceParsedTransaction
import com.braveboy.hesabi.core.voice.VoiceTransactionParser
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionSource
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class VoiceEntryUiState(
    val isListening: Boolean = false,
    val spokenText: String = "",
    val amplitudes: List<Float> = emptyList(),
    val parsed: VoiceParsedTransaction? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val overriddenAmount: Long? = null,
    val overriddenType: TransactionType? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

class VoiceEntryViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceEntryUiState())
    val state: StateFlow<VoiceEntryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val defaultAccId = settingsRepository.current().defaultAccountId
            if (defaultAccId != null) {
                _state.update { it.copy(selectedAccountId = defaultAccId) }
            }
            combine(
                accountRepository.observeAll(),
                categoryRepository.observeAll(),
            ) { accounts, categories ->
                _state.update { current ->
                    val defaultId = current.selectedAccountId ?: accounts.firstOrNull()?.id
                    current.copy(
                        accounts = accounts,
                        categories = categories,
                        selectedAccountId = defaultId,
                    )
                }
            }.collect()
        }
    }

    fun onSpeechResult(text: String) {
        val parsed = VoiceTransactionParser.parse(text)
        val matchedAccountId = parsed.accountHint?.let { hint ->
            _state.value.accounts.firstOrNull { account ->
                account.name.contains(hint, ignoreCase = true)
            }?.id
        }
        val matchedCategoryId = parsed.categoryHint?.let { hint ->
            _state.value.categories.firstOrNull { it.id == hint }?.id
        }

        _state.update { current ->
            current.copy(
                spokenText = text,
                parsed = parsed,
                selectedAccountId = matchedAccountId ?: current.selectedAccountId,
                selectedCategoryId = matchedCategoryId ?: current.selectedCategoryId,
            )
        }
    }

    fun onAmplitudeUpdate(amplitudes: List<Float>) {
        _state.update { it.copy(amplitudes = amplitudes) }
    }

    fun setListening(listening: Boolean) {
        _state.update { it.copy(isListening = listening) }
        if (!listening) {
            _state.update { it.copy(amplitudes = emptyList()) }
        }
    }

    fun overrideAmount(amount: Long) {
        _state.update { it.copy(overriddenAmount = amount) }
    }

    fun overrideType(type: TransactionType) {
        _state.update { it.copy(overriddenType = type) }
    }

    fun selectAccount(accountId: String) {
        _state.update { it.copy(selectedAccountId = accountId) }
    }

    fun selectCategory(categoryId: String) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun save() {
        val s = _state.value
        val parsed = s.parsed ?: return
        val amount = s.overriddenAmount ?: parsed.amountToman
        if (amount == null || amount <= 0L) {
            _state.update { it.copy(error = "مبلغ معتبر نیست.") }
            return
        }
        val type = s.overriddenType ?: parsed.type
        val accountId = s.selectedAccountId
        if (accountId.isNullOrBlank()) {
            _state.update { it.copy(error = "یک حساب انتخاب کنید.") }
            return
        }

        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    amountToman = amount,
                    accountId = accountId,
                    categoryId = s.selectedCategoryId,
                    occurredOnEpochDay = JalaliDate.today().toEpochDay(),
                    note = s.spokenText,
                    source = TransactionSource.VOICE,
                )
                transactionRepository.save(transaction)
                _state.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "خطا در ذخیره: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun reset() {
        _state.update { current ->
            VoiceEntryUiState(
                accounts = current.accounts,
                categories = current.categories,
                selectedAccountId = current.selectedAccountId,
            )
        }
    }
}
