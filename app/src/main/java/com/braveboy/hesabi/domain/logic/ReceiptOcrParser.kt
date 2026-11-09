package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Digits
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Everything the app is allowed to read out of a receipt image.
 *
 * The parser never persists anything by itself: the UI shows this result inside an
 * editable confirmation sheet and the user must approve it before a transaction is
 * written. That keeps the "no data without confirmation" rule of phase 3.
 */
data class ReceiptOcrResult(
    val amountToman: Long? = null,
    val merchant: String? = null,
    val date: JalaliDate? = null,
    val rawText: String = "",
    val amountFoundByKeyword: Boolean = false,
) {
    val hasAnything: Boolean get() = amountToman != null || merchant != null || date != null

    /** 0..1, used only to order the suggested fields, never to auto-save. */
    val confidence: Float
        get() {
            var score = 0f
            if (amountToman != null) score += if (amountFoundByKeyword) 0.6f else 0.35f
            if (merchant != null) score += 0.25f
            if (date != null) score += 0.15f
            return score.coerceAtMost(1f)
        }
}

/**
 * Offline, rule-based receipt reader. ML Kit only turns the picture into lines of
 * text; every amount/date/merchant decision lives here so it can be unit tested
 * with real receipt layouts without a camera or a device.
 */
object ReceiptOcrParser {

    private const val MIN_AMOUNT = 1_000L
    private const val MAX_AMOUNT = 9_999_999_999_999L

    private val amountKeywords = listOf(
        "مبلغ قابل پرداخت", "قابل پرداخت", "مبلغ کل", "جمع کل", "جمع", "مبلغ",
        "بدهی", "پرداخت", "خرید", "total", "amount", "grand total",
    )

    /** Ordered from strongest to weakest; the first match on a line wins. */
    private val amountKeywordStrength = listOf(
        "قابل پرداخت", "مبلغ کل", "جمع کل", "grand total", "total", "مبلغ",
        "جمع", "amount", "بدهی", "پرداخت", "خرید",
    )

    private val merchantPrefixes = listOf(
        "فروشگاه", "فروشكاه", "نام", "شرکت", "شركت", "آبونمان", "کافه", "رستوران",
        "سوپرمارکت", "مارکت", "سوپر", "شعبه", "store", "shop", "market",
    )

    private val merchantNoise = listOf(
        "رسید", "فاکتور", "فاكتور", "قبض", "شماره", "تاریخ", "ساعت", "تراکنش",
        "کارت", "خرید", "مبلغ", "جمع", "تخفیف", "مالیات", "ارزش افزوده",
        "invoice", "receipt", "total", "date", "time", "cash", "card",
    )

