package com.braveboy.hesabi.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `rial is converted to toman`() {
        assertEquals(125_000L, Money.rialToToman(1_250_000L))
        assertEquals(1_250_000L, Money.tomanToRial(125_000L))
    }

    @Test
    fun `formats persian grouped amount`() {
        assertEquals("۱٬۲۵۰٬۰۰۰ تومان", Money.format(1_250_000L))
        assertEquals("۱۲٬۵۰۰٬۰۰۰ ریال", Money.format(1_250_000L, Currency.RIAL))
        assertEquals("-۲٬۵۰۰ تومان", Money.format(-2_500L))
    }

    @Test
    fun `parses persian digits and rial suffix`() {
        assertEquals(125_000L, Money.parseToman("۱٬۲۵۰٬۰۰۰ ریال"))
        assertEquals(45_000L, Money.parseToman("45,000 تومان"))
        assertEquals(45_000L, Money.parseToman("۴۵ هزار"))
        assertEquals(2_500_000L, Money.parseToman("۲.۵ میلیون"))
    }

    @Test
    fun `rejects unparseable input`() {
        assertNull(Money.parseToman(""))
        assertNull(Money.parseToman("سلام"))
    }

    @Test
    fun `compact labels are readable`() {
        assertEquals("۱٫۲ میلیون", Money.compact(1_250_000L))
        assertEquals("۴۵۰ هزار", Money.compact(450_000L))
        assertEquals("۱۲٬۵۰۰", Money.compact(12_500L))
    }

    @Test
    fun `digits normalize both persian and arabic numerals`() {
        assertEquals("1405/08/02", Digits.toAscii("۱۴۰۵/۰۸/۰۲"))
        assertEquals("1405", Digits.toAscii("١٤٠٥"))
        assertEquals("۱۴۰۵", Digits.toPersian("1405"))
    }
}
