package com.braveboy.hesabi.data.local

/**
 * Explicit migrations 1..6. Kept in their own file so the instrumentation test
 * suite can reuse the exact same list the app runs at startup instead of a copy.
 */
object HesabiMigrations {
    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
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

val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
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

val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `receiptImagePath` TEXT")
    }
}

val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
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


val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
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
