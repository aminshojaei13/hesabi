package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanningLogicTest {

    private fun tx(
        amount: Long,
        date: JalaliDate,
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: String? = "cat_food",
        note: String = "",
    ) = Transaction(
        id = "tx-${amount}-${date.toEpochDay()}-$type-$note",
        type = type,
        amountToman = amount,
        accountId = "acc",
        categoryId = categoryId,
        occurredOnEpochDay = date.toEpochDay(),
        note = note,
    )

    @Test
    fun `forecast on the first day uses one elapsed day`() {
        val period = MonthPeriod(1405, 8)
        val today = JalaliDate(1405, 8, 1)
        val forecast = MonthForecastCalculator.forecast(
            period = period,
            today = today,
            transactions = listOf(tx(100_000, today)),
            currentBalanceToman = 10_000_000,
        )
        assertEquals(1, forecast.daysElapsed)
        assertEquals(29, forecast.daysRemaining)
        assertEquals(100_000, forecast.averageDailyExpenseToman)
        assertEquals(3_000_000, forecast.projectedExpenseToman)
        assertEquals(ForecastStatus.ON_TRACK, forecast.status)
    }

    @Test
    fun `forecast flags a shortfall when commitments exceed balance`() {
        val period = MonthPeriod(1405, 8)
        val today = JalaliDate(1405, 8, 20)
        val forecast = MonthForecastCalculator.forecast(
            period = period,
            today = today,
            transactions = listOf(tx(9_000_000, today)),
            currentBalanceToman = 2_000_000,
            upcomingCommitmentsToman = 4_000_000,
        )
        assertTrue(forecast.shortfallToman > 0)
        assertEquals(ForecastStatus.AT_RISK, forecast.status)
    }

    @Test
    fun `envelope rolls unused money and never rolls overspend`() {
        val period = MonthPeriod(1405, 8)
        val previous = period.previous()
        val budget = Budget(
            id = "b",
            categoryId = "cat_food",
            periodKey = period.key,
            limitToman = 1_000_000,
            rolloverEnabled = true,
        )
        val transactions = listOf(
            tx(400_000, JalaliDate(previous.year, previous.month, 5)),
            tx(900_000, JalaliDate(period.year, period.month, 3)),
        )
        val status = EnvelopeBudgetCalculator.status(budget, transactions, period)
        assertEquals(600_000, status.carriedOverToman)
        assertEquals(1_600_000, status.availableToman)
        assertEquals(700_000, status.remainingToman)

        val overspent = EnvelopeBudgetCalculator.status(
            budget,
            listOf(tx(2_000_000, JalaliDate(previous.year, previous.month, 5))),
            period,
        )
        assertEquals(0, overspent.carriedOverToman)
    }

    @Test
    fun `purchase simulator reports runway loss and affordability`() {
        val cycle = PaydayCycleCalculator.calculateCycle(JalaliDate(1405, 8, 10), 1)
        val safe = PaydayCycleCalculator.calculateSafeToSpend(
            totalLiquidBalanceToman = 3_000_000,
            totalCycleExpenseToman = 0,
            spentTodayToman = 0,
            cycle = cycle,
        )
        val affordable = PurchaseSimulator.simulate(
            amountToman = 500_000,
            safeToSpend = safe,
            liquidBalanceToman = 3_000_000,
        )
        assertTrue(affordable.affordableNow)
        assertTrue(affordable.daysOfRunwayLost >= 1)

        val tooMuch = PurchaseSimulator.simulate(
            amountToman = 9_000_000,
            safeToSpend = safe,
            liquidBalanceToman = 3_000_000,
        )
        assertFalse(tooMuch.affordableNow)
        assertTrue(tooMuch.verdict.contains("جا نمی‌شود"))
    }

    @Test
    fun `smart category learns from keywords and history`() {
        val categories = listOf(
            Category("cat_food", "خوراک", CategoryKind.EXPENSE, "restaurant", 0),
            Category("cat_transport", "حمل‌ونقل", CategoryKind.EXPENSE, "car", 0),
        )
        val suggestion = SmartCategorySuggester.suggest(
            text = "اینترنت و قبض",
            history = listOf(tx(50_000, JalaliDate(1405, 8, 1), note = "قبض اینترنت")),
            categories = categories,
        )
        assertNotNull(suggestion)
    }

    @Test
    fun `annual report computes savings rate and totals`() {
        val year = 1405
        val categories = listOf(
            Category("cat_food", "خوراک", CategoryKind.EXPENSE, "restaurant", 0),
            Category("cat_salary", "حقوق", CategoryKind.INCOME, "wallet", 0),
        )
        val transactions = listOf(
            tx(10_000_000, JalaliDate(year, 1, 10), TransactionType.INCOME, "cat_salary", "حقوق"),
            tx(4_000_000, JalaliDate(year, 1, 12), TransactionType.EXPENSE, "cat_food", "خرید"),
        )
        val report = AnnualReportCalculator.build(year, transactions, categories)
        assertEquals(10_000_000, report.totalIncomeToman)
        assertEquals(4_000_000, report.totalExpenseToman)
        assertEquals(6_000_000, report.totalNetToman)
        assertEquals(60, report.savingsRatePercent)
        assertEquals("خوراک", report.topCategory?.name)
        assertEquals(12, report.months.size)
    }
}
