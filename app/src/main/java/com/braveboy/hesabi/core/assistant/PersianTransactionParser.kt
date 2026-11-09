package com.braveboy.hesabi.core.assistant

import com.braveboy.hesabi.core.date.JalaliCalendar
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.logic.SmartCategorySuggester
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType

data class AssistantEntry(
    val amountToman: Long,
    val type: TransactionType,
    val occurredOnEpochDay: Long,
    val categoryId: String?,
    val categoryName: String?,
    val categoryReason: String?,
    val merchant: String?,
    val note: String,
    val confidence: Float,
    val explanation: List<String>,
)

/**
 * Rule-based Persian transaction parser. Runs fully offline; no text ever leaves the
 * device when the user keeps the default "local" AI mode.
 */
object PersianTransactionParser {

    private val expenseWords = listOf(
        "خریدم", "خرید", "دادم", "پرداخت", "هزینه", "خرج", "خرج کردم", "صرف",
        "اجاره", "قسط", "قبض", "شارژ", "بدهی",
    )
    private val incomeWords = listOf(
        "گرفتم", "درآمد", "فروختم", "واریز", "حقوق", "پاداش", "طلب", "برگشت",
        "دریافت", "کارکرد",
    )
    private val transferWords = listOf("انتقال", "ریختم", "جابه‌جا", "منتقل")

    fun parse(
        text: String,
        categories: List<Category>,
        history: List<Transaction> = emptyList(),
        today: JalaliDate = JalaliDate.today(),
    ): AssistantEntry? {
        val original = text.trim()
        if (original.isEmpty()) return null
        val normalized = Digits.toAscii(original)
            .replace("\u200c", " ")
            .replace("ي", "ی")
            .replace("ك", "ک")

        val amount = findAmount(normalized)
        val type = detectType(normalized)
        val date = detectDate(normalized, today) ?: today
        val merchant = detectMerchant(normalized)
        val categoryText = "$normalized ${merchant.orEmpty()}"
        val suggestion = SmartCategorySuggester.suggest(
            text = categoryText,
            history = history,
            categories = categories,
            kind = if (type == TransactionType.INCOME) com.braveboy.hesabi.domain.model.CategoryKind.INCOME
            else com.braveboy.hesabi.domain.model.CategoryKind.EXPENSE,
        )

        if (amount == null && type == TransactionType.EXPENSE) return null

        val explanation = buildList {
            add(
                when (type) {
                    TransactionType.EXPENSE -> "این متن را یک هزینه دیدم."
                    TransactionType.INCOME -> "این متن را یک درآمد دیدم."
                    TransactionType.TRANSFER -> "این متن را یک انتقال بین حساب‌ها دیدم."
                },
            )
            amount?.let { add("مبلغ تشخیص‌داده‌شده: ${Money.format(it)}") }
            if (date != today) add("تاریخ از متن خوانده شد: ${date.formatDayMonth()}")
            suggestion?.let { add("دستهٔ پیشنهادی «${it.categoryName}» چون ${it.reason}.") }
        }

        var confidence = 0.35f
        if (amount != null) confidence += 0.35f
        if (expenseWords.any { normalized.contains(it) } || incomeWords.any { normalized.contains(it) }) {
            confidence += 0.15f
        }
        if (date != today || normalized.contains("امروز")) confidence += 0.05f
        if (suggestion != null) confidence += 0.10f

        return AssistantEntry(
            amountToman = amount ?: 0L,
            type = type,
            occurredOnEpochDay = date.toEpochDay(),
            categoryId = suggestion?.categoryId,
            categoryName = suggestion?.categoryName,
            categoryReason = suggestion?.reason,
            merchant = merchant,
            note = original.take(200),
            confidence = confidence.coerceIn(0f, 0.99f),
            explanation = explanation,
        )
    }

