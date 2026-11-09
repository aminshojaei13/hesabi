package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType

data class CategorySuggestion(
    val categoryId: String,
    val categoryName: String,
    val confidence: Float,
    val reason: String,
)

/**
 * Local-only category suggestion: keyword rules first, then the user's own history.
 * No network call and no model download, matching the "offline first" promise.
 */
object SmartCategorySuggester {

    private val keywords: Map<String, List<String>> = mapOf(
        "cat_food" to listOf(
            "ناهار", "شام", "صبحانه", "کافه", "رستوران", "فست فود", "پیتزا", "ساندویچ",
            "قهوه", "چای", "بستنی", "شیرینی", "نان", "سوپرمارکت", "بقالی", "میوه",
        ),
        "cat_transport" to listOf(
            "تاکسی", "اسنپ", "سفر", "مترو", "اتوبوس", "بنزین", "گازوئیل", "پارکینگ",
            "جریمه", "قطار", "بلیط", "هواپیما", "اتوکار",
        ),
        "cat_home" to listOf("اجاره", "شارژ", "خانه", "تعمیر", "لوازم خانگی", "مبلمان", "فرش"),
        "cat_health" to listOf("دارو", "دکتر", "پزشک", "بیمارستان", "درمان", "آزمایش", "دندان", "ورزش"),
        "cat_shopping" to listOf("خرید", "لباس", "کفش", "پوشاک", "فروشگاه", "آنلاین", "دیجی کالا"),
        "cat_bills" to listOf("قبض", "برق", "آب", "گاز", "تلفن", "اینترنت", "همراه", "شارژ سیم‌کارت"),
        "cat_fun" to listOf("سینما", "بازی", "کنسرت", "تفریح", "سرگرمی", "اشتراک", "نتفلیکس", "اسپاتیفای"),
        "cat_education" to listOf("کتاب", "کلاس", "دوره", "مدرسه", "شهریه", "آموزش", "جزوه"),
        "cat_gift" to listOf("هدیه", "کادو", "تولد", "عروسی", "گل"),
        "cat_salary" to listOf("حقوق", "دستمزد", "پاداش", "اضافه کار"),
        "cat_business" to listOf("فروش", "درآمد", "کسب و کار", "مشتری", "سفارش"),
    )

    fun suggest(
        text: String,
        history: List<Transaction>,
        categories: List<Category>,
        kind: CategoryKind = CategoryKind.EXPENSE,
    ): CategorySuggestion? {
        val normalized = Digits.toAscii(text).replace("\u200c", " ").trim()
        if (normalized.isBlank()) return null
        val candidates = categories.filter { it.kind == kind && !it.archived }
        if (candidates.isEmpty()) return null

        val scores = HashMap<String, Float>()
        val reasons = HashMap<String, String>()

        for (category in candidates) {
            // The category name itself is the strongest signal.
            val nameToken = Digits.toAscii(category.name).replace("\u200c", " ")
            if (nameToken.length >= 2 && normalized.contains(nameToken, ignoreCase = true)) {
                scores[category.id] = (scores[category.id] ?: 0f) + 5f
                reasons[category.id] = "نام دسته در متن آمده است"
            }
            val keywordsForCategory = keywords[category.id].orEmpty()
            for (keyword in keywordsForCategory) {
                if (normalized.contains(keyword, ignoreCase = true)) {
                    scores[category.id] = (scores[category.id] ?: 0f) + 3f
                    reasons.putIfAbsent(category.id, "کلیدواژهٔ «$keyword»")
                }
            }
        }

        // Learn from similar past notes: "کافه نگاه" seen before is a strong hint.
        val tokens = normalized.split(Regex("\\s+")).filter { it.length >= 3 }
        if (tokens.isNotEmpty()) {
            for (transaction in history) {
                if (transaction.type == TransactionType.TRANSFER) continue
                val categoryId = transaction.categoryId ?: continue
                if (candidates.none { it.id == categoryId }) continue
                val note = Digits.toAscii(transaction.note).replace("\u200c", " ")
                if (note.isBlank()) continue
                val overlap = tokens.count { token -> note.contains(token, ignoreCase = true) }
                if (overlap > 0) {
                    scores[categoryId] = (scores[categoryId] ?: 0f) + overlap
                    reasons.putIfAbsent(categoryId, "در تراکنش‌های قبلی همین دسته بود")
                }
            }
        }

        val best = scores.maxByOrNull { it.value } ?: return null
        val category = candidates.firstOrNull { it.id == best.key } ?: return null
        val confidence = (best.value / 12f).coerceIn(0.2f, 1f)
        return CategorySuggestion(
            categoryId = category.id,
            categoryName = category.name,
            confidence = confidence,
            reason = reasons[best.key] ?: "بر اساس سابقه",
        )
    }
}
