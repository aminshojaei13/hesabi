package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType

/**
 * "Envelope" budgets: a category gets a fixed amount, and with rollover enabled the
 * unspent part moves to the next month (negative balances do *not* roll over, so an
 * overspend can never create a debt envelope).
 */
data class EnvelopeStatus(
    val budget: Budget,
    val carriedOverToman: Long,
    val availableToman: Long,
    val spentToman: Long,
) {
    val remainingToman: Long get() = (availableToman - spentToman).coerceAtLeast(0L)
    val overspentToman: Long get() = (spentToman - availableToman).coerceAtLeast(0L)
    val percent: Float
        get() = if (availableToman <= 0L) {
            if (spentToman > 0) 1f else 0f
        } else {
            (spentToman.toFloat() / availableToman).coerceAtLeast(0f)
        }
    val isOver: Boolean get() = spentToman > availableToman
}

object EnvelopeBudgetCalculator {

    /** How many past months are folded into the carry. */
    const val MAX_CARRY_MONTHS = 12

    fun status(
        budget: Budget,
        transactions: List<Transaction>,
        period: MonthPeriod = budget.periodOrNull() ?: MonthPeriod.current(),
        history: List<Budget> = emptyList(),
    ): EnvelopeStatus {
        val carry = if (budget.rolloverEnabled) {
            carryOver(budget, transactions, period, history)
        } else {
            0L
        }
        val spent = transactions
            .filter {
                it.type == TransactionType.EXPENSE &&
                    !it.isDeleted &&
                    period.contains(it.occurredOnEpochDay) &&
                    (budget.categoryId.isBlank() || it.categoryId == budget.categoryId)
            }
            .sumOf { it.amountToman }
        return EnvelopeStatus(
            budget = budget,
            carriedOverToman = carry,
            availableToman = budget.limitToman + carry,
            spentToman = spent,
        )
    }

    /**
     * Folds up to [MAX_CARRY_MONTHS] stored envelopes. A missing row stops the fold, so
     * a gap never creates free budget. With no history at all, only the immediately
     * previous month is considered using the current limit.
     */
    fun carryOver(
        budget: Budget,
        transactions: List<Transaction>,
        period: MonthPeriod,
        history: List<Budget> = emptyList(),
    ): Long {
        if (!budget.rolloverEnabled) return 0L
        var carry = 0L
        var cursor = period.previous()
        var guard = 0
        while (guard < MAX_CARRY_MONTHS) {
            val monthBudget = history.firstOrNull {
                it.categoryId == budget.categoryId && it.periodKey == cursor.key
            }
            if (monthBudget == null) {
                // No stored envelope for that month: do not invent one. The only
                // exception is the immediate previous month when no history was loaded,
                // where reusing the current limit matches how envelopes are set.
                if (history.isEmpty() && guard == 0) {
                    val spent = spentIn(budget, transactions, cursor)
                    return (budget.limitToman - spent).coerceAtLeast(0L)
                }
                break
            }
            val spent = spentIn(budget, transactions, cursor)
            carry = (monthBudget.limitToman + carry - spent).coerceAtLeast(0L)
            cursor = cursor.previous()
            guard++
        }
        return carry
    }

    private fun spentIn(
        budget: Budget,
        transactions: List<Transaction>,
        period: MonthPeriod,
    ): Long = transactions
        .filter {
            it.type == TransactionType.EXPENSE &&
                !it.isDeleted &&
                period.contains(it.occurredOnEpochDay) &&
                (budget.categoryId.isBlank() || it.categoryId == budget.categoryId)
        }
        .sumOf { it.amountToman }

    /**
     * Category budget consumption in a month, used by reports so a rollover envelope
     * and the budget screen always agree on one number (M28).
     */
    fun categoryLimitForReporting(
        period: MonthPeriod,
        categories: List<String>,
        budgets: List<Budget>,
    ): Long = budgets
        .filter { it.periodKey == period.key && it.categoryId in categories }
        .sumOf { it.limitToman }
}
