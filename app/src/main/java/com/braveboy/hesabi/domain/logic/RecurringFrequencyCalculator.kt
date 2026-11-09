package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.JalaliCalendar
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.domain.model.RecurringFrequency

object RecurringFrequencyCalculator {

    /**
     * First monthly occurrence at or after [today] for an item anchored on [dayOfMonth].
     * The anchor is clamped for short months but never permanently shifted.
     */
    fun firstMonthlyOccurrence(today: JalaliDate, dayOfMonth: Int): Long {
        val anchor = dayOfMonth.coerceIn(1, 31)
        val candidate = JalaliDate(today.year, today.month, anchor.coerceAtMost(today.monthLength()))
        if (candidate >= today) return candidate.toEpochDay()

        val nextMonth = today.plusMonths(1)
        val nextDay = anchor.coerceAtMost(nextMonth.monthLength())
        return JalaliDate(nextMonth.year, nextMonth.month, nextDay).toEpochDay()
    }

    /**
     * Computes the next occurrence epoch day based on the starting / current epoch day,
     * recurring frequency, interval (default 1), and optional preferred dayOfMonth.
     */
    fun computeNextOccurrence(
        currentEpochDay: Long,
        frequency: RecurringFrequency,
        interval: Int = 1,
        dayOfMonth: Int? = null,
    ): Long {
        val safeInterval = interval.coerceAtLeast(1)
        val currentDate = JalaliDate.fromEpochDay(currentEpochDay)

        return when (frequency) {
            RecurringFrequency.DAILY -> {
                currentDate.plusDays(safeInterval.toLong()).toEpochDay()
            }
            RecurringFrequency.WEEKLY -> {
                currentDate.plusDays(safeInterval.toLong() * 7L).toEpochDay()
            }
            RecurringFrequency.MONTHLY -> {
                val nextMonthDate = currentDate.plusMonths(safeInterval)
                val targetDay = (dayOfMonth ?: currentDate.day).coerceIn(1, 31)
                val maxDay = JalaliCalendar.daysInMonth(nextMonthDate.year, nextMonthDate.month)
                JalaliDate(nextMonthDate.year, nextMonthDate.month, targetDay.coerceAtMost(maxDay)).toEpochDay()
            }
            RecurringFrequency.YEARLY -> {
                val nextYear = currentDate.year + safeInterval
                val targetMonth = currentDate.month
                val targetDay = (dayOfMonth ?: currentDate.day).coerceIn(1, 31)
                val maxDay = JalaliCalendar.daysInMonth(nextYear, targetMonth)
                JalaliDate(nextYear, targetMonth, targetDay.coerceAtMost(maxDay)).toEpochDay()
            }
        }
    }
}
