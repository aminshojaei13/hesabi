package com.braveboy.hesabi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.braveboy.hesabi.data.local.dao.AccountDao
import com.braveboy.hesabi.data.local.dao.BudgetDao
import com.braveboy.hesabi.data.local.dao.CategoryDao
import com.braveboy.hesabi.data.local.dao.ChequeDao
import com.braveboy.hesabi.data.local.dao.DebtDao
import com.braveboy.hesabi.data.local.dao.InstallmentDao
import com.braveboy.hesabi.data.local.dao.LoanDao
import com.braveboy.hesabi.data.local.dao.PersonDao
import com.braveboy.hesabi.data.local.dao.SavingsGoalDao
import com.braveboy.hesabi.data.local.dao.SenderRuleDao
import com.braveboy.hesabi.data.local.dao.SmsDao
import com.braveboy.hesabi.data.local.dao.TransactionDao
import com.braveboy.hesabi.data.local.entity.AccountEntity
import com.braveboy.hesabi.data.local.entity.BudgetEntity
import com.braveboy.hesabi.data.local.entity.CategoryEntity
import com.braveboy.hesabi.data.local.entity.ChequeEntity
import com.braveboy.hesabi.data.local.entity.DebtEntity
import com.braveboy.hesabi.data.local.entity.InstallmentEntity
import com.braveboy.hesabi.data.local.entity.LoanEntity
import com.braveboy.hesabi.data.local.entity.PersonEntity
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
    ],
    version = 3,
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

    companion object {
        const val NAME = "hesabi.db"
    }
}
