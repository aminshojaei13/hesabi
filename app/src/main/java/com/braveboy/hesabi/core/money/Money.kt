package com.braveboy.hesabi.core.money

import java.math.BigDecimal

/**
 * Persian/Arabic digit normalization and grouping used across the whole app.
 *
 * Money is always stored as [Long] Toman inside the domain. Formatting to Rial is a
 * presentation concern; no floating point value ever touches a monetary amount.
 */
object Digits {

    private val persianZero = '۰'.code
    private val arabicZero = '٠'.code

    fun toAscii(input: String): String {
        val out = StringBuilder(input.length)
        for (ch in input) {
            out.append(
                when {
                    ch in '۰'..'۹' -> ('0' + (ch.code - persianZero))
                    ch in '٠'..'٩' -> ('0' + (ch.code - arabicZero))
                    ch == '٬' || ch == '،' -> ','
                    ch == '٫' -> '.'
                    ch == 'ك' -> 'ک'
                    ch == 'ي' -> 'ی'
                    ch == 'ٔ' -> 'ء'
                    else -> ch
                },
            )
        }
        return out.toString()
    }

    fun toPersian(input: String): String {
        val out = StringBuilder(input.length)
        for (ch in input) {
            out.append(if (ch in '0'..'9') ('۰' + (ch - '0')) else ch)
        }
        return out.toString()
    }

    /** Groups 1250000 as "۱٬۲۵۰٬۰۰۰" when Persian is requested. */
    fun grouped(value: Long, persian: Boolean = true): String {
        // Never use abs(): abs(Long.MIN_VALUE) overflows and prints a wrong number.
        val raw = value.toString()
        val negative = raw.startsWith("-")
        val digits = if (negative) raw.substring(1) else raw
        val grouped = buildString {
            digits.forEachIndexed { index, ch ->
                if (index > 0 && (digits.length - index) % 3 == 0) append('٬')
                append(ch)
            }
        }
        val signed = if (negative) "-$grouped" else grouped
        return if (persian) toPersian(signed) else signed
    }
}

/** ISO-4217-like currency choice. Domain amounts are Toman; Rial is 10x. */
enum class Currency(val code: String, val labelFa: String) {
    TOMAN("IRT", "تومان"),
    RIAL("IRR", "ریال"),
    ;

    fun fromStorage(raw: String?): Currency = entries.firstOrNull { it.name == raw } ?: TOMAN
}

/**
 * One switch for the whole UI. The theme keeps this in sync with the "hide amounts"
 * setting so every screen, chart label and helper string masks money consistently (H8).
 * Export builders pass `respectPrivacy = false` because a report must contain numbers.
 */
object AmountVisibility {
    @Volatile
    var hideAmounts: Boolean = false
}

object Money {

    const val TOMAN_PER_RIAL = 10L

    /** Converts a Rial amount as printed by banks into the app's Toman ledger. */
    fun rialToToman(rial: Long): Long = rial / TOMAN_PER_RIAL

    fun tomanToRial(toman: Long): Long = toman * TOMAN_PER_RIAL

    fun displayAmount(amountToman: Long, currency: Currency): Long =
        if (currency == Currency.RIAL) tomanToRial(amountToman) else amountToman

    /**
     * Parses free-form user input such as "۱٬۲۵۰٬۰۰۰", "1,250,000 ریال" or "45 هزار".
     * Returns Toman. Returns null for unparseable input.
     *
     * Uses [java.math.BigDecimal] end-to-end: [Double] would silently lose tomans for
     * large amounts (M24). Rial inputs are divided by ten and rounded down, because a
     * bank SMS never carries a fraction of a Toman.
     */
    fun parseToman(input: String, allowNegative: Boolean = false): Long? {
        val normalized = Digits.toAscii(input)
            .lowercase()
            .replace("٬", "")
            .replace(",", "")
            .replace("٫", ".")
            .trim()
        if (normalized.isEmpty()) return null
        val numberMatch = Regex("""[-+]?\d+(?:\.\d+)?""").find(normalized) ?: return null
        val base = numberMatch.value.toBigDecimalOrNull() ?: return null
        val multiplier = when {
            normalized.contains("میلیارد") || normalized.contains("ميليارد") -> BigDecimal("1000000000")
            normalized.contains("میلیون") || normalized.contains("ميليون") -> BigDecimal("1000000")
            normalized.contains("هزار") -> BigDecimal("1000")
            else -> BigDecimal.ONE
        }
        val isRial = normalized.contains("ریال") || normalized.contains("ريال")
        var toman = base.multiply(multiplier)
        if (isRial) toman = toman.divide(BigDecimal.TEN, 0, java.math.RoundingMode.DOWN)
        val rounded = toman.setScale(0, java.math.RoundingMode.DOWN)
        if (!allowNegative && rounded.signum() < 0) return null
        return runCatching { rounded.longValueExact() }.getOrNull()
    }

    /** Compact, human label used on cards: ۱٫۲ میلیون / ۴۵۰ هزار / ۱۲٬۵۰۰. */
    fun compact(amountToman: Long, persian: Boolean = true, respectPrivacy: Boolean = true): String {
        if (respectPrivacy && AmountVisibility.hideAmounts) return "••••••"
        val abs = kotlin.math.abs(amountToman)
        val text = when {
            abs >= 1_000_000_000 -> oneDecimal(amountToman / 1_000_000_000.0) + " میلیارد"
            abs >= 1_000_000 -> oneDecimal(amountToman / 1_000_000.0) + " میلیون"
            abs >= 100_000 -> Digits.grouped(amountToman / 1_000) + " هزار"
            else -> Digits.grouped(amountToman)
        }
        return if (persian) Digits.toPersian(text) else text
    }

    private fun oneDecimal(value: Double): String {
        val scaled = (value * 10).toLong() // truncate: 1.25 -> 1.2, never overstate money
        val whole = scaled / 10
        val fraction = scaled % 10
        return if (fraction == 0L) whole.toString() else "$whole٫$fraction"
    }

    fun format(
        amountToman: Long,
        currency: Currency = Currency.TOMAN,
        withUnit: Boolean = true,
        persian: Boolean = true,
        signed: Boolean = false,
        respectPrivacy: Boolean = true,
    ): String {
        if (respectPrivacy && AmountVisibility.hideAmounts) return "••••••"
        val display = displayAmount(amountToman, currency)
        val body = Digits.grouped(display, persian)
        val sign = if (signed && display > 0) "+" else ""
        val unit = if (withUnit) " ${currency.labelFa}" else ""
        return "$sign$body$unit"
    }
}
