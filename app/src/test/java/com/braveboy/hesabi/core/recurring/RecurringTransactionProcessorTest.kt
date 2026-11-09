package com.braveboy.hesabi.core.recurring

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.domain.model.RecurringFrequency
import com.braveboy.hesabi.domain.model.RecurringTransaction
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.RecurringTransactionRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringTransactionProcessorTest {

    private val rent = RecurringTransaction(
        id = "rec_rent",
        title = "اجاره",
        type = TransactionType.EXPENSE,
        amountToman = 1_000_000L,
        accountId = "acc_cash",
        frequency = RecurringFrequency.MONTHLY,
        interval = 1,
        dayOfMonth = 1,
        nextOccurrenceEpochDay = JalaliDate(1405, 1, 1).toEpochDay(),
        autoApply = true,
    )

    @Test
    fun `processDueTransactions catches up every missed month with the occurrence date`() = runTest {
        val recurringRepo = FakeRecurringRepository(listOf(rent))
        val transactionRepo = FakeTransactionRepository()
        val processor = RecurringTransactionProcessor(recurringRepo, transactionRepo)

        val today = JalaliDate(1405, 3, 15).toEpochDay()
        val applied = processor.processDueTransactions(today)

        assertEquals(3, applied)
        assertEquals(3, transactionRepo.transactions.value.size)
        assertEquals(
            listOf(
                JalaliDate(1405, 1, 1).toEpochDay(),
                JalaliDate(1405, 2, 1).toEpochDay(),
                JalaliDate(1405, 3, 1).toEpochDay(),
            ),
            transactionRepo.transactions.value.map { it.occurredOnEpochDay }.sorted(),
        )
        assertEquals(3, transactionRepo.transactions.value.mapNotNull { it.externalRef }.toSet().size)
        assertEquals(
            JalaliDate(1405, 4, 1).toEpochDay(),
            recurringRepo.items.value.single().nextOccurrenceEpochDay,
        )
    }

    @Test
    fun `processDueTransactions is idempotent`() = runTest {
        val recurringRepo = FakeRecurringRepository(listOf(rent))
        val transactionRepo = FakeTransactionRepository()
        val processor = RecurringTransactionProcessor(recurringRepo, transactionRepo)
        val today = JalaliDate(1405, 3, 15).toEpochDay()

        assertEquals(3, processor.processDueTransactions(today))
        assertEquals(0, processor.processDueTransactions(today))
        assertEquals(3, transactionRepo.transactions.value.size)
    }

    @Test
    fun `manual apply does not duplicate the same occurrence`() = runTest {
        val recurringRepo = FakeRecurringRepository(listOf(rent))
        val transactionRepo = FakeTransactionRepository()
        val processor = RecurringTransactionProcessor(recurringRepo, transactionRepo)
        val day = JalaliDate(1405, 1, 1).toEpochDay()

        processor.applyRecurringTransaction(rent, day)
        processor.applyRecurringTransaction(rent, day)

        assertEquals(1, transactionRepo.transactions.value.size)
        assertEquals(day, transactionRepo.transactions.value.single().occurredOnEpochDay)
    }

    @Test
    fun `items without autoApply are not posted automatically`() = runTest {
        val recurringRepo = FakeRecurringRepository(listOf(rent.copy(autoApply = false)))
        val transactionRepo = FakeTransactionRepository()
        val processor = RecurringTransactionProcessor(recurringRepo, transactionRepo)

        val applied = processor.processDueTransactions(JalaliDate(1405, 3, 15).toEpochDay())

        assertEquals(0, applied)
        assertTrue(transactionRepo.transactions.value.isEmpty())
        assertEquals(rent.nextOccurrenceEpochDay, recurringRepo.items.value.single().nextOccurrenceEpochDay)
    }

    private class FakeRecurringRepository(initial: List<RecurringTransaction>) : RecurringTransactionRepository {
        val items = MutableStateFlow(initial)

        override fun observeAll(): Flow<List<RecurringTransaction>> = items

        override fun observeActive(): Flow<List<RecurringTransaction>> =
            items.map { list -> list.filter { it.isActive } }

        override fun observeDue(todayEpochDay: Long): Flow<List<RecurringTransaction>> =
            items.map { list -> list.filter { it.isActive && it.nextOccurrenceEpochDay <= todayEpochDay } }

        override suspend fun getAll(): List<RecurringTransaction> = items.value

        override suspend fun getDue(todayEpochDay: Long): List<RecurringTransaction> =
            items.value.filter { it.isActive && it.nextOccurrenceEpochDay <= todayEpochDay }

        override suspend fun getById(id: String): RecurringTransaction? = items.value.firstOrNull { it.id == id }

        override suspend fun save(recurring: RecurringTransaction) {
            items.value = items.value.filterNot { it.id == recurring.id } + recurring
        }

        override suspend fun delete(id: String) {
            items.value = items.value.filterNot { it.id == id }
        }

        override suspend fun updateOccurrence(id: String, appliedEpochDay: Long, nextOccurrenceEpochDay: Long) {
            items.value = items.value.map {
                if (it.id == id) {
                    it.copy(
                        lastAppliedEpochDay = appliedEpochDay,
                        nextOccurrenceEpochDay = nextOccurrenceEpochDay,
                    )
                } else {
                    it
                }
            }
        }

        override suspend fun toggleActive(id: String, isActive: Boolean) {
            items.value = items.value.map { if (it.id == id) it.copy(isActive = isActive) else it }
        }
    }

    private class FakeTransactionRepository : TransactionRepository {
        val transactions = MutableStateFlow<List<Transaction>>(emptyList())

        override fun observeAll(): Flow<List<Transaction>> = transactions

        override fun observeBetween(start: Long, endExclusive: Long): Flow<List<Transaction>> =
            transactions.map { list -> list.filter { it.occurredOnEpochDay in start until endExclusive } }

        override fun observeRecent(limit: Int): Flow<List<Transaction>> =
            transactions.map { list -> list.sortedByDescending { it.occurredOnEpochDay }.take(limit) }

        override fun observeTrash(): Flow<List<Transaction>> =
            transactions.map { list -> list.filter { it.isDeleted } }

        override suspend fun getBetween(start: Long, endExclusive: Long): List<Transaction> =
            transactions.value.filter { it.occurredOnEpochDay in start until endExclusive }

        override suspend fun getAll(): List<Transaction> = transactions.value.filterNot { it.isDeleted }

        override suspend fun getAllIncludingDeleted(): List<Transaction> = transactions.value

        override suspend fun getById(id: String): Transaction? = transactions.value.firstOrNull { it.id == id }

        override suspend fun findByExternalRef(ref: String): Transaction? =
            transactions.value.firstOrNull { it.externalRef == ref }

        override suspend fun findByLinkedEntity(type: String, id: String): List<Transaction> =
            transactions.value.filter { it.linkedEntityType == type && it.linkedEntityId == id }

        override suspend fun save(transaction: Transaction) {
            transactions.value = transactions.value.filterNot { it.id == transaction.id } + transaction
        }

        override suspend fun saveAll(transactions: List<Transaction>) {
            transactions.forEach { save(it) }
        }

        override suspend fun delete(id: String) {
            transactions.value = transactions.value.map {
                if (it.id == id) it.copy(deletedAtMillis = System.currentTimeMillis()) else it
            }
        }

        override suspend fun restore(id: String) {
            transactions.value = transactions.value.map {
                if (it.id == id) it.copy(deletedAtMillis = null) else it
            }
        }

        override suspend fun deletePermanently(id: String) {
            transactions.value = transactions.value.filterNot { it.id == id }
        }

        override suspend fun purgeTrash(olderThanMillis: Long): Int {
            val before = transactions.value.size
            transactions.value = transactions.value.filterNot {
                it.isDeleted && (it.deletedAtMillis ?: 0L) < olderThanMillis
            }
            return before - transactions.value.size
        }

        override suspend fun countForCategory(categoryId: String): Int =
            transactions.value.count { it.categoryId == categoryId }

        override suspend fun countForAccount(accountId: String): Int =
            transactions.value.count { it.accountId == accountId || it.toAccountId == accountId }
    }
}
