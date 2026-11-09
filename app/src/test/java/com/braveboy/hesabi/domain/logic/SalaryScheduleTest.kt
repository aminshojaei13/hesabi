package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.date.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SalaryScheduleTest {

    @Test
    fun `payday 31 lands on the last day of a 30-day month`() {
        val period = MonthPeriod(1405, 8) // 30 days
        assertEquals(30, SalarySchedule.effectivePayday(period, 31).day)
        assertEquals(29, SalarySchedule.effectivePayday(MonthPeriod(1405, 12), 31).day)
    }

    @Test
    fun `first run posts only after payday`() {
        val before = JalaliDate(1405, 8, 29)
        assertEquals(emptyList<MonthPeriod>(), SalarySchedule.dueMonths(before, 31, null))
        val on = JalaliDate(1405, 8, 30)
        assertEquals(listOf(MonthPeriod(1405, 8)), SalarySchedule.dueMonths(on, 31, null))
    }

    @Test
    fun `skipped months are caught up in order`() {
        val due = SalarySchedule.dueMonths(
            today = JalaliDate(1405, 8, 15),
            configuredDay = 1,
            lastDepositedMonth = MonthPeriod(1405, 5),
        )
        assertEquals(
            listOf(MonthPeriod(1405, 6), MonthPeriod(1405, 7), MonthPeriod(1405, 8)),
            due,
        )
    }

    @Test
    fun `no month is duplicated when the current payday has passed`() {
        val due = SalarySchedule.dueMonths(
            today = JalaliDate(1405, 8, 25),
            configuredDay = 20,
            lastDepositedMonth = MonthPeriod(1405, 8),
        )
        assertEquals(emptyList<MonthPeriod>(), due)
    }

    @Test
    fun `catch-up is bounded to protect against corrupt dates`() {
        val due = SalarySchedule.dueMonths(
            today = JalaliDate(1500, 1, 1),
            configuredDay = 1,
            lastDepositedMonth = MonthPeriod(1400, 1),
        )
        assertEquals(SalarySchedule.MAX_CATCH_UP_MONTHS, due.size)
    }
}
