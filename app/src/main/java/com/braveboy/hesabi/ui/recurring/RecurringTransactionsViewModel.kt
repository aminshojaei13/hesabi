package com.braveboy.hesabi.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.recurring.RecurringTransactionProcessor
import com.braveboy.hesabi.domain.logic.RecurringFrequencyCalculator
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.RecurringFrequency
import com.braveboy.hesabi.domain.model.RecurringTransaction
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class RecurringUiState(
    val items: List<RecurringTransaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val todayEpochDay: Long = JalaliDate.today().toEpochDay(),
    val message: String? = null,
    val error: String? = null,
    val loading: Boolean = true,
) {
    val activeItems: List<RecurringTransaction> get() = items.filter { it.isActive }
    val dueItems: List<RecurringTransaction> get() = activeItems.filter { it.nextOccurrenceEpochDay <= todayEpochDay }
    val monthlyCommitmentToman: Long
        get() = activeItems.filter { it.type == TransactionType.EXPENSE }.sumOf {
            when (it.frequency) {
                RecurringFrequency.DAILY -> it.amountToman * 30L / it.interval.coerceAtLeast(1)
                RecurringFrequency.WEEKLY -> it.amountToman * 4L / it.interval.coerceAtLeast(1)
                RecurringFrequency.MONTHLY -> it.amountToman / it.interval.coerceAtLeast(1)
                RecurringFrequency.YEARLY -> it.amountToman / (12L * it.interval.coerceAtLeast(1))
            }
        }
}

sealed interface RecurringIntent {
    data class Save(
        val id: String?,
        val title: String,
        val type: TransactionType,
        val amountToman: Long,
        val accountId: String,
        val toAccountId: String?,
        val categoryId: String?,
        val frequency: RecurringFrequency,
        val interval: Int,
        val dayOfMonth: Int?,
        val startEpochDay: Long,
        val autoApply: Boolean,
        val note: String,
    ) : RecurringIntent
    data class ToggleActive(val id: String, val isActive: Boolean) : RecurringIntent
    data class ApplyNow(val recurring: RecurringTransaction) : RecurringIntent
    data class Delete(val id: String) : RecurringIntent
    data object ProcessDue : RecurringIntent
    data object DismissNotice : RecurringIntent
}

class RecurringTransactionsViewModel(
    private val recurringRepo: RecurringTransactionRepository,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val processor: RecurringTransactionProcessor,
) : ViewModel() {

    private val notice = MutableStateFlow<Pair<String?, String?>>(null to null)

    val state: StateFlow<RecurringUiState> = combine(
        recurringRepo.observeAll(),
        accountRepo.observeAll(),
        categoryRepo.observeAll(),
        notice,
    ) { items, accounts, categories, n ->
        RecurringUiState(
            items = items,
            accounts = accounts,
            categories = categories,
            todayEpochDay = JalaliDate.today().toEpochDay(),
            message = n.first,
            error = n.second,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUiState())

    init {
        onIntent(RecurringIntent.ProcessDue)
    }

    fun onIntent(intent: RecurringIntent) {
        when (intent) {
            is RecurringIntent.Save -> save(intent)
            is RecurringIntent.ToggleActive -> toggleActive(intent.id, intent.isActive)
            is RecurringIntent.ApplyNow -> applyNow(intent.recurring)
            is RecurringIntent.Delete -> delete(intent.id)
            is RecurringIntent.ProcessDue -> processDue()
            is RecurringIntent.DismissNotice -> notice.value = null to null
        }
    }

    private fun save(intent: RecurringIntent.Save) {
        val cleanTitle = intent.title.trim()
        if (cleanTitle.isEmpty()) {
            notice.value = null to "عنوان تراکنش دوره‌ای را وارد کن."
            return
        }
        if (intent.amountToman <= 0) {
            notice.value = null to "مبلغ باید بیشتر از صفر باشد."
            return
        }
        viewModelScope.launch {
            val existing = intent.id?.let { recurringRepo.getById(it) }
            val nextEpochDay = existing?.nextOccurrenceEpochDay ?: intent.startEpochDay
            val item = RecurringTransaction(
                id = intent.id ?: "rec_" + UUID.randomUUID().toString().take(8),
                title = cleanTitle,
                type = intent.type,
                amountToman = intent.amountToman,
                accountId = intent.accountId,
                toAccountId = intent.toAccountId,
                categoryId = intent.categoryId,
                frequency = intent.frequency,
                interval = intent.interval.coerceAtLeast(1),
                dayOfMonth = intent.dayOfMonth,
                nextOccurrenceEpochDay = nextEpochDay,
                lastAppliedEpochDay = existing?.lastAppliedEpochDay,
                autoApply = intent.autoApply,
                isActive = existing?.isActive ?: true,
                note = intent.note.trim(),
                createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
            )
            recurringRepo.save(item)
            notice.value = (if (existing == null) "تراکنش دوره‌ای ذخیره شد." else "تغییرات ذخیره شد.") to null
        }
    }

    private fun toggleActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            recurringRepo.toggleActive(id, isActive)
            notice.value = (if (isActive) "تراکنش فعال شد." else "تراکنش متوقف شد.") to null
        }
    }

    private fun applyNow(recurring: RecurringTransaction) {
        viewModelScope.launch {
            val today = JalaliDate.today().toEpochDay()
            processor.applyRecurringTransaction(recurring, today)
            notice.value = "تراکنش برای امروز ثبت گردید و سررسید بعدی محاسبه شد." to null
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            recurringRepo.delete(id)
            notice.value = "تراکنش دوره‌ای حذف شد." to null
        }
    }

    private fun processDue() {
        viewModelScope.launch {
            val today = JalaliDate.today().toEpochDay()
            val applied = processor.processDueTransactions(today)
            if (applied > 0) {
                notice.value = "$applied تراکنش دوره‌ای موعد رسیده خودکار ثبت شد." to null
            }
        }
    }
}
