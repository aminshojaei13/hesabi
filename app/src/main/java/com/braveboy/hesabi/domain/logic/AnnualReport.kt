package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategorySlice
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType

data class AnnualMonthTotal(
    val month: Int,
    val label: String,
    val incomeToman: Long,
    val expenseToman: Long,
) {
    val netToman: Long get() = incomeToman - expenseToman
}

data class AnnualReport(
    val year: Int,
    val months: List<AnnualMonthTotal>,
    val totalIncomeToman: Long,
    val totalExpenseToman: Long,
    val totalNetToman: Long,
    val savingsRatePercent: Int,
    val transactionCount: Int,
    val topCategory: CategorySlice?,
    val biggestExpense: Transaction?,
    val bestMonth: AnnualMonthTotal?,
    val hardestMonth: AnnualMonthTotal?,
    val insights: List<String>,
)

/** "حساب سال من": a year in one page, computed only from the user's own ledger. */
object AnnualReportCalculator {

    fun build(
        year: Int,
        transactions: List<Transaction>,
        categories: List<Category>,
    ): AnnualReport {
        val active = transactions.filterNot { it.isDeleted }
        val months = (1..12).map { month ->
            val period = MonthPeriod(year, month)
            val inMonth = active.filter { period.contains(it.occurredOnEpochDay) }
            AnnualMonthTotal(
                month = month,
                label = period.label,
                incomeToman = inMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amountToman },
                expenseToman = inMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountToman },
            )
        }
        val income = months.sumOf { it.incomeToman }
        val expense = months.sumOf { it.expenseToman }
        val net = income - expense
        val savingsRate = if (income > 0) ((net.toDouble() / income) * 100).toInt() else 0

        val yearTransactions = active.filter {
            it.occurredOn.year == year && it.affectsIncomeExpense
        }
        val slices = ReportCalculator.totalExpenseByCategory(yearTransactions, categories)
        val topCategory = slices.firstOrNull()
        val biggestExpense = yearTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .maxByOrNull { it.amountToman }
        val best = months.maxByOrNull { it.netToman }
        val hardest = months.minByOrNull { it.netToman }

        val insights = buildList {
            if (income > 0) {
                add(
                    "از هر ۱۰۰ تومان درآمد، ${(savingsRate.coerceAtLeast(0) / 10.0).formatOne()} تومان پس‌انداز شده است.",
                )
            }
            if (topCategory != null) {
                add(
                    "بیشترین هزینهٔ سال در «${topCategory.name}» با " +
                        Money.format(topCategory.amountToman) + " بوده است.",
                )
            }
            if (hardest != null && hardest.expenseToman > 0) {
                add("پرهزینه‌ترین ماه ${hardest.label} بود.")
            }
            if (best != null) {
                add("بهترین ماه از نظر مانده، ${best.label} بود.")
            }
            if (months.all { it.expenseToman == 0L }) {
                add("برای دیدن گزارش سالانه، ابتدا چند تراکنش ثبت کن.")
            }
        }

        return AnnualReport(
            year = year,
            months = months,
            totalIncomeToman = income,
            totalExpenseToman = expense,
            totalNetToman = net,
            savingsRatePercent = savingsRate,
            transactionCount = yearTransactions.size,
            topCategory = topCategory,
            biggestExpense = biggestExpense,
            bestMonth = best,
            hardestMonth = hardest,
            insights = insights,
        )
    }

    private fun Double.formatOne(): String {
        val scaled = (this * 10).toInt()
        return if (scaled % 10 == 0) (scaled / 10).toString() else "${scaled / 10}٫${scaled % 10}"
    }
}
