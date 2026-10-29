package com.braveboy.hesabi.core.date

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JalaliDateTest {

    @Test
    fun `known new year conversions`() {
        assertEquals(LocalDate.of(2025, 3, 21), JalaliDate(1404, 1, 1).toGregorian())
        assertEquals(LocalDate.of(2026, 10, 24), JalaliDate(1405, 8, 2).toGregorian())
        assertEquals(JalaliDate(1405, 8, 2), JalaliDate.fromGregorian(LocalDate.of(2026, 10, 24)))
    }

    @Test
    fun `round trip every month boundary 1300 to 1500`() {
        for (year in 1300..1500) {
            for (month in 1..12) {
                val date = JalaliDate(year, month, 1)
                assertEquals(date, JalaliDate.fromEpochDay(date.toEpochDay()))
                val lastDay = JalaliDate(year, month, JalaliCalendar.daysInMonth(year, month))
                assertEquals(lastDay, JalaliDate.fromEpochDay(lastDay.toEpochDay()))
            }
        }
    }

    @Test
    fun `leap years have 30 day esfand`() {
        assertTrue(JalaliCalendar.isLeapYear(1403))
        assertEquals(30, JalaliCalendar.daysInMonth(1403, 12))
        assertFalse(JalaliCalendar.isLeapYear(1404))
        assertEquals(29, JalaliCalendar.daysInMonth(1404, 12))
    }

    @Test
    fun `persian week starts on saturday`() {
        val saturday = JalaliDate(1405, 8, 2).toEpochDay()
        assertEquals(0, JalaliCalendar.weekdayIndex(saturday))
        assertEquals("شنبه", JalaliDate.fromEpochDay(saturday).weekdayName())
        assertEquals("جمعه", JalaliDate.fromEpochDay(saturday + 6).weekdayName())
    }

    @Test
    fun `formats persian labels`() {
        val date = JalaliDate(1405, 8, 2)
        assertEquals("۱۴۰۵/۰۸/۰۲", date.formatShort())
        assertEquals("۲ آبان ۱۴۰۵", date.formatDayMonth())
        assertEquals("آبان ۱۴۰۵", MonthPeriod.of(date).label)
    }

    @Test
    fun `month period has correct bounds and navigation`() {
        val period = MonthPeriod(1405, 8)
        assertEquals(30, period.days)
        assertEquals(1405, period.previous().year)
        assertEquals(7, period.previous().month)
        assertEquals(9, period.next().month)
        assertTrue(period.contains(period.startEpochDay))
        assertFalse(period.contains(period.endEpochDayExclusive))
    }

    @Test
    fun `parses and rejects dates`() {
        assertEquals(JalaliDate(1405, 8, 2), JalaliDate.parse("۱۴۰۵/۰۸/۰۲"))
        assertNull(JalaliDate.parse("1405/13/01"))
        assertNull(JalaliDate.parse("not a date"))
    }

    @Test
    fun `plus months clamps day to month length`() {
        assertEquals(JalaliDate(1405, 7, 30), JalaliDate(1405, 6, 31).plusMonths(1))
        assertEquals(1404, JalaliDate(1405, 1, 1).plusMonths(-1).year)
    }
}
