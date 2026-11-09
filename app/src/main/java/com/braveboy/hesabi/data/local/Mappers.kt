package com.braveboy.hesabi.data.local

import com.braveboy.hesabi.core.security.FieldCrypto
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountKind
import com.braveboy.hesabi.domain.model.AppEvent
import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.AssetKind
import com.braveboy.hesabi.domain.model.AssetPriceSnapshot
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.Cheque
import com.braveboy.hesabi.domain.model.ChequeDirection
import com.braveboy.hesabi.domain.model.ChequeStatus
import com.braveboy.hesabi.domain.model.Debt
import com.braveboy.hesabi.domain.model.DebtDirection
import com.braveboy.hesabi.domain.model.Installment
import com.braveboy.hesabi.domain.model.Loan
import com.braveboy.hesabi.domain.model.Person
import com.braveboy.hesabi.domain.model.QuickTemplate
import com.braveboy.hesabi.domain.model.RecurringFrequency
import com.braveboy.hesabi.domain.model.RecurringTransaction
import com.braveboy.hesabi.domain.model.SavingsDeposit
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.model.SenderRule
import com.braveboy.hesabi.domain.model.SmsEvent
import com.braveboy.hesabi.domain.model.SmsStatus
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionSource
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.data.local.entity.AccountEntity
import com.braveboy.hesabi.data.local.entity.AppEventEntity
import com.braveboy.hesabi.data.local.entity.AssetEntity
import com.braveboy.hesabi.data.local.entity.AssetPriceSnapshotEntity
import com.braveboy.hesabi.data.local.entity.BudgetEntity
import com.braveboy.hesabi.data.local.entity.CategoryEntity
import com.braveboy.hesabi.data.local.entity.ChequeEntity
import com.braveboy.hesabi.data.local.entity.DebtEntity
import com.braveboy.hesabi.data.local.entity.InstallmentEntity
import com.braveboy.hesabi.data.local.entity.LoanEntity
import com.braveboy.hesabi.data.local.entity.PersonEntity
import com.braveboy.hesabi.data.local.entity.QuickTemplateEntity
import com.braveboy.hesabi.data.local.entity.RecurringTransactionEntity
import com.braveboy.hesabi.data.local.entity.SavingsDepositEntity
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
    note = FieldCrypto.open(note),
    source = TransactionSource.entries.firstOrNull { it.name == source } ?: TransactionSource.MANUAL,
    externalRef = externalRef,
    receiptImagePath = receiptImagePath,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    deletedAtMillis = deletedAtMillis,
    linkedEntityType = linkedEntityType,
    linkedEntityId = linkedEntityId,
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    type = type.name,
    amountToman = amountToman,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    occurredOnEpochDay = occurredOnEpochDay,
    note = FieldCrypto.seal(note),
    source = source.name,
    externalRef = externalRef,
    receiptImagePath = receiptImagePath,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    deletedAtMillis = deletedAtMillis,
    linkedEntityType = linkedEntityType,
    linkedEntityId = linkedEntityId,
)

fun BudgetEntity.toDomain() = Budget(
    id = id,
    categoryId = categoryId,
    periodKey = periodKey,
    limitToman = limitToman,
    rolloverEnabled = rolloverEnabled,
)

fun Budget.toEntity() = BudgetEntity(
    id = id,
    categoryId = categoryId,
    periodKey = periodKey,
    limitToman = limitToman,
    rolloverEnabled = rolloverEnabled,
)

fun SmsEventEntity.toDomain() = SmsEvent(
    id = id,
    sender = sender,
    body = FieldCrypto.open(body),
    receivedAtMillis = receivedAtMillis,
    rawHash = rawHash,
    amountToman = amountToman,
    type = type?.let { raw -> TransactionType.entries.firstOrNull { it.name == raw } },
    last4 = last4,
    occurredOnEpochDay = occurredOnEpochDay,
    merchant = merchant?.let { FieldCrypto.open(it) },
    confidence = confidence,
    status = SmsStatus.entries.firstOrNull { it.name == status } ?: SmsStatus.PENDING,
    duplicateOfTransactionId = duplicateOfTransactionId,
)

fun SmsEvent.toEntity() = SmsEventEntity(
    id = id,
    sender = sender,
    body = FieldCrypto.seal(body),
    receivedAtMillis = receivedAtMillis,
    rawHash = rawHash,
    amountToman = amountToman,
    type = type?.name,
    last4 = last4,
    occurredOnEpochDay = occurredOnEpochDay,
    merchant = merchant?.let { FieldCrypto.seal(it) },
    confidence = confidence,
    status = status.name,
    duplicateOfTransactionId = duplicateOfTransactionId,
)

