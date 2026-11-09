package com.braveboy.hesabi.domain.repository

import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountWithBalance
import com.braveboy.hesabi.domain.model.AppEvent
import com.braveboy.hesabi.domain.model.AppSettings
import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.AssetPriceSnapshot
import com.braveboy.hesabi.domain.model.BackupSummary
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.Cheque
import com.braveboy.hesabi.domain.model.Debt
import com.braveboy.hesabi.domain.model.ImportResult
import com.braveboy.hesabi.domain.model.Installment
import com.braveboy.hesabi.domain.model.Loan
import com.braveboy.hesabi.domain.model.LoanWithInstallments
import com.braveboy.hesabi.domain.model.Person
import com.braveboy.hesabi.domain.model.PersonWithDebts
import com.braveboy.hesabi.domain.model.QuickTemplate
import com.braveboy.hesabi.domain.model.RecurringTransaction
import com.braveboy.hesabi.domain.model.SavingsDeposit
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.model.SenderRule
import com.braveboy.hesabi.domain.model.SmsEvent
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    fun observeWithBalances(): Flow<List<AccountWithBalance>>
    suspend fun getAll(): List<Account>
    suspend fun getById(id: String): Account?
    suspend fun save(account: Account)
    suspend fun delete(id: String)
}

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeByKind(kind: CategoryKind): Flow<List<Category>>
    suspend fun getAll(): List<Category>
    suspend fun getById(id: String): Category?
    suspend fun save(category: Category)
    suspend fun delete(id: String)
}

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeBetween(start: Long, endExclusive: Long): Flow<List<Transaction>>
    fun observeRecent(limit: Int): Flow<List<Transaction>>
    fun observeTrash(): Flow<List<Transaction>>
    suspend fun getBetween(start: Long, endExclusive: Long): List<Transaction>
    suspend fun getAll(): List<Transaction>
    suspend fun getAllIncludingDeleted(): List<Transaction>
    suspend fun getById(id: String): Transaction?
    suspend fun findByExternalRef(ref: String): Transaction?
    suspend fun findByLinkedEntity(type: String, id: String): List<Transaction>
    suspend fun save(transaction: Transaction)
    suspend fun saveAll(transactions: List<Transaction>)
    /** Moves the row to the recycle bin (30 days) instead of destroying it. */
    suspend fun delete(id: String)
    suspend fun restore(id: String)
    suspend fun deletePermanently(id: String)
    suspend fun purgeTrash(olderThanMillis: Long): Int
    suspend fun countForCategory(categoryId: String): Int
    suspend fun countForAccount(accountId: String): Int
}

interface BudgetRepository {
    fun observeForPeriod(periodKey: String): Flow<List<Budget>>
    suspend fun getForPeriod(periodKey: String): List<Budget>
    suspend fun save(budget: Budget)
    suspend fun delete(id: String)
}

interface SavingsGoalRepository {
    fun observeAll(): Flow<List<SavingsGoal>>
    fun observeActive(): Flow<List<SavingsGoal>>
    fun observeDeposits(goalId: String): Flow<List<SavingsDeposit>>
    suspend fun getAll(): List<SavingsGoal>
    suspend fun getById(id: String): SavingsGoal?
    suspend fun save(goal: SavingsGoal)
    suspend fun delete(id: String)
    suspend fun deposit(goalId: String, amountToman: Long, note: String = "")
    suspend fun withdraw(goalId: String, amountToman: Long, note: String = "")
}

data class SmsAcceptInput(
    val eventId: String,
    val type: TransactionType,
    val amountToman: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String?,
    val occurredOnEpochDay: Long,
    val note: String,
)

interface SmsRepository {
    fun observePending(): Flow<List<SmsEvent>>
    fun observeRecent(): Flow<List<SmsEvent>>
    fun observePendingCount(): Flow<Int>
    fun observeSenderRules(): Flow<List<SenderRule>>
    suspend fun ingest(sender: String, body: String, receivedAtMillis: Long, todayEpochDay: Long): IngestOutcome
    suspend fun importText(text: String, todayEpochDay: Long): List<IngestOutcome>
    suspend fun acceptAll(entries: List<SmsAcceptInput>): Int
    suspend fun ignore(eventId: String)
    suspend fun ignoreSender(sender: String)
    suspend fun allowSender(sender: String)
    suspend fun delete(eventId: String)
    suspend fun clearAll()
}

