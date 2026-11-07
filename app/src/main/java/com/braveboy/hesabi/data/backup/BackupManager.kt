package com.braveboy.hesabi.data.backup

import androidx.room.withTransaction
import com.braveboy.hesabi.data.local.HesabiDatabase
import com.braveboy.hesabi.data.local.dao.AccountDao
import com.braveboy.hesabi.data.local.dao.BudgetDao
import com.braveboy.hesabi.data.local.dao.CategoryDao
import com.braveboy.hesabi.data.local.dao.ChequeDao
import com.braveboy.hesabi.data.local.dao.DebtDao
import com.braveboy.hesabi.data.local.dao.InstallmentDao
import com.braveboy.hesabi.data.local.dao.LoanDao
import com.braveboy.hesabi.data.local.dao.PersonDao
import com.braveboy.hesabi.data.local.dao.RecurringTransactionDao
import com.braveboy.hesabi.data.local.dao.SavingsGoalDao
import com.braveboy.hesabi.data.local.dao.TransactionDao
import com.braveboy.hesabi.data.local.entity.AccountEntity
import com.braveboy.hesabi.data.local.entity.BudgetEntity
import com.braveboy.hesabi.data.local.entity.CategoryEntity
import com.braveboy.hesabi.data.local.entity.ChequeEntity
import com.braveboy.hesabi.data.local.entity.DebtEntity
import com.braveboy.hesabi.data.local.entity.InstallmentEntity
import com.braveboy.hesabi.data.local.entity.LoanEntity
import com.braveboy.hesabi.data.local.entity.PersonEntity
import com.braveboy.hesabi.data.local.entity.RecurringTransactionEntity
import com.braveboy.hesabi.data.local.entity.SavingsGoalEntity
import com.braveboy.hesabi.data.local.entity.TransactionEntity
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.BackupSummary
import com.braveboy.hesabi.domain.model.ImportResult
import com.braveboy.hesabi.domain.repository.BackupRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class BackupManager(
    private val db: HesabiDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val personDao: PersonDao,
    private val debtDao: DebtDao,
    private val chequeDao: ChequeDao,
    private val loanDao: LoanDao,
    private val installmentDao: InstallmentDao,
    private val recurringTransactionDao: RecurringTransactionDao,
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
            transactions = transactionDao.getAll().map {
                TransactionDto(
                    id = it.id,
                    type = it.type,
                    amountToman = it.amountToman,
                    accountId = it.accountId,
                    toAccountId = it.toAccountId,
                    categoryId = it.categoryId,
                    occurredOnEpochDay = it.occurredOnEpochDay,
                    note = it.note,
                    source = it.source,
                    externalRef = it.externalRef,
                    receiptImagePath = it.receiptImagePath,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis,
                )
            },
            budgets = budgetDao.getAll().map {
                BudgetDto(
                    id = it.id,
                    categoryId = it.categoryId,
                    periodKey = it.periodKey,
                    limitToman = it.limitToman,
                )
            },
            savingsGoals = savingsGoalDao.getAll().map {
                SavingsGoalDto(
                    id = it.id,
                    title = it.title,
                    targetAmountToman = it.targetAmountToman,
                    savedAmountToman = it.savedAmountToman,
                    colorArgb = it.colorArgb,
                    iconKey = it.iconKey,
                    targetDateEpochDay = it.targetDateEpochDay,
                    isCompleted = it.isCompleted,
                    createdAtMillis = it.createdAtMillis,
                )
            },
            people = personDao.getAll().map {
                PersonDto(
                    id = it.id,
                    name = it.name,
                    phoneNumber = it.phoneNumber,
                    notes = it.notes,
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
                    note = it.note,
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
                    payeeOrDrawerName = it.payeeOrDrawerName,
                    direction = it.direction,
                    status = it.status,
                    note = it.note,
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
                    title = it.title,
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
                    note = it.note,
                    createdAtMillis = it.createdAtMillis,
                )
            },
        )
        val plain = json.encodeToString(BackupEnvelope.serializer(), envelope).toByteArray(Charsets.UTF_8)
        val encrypted = BackupCrypto.encrypt(plain, password)
        out.write(encrypted)
        out.flush()
        settingsRepository.setLastBackupAt(exportedAt)
        return BackupSummary(
            transactionCount = envelope.transactions.size,
            accountCount = envelope.accounts.size,
            categoryCount = envelope.categories.size,
            exportedAtMillis = exportedAt,
        )
    }

    override suspend fun importEncrypted(input: InputStream, password: CharArray): ImportResult {
        val bytes = input.readBytes()
        if (bytes.size > MAX_BACKUP_BYTES) {
            throw BackupException("حجم فایل پشتیبان بیش از حد مجاز است.")
        }
        val plain = BackupCrypto.decrypt(bytes, password)
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
        val transactions = envelope.transactions.map {
            TransactionEntity(
                id = it.id,
                type = it.type,
                amountToman = it.amountToman,
                accountId = it.accountId,
                toAccountId = it.toAccountId,
                categoryId = it.categoryId,
                occurredOnEpochDay = it.occurredOnEpochDay,
                note = it.note,
                source = it.source,
                externalRef = it.externalRef,
                receiptImagePath = it.receiptImagePath,
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
                updatedAtMillis = if (it.updatedAtMillis == 0L) envelope.exportedAtMillis else it.updatedAtMillis,
            )
        }
        val budgets = envelope.budgets.map {
            BudgetEntity(
                id = it.id,
                categoryId = it.categoryId,
                periodKey = it.periodKey,
                limitToman = it.limitToman,
            )
        }
        val savingsGoals = envelope.savingsGoals.map {
            SavingsGoalEntity(
                id = it.id,
                title = it.title,
                targetAmountToman = it.targetAmountToman,
                savedAmountToman = it.savedAmountToman,
                colorArgb = it.colorArgb,
                iconKey = it.iconKey,
                targetDateEpochDay = it.targetDateEpochDay,
                isCompleted = it.isCompleted,
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
            )
        }
        val people = envelope.people.map {
            PersonEntity(
                id = it.id,
                name = it.name,
                phoneNumber = it.phoneNumber,
                notes = it.notes,
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
                note = it.note,
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
                payeeOrDrawerName = it.payeeOrDrawerName,
                direction = it.direction,
                status = it.status,
                note = it.note,
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
                title = it.title,
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
                note = it.note,
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
            )
        }

        // Replace-all inside one transaction: a bad file can never leave half a restore.
        db.withTransaction {
            recurringTransactionDao.deleteAll()
            installmentDao.deleteAll()
            loanDao.deleteAll()
            chequeDao.deleteAll()
            debtDao.deleteAll()
            personDao.deleteAll()
            savingsGoalDao.deleteAll()
            transactionDao.deleteAll()
            budgetDao.deleteAll()
            categoryDao.deleteAll()
            accountDao.deleteAll()

            accountDao.upsertAll(accounts)
            categoryDao.upsertAll(categories)
            transactionDao.upsertAll(transactions)
            budgetDao.upsertAll(budgets)
            if (savingsGoals.isNotEmpty()) savingsGoalDao.upsertAll(savingsGoals)
            if (people.isNotEmpty()) personDao.upsertAll(people)
            if (debts.isNotEmpty()) debtDao.upsertAll(debts)
            if (cheques.isNotEmpty()) chequeDao.upsertAll(cheques)
            if (loans.isNotEmpty()) loanDao.upsertAll(loans)
            if (installments.isNotEmpty()) installmentDao.upsertAll(installments)
            if (recurringTransactions.isNotEmpty()) recurringTransactionDao.upsertAll(recurringTransactions)
        }

        val defaultAccountId = settingsRepository.current().defaultAccountId
        if (defaultAccountId == null || accounts.none { it.id == defaultAccountId }) {
            settingsRepository.setDefaultAccountId(accounts.first().id)
        }

        return ImportResult(
            summary = BackupSummary(
                transactionCount = transactions.size,
                accountCount = accounts.size,
                categoryCount = categories.size,
                exportedAtMillis = envelope.exportedAtMillis,
            ),
            schemaVersion = envelope.schema,
        )
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

    companion object {
        const val MAX_BACKUP_BYTES = 25 * 1024 * 1024
    }
}
