package com.braveboy.hesabi.core.assistant

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.logic.DailySafeToSpend
import com.braveboy.hesabi.domain.logic.SmartCategorySuggester
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType

data class FinancialContext(
    val period: MonthPeriod,
    val today: JalaliDate,
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val totalBalanceToman: Long,
    val safeToSpend: DailySafeToSpend? = null,
    val budgets: List<Budget> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
)

data class FinancialAnswer(
    val text: String,
    val transactionIds: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
)

/**
 * Answers simple money questions from the local ledger. It is intentionally
 * deterministic: every number in the answer can be traced back to transactions.
 */
object FinancialQueryEngine {

    private val monthWords = listOf("این ماه", "ماه جاری", "ماه", "این دوره")
    private val helpSuggestions = listOf(
        "این ماه چقدر خرج کردم؟",
        "بیشترین هزینهٔ این ماه کدام دسته بود؟",
        "برای خوراک این ماه چقدر خرج کردم؟",
        "چقدر می‌توانم امروز خرج کنم؟",
    )

    fun ask(question: String, context: FinancialContext): FinancialAnswer {
        val normalized = question.trim()
        if (normalized.isBlank()) {
            return FinancialAnswer(
                text = "سؤالت را بنویس؛ مثلاً «این ماه چقدر خرج کردم؟»",
                suggestions = helpSuggestions,
            )
        }
        val inMonth = context.transactions.filter {
            !it.isDeleted && context.period.contains(it.occurredOnEpochDay)
        }
        val monthExpenses = inMonth.filter { it.type == TransactionType.EXPENSE }
        val monthIncome = inMonth.filter { it.type == TransactionType.INCOME }
        val todayExpenses = monthExpenses.filter { it.occurredOnEpochDay == context.today.toEpochDay() }

        // "بیشترین هزینه کدام دسته بود؟"
        if ((normalized.contains("بیشترین") || normalized.contains("بیشتر")) &&
            (normalized.contains("خرج") || normalized.contains("هزینه"))
        ) {
            val slices = com.braveboy.hesabi.domain.logic.ReportCalculator
                .totalExpenseByCategory(inMonth, context.categories)
            val top = slices.firstOrNull()
                ?: return FinancialAnswer("در ${context.period.label} هنوز هزینه‌ای ثبت نشده است.")
            return FinancialAnswer(
                text = "بیشترین هزینهٔ ${context.period.label} در «${top.name}» با " +
                    Money.format(top.amountToman) + " بوده است.",
                transactionIds = monthExpenses
                    .filter { it.categoryId == top.categoryId }
                    .sortedByDescending { it.occurredOnEpochDay }
                    .take(20)
                    .map { it.id },
            )
        }

        // "برای خوراک این ماه چقدر خرج کردم؟"
        if (normalized.contains("چقدر") || normalized.contains("چند")) {
            val suggestion = SmartCategorySuggester.suggest(
                text = normalized,
                history = context.transactions,
                categories = context.categories,
                kind = CategoryKind.EXPENSE,
            )
            if (suggestion != null && (normalized.contains("خرج") || normalized.contains("هزینه"))) {
                val categoryTransactions = monthExpenses.filter { it.categoryId == suggestion.categoryId }
                val total = categoryTransactions.sumOf { it.amountToman }
                return FinancialAnswer(
                    text = "برای «${suggestion.categoryName}» در ${context.period.label} " +
                        Money.format(total) + " خرج کرده‌ای.",
                    transactionIds = categoryTransactions
                        .sortedByDescending { it.occurredOnEpochDay }
                        .take(20)
                        .map { it.id },
                    suggestions = listOf("بیشترین هزینهٔ این ماه کدام دسته بود؟"),
                )
            }
        }

        if (normalized.contains("امروز") && (normalized.contains("خرج") || normalized.contains("هزینه"))) {
            val total = todayExpenses.sumOf { it.amountToman }
            val safe = context.safeToSpend
            val tail = safe?.let {
                " سقف امن امروز ${Money.format(it.safeToSpendTodayToman)} است."
            }.orEmpty()
            return FinancialAnswer(
                text = "امروز ${Money.format(total)} خرج کرده‌ای.$tail",
                transactionIds = todayExpenses.map { it.id },
            )
        }

        if (normalized.contains("درآمد") || normalized.contains("واریز") || normalized.contains("حقوق")) {
            val total = monthIncome.sumOf { it.amountToman }
            return FinancialAnswer(
                text = "درآمد ثبت‌شدهٔ ${context.period.label} " + Money.format(total) + " است.",
                transactionIds = monthIncome.sortedByDescending { it.occurredOnEpochDay }.take(20).map { it.id },
            )
        }

        if (normalized.contains("بودجه")) {
            val overall = context.budgets.firstOrNull { it.categoryId.isBlank() }
            val limit = overall?.limitToman
            if (limit == null || limit <= 0) {
                return FinancialAnswer(
                    text = "برای ${context.period.label} بودجه‌ای تعیین نکرده‌ای. از صفحهٔ بودجه می‌توانی سقف ماه را بگذاری.",
                    suggestions = listOf("خرج امن امروز چقدر است؟"),
                )
            }
            val spent = monthExpenses.sumOf { it.amountToman }
            val remaining = (limit - spent).coerceAtLeast(0)
            return FinancialAnswer(
                text = "از بودجهٔ ${Money.format(limit)}، " + Money.format(spent) +
                    " خرج شده و " + Money.format(remaining) + " باقی مانده است.",
            )
        }

        if (normalized.contains("پس‌انداز") || normalized.contains("پس انداز") || normalized.contains("هدف")) {
            val saved = context.goals.sumOf { it.savedAmountToman }
            val target = context.goals.sumOf { it.targetAmountToman }
            return FinancialAnswer(
                text = if (context.goals.isEmpty()) {
                    "هنوز هدف پس‌اندازی نساخته‌ای."
                } else {
                    "روی ${context.goals.size} هدف، " + Money.format(saved) +
                        " از " + Money.format(target) + " پس‌انداز کرده‌ای."
                },
            )
        }

        if (normalized.contains("موجودی") || normalized.contains("مانده") &&
            !normalized.contains("بودجه")
        ) {
            return FinancialAnswer(
                text = "موجودی کل حساب‌های فعال " + Money.format(context.totalBalanceToman) + " است.",
            )
        }

        // "چقدر می‌توانم خرج کنم؟"
        if (normalized.contains("خرج کنم") || normalized.contains("بتونم") ||
            normalized.contains("می‌توانم") || normalized.contains("امن")
        ) {
            val safe = context.safeToSpend
            return if (safe == null) {
                FinancialAnswer("برای محاسبهٔ خرج امن، ابتدا حقوق و روز حقوق را در تنظیمات ثبت کن.")
            } else {
                FinancialAnswer(
                    text = "امروز می‌توانی حدود " + Money.format(safe.safeToSpendTodayToman) +
                        " خرج کنی. ${safe.daysRemaining} روز تا پایان دورهٔ حقوقی باقی است.",
                )
            }
        }

        if (normalized.contains("چند تراکنش") || normalized.contains("تعداد")) {
            return FinancialAnswer(
                text = "در ${context.period.label} تعداد ${inMonth.count { it.affectsIncomeExpense }} تراکنش ثبت شده است.",
                transactionIds = inMonth.take(20).map { it.id },
            )
        }

        // Default: month summary with a useful next step.
        val expense = monthExpenses.sumOf { it.amountToman }
        val income = monthIncome.sumOf { it.amountToman }
        return FinancialAnswer(
            text = "در ${context.period.label}: " + Money.format(expense) + " هزینه و " +
                Money.format(income) + " درآمد ثبت شده است.",
            transactionIds = monthExpenses.sortedByDescending { it.occurredOnEpochDay }.take(20).map { it.id },
            suggestions = helpSuggestions,
        )
    }
}
