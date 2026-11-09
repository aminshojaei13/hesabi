package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.domain.model.SavingsGoal

data class PurchaseImpact(
    val amountToman: Long,
    val affordableNow: Boolean,
    val safeToSpendTodayBeforeToman: Long,
    val safeToSpendTodayAfterToman: Long,
    val daysOfRunwayLost: Int,
    val affectsGoals: Boolean,
    val goalDelayDays: Int,
    val verdict: String,
    val alternatives: List<String>,
)

/**
 * Answers the product question "می‌توانم این را بخرم؟" using the same numbers the
 * home screen shows, never a made-up budget.
 */
object PurchaseSimulator {

    fun simulate(
        amountToman: Long,
        safeToSpend: DailySafeToSpend,
        liquidBalanceToman: Long,
        activeGoals: List<SavingsGoal> = emptyList(),
        daysRemainingOverride: Int? = null,
    ): PurchaseImpact {
        val amount = amountToman.coerceAtLeast(0L)
        val daysRemaining = (daysRemainingOverride ?: safeToSpend.daysRemaining).coerceAtLeast(1)
        val safeBefore = safeToSpend.safeToSpendTodayToman
        val remainingFunds = (safeToSpend.totalAvailableToman - amount).coerceAtLeast(0L)
        val safeAfter = remainingFunds / daysRemaining

        val affordable = amount <= liquidBalanceToman && amount <= safeToSpend.totalAvailableToman
        val runwayLost = if (safeBefore <= 0L) {
            if (amount > 0) daysRemaining else 0
        } else {
            ((amount.toDouble() / safeBefore).toInt()).coerceIn(0, daysRemaining)
        }

        val goalAllocation = activeGoals.sumOf { it.savedAmountToman }
        val affectsGoals = goalAllocation in 1..amount

        val goalDelayDays = if (affectsGoals && activeGoals.isNotEmpty()) {
            val monthlyTarget = activeGoals.sumOf { it.monthlyTargetToman }
            if (monthlyTarget <= 0) 0 else ((amount.toDouble() / monthlyTarget) * 30).toInt().coerceAtLeast(1)
        } else {
            0
        }

        val verdict = when {
            amount <= 0 -> "مبلغی وارد کن تا اثرش را ببینی."
            !affordable -> "با موجودی فعلی، این خرید در بودجهٔ این دوره جا نمی‌شود."
            safeAfter == 0L -> "این خرید تمام بودجهٔ باقی‌ماندهٔ دوره را مصرف می‌کند."
            amount > safeBefore * 3 -> "خرید بزرگی است؛ بعد از آن باید چند روز کم‌خرج باشی."
            else -> "در بودجهٔ فعلی جا می‌شود، اما خرج روزهای بعد کم می‌شود."
        }

        val alternatives = buildList {
            if (!affordable) {
                add("مبلغ را کمتر کن یا برای دورهٔ بعد برنامه‌ریزی کن.")
                add("از یک هدف پس‌انداز به‌اندازهٔ بخشی از مبلغ برداشت کن.")
            } else if (runwayLost > 0) {
                add("خرید را چند روز به تعویق بینداز تا میانگین روزانه جبران شود.")
                add("یک دستهٔ کم‌خرج را برای این هفته انتخاب کن.")
            } else {
                add("می‌توانی همین حالا بخری؛ بودجهٔ روزانه‌ات تغییر محسوسی نمی‌کند.")
            }
        }

        return PurchaseImpact(
            amountToman = amount,
            affordableNow = affordable,
            safeToSpendTodayBeforeToman = safeBefore,
            safeToSpendTodayAfterToman = safeAfter,
            daysOfRunwayLost = runwayLost,
            affectsGoals = affectsGoals,
            goalDelayDays = goalDelayDays,
            verdict = verdict,
            alternatives = alternatives,
        )
    }
}
