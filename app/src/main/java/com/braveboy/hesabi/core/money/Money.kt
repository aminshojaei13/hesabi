package com.braveboy.hesabi.core.money

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
        val negative = value < 0
        val raw = kotlin.math.abs(value).toString()
        val grouped = buildString {
            raw.forEachIndexed { index, ch ->
                if (index > 0 && (raw.length - index) % 3 == 0) append('٬')
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
     */
    fun parseToman(input: String): Long? {
        val normalized = Digits.toAscii(input).lowercase().trim()
        if (normalized.isEmpty()) return null
        val numberMatch = Regex("""-?\d+(?:[.,]\d+)*""").find(normalized) ?: return null
        val rawNumber = numberMatch.value.replace(",", "")
        val base = rawNumber.toDoubleOrNull() ?: return null
        val multiplier = when {
            normalized.contains("میلیون") || normalized.contains("ميليون") -> 1_000_000.0
            normalized.contains("هزار") -> 1_000.0
            else -> 1.0
        }
        val isRial = normalized.contains("ریال") || normalized.contains("ريال")
        val toman = base * multiplier / if (isRial) TOMAN_PER_RIAL.toDouble() else 1.0
        return toman.toLong().takeIf { it >= 0 }
    }

    /** Compact, human label used on cards: ۱٫۲ میلیون / ۴۵۰ هزار / ۱۲٬۵۰۰. */
    fun compact(amountToman: Long, persian: Boolean = true): String {
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
    ): String {
        val display = displayAmount(amountToman, currency)
        val body = Digits.grouped(display, persian)
        val sign = if (signed && display > 0) "+" else ""
        val unit = if (withUnit) " ${currency.labelFa}" else ""
        return "$sign$body$unit"
    }
}
