package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountKind
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportCalculatorTest {

    private val period = MonthPeriod(1405, 8)
    private val dayOne = JalaliDate(1405, 8, 1).toEpochDay()
    private val dayTwo = JalaliDate(1405, 8, 2).toEpochDay()

    private val food = Category("food", "خوراک", CategoryKind.EXPENSE, "restaurant", 0xFFD6455D, sortOrder = 1)
    private val transport = Category("taxi", "حمل‌ونقل", CategoryKind.EXPENSE, "directions_car", 0xFF3B82F6, sortOrder = 2)
    private val salary = Category("salary", "حقوق", CategoryKind.INCOME, "payments", 0xFF1F9D6B, sortOrder = 1)
    private val categories = listOf(food, transport, salary)

    private val accountA = Account("a", "نقدی", AccountKind.CASH, 0xFF12AFC0, 0)
    private val accountB = Account("b", "کارت", AccountKind.CARD, 0xFF6E7B80, 0)

    private fun tx(
        id: String,
        type: TransactionType,
        amount: Long,
        day: Long,
        categoryId: String? = null,
        account: String = "a",
        to: String? = null,
    ) = Transaction(
        id = id,
        type = type,
        amountToman = amount,
        accountId = account,
        toAccountId = to,
        categoryId = categoryId,
        occurredOnEpochDay = day,
    )

    private val transactions = listOf(
        tx("1", TransactionType.EXPENSE, 100_000, dayOne, "food"),
        tx("2", TransactionType.EXPENSE, 50_000, dayTwo, "taxi"),
        tx("3", TransactionType.INCOME, 500_000, dayOne, "salary"),
        tx("4", TransactionType.TRANSFER, 200_000, dayOne + 2, null, "a", "b"),
        tx("5", TransactionType.EXPENSE, 20_000, dayOne + 3, null),
    )

    private val previous = listOf(
        tx("p1", TransactionType.EXPENSE, 100_000, JalaliDate(1405, 7, 1).toEpochDay(), "food"),
    )

    @Test
    fun `totals exclude transfers`() {
        val report = ReportCalculator.build(period, transactions, categories, previous)
        assertEquals(170_000L, report.totalExpenseToman)
        assertEquals(500_000L, report.totalIncomeToman)
        assertEquals(330_000L, report.netToman)
        // 3 expenses + 1 income = 4 counted rows, transfer excluded
        assertEquals(4, report.transactionCount)
    }

    @Test
    fun `category slices are sorted and shares add up`() {
        val report = ReportCalculator.build(period, transactions, categories, previous)
        assertEquals(3, report.slices.size)
        assertEquals("food", report.slices.first().categoryId)
        assertEquals(100_000L, report.slices.first().amountToman)
        assertEquals(1f, report.slices.sumOf { it.share.toDouble() }.toFloat(), 0.001f)
        assertEquals("بدون دسته", report.slices.last().name)
    }

    @Test
    fun `month comparison is computed against previous month`() {
        val report = ReportCalculator.build(period, transactions, categories, previous)
        assertEquals(100_000L, report.previousExpenseToman)
        assertEquals(70, report.expenseDeltaPercent)
    }

    @Test
    fun `no previous data yields null delta`() {
        val report = ReportCalculator.build(period, transactions, categories)
        assertNull(report.expenseDeltaPercent)
    }

    @Test
    fun `daily series covers every day of the month`() {
        val report = ReportCalculator.build(period, transactions, categories, previous)
        assertEquals(30, report.daily.size)
        assertEquals(100_000L, report.daily.first().expenseToman)
        assertEquals(500_000L, report.daily.first().incomeToman)
    }

    @Test
    fun `overall budget is attached to report`() {
        val budget = Budget("b1", "", period.key, 500_000)
        val report = ReportCalculator.build(period, transactions, categories, previous, listOf(budget))
        assertEquals(500_000L, report.budgetLimitToman)
        assertEquals(170_000L, report.budgetSpentToman)
        assertFalse(report.isOverBudget)
        assertEquals(34, (report.budgetPercent!! * 100).toInt())
    }

    @Test
    fun `over budget is detected`() {
        val budget = Budget("b1", "", period.key, 100_000)
        val report = ReportCalculator.build(period, transactions, categories, previous, listOf(budget))
        assertTrue(report.isOverBudget)
    }

    @Test
    fun `category budget aggregates only that category`() {
        val budget = Budget("b1", "food", period.key, 200_000)
        val report = ReportCalculator.build(period, transactions, categories, previous, listOf(budget))
        assertEquals(100_000L, report.budgetSpentToman)
    }

    @Test
    fun `transactions outside the month are ignored`() {
        val outside = tx("x", TransactionType.EXPENSE, 999_000, JalaliDate(1405, 9, 1).toEpochDay(), "food")
        val report = ReportCalculator.build(period, transactions + outside, categories, previous)
        assertEquals(170_000L, report.totalExpenseToman)
    }
}
