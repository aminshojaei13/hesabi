package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.domain.model.RecurringFrequency
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringFrequencyCalculatorTest {

    @Test
    fun computeNextOccurrence_daily_addsDays() {
        val start = JalaliDate(1405, 1, 10).toEpochDay()
        val next = RecurringFrequencyCalculator.computeNextOccurrence(
            currentEpochDay = start,
            frequency = RecurringFrequency.DAILY,
            interval = 1,
        )
        val expected = JalaliDate(1405, 1, 11).toEpochDay()
        assertEquals(expected, next)

        val next3 = RecurringFrequencyCalculator.computeNextOccurrence(
            currentEpochDay = start,
            frequency = RecurringFrequency.DAILY,
            interval = 3,
        )
        val expected3 = JalaliDate(1405, 1, 13).toEpochDay()
        assertEquals(expected3, next3)
    }

    @Test
    fun computeNextOccurrence_weekly_addsSevenDays() {
        val start = JalaliDate(1405, 1, 10).toEpochDay()
        val next = RecurringFrequencyCalculator.computeNextOccurrence(
            currentEpochDay = start,
            frequency = RecurringFrequency.WEEKLY,
            interval = 1,
        )
        val expected = JalaliDate(1405, 1, 17).toEpochDay()
        assertEquals(expected, next)
    }

    @Test
    fun computeNextOccurrence_monthly_preservesDayOrClamps() {
        // From Farvardin 15 to Ordibehesht 15
        val start = JalaliDate(1405, 1, 15).toEpochDay()
        val next = RecurringFrequencyCalculator.computeNextOccurrence(
            currentEpochDay = start,
            frequency = RecurringFrequency.MONTHLY,
            interval = 1,
            dayOfMonth = 15,
        )
        val expected = JalaliDate(1405, 2, 15).toEpochDay()
        assertEquals(expected, next)

        // Day 31 into Esfand (29 or 30 days) should clamp to max days of Esfand
        val startBahman = JalaliDate(1405, 11, 30).toEpochDay()
        val nextEsfand = RecurringFrequencyCalculator.computeNextOccurrence(
            currentEpochDay = startBahman,
            frequency = RecurringFrequency.MONTHLY,
            interval = 1,
            dayOfMonth = 31,
        )
        val esfandDate = JalaliDate.fromEpochDay(nextEsfand)
        assertEquals(1405, esfandDate.year)
        assertEquals(12, esfandDate.month)
        assertEquals(29, esfandDate.day) // 1405 is not leap year -> 29 days
    }

    @Test
    fun computeNextOccurrence_yearly_advancesYear() {
        val start = JalaliDate(1405, 6, 20).toEpochDay()
        val next = RecurringFrequencyCalculator.computeNextOccurrence(
            currentEpochDay = start,
            frequency = RecurringFrequency.YEARLY,
            interval = 1,
        )
        val expected = JalaliDate(1406, 6, 20).toEpochDay()
        assertEquals(expected, next)
    }
}
