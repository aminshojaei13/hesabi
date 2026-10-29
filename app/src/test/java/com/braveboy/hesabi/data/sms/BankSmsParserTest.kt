package com.braveboy.hesabi.data.sms

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSmsParserTest {

    private val today = JalaliDate(1405, 8, 6).toEpochDay()
    private val todayAsDate = JalaliDate(1405, 8, 6)

    private fun parse(sender: String, body: String): SmsParseResult =
        BankSmsParser.parse(sender, body, receivedAtMillis = 1_760_000_000_000L, todayEpochDay = today)

    private fun accepted(sender: String, body: String): ParsedBankSms {
        val result = parse(sender, body)
        assertTrue("expected accepted, got $result", result is SmsParseResult.Accepted)
        return (result as SmsParseResult.Accepted).parsed
    }

    @Test
    fun `debit sms with persian digits and balance`() {
        val parsed = accepted(
            "9830001234",
            "برداشت ۱٬۲۵۰٬۰۰۰ ریال از حساب ۱۲۳۴۵۶۷۸۹۰ مانده ۴۵٬۶۷۸٬۹۰۰",
        )
        assertEquals(125_000L, parsed.amountToman)
        assertEquals(SmsDirection.DEBIT, parsed.direction)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("7890", parsed.last4)
        assertEquals(today, parsed.occurredOnEpochDay)
    }

    @Test
    fun `card purchase with masked card number`() {
        val parsed = accepted("BankMellat", "خرید ۴۵۰,۰۰۰ ریال کارت ...۱۲۳۴")
        assertEquals(45_000L, parsed.amountToman)
        assertEquals(SmsDirection.DEBIT, parsed.direction)
        assertEquals("1234", parsed.last4)
        assertEquals("بانک ملت", parsed.bankName)
    }

    @Test
    fun `credit sms becomes income`() {
        val parsed = accepted("SAMAN", "واریز ۵۰,۰۰۰,۰۰۰ ریال به حساب ۱۲۳۴")
        assertEquals(5_000_000L, parsed.amountToman)
        assertEquals(SmsDirection.CREDIT, parsed.direction)
        assertEquals(TransactionType.INCOME, parsed.type)
    }

    @Test
    fun `bank wording with bدهکار is a debit`() {
        val parsed = accepted("BankSaderat", "حساب شما 1234 بابت خرید 250,000 ریال بدهکار شد")
        assertEquals(25_000L, parsed.amountToman)
        assertEquals(SmsDirection.DEBIT, parsed.direction)
        assertEquals("1234", parsed.last4)
    }

    @Test
    fun `otp messages are ignored`() {
        val result = parse("9830001234", "رمز پویا: ۱۲۳۴۵۶")
        assertTrue(result is SmsParseResult.Ignored)
        assertEquals(SmsIgnoreReason.OTP, (result as SmsParseResult.Ignored).reason)
    }

    @Test
    fun `promotional messages are ignored`() {
        val result = parse("BankMellat", "جشنواره وام با سود ویژه، همین حالا ثبت‌نام کنید")
        assertTrue(result is SmsParseResult.Ignored)
        assertEquals(SmsIgnoreReason.PROMOTION, (result as SmsParseResult.Ignored).reason)
    }

    @Test
    fun `balance only messages are ignored`() {
        val result = parse("9830001234", "مانده موجودی: ۱۲,۰۰۰,۰۰۰ ریال")
        assertTrue(result is SmsParseResult.Ignored)
        assertEquals(SmsIgnoreReason.BALANCE_ONLY, (result as SmsParseResult.Ignored).reason)
    }

    @Test
    fun `transfer between accounts is a debit from the source`() {
        val parsed = accepted("BankTejarat", "انتقال 2,000,000 ریال از حساب 1234 به حساب 5678")
        assertEquals(200_000L, parsed.amountToman)
        assertEquals(SmsDirection.DEBIT, parsed.direction)
        assertEquals("1234", parsed.last4)
    }

    @Test
    fun `jalali date inside sms is used`() {
        val parsed = accepted("BankMelli", "خرید ۲۵۰,۰۰۰ ریال در ۱۴۰۵/۰۸/۰۲")
        assertEquals(25_000L, parsed.amountToman)
        assertEquals(JalaliDate(1405, 8, 2).toEpochDay(), parsed.occurredOnEpochDay)
    }

    @Test
    fun `toman amounts are not divided`() {
        val parsed = accepted("BLUE", "خرید ۴۵,۰۰۰ تومان از فروشگاه")
        assertEquals(45_000L, parsed.amountToman)
    }

    @Test
    fun `unknown text is ignored`() {
        val result = parse("09120000000", "سلام، وقت بخیر")
        assertTrue(result is SmsParseResult.Ignored)
        assertEquals(SmsIgnoreReason.NO_AMOUNT, (result as SmsParseResult.Ignored).reason)
    }

    @Test
    fun `ignored sender is skipped`() {
        val result = BankSmsParser.parse(
            sender = "9830001234",
            body = "خرید ۴۵۰,۰۰۰ ریال",
            receivedAtMillis = 0,
            todayEpochDay = today,
            ignoredSenders = setOf("9830001234"),
        )
        assertTrue(result is SmsParseResult.Ignored)
        assertEquals(SmsIgnoreReason.IGNORED_SENDER, (result as SmsParseResult.Ignored).reason)
    }

    @Test
    fun `persian month name in sms is parsed`() {
        val parsed = accepted("BankParsian", "خرید ۱۰۰,۰۰۰ ریال در ۲ آبان ۱۴۰۵")
        assertEquals(JalaliDate(1405, 8, 2).toEpochDay(), parsed.occurredOnEpochDay)
    }

    @Test
    fun `merchant is extracted when present`() {
        val parsed = accepted("BankMellat", "خرید ۴۵۰,۰۰۰ ریال از فروشگاه دیجی کالا")
        assertNotNull(parsed.merchant)
        assertTrue(parsed.merchant!!.contains("دیجی"))
    }

    @Test
    fun `ambiguous sms is flagged for review`() {
        val parsed = accepted("UnknownSender", "مبلغ ۱۲۰,۰۰۰ ریال از کارت شما کسر شد")
        assertTrue(parsed.requiresReview)
        assertTrue(parsed.confidence < 0.75f)
    }

    @Test
    fun `date-only helper matches test anchor`() {
        assertEquals(JalaliDate(1405, 8, 6), todayAsDate)
    }
}
