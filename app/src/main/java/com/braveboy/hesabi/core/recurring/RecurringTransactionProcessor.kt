package com.braveboy.hesabi.core.recurring

import com.braveboy.hesabi.domain.logic.RecurringFrequencyCalculator
import com.braveboy.hesabi.domain.model.RecurringTransaction
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionSource
import com.braveboy.hesabi.domain.repository.RecurringTransactionRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import java.util.UUID

class RecurringTransactionProcessor(
    private val recurringRepo: RecurringTransactionRepository,
    private val transactionRepo: TransactionRepository,
) {

    /**
     * Finds active recurring transactions with nextOccurrenceEpochDay <= todayEpochDay.
     * For each, if autoApply is true:
     *   1. Creates a Transaction in transactionRepo.
     *   2. Advances nextOccurrenceEpochDay via RecurringFrequencyCalculator.
     *   3. Updates the recurring item in recurringRepo.
     * Returns the count of applied transactions.
     */
    suspend fun processDueTransactions(todayEpochDay: Long): Int {
        val dueList = recurringRepo.getDue(todayEpochDay)
        var appliedCount = 0

        for (recurring in dueList) {
            if (recurring.autoApply) {
                applyRecurringTransaction(recurring, todayEpochDay)
                appliedCount++
            }
        }
        return appliedCount
    }

    /**
     * Manually or automatically applies a recurring transaction for the specified epochDay,
     * creating the corresponding financial transaction and advancing its next occurrence.
     */
    suspend fun applyRecurringTransaction(recurring: RecurringTransaction, applyEpochDay: Long) {
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            type = recurring.type,
            amountToman = recurring.amountToman,
            accountId = recurring.accountId,
            toAccountId = recurring.toAccountId,
            categoryId = recurring.categoryId,
            occurredOnEpochDay = applyEpochDay,
            note = recurring.note.ifBlank { recurring.title },
            source = TransactionSource.RECURRING,
            createdAtMillis = System.currentTimeMillis(),
        )
        transactionRepo.save(transaction)

        val nextOccurrence = RecurringFrequencyCalculator.computeNextOccurrence(
            currentEpochDay = recurring.nextOccurrenceEpochDay,
            frequency = recurring.frequency,
            interval = recurring.interval,
            dayOfMonth = recurring.dayOfMonth,
        )
        recurringRepo.updateOccurrence(recurring.id, applyEpochDay, nextOccurrence)
    }
}
