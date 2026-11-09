package com.braveboy.hesabi.di

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.braveboy.hesabi.BuildConfig
import com.braveboy.hesabi.data.backup.BackupManager
import com.braveboy.hesabi.data.local.HesabiDatabase
import com.braveboy.hesabi.data.repository.AccountRepositoryImpl
import com.braveboy.hesabi.data.repository.AssetRepositoryImpl
import com.braveboy.hesabi.data.repository.BudgetRepositoryImpl
import com.braveboy.hesabi.data.repository.CategoryRepositoryImpl
import com.braveboy.hesabi.data.repository.DiagnosticsRepositoryImpl
import com.braveboy.hesabi.data.repository.ChequeRepositoryImpl
import com.braveboy.hesabi.data.repository.DebtRepositoryImpl
import com.braveboy.hesabi.data.repository.LoanRepositoryImpl
import com.braveboy.hesabi.data.repository.PersonRepositoryImpl
import com.braveboy.hesabi.data.repository.QuickTemplateRepositoryImpl
import com.braveboy.hesabi.data.repository.RecurringTransactionRepositoryImpl
import com.braveboy.hesabi.data.repository.SavingsGoalRepositoryImpl
import com.braveboy.hesabi.data.repository.SettingsRepositoryImpl
import com.braveboy.hesabi.data.repository.SmsRepositoryImpl
import com.braveboy.hesabi.data.repository.TransactionRepositoryImpl
import com.braveboy.hesabi.core.receipt.ReceiptStorageManager
import com.braveboy.hesabi.domain.logic.SalarySchedule
import com.braveboy.hesabi.data.seed.SeedData
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.AssetRepository
import com.braveboy.hesabi.domain.repository.BackupRepository
import com.braveboy.hesabi.domain.repository.BudgetRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.DiagnosticsRepository
import com.braveboy.hesabi.domain.repository.ChequeRepository
import com.braveboy.hesabi.domain.repository.DebtRepository
import com.braveboy.hesabi.domain.repository.LoanRepository
import com.braveboy.hesabi.domain.repository.PersonRepository
import com.braveboy.hesabi.domain.repository.QuickTemplateRepository
import com.braveboy.hesabi.domain.repository.RecurringTransactionRepository
import com.braveboy.hesabi.domain.repository.SavingsGoalRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import com.braveboy.hesabi.domain.repository.SmsRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: HesabiDatabase = Room.databaseBuilder(
        appContext,
        HesabiDatabase::class.java,
        HesabiDatabase.NAME,
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .build()

    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(appContext)
    val accountRepository: AccountRepository = AccountRepositoryImpl(database.accountDao(), database.transactionDao())
    val categoryRepository: CategoryRepository = CategoryRepositoryImpl(database.categoryDao())
    val transactionRepository: TransactionRepository = TransactionRepositoryImpl(database.transactionDao())
    val budgetRepository: BudgetRepository = BudgetRepositoryImpl(database.budgetDao())
    val savingsGoalRepository: SavingsGoalRepository =
        SavingsGoalRepositoryImpl(database.savingsGoalDao(), database.savingsDepositDao())
    val quickTemplateRepository: QuickTemplateRepository = QuickTemplateRepositoryImpl(database.quickTemplateDao())
    val assetRepository: AssetRepository = AssetRepositoryImpl(database.assetDao(), database.assetPriceSnapshotDao())
    val diagnosticsRepository: DiagnosticsRepository = DiagnosticsRepositoryImpl(database.appEventDao())
    val personRepository: PersonRepository = PersonRepositoryImpl(database.personDao(), database.debtDao())
    val debtRepository: DebtRepository = DebtRepositoryImpl(database.debtDao())
    val chequeRepository: ChequeRepository = ChequeRepositoryImpl(database.chequeDao())
    val loanRepository: LoanRepository = LoanRepositoryImpl(database, database.loanDao(), database.installmentDao())
    val recurringTransactionRepository: RecurringTransactionRepository = RecurringTransactionRepositoryImpl(database.recurringTransactionDao())
    val recurringTransactionProcessor: com.braveboy.hesabi.core.recurring.RecurringTransactionProcessor =
        com.braveboy.hesabi.core.recurring.RecurringTransactionProcessor(recurringTransactionRepository, transactionRepository)
    val smsRepository: SmsRepository = SmsRepositoryImpl(
        db = database,
        smsDao = database.smsDao(),
        senderRuleDao = database.senderRuleDao(),
        transactionDao = database.transactionDao(),
        settingsRepository = settingsRepository,
    )
    val backupRepository: BackupRepository = BackupManager(
        appContext = appContext,
        db = database,
        accountDao = database.accountDao(),
        categoryDao = database.categoryDao(),
        transactionDao = database.transactionDao(),
        budgetDao = database.budgetDao(),
        savingsGoalDao = database.savingsGoalDao(),
        personDao = database.personDao(),
        debtDao = database.debtDao(),
        chequeDao = database.chequeDao(),
        loanDao = database.loanDao(),
        installmentDao = database.installmentDao(),
        recurringTransactionDao = database.recurringTransactionDao(),
        savingsDepositDao = database.savingsDepositDao(),
        quickTemplateDao = database.quickTemplateDao(),
        assetDao = database.assetDao(),
        assetPriceSnapshotDao = database.assetPriceSnapshotDao(),
        smsDao = database.smsDao(),
        senderRuleDao = database.senderRuleDao(),
        settingsRepository = settingsRepository,
        appVersion = BuildConfig.VERSION_NAME,
    )

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (database.accountDao().count() == 0) {
            database.accountDao().upsertAll(SeedData.accounts.map { it.toEntity() })
        }
        if (database.categoryDao().count() == 0) {
            database.categoryDao().upsertAll(SeedData.categories.map { it.toEntity() })
        }
        if (database.quickTemplateDao().getAll().isEmpty()) {
            database.quickTemplateDao().upsertAll(SeedData.quickTemplates.map { it.toEntity() })
        }
        if (settingsRepository.current().defaultAccountId == null) {
            val first = database.accountDao().getAll().firstOrNull()
            if (first != null) settingsRepository.setDefaultAccountId(first.id)
        }
    }

    /**
     * Deposits the monthly salary for every due month that has not been posted yet.
     *
     * The effective payday is `min(configuredDay, daysInMonth)`, so a payday of 31
     * still runs in 30-day (and 29-day) months. Each month is keyed by
     * `salary:<year-month>` so a second run never duplicates it.
     */
    suspend fun processSalaryAutoDeposit(
        todayEpochDay: Long = com.braveboy.hesabi.core.date.JalaliDate.today().toEpochDay(),
    ) = withContext(Dispatchers.IO) {
        val settings = settingsRepository.current()
        if (!settings.autoDepositSalary || settings.monthlySalaryToman <= 0) return@withContext

        val today = com.braveboy.hesabi.core.date.JalaliDate.fromEpochDay(todayEpochDay)
        val paydayDay = settings.paydayDayOfMonth.coerceIn(1, 31)
        val currentMonth = com.braveboy.hesabi.core.date.MonthPeriod.of(today)
        val lastDueMonth = SalarySchedule.lastDueMonth(today, paydayDay)

        val allTransactions = database.transactionDao().getAll().map { it.toDomain() }
        val depositedMonths = allTransactions.mapNotNull { transaction ->
            val ref = transaction.externalRef ?: return@mapNotNull null
            if (!ref.startsWith(SALARY_REF_PREFIX)) return@mapNotNull null
            com.braveboy.hesabi.core.date.MonthPeriod.fromKey(ref.removePrefix(SALARY_REF_PREFIX))
        }
        // Rows created before the unique key existed still count as a deposit month.
        val legacyMonths = allTransactions
            .filter {
                it.externalRef == null &&
                    it.type == com.braveboy.hesabi.domain.model.TransactionType.INCOME &&
                    it.note.contains("حقوق ماهانه")
            }
            .map { com.braveboy.hesabi.core.date.MonthPeriod.of(it.occurredOn) }
        val lastDepositedMonth = (depositedMonths + legacyMonths).maxByOrNull { it.year * 12 + it.month }

        val dueMonths = SalarySchedule.dueMonths(today, paydayDay, lastDepositedMonth)
        if (dueMonths.isEmpty()) return@withContext

        val targetAccountId = settings.salaryAccountId
            ?: settings.defaultAccountId
            ?: database.accountDao().getAll().firstOrNull()?.id
            ?: return@withContext

        val categories = database.categoryDao().getAll()
        val salaryCategory = categories.firstOrNull { it.kind == "INCOME" && (it.name.contains("حقوق") || it.name.contains("درآمد")) }
            ?: categories.firstOrNull { it.kind == "INCOME" }

        for (month in dueMonths) {
            val ref = SALARY_REF_PREFIX + month.key
            if (transactionRepository.findByExternalRef(ref) != null) continue

            // Backfill the key on salary rows created by older versions.
            val legacy = allTransactions.firstOrNull { transaction ->
                transaction.externalRef == null &&
                    transaction.type == com.braveboy.hesabi.domain.model.TransactionType.INCOME &&
                    transaction.note.contains("حقوق ماهانه") &&
                    com.braveboy.hesabi.core.date.MonthPeriod.of(transaction.occurredOn) == month
            }
            if (legacy != null) {
                transactionRepository.save(legacy.copy(externalRef = ref))
                continue
            }

            val dueEpochDay = com.braveboy.hesabi.core.date.JalaliDate(
                month.year,
                month.month,
                paydayDay.coerceAtMost(month.days),
            ).toEpochDay()
            val transaction = com.braveboy.hesabi.domain.model.Transaction(
                id = java.util.UUID.randomUUID().toString(),
                type = com.braveboy.hesabi.domain.model.TransactionType.INCOME,
                amountToman = settings.monthlySalaryToman,
                accountId = targetAccountId,
                categoryId = salaryCategory?.id,
                occurredOnEpochDay = dueEpochDay,
                note = "حقوق ماهانه (ثبت خودکار)",
                source = com.braveboy.hesabi.domain.model.TransactionSource.MANUAL,
                externalRef = ref,
                createdAtMillis = System.currentTimeMillis(),
            )
            transactionRepository.save(transaction)
        }
    }

    /**
     * Deletes recycle-bin rows older than 30 days, their encrypted receipts, and any
     * orphan image left behind by an interrupted save.
     */
    suspend fun purgeExpiredTrash() = withContext(Dispatchers.IO) {
        runCatching {
            val cutoff = System.currentTimeMillis() - TRASH_RETENTION_MILLIS
            val expired = database.transactionDao().getAllIncludingDeleted()
                .filter { it.deletedAtMillis != null && it.deletedAtMillis < cutoff }
            expired.forEach { ReceiptStorageManager.deleteReceiptFile(it.receiptImagePath) }
            transactionRepository.purgeTrash(cutoff)

            val referenced = database.transactionDao().getAllIncludingDeleted()
                .mapNotNull { it.receiptImagePath }
                .toSet()
            ReceiptStorageManager.deleteOrphaned(appContext, referenced)
        }
    }

    /** Removes every user row, then restores the minimal defaults. Settings are reset too. */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.recurringTransactionDao().deleteAll()
            database.installmentDao().deleteAll()
            database.loanDao().deleteAll()
            database.chequeDao().deleteAll()
            database.debtDao().deleteAll()
            database.personDao().deleteAll()
            database.transactionDao().deleteAll()
            database.budgetDao().deleteAll()
            database.savingsGoalDao().deleteAll()
            database.smsDao().deleteAll()
            database.senderRuleDao().deleteAll()
            database.quickTemplateDao().deleteAll()
            database.assetPriceSnapshotDao().deleteAll()
            database.assetDao().deleteAll()
            database.savingsDepositDao().deleteAll()
            database.appEventDao().deleteAll()
            database.categoryDao().deleteAll()
            database.accountDao().deleteAll()
        }
        settingsRepository.setOnboarded(false)
        settingsRepository.setDefaultAccountId(null)
        settingsRepository.setLastBackupAt(0L)
        ensureSeeded()
    }
}

