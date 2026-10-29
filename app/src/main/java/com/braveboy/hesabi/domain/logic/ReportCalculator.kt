package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.CategorySlice
import com.braveboy.hesabi.domain.model.DailyPoint
import com.braveboy.hesabi.domain.model.MonthReport
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType
import kotlin.math.roundToInt

/**
 * Pure report aggregation. Transfers are deliberately excluded from income and
 * expenses so moving money between two accounts never inflates the month.
 */
object ReportCalculator {

    const val UNCATEGORIZED_ID = "uncategorized"
    const val UNCATEGORIZED_NAME = "بدون دسته"
    const val UNCATEGORIZED_COLOR = 0xFF8A9A9EL

    fun build(
        period: MonthPeriod,
        transactions: List<Transaction>,
        categories: List<Category>,
        previousTransactions: List<Transaction> = emptyList(),
        budgets: List<Budget> = emptyList(),
    ): MonthReport {
        val categoryById = categories.associateBy { it.id }
        val inMonth = transactions.filter { period.contains(it.occurredOnEpochDay) }
        val counted = inMonth.filter { it.affectsIncomeExpense }

        var expense = 0L
        var income = 0L
        var transferCount = 0
        val perCategory = LinkedHashMap<String, Long>()
        val dailyExpense = LinkedHashMap<Long, Long>()
        val dailyIncome = LinkedHashMap<Long, Long>()

        for (tx in inMonth) {
            when (tx.type) {
                TransactionType.EXPENSE -> {
                    expense += tx.amountToman
                    val key = tx.categoryId ?: UNCATEGORIZED_ID
                    perCategory[key] = (perCategory[key] ?: 0L) + tx.amountToman
                    dailyExpense[tx.occurredOnEpochDay] =
                        (dailyExpense[tx.occurredOnEpochDay] ?: 0L) + tx.amountToman
                }
                TransactionType.INCOME -> {
                    income += tx.amountToman
                    dailyIncome[tx.occurredOnEpochDay] =
                        (dailyIncome[tx.occurredOnEpochDay] ?: 0L) + tx.amountToman
                }
                TransactionType.TRANSFER -> transferCount++
            }
        }

        val slices = perCategory.entries
            .map { (categoryId, amount) ->
                val category = categoryById[categoryId]
                CategorySlice(
                    categoryId = categoryId,
                    name = category?.name ?: UNCATEGORIZED_NAME,
                    iconKey = category?.iconKey ?: "receipt",
                    colorArgb = category?.colorArgb ?: UNCATEGORIZED_COLOR,
                    amountToman = amount,
                    share = if (expense == 0L) 0f else amount.toFloat() / expense,
                )
            }
            .sortedWith(compareByDescending<CategorySlice> { it.amountToman }.thenBy { it.name })

        val previousExpense = previousTransactions
            .filter { it.type == TransactionType.EXPENSE && it.affectsIncomeExpense }
            .sumOf { it.amountToman }

        val deltaPercent = if (previousExpense <= 0L) {
            null
        } else {
            (((expense - previousExpense).toDouble() / previousExpense) * 100).roundToInt()
        }

        val days = (0 until period.days).map { offset ->
            val epochDay = period.startEpochDay + offset
            DailyPoint(
                epochDay = epochDay,
                expenseToman = dailyExpense[epochDay] ?: 0L,
                incomeToman = dailyIncome[epochDay] ?: 0L,
            )
        }

        val overallBudget = budgets.firstOrNull { it.periodKey == period.key && it.categoryId.isBlank() }
        val categoryBudgets = budgets.filter { it.periodKey == period.key && it.categoryId.isNotBlank() }
        val categorySpent = categoryBudgets.sumOf { budget -> perCategory[budget.categoryId] ?: 0L }

        return MonthReport(
            period = period,
            totalExpenseToman = expense,
            totalIncomeToman = income,
            netToman = income - expense,
            transactionCount = counted.size,
            previousExpenseToman = previousExpense,
            expenseDeltaPercent = deltaPercent,
            slices = slices,
            daily = days,
            budgetLimitToman = overallBudget?.limitToman ?: categoryBudgets.sumOf { it.limitToman }.takeIf { it > 0 },
            budgetSpentToman = if (overallBudget != null) expense else categorySpent,
        )
    }

    /** Category spending used by the budget screen. */
    fun spentByCategory(
        period: MonthPeriod,
        transactions: List<Transaction>,
        categoryId: String,
    ): Long = transactions
        .filter {
            it.type == TransactionType.EXPENSE &&
                it.categoryId == categoryId &&
                period.contains(it.occurredOnEpochDay)
        }
        .sumOf { it.amountToman }

    fun monthExpense(period: MonthPeriod, transactions: List<Transaction>): Long = transactions
        .filter { it.type == TransactionType.EXPENSE && period.contains(it.occurredOnEpochDay) }
        .sumOf { it.amountToman }

    fun totalExpenseByCategory(
        transactions: List<Transaction>,
        categories: List<Category> = emptyList(),
    ): List<CategorySlice> {
        val filtered = transactions.filter { it.type == TransactionType.EXPENSE }
        val total = filtered.sumOf { it.amountToman }
        val byId = categories.filter { it.kind == CategoryKind.EXPENSE }.associateBy { it.id }
        return filtered.groupBy { it.categoryId ?: UNCATEGORIZED_ID }
            .map { (id, list) ->
                val amount = list.sumOf { it.amountToman }
                val category = byId[id]
                CategorySlice(
                    categoryId = id,
                    name = category?.name ?: UNCATEGORIZED_NAME,
                    iconKey = category?.iconKey ?: "receipt",
                    colorArgb = category?.colorArgb ?: UNCATEGORIZED_COLOR,
                    amountToman = amount,
                    share = if (total == 0L) 0f else amount.toFloat() / total,
                )
            }
            .sortedByDescending { it.amountToman }
    }
}
