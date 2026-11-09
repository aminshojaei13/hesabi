package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType

/**
 * Projects how the current month ends if the user keeps spending at today's pace.
 *
 * The calculation is deliberately transparent: every number in [EndOfMonthForecast]
 * can be explained with the inputs, and the tests cover 29/30/31-day months and the
 * first day of a month (when no daily average exists yet).
 */
data class EndOfMonthForecast(
    val period: MonthPeriod,
    val daysElapsed: Int,
    val daysRemaining: Int,
    val spentSoFarToman: Long,
    val incomeSoFarToman: Long,
    val currentBalanceToman: Long,
    val averageDailyExpenseToman: Long,
    val projectedExpenseToman: Long,
    val upcomingCommitmentsToman: Long,
    val projectedRemainingBalanceToman: Long,
    val shortfallToman: Long,
    val safeDailyAfterCommitmentsToman: Long,
    val status: ForecastStatus,
) {
    val isAtRisk: Boolean get() = status == ForecastStatus.AT_RISK || status == ForecastStatus.CRITICAL

    fun headline(): String = when (status) {
        ForecastStatus.ON_TRACK -> "با این روند، پایان ماه را با ماندهٔ مثبت تمام می‌کنی."
        ForecastStatus.TIGHT -> "پایان ماه نزدیک به صفر است؛ کمی احتیاط لازم است."
        ForecastStatus.AT_RISK -> "با این روند، پایان ماه کم می‌آوری."
        ForecastStatus.CRITICAL -> "همین حالا از بودجهٔ ماه عبور کرده‌ای."
    }
}

enum class ForecastStatus { ON_TRACK, TIGHT, AT_RISK, CRITICAL }

object MonthForecastCalculator {

    fun forecast(
        period: MonthPeriod,
        today: JalaliDate,
        transactions: List<Transaction>,
        currentBalanceToman: Long,
        upcomingCommitmentsToman: Long = 0L,
        monthlyBudgetLimitToman: Long? = null,
        expectedRemainingIncomeToman: Long = 0L,
    ): EndOfMonthForecast {
        val todayEpochDay = today.toEpochDay()
        val daysInMonth = period.days
        val daysElapsed = if (todayEpochDay < period.startEpochDay) {
            0
        } else {
            ((todayEpochDay - period.startEpochDay) + 1).toInt().coerceIn(0, daysInMonth)
        }
        val daysRemaining = (daysInMonth - daysElapsed).coerceAtLeast(0)

        val inMonth = transactions.filter {
            period.contains(it.occurredOnEpochDay) && it.affectsIncomeExpense
        }
        val spent = inMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountToman }
        val income = inMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amountToman }

        val divisor = daysElapsed.coerceAtLeast(1)
        val averageDaily = spent / divisor
        val projectedExpense = spent + averageDaily * daysRemaining
        val projectedAvailable = currentBalanceToman + expectedRemainingIncomeToman -
            upcomingCommitmentsToman
        val projectedRemaining = projectedAvailable - (projectedExpense - spent)
        val shortfall = (-projectedRemaining).coerceAtLeast(0L)
        val safeDailyAfterCommitments = if (daysRemaining > 0) {
            (projectedAvailable - (projectedExpense - spent)).coerceAtLeast(0L) / daysRemaining
        } else {
            0L
        }

        val budgetLimit = monthlyBudgetLimitToman?.takeIf { it > 0 }
        val status = when {
            budgetLimit != null && spent > budgetLimit -> ForecastStatus.CRITICAL
            projectedRemaining < 0L -> ForecastStatus.AT_RISK
            budgetLimit != null && projectedExpense > budgetLimit -> ForecastStatus.AT_RISK
            projectedExpense > 0 && projectedRemaining < projectedExpense / 10 -> ForecastStatus.TIGHT
            else -> ForecastStatus.ON_TRACK
        }

        return EndOfMonthForecast(
            period = period,
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining,
            spentSoFarToman = spent,
            incomeSoFarToman = income,
            currentBalanceToman = currentBalanceToman,
            averageDailyExpenseToman = averageDaily,
            projectedExpenseToman = projectedExpense,
            upcomingCommitmentsToman = upcomingCommitmentsToman,
            projectedRemainingBalanceToman = projectedRemaining,
            shortfallToman = shortfall,
            safeDailyAfterCommitmentsToman = safeDailyAfterCommitments,
            status = status,
        )
    }
}
