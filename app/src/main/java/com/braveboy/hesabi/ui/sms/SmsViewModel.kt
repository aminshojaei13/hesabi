package com.braveboy.hesabi.ui.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AppSettings
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.SmsEvent
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.IngestOutcome
import com.braveboy.hesabi.domain.repository.SettingsRepository
import com.braveboy.hesabi.domain.repository.SmsAcceptInput
import com.braveboy.hesabi.domain.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmsUiState(
    val pending: List<SmsEvent> = emptyList(),
    val recent: List<SmsEvent> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val loading: Boolean = true,
) {
    val defaultAccount: Account? get() = accounts.firstOrNull { it.id == settings.defaultAccountId } ?: accounts.firstOrNull()
}

class SmsViewModel(
    private val smsRepository: SmsRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val notice = MutableStateFlow<Pair<String?, String?>>(null to null)
    private val busy = MutableStateFlow(false)

    private data class Inputs(
        val pending: List<SmsEvent>,
        val recent: List<SmsEvent>,
        val accounts: List<Account>,
        val categories: List<Category>,
        val settings: AppSettings,
    )

    private val inputs = combine(
        smsRepository.observePending(),
        smsRepository.observeRecent(),
        accountRepository.observeAll(),
        categoryRepository.observeAll(),
        settingsRepository.settings,
    ) { pending, recent, accounts, categories, settings ->
        Inputs(pending, recent, accounts, categories, settings)
    }

    private val content = combine(inputs, notice, busy) { data, message, working ->
        SmsUiState(
            pending = data.pending,
            recent = data.recent,
            accounts = data.accounts,
            categories = data.categories,
            settings = data.settings,
            busy = working,
            message = message.first,
            error = message.second,
            loading = false,
        )
    }

    val state: StateFlow<SmsUiState> = content.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SmsUiState(),
    )

    fun importText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            busy.value = true
            val outcomes = smsRepository.importText(text, JalaliDate.today().toEpochDay())
            val added = outcomes.count { it is IngestOutcome.Added }
            val duplicates = outcomes.count { it is IngestOutcome.Duplicate }
            val ignored = outcomes.count { it is IngestOutcome.Ignored }
            notice.value = when {
                added > 0 -> "${added} پیامک برای بررسی اضافه شد." to null
                duplicates > 0 && ignored > 0 -> "همهٔ پیامک‌ها تکراری یا غیرمالی بودند." to null
                duplicates > 0 -> "این پیامک قبلاً ثبت شده بود." to null
                else -> null to "پیامک قابل خواندن نبود. متن کامل بانکی را بچسبان."
            }
            busy.value = false
        }
    }

    fun accept(
        event: SmsEvent,
        type: TransactionType,
        amountToman: Long,
        accountId: String,
        categoryId: String?,
        occurredOnEpochDay: Long,
        note: String,
    ) {
        viewModelScope.launch {
            busy.value = true
            val accepted = smsRepository.acceptAll(
                listOf(
                    SmsAcceptInput(
                        eventId = event.id,
                        type = type,
                        amountToman = amountToman,
                        accountId = accountId,
                        categoryId = categoryId,
                        occurredOnEpochDay = occurredOnEpochDay,
                        note = note.ifBlank { event.merchant ?: "ثبت از پیامک" },
                    ),
                ),
            )
            notice.value = if (accepted > 0) {
                "پیامک به تراکنش تبدیل شد." to null
            } else {
                null to "ثبت نشد. دسته و حساب را بررسی کن."
            }
            busy.value = false
        }
    }

    fun bulkAccept() {
        val current = state.value
        val account = current.defaultAccount ?: return
        val entries = current.pending
            .filter { it.amountToman != null && it.amountToman > 0 && it.confidence >= 0.7f }
            .mapNotNull { event ->
                val type = event.type ?: return@mapNotNull null
                val kind = if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
                val category = current.categories.firstOrNull { it.kind == kind }
                SmsAcceptInput(
                    eventId = event.id,
                    type = type,
                    amountToman = event.amountToman ?: return@mapNotNull null,
                    accountId = account.id,
                    categoryId = category?.id,
                    occurredOnEpochDay = event.occurredOnEpochDay,
                    note = event.merchant ?: "ثبت گروهی از پیامک",
                )
            }
        if (entries.isEmpty()) {
            notice.value = null to "پیامک با اطمینان کافی برای ثبت گروهی وجود ندارد."
            return
        }
        viewModelScope.launch {
            busy.value = true
            val accepted = smsRepository.acceptAll(entries)
            notice.value = "${accepted} پیامک ثبت شد. بقیه را دستی بررسی کن." to null
            busy.value = false
        }
    }

    fun ignore(event: SmsEvent) {
        viewModelScope.launch { smsRepository.ignore(event.id) }
    }

    fun ignoreSender(sender: String) {
        viewModelScope.launch {
            smsRepository.ignoreSender(sender)
            notice.value = "از این به بعد پیامک‌های «$sender» نادیده گرفته می‌شود." to null
        }
    }

    fun delete(event: SmsEvent) {
        viewModelScope.launch { smsRepository.delete(event.id) }
    }

    fun clearAll() {
        viewModelScope.launch {
            smsRepository.clearAll()
            notice.value = "تاریخچهٔ پیامک پاک شد." to null
        }
    }

    fun setSmsCapture(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSmsCaptureEnabled(enabled) }
    }

    fun setAmountsAreRial(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAmountsAreRial(enabled) }
    }

    fun dismissNotice() {
        notice.value = null to null
    }

    fun showMessage(text: String) {
        notice.value = text to null
    }

    fun showError(text: String) {
        notice.value = null to text
    }
}
