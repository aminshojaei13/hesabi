package com.braveboy.hesabi.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountKind
import com.braveboy.hesabi.domain.model.AccountWithBalance
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class AccountsUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val message: String? = null,
    val error: String? = null,
    val loading: Boolean = true,
)

class AccountsViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val notice = MutableStateFlow<Pair<String?, String?>>(null to null)

    val state: StateFlow<AccountsUiState> = combine(
        accountRepository.observeWithBalances(),
        notice,
    ) { accounts, message ->
        AccountsUiState(
            accounts = accounts,
            message = message.first,
            error = message.second,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun save(
        id: String?,
        name: String,
        kind: AccountKind,
        colorArgb: Long,
        openingBalanceToman: Long,
        archived: Boolean = false,
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            notice.value = null to "نام حساب را بنویس."
            return
        }
        viewModelScope.launch {
            val existing = id?.let { accountRepository.getById(it) }
            val all = accountRepository.getAll()
            val account = Account(
                id = existing?.id ?: "acc_" + UUID.randomUUID().toString().take(8),
                name = cleanName,
                kind = kind,
                colorArgb = colorArgb,
                openingBalanceToman = openingBalanceToman,
                archived = archived,
                sortOrder = existing?.sortOrder ?: ((all.maxOfOrNull { it.sortOrder } ?: -1) + 1),
            )
            accountRepository.save(account)
            notice.value = if (existing == null) "حساب ساخته شد." to null else "حساب به‌روز شد." to null
        }
    }

    fun setArchived(account: Account, archived: Boolean) {
        viewModelScope.launch {
            accountRepository.save(account.copy(archived = archived))
        }
    }

    fun delete(account: Account) {
        viewModelScope.launch {
            val usage = transactionRepository.countForAccount(account.id)
            if (usage > 0) {
                notice.value = null to "این حساب در ${usage} تراکنش استفاده شده و قابل حذف نیست. آرشیوش کن."
            } else {
                accountRepository.delete(account.id)
                notice.value = "حساب حذف شد." to null
            }
        }
    }

    fun dismissNotice() {
        notice.value = null to null
    }
}
