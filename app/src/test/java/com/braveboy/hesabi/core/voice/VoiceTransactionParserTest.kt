package com.braveboy.hesabi.core.voice

import com.braveboy.hesabi.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class VoiceTransactionParserTest {

    @Test
    fun `basic expense with amount, category and account`() {
        val result = VoiceTransactionParser.parse("پنجاه تومن بنزین با کارت ملت")
        assertEquals(50_000L, result.amountToman)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("cat_transport", result.categoryHint)
        assertEquals("ملت", result.accountHint)
    }

    @Test
    fun `income detection`() {
        val result = VoiceTransactionParser.parse("حقوق واریز شد دو میلیون")
        assertEquals(2_000_000L, result.amountToman)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals("cat_salary", result.categoryHint)
    }

    @Test
    fun `food category detection`() {
        val result = VoiceTransactionParser.parse("بیست و پنج تومن ناهار")
        assertEquals(25_000L, result.amountToman)
        assertEquals("cat_food", result.categoryHint)
    }

    @Test
    fun `no amount still returns parsed structure`() {
        val result = VoiceTransactionParser.parse("ناهار خوردم")
        assertNull(result.amountToman)
        assertEquals("cat_food", result.categoryHint)
    }

    @Test
    fun `confidence increases with more data`() {
        val sparse = VoiceTransactionParser.parse("خرید")
        val rich = VoiceTransactionParser.parse("پنجاه تومن بنزین با کارت ملت")
        assertTrue(rich.confidence > sparse.confidence)
    }
}
