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
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration()
        .build()

    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(appContext)
    val accountRepository: AccountRepository = AccountRepositoryImpl(database.accountDao(), database.transactionDao())
    val categoryRepository: CategoryRepository = CategoryRepositoryImpl(database.categoryDao())
    val transactionRepository: TransactionRepository = TransactionRepositoryImpl(database.transactionDao())
    val budgetRepository: BudgetRepository = BudgetRepositoryImpl(database.budgetDao())
    val savingsGoalRepository: SavingsGoalRepository = SavingsGoalRepositoryImpl(database.savingsGoalDao())
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