fun SenderRuleEntity.toDomain() = SenderRule(sender = sender, ignored = ignored)

fun SenderRule.toEntity() = SenderRuleEntity(sender = sender, ignored = ignored)

fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id = id,
    title = FieldCrypto.open(title),
    targetAmountToman = targetAmountToman,
    savedAmountToman = savedAmountToman,
    colorArgb = colorArgb,
    iconKey = iconKey,
    targetDateEpochDay = targetDateEpochDay,
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
    monthlyTargetToman = monthlyTargetToman,
)

fun SavingsGoal.toEntity() = SavingsGoalEntity(
    id = id,
    title = FieldCrypto.seal(title),
    targetAmountToman = targetAmountToman,
    savedAmountToman = savedAmountToman,
    colorArgb = colorArgb,
    iconKey = iconKey,
    targetDateEpochDay = targetDateEpochDay,
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
    monthlyTargetToman = monthlyTargetToman,
)

fun PersonEntity.toDomain() = Person(
    id = id,
    name = FieldCrypto.open(name),
    phoneNumber = FieldCrypto.open(phoneNumber),
    notes = FieldCrypto.open(notes),
    createdAtMillis = createdAtMillis,
)

fun Person.toEntity() = PersonEntity(
    id = id,
    name = FieldCrypto.seal(name),
    phoneNumber = FieldCrypto.seal(phoneNumber),
    notes = FieldCrypto.seal(notes),
    createdAtMillis = createdAtMillis,
)

fun DebtEntity.toDomain() = Debt(
    id = id,
    personId = personId,
    amountToman = amountToman,
    settledAmountToman = settledAmountToman,
    direction = DebtDirection.entries.firstOrNull { it.name == direction } ?: DebtDirection.BORROWED,
    dueEpochDay = dueEpochDay,
    note = FieldCrypto.open(note),
    isSettled = isSettled,
    createdAtMillis = createdAtMillis,
)

fun Debt.toEntity() = DebtEntity(
    id = id,
    personId = personId,
    amountToman = amountToman,
    settledAmountToman = settledAmountToman,
    direction = direction.name,
    dueEpochDay = dueEpochDay,
    note = FieldCrypto.seal(note),
    isSettled = isSettled,
    createdAtMillis = createdAtMillis,
)

fun ChequeEntity.toDomain() = Cheque(
    id = id,
    sayadNumber = sayadNumber,
    bankName = bankName,
    amountToman = amountToman,
    dueEpochDay = dueEpochDay,
    payeeOrDrawerName = FieldCrypto.open(payeeOrDrawerName),
    direction = ChequeDirection.entries.firstOrNull { it.name == direction } ?: ChequeDirection.ISSUED,
    status = ChequeStatus.entries.firstOrNull { it.name == status } ?: ChequeStatus.PENDING,
    note = FieldCrypto.open(note),
    createdAtMillis = createdAtMillis,
)

fun Cheque.toEntity() = ChequeEntity(
    id = id,
    sayadNumber = sayadNumber,
    bankName = bankName,
    amountToman = amountToman,
    dueEpochDay = dueEpochDay,
    payeeOrDrawerName = FieldCrypto.seal(payeeOrDrawerName),
    direction = direction.name,
    status = status.name,
    note = FieldCrypto.seal(note),
    createdAtMillis = createdAtMillis,
)

fun LoanEntity.toDomain() = Loan(
    id = id,
    title = title,
    totalAmountToman = totalAmountToman,
    installmentCount = installmentCount,
    installmentAmountToman = installmentAmountToman,
    accountId = accountId,
    firstDueEpochDay = firstDueEpochDay,
    note = note,
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
)

fun Loan.toEntity() = LoanEntity(
    id = id,
    title = title,
    totalAmountToman = totalAmountToman,
    installmentCount = installmentCount,
    installmentAmountToman = installmentAmountToman,
    accountId = accountId,
    firstDueEpochDay = firstDueEpochDay,
    note = note,
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
)

fun InstallmentEntity.toDomain() = Installment(
    id = id,
    loanId = loanId,
    installmentNumber = installmentNumber,
    amountToman = amountToman,
    dueEpochDay = dueEpochDay,
    isPaid = isPaid,
    paidAtEpochDay = paidAtEpochDay,
)

fun Installment.toEntity() = InstallmentEntity(
    id = id,
    loanId = loanId,
    installmentNumber = installmentNumber,
    amountToman = amountToman,
    dueEpochDay = dueEpochDay,
    isPaid = isPaid,
    paidAtEpochDay = paidAtEpochDay,
)

