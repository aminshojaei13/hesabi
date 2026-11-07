package com.braveboy.hesabi.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.braveboy.hesabi.data.local.entity.AccountEntity
import com.braveboy.hesabi.data.local.entity.BudgetEntity
import com.braveboy.hesabi.data.local.entity.CategoryEntity
import com.braveboy.hesabi.data.local.entity.ChequeEntity
import com.braveboy.hesabi.data.local.entity.DebtEntity
import com.braveboy.hesabi.data.local.entity.InstallmentEntity
import com.braveboy.hesabi.data.local.entity.LoanEntity
import com.braveboy.hesabi.data.local.entity.PersonEntity
import com.braveboy.hesabi.data.local.entity.RecurringTransactionEntity
import com.braveboy.hesabi.data.local.entity.SavingsGoalEntity
import com.braveboy.hesabi.data.local.entity.SenderRuleEntity
import com.braveboy.hesabi.data.local.entity.SmsEventEntity
import com.braveboy.hesabi.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, name")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun findById(id: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Upsert
    suspend fun upsertAll(accounts: List<AccountEntity>)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE kind = :kind ORDER BY sortOrder, name")
    fun observeByKind(kind: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredOnEpochDay DESC, createdAtMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE occurredOnEpochDay >= :start AND occurredOnEpochDay < :end " +
            "ORDER BY occurredOnEpochDay DESC, createdAtMillis DESC",
    )
    fun observeBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY occurredOnEpochDay DESC, createdAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE occurredOnEpochDay >= :start AND occurredOnEpochDay < :end " +
            "ORDER BY occurredOnEpochDay DESC, createdAtMillis DESC",
    )
    suspend fun getBetween(start: Long, end: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY occurredOnEpochDay DESC, createdAtMillis DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE externalRef = :ref LIMIT 1")
    suspend fun findByExternalRef(ref: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun countForAccount(accountId: String): Int

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Upsert
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE periodKey = :periodKey")
    fun observeForPeriod(periodKey: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE periodKey = :periodKey")
    suspend fun getForPeriod(periodKey: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun findById(id: String): BudgetEntity?

    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Upsert
    suspend fun upsertAll(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_events WHERE status = 'PENDING' ORDER BY occurredOnEpochDay DESC, receivedAtMillis DESC")
    fun observePending(): Flow<List<SmsEventEntity>>

    @Query("SELECT * FROM sms_events ORDER BY receivedAtMillis DESC LIMIT 200")
    fun observeRecent(): Flow<List<SmsEventEntity>>

    @Query("SELECT * FROM sms_events WHERE status = 'PENDING' ORDER BY occurredOnEpochDay DESC, receivedAtMillis DESC")
    suspend fun getPending(): List<SmsEventEntity>

    @Query("SELECT * FROM sms_events WHERE rawHash = :hash")
    suspend fun findByHash(hash: String): SmsEventEntity?

    @Query("SELECT COUNT(*) FROM sms_events WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(event: SmsEventEntity): Long

    @Query("UPDATE sms_events SET status = :status, duplicateOfTransactionId = :duplicateId WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, duplicateId: String? = null)

    @Query("UPDATE sms_events SET status = 'IMPORTED', duplicateOfTransactionId = :transactionId WHERE id = :id")
    suspend fun markImported(id: String, transactionId: String)

    @Query("UPDATE sms_events SET status = 'IGNORED' WHERE sender = :sender AND status = 'PENDING'")
    suspend fun ignoreAllFromSender(sender: String)

    @Query("DELETE FROM sms_events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sms_events")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsertAll(events: List<SmsEventEntity>)
}

@Dao
interface SenderRuleDao {
    @Query("SELECT * FROM sender_rules")
    fun observeAll(): Flow<List<SenderRuleEntity>>

    @Query("SELECT * FROM sender_rules")
    suspend fun getAll(): List<SenderRuleEntity>

    @Upsert
    suspend fun upsert(rule: SenderRuleEntity)

    @Upsert
    suspend fun upsertAll(rules: List<SenderRuleEntity>)

    @Query("DELETE FROM sender_rules WHERE sender = :sender")
    suspend fun deleteBySender(sender: String)

    @Query("DELETE FROM sender_rules")
    suspend fun deleteAll()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, createdAtMillis DESC")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE isCompleted = 0 ORDER BY createdAtMillis DESC")
    fun observeActive(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, createdAtMillis DESC")
    suspend fun getAll(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun findById(id: String): SavingsGoalEntity?

    @Upsert
    suspend fun upsert(goal: SavingsGoalEntity)

    @Upsert
    suspend fun upsertAll(goals: List<SavingsGoalEntity>)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE savings_goals SET savedAmountToman = :newAmount, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateAmount(id: String, newAmount: Long, isCompleted: Boolean)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll()
}

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY name ASC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY name ASC")
    suspend fun getAll(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun findById(id: String): PersonEntity?

    @Upsert
    suspend fun upsert(person: PersonEntity)

    @Upsert
    suspend fun upsertAll(people: List<PersonEntity>)

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM people")
    suspend fun deleteAll()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE personId = :personId ORDER BY createdAtMillis DESC")
    fun observeForPerson(personId: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE isSettled = 0 ORDER BY dueEpochDay ASC, createdAtMillis DESC")
    fun observeAllUnsettled(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts")
    suspend fun getAll(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun findById(id: String): DebtEntity?

    @Upsert
    suspend fun upsert(debt: DebtEntity)

    @Upsert
    suspend fun upsertAll(debts: List<DebtEntity>)

    @Query("UPDATE debts SET settledAmountToman = :settledAmount, isSettled = :isSettled WHERE id = :id")
    suspend fun updateSettlement(id: String, settledAmount: Long, isSettled: Boolean)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM debts WHERE personId = :personId")
    suspend fun deleteByPersonId(personId: String)

    @Query("DELETE FROM debts")
    suspend fun deleteAll()
}

@Dao
interface ChequeDao {
    @Query("SELECT * FROM cheques ORDER BY dueEpochDay ASC")
    fun observeAll(): Flow<List<ChequeEntity>>

    @Query("SELECT * FROM cheques WHERE status = 'PENDING' ORDER BY dueEpochDay ASC")
    fun observePending(): Flow<List<ChequeEntity>>

    @Query("SELECT * FROM cheques ORDER BY dueEpochDay ASC")
    suspend fun getAll(): List<ChequeEntity>

    @Query("SELECT * FROM cheques WHERE id = :id")
    suspend fun findById(id: String): ChequeEntity?

    @Upsert
    suspend fun upsert(cheque: ChequeEntity)

    @Upsert
    suspend fun upsertAll(cheques: List<ChequeEntity>)

    @Query("UPDATE cheques SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM cheques WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cheques")
    suspend fun deleteAll()
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY isCompleted ASC, createdAtMillis DESC")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans ORDER BY isCompleted ASC, createdAtMillis DESC")
    suspend fun getAll(): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun findById(id: String): LoanEntity?

    @Upsert
    suspend fun upsert(loan: LoanEntity)

    @Upsert
    suspend fun upsertAll(loans: List<LoanEntity>)

    @Query("UPDATE loans SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompleted(id: String, isCompleted: Boolean)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM loans")
    suspend fun deleteAll()
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    fun observeForLoan(loanId: String): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    suspend fun getForLoan(loanId: String): List<InstallmentEntity>

    @Query("SELECT * FROM installments ORDER BY dueEpochDay ASC")
    fun observeAll(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments ORDER BY dueEpochDay ASC")
    suspend fun getAll(): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun findById(id: String): InstallmentEntity?

    @Upsert
    suspend fun upsert(installment: InstallmentEntity)

    @Upsert
    suspend fun upsertAll(installments: List<InstallmentEntity>)

    @Query("UPDATE installments SET isPaid = :isPaid, paidAtEpochDay = :paidAtEpochDay WHERE id = :id")
    suspend fun updatePaid(id: String, isPaid: Boolean, paidAtEpochDay: Long?)

    @Query("DELETE FROM installments WHERE loanId = :loanId")
    suspend fun deleteByLoanId(loanId: String)

    @Query("DELETE FROM installments")
    suspend fun deleteAll()
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY isActive DESC, nextOccurrenceEpochDay ASC")
    fun observeAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextOccurrenceEpochDay ASC")
    fun observeActive(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAll(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextOccurrenceEpochDay <= :todayEpochDay")
    suspend fun getDue(todayEpochDay: Long): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun findById(id: String): RecurringTransactionEntity?

    @Upsert
    suspend fun upsert(recurring: RecurringTransactionEntity)

    @Upsert
    suspend fun upsertAll(recurring: List<RecurringTransactionEntity>)

    @Query("UPDATE recurring_transactions SET lastAppliedEpochDay = :appliedDay, nextOccurrenceEpochDay = :nextDay WHERE id = :id")
    suspend fun updateOccurrence(id: String, appliedDay: Long, nextDay: Long)

    @Query("UPDATE recurring_transactions SET isActive = :isActive WHERE id = :id")
    suspend fun updateActive(id: String, isActive: Boolean)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()
}
