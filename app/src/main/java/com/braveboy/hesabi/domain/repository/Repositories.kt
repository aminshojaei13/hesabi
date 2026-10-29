package com.braveboy.hesabi.domain.repository

import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountWithBalance
import com.braveboy.hesabi.domain.model.AppSettings
import com.braveboy.hesabi.domain.model.BackupSummary
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.ImportResult
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
    suspend fun getBetween(start: Long, endExclusive: Long): List<Transaction>
    suspend fun getAll(): List<Transaction>
    suspend fun getById(id: String): Transaction?
    suspend fun save(transaction: Transaction)
    suspend fun saveAll(transactions: List<Transaction>)
    suspend fun delete(id: String)
    suspend fun countForCategory(categoryId: String): Int
    suspend fun countForAccount(accountId: String): Int
}

interface BudgetRepository {
    fun observeForPeriod(periodKey: String): Flow<List<Budget>>
    suspend fun getForPeriod(periodKey: String): List<Budget>
    suspend fun save(budget: Budget)
    suspend fun delete(id: String)
}

data class SmsAcceptInput(
    val eventId: String,
    val type: TransactionType,
    val amountToman: Long,
    val accountId: String,
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
}

interface BackupRepository {
    suspend fun exportEncrypted(out: OutputStream, password: CharArray): BackupSummary
    suspend fun importEncrypted(input: InputStream, password: CharArray): ImportResult
    suspend fun exportCsv(out: OutputStream): Int
}
