package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionDraft
import com.braveboy.hesabi.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetAndDuplicatesTest {

    private val period = MonthPeriod(1405, 8)
    private val day = JalaliDate(1405, 8, 3).toEpochDay()

    private fun tx(
        id: String,
        amount: Long,
        type: TransactionType = TransactionType.EXPENSE,
        category: String? = "food",
        account: String = "a",
    ) = Transaction(
        id = id,
        type = type,
        amountToman = amount,
        accountId = account,
        categoryId = category,
        occurredOnEpochDay = day,
    )

    @Test
    fun `budget status computes remaining and percentage`() {
        val budget = Budget("b", "food", period.key, 500_000)
        val status = BudgetCalculator.status(budget, listOf(tx("1", 200_000), tx("2", 100_000)))
        assertEquals(300_000, status.spentToman)
        assertEquals(200_000, status.remainingToman)
        assertEquals(0.6f, status.percent, 0.001f)
        assertFalse(status.isOver)
        assertFalse(status.isNearLimit)
    }

    @Test
    fun `budget near limit and over limit`() {
        val budget = Budget("b", "", period.key, 100_000)
        val near = BudgetCalculator.status(budget, listOf(tx("1", 85_000)))
        assertTrue(near.isNearLimit)
        val over = BudgetCalculator.status(budget, listOf(tx("1", 120_000)))
        assertTrue(over.isOver)
        assertEquals(-20_000, over.remainingToman)
    }

    @Test
    fun `category budget ignores other categories and transfers`() {
        val budget = Budget("b", "food", period.key, 100_000)
        val status = BudgetCalculator.status(
            budget,
            listOf(
                tx("1", 40_000, category = "food"),
                tx("2", 60_000, category = "taxi"),
                tx("3", 90_000, type = TransactionType.TRANSFER, category = null),
            ),
        )
        assertEquals(40_000, status.spentToman)
    }

    @Test
    fun `duplicate detector matches amount account type and nearby day`() {
        val existing = listOf(tx("1", 45_000))
        val draft = TransactionDraft(
            type = TransactionType.EXPENSE,
            amountToman = 45_000,
            accountId = "a",
            categoryId = "food",
            occurredOnEpochDay = day + 1,
        )
        assertNotNull(DuplicateDetector.findDuplicate(draft, existing))
    }

    @Test
    fun `duplicate detector rejects different amount or far date`() {
        val existing = listOf(tx("1", 45_000))
        val differentAmount = TransactionDraft(amountToman = 46_000, accountId = "a", categoryId = "food", occurredOnEpochDay = day)
        assertNull(DuplicateDetector.findDuplicate(differentAmount, existing))
        val farDate = TransactionDraft(amountToman = 45_000, accountId = "a", categoryId = "food", occurredOnEpochDay = day + 5)
        assertNull(DuplicateDetector.findDuplicate(farDate, existing))
    }

    @Test
    fun `transfer duplicates compare destination account`() {
        val existing = listOf(
            Transaction(
                id = "t1",
                type = TransactionType.TRANSFER,
                amountToman = 200_000,
                accountId = "a",
                toAccountId = "b",
                occurredOnEpochDay = day,
            ),
        )
        val same = TransactionDraft(
            type = TransactionType.TRANSFER,
            amountToman = 200_000,
            accountId = "a",
            toAccountId = "b",
            occurredOnEpochDay = day,
        )
        val otherDestination = same.copy(toAccountId = "c")
        assertNotNull(DuplicateDetector.findDuplicate(same, existing))
        assertNull(DuplicateDetector.findDuplicate(otherDestination, existing))
    }

    @Test
    fun `draft validation blocks incomplete entries`() {
        assertTrue(TransactionDraft().validate().isNotEmpty())
        val valid = TransactionDraft(
            amountToman = 10_000,
            accountId = "a",
            categoryId = "food",
            occurredOnEpochDay = day,
        )
        assertTrue(valid.validate().isEmpty())
        val transferToSameAccount = TransactionDraft(
            type = TransactionType.TRANSFER,
            amountToman = 10_000,
            accountId = "a",
            toAccountId = "a",
            occurredOnEpochDay = day,
        )
        assertTrue(transferToSameAccount.validate().any { it.contains("مبدأ") })
    }
}
