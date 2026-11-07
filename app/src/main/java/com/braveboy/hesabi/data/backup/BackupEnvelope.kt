package com.braveboy.hesabi.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupEnvelope(
    val magic: String = MAGIC,
    val schema: Int = SCHEMA,
    val appVersion: String,
    val exportedAtMillis: Long,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
    val savingsGoals: List<SavingsGoalDto> = emptyList(),
    val people: List<PersonDto> = emptyList(),
    val debts: List<DebtDto> = emptyList(),
    val cheques: List<ChequeDto> = emptyList(),
    val loans: List<LoanDto> = emptyList(),
    val installments: List<InstallmentDto> = emptyList(),
    val recurringTransactions: List<RecurringTransactionDto> = emptyList(),
) {
    companion object {
        const val MAGIC = "hesabi-backup"
        const val SCHEMA = 5
    }
}

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val kind: String,
    val colorArgb: Long,
    val openingBalanceToman: Long,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val kind: String,
    val iconKey: String,
    val colorArgb: Long,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

@Serializable
data class TransactionDto(
    val id: String,
    val type: String,
    val amountToman: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val occurredOnEpochDay: Long,
    val note: String = "",
    val source: String = "MANUAL",
    val externalRef: String? = null,
    val receiptImagePath: String? = null,
    val createdAtMillis: Long = 0,
    val updatedAtMillis: Long = 0,
)

@Serializable
data class BudgetDto(
    val id: String,
    val categoryId: String = "",
    val periodKey: String,
    val limitToman: Long,
)

@Serializable
data class SavingsGoalDto(
    val id: String,
    val title: String,
    val targetAmountToman: Long,
    val savedAmountToman: Long,
    val colorArgb: Long,
    val iconKey: String,
    val targetDateEpochDay: Long? = null,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = 0,
)

@Serializable
data class PersonDto(
    val id: String,
    val name: String,
    val phoneNumber: String = "",
    val notes: String = "",
    val createdAtMillis: Long = 0,
)

@Serializable
data class DebtDto(
    val id: String,
    val personId: String,
    val amountToman: Long,
    val settledAmountToman: Long = 0,
    val direction: String,
    val dueEpochDay: Long? = null,
    val note: String = "",
    val isSettled: Boolean = false,
    val createdAtMillis: Long = 0,
)

@Serializable
data class ChequeDto(
    val id: String,
    val sayadNumber: String = "",
    val bankName: String,
    val amountToman: Long,
    val dueEpochDay: Long,
    val payeeOrDrawerName: String,
    val direction: String,
    val status: String,
    val note: String = "",
    val createdAtMillis: Long = 0,
)

@Serializable
data class LoanDto(
    val id: String,
    val title: String,
    val totalAmountToman: Long,
    val installmentCount: Int,
    val installmentAmountToman: Long,
    val accountId: String? = null,
    val firstDueEpochDay: Long,
    val note: String = "",
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = 0,
)

@Serializable
data class InstallmentDto(
    val id: String,
    val loanId: String,
    val installmentNumber: Int,
    val amountToman: Long,
    val dueEpochDay: Long,
    val isPaid: Boolean = false,
    val paidAtEpochDay: Long? = null,
)

@Serializable
data class RecurringTransactionDto(
    val id: String,
    val title: String,
    val type: String,
    val amountToman: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val frequency: String,
    val interval: Int = 1,
    val dayOfMonth: Int? = null,
    val nextOccurrenceEpochDay: Long,
    val lastAppliedEpochDay: Long? = null,
    val autoApply: Boolean = true,
    val isActive: Boolean = true,
    val note: String = "",
    val createdAtMillis: Long = 0,
)
