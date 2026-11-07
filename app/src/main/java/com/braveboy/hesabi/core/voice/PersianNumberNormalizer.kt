package com.braveboy.hesabi.core.voice

import com.braveboy.hesabi.core.money.Digits

/**
 * Converts spoken Persian number words and phrases into numeric Long values (in Toman).
 * Handles colloquial / informal speech patterns common in daily Iranian usage.
 *
 * Examples:
 * - "پنجاه هزار تومان" → 50_000L
 * - "پنجاه تومن" → 50_000L  (colloquial "تومن" implies thousands)
 * - "یک و دویست" → 1_200_000L  (shorthand for یک میلیون و دویست هزار)
 * - "سیصد و پنجاه هزار" → 350_000L
 * - "دو میلیون و نیم" → 2_500_000L
 * - "سه تومن" → 3_000L (colloquial)
 * - "هشتاد و پنج تومان" → 85_000L
 * - "۵۰ هزار تومان" → 50_000L (mixed digit+word)
 * - "50000" → 50_000L (plain digits)
 * - "سیصد و پنجاه هزار ریال" → 35_000L (convert rial to toman)
 */
object PersianNumberNormalizer {

    // Map of Persian word numbers to values
    private val ones = mapOf(
        "صفر" to 0L, "یک" to 1L, "یه" to 1L, "دو" to 2L, "سه" to 3L,
        "چهار" to 4L, "پنج" to 5L, "شش" to 6L, "شیش" to 6L,
        "هفت" to 7L, "هشت" to 8L, "نه" to 9L, "نُه" to 9L,
        "ده" to 10L, "یازده" to 11L, "دوازده" to 12L, "سیزده" to 13L,
        "چهارده" to 14L, "پانزده" to 15L, "پونزده" to 15L,
        "شانزده" to 16L, "هفده" to 17L, "هجده" to 18L, "هیجده" to 18L,
        "نوزده" to 19L,
        "بیست" to 20L, "سی" to 30L, "چهل" to 40L, "پنجاه" to 50L,
        "شصت" to 60L, "هفتاد" to 70L, "هشتاد" to 80L, "نود" to 90L,
        "صد" to 100L, "دویست" to 200L, "سیصد" to 300L, "چهارصد" to 400L,
        "پانصد" to 500L, "پونصد" to 500L, "ششصد" to 600L, "هفتصد" to 700L,
        "هشتصد" to 800L, "نهصد" to 900L,
    )

    private val multipliers = mapOf(
        "هزار" to 1_000L,
        "میلیون" to 1_000_000L, "ملیون" to 1_000_000L, "میلیارد" to 1_000_000_000L,
    )

    /**
     * Parse the full spoken text and extract the monetary amount in Toman.
     * Returns null if no amount could be parsed.
     */
    fun parseToman(text: String): Long? {
        val normalized = normalize(text)
        if (normalized.isBlank()) return null

        // Try direct numeric extraction first (e.g., "۵۰۰۰۰" or "50000")
        val directDigit = extractDirectDigits(normalized)
        if (directDigit != null) {
            return applyUnitAndCurrency(directDigit, normalized)
        }

        // Parse word-based numbers
        val wordValue = parseWordNumber(normalized) ?: return null
        return applyUnitAndCurrency(wordValue, normalized)
    }

    private fun normalize(text: String): String {
        var result = Digits.toAscii(text)
            .trim()
            .lowercase()
        // Replace colloquial تومن with تومان for consistency in checks,
        // but keep original for detection (we work on the normalized copy)
        // Normalize whitespace
        result = result.replace(Regex("\\s+"), " ")
        return result
    }

    private fun extractDirectDigits(text: String): Long? {
        val tokens = text.split(" ")
        if (tokens.any { multipliers.containsKey(it) || ones.containsKey(it) || it == "نیم" }) {
            return null
        }
        for (token in tokens) {
            if (token.all { it.isDigit() } && token.length >= 2) {
                return token.toLongOrNull()
            }
        }
        return null
    }

    private fun parseWordNumber(text: String): Long? {
        val tokens = text.split(" ").filter { it != "و" && it.isNotBlank() }
        if (tokens.isEmpty()) return null

        // Accumulate: track current group value and total
        var total = 0L
        var current = 0L
        var foundAnyNumber = false
        var hasExplicitMultiplier = false

        for (token in tokens) {
            val onesVal = ones[token]
            val multVal = multipliers[token]
            val digitVal = if (token.all { it.isDigit() } && token.isNotEmpty()) token.toLongOrNull() else null

            when {
                token == "نیم" -> {
                    // "نیم" means half of the last multiplier context
                    // If we have a total with millions, add 500_000
                    // If we have a total with thousands, add 500
                    // Default: treat as 500 (half of هزار)
                    if (total >= 1_000_000L || hasMultiplier(tokens, "میلیون") || hasMultiplier(tokens, "ملیون")) {
                        total += 500_000L
                    } else if (total >= 1_000L || hasMultiplier(tokens, "هزار")) {
                        total += 500L
                    } else {
                        total += 500L
                    }
                    foundAnyNumber = true
                }
                onesVal != null -> {
                    current += onesVal
                    foundAnyNumber = true
                }
                digitVal != null -> {
                    current += digitVal
                    foundAnyNumber = true
                }
                multVal != null -> {
                    hasExplicitMultiplier = true
                    if (current == 0L) current = 1L // "هزار" alone means 1000
                    total += current * multVal
                    current = 0L
                }
                // Skip non-number tokens (تومان, ریال, بنزین, etc.)
                else -> { /* skip */ }
            }
        }

        // Add any remaining accumulated value
        total += current

        return if (foundAnyNumber && total > 0L) total else null
    }

    private fun hasMultiplier(tokens: List<String>, word: String): Boolean {
        return tokens.any { it == word }
    }

    private fun applyUnitAndCurrency(value: Long, originalText: String): Long? {
        val isRial = originalText.contains("ریال") || originalText.contains("ريال")
        val isToman = originalText.contains("تومان") || originalText.contains("تومن")
        val hasExplicitHezar = originalText.contains("هزار")
        val hasExplicitMillion = originalText.contains("میلیون") || originalText.contains("ملیون")
        val hasExplicitMilliard = originalText.contains("میلیارد")

        val adjusted = when {
            isRial -> value / 10 // Convert rial to toman
            // Colloquial rule: "پنجاه تومن" means 50,000 toman (implied thousands)
            // But only when there's no explicit multiplier and value is small
            isToman && value < 1000 && !hasExplicitHezar && !hasExplicitMillion && !hasExplicitMilliard -> value * 1000
            else -> value
        }
        return adjusted.takeIf { it > 0 }
    }
}
