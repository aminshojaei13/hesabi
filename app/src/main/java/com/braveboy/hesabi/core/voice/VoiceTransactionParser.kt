package com.braveboy.hesabi.core.voice

import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.domain.model.TransactionType

/**
 * Result of parsing voice input into a potential transaction.
 */
data class VoiceParsedTransaction(
    val amountToman: Long?,
    val type: TransactionType,
    val categoryHint: String?,
    val accountHint: String?,
    val note: String,
    val confidence: Float,
)

/**
 * Pure, deterministic parser that extracts financial entities from
 * Persian spoken text. No Android dependencies - fully unit testable.
 *
 * Usage:
 *   val result = VoiceTransactionParser.parse("پنجاه تومن بنزین با کارت ملت")
 *   // result.amountToman = 50_000
 *   // result.type = EXPENSE
 *   // result.categoryHint = "cat_transport" (fuel)
 *   // result.accountHint = "ملت"
 */
object VoiceTransactionParser {

    // Category keyword mappings (category ID -> list of trigger words)
    private val categoryKeywords: Map<String, List<String>> = mapOf(
        "cat_food" to listOf(
            "غذا", "ناهار", "شام", "صبحانه", "رستوران", "فست‌فود", "فست فود",
            "کافه", "قهوه", "چای", "اسنپ‌فود", "اسنپ فود", "ساندویچ", "پیتزا", "کباب",
            "سوپرمارکت", "میوه", "نان", "نانوایی", "شیرینی", "بستنی",
        ),
        "cat_transport" to listOf(
            "بنزین", "سوخت", "تاکسی", "اسنپ", "تپسی", "مترو",
            "اتوبوس", "بلیط", "پارکینگ", "کارواش", "تعمیرگاه", "لاستیک", "ماشین",
            "بیمه ماشین", "عوارض", "جریمه",
        ),
        "cat_home" to listOf(
            "اجاره", "قبض", "برق", "آب", "گاز", "تلفن", "اینترنت",
            "شارژ ساختمان", "تعمیرات", "لوازم خانه", "مبل",
        ),
        "cat_health" to listOf(
            "دکتر", "دارو", "داروخانه", "بیمارستان", "ویزیت",
            "آزمایش", "دندان", "دندانپزشکی", "عینک", "چشم‌پزشک",
        ),
        "cat_shopping" to listOf(
            "خرید", "لباس", "کفش", "کیف", "ساعت", "عطر",
            "آرایشی", "بهداشتی", "موبایل", "لپ‌تاپ", "لوازم",
        ),
        "cat_bills" to listOf("قبض", "فیش", "مالیات", "بیمه", "عوارض"),
        "cat_fun" to listOf(
            "سینما", "تئاتر", "کنسرت", "بازی", "سرگرمی",
            "استخر", "باشگاه", "ورزش", "ماهواره", "فیلم",
        ),
        "cat_education" to listOf(
            "کتاب", "دانشگاه", "کلاس", "آموزش", "دوره",
            "مدرسه", "کنکور",
        ),
        "cat_gift" to listOf("هدیه", "کادو", "صدقه", "خیریه", "کمک"),
        "cat_salary" to listOf("حقوق", "دستمزد", "پاداش", "عیدی", "اضافه‌کار"),
        "cat_business" to listOf("فروش", "درآمد فروش", "مشتری"),
    )

    // Expense-indicating verbs/phrases
    private val expenseIndicators = listOf(
        "خریدم", "دادم", "زدم", "پرداخت", "پرداختم", "خرج", "هزینه",
        "گرفتم", "رفتم", "خوردم", "خوردیم", "رفتیم",
        "حساب کردم", "پول دادم", "کارت کشیدم",
    )

    // Income-indicating verbs/phrases
    private val incomeIndicators = listOf(
        "واریز", "واریز شد", "ریخت", "ریختن", "حقوق", "دریافت",
        "دریافتی", "گرفتم از", "سود", "برگشت",
        "حقوقم اومد", "پول اومد", "درآمد",
    )

    // Common bank name aliases
    private val bankAliases = mapOf(
        "ملت" to "ملت", "ملی" to "ملی", "صادرات" to "صادرات",
        "تجارت" to "تجارت", "سپه" to "سپه", "پاسارگاد" to "پاسارگاد",
        "سامان" to "سامان", "پارسیان" to "پارسیان", "آینده" to "آینده",
        "رفاه" to "رفاه", "مسکن" to "مسکن", "شهر" to "شهر",
        "کشاورزی" to "کشاورزی", "دی" to "دی", "سینا" to "سینا",
        "بلوبانک" to "بلوبانک", "بلو" to "بلوبانک",
        "نقدی" to "نقدی", "نقد" to "نقدی", "کیف پول" to "کیف پول",
    )

    fun parse(spokenText: String): VoiceParsedTransaction {
        val normalized = normalize(spokenText)

        val amount = PersianNumberNormalizer.parseToman(normalized)
        val type = detectType(normalized)
        val category = detectCategory(normalized)
        val account = detectAccount(normalized)
        val note = buildNote(normalized, amount, category, account)

        var confidence = 0.3f
        if (amount != null) confidence += 0.3f
        if (category != null) confidence += 0.15f
        if (account != null) confidence += 0.1f
        if (type != TransactionType.EXPENSE || incomeIndicators.any { normalized.contains(it) }) confidence += 0.15f
        confidence = confidence.coerceIn(0f, 1f)

        return VoiceParsedTransaction(
            amountToman = amount,
            type = type,
            categoryHint = category,
            accountHint = account,
            note = note,
            confidence = confidence,
        )
    }

    private fun normalize(text: String): String {
        return Digits.toAscii(text)
            .replace('\u200c', ' ') // ZWNJ
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }

    private fun detectType(text: String): TransactionType {
        if (incomeIndicators.any { text.contains(it) }) return TransactionType.INCOME
        // Default to expense for ambiguous cases
        return TransactionType.EXPENSE
    }

    private fun detectCategory(text: String): String? {
        // Find the category with the most keyword matches
        var bestCategory: String? = null
        var bestScore = 0
        for ((catId, keywords) in categoryKeywords) {
            val score = keywords.count { text.contains(it) }
            if (score > bestScore) {
                bestScore = score
                bestCategory = catId
            }
        }
        return bestCategory
    }

    private fun detectAccount(text: String): String? {
        // Check for bank name mentions — iterate longest aliases first to avoid
        // partial matches (e.g., "بلوبانک" before "بلو")
        for ((alias, bankName) in bankAliases.entries.sortedByDescending { it.key.length }) {
            if (text.contains(alias)) return bankName
        }
        return null
    }

    private fun buildNote(text: String, amount: Long?, category: String?, account: String?): String {
        // Return the original spoken text as the note, cleaned up
        return text.trim()
    }
}
