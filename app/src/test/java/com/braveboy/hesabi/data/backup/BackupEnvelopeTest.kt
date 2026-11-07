package com.braveboy.hesabi.data.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupEnvelopeTest {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `backup envelope v5 roundtrip preserves all room v5 entities`() {
        val original = BackupEnvelope(
            appVersion = "1.0.0",
            exportedAtMillis = 1700000000000L,
            accounts = listOf(
                AccountDto(
                    id = "acc-1",
                    name = "بانک ملی",
                    kind = "BANK",
                    colorArgb = 0xFF10B981,
                    openingBalanceToman = 10_000_000L,
                )
            ),
            categories = listOf(
                CategoryDto(
                    id = "cat-1",
                    name = "حقوق",
                    kind = "INCOME",
                    iconKey = "wallet",
                    colorArgb = 0xFF00F2FE,
                )
            ),
            transactions = listOf(
                TransactionDto(
                    id = "tx-1",
                    type = "INCOME",
                    amountToman = 25_000_000L,
                    accountId = "acc-1",
                    occurredOnEpochDay = 19700L,
                    note = "حقوق ماهانه",
                    source = "MANUAL",
                    createdAtMillis = 1700000000000L,
                    receiptImagePath = "/path/to/receipt.jpg",
                )
            ),
            budgets = listOf(
                BudgetDto(
                    id = "b-1",
                    periodKey = "1403-08",
                    limitToman = 15_000_000L,
                )
            ),
            savingsGoals = listOf(
                SavingsGoalDto(
                    id = "g-1",
                    title = "خرید لپ‌تاپ",
                    targetAmountToman = 50_000_000L,
                    savedAmountToman = 20_000_000L,
                    colorArgb = 0xFF6366F1,
                    iconKey = "laptop",
                    targetDateEpochDay = 19800L,
                    isCompleted = false,
                    createdAtMillis = 1700000000000L,
                )
            ),
            people = listOf(
                PersonDto(
                    id = "p-1",
                    name = "علی رضایی",
                    phoneNumber = "09123456789",
                    notes = "همکار",
                    createdAtMillis = 1700000000000L,
                )
            ),
            debts = listOf(
                DebtDto(
                    id = "d-1",
                    personId = "p-1",
                    amountToman = 2_000_000L,
                    settledAmountToman = 0L,
                    direction = "LENT",
                    dueEpochDay = 19750L,
                    note = "قرض برای خرید کتاب",
                    isSettled = false,
                    createdAtMillis = 1700000000000L,
                )
            ),
            cheques = listOf(
                ChequeDto(
                    id = "ch-1",
                    sayadNumber = "9876543210123456",
                    bankName = "ملت",
                    amountToman = 8_000_000L,
                    dueEpochDay = 19760L,
                    payeeOrDrawerName = "فروشگاه مرکزی",
                    direction = "ISSUED",
                    status = "PENDING",
                    note = "قسط خرید",
                    createdAtMillis = 1700000000000L,
                )
            ),
            loans = listOf(
                LoanDto(
                    id = "l-1",
                    title = "وام مسکن",
                    totalAmountToman = 100_000_000L,
                    installmentCount = 24,
                    installmentAmountToman = 5_000_000L,
                    accountId = "acc-1",
                    firstDueEpochDay = 19720L,
                    note = "مسکن جوانان",
                    isCompleted = false,
                    createdAtMillis = 1700000000000L,
                )
            ),
            installments = listOf(
                InstallmentDto(
                    id = "inst-1",
                    loanId = "l-1",
                    installmentNumber = 1,
                    amountToman = 5_000_000L,
                    dueEpochDay = 19720L,
                    isPaid = true,
                    paidAtEpochDay = 19720L,
                )
            ),
            recurringTransactions = listOf(
                RecurringTransactionDto(
                    id = "rec-1",
                    title = "اشتراک اینترنت",
                    type = "EXPENSE",
                    amountToman = 350_000L,
                    accountId = "acc-1",
                    frequency = "MONTHLY",
                    interval = 1,
                    dayOfMonth = 1,
                    nextOccurrenceEpochDay = 19730L,
                    lastAppliedEpochDay = 19700L,
                    autoApply = true,
                    isActive = true,
                    note = "فیبر نوری",
                    createdAtMillis = 1700000000000L,
                )
            ),
        )

        val serialized = json.encodeToString(original)
        assertTrue(serialized.contains("hesabi-backup"))
        assertTrue(serialized.contains("\"schema\":5"))

        val restored = json.decodeFromString<BackupEnvelope>(serialized)
        assertEquals(BackupEnvelope.MAGIC, restored.magic)
        assertEquals(5, restored.schema)
        assertEquals(1, restored.accounts.size)
        assertEquals("بانک ملی", restored.accounts[0].name)
        assertEquals(1, restored.categories.size)
        assertEquals("حقوق", restored.categories[0].name)
        assertEquals(1, restored.transactions.size)
        assertEquals("/path/to/receipt.jpg", restored.transactions[0].receiptImagePath)
        assertEquals(1, restored.savingsGoals.size)
        assertEquals("خرید لپ‌تاپ", restored.savingsGoals[0].title)
        assertEquals(1, restored.people.size)
        assertEquals("علی رضایی", restored.people[0].name)
        assertEquals(1, restored.debts.size)
        assertEquals(1, restored.cheques.size)
        assertEquals(1, restored.loans.size)
        assertEquals(1, restored.installments.size)
        assertEquals(1, restored.recurringTransactions.size)
        assertEquals("اشتراک اینترنت", restored.recurringTransactions[0].title)
    }

    @Test
    fun `backward compatibility - decoding v1 envelope with missing new tables defaults to empty lists`() {
        val legacyJson = """
            {
                "magic": "hesabi-backup",
                "schema": 1,
                "appVersion": "0.9.0",
                "exportedAtMillis": 1690000000000,
                "accounts": [
                    {
                        "id": "acc-old",
                        "name": "کیف پول",
                        "kind": "CASH",
                        "colorArgb": 4278190080,
                        "openingBalanceToman": 500000
                    }
                ],
                "categories": [],
                "transactions": [],
                "budgets": []
            }
        """.trimIndent()

        val envelope = json.decodeFromString<BackupEnvelope>(legacyJson)
        assertEquals(1, envelope.schema)
        assertEquals(1, envelope.accounts.size)
        assertNotNull(envelope.savingsGoals)
        assertTrue(envelope.savingsGoals.isEmpty())
        assertNotNull(envelope.debts)
        assertTrue(envelope.debts.isEmpty())
        assertNotNull(envelope.cheques)
        assertTrue(envelope.cheques.isEmpty())
        assertNotNull(envelope.recurringTransactions)
        assertTrue(envelope.recurringTransactions.isEmpty())
    }
}
