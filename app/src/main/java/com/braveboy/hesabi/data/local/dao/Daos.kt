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
