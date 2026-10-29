package com.braveboy.hesabi.core.date

import com.braveboy.hesabi.core.money.Digits
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure Jalali (Solar Hijri) calendar math. Dates are persisted as epoch days
 * (days since 1970-01-01 UTC), which makes range queries trivial and timezone-safe.
 *
 * Algorithm is a Kotlin port of the well-known jalaali-js implementation and is
 * covered by round-trip tests for 1300..1500.
 */
data class JalaliDate(val year: Int, val month: Int, val day: Int) : Comparable<JalaliDate> {

    init {
        require(year in 1..9999) { "year out of range: $year" }
        require(month in 1..12) { "month out of range: $month" }
        require(day in 1..31) { "day out of range: $day" }
    }

    fun toEpochDay(): Long = JalaliCalendar.jalaliToEpochDay(year, month, day)

    fun toGregorian(): LocalDate = LocalDate.ofEpochDay(toEpochDay())

    fun startOfMonth(): JalaliDate = copy(day = 1)

    fun plusDays(days: Long): JalaliDate = JalaliCalendar.fromEpochDay(toEpochDay() + days)

    fun plusMonths(months: Int): JalaliDate {
        val total = (year * 12 + (month - 1)) + months
        val targetYear = Math.floorDiv(total, 12)
        val targetMonth = Math.floorMod(total, 12) + 1
        val maxDay = JalaliCalendar.daysInMonth(targetYear, targetMonth)
        return JalaliDate(targetYear, targetMonth, day.coerceAtMost(maxDay))
    }

    fun monthLength(): Int = JalaliCalendar.daysInMonth(year, month)

    fun weekdayIndex(): Int = JalaliCalendar.weekdayIndex(toEpochDay())

    fun weekdayName(): String = JalaliCalendar.WEEKDAYS[weekdayIndex()]

    fun monthName(): String = JalaliCalendar.MONTHS[month - 1]

    /** ۱۴۰۵/۰۸/۰۲ */
    fun formatShort(): String = Digits.toPersian("%04d/%02d/%02d".format(year, month, day))

    /** شنبه ۲ آبان ۱۴۰۵ */
    fun formatLong(): String =
        Digits.toPersian("${weekdayName()} $day ${monthName()} $year")

    /** ۲ آبان ۱۴۰۵ */
    fun formatDayMonth(): String = Digits.toPersian("$day ${monthName()} $year")

    /** ۱۴۰۵/۰۸ */
    fun formatYearMonth(): String = Digits.toPersian("%04d/%02d".format(year, month))

    fun toStorage(): String = "%04d-%02d-%02d".format(year, month, day)

    override fun compareTo(other: JalaliDate): Int =
        compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

