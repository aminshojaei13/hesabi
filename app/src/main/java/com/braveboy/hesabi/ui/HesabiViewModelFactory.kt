package com.braveboy.hesabi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.ui.accounts.AccountsViewModel
import com.braveboy.hesabi.ui.assistant.AssistantViewModel
import com.braveboy.hesabi.ui.annual.AnnualReportViewModel
import com.braveboy.hesabi.ui.assets.AssetsViewModel
import com.braveboy.hesabi.ui.calendar.DueCalendarViewModel
import com.braveboy.hesabi.ui.importer.ImportViewModel
import com.braveboy.hesabi.ui.simulator.PurchaseSimulatorViewModel
import com.braveboy.hesabi.ui.trash.TrashViewModel
import com.braveboy.hesabi.data.importer.TransactionImporter
import com.braveboy.hesabi.ui.backup.BackupViewModel
import com.braveboy.hesabi.ui.budget.BudgetViewModel
import com.braveboy.hesabi.ui.categories.CategoriesViewModel
import com.braveboy.hesabi.ui.entry.AddEditViewModel
import com.braveboy.hesabi.ui.entry.QuickEntryViewModel
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
            modelClass.isAssignableFrom(AssistantViewModel::class.java) -> AssistantViewModel(
                transactionRepository = container.transactionRepository,
                accountRepository = container.accountRepository,
                categoryRepository = container.categoryRepository,
                budgetRepository = container.budgetRepository,
                savingsGoalRepository = container.savingsGoalRepository,
                settingsRepository = container.settingsRepository,
            )
            modelClass.isAssignableFrom(AssetsViewModel::class.java) -> AssetsViewModel(
                assetRepository = container.assetRepository,
                diagnosticsRepository = container.diagnosticsRepository,
                priceSyncManager = container.assetPriceSyncManager,
                settingsRepository = container.settingsRepository,
            )
            modelClass.isAssignableFrom(TrashViewModel::class.java) -> TrashViewModel(
                transactionRepository = container.transactionRepository,
            )
            modelClass.isAssignableFrom(ImportViewModel::class.java) -> ImportViewModel(
                importer = TransactionImporter(
                    transactionRepository = container.transactionRepository,
                    accountRepository = container.accountRepository,
                    categoryRepository = container.categoryRepository,
                ),
                settingsRepository = container.settingsRepository,
            )
            modelClass.isAssignableFrom(DueCalendarViewModel::class.java) -> DueCalendarViewModel(
                chequeRepository = container.chequeRepository,
                loanRepository = container.loanRepository,
                debtRepository = container.debtRepository,
                personRepository = container.personRepository,
                recurringRepository = container.recurringTransactionRepository,
                savingsGoalRepository = container.savingsGoalRepository,
                settingsRepository = container.settingsRepository,
            )
            modelClass.isAssignableFrom(PurchaseSimulatorViewModel::class.java) -> PurchaseSimulatorViewModel(
                transactionRepository = container.transactionRepository,
                accountRepository = container.accountRepository,
                budgetRepository = container.budgetRepository,
                savingsGoalRepository = container.savingsGoalRepository,
                settingsRepository = container.settingsRepository,
            )
            modelClass.isAssignableFrom(AnnualReportViewModel::class.java) -> AnnualReportViewModel(
                transactionRepository = container.transactionRepository,
                categoryRepository = container.categoryRepository,
                assetRepository = container.assetRepository,
            )
            modelClass.isAssignableFrom(QuickEntryViewModel::class.java) -> QuickEntryViewModel(
                transactionRepository = container.transactionRepository,
                accountRepository = container.accountRepository,
                categoryRepository = container.categoryRepository,
                quickTemplateRepository = container.quickTemplateRepository,
                settingsRepository = container.settingsRepository,
                initialType = initialType,
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
                accountRepository = container.accountRepository,
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
            modelClass.isAssignableFrom(com.braveboy.hesabi.ui.debts.DebtsAndCommitmentsViewModel::class.java) -> com.braveboy.hesabi.ui.debts.DebtsAndCommitmentsViewModel(
                personRepository = container.personRepository,
                debtRepository = container.debtRepository,
                chequeRepository = container.chequeRepository,
                loanRepository = container.loanRepository,
                transactionRepository = container.transactionRepository,
                accountRepository = container.accountRepository,
                categoryRepository = container.categoryRepository,
                settingsRepository = container.settingsRepository,
            )
            modelClass.isAssignableFrom(com.braveboy.hesabi.ui.recurring.RecurringTransactionsViewModel::class.java) -> com.braveboy.hesabi.ui.recurring.RecurringTransactionsViewModel(
                recurringRepo = container.recurringTransactionRepository,
                accountRepo = container.accountRepository,
                categoryRepo = container.categoryRepository,
                processor = container.recurringTransactionProcessor,
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
