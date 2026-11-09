package com.braveboy.hesabi.core.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Tiny calculator used by the amount field: `۱۲۵ + ۳۰٪`, `۲۵۰۰۰ × ۴`, `10%`.
 *
 * Only the four basic operations and a postfix percentage are supported. Results are
 * always rounded down to whole Toman, so a calculator expression can never create a
 * fraction of a Toman in the ledger.
 */
object AmountExpressionEvaluator {

    sealed interface Result {
        data class Value(val tonoman: Long) : Result
        data class Invalid(val reason: String) : Result
    }

    fun evaluate(expression: String): Result {
        val normalized = normalize(expression)
        if (normalized.isBlank()) return Result.Invalid("عبارت خالی است.")
        return runCatching {
            val parser = Parser(normalized)
            val value = parser.parseExpression()
            parser.expectEnd()
            if (value.signum() < 0) {
                Result.Invalid("مبلغ نمی‌تواند منفی باشد.")
            } else {
                Result.Value(value.setScale(0, RoundingMode.DOWN).longValueExact())
            }
        }.getOrElse { error ->
            Result.Invalid(error.message ?: "عبارت نامعتبر است.")
        }
    }

    fun isOperator(char: Char): Boolean = char in "+-*/÷×٪%"

    private fun normalize(input: String): String = Digits.toAscii(input)
        .replace("÷", "/")
        .replace("×", "*")
        .replace("٪", "%")
        .replace("٫", ".")
        .filterNot { it.isWhitespace() }

    private class Parser(private val text: String) {
        private var position = 0

        fun parseExpression(): BigDecimal {
            var value = parseTerm()
            while (position < text.length) {
                when (text[position]) {
                    '+' -> {
                        position++
                        value = value.add(parseTerm())
                    }
                    '-' -> {
                        position++
                        value = value.subtract(parseTerm())
                    }
                    else -> return value
                }
            }
            return value
        }

        private fun parseTerm(): BigDecimal {
            var value = parseFactor()
            while (position < text.length) {
                when (text[position]) {
                    '*' -> {
                        position++
                        value = value.multiply(parseFactor())
                    }
                    '/' -> {
                        position++
                        val divisor = parseFactor()
                        if (divisor.signum() == 0) error("تقسیم بر صفر ممکن نیست.")
                        value = value.divide(divisor, 6, RoundingMode.HALF_UP)
                    }
                    else -> return value
                }
            }
            return value
        }

        private fun parseFactor(): BigDecimal {
            if (position >= text.length) error("عبارت ناقص است.")
            val start = position
            if (text[position] == '+' || text[position] == '-') position++
            while (position < text.length && (text[position].isDigit() || text[position] == '.')) position++
            if (start == position) error("عدد مورد انتظار بود.")
            val number = text.substring(start, position).toBigDecimalOrNull()
                ?: error("عدد نامعتبر است.")
            var value = number
            if (position < text.length && text[position] == '%') {
                position++
                value = value.divide(BigDecimal(100), 6, RoundingMode.HALF_UP)
            }
            return value
        }

        fun expectEnd() {
            if (position != text.length) error("عملگر ناشناخته در مبلغ.")
        }
    }
}