    companion object {
        fun parse(input: String): JalaliDate? {
            val normalized = Digits.toAscii(input).trim()
            val match = Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})""").find(normalized) ?: return null
            val (y, m, d) = match.destructured
            val year = y.toIntOrNull() ?: return null
            val month = m.toIntOrNull() ?: return null
            val day = d.toIntOrNull() ?: return null
            if (month !in 1..12) return null
            if (day !in 1..JalaliCalendar.daysInMonth(year, month)) return null
            return JalaliDate(year, month, day)
        }

        fun fromEpochDay(epochDay: Long): JalaliDate = JalaliCalendar.fromEpochDay(epochDay)

        fun today(
            clock: java.time.Clock = java.time.Clock.systemDefaultZone(),
            zone: ZoneId = ZoneId.systemDefault(),
        ): JalaliDate {
            val local = Instant.ofEpochMilli(clock.millis()).atZone(zone).toLocalDate()
            return JalaliCalendar.fromEpochDay(local.toEpochDay())
        }

        fun fromGregorian(date: LocalDate): JalaliDate =
            JalaliCalendar.fromEpochDay(date.toEpochDay())
    }
}

/** A Persian month, used for reports, budgets and month switching. */
data class MonthPeriod(val year: Int, val month: Int) {

    val startEpochDay: Long get() = JalaliDate(year, month, 1).toEpochDay()

    val endEpochDayExclusive: Long
        get() = JalaliDate(year, month, 1).plusMonths(1).toEpochDay()

    val days: Int get() = JalaliCalendar.daysInMonth(year, month)

    val key: String get() = "%04d-%02d".format(year, month)

    val label: String get() = Digits.toPersian("${JalaliCalendar.MONTHS[month - 1]} $year")

    fun previous(): MonthPeriod = JalaliDate(year, month, 1).plusMonths(-1).let { MonthPeriod(it.year, it.month) }

    fun next(): MonthPeriod = JalaliDate(year, month, 1).plusMonths(1).let { MonthPeriod(it.year, it.month) }

    fun contains(epochDay: Long): Boolean = epochDay in startEpochDay until endEpochDayExclusive

    companion object {
        fun of(date: JalaliDate): MonthPeriod = MonthPeriod(date.year, date.month)

        fun fromKey(key: String): MonthPeriod? {
            val parts = key.split("-")
            if (parts.size != 2) return null
            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            if (month !in 1..12) return null
            return MonthPeriod(year, month)
        }

        fun current(
            clock: java.time.Clock = java.time.Clock.systemDefaultZone(),
        ): MonthPeriod = of(JalaliDate.today(clock))
    }
}

object JalaliCalendar {

    val MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    val WEEKDAYS = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    private data class JalCal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int, withoutLeap: Boolean = false): JalCal {
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        for (i in 1 until BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + (n % 33 + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (!withoutLeap) {
            if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
            var leap = ((n + 1) % 33 - 1) % 4
            if (leap == -1) leap = 4
            return JalCal(leap, gy, march)
        }
        return JalCal(0, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Long {
        var d = ((gy + (gm - 8) / 6 + 100100) * 1461) / 4 +
            (153 * ((gm + 9) % 12) + 2) / 5 + gd - 34840408
        d = d - (((gy + 100100 + (gm - 8) / 6) / 100) * 3) / 4 + 752
        return d.toLong()
    }

    private fun d2g(jdn: Long): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j += ((4 * jdn + 183187720) / 146097 * 3) / 4 * 4 - 3908
        val i = ((j % 1461) / 4) * 5 + 308
        val gd = (((i % 153) / 5) + 1).toInt()
        val gm = (((i / 153) % 12) + 1).toInt()
        val gy = (j / 1461).toInt() - 100100 + (8 - gm) / 6
        return Triple(gy, gm, gd)
    }

    fun jalaliToEpochDay(jy: Int, jm: Int, jd: Int): Long {
        val r = jalCal(jy, withoutLeap = true)
        val jdn = g2d(r.gy, 3, r.march) + (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1
        return jdn - 2440588L
    }

    fun fromEpochDay(epochDay: Long): JalaliDate {
        val jdn = epochDay + 2440588L
        val (gy, _, _) = d2g(jdn)
        var jy = gy - 621
        val r = jalCal(jy, withoutLeap = false)
        val jdn1f = g2d(gy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                val jm = 1 + (k / 31).toInt()
                val jd = (k % 31).toInt() + 1
                return JalaliDate(jy, jm, jd)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        val jm = 7 + (k / 30).toInt()
        val jd = (k % 30).toInt() + 1
        return JalaliDate(jy, jm, jd)
    }

    fun daysInMonth(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        isLeapYear(jy) -> 30
        else -> 29
    }

    fun isLeapYear(jy: Int): Boolean = jalCal(jy).leap == 0

    /** 0 = Saturday … 6 = Friday, matching the Persian week. */
    fun weekdayIndex(epochDay: Long): Int = Math.floorMod(epochDay + 5, 7L).toInt()

    /** Persian week starts Saturday; the grid offset for a month. */
    fun monthGridOffset(period: MonthPeriod): Int =
        weekdayIndex(JalaliDate(period.year, period.month, 1).toEpochDay())
}
