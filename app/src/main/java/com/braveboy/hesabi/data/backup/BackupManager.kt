package com.braveboy.hesabi.data.backup

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.braveboy.hesabi.core.money.Currency
import com.braveboy.hesabi.core.receipt.ReceiptStorageManager
import com.braveboy.hesabi.core.security.FieldCrypto
import com.braveboy.hesabi.data.local.HesabiDatabase
import com.braveboy.hesabi.data.local.dao.AccountDao
import com.braveboy.hesabi.data.local.dao.AssetDao
import com.braveboy.hesabi.data.local.dao.AssetPriceSnapshotDao
import com.braveboy.hesabi.data.local.dao.BudgetDao
import com.braveboy.hesabi.data.local.dao.CategoryDao
import com.braveboy.hesabi.data.local.dao.ChequeDao
import com.braveboy.hesabi.data.local.dao.DebtDao
import com.braveboy.hesabi.data.local.dao.InstallmentDao
import com.braveboy.hesabi.data.local.dao.LoanDao
import com.braveboy.hesabi.data.local.dao.PersonDao
import com.braveboy.hesabi.data.local.dao.QuickTemplateDao
import com.braveboy.hesabi.data.local.dao.RecurringTransactionDao
import com.braveboy.hesabi.data.local.dao.SavingsDepositDao
import com.braveboy.hesabi.data.local.dao.SavingsGoalDao
import com.braveboy.hesabi.data.local.dao.SenderRuleDao
import com.braveboy.hesabi.data.local.dao.SmsDao
import com.braveboy.hesabi.data.local.dao.TransactionDao
import com.braveboy.hesabi.data.local.entity.AccountEntity
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
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.domain.model.AiMode
import com.braveboy.hesabi.domain.model.BackupSummary
import com.braveboy.hesabi.domain.model.ImportResult
import com.braveboy.hesabi.domain.model.ThemeMode
import com.braveboy.hesabi.domain.repository.BackupImportMode
import com.braveboy.hesabi.domain.repository.BackupRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Single-file, password-encrypted backup of *everything the user created*:
 * transactions (including the recycle bin), accounts, categories, budgets, goals and
 * their deposits, people/debts/cheques/loans, recurring entries, quick templates,
 * assets and their price history, SMS history, sender rules, non-secret settings and
 * the receipt images themselves.
 *
 * Receipts are read through [ReceiptStorageManager], so a backup contains plaintext
 * images inside the encrypted envelope but the files on disk stay encrypted.
 */
