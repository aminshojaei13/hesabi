package com.braveboy.hesabi.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrashUiState(
    val items: List<Transaction> = emptyList(),
    val message: String? = null,
    val error: String? = null,
    val loading: Boolean = true,
) {
    val retentionDays: Int get() = 30
}

class TrashViewModel(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val notice = MutableStateFlow<Pair<String?, String?>>(null to null)

    val state: StateFlow<TrashUiState> = combine(
        transactionRepository.observeTrash(),
        notice,
    ) { items, message ->
        TrashUiState(
            items = items,
            message = message.first,
            error = message.second,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrashUiState())

    fun restore(transaction: Transaction) {
        viewModelScope.launch {
            runCatching { transactionRepository.restore(transaction.id) }
                .onSuccess { notice.value = "تراکنش بازگردانی شد." to null }
                .onFailure { notice.value = null to "بازگردانی انجام نشد." }
        }
    }

    fun deletePermanently(transaction: Transaction) {
        viewModelScope.launch {
            runCatching { transactionRepository.deletePermanently(transaction.id) }
                .onSuccess { notice.value = "برای همیشه حذف شد." to null }
                .onFailure { notice.value = null to "حذف انجام نشد." }
        }
    }

    fun dismissNotice() {
        notice.value = null to null
    }
}
