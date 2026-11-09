package com.braveboy.hesabi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.braveboy.hesabi.data.local.dao.AccountDao
import com.braveboy.hesabi.data.local.dao.AppEventDao
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

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SmsEventEntity::class,
        SenderRuleEntity::class,
        SavingsGoalEntity::class,
        PersonEntity::class,
        DebtEntity::class,
        ChequeEntity::class,
        LoanEntity::class,
        InstallmentEntity::class,
        RecurringTransactionEntity::class,
        QuickTemplateEntity::class,
        AssetEntity::class,
        AssetPriceSnapshotEntity::class,
        AppEventEntity::class,
        SavingsDepositEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class HesabiDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun smsDao(): SmsDao
    abstract fun senderRuleDao(): SenderRuleDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun personDao(): PersonDao
    abstract fun debtDao(): DebtDao
    abstract fun chequeDao(): ChequeDao
    abstract fun loanDao(): LoanDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun quickTemplateDao(): QuickTemplateDao
    abstract fun assetDao(): AssetDao
    abstract fun assetPriceSnapshotDao(): AssetPriceSnapshotDao
    abstract fun appEventDao(): AppEventDao
    abstract fun savingsDepositDao(): SavingsDepositDao

    companion object {
        const val NAME = "hesabi.db"
    }
}