private const val TRASH_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
private const val SALARY_REF_PREFIX = "salary:"

private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `savings_goals` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `title` TEXT NOT NULL,
                `targetAmountToman` INTEGER NOT NULL,
                `savedAmountToman` INTEGER NOT NULL,
                `colorArgb` INTEGER NOT NULL,
                `iconKey` TEXT NOT NULL,
                `targetDateEpochDay` INTEGER,
                `isCompleted` INTEGER NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goals_isCompleted` ON `savings_goals` (`isCompleted`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goals_createdAtMillis` ON `savings_goals` (`createdAtMillis`)")
    }
}

private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `people` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `phoneNumber` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_people_name` ON `people` (`name`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `debts` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `personId` TEXT NOT NULL,
                `amountToman` INTEGER NOT NULL,
                `settledAmountToman` INTEGER NOT NULL,
                `direction` TEXT NOT NULL,
                `dueEpochDay` INTEGER,
                `note` TEXT NOT NULL,
                `isSettled` INTEGER NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_personId` ON `debts` (`personId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_isSettled` ON `debts` (`isSettled`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_dueEpochDay` ON `debts` (`dueEpochDay`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cheques` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `sayadNumber` TEXT NOT NULL,
                `bankName` TEXT NOT NULL,
                `amountToman` INTEGER NOT NULL,
                `dueEpochDay` INTEGER NOT NULL,
                `payeeOrDrawerName` TEXT NOT NULL,
                `direction` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cheques_dueEpochDay` ON `cheques` (`dueEpochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cheques_status` ON `cheques` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cheques_direction` ON `cheques` (`direction`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `loans` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `title` TEXT NOT NULL,
                `totalAmountToman` INTEGER NOT NULL,
                `installmentCount` INTEGER NOT NULL,
                `installmentAmountToman` INTEGER NOT NULL,
                `accountId` TEXT,
                `firstDueEpochDay` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `isCompleted` INTEGER NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loans_isCompleted` ON `loans` (`isCompleted`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loans_createdAtMillis` ON `loans` (`createdAtMillis`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `installments` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `loanId` TEXT NOT NULL,
                `installmentNumber` INTEGER NOT NULL,
                `amountToman` INTEGER NOT NULL,
                `dueEpochDay` INTEGER NOT NULL,
                `isPaid` INTEGER NOT NULL,
                `paidAtEpochDay` INTEGER
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installments_loanId` ON `installments` (`loanId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installments_dueEpochDay` ON `installments` (`dueEpochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installments_isPaid` ON `installments` (`isPaid`)")
    }
}

private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `receiptImagePath` TEXT")
    }
}

private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `title` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `amountToman` INTEGER NOT NULL,
                `accountId` TEXT NOT NULL,
                `toAccountId` TEXT,
                `categoryId` TEXT,
                `frequency` TEXT NOT NULL,
                `interval` INTEGER NOT NULL,
                `dayOfMonth` INTEGER,
                `nextOccurrenceEpochDay` INTEGER NOT NULL,
                `lastAppliedEpochDay` INTEGER,
                `autoApply` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_isActive` ON `recurring_transactions` (`isActive`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_nextOccurrenceEpochDay` ON `recurring_transactions` (`nextOccurrenceEpochDay`)")
    }
}


private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Recycle bin, commitment links and quick entries.
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `deletedAtMillis` INTEGER")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `linkedEntityType` TEXT")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `linkedEntityId` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_deletedAtMillis` ON `transactions` (`deletedAtMillis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_linkedEntityId` ON `transactions` (`linkedEntityId`)")

        db.execSQL("ALTER TABLE `budgets` ADD COLUMN `rolloverEnabled` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `monthlyTargetToman` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `quick_templates` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `label` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `amountToman` INTEGER NOT NULL,
                `categoryId` TEXT,
                `accountId` TEXT,
                `toAccountId` TEXT,
                `note` TEXT NOT NULL,
                `usageCount` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_quick_templates_type` ON `quick_templates` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_quick_templates_sortOrder` ON `quick_templates` (`sortOrder`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `assets` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `quantityMilli` INTEGER NOT NULL,
                `unitLabel` TEXT NOT NULL,
                `buyPriceTomanPerUnit` INTEGER NOT NULL,
                `currentPriceTomanPerUnit` INTEGER NOT NULL,
                `priceUpdatedAtMillis` INTEGER NOT NULL,
                `priceSourceLabel` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `archived` INTEGER NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_kind` ON `assets` (`kind`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_archived` ON `assets` (`archived`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_createdAtMillis` ON `assets` (`createdAtMillis`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `asset_price_snapshots` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `assetId` TEXT NOT NULL,
                `priceTomanPerUnit` INTEGER NOT NULL,
                `recordedAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_price_snapshots_assetId` ON `asset_price_snapshots` (`assetId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_price_snapshots_recordedAtMillis` ON `asset_price_snapshots` (`recordedAtMillis`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `app_events` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `type` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `metadata` TEXT NOT NULL,
                `occurredAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_events_type` ON `app_events` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_events_occurredAtMillis` ON `app_events` (`occurredAtMillis`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `savings_deposits` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `goalId` TEXT NOT NULL,
                `amountToman` INTEGER NOT NULL,
                `isWithdrawal` INTEGER NOT NULL,
                `occurredOnEpochDay` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_deposits_goalId` ON `savings_deposits` (`goalId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_deposits_occurredOnEpochDay` ON `savings_deposits` (`occurredOnEpochDay`)")
    }
}
