package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionDraft
import com.braveboy.hesabi.domain.model.TransactionType

data class BudgetStatus(
    val budget: Budget,
    val spentToman: Long,
) {
    val remainingToman: Long get() = budget.limitToman - spentToman
    val percent: Float
        get() = if (budget.limitToman <= 0) 0f else (spentToman.toFloat() / budget.limitToman).coerceAtLeast(0f)
    val isOver: Boolean get() = spentToman > budget.limitToman
    val isNearLimit: Boolean get() = !isOver && percent >= 0.8f
}

object BudgetCalculator {

    fun status(budget: Budget, transactions: List<Transaction>): BudgetStatus {
        val period = budget.periodOrNull()
        val relevant = transactions.filter {
            it.type == TransactionType.EXPENSE &&
                (period == null || period.contains(it.occurredOnEpochDay)) &&
                (budget.categoryId.isBlank() || it.categoryId == budget.categoryId)
        }
        return BudgetStatus(budget = budget, spentToman = relevant.sumOf { it.amountToman })
    }

    fun statuses(budgets: List<Budget>, transactions: List<Transaction>): List<BudgetStatus> =
        budgets.map { status(it, transactions) }
            .sortedWith(compareBy({ it.budget.categoryId.isNotBlank() }, { it.budget.categoryId }))

    fun overallStatus(monthBudget: Budget?, transactions: List<Transaction>, monthKey: String): BudgetStatus? {
        val budget = monthBudget ?: return null
        if (budget.periodKey != monthKey) return null
        return status(budget, transactions)
    }
}

/**
 * Duplicate detection for both manual entries and SMS candidates.
 * A candidate is duplicate when type, amount, account and day (within a small
 * window) all match; transfers additionally compare the destination account.
 */
object DuplicateDetector {

    fun findDuplicate(
        draft: TransactionDraft,
        existing: List<Transaction>,
        windowDays: Long = 1,
    ): Transaction? = existing.firstOrNull { tx ->
        tx.type == draft.type &&
            tx.amountToman == draft.amountToman &&
            tx.accountId == draft.accountId &&
            kotlin.math.abs(tx.occurredOnEpochDay - draft.occurredOnEpochDay) <= windowDays &&
            (draft.type != TransactionType.TRANSFER || tx.toAccountId == draft.toAccountId)
    }

    fun findDuplicate(
        amountToman: Long,
        type: TransactionType,
        accountId: String,
        epochDay: Long,
        existing: List<Transaction>,
        windowDays: Long = 1,
    ): Transaction? = existing.firstOrNull { tx ->
        tx.type == type &&
            tx.amountToman == amountToman &&
            tx.accountId == accountId &&
            kotlin.math.abs(tx.occurredOnEpochDay - epochDay) <= windowDays
    }
}
