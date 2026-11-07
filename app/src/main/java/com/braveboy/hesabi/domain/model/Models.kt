package com.braveboy.hesabi.domain.model

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod

enum class AccountKind(val label: String) {
    CASH("نقدی"),
    CARD("کارت بانکی"),
    BANK("حساب بانکی"),
    WALLET("کیف پول"),
}

data class Account(
    val id: String,
    val name: String,
    val kind: AccountKind,
    val colorArgb: Long,
    val openingBalanceToman: Long,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

enum class CategoryKind(val label: String) {
    EXPENSE("هزینه"),
    INCOME("درآمد"),
}

data class Category(
    val id: String,
    val name: String,
    val kind: CategoryKind,
    val iconKey: String,
    val colorArgb: Long,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

enum class TransactionType(val label: String) {
    EXPENSE("هزینه"),
    INCOME("درآمد"),
    TRANSFER("انتقال"),
}

enum class TransactionSource {
    MANUAL,
    SMS,
    IMPORT,
    QUICK_REPEAT,
    VOICE,
}

data class Transaction(
    val id: String,
    val type: TransactionType,
    /** Positive amount in Toman. Direction is carried by [type]. */
    val amountToman: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val occurredOnEpochDay: Long,
    val note: String = "",
    val source: TransactionSource = TransactionSource.MANUAL,
    val externalRef: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = createdAtMillis,
) {
    val occurredOn: JalaliDate get() = JalaliDate.fromEpochDay(occurredOnEpochDay)

    /** Transfers move money, they are never income or expense. */
    val affectsIncomeExpense: Boolean get() = type != TransactionType.TRANSFER
}

/** A budget is defined for one Persian month; empty [categoryId] means the whole month. */
data class Budget(
    val id: String,
    val categoryId: String = "",
    val periodKey: String,
    val limitToman: Long,
) {
    fun periodOrNull(): MonthPeriod? = MonthPeriod.fromKey(periodKey)
}

data class CategorySlice(
    val categoryId: String,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
    val amountToman: Long,
    val share: Float,
)

data class DailyPoint(
    val epochDay: Long,
    val expenseToman: Long,
    val incomeToman: Long,
)

data class MonthReport(
    val period: MonthPeriod,
    val totalExpenseToman: Long,
    val totalIncomeToman: Long,
    val netToman: Long,
    val transactionCount: Int,
    val previousExpenseToman: Long,
    /** Negative means spending went down. Null when there is no previous month data. */
    val expenseDeltaPercent: Int?,
    val slices: List<CategorySlice>,
    val daily: List<DailyPoint>,
    val budgetLimitToman: Long? = null,
    val budgetSpentToman: Long = 0,
) {
    val budgetRemainingToman: Long?
        get() = budgetLimitToman?.minus(budgetSpentToman)

    val budgetPercent: Float?
        get() = budgetLimitToman?.takeIf { it > 0 }?.let { (budgetSpentToman.toFloat() / it).coerceAtLeast(0f) }

    val isOverBudget: Boolean
        get() = budgetLimitToman != null && budgetSpentToman > budgetLimitToman!!
}

data class SavingsGoal(
    val id: String,
    val title: String,
    val targetAmountToman: Long,
    val savedAmountToman: Long = 0L,
    val colorArgb: Long = 0xFF10B981,
    val iconKey: String = "savings",
    val targetDateEpochDay: Long? = null,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    val progress: Float
        get() = if (targetAmountToman > 0) (savedAmountToman.toFloat() / targetAmountToman).coerceIn(0f, 1f) else 0f

    val remainingToman: Long
        get() = (targetAmountToman - savedAmountToman).coerceAtLeast(0L)
}

/** A single parsed bank SMS waiting for the user's approval. */
data class SmsEvent(
    val id: String,
    val sender: String,
    val body: String,
    val receivedAtMillis: Long,
    val rawHash: String,
    val amountToman: Long?,
    val type: TransactionType?,
    val last4: String?,
    val occurredOnEpochDay: Long,
    val merchant: String? = null,
    val confidence: Float = 0f,
    val status: SmsStatus = SmsStatus.PENDING,
    val duplicateOfTransactionId: String? = null,
)

enum class SmsStatus {
    PENDING,
    IMPORTED,
    IGNORED,
}

data class SenderRule(
    val sender: String,
    val ignored: Boolean = true,
)

/** Snapshot used by the entry form, independent from the persistence layer. */
data class TransactionDraft(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountToman: Long = 0,
    val accountId: String = "",
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val occurredOnEpochDay: Long = JalaliDate.today().toEpochDay(),
    val note: String = "",
    val source: TransactionSource = TransactionSource.MANUAL,
) {
    fun validate(): List<String> = buildList {
        if (amountToman <= 0) add("مبلغ باید بیشتر از صفر باشد.")
        if (accountId.isBlank()) add("یک حساب انتخاب کنید.")
        if (type == TransactionType.TRANSFER) {
            if (toAccountId.isNullOrBlank()) add("حساب مقصد را انتخاب کنید.")
            if (toAccountId == accountId) add("حساب مبدأ و مقصد نباید یکی باشند.")
        } else if (categoryId.isNullOrBlank()) {
            add("یک دسته انتخاب کنید.")
        }
    }
}
