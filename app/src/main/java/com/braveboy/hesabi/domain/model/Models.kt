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
    RECURRING,
    ASSISTANT,
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
    val receiptImagePath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = createdAtMillis,
    /** Non-null means the row sits in the recycle bin until this time + 30 days. */
    val deletedAtMillis: Long? = null,
    /** `debt`, `installment` or `cheque` when this entry settles a commitment. */
    val linkedEntityType: String? = null,
    val linkedEntityId: String? = null,
) {
    val occurredOn: JalaliDate get() = JalaliDate.fromEpochDay(occurredOnEpochDay)

    /** Transfers move money, they are never income or expense. */
    val affectsIncomeExpense: Boolean get() = type != TransactionType.TRANSFER

    val isDeleted: Boolean get() = deletedAtMillis != null
}

/** Keeps the link between a cash movement and the commitment it settles. */
object LinkedEntityType {
    const val DEBT = "debt"
    const val INSTALLMENT = "installment"
    const val CHEQUE = "cheque"
}

/** A budget is defined for one Persian month; empty [categoryId] means the whole month. */
data class Budget(
    val id: String,
    val categoryId: String = "",
    val periodKey: String,
    val limitToman: Long,
    /** When true, the unused part of last month's envelope moves into this one. */
    val rolloverEnabled: Boolean = false,
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
        get() = budgetLimitToman != null && budgetSpentToman > budgetLimitToman
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
    /** 0 means "no monthly challenge target". */
    val monthlyTargetToman: Long = 0L,
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
    val receiptImagePath: String? = null,
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

data class Person(
    val id: String,
    val name: String,
    val phoneNumber: String = "",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

enum class DebtDirection(val labelFa: String) {
    LENT("طلبکارم"),
    BORROWED("بدهکارم"),
}

data class Debt(
    val id: String,
    val personId: String,
    val amountToman: Long,
    val settledAmountToman: Long = 0L,
    val direction: DebtDirection,
    val dueEpochDay: Long? = null,
    val note: String = "",
    val isSettled: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    val remainingAmountToman: Long get() = (amountToman - settledAmountToman).coerceAtLeast(0L)
}

data class PersonWithDebts(
    val person: Person,
    val debts: List<Debt>,
) {
    val netBalanceToman: Long
        get() = debts.filter { !it.isSettled }.sumOf {
            if (it.direction == DebtDirection.LENT) it.remainingAmountToman else -it.remainingAmountToman
        }
}

enum class ChequeDirection(val labelFa: String) {
    RECEIVED("دریافتی"),
    ISSUED("صادره"),
}

enum class ChequeStatus(val labelFa: String) {
    PENDING("در جریان"),
    CLEARED("وصول شده"),
    BOUNCED("برگشت خورده"),
}

data class Cheque(
    val id: String,
    val sayadNumber: String = "",
    val bankName: String,
    val amountToman: Long,
    val dueEpochDay: Long,
    val payeeOrDrawerName: String,
    val direction: ChequeDirection,
    val status: ChequeStatus = ChequeStatus.PENDING,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

data class Loan(
    val id: String,
    val title: String,
    val totalAmountToman: Long,
    val installmentCount: Int,
    val installmentAmountToman: Long,
    val accountId: String? = null,
    val firstDueEpochDay: Long,
    val note: String = "",
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

data class Installment(
    val id: String,
    val loanId: String,
    val installmentNumber: Int,
    val amountToman: Long,
    val dueEpochDay: Long,
    val isPaid: Boolean = false,
    val paidAtEpochDay: Long? = null,
)

data class LoanWithInstallments(
    val loan: Loan,
    val installments: List<Installment>,
) {
    val paidCount: Int get() = installments.count { it.isPaid }
    val remainingCount: Int get() = installments.size - paidCount
    val paidAmountToman: Long get() = installments.filter { it.isPaid }.sumOf { it.amountToman }
    val remainingAmountToman: Long get() = (loan.totalAmountToman - paidAmountToman).coerceAtLeast(0L)
    val nextPendingInstallment: Installment?
        get() = installments.filter { !it.isPaid }.minByOrNull { it.dueEpochDay }
}

enum class RecurringFrequency(val labelFa: String) {
    DAILY("روزانه"),
    WEEKLY("هفتگی"),
    MONTHLY("ماهانه"),
    YEARLY("سالانه"),
}

data class RecurringTransaction(
    val id: String,
    val title: String,
    val type: TransactionType,
    val amountToman: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val interval: Int = 1,
    val dayOfMonth: Int? = null,
    val nextOccurrenceEpochDay: Long,
    val lastAppliedEpochDay: Long? = null,
    val autoApply: Boolean = true,
    val isActive: Boolean = true,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)


/** A one-tap shortcut on the quick-entry screen ("قهوه", "تاکسی"). */
data class QuickTemplate(
    val id: String,
    val label: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountToman: Long = 0L,
    val categoryId: String? = null,
    val accountId: String? = null,
    val toAccountId: String? = null,
    val note: String = "",
    val usageCount: Int = 0,
    val sortOrder: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

enum class AssetKind(val labelFa: String) {
    GOLD("طلا و سکه"),
    CURRENCY("ارز"),
    CRYPTO("رمزارز"),
    VEHICLE("خودرو"),
    PROPERTY("ملک"),
    FUND("صندوق و سهام"),
    OTHER("سایر"),
}

/**
 * A non-cash holding. Quantity is stored multiplied by 1000 so grams of gold and
 * foreign currency can be fractional without ever using floating point money.
 */
data class Asset(
    val id: String,
    val name: String,
    val kind: AssetKind,
    val quantityMilli: Long,
    val unitLabel: String,
    val buyPriceTomanPerUnit: Long,
    val currentPriceTomanPerUnit: Long,
    val priceUpdatedAtMillis: Long = 0L,
    val priceSourceLabel: String = "ورود دستی",
    val note: String = "",
    val archived: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    val quantity: Double get() = quantityMilli / 1000.0
    val costToman: Long get() = (quantityMilli * buyPriceTomanPerUnit) / 1000
    val currentValueToman: Long get() = (quantityMilli * currentPriceTomanPerUnit) / 1000
    val profitToman: Long get() = currentValueToman - costToman
    val profitPercent: Double? get() = if (costToman > 0) profitToman * 100.0 / costToman else null
}

data class AssetPriceSnapshot(
    val id: String,
    val assetId: String,
    val priceTomanPerUnit: Long,
    val recordedAtMillis: Long,
)

/** Audit trail for savings deposits/withdrawals, used by goals and challenges. */
data class SavingsDeposit(
    val id: String,
    val goalId: String,
    val amountToman: Long,
    val isWithdrawal: Boolean = false,
    val occurredOnEpochDay: Long = JalaliDate.today().toEpochDay(),
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

/** Local, anonymous diagnostics. Never contains financial values or free text. */
data class AppEvent(
    val id: String,
    val type: String,
    val name: String,
    val metadata: String = "",
    val occurredAtMillis: Long = System.currentTimeMillis(),
)

enum class AiMode(val labelFa: String) {
    LOCAL("فقط روی گوشی"),
    CLOUD("ابر (اختیاری)"),
}
