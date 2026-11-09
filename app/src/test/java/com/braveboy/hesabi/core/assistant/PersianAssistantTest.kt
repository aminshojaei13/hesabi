package com.braveboy.hesabi.core.assistant

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersianAssistantTest {

    private val today = JalaliDate(1405, 8, 20)
    private val categories = listOf(
        category("cat_food", "خوراک", CategoryKind.EXPENSE),
        category("cat_transport", "حمل‌ونقل", CategoryKind.EXPENSE),
        category("cat_salary", "حقوق", CategoryKind.INCOME),
    )

    private fun category(id: String, name: String, kind: CategoryKind) = Category(
        id = id,
        name = name,
        kind = kind,
        iconKey = "receipt",
        colorArgb = 0xFF000000,
    )

    @Test
    fun `parses yesterday taxi expense`() {
        val result = PersianTransactionParser.parse(
            "دیروز ۲۵۰ هزار تومان تاکسی دادم",
            categories,
            today = today,
        )
        assertNotNull(result)
        assertEquals(250_000L, result!!.amountToman)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(today.plusDays(-1).toEpochDay(), result.occurredOnEpochDay)
        assertEquals("cat_transport", result.categoryId)
    }

    @Test
    fun `parses income with millions`() {
        val result = PersianTransactionParser.parse(
            "امروز ۱٫۲ میلیون تومان حقوق گرفتم",
            categories,
            today = today,
        )
        assertNotNull(result)
        assertEquals(1_200_000L, result!!.amountToman)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals("cat_salary", result.categoryId)
    }

    @Test
    fun `parses explicit day and month`() {
        val result = PersianTransactionParser.parse(
            "۵ آبان ۱۴۰۴ ناهار ۱۸۰ هزار تومان",
            categories,
            today = today,
        )
        assertNotNull(result)
        assertEquals(180_000L, result!!.amountToman)
        assertEquals(JalaliDate(1404, 8, 5).toEpochDay(), result.occurredOnEpochDay)
        assertEquals("cat_food", result.categoryId)
    }

    @Test
    fun `returns null when no amount exists`() {
        assertEquals(null, PersianTransactionParser.parse("سلام، چطوری؟", categories, today = today))
    }

    @Test
    fun `query answers month expense and balance`() {
        val context = FinancialContext(
            period = MonthPeriod(1405, 8),
            today = today,
            transactions = listOf(
                expense(500_000, today, "cat_food", "ناهار"),
                expense(300_000, today.plusDays(-2), "cat_transport", "تاکسی"),
                income(20_000_000, today, "cat_salary", "حقوق"),
            ),
            categories = categories,
            totalBalanceToman = 12_000_000,
            goals = listOf(SavingsGoal(id = "g", title = "سفر", targetAmountToman = 10_000_000)),
        )
        val month = FinancialQueryEngine.ask("این ماه چقدر خرج کردم؟", context)
        assertTrue(month.text.contains("۸۰۰٬۰۰۰"))

        val balance = FinancialQueryEngine.ask("موجودی من چقدر است؟", context)
        assertTrue(balance.text.contains("۱۲٬۰۰۰٬۰۰۰"))

        val top = FinancialQueryEngine.ask("بیشترین هزینهٔ این ماه کدام دسته بود؟", context)
        assertTrue(top.text.contains("خوراک"))
        assertEquals(1, top.transactionIds.size)
    }

    private fun expense(amount: Long, date: JalaliDate, categoryId: String, note: String) = Transaction(
        id = "t-$amount-$categoryId",
        type = TransactionType.EXPENSE,
        amountToman = amount,
        accountId = "a",
        categoryId = categoryId,
        occurredOnEpochDay = date.toEpochDay(),
        note = note,
    )

    private fun income(amount: Long, date: JalaliDate, categoryId: String, note: String) = Transaction(
        id = "i-$amount",
        type = TransactionType.INCOME,
        amountToman = amount,
        accountId = "a",
        categoryId = categoryId,
        occurredOnEpochDay = date.toEpochDay(),
        note = note,
    )
}