sealed interface IngestOutcome {
    data class Added(val eventId: String) : IngestOutcome
    data object Duplicate : IngestOutcome
    data class Ignored(val reason: String) : IngestOutcome
    data object Failed : IngestOutcome
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun setOnboarded(value: Boolean)
    suspend fun setThemeMode(mode: com.braveboy.hesabi.domain.model.ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setCurrency(currency: com.braveboy.hesabi.core.money.Currency)
    suspend fun setHideAmounts(hidden: Boolean)
    suspend fun setSmsCaptureEnabled(enabled: Boolean)
    suspend fun setAmountsAreRial(enabled: Boolean)
    suspend fun setLastBackupAt(millis: Long)
    suspend fun setDefaultAccountId(id: String?)
    suspend fun setPaydayDayOfMonth(day: Int)
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun setPinCodeHash(hash: String)
    suspend fun setPinFailedAttempts(attempts: Int)
    suspend fun setPinLockedUntil(millis: Long)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setMonthlySalary(amountToman: Long)
    suspend fun setSalaryAccountId(id: String?)
    suspend fun setAutoDepositSalary(enabled: Boolean)
    suspend fun setAiMode(mode: com.braveboy.hesabi.domain.model.AiMode)
    suspend fun setLastAssistantUseAt(millis: Long)
    suspend fun setSmsRationaleAccepted(accepted: Boolean)
    suspend fun setAssetPricesAutoUpdate(enabled: Boolean)
}

interface PersonRepository {
    fun observeAll(): Flow<List<Person>>
    fun observeWithDebts(): Flow<List<PersonWithDebts>>
    suspend fun getAll(): List<Person>
    suspend fun getById(id: String): Person?
    suspend fun save(person: Person)
    suspend fun delete(id: String)
}

interface DebtRepository {
    fun observeForPerson(personId: String): Flow<List<Debt>>
    fun observeAllUnsettled(): Flow<List<Debt>>
    fun observeAll(): Flow<List<Debt>>
    suspend fun getById(id: String): Debt?
    suspend fun save(debt: Debt)
    suspend fun delete(id: String)
    suspend fun settle(debtId: String, amountToman: Long)
}

interface ChequeRepository {
    fun observeAll(): Flow<List<Cheque>>
    fun observePending(): Flow<List<Cheque>>
    suspend fun getAll(): List<Cheque>
    suspend fun getById(id: String): Cheque?
    suspend fun save(cheque: Cheque)
    suspend fun delete(id: String)
    suspend fun updateStatus(id: String, status: com.braveboy.hesabi.domain.model.ChequeStatus)
}

interface LoanRepository {
    fun observeAllWithInstallments(): Flow<List<LoanWithInstallments>>
    suspend fun getAllWithInstallments(): List<LoanWithInstallments>
    suspend fun getById(id: String): LoanWithInstallments?
    suspend fun saveLoan(loan: Loan, installments: List<Installment>)
    suspend fun deleteLoan(id: String)
    suspend fun payInstallment(installmentId: String, paidAtEpochDay: Long)
    suspend fun revertInstallmentPayment(installmentId: String)
}

interface RecurringTransactionRepository {
    fun observeAll(): Flow<List<RecurringTransaction>>
    fun observeActive(): Flow<List<RecurringTransaction>>
    fun observeDue(todayEpochDay: Long): Flow<List<RecurringTransaction>>
    suspend fun getAll(): List<RecurringTransaction>
    suspend fun getDue(todayEpochDay: Long): List<RecurringTransaction>
    suspend fun getById(id: String): RecurringTransaction?
    suspend fun save(recurring: RecurringTransaction)
    suspend fun delete(id: String)
    suspend fun updateOccurrence(id: String, appliedEpochDay: Long, nextOccurrenceEpochDay: Long)
    suspend fun toggleActive(id: String, isActive: Boolean)
}

enum class BackupImportMode { REPLACE, MERGE }

interface BackupRepository {
    suspend fun exportEncrypted(out: OutputStream, password: CharArray): BackupSummary
    suspend fun importEncrypted(
        input: InputStream,
        password: CharArray,
        mode: BackupImportMode = BackupImportMode.REPLACE,
    ): ImportResult
    suspend fun exportCsv(out: OutputStream): Int
}


interface QuickTemplateRepository {
    fun observeAll(): Flow<List<QuickTemplate>>
    suspend fun getAll(): List<QuickTemplate>
    suspend fun save(template: QuickTemplate)
    suspend fun delete(id: String)
    suspend fun markUsed(id: String)
}

interface AssetRepository {
    fun observeAll(): Flow<List<Asset>>
    fun observeSnapshots(): Flow<List<AssetPriceSnapshot>>
    suspend fun getAll(): List<Asset>
    suspend fun getById(id: String): Asset?
    suspend fun save(asset: Asset, recordSnapshot: Boolean = true)
    suspend fun delete(id: String)
    suspend fun updatePrice(assetId: String, priceTomanPerUnit: Long, source: String, atMillis: Long)
}

/** Local-only, anonymous product diagnostics. */
interface DiagnosticsRepository {
    suspend fun record(type: String, name: String, metadata: String = "")
    suspend fun recent(): List<AppEvent>
    suspend fun clear()
}
