package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.JalaliCalendar
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Digits

enum class BurnRateStatus {
    EXCELLENT, // Green - spending well below daily limit
    NORMAL,    // Emerald / Cyan - on track
    CAUTION,   // Yellow - near daily budget limit
    DANGER,    // Red - over budget or empty funds
}

data class PaydayCycle(
    val startDate: JalaliDate,
    val nextPaydayDate: JalaliDate,
    val startEpochDay: Long,
    val endEpochDayExclusive: Long,
    val totalDays: Int,
    val daysRemaining: Int,
    val daysElapsed: Int,
    val label: String,
)

data class DailySafeToSpend(
    val safeToSpendTodayToman: Long,
    val daysRemaining: Int,
    val totalAvailableToman: Long,
    val totalExpenseThisCycleToman: Long,
    val spentTodayToman: Long,
    val cycleStartDate: JalaliDate,
    val nextPaydayDate: JalaliDate,
    val burnRateStatus: BurnRateStatus,
    val dailyBudgetCapToman: Long,
)

object PaydayCycleCalculator {

    /**
     * Calculates the active financial cycle based on user's payday preference.
     * @param today The reference date (defaults to JalaliDate.today())
     * @param paydayDay The day of month the user gets paid (1..31). 1 means standard 1st of month.
     */
    fun calculateCycle(
        today: JalaliDate = JalaliDate.today(),
        paydayDay: Int = 1,
    ): PaydayCycle {
        val clampedPayday = paydayDay.coerceIn(1, 31)

        val (startDate, nextPaydayDate) = if (clampedPayday == 1) {
            val start = JalaliDate(today.year, today.month, 1)
            val end = start.plusMonths(1)
            start to end
        } else {
            if (today.day >= clampedPayday) {
                // We are after payday this month: cycle started on payday of this month
                val startMonthLength = JalaliCalendar.daysInMonth(today.year, today.month)
                val startDay = clampedPayday.coerceAtMost(startMonthLength)
                val start = JalaliDate(today.year, today.month, startDay)

                val nextMonthDate = today.plusMonths(1)
                val nextMonthLength = JalaliCalendar.daysInMonth(nextMonthDate.year, nextMonthDate.month)
                val endDay = clampedPayday.coerceAtMost(nextMonthLength)
                val end = JalaliDate(nextMonthDate.year, nextMonthDate.month, endDay)
                start to end
            } else {
                // We are before payday this month: cycle started on payday of previous month
                val prevMonthDate = today.plusMonths(-1)
                val prevMonthLength = JalaliCalendar.daysInMonth(prevMonthDate.year, prevMonthDate.month)
                val startDay = clampedPayday.coerceAtMost(prevMonthLength)
                val start = JalaliDate(prevMonthDate.year, prevMonthDate.month, startDay)

                val thisMonthLength = JalaliCalendar.daysInMonth(today.year, today.month)
                val endDay = clampedPayday.coerceAtMost(thisMonthLength)
                val end = JalaliDate(today.year, today.month, endDay)
                start to end
            }
        }

        val startEpochDay = startDate.toEpochDay()
        val endEpochDayExclusive = nextPaydayDate.toEpochDay()
        val todayEpochDay = today.toEpochDay()

        val totalDays = (endEpochDayExclusive - startEpochDay).toInt().coerceAtLeast(1)
        val daysRemaining = (endEpochDayExclusive - todayEpochDay).toInt().coerceAtLeast(1)
        val daysElapsed = (todayEpochDay - startEpochDay).toInt().coerceAtLeast(0)

        val label = if (clampedPayday == 1) {
            Digits.toPersian("${startDate.monthName()} ${startDate.year}")
        } else {
            Digits.toPersian("${startDate.day} ${startDate.monthName()} تا ${nextPaydayDate.day} ${nextPaydayDate.monthName()}")
        }

        return PaydayCycle(
            startDate = startDate,
            nextPaydayDate = nextPaydayDate,
            startEpochDay = startEpochDay,
            endEpochDayExclusive = endEpochDayExclusive,
            totalDays = totalDays,
            daysRemaining = daysRemaining,
            daysElapsed = daysElapsed,
            label = label,
        )
    }

    /**
     * Calculates the daily safe-to-spend amount and status.
     *
     * @param totalLiquidBalanceToman Current total balance of liquid accounts (cash, cards, bank)
     * @param totalCycleExpenseToman Total expenses incurred during this current cycle so far
     * @param spentTodayToman Total expenses recorded today
     * @param cycle The current payday cycle
     * @param monthlyBudgetLimitToman Optional total budget ceiling for the month/cycle
     * @param savingsGoalsAllocatedToman Total money set aside for active savings goals
     */
    fun calculateSafeToSpend(
        totalLiquidBalanceToman: Long,
        totalCycleExpenseToman: Long,
        spentTodayToman: Long,
        cycle: PaydayCycle,
        monthlyBudgetLimitToman: Long? = null,
        savingsGoalsAllocatedToman: Long = 0L,
    ): DailySafeToSpend {
        // Funds available for the remainder of the cycle
        val effectiveLiquid = (totalLiquidBalanceToman - savingsGoalsAllocatedToman).coerceAtLeast(0L)

        val availableFunds = if (monthlyBudgetLimitToman != null && monthlyBudgetLimitToman > 0) {
            val budgetRemaining = (monthlyBudgetLimitToman - totalCycleExpenseToman).coerceAtLeast(0L)
            minOf(effectiveLiquid, budgetRemaining)
        } else {
            effectiveLiquid
        }

        val safeToSpendToday = if (cycle.daysRemaining > 0) {
            (availableFunds / cycle.daysRemaining).coerceAtLeast(0L)
        } else {
            availableFunds
        }

        val burnRateStatus = when {
            availableFunds <= 0L -> BurnRateStatus.DANGER
            spentTodayToman > safeToSpendToday && safeToSpendToday > 0 -> BurnRateStatus.DANGER
            spentTodayToman > (safeToSpendToday * 0.8f) -> BurnRateStatus.CAUTION
            spentTodayToman < (safeToSpendToday * 0.5f) -> BurnRateStatus.EXCELLENT
            else -> BurnRateStatus.NORMAL
        }

        return DailySafeToSpend(
            safeToSpendTodayToman = safeToSpendToday,
            daysRemaining = cycle.daysRemaining,
            totalAvailableToman = availableFunds,
            totalExpenseThisCycleToman = totalCycleExpenseToman,
            spentTodayToman = spentTodayToman,
            cycleStartDate = cycle.startDate,
            nextPaydayDate = cycle.nextPaydayDate,
            burnRateStatus = burnRateStatus,
            dailyBudgetCapToman = safeToSpendToday,
        )
    }
}
