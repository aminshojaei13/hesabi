package com.braveboy.hesabi.di

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.braveboy.hesabi.BuildConfig
import com.braveboy.hesabi.data.backup.BackupManager
import com.braveboy.hesabi.data.local.HesabiDatabase
import com.braveboy.hesabi.data.repository.AccountRepositoryImpl
import com.braveboy.hesabi.data.repository.BudgetRepositoryImpl
import com.braveboy.hesabi.data.repository.CategoryRepositoryImpl
import com.braveboy.hesabi.data.repository.ChequeRepositoryImpl
import com.braveboy.hesabi.data.repository.DebtRepositoryImpl
import com.braveboy.hesabi.data.repository.LoanRepositoryImpl
import com.braveboy.hesabi.data.repository.PersonRepositoryImpl
import com.braveboy.hesabi.data.repository.SavingsGoalRepositoryImpl
import com.braveboy.hesabi.data.repository.SettingsRepositoryImpl
import com.braveboy.hesabi.data.repository.SmsRepositoryImpl
import com.braveboy.hesabi.data.repository.TransactionRepositoryImpl
import com.braveboy.hesabi.data.seed.SeedData
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.BackupRepository
import com.braveboy.hesabi.domain.repository.BudgetRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.ChequeRepository
import com.braveboy.hesabi.domain.repository.DebtRepository
import com.braveboy.hesabi.domain.repository.LoanRepository
import com.braveboy.hesabi.domain.repository.PersonRepository
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
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .fallbackToDestructiveMigration(dropAllTables = false)
        .build()

    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(appContext)
    val accountRepository: AccountRepository = AccountRepositoryImpl(database.accountDao(), database.transactionDao())
    val categoryRepository: CategoryRepository = CategoryRepositoryImpl(database.categoryDao())
    val transactionRepository: TransactionRepository = TransactionRepositoryImpl(database.transactionDao())
    val budgetRepository: BudgetRepository = BudgetRepositoryImpl(database.budgetDao())
    val savingsGoalRepository: SavingsGoalRepository = SavingsGoalRepositoryImpl(database.savingsGoalDao())
    val personRepository: PersonRepository = PersonRepositoryImpl(database.personDao(), database.debtDao())
    val debtRepository: DebtRepository = DebtRepositoryImpl(database.debtDao())
    val chequeRepository: ChequeRepository = ChequeRepositoryImpl(database.chequeDao())
    val loanRepository: LoanRepository = LoanRepositoryImpl(database, database.loanDao(), database.installmentDao())
    val smsRepository: SmsRepository = SmsRepositoryImpl(
        db = database,
        smsDao = database.smsDao(),
        senderRuleDao = database.senderRuleDao(),
        transactionDao = database.transactionDao(),
        settingsRepository = settingsRepository,
    )
    val backupRepository: BackupRepository = BackupManager(
        db = database,
        accountDao = database.accountDao(),
        categoryDao = database.categoryDao(),
        transactionDao = database.transactionDao(),
        budgetDao = database.budgetDao(),
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
        if (settingsRepository.current().defaultAccountId == null) {
            val first = database.accountDao().getAll().firstOrNull()
            if (first != null) settingsRepository.setDefaultAccountId(first.id)
        }
    }

    /** Removes every user row, then restores the minimal defaults. Settings are reset too. */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.withTransaction {
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
            database.categoryDao().deleteAll()
            database.accountDao().deleteAll()
        }
        settingsRepository.setOnboarded(false)
        settingsRepository.setDefaultAccountId(null)
        settingsRepository.setLastBackupAt(0L)
        ensureSeeded()
    }
}

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