class BackupManager(
    private val appContext: Context,
    private val db: HesabiDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val savingsDepositDao: SavingsDepositDao,
    private val personDao: PersonDao,
    private val debtDao: DebtDao,
    private val chequeDao: ChequeDao,
    private val loanDao: LoanDao,
    private val installmentDao: InstallmentDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val quickTemplateDao: QuickTemplateDao,
    private val assetDao: AssetDao,
    private val assetPriceSnapshotDao: AssetPriceSnapshotDao,
    private val smsDao: SmsDao,
    private val senderRuleDao: SenderRuleDao,
    private val settingsRepository: SettingsRepository,
    private val appVersion: String,
) : BackupRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override suspend fun exportEncrypted(out: OutputStream, password: CharArray): BackupSummary {
        val exportedAt = System.currentTimeMillis()
        val transactions = transactionDao.getAllIncludingDeleted()
        val (receiptDtos, skippedReceipts) = collectReceipts(transactions)

        val settings = settingsRepository.current()
        val envelope = BackupEnvelope(
            appVersion = appVersion,
            exportedAtMillis = exportedAt,
            accounts = accountDao.getAll().map {
                AccountDto(
                    id = it.id,
                    name = it.name,
                    kind = it.kind,
                    colorArgb = it.colorArgb,
                    openingBalanceToman = it.openingBalanceToman,
                    archived = it.archived,
                    sortOrder = it.sortOrder,
                )
            },
            categories = categoryDao.getAll().map {
                CategoryDto(
                    id = it.id,
                    name = it.name,
                    kind = it.kind,
                    iconKey = it.iconKey,
                    colorArgb = it.colorArgb,
                    archived = it.archived,
                    sortOrder = it.sortOrder,
                )
            },
            transactions = transactions.map {
                TransactionDto(
                    id = it.id,
                    type = it.type,
                    amountToman = it.amountToman,
                    accountId = it.accountId,
                    toAccountId = it.toAccountId,
                    categoryId = it.categoryId,
                    occurredOnEpochDay = it.occurredOnEpochDay,
                    note = FieldCrypto.open(it.note),
                    source = it.source,
                    externalRef = it.externalRef,
                    receiptImagePath = it.receiptImagePath,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis,
                    deletedAtMillis = it.deletedAtMillis,
                    linkedEntityType = it.linkedEntityType,
                    linkedEntityId = it.linkedEntityId,
                )
            },
            budgets = budgetDao.getAll().map {
                BudgetDto(
                    id = it.id,
                    categoryId = it.categoryId,
                    periodKey = it.periodKey,
                    limitToman = it.limitToman,
                    rolloverEnabled = it.rolloverEnabled,
                )
            },
            savingsGoals = savingsGoalDao.getAll().map {
                SavingsGoalDto(
                    id = it.id,
                    title = FieldCrypto.open(it.title),
                    targetAmountToman = it.targetAmountToman,
                    savedAmountToman = it.savedAmountToman,
                    colorArgb = it.colorArgb,
                    iconKey = it.iconKey,
                    targetDateEpochDay = it.targetDateEpochDay,
                    isCompleted = it.isCompleted,
                    createdAtMillis = it.createdAtMillis,
                    monthlyTargetToman = it.monthlyTargetToman,
                )
            },
            savingsDeposits = savingsDepositDao.getAll().map {
                SavingsDepositDto(
                    id = it.id,
                    goalId = it.goalId,
                    amountToman = it.amountToman,
                    isWithdrawal = it.isWithdrawal,
                    occurredOnEpochDay = it.occurredOnEpochDay,
                    note = it.note,
                    createdAtMillis = it.createdAtMillis,
                )
            },
            people = personDao.getAll().map {
                PersonDto(
                    id = it.id,
                    name = FieldCrypto.open(it.name),
                    phoneNumber = FieldCrypto.open(it.phoneNumber),
                    notes = FieldCrypto.open(it.notes),
                    createdAtMillis = it.createdAtMillis,
                )
            },
            debts = debtDao.getAll().map {
                DebtDto(
                    id = it.id,
                    personId = it.personId,
                    amountToman = it.amountToman,
                    settledAmountToman = it.settledAmountToman,
                    direction = it.direction,
                    dueEpochDay = it.dueEpochDay,
                    note = FieldCrypto.open(it.note),
                    isSettled = it.isSettled,
                    createdAtMillis = it.createdAtMillis,
                )
            },
            cheques = chequeDao.getAll().map {
                ChequeDto(
                    id = it.id,
                    sayadNumber = it.sayadNumber,
                    bankName = it.bankName,
                    amountToman = it.amountToman,
                    dueEpochDay = it.dueEpochDay,
                    payeeOrDrawerName = FieldCrypto.open(it.payeeOrDrawerName),
                    direction = it.direction,
                    status = it.status,
                    note = FieldCrypto.open(it.note),
                    createdAtMillis = it.createdAtMillis,
                )
            },
            loans = loanDao.getAll().map {
                LoanDto(
                    id = it.id,
                    title = it.title,
                    totalAmountToman = it.totalAmountToman,
                    installmentCount = it.installmentCount,
                    installmentAmountToman = it.installmentAmountToman,
                    accountId = it.accountId,
                    firstDueEpochDay = it.firstDueEpochDay,
                    note = it.note,
                    isCompleted = it.isCompleted,
                    createdAtMillis = it.createdAtMillis,
                )
            },
            installments = installmentDao.getAll().map {
                InstallmentDto(
                    id = it.id,
                    loanId = it.loanId,
                    installmentNumber = it.installmentNumber,
                    amountToman = it.amountToman,
                    dueEpochDay = it.dueEpochDay,
                    isPaid = it.isPaid,
                    paidAtEpochDay = it.paidAtEpochDay,
                )
            },
            recurringTransactions = recurringTransactionDao.getAll().map {
                RecurringTransactionDto(
                    id = it.id,
                    title = FieldCrypto.open(it.title),
                    type = it.type,
                    amountToman = it.amountToman,
                    accountId = it.accountId,
                    toAccountId = it.toAccountId,
                    categoryId = it.categoryId,
                    frequency = it.frequency,
                    interval = it.interval,
                    dayOfMonth = it.dayOfMonth,
                    nextOccurrenceEpochDay = it.nextOccurrenceEpochDay,
                    lastAppliedEpochDay = it.lastAppliedEpochDay,
                    autoApply = it.autoApply,
                    isActive = it.isActive,
                    note = FieldCrypto.open(it.note),
                    createdAtMillis = it.createdAtMillis,
                )
            },
            quickTemplates = quickTemplateDao.getAll().map {
                QuickTemplateDto(
                    id = it.id,
                    label = FieldCrypto.open(it.label),
                    type = it.type,
                    amountToman = it.amountToman,
                    categoryId = it.categoryId,
                    accountId = it.accountId,
                    toAccountId = it.toAccountId,
                    note = FieldCrypto.open(it.note),
                    usageCount = it.usageCount,
                    sortOrder = it.sortOrder,
                    createdAtMillis = it.createdAtMillis,
                )
            },
            assets = assetDao.getAll().map {
                AssetDto(
                    id = it.id,
                    name = FieldCrypto.open(it.name),
                    kind = it.kind,
                    quantityMilli = it.quantityMilli,
                    unitLabel = it.unitLabel,
                    buyPriceTomanPerUnit = it.buyPriceTomanPerUnit,
                    currentPriceTomanPerUnit = it.currentPriceTomanPerUnit,
                    priceUpdatedAtMillis = it.priceUpdatedAtMillis,
                    priceSourceLabel = it.priceSourceLabel,
                    note = FieldCrypto.open(it.note),
                    archived = it.archived,
                    createdAtMillis = it.createdAtMillis,
                )
            },
            assetPriceSnapshots = assetPriceSnapshotDao.getAll().map {
                AssetPriceSnapshotDto(
                    id = it.id,
                    assetId = it.assetId,
                    priceTomanPerUnit = it.priceTomanPerUnit,
                    recordedAtMillis = it.recordedAtMillis,
                )
            },
            smsEvents = smsDao.getAll().map {
                SmsEventDto(
                    id = it.id,
                    sender = it.sender,
                    body = FieldCrypto.open(it.body),
                    receivedAtMillis = it.receivedAtMillis,
                    rawHash = it.rawHash,
                    amountToman = it.amountToman,
                    type = it.type,
                    last4 = it.last4,
                    occurredOnEpochDay = it.occurredOnEpochDay,
                    merchant = it.merchant?.let { value -> FieldCrypto.open(value) },
                    confidence = it.confidence,
                    status = it.status,
                    duplicateOfTransactionId = it.duplicateOfTransactionId,
                )
            },
            senderRules = senderRuleDao.getAll().map { SenderRuleDto(sender = it.sender, ignored = it.ignored) },
            receipts = receiptDtos,
            settings = SettingsDto(
                onboarded = settings.onboarded,
                themeMode = settings.themeMode.name,
                dynamicColor = settings.dynamicColor,
                currency = settings.currency.name,
                hideAmounts = settings.hideAmounts,
                smsCaptureEnabled = settings.smsCaptureEnabled,
                amountsAreRial = settings.amountsAreRial,
                paydayDayOfMonth = settings.paydayDayOfMonth,
                remindersEnabled = settings.remindersEnabled,
                monthlySalaryToman = settings.monthlySalaryToman,
                salaryAccountId = settings.salaryAccountId,
                autoDepositSalary = settings.autoDepositSalary,
                aiMode = settings.aiMode.name,
                smsRationaleAccepted = settings.smsRationaleAccepted,
                assetPricesAutoUpdate = settings.assetPricesAutoUpdate,
            ),
        )

        val plain = json.encodeToString(BackupEnvelope.serializer(), envelope).toByteArray(Charsets.UTF_8)
        val encrypted = BackupCrypto.encrypt(plain, password)
        out.write(FILE_HEADER)
        out.write(encrypted)
        out.flush()
        settingsRepository.setLastBackupAt(exportedAt)
        return BackupSummary(
            transactionCount = envelope.transactions.size,
            accountCount = envelope.accounts.size,
            categoryCount = envelope.categories.size,
            exportedAtMillis = exportedAt,
            receiptCount = receiptDtos.size,
            skippedReceipts = skippedReceipts,
        )
    }

    private suspend fun collectReceipts(transactions: List<TransactionEntity>): Pair<List<ReceiptDto>, Int> {
        val paths = transactions.mapNotNull { it.receiptImagePath }.distinct()
        val receipts = mutableListOf<ReceiptDto>()
        var skipped = 0
        var totalBytes = 0L
        for (path in paths) {
            val bytes = ReceiptStorageManager.readDecrypted(path)
            if (bytes == null) {
                skipped++
                continue
            }
            if (totalBytes + bytes.size > MAX_RECEIPT_BYTES) {
                skipped++
                continue
            }
            totalBytes += bytes.size
            receipts += ReceiptDto(
                path = path,
                base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
            )
        }
        return receipts to skipped
    }

    override suspend fun importEncrypted(
        input: InputStream,
        password: CharArray,
        mode: BackupImportMode,
    ): ImportResult {
        val raw = readLimited(input)
        val data = if (raw.size > FILE_HEADER.size &&
            FILE_HEADER.indices.all { raw[it] == FILE_HEADER[it] }
        ) {
            raw.copyOfRange(FILE_HEADER.size, raw.size)
        } else {
            // Backups written before the plain header existed are still accepted.
            raw
        }
        val plain = BackupCrypto.decrypt(data, password)
        val envelope = try {
            json.decodeFromString(BackupEnvelope.serializer(), plain.toString(Charsets.UTF_8))
        } catch (error: Exception) {
            throw BackupException("محتوای فایل پشتیبان قابل خواندن نیست.", error)
        }
        if (envelope.magic != BackupEnvelope.MAGIC) throw BackupException("این فایل، پشتیبان حسابی نیست.")
        if (envelope.schema > BackupEnvelope.SCHEMA) {
            throw BackupException("این پشتیبان از نسخهٔ جدیدتر حسابی است.")
        }
        if (envelope.accounts.isEmpty()) throw BackupException("فایل پشتیبان حساب ندارد.")

        // Restore receipt files first so transactions can point at real paths.
        val receiptPathMap = LinkedHashMap<String, String>()
        envelope.receipts.forEach { receipt ->
            val bytes = runCatching { Base64.decode(receipt.base64, Base64.NO_WRAP) }.getOrNull()
                ?: return@forEach
            if (bytes.size > MAX_SINGLE_RECEIPT_BYTES) return@forEach
            ReceiptStorageManager.saveReceiptBytes(appContext, bytes)?.let { newPath ->
                receiptPathMap[receipt.path] = newPath
            }
        }

        val accounts = envelope.accounts.map {
            AccountEntity(
                id = it.id,
                name = it.name,
                kind = it.kind,
                colorArgb = it.colorArgb,
                openingBalanceToman = it.openingBalanceToman,
                archived = it.archived,
                sortOrder = it.sortOrder,
            )
        }
        val categories = envelope.categories.map {
            CategoryEntity(
                id = it.id,
                name = it.name,
                kind = it.kind,
                iconKey = it.iconKey,
                colorArgb = it.colorArgb,
                archived = it.archived,
                sortOrder = it.sortOrder,
            )
        }
        // A transaction keeps its receipt only when the blob came with the backup.
        val transactions = envelope.transactions.map {
            TransactionEntity(
                id = it.id,
                type = it.type,
                amountToman = it.amountToman,
                accountId = it.accountId,
                toAccountId = it.toAccountId,
                categoryId = it.categoryId,
                occurredOnEpochDay = it.occurredOnEpochDay,
                note = FieldCrypto.seal(it.note),
                source = it.source,
                externalRef = it.externalRef,
                receiptImagePath = it.receiptImagePath?.let { old -> receiptPathMap[old] },
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
                updatedAtMillis = if (it.updatedAtMillis == 0L) envelope.exportedAtMillis else it.updatedAtMillis,
                deletedAtMillis = it.deletedAtMillis,
                linkedEntityType = it.linkedEntityType,
                linkedEntityId = it.linkedEntityId,
            )
        }
        val budgets = envelope.budgets.map {
            BudgetEntity(
                id = it.id,
                categoryId = it.categoryId,
                periodKey = it.periodKey,
                limitToman = it.limitToman,
                rolloverEnabled = it.rolloverEnabled,
            )
        }
        val savingsGoals = envelope.savingsGoals.map {
            SavingsGoalEntity(
                id = it.id,
                title = FieldCrypto.seal(it.title),
                targetAmountToman = it.targetAmountToman,
                savedAmountToman = it.savedAmountToman,
                colorArgb = it.colorArgb,
                iconKey = it.iconKey,
                targetDateEpochDay = it.targetDateEpochDay,
                isCompleted = it.isCompleted,
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
                monthlyTargetToman = it.monthlyTargetToman,
            )
        }
        val savingsDeposits = envelope.savingsDeposits.map {
            SavingsDepositEntity(
                id = it.id,
                goalId = it.goalId,
                amountToman = it.amountToman,
                isWithdrawal = it.isWithdrawal,
                occurredOnEpochDay = it.occurredOnEpochDay,
                note = it.note,
                createdAtMillis = it.createdAtMillis,
            )
        }
        val people = envelope.people.map {
            PersonEntity(
                id = it.id,
                name = FieldCrypto.seal(it.name),
                phoneNumber = FieldCrypto.seal(it.phoneNumber),
                notes = FieldCrypto.seal(it.notes),
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
            )
        }
        val debts = envelope.debts.map {
            DebtEntity(
                id = it.id,
                personId = it.personId,
                amountToman = it.amountToman,
                settledAmountToman = it.settledAmountToman,
                direction = it.direction,
                dueEpochDay = it.dueEpochDay,
                note = FieldCrypto.seal(it.note),
                isSettled = it.isSettled,
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
            )
        }
        val cheques = envelope.cheques.map {
            ChequeEntity(
                id = it.id,
                sayadNumber = it.sayadNumber,
                bankName = it.bankName,
                amountToman = it.amountToman,
                dueEpochDay = it.dueEpochDay,
                payeeOrDrawerName = FieldCrypto.seal(it.payeeOrDrawerName),
                direction = it.direction,
                status = it.status,
                note = FieldCrypto.seal(it.note),
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
            )
        }
        val loans = envelope.loans.map {
            LoanEntity(
                id = it.id,
                title = it.title,
                totalAmountToman = it.totalAmountToman,
                installmentCount = it.installmentCount,
                installmentAmountToman = it.installmentAmountToman,
                accountId = it.accountId,
                firstDueEpochDay = it.firstDueEpochDay,
                note = it.note,
                isCompleted = it.isCompleted,
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
            )
        }
        val installments = envelope.installments.map {
            InstallmentEntity(
                id = it.id,
                loanId = it.loanId,
                installmentNumber = it.installmentNumber,
                amountToman = it.amountToman,
                dueEpochDay = it.dueEpochDay,
                isPaid = it.isPaid,
                paidAtEpochDay = it.paidAtEpochDay,
            )
        }
        val recurringTransactions = envelope.recurringTransactions.map {
            RecurringTransactionEntity(
                id = it.id,
                title = FieldCrypto.seal(it.title),
                type = it.type,
                amountToman = it.amountToman,
                accountId = it.accountId,
                toAccountId = it.toAccountId,
                categoryId = it.categoryId,
                frequency = it.frequency,
                interval = it.interval,
                dayOfMonth = it.dayOfMonth,
                nextOccurrenceEpochDay = it.nextOccurrenceEpochDay,
                lastAppliedEpochDay = it.lastAppliedEpochDay,
                autoApply = it.autoApply,
                isActive = it.isActive,
                note = FieldCrypto.seal(it.note),
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
            )
        }
        val quickTemplates = envelope.quickTemplates.map {
            QuickTemplateEntity(
                id = it.id,
                label = FieldCrypto.seal(it.label),
                type = it.type,
                amountToman = it.amountToman,
                categoryId = it.categoryId,
                accountId = it.accountId,
                toAccountId = it.toAccountId,
                note = FieldCrypto.seal(it.note),
                usageCount = it.usageCount,
                sortOrder = it.sortOrder,
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
            )
        }
        val assets = envelope.assets.map {
            AssetEntity(
                id = it.id,
                name = FieldCrypto.seal(it.name),
                kind = it.kind,
                quantityMilli = it.quantityMilli,
                unitLabel = it.unitLabel,
                buyPriceTomanPerUnit = it.buyPriceTomanPerUnit,
                currentPriceTomanPerUnit = it.currentPriceTomanPerUnit,
                priceUpdatedAtMillis = it.priceUpdatedAtMillis,
                priceSourceLabel = it.priceSourceLabel,
                note = FieldCrypto.seal(it.note),
                archived = it.archived,
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
            )
        }
        val assetSnapshots = envelope.assetPriceSnapshots.map {
            AssetPriceSnapshotEntity(
                id = it.id,
                assetId = it.assetId,
                priceTomanPerUnit = it.priceTomanPerUnit,
                recordedAtMillis = it.recordedAtMillis,
            )
        }
        val smsEvents = envelope.smsEvents.map {
            SmsEventEntity(
                id = it.id,
                sender = it.sender,
                body = FieldCrypto.seal(it.body),
                receivedAtMillis = it.receivedAtMillis,
                rawHash = it.rawHash,
                amountToman = it.amountToman,
                type = it.type,
                last4 = it.last4,
                occurredOnEpochDay = it.occurredOnEpochDay,
                merchant = it.merchant?.let { value -> FieldCrypto.seal(value) },
                confidence = it.confidence,
                status = it.status,
                duplicateOfTransactionId = it.duplicateOfTransactionId,
            )
        }
        val senderRules = envelope.senderRules.map {
            SenderRuleEntity(sender = it.sender, ignored = it.ignored)
        }

        val transactionsToWrite = if (mode == BackupImportMode.MERGE) {
            val existing = transactionDao.getAllIncludingDeleted()
            val existingRefs = existing.mapNotNull { it.externalRef }.toSet()
            val existingById = existing.associateBy { it.id }
            transactions.filter { tx ->
                tx.externalRef == null || tx.externalRef !in existingRefs ||
                    existingById[tx.id]?.externalRef == tx.externalRef
            }
        } else {
            transactions
        }

        db.withTransaction {
            if (mode == BackupImportMode.REPLACE) {
                recurringTransactionDao.deleteAll()
                installmentDao.deleteAll()
                loanDao.deleteAll()
                chequeDao.deleteAll()
                debtDao.deleteAll()
                personDao.deleteAll()
                savingsDepositDao.deleteAll()
                savingsGoalDao.deleteAll()
                transactionDao.deleteAll()
                budgetDao.deleteAll()
                quickTemplateDao.deleteAll()
                assetPriceSnapshotDao.deleteAll()
                assetDao.deleteAll()
                smsDao.deleteAll()
                senderRuleDao.deleteAll()
                categoryDao.deleteAll()
                accountDao.deleteAll()
            }

            accountDao.upsertAll(accounts)
            categoryDao.upsertAll(categories)
            transactionDao.upsertAll(transactionsToWrite)
            budgetDao.upsertAll(budgets)
            if (savingsGoals.isNotEmpty()) savingsGoalDao.upsertAll(savingsGoals)
            if (savingsDeposits.isNotEmpty()) savingsDepositDao.upsertAll(savingsDeposits)
            if (people.isNotEmpty()) personDao.upsertAll(people)
            if (debts.isNotEmpty()) debtDao.upsertAll(debts)
            if (cheques.isNotEmpty()) chequeDao.upsertAll(cheques)
            if (loans.isNotEmpty()) loanDao.upsertAll(loans)
            if (installments.isNotEmpty()) installmentDao.upsertAll(installments)
            if (recurringTransactions.isNotEmpty()) recurringTransactionDao.upsertAll(recurringTransactions)
            if (quickTemplates.isNotEmpty()) quickTemplateDao.upsertAll(quickTemplates)
            if (assets.isNotEmpty()) assetDao.upsertAll(assets)
            if (assetSnapshots.isNotEmpty()) assetPriceSnapshotDao.upsertAll(assetSnapshots)
            if (smsEvents.isNotEmpty()) smsDao.upsertAll(smsEvents)
            if (senderRules.isNotEmpty()) senderRuleDao.upsertAll(senderRules)
        }

        envelope.settings?.let { applySettings(it) }

        val defaultAccountId = settingsRepository.current().defaultAccountId
        if (defaultAccountId == null || accounts.none { it.id == defaultAccountId }) {
            settingsRepository.setDefaultAccountId(accounts.first().id)
        }

        // Remove receipt files of replaced transactions that were not restored.
        if (mode == BackupImportMode.REPLACE) {
            val referenced = transactionDao.getAllIncludingDeleted().mapNotNull { it.receiptImagePath }.toSet()
            ReceiptStorageManager.deleteOrphaned(appContext, referenced)
        }

        return ImportResult(
            summary = BackupSummary(
                transactionCount = transactionsToWrite.size,
                accountCount = accounts.size,
                categoryCount = categories.size,
                exportedAtMillis = envelope.exportedAtMillis,
                receiptCount = receiptPathMap.size,
            ),
            schemaVersion = envelope.schema,
        )
    }

    private suspend fun applySettings(settings: SettingsDto) {
        settingsRepository.setOnboarded(settings.onboarded)
        ThemeMode.entries.firstOrNull { it.name == settings.themeMode }?.let {
            settingsRepository.setThemeMode(it)
        }
        settingsRepository.setDynamicColor(settings.dynamicColor)
        Currency.entries.firstOrNull { it.name == settings.currency }?.let {
            settingsRepository.setCurrency(it)
        }
        settingsRepository.setHideAmounts(settings.hideAmounts)
        settingsRepository.setSmsCaptureEnabled(settings.smsCaptureEnabled)
        settingsRepository.setAmountsAreRial(settings.amountsAreRial)
        settingsRepository.setPaydayDayOfMonth(settings.paydayDayOfMonth)
        settingsRepository.setRemindersEnabled(settings.remindersEnabled)
        settingsRepository.setMonthlySalary(settings.monthlySalaryToman)
        settingsRepository.setSalaryAccountId(settings.salaryAccountId)
        settingsRepository.setAutoDepositSalary(settings.autoDepositSalary)
        AiMode.entries.firstOrNull { it.name == settings.aiMode }?.let {
            settingsRepository.setAiMode(it)
        }
        settingsRepository.setSmsRationaleAccepted(settings.smsRationaleAccepted)
        settingsRepository.setAssetPricesAutoUpdate(settings.assetPricesAutoUpdate)
    }

    override suspend fun exportCsv(out: OutputStream): Int {
        val transactions = transactionDao.getAll().map { it.toDomain() }
        val accounts = accountDao.getAll().map { it.toDomain() }
        val categories = categoryDao.getAll().map { it.toDomain() }
        val csv = CsvCodec.export(transactions, accounts, categories)
        out.write(csv.toByteArray(Charsets.UTF_8))
        out.flush()
        settingsRepository.setLastBackupAt(System.currentTimeMillis())
        return transactions.size
    }

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_BACKUP_BYTES) {
                throw BackupException("حجم فایل پشتیبان بیش از حد مجاز است.")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    companion object {
        const val MAX_BACKUP_BYTES = 25 * 1024 * 1024
        private const val MAX_RECEIPT_BYTES = 14L * 1024 * 1024
        private const val MAX_SINGLE_RECEIPT_BYTES = 3 * 1024 * 1024
        private val FILE_HEADER = "HESABI-BACKUP-1\n".toByteArray(Charsets.UTF_8)
    }
}