    fun findAmount(normalized: String): Long? {
        val withUnit = Regex(
            """([0-9][0-9٬,٫.]*)\s*(میلیارد|میلیون|هزار|تومان|تومن|ریال|ريال)?""",
        ).findAll(normalized)
            .mapNotNull { match ->
                val value = Money.parseToman(match.value) ?: return@mapNotNull null
                // A bare number under 32 is probably a day, not an amount.
                if (match.groupValues[2].isBlank() && value < 100) return@mapNotNull null
                value to match.groupValues[2].isNotBlank()
            }
            .toList()
        return withUnit.maxByOrNull { (value, hasUnit) -> (if (hasUnit) 1_000_000_000_000L else 0L) + value }?.first
    }

    private fun detectType(normalized: String): TransactionType {
        if (transferWords.any { normalized.contains(it) } &&
            (normalized.contains("به ") || normalized.contains("حساب"))
        ) {
            return TransactionType.TRANSFER
        }
        val income = incomeWords.count { normalized.contains(it) }
        val expense = expenseWords.count { normalized.contains(it) }
        return if (income > expense) TransactionType.INCOME else TransactionType.EXPENSE
    }

    private fun detectDate(normalized: String, today: JalaliDate): JalaliDate? {
        when {
            normalized.contains("پریروز") -> return today.plusDays(-2)
            normalized.contains("دیروز") -> return today.plusDays(-1)
            normalized.contains("امروز") || normalized.contains("همین حالا") -> return today
            normalized.contains("فردا") -> return today.plusDays(1)
        }
        Regex("""(\d{1,2})\s*روز\s*(?:پیش|قبل)""").find(normalized)?.let { match ->
            match.groupValues[1].toIntOrNull()?.let { return today.plusDays(-it.toLong()) }
        }
        Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})""").find(normalized)?.let {
            JalaliDate.parse(it.value)?.let { date -> return date }
        }
        // "۵ آبان" or "۵ آبان ۱۴۰۴"
        val monthNames = JalaliCalendar.MONTHS
        Regex("""(\d{1,2})\s*(فروردین|اردیبهشت|خرداد|تیر|مرداد|شهریور|مهر|آبان|آذر|دی|بهمن|اسفند)(?:\s*(\d{4}))?""")
            .find(normalized)?.let { match ->
                val day = match.groupValues[1].toIntOrNull() ?: return@let
                val month = monthNames.indexOf(match.groupValues[2]) + 1
                if (month !in 1..12) return@let
                val year = match.groupValues[3].toIntOrNull() ?: today.year
                runCatching { JalaliDate(year, month, day) }.getOrNull()?.let { date ->
                    // A date without a year that is in the future means the previous year.
                    return if (match.groupValues[3].isBlank() && date > today) {
                        date.copy(year = date.year - 1)
                    } else {
                        date
                    }
                }
            }
        val weekdays = JalaliCalendar.WEEKDAYS
        for (index in weekdays.indices) {
            if (normalized.contains(weekdays[index])) {
                val todayIndex = today.weekdayIndex()
                var delta = index - todayIndex
                if (delta > 0) delta -= 7
                return today.plusDays(delta.toLong())
            }
        }
        return null
    }

    private fun detectMerchant(normalized: String): String? {
        val stopWords = setOf(
            "حساب", "کارت", "بانک", "نقدی", "امروز", "دیروز", "تومان", "تومن", "ریال",
            "هزار", "میلیون", "میلیارد", "خرج", "هزینه", "خرید", "پرداخت", "دادم",
            "گرفتم", "واریز", "درآمد", "حقوق", "برای", "بابت",
        )
        val candidates = Regex("""(?:از|در|به|برای|بابت)\s+([^\d]{2,40}?)(?=\s(?:خریدم|دادم|پرداخت|گرفتم|بود|شد|کردم)|[،.!؟]|$)""")
            .findAll(normalized)
            .map { it.groupValues[1].trim() }
            .filter { value ->
                value.isNotBlank() &&
                    value.split(" ").none { it in stopWords } &&
                    value.split(" ").size <= 5
            }
            .toList()
        return candidates.lastOrNull()?.take(40)
    }
}
