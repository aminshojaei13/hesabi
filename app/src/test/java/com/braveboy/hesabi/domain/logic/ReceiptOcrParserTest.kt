package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptOcrParserTest {

    @Test
    fun `reads a plain Persian receipt with separated digits`() {
        val text = """
            فروشگاه زنجیره‌ای افق
            تاریخ: ۱۴۰۳/۰۵/۱۲
            نان ۲ عدد ۴۵٬۰۰۰
            شیر ۱ عدد ۳۸٬۰۰۰
            جمع کل: ۸۳٬۰۰۰
        """.trimIndent()

        val result = ReceiptOcrParser.parse(text)

        assertEquals(83_000L, result.amountToman)
        assertTrue(result.amountFoundByKeyword)
        assertEquals(JalaliDate(1403, 5, 12), result.date)
        assertEquals("زنجیره‌ای افق", result.merchant)
    }

    @Test
    fun `converts a Rial total to Toman`() {
        val text = """
            سوپرمارکت آرام
            مبلغ کل ۱٬۲۵۰٬۰۰۰ ریال
        """.trimIndent()

        assertEquals(125_000L, ReceiptOcrParser.parse(text).amountToman)
    }

    @Test
    fun `keeps the total when it is the largest number on the line`() {
        val text = """
            کافه ویونا
            قهوه ۲ عدد ۱۲۰٬۰۰۰
            مالیات ۱۵٬۰۰۰
            قابل پرداخت ۱۳۵٬۰۰۰ تومان
        """.trimIndent()

        val result = ReceiptOcrParser.parse(text)

        assertEquals(135_000L, result.amountToman)
        assertEquals("کافه ویونا", result.merchant)
    }

    @Test
    fun `ignores dates phone numbers and card numbers`() {
        val text = """
            رستوران باغ
            تلفن: 09121234567
            کارت: 6037991112345678
            تاریخ 1403-05-12 ساعت 13:45
            مبلغ ۹۸۰٬۰۰۰ ریال
        """.trimIndent()

        val result = ReceiptOcrParser.parse(text)

        assertEquals(98_000L, result.amountToman)
        assertEquals(JalaliDate(1403, 5, 12), result.date)
        assertEquals("رستوران باغ", result.merchant)
    }

    @Test
    fun `handles Latin digits and thousand separators`() {
        val text = "Store Name\nTOTAL 2,450,000 RIAL"

        val result = ReceiptOcrParser.parse(text)

        assertEquals(245_000L, result.amountToman)
        assertEquals("Store Name", result.merchant)
    }

    @Test
    fun `falls back to the largest plausible number without a keyword`() {
        val text = """
            داروخانه شبانه‌روزی
            ۲ عدد ۱۵٬۰۰۰
            ۱۲٬۵۰۰
            ۱٬۲۰۰٬۰۰۰
        """.trimIndent()

        val result = ReceiptOcrParser.parse(text)

        assertEquals(1_200_000L, result.amountToman)
        assertTrue(!result.amountFoundByKeyword)
    }

    @Test
    fun `drops numbers below one thousand tomans`() {
        val text = "نانوایی\nمبلغ ۹۵۰"

        assertNull(ReceiptOcrParser.parse(text).amountToman)
    }

    @Test
    fun `reads a Gregorian date and converts it`() {
        val text = "Cafe\nDate 2024-07-02\nTotal 250000"

        val result = ReceiptOcrParser.parse(text)

        assertEquals(JalaliDate.fromGregorian(java.time.LocalDate.of(2024, 7, 2)), result.date)
    }

    @Test
    fun `empty text yields an empty result`() {
        val result = ReceiptOcrParser.parse("   ")

        assertNull(result.amountToman)
        assertNull(result.merchant)
        assertNull(result.date)
        assertTrue(!result.hasAnything)
    }

    @Test
    fun `dot separated thousands are read as one amount`() {
        val text = "فروشگاه\nجمع ۱.۲۵۰.۰۰۰"

        assertEquals(1_250_000L, ReceiptOcrParser.parse(text).amountToman)
    }

    @Test
    fun `merchant prefix is stripped when a name is present`() {
        val text = "فروشگاه: هایپر مهر\nجمع کل ۴۵۰٬۰۰۰"

        assertEquals("هایپر مهر", ReceiptOcrParser.parse(text).merchant)
    }
}
