package com.braveboy.hesabi.data.backup

import androidx.room.withTransaction
import com.braveboy.hesabi.data.local.HesabiDatabase
import com.braveboy.hesabi.data.local.dao.AccountDao
import com.braveboy.hesabi.data.local.dao.BudgetDao
import com.braveboy.hesabi.data.local.dao.CategoryDao
import com.braveboy.hesabi.data.local.dao.TransactionDao
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
            com.braveboy.hesabi.domain.model.Account(
                id = it.id,
                name = it.name,
                kind = com.braveboy.hesabi.domain.model.AccountKind.entries
                    .firstOrNull { kind -> kind.name == it.kind }
                    ?: com.braveboy.hesabi.domain.model.AccountKind.CASH,
                colorArgb = it.colorArgb,
                openingBalanceToman = it.openingBalanceToman,
                archived = it.archived,
                sortOrder = it.sortOrder,
            )
        }
        val categories = envelope.categories.map {
            com.braveboy.hesabi.domain.model.Category(
                id = it.id,
                name = it.name,
                kind = com.braveboy.hesabi.domain.model.CategoryKind.entries
                    .firstOrNull { kind -> kind.name == it.kind }
                    ?: com.braveboy.hesabi.domain.model.CategoryKind.EXPENSE,
                iconKey = it.iconKey,
                colorArgb = it.colorArgb,
                archived = it.archived,
                sortOrder = it.sortOrder,
            )
        }
        val transactions = envelope.transactions.map {
            com.braveboy.hesabi.domain.model.Transaction(
                id = it.id,
                type = com.braveboy.hesabi.domain.model.TransactionType.entries
                    .firstOrNull { type -> type.name == it.type }
                    ?: com.braveboy.hesabi.domain.model.TransactionType.EXPENSE,
                amountToman = it.amountToman,
                accountId = it.accountId,
                toAccountId = it.toAccountId,
                categoryId = it.categoryId,
                occurredOnEpochDay = it.occurredOnEpochDay,
                note = it.note,
                source = com.braveboy.hesabi.domain.model.TransactionSource.entries
                    .firstOrNull { source -> source.name == it.source }
                    ?: com.braveboy.hesabi.domain.model.TransactionSource.IMPORT,
                externalRef = it.externalRef,
                createdAtMillis = if (it.createdAtMillis == 0L) envelope.exportedAtMillis else it.createdAtMillis,
                updatedAtMillis = if (it.updatedAtMillis == 0L) envelope.exportedAtMillis else it.updatedAtMillis,
            )
        }
        val budgets = envelope.budgets.map {
            com.braveboy.hesabi.domain.model.Budget(
                id = it.id,
                categoryId = it.categoryId,
                periodKey = it.periodKey,
                limitToman = it.limitToman,
            )
        }

        // Replace-all inside one transaction: a bad file can never leave half a restore.
        db.withTransaction {
            transactionDao.deleteAll()
            budgetDao.deleteAll()
            categoryDao.deleteAll()
            accountDao.deleteAll()
            accountDao.upsertAll(accounts.map { it.toEntity() })
            categoryDao.upsertAll(categories.map { it.toEntity() })
            transactionDao.upsertAll(transactions.map { it.toEntity() })
            budgetDao.upsertAll(budgets.map { it.toEntity() })
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
