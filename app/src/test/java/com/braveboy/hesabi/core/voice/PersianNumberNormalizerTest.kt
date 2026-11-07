package com.braveboy.hesabi.core.voice

import org.junit.Assert.*
import org.junit.Test

class PersianNumberNormalizerTest {

    @Test
    fun `plain digits return correct toman`() {
        assertEquals(50_000L, PersianNumberNormalizer.parseToman("50000"))
        assertEquals(50_000L, PersianNumberNormalizer.parseToman("۵۰۰۰۰"))
    }

    @Test
    fun `spoken number with toman suffix`() {
        assertEquals(50_000L, PersianNumberNormalizer.parseToman("پنجاه هزار تومان"))
        assertEquals(50_000L, PersianNumberNormalizer.parseToman("پنجاه تومن"))
    }

    @Test
    fun `colloquial shorthand with toman`() {
        assertEquals(3_000L, PersianNumberNormalizer.parseToman("سه تومن"))
        assertEquals(85_000L, PersianNumberNormalizer.parseToman("هشتاد و پنج تومن"))
    }

    @Test
    fun `compound numbers`() {
        assertEquals(350_000L, PersianNumberNormalizer.parseToman("سیصد و پنجاه هزار تومان"))
        assertEquals(25_000L, PersianNumberNormalizer.parseToman("بیست و پنج هزار"))
    }

    @Test
    fun `millions`() {
        assertEquals(2_000_000L, PersianNumberNormalizer.parseToman("دو میلیون"))
        assertEquals(2_500_000L, PersianNumberNormalizer.parseToman("دو میلیون و نیم"))
        assertEquals(1_200_000L, PersianNumberNormalizer.parseToman("یک میلیون و دویست هزار"))
    }

    @Test
    fun `rial conversion`() {
        assertEquals(35_000L, PersianNumberNormalizer.parseToman("سیصد و پنجاه هزار ریال"))
    }

    @Test
    fun `mixed digits and words`() {
        assertEquals(50_000L, PersianNumberNormalizer.parseToman("۵۰ هزار تومان"))
    }

    @Test
    fun `null for unparseable`() {
        assertNull(PersianNumberNormalizer.parseToman("سلام"))
        assertNull(PersianNumberNormalizer.parseToman(""))
    }
}
