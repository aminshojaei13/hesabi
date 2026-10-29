package com.braveboy.hesabi.ui.preview

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountKind
import com.braveboy.hesabi.domain.model.AccountWithBalance
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.SmsEvent
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionSource
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.data.seed.SeedData

/** Deterministic sample data for previews and screenshot tests. */
object PreviewData {

    val today: JalaliDate = JalaliDate(1405, 8, 6)

    val accounts = listOf(
        Account("acc_cash", "نقدی", AccountKind.CASH, 0xFF12AFC0, 2_450_000, sortOrder = 0),
        Account("acc_card", "کارت بانکی", AccountKind.CARD, 0xFF6E7B80, 12_800_000, sortOrder = 1),
    )

    val balances = listOf(
        AccountWithBalance(accounts[0], 2_450_000),
        AccountWithBalance(accounts[1], 12_800_000),
    )

    val categories: List<Category> = SeedData.categories

    val food = categories.first { it.id == "cat_food" }
    val transport = categories.first { it.id == "cat_transport" }
    val shopping = categories.first { it.id == "cat_shopping" }
    val salary = categories.first { it.id == "cat_salary" }

    val transactions = listOf(
        Transaction(
            id = "tx1",
            type = TransactionType.EXPENSE,
            amountToman = 185_000,
            accountId = "acc_card",
            categoryId = "cat_food",
            occurredOnEpochDay = today.toEpochDay(),
            note = "ناهار با همکارها",
            source = TransactionSource.MANUAL,
            createdAtMillis = 1_760_000_000_000,
        ),
        Transaction(
            id = "tx2",
            type = TransactionType.EXPENSE,
            amountToman = 62_000,
            accountId = "acc_cash",
            categoryId = "cat_transport",
            occurredOnEpochDay = today.toEpochDay(),
            note = "تاکسی",
            createdAtMillis = 1_760_000_100_000,
        ),
        Transaction(
            id = "tx3",
            type = TransactionType.INCOME,
            amountToman = 24_000_000,
            accountId = "acc_card",
            categoryId = "cat_salary",
            occurredOnEpochDay = today.plusDays(-2).toEpochDay(),
            note = "حقوق آبان",
            createdAtMillis = 1_760_000_200_000,
        ),
        Transaction(
            id = "tx4",
            type = TransactionType.TRANSFER,
            amountToman = 1_500_000,
            accountId = "acc_card",
            toAccountId = "acc_cash",
            occurredOnEpochDay = today.plusDays(-1).toEpochDay(),
            note = "برداشت وجه",
            createdAtMillis = 1_760_000_300_000,
        ),
        Transaction(
            id = "tx5",
            type = TransactionType.EXPENSE,
            amountToman = 820_000,
            accountId = "acc_card",
            categoryId = "cat_shopping",
            occurredOnEpochDay = today.plusDays(-3).toEpochDay(),
            note = "خرید هفتگی",
            createdAtMillis = 1_760_000_400_000,
        ),
    )

    val sms = SmsEvent(
        id = "sms1",
        sender = "BankMellat",
        body = "خرید ۴۵۰,۰۰۰ ریال کارت ...۱۲۳۴",
        receivedAtMillis = 1_760_000_500_000,
        rawHash = "preview",
        amountToman = 45_000,
        type = TransactionType.EXPENSE,
        last4 = "1234",
        occurredOnEpochDay = today.toEpochDay(),
        merchant = "فروشگاه دیجی کالا",
        confidence = 0.9f,
    )

    val smsLowConfidence = sms.copy(
        id = "sms2",
        body = "مبلغ ۱۲۰,۰۰۰ ریال از کارت شما کسر شد",
        amountToman = 12_000,
        last4 = null,
        merchant = null,
        confidence = 0.55f,
    )
}
