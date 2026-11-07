package com.braveboy.hesabi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.ui.accounts.AccountsViewModel
import com.braveboy.hesabi.ui.backup.BackupViewModel
import com.braveboy.hesabi.ui.budget.BudgetViewModel
import com.braveboy.hesabi.ui.categories.CategoriesViewModel
import com.braveboy.hesabi.ui.entry.AddEditViewModel
import com.braveboy.hesabi.ui.home.HomeViewModel
import com.braveboy.hesabi.ui.reports.ReportsViewModel
import com.braveboy.hesabi.ui.settings.SettingsViewModel
import com.braveboy.hesabi.ui.sms.SmsViewModel
import com.braveboy.hesabi.ui.transactions.TransactionsViewModel

/**
 * Explicit, testable wiring without a DI framework. The container is the only
 * source of dependencies, so swapping a repository in tests is a one-liner.
 */
class HesabiViewModelFactory(
    private val container: AppContainer,
    private val transactionId: String? = null,
    private val initialType: com.braveboy.hesabi.domain.model.TransactionType? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel: ViewModel = when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(
                transactionRepository = container.transactionRepository,
                categoryRepository = container.categoryRepository,
                accountRepository = container.accountRepository,
                budgetRepository = container.budgetRepository,
                smsRepository = container.smsRepository,
                savingsGoalRepository = container.savingsGoalRepository,
                settingsRepository = container.settingsRepository,
            )
            modelClass.isAssignableFrom(AddEditViewModel::class.java) -> AddEditViewModel(
                transactionId = transactionId,
                initialType = initialType,
                transactionRepository = container.transactionRepository,
                accountRepository = container.accountRepository,
                categoryRepository = container.categoryRepository,
                settingsRepository = container.settingsRepository,
            )
            modelClass.isAssignableFrom(TransactionsViewModel::class.java) -> TransactionsViewModel(
                transactionRepository = container.transactionRepository,
                categoryRepository = container.categoryRepository,
                accountRepository = container.accountRepository,
            )
            modelClass.isAssignableFrom(ReportsViewModel::class.java) -> ReportsViewModel(
                transactionRepository = container.transactionRepository,
                categoryRepository = container.categoryRepository,
                budgetRepository = container.budgetRepository,
            )
            modelClass.isAssignableFrom(AccountsViewModel::class.java) -> AccountsViewModel(
                accountRepository = container.accountRepository,
                transactionRepository = container.transactionRepository,
            )
            modelClass.isAssignableFrom(CategoriesViewModel::class.java) -> CategoriesViewModel(
                categoryRepository = container.categoryRepository,
                transactionRepository = container.transactionRepository,
            )
            modelClass.isAssignableFrom(BudgetViewModel::class.java) -> BudgetViewModel(
                budgetRepository = container.budgetRepository,
                transactionRepository = container.transactionRepository,
                categoryRepository = container.categoryRepository,
            )
            modelClass.isAssignableFrom(SmsViewModel::class.java) -> SmsViewModel(
                smsRepository = container.smsRepository,
                accountRepository = container.accountRepository,
                categoryRepository = container.categoryRepository,
                settingsRepository = container.settingsRepository,
            )
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                settingsRepository = container.settingsRepository,
                accountRepository = container.accountRepository,
                clearAllData = container::clearAllData,
            )
            modelClass.isAssignableFrom(BackupViewModel::class.java) -> BackupViewModel(
                backupRepository = container.backupRepository,
                settingsRepository = container.settingsRepository,
            )
            modelClass.isAssignableFrom(com.braveboy.hesabi.ui.voice.VoiceEntryViewModel::class.java) -> com.braveboy.hesabi.ui.voice.VoiceEntryViewModel(
                transactionRepository = container.transactionRepository,
                accountRepository = container.accountRepository,
                categoryRepository = container.categoryRepository,
                settingsRepository = container.settingsRepository,
            )
            else -> error("ViewModel not registered: ${modelClass.name}")
        }
        return viewModel as T
    }
}

@Composable
inline fun <reified T : ViewModel> hesabiViewModel(
    container: AppContainer,
    transactionId: String? = null,
    initialType: com.braveboy.hesabi.domain.model.TransactionType? = null,
): T {
    val factory = remember(container, transactionId, initialType) {
        HesabiViewModelFactory(container, transactionId, initialType)
    }
    return viewModel(factory = factory)
}
