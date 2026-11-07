package com.braveboy.hesabi.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [Index("sortOrder")],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val colorArgb: Long,
    val openingBalanceToman: Long,
    val archived: Boolean,
    val sortOrder: Int,
)

@Entity(
    tableName = "categories",
    indices = [Index("kind"), Index("sortOrder")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val iconKey: String,
    val colorArgb: Long,
    val archived: Boolean,
    val sortOrder: Int,
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("occurredOnEpochDay"),
        Index(value = ["accountId", "occurredOnEpochDay"]),
        Index("categoryId"),
        Index(value = ["externalRef"], unique = true),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountToman: Long,
    val accountId: String,
    val toAccountId: String?,
    val categoryId: String?,
    val occurredOnEpochDay: Long,
    val note: String,
    val source: String,
    val externalRef: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "periodKey"], unique = true)],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val periodKey: String,
    val limitToman: Long,
)

@Entity(
    tableName = "sms_events",
    indices = [Index(value = ["rawHash"], unique = true), Index("status")],
)
data class SmsEventEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val body: String,
    val receivedAtMillis: Long,
    val rawHash: String,
    val amountToman: Long?,
    val type: String?,
    val last4: String?,
    val occurredOnEpochDay: Long,
    val merchant: String?,
    val confidence: Float,
    val status: String,
    val duplicateOfTransactionId: String?,
)

@Entity(tableName = "sender_rules")
data class SenderRuleEntity(
    @PrimaryKey val sender: String,
    val ignored: Boolean,
)

@Entity(
    tableName = "savings_goals",
    indices = [Index("isCompleted"), Index("createdAtMillis")],
)
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val targetAmountToman: Long,
    val savedAmountToman: Long,
    val colorArgb: Long,
    val iconKey: String,
    val targetDateEpochDay: Long?,
    val isCompleted: Boolean,
    val createdAtMillis: Long,
)