    private val dateRegex = Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})""")
    private val timeRegex = Regex("""\b\d{1,2}:\d{2}(?::\d{2})?\b""")
    private val cardLikeRegex = Regex("""\b\d{12,19}\b""")
    private val phoneLikeRegex = Regex("""\b0\d{10}\b""")
    private val numericToken = Regex("""\d[\d.,'\u066C\u066B]*\d|\d""")

    fun parse(rawText: String): ReceiptOcrResult {
        val normalized = Digits.toAscii(sanitize(rawText))
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val keywordAmount = strongestKeywordAmount(lines)
        val fallbackAmount = keywordAmount.first ?: largestPlausibleAmount(lines)
        return ReceiptOcrResult(
            amountToman = keywordAmount.first ?: fallbackAmount,
            amountFoundByKeyword = keywordAmount.first != null,
            merchant = detectMerchant(lines),
            date = detectDate(lines),
            rawText = lines.joinToString("\n"),
        )
    }

    /**
     * OCR often glues RTL text and Latin digits with control characters. Normalize
     * them to spaces so the line pattern still matches.
     */
    private fun sanitize(text: String): String = text
        .replace('\u200E', ' ')
        .replace('\u200F', ' ')
        .replace('\u202A', ' ')
        .replace('\u202B', ' ')
        .replace('\u202C', ' ')
        .replace('\u00A0', ' ')
        .replace('\u066C', ',')
        .replace('\u066B', '.')
        .replace("٫", ".")

    private fun strongestKeywordAmount(lines: List<String>): Pair<Long?, Int> {
        var best: Long? = null
        var bestStrength = Int.MAX_VALUE
        for (line in lines) {
            val lower = line.lowercase()
            val strength = amountKeywordStrength.indexOfFirst { lower.contains(it) }
            if (strength == -1) continue
            val candidate = extractAmount(line) ?: continue
            if (strength < bestStrength) {
                bestStrength = strength
                best = candidate
            }
        }
        return best to bestStrength
    }

    private fun largestPlausibleAmount(lines: List<String>): Long? {
        var best: Long? = null
        for (line in lines) {
            val candidate = extractAmount(line) ?: continue
            if (best == null || candidate > best) best = candidate
        }
        return best
    }

    /**
     * Reads the last plausible number on a line. In Persian receipts the amount sits
     * at the end of the line ("جمع کل ۲٬۵۰۰٬۰۰۰ ریال"), and the first numbers are
     * usually quantity, item code or row number.
     */
    private fun extractAmount(line: String): Long? {
        if (dateRegex.containsMatchIn(line)) return null
        if (timeRegex.containsMatchIn(line)) return null
        if (cardLikeRegex.containsMatchIn(line) && !line.containsAmountKeyword()) return null
        if (phoneLikeRegex.containsMatchIn(line)) return null

        val lowerLine = line.lowercase()
        val isRial = line.contains("ریال") || line.contains("ريال") ||
            lowerLine.contains("rial") || lowerLine.contains("irr")
        val candidates = numericToken.findAll(line).mapNotNull { token ->
            parseNumericToken(token.value, isRial)
        }.toList()
        if (candidates.isEmpty()) return null
        // Prefer the largest value: discounts and taxes appear next to the total and
        // the total is what the user actually paid.
        return candidates.filter { it in MIN_AMOUNT..MAX_AMOUNT }.maxOrNull()
    }

    private fun String.containsAmountKeyword(): Boolean {
        val lower = lowercase()
        return amountKeywords.any { lower.contains(it) }
    }

    private fun parseNumericToken(token: String, isRial: Boolean): Long? {
        var cleaned = token.trim()
        if (cleaned.isEmpty()) return null
        // "1.250.000" / "1,250,000" -> 1250000. A single decimal part of 1-2 digits is
        // kept (prices are never fractional in Toman, so it is dropped later anyway).
        cleaned = cleaned.replace(",", "").replace("'", "")
        if (Regex("""^\d{1,3}(\.\d{3})+$""").matches(cleaned)) {
            cleaned = cleaned.replace(".", "")
        }
        val value = cleaned.replace(".", "").toBigDecimalOrNull() ?: return null
        var toman = value
        if (isRial) toman = toman.divide(BigDecimal.TEN, 0, RoundingMode.DOWN)
        val rounded = toman.setScale(0, RoundingMode.DOWN)
        val long = runCatching { rounded.longValueExact() }.getOrNull() ?: return null
        // A bare "12" is a quantity, not money.
        if (long < MIN_AMOUNT) return null
        return long
    }

    private fun detectMerchant(lines: List<String>): String? {
        // Explicit "نام فروشگاه: X" wins.
        for (line in lines) {
            val match = Regex("""(?:نام\s+)?(?:فروشگاه|شرکت|فروشنده)\s*[:：]?\s*(.+)""").find(line)
            val value = match?.groupValues?.get(1)?.trim()
            if (!value.isNullOrBlank() && value.any { it.isLetter() }) {
                return cleanMerchant(value, stripPrefix = true)
            }
        }
        for (line in lines) {
            val letters = line.count { it.isLetter() }
            if (letters < 2) continue
            val lower = line.lowercase()
            if (merchantNoise.any { lower.contains(it) }) continue
            if (cardLikeRegex.containsMatchIn(line) || phoneLikeRegex.containsMatchIn(line)) continue
            if (line.count { it.isDigit() } > 4) continue
            val hasPrefix = merchantPrefixes.any { lower.startsWith(it) }
            if (!hasPrefix && letters < 4) continue
            return cleanMerchant(line, stripPrefix = false)
        }
        return null
    }

    private fun cleanMerchant(value: String, stripPrefix: Boolean): String {
        var text = value
            .replace(Regex("""[|:؛،,\-–—]+$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (stripPrefix) {
            for (prefix in merchantPrefixes) {
                if (text.lowercase().startsWith(prefix) && text.length > prefix.length + 3) {
                    text = text.removePrefix(prefix).trimStart(':', ' ', '-', '：')
                }
            }
        }
        return text.take(48)
    }

    private fun detectDate(lines: List<String>): JalaliDate? {
        for (line in lines) {
            val match = dateRegex.find(line) ?: continue
            val (y, m, d) = match.destructured
            val year = y.toIntOrNull() ?: continue
            val month = m.toIntOrNull() ?: continue
            val day = d.toIntOrNull() ?: continue
            if (month !in 1..12 || day !in 1..31) continue
            if (year in 1300..1500) {
                runCatching { return JalaliDate(year, month, day) }
            } else if (year in 1900..2200) {
                val gregorian = runCatching {
                    java.time.LocalDate.of(year, month, day)
                }.getOrNull() ?: continue
                return JalaliDate.fromGregorian(gregorian)
            }
        }
        return null
    }
}
