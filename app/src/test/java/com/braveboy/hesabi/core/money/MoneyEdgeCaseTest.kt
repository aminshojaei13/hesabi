package com.braveboy.hesabi.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyEdgeCaseTest {

    @Test
    fun `grouped never overflows on Long MIN_VALUE`() {
        val formatted = Digits.grouped(Long.MIN_VALUE, persian = false)
        assertEquals("-9,223,372,036,854,775,808", formatted.replace('٬', ','))
    }

    @Test
    fun `parse accepts persian separators and thousand words without double precision loss`() {
        assertEquals(1_250_000L, Money.parseToman("۱٬۲۵۰٬۰۰۰ تومان"))
        assertEquals(45_000L, Money.parseToman("45 هزار"))
        assertEquals(2_500_000L, Money.parseToman("۲٫۵ میلیون"))
        assertEquals(1_200_000L, Money.parseToman("12,000,000 ریال"))
        assertEquals(123_456_789_012L, Money.parseToman("123456789012"))
    }

    @Test
    fun `parse rejects negative by default but can allow it`() {
        assertNull(Money.parseToman("-۵۰۰۰"))
        assertEquals(-5_000L, Money.parseToman("-۵۰۰۰", allowNegative = true))
    }

    @Test
    fun `parse handles very large values exactly`() {
        assertEquals(9_000_000_000_000_000L, Money.parseToman("9000000000000000"))
    }

    @Test
    fun `calculator respects precedence and percent`() {
        assertEquals(1_600L, value("1000+300*2"))
        assertEquals(1_250L, value("1250"))
        assertEquals(375L, value("1500*25%"))
        assertEquals(900L, value("1000-100"))
    }

    @Test
    fun `calculator rejects invalid or negative results`() {
        assertTrueInvalid("10/0")
        assertTrueInvalid("")
        assertTrueInvalid("100-200")
    }

    private fun value(expression: String): Long =
        (AmountExpressionEvaluator.evaluate(expression) as AmountExpressionEvaluator.Result.Value).tonoman

    private fun assertTrueInvalid(expression: String) {
        val result = AmountExpressionEvaluator.evaluate(expression)
        assert(result is AmountExpressionEvaluator.Result.Invalid) { "expected invalid: $expression" }
    }
}
