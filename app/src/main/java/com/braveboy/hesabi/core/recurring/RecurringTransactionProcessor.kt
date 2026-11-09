package com.braveboy.hesabi.core.recurring

import com.braveboy.hesabi.domain.logic.RecurringFrequencyCalculator
import com.braveboy.hesabi.domain.model.RecurringTransaction
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionSource
import com.braveboy.hesabi.domain.repository.RecurringTransactionRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class RecurringTransactionProcessor(
    private val recurringRepo: RecurringTransactionRepository,
    private val transactionRepo: TransactionRepository,
) {

    /** Serializes processing so the app start and the worker can never double-post. */
    private val mutex = Mutex()

    /**
     * Posts every missed occurrence of every due item, using the occurrence date
     * (not today) for the transaction. Re-running is safe because each occurrence
     * carries a unique [externalRef].
     */
    suspend fun processDueTransactions(todayEpochDay: Long): Int = mutex.withLock {
        val dueList = recurringRepo.getDue(todayEpochDay)
        var appliedCount = 0

        for (recurring in dueList) {
            if (!recurring.autoApply) continue
            var occurrence = recurring.nextOccurrenceEpochDay
            var guard = 0
            while (occurrence <= todayEpochDay && guard < MAX_CATCH_UP) {
                if (postOccurrence(recurring, occurrence)) appliedCount++
                val next = RecurringFrequencyCalculator.computeNextOccurrence(
                    currentEpochDay = occurrence,
                    frequency = recurring.frequency,
                    interval = recurring.interval,
                    dayOfMonth = recurring.dayOfMonth,
                )
                recurringRepo.updateOccurrence(recurring.id, occurrence, next)
                occurrence = next
                guard++
            }
        }
        appliedCount
    }

    /**
     * Manually applies an item for [applyEpochDay]. The schedule only advances when
     * the item was actually due, so applying early does not skip the next occurrence.
     */
    suspend fun applyRecurringTransaction(
        recurring: RecurringTransaction,
        applyEpochDay: Long,
    ) = mutex.withLock {
        if (!postOccurrence(recurring, applyEpochDay)) return@withLock
        val next = if (recurring.nextOccurrenceEpochDay <= applyEpochDay) {
            RecurringFrequencyCalculator.computeNextOccurrence(
                currentEpochDay = recurring.nextOccurrenceEpochDay,
                frequency = recurring.frequency,
                interval = recurring.interval,
                dayOfMonth = recurring.dayOfMonth,
            )
        } else {
            recurring.nextOccurrenceEpochDay
        }
        recurringRepo.updateOccurrence(recurring.id, applyEpochDay, next)
    }

    private suspend fun postOccurrence(
        recurring: RecurringTransaction,
        occurrenceEpochDay: Long,
    ): Boolean {
        val ref = externalRef(recurring.id, occurrenceEpochDay)
        if (transactionRepo.findByExternalRef(ref) != null) return false

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            type = recurring.type,
            amountToman = recurring.amountToman,
            accountId = recurring.accountId,
            toAccountId = recurring.toAccountId,
            categoryId = recurring.categoryId,
            occurredOnEpochDay = occurrenceEpochDay,
            note = recurring.note.ifBlank { recurring.title },
            source = TransactionSource.RECURRING,
            externalRef = ref,
            createdAtMillis = System.currentTimeMillis(),
        )
        transactionRepo.save(transaction)
        return true
    }

    companion object {
        /** Safety valve so a long-abandoned daily item cannot loop forever. */
        private const val MAX_CATCH_UP = 400

        fun externalRef(recurringId: String, occurrenceEpochDay: Long): String =
            "recurring:$recurringId:$occurrenceEpochDay"
    }
}
