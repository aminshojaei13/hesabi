package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod

/**
 * Pure salary-deposit calendar. A payday of 31 in a 29/30-day month lands on the last
 * day, and every skipped month is caught up exactly once.
 */
object SalarySchedule {

    const val MAX_CATCH_UP_MONTHS = 24

    fun effectivePayday(period: MonthPeriod, configuredDay: Int): JalaliDate =
        JalaliDate(period.year, period.month, configuredDay.coerceIn(1, period.days))

    fun lastDueMonth(today: JalaliDate, configuredDay: Int): MonthPeriod {
        val current = MonthPeriod.of(today)
        val payday = effectivePayday(current, configuredDay)
        return if (today >= payday) current else current.previous()
    }

    /**
     * Months that still need a deposit. `lastDepositedMonth == null` means a first run:
     * only the current month is posted, and only after its payday has arrived.
     */
    fun dueMonths(
        today: JalaliDate,
        configuredDay: Int,
        lastDepositedMonth: MonthPeriod?,
    ): List<MonthPeriod> {
        val current = MonthPeriod.of(today)
        val currentPayday = effectivePayday(current, configuredDay)
        if (lastDepositedMonth == null) {
            return if (today >= currentPayday) listOf(current) else emptyList()
        }
        val lastDue = lastDueMonth(today, configuredDay)
        val result = mutableListOf<MonthPeriod>()
        var month = lastDepositedMonth.next()
        var guard = 0
        while (month.year * 12 + month.month <= lastDue.year * 12 + lastDue.month &&
            guard < MAX_CATCH_UP_MONTHS
        ) {
            result += month
            month = month.next()
            guard++
        }
        return result
    }
}
