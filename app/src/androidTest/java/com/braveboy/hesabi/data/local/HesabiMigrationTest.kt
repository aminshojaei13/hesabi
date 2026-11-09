package com.braveboy.hesabi.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real migration list from [HesabiMigrations] against the exported schemas.
 * Every version bump must keep old rows and produce exactly the schema Room expects.
 */
@RunWith(AndroidJUnit4::class)
class HesabiMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HesabiDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private fun createV1WithAccount(name: String): String {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO accounts (id, name, kind, colorArgb, openingBalanceToman, archived, sortOrder) " +
                    "VALUES ('acc-1', '$name', 'CASH', 0, 1250000, 0, 0)",
            )
            close()
        }
        return TEST_DB
    }

    @Test
    fun migrate1To6_keepsExistingData() {
        createV1WithAccount("نقد")

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, *HesabiMigrations.ALL)
        db.query("SELECT name, openingBalanceToman FROM accounts WHERE id = 'acc-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("نقد", cursor.getString(0))
            assertEquals(1_250_000L, cursor.getLong(1))
        }
        db.close()
    }

    @Test
    fun migrate1To6_createsEveryNewTable() {
        createV1WithAccount("بانک")

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, *HesabiMigrations.ALL)
        val expectedTables = listOf(
            "savings_goals", "people", "debts", "cheques", "loans", "installments",
            "recurring_transactions", "quick_templates", "assets", "asset_price_snapshots",
            "budgets", "savings_deposits", "app_events", "sender_rules",
        )
        for (table in expectedTables) {
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table))
                .use { cursor -> assertTrue("table $table is missing", cursor.moveToFirst()) }
        }
        db.close()
    }

    @Test
    fun migrate3To4_addsReceiptColumn() {
        helper.createDatabase("step-3-4", 3).close()

        val db = helper.runMigrationsAndValidate("step-3-4", 4, true, MIGRATION_3_4)
        db.query("PRAGMA table_info(transactions)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == "receiptImagePath") found = true
            }
            assertTrue("receiptImagePath column is missing", found)
        }
        db.close()
    }

    @Test
    fun migrate4To5_addsRecurringTable() {
        helper.createDatabase("step-4-5", 4).close()

        val db = helper.runMigrationsAndValidate("step-4-5", 5, true, MIGRATION_4_5)
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'recurring_transactions'")
            .use { cursor -> assertTrue(cursor.moveToFirst()) }
        db.close()
    }

    @Test
    fun migrate5To6_addsTrashColumns() {
        helper.createDatabase("step-5-6", 5).close()

        val db = helper.runMigrationsAndValidate("step-5-6", 6, true, MIGRATION_5_6)
        val columns = buildSet {
            db.query("PRAGMA table_info(transactions)").use { cursor ->
                while (cursor.moveToNext()) add(cursor.getString(1))
            }
        }
        assertTrue("deletedAtMillis is missing", "deletedAtMillis" in columns)
        assertTrue("linkedEntityType is missing", "linkedEntityType" in columns)
        assertTrue("linkedEntityId is missing", "linkedEntityId" in columns)
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-full-test"
    }
}
