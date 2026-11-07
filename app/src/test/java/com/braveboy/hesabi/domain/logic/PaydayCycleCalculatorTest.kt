package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaydayCycleCalculatorTest {

    @Test
    fun `default payday on 1st of month spans whole jalali month`() {
        val date = JalaliDate(1403, 8, 15) // 15 Aban 1403
        val cycle = PaydayCycleCalculator.calculateCycle(today = date, paydayDay = 1)

        assertEquals(JalaliDate(1403, 8, 1), cycle.startDate)
        assertEquals(JalaliDate(1403, 9, 1), cycle.nextPaydayDate)
        assertEquals(30, cycle.totalDays)
        assertEquals(16, cycle.daysRemaining) // 15th to 30th = 16 days remaining
    }

    @Test
    fun `custom payday on 25th when today is before payday`() {
        val date = JalaliDate(1403, 8, 10) // 10 Aban 1403 (before 25th)
        val cycle = PaydayCycleCalculator.calculateCycle(today = date, paydayDay = 25)

        assertEquals(JalaliDate(1403, 7, 25), cycle.startDate)
        assertEquals(JalaliDate(1403, 8, 25), cycle.nextPaydayDate)
        assertEquals(15, cycle.daysRemaining) // 10th to 25th = 15 days
    }

    @Test
    fun `custom payday on 25th when today is after payday`() {
        val date = JalaliDate(1403, 8, 27) // 27 Aban 1403 (after 25th)
        val cycle = PaydayCycleCalculator.calculateCycle(today = date, paydayDay = 25)

        assertEquals(JalaliDate(1403, 8, 25), cycle.startDate)
        assertEquals(JalaliDate(1403, 9, 25), cycle.nextPaydayDate)
        assertTrue(cycle.daysRemaining > 0)
    }

    @Test
    fun `daily safe to spend divides remaining funds by days left`() {
        val date = JalaliDate(1403, 8, 20)
        val cycle = PaydayCycleCalculator.calculateCycle(today = date, paydayDay = 1) // 11 days left
        val safe = PaydayCycleCalculator.calculateSafeToSpend(
            totalLiquidBalanceToman = 11_000_000L,
            totalCycleExpenseToman = 2_000_000L,
            spentTodayToman = 100_000L,
            cycle = cycle,
        )

        // 11,000,000 / 11 days = 1,000,000 per day
        assertEquals(1_000_000L, safe.safeToSpendTodayToman)
        assertEquals(BurnRateStatus.EXCELLENT, safe.burnRateStatus)
    }
}