fun RecurringTransactionEntity.toDomain() = RecurringTransaction(
    id = id,
    title = FieldCrypto.open(title),
    type = TransactionType.entries.firstOrNull { it.name == type } ?: TransactionType.EXPENSE,
    amountToman = amountToman,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    frequency = RecurringFrequency.entries.firstOrNull { it.name == frequency } ?: RecurringFrequency.MONTHLY,
    interval = interval,
    dayOfMonth = dayOfMonth,
    nextOccurrenceEpochDay = nextOccurrenceEpochDay,
    lastAppliedEpochDay = lastAppliedEpochDay,
    autoApply = autoApply,
    isActive = isActive,
    note = FieldCrypto.open(note),
    createdAtMillis = createdAtMillis,
)

fun RecurringTransaction.toEntity() = RecurringTransactionEntity(
    id = id,
    title = FieldCrypto.seal(title),
    type = type.name,
    amountToman = amountToman,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    frequency = frequency.name,
    interval = interval,
    dayOfMonth = dayOfMonth,
    nextOccurrenceEpochDay = nextOccurrenceEpochDay,
    lastAppliedEpochDay = lastAppliedEpochDay,
    autoApply = autoApply,
    isActive = isActive,
    note = FieldCrypto.seal(note),
    createdAtMillis = createdAtMillis,
)


fun QuickTemplateEntity.toDomain() = QuickTemplate(
    id = id,
    label = FieldCrypto.open(label),
    type = TransactionType.entries.firstOrNull { it.name == type } ?: TransactionType.EXPENSE,
    amountToman = amountToman,
    categoryId = categoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    note = FieldCrypto.open(note),
    usageCount = usageCount,
    sortOrder = sortOrder,
    createdAtMillis = createdAtMillis,
)

fun QuickTemplate.toEntity() = QuickTemplateEntity(
    id = id,
    label = FieldCrypto.seal(label),
    type = type.name,
    amountToman = amountToman,
    categoryId = categoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    note = FieldCrypto.seal(note),
    usageCount = usageCount,
    sortOrder = sortOrder,
    createdAtMillis = createdAtMillis,
)

fun AssetEntity.toDomain() = Asset(
    id = id,
    name = FieldCrypto.open(name),
    kind = AssetKind.entries.firstOrNull { it.name == kind } ?: AssetKind.OTHER,
    quantityMilli = quantityMilli,
    unitLabel = unitLabel,
    buyPriceTomanPerUnit = buyPriceTomanPerUnit,
    currentPriceTomanPerUnit = currentPriceTomanPerUnit,
    priceUpdatedAtMillis = priceUpdatedAtMillis,
    priceSourceLabel = priceSourceLabel,
    note = FieldCrypto.open(note),
    archived = archived,
    createdAtMillis = createdAtMillis,
)

fun Asset.toEntity() = AssetEntity(
    id = id,
    name = FieldCrypto.seal(name),
    kind = kind.name,
    quantityMilli = quantityMilli,
    unitLabel = unitLabel,
    buyPriceTomanPerUnit = buyPriceTomanPerUnit,
    currentPriceTomanPerUnit = currentPriceTomanPerUnit,
    priceUpdatedAtMillis = priceUpdatedAtMillis,
    priceSourceLabel = priceSourceLabel,
    note = FieldCrypto.seal(note),
    archived = archived,
    createdAtMillis = createdAtMillis,
)

fun AssetPriceSnapshotEntity.toDomain() = AssetPriceSnapshot(
    id = id,
    assetId = assetId,
    priceTomanPerUnit = priceTomanPerUnit,
    recordedAtMillis = recordedAtMillis,
)

fun AssetPriceSnapshot.toEntity() = AssetPriceSnapshotEntity(
    id = id,
    assetId = assetId,
    priceTomanPerUnit = priceTomanPerUnit,
    recordedAtMillis = recordedAtMillis,
)

fun AppEventEntity.toDomain() = AppEvent(
    id = id,
    type = type,
    name = name,
    metadata = metadata,
    occurredAtMillis = occurredAtMillis,
)

fun AppEvent.toEntity() = AppEventEntity(
    id = id,
    type = type,
    name = name,
    metadata = metadata,
    occurredAtMillis = occurredAtMillis,
)

fun SavingsDepositEntity.toDomain() = SavingsDeposit(
    id = id,
    goalId = goalId,
    amountToman = amountToman,
    isWithdrawal = isWithdrawal,
    occurredOnEpochDay = occurredOnEpochDay,
    note = note,
    createdAtMillis = createdAtMillis,
)

fun SavingsDeposit.toEntity() = SavingsDepositEntity(
    id = id,
    goalId = goalId,
    amountToman = amountToman,
    isWithdrawal = isWithdrawal,
    occurredOnEpochDay = occurredOnEpochDay,
    note = note,
    createdAtMillis = createdAtMillis,
)
