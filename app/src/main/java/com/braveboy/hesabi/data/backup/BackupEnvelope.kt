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
) {
    companion object {
        const val MAGIC = "hesabi-backup"
        const val SCHEMA = 1
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
