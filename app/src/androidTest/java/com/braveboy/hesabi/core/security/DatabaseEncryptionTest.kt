package com.braveboy.hesabi.core.security

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.braveboy.hesabi.data.local.HesabiDatabase
import com.braveboy.hesabi.data.local.entity.AccountEntity
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Proves H3 on a real device: an existing plaintext database is exported into a
 * SQLCipher file, the rows survive, the file no longer looks like SQLite, and a
 * wrong passphrase cannot read it.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseEncryptionTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "encryption-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    private fun seedPlaintextDatabase() {
        context.deleteDatabase(name)
        val plain = Room.databaseBuilder(context, HesabiDatabase::class.java, name)
            .allowMainThreadQueries()
            .build()
        plain.openHelper.writableDatabase
        runBlocking {
            plain.accountDao().upsert(
                AccountEntity(
                    id = "acc-secure",
                    name = "حساب رمزشده",
                    kind = "CASH",
                    colorArgb = 0,
                    openingBalanceToman = 2_500_000,
                    archived = false,
                    sortOrder = 0,
                ),
            )
        }
        plain.close()
    }

    @Test
    fun plaintextDatabaseIsMigratedAndReadable() {
        seedPlaintextDatabase()
        val file = context.getDatabasePath(name)
        assertTrue(
            "fixture is not plaintext: exists=${file.exists()} size=${file.length()} " +
                "header=${file.takeIf { it.exists() }?.readBytes()?.take(16)?.toList()}",
            EncryptedDatabaseMigrator.isPlaintext(file),
        )

        val passphrase = "test-" + UUID.randomUUID()
        assertTrue(EncryptedDatabaseMigrator.prepare(context, name, passphrase))

        assertFalse("database must no longer be plaintext", EncryptedDatabaseMigrator.isPlaintext(file))
        assertTrue(DatabaseKeyManager.isEncryptedDatabase(context, name))
        assertFalse("temporary plaintext copy must be deleted", File(file.parentFile, "$name.plain-migrating").exists())

        val encrypted = Room.databaseBuilder(context, HesabiDatabase::class.java, name)
            .allowMainThreadQueries()
            .openHelperFactory(SupportOpenHelperFactory(passphrase.toByteArray()))
            .build()
        val accounts = runBlocking { encrypted.accountDao().getAll() }
        assertEquals(1, accounts.size)
        assertEquals("حساب رمزشده", accounts.single().name)
        assertEquals(2_500_000L, accounts.single().openingBalanceToman)
        encrypted.close()
    }

    @Test
    fun wrongPassphraseCannotReadTheDatabase() {
        seedPlaintextDatabase()
        val passphrase = "correct-" + UUID.randomUUID()
        assertTrue(EncryptedDatabaseMigrator.prepare(context, name, passphrase))
        val file = context.getDatabasePath(name)

        val wrong = runCatching {
            System.loadLibrary("sqlcipher")
            val db = SQLiteDatabase.openDatabase(
                file.absolutePath,
                "definitely-wrong",
                null,
                SQLiteDatabase.OPEN_READONLY,
                null,
            )
            db.rawQuery("SELECT count(*) FROM accounts", null).use { it.moveToFirst() }
        }

        assertTrue("opening with a wrong key must fail", wrong.isFailure)
    }

    @Test
    fun passphraseIsStableAcrossCalls() {
        val first = DatabaseKeyManager.passphrase(context)
        val second = DatabaseKeyManager.passphrase(context)

        assertEquals(first, second)
        assertTrue(first.length >= 32)
    }

    @Test
    fun migrationIsIdempotentOnAnAlreadyEncryptedFile() {
        seedPlaintextDatabase()
        val passphrase = "once-" + UUID.randomUUID()
        assertTrue(EncryptedDatabaseMigrator.prepare(context, name, passphrase))

        // Second launch must not touch an encrypted file.
        assertFalse(EncryptedDatabaseMigrator.prepare(context, name, passphrase))
        assertFalse(EncryptedDatabaseMigrator.isPlaintext(context.getDatabasePath(name)))
    }
}
