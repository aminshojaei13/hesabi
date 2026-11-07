package com.braveboy.hesabi.data.local

import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountKind
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.model.SenderRule
import com.braveboy.hesabi.domain.model.SmsEvent
import com.braveboy.hesabi.domain.model.SmsStatus
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionSource
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.data.local.entity.AccountEntity
import com.braveboy.hesabi.data.local.entity.BudgetEntity
import com.braveboy.hesabi.data.local.entity.CategoryEntity
import com.braveboy.hesabi.data.local.entity.SavingsGoalEntity
import com.braveboy.hesabi.data.local.entity.SenderRuleEntity
import com.braveboy.hesabi.data.local.entity.SmsEventEntity
import com.braveboy.hesabi.data.local.entity.TransactionEntity

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    kind = AccountKind.entries.firstOrNull { it.name == kind } ?: AccountKind.CASH,
    colorArgb = colorArgb,
    openingBalanceToman = openingBalanceToman,
    archived = archived,
    sortOrder = sortOrder,
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    kind = kind.name,
    colorArgb = colorArgb,
    openingBalanceToman = openingBalanceToman,
    archived = archived,
    sortOrder = sortOrder,
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    kind = CategoryKind.entries.firstOrNull { it.name == kind } ?: CategoryKind.EXPENSE,
    iconKey = iconKey,
    colorArgb = colorArgb,
    archived = archived,
    sortOrder = sortOrder,
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    kind = kind.name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    archived = archived,
    sortOrder = sortOrder,
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    type = TransactionType.entries.firstOrNull { it.name == type } ?: TransactionType.EXPENSE,
    amountToman = amountToman,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    occurredOnEpochDay = occurredOnEpochDay,
    note = note,
    source = TransactionSource.entries.firstOrNull { it.name == source } ?: TransactionSource.MANUAL,
    externalRef = externalRef,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    type = type.name,
    amountToman = amountToman,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    occurredOnEpochDay = occurredOnEpochDay,
    note = note,
    source = source.name,
    externalRef = externalRef,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

fun BudgetEntity.toDomain() = Budget(
    id = id,
    categoryId = categoryId,
    periodKey = periodKey,
    limitToman = limitToman,
)

fun Budget.toEntity() = BudgetEntity(
    id = id,
    categoryId = categoryId,
    periodKey = periodKey,
    limitToman = limitToman,
)

fun SmsEventEntity.toDomain() = SmsEvent(
    id = id,
    sender = sender,
    body = body,
    receivedAtMillis = receivedAtMillis,
    rawHash = rawHash,
    amountToman = amountToman,
    type = type?.let { raw -> TransactionType.entries.firstOrNull { it.name == raw } },
    last4 = last4,
    occurredOnEpochDay = occurredOnEpochDay,
    merchant = merchant,
    confidence = confidence,
    status = SmsStatus.entries.firstOrNull { it.name == status } ?: SmsStatus.PENDING,
    duplicateOfTransactionId = duplicateOfTransactionId,
)

fun SmsEvent.toEntity() = SmsEventEntity(
    id = id,
    sender = sender,
    body = body,
    receivedAtMillis = receivedAtMillis,
    rawHash = rawHash,
    amountToman = amountToman,
    type = type?.name,
    last4 = last4,
    occurredOnEpochDay = occurredOnEpochDay,
    merchant = merchant,
    confidence = confidence,
    status = status.name,
    duplicateOfTransactionId = duplicateOfTransactionId,
)

fun SenderRuleEntity.toDomain() = SenderRule(sender = sender, ignored = ignored)

fun SenderRule.toEntity() = SenderRuleEntity(sender = sender, ignored = ignored)

fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id = id,
    title = title,
    targetAmountToman = targetAmountToman,
    savedAmountToman = savedAmountToman,
    colorArgb = colorArgb,
    iconKey = iconKey,
    targetDateEpochDay = targetDateEpochDay,
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
)

fun SavingsGoal.toEntity() = SavingsGoalEntity(
    id = id,
    title = title,
    targetAmountToman = targetAmountToman,
    savedAmountToman = savedAmountToman,
    colorArgb = colorArgb,
    iconKey = iconKey,
    targetDateEpochDay = targetDateEpochDay,
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
)
