package com.braveboy.hesabi.core.security

import android.content.Context
import android.util.Log
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.FileInputStream

/**
 * One-time upgrade of the database created by older versions (plain SQLite) to a
 * SQLCipher-encrypted file, in place and without losing rows (H3).
 *
 * SQLCipher attaches the old file, runs `sqlcipher_export` into the encrypted file
 * and only then removes the plaintext copy. A failed migration restores the original
 * file, so the worst case is "still unencrypted", never "data gone".
 */
object EncryptedDatabaseMigrator {

    private const val TAG = "EncryptedDbMigrator"
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    private fun ensureLibrary() {
        if (!loaded) {
            System.loadLibrary("sqlcipher")
            loaded = true
        }
    }

    @Volatile
    private var loaded = false

    fun prepare(context: Context, dbName: String, passphrase: String): Boolean {
        ensureLibrary()
        val databaseFile = context.getDatabasePath(dbName)
        if (!databaseFile.exists() || databaseFile.length() == 0L) return false
        if (!isPlaintext(databaseFile)) return false

        val legacyFile = File(databaseFile.parentFile, "$dbName.plain-migrating")
        val wal = File(databaseFile.parentFile, "$dbName-wal")
        val walLegacy = File(databaseFile.parentFile, "$dbName.plain-migrating-wal")
        val shm = File(databaseFile.parentFile, "$dbName-shm")
        val shmLegacy = File(databaseFile.parentFile, "$dbName.plain-migrating-shm")

        val movedMain = databaseFile.renameTo(legacyFile)
        if (!movedMain) return false
        // WAL/SHM belong to the same base name, otherwise uncheckpointed rows are lost.
        if (wal.exists()) wal.renameTo(walLegacy)
        if (shm.exists()) shm.renameTo(shmLegacy)

        var encrypted: SQLiteDatabase? = null
        return try {
            // Documented SQLCipher path: the destination (encrypted) database is the
            // main connection and the old plaintext file is attached with an empty key.
            val attachedPath = legacyFile.absolutePath.replace("'", "''")
            encrypted = SQLiteDatabase.openDatabase(
                databaseFile.absolutePath,
                passphrase,
                null,
                SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.OPEN_READWRITE,
                null,
            )
            encrypted.rawExecSQL("ATTACH DATABASE '$attachedPath' AS plaintext KEY ''")
            encrypted.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
            encrypted.rawExecSQL("DETACH DATABASE plaintext")
            encrypted.close()
            encrypted = null

            if (!verify(databaseFile, passphrase)) {
                Log.e(TAG, "verification failed: size=${databaseFile.length()} plaintext=${isPlaintext(databaseFile)}")
                error("encrypted database did not pass verification")
            }
            deleteQuietly(legacyFile)
            deleteQuietly(walLegacy)
            deleteQuietly(shmLegacy)
            true
        } catch (error: Throwable) {
            Log.e(TAG, "database encryption migration failed", error)
            runCatching { encrypted?.close() }
            deleteQuietly(databaseFile)
            deleteQuietly(File(databaseFile.parentFile, "$dbName-wal"))
            deleteQuietly(File(databaseFile.parentFile, "$dbName-shm"))
            legacyFile.renameTo(databaseFile)
            if (walLegacy.exists()) walLegacy.renameTo(wal)
            if (shmLegacy.exists()) shmLegacy.renameTo(shm)
            false
        }
    }

    /** SQLite files start with a known magic string; SQLCipher output looks random. */
    fun isPlaintext(file: File): Boolean {
        if (!file.exists() || file.length() < SQLITE_HEADER.size) return false
        return runCatching {
            val header = ByteArray(SQLITE_HEADER.size)
            FileInputStream(file).use { it.read(header) }
            header.contentEquals(SQLITE_HEADER)
        }.getOrDefault(false)
    }

    private fun verify(file: File, passphrase: String): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        if (isPlaintext(file)) return false
        return runCatching {
            val db = SQLiteDatabase.openDatabase(
                file.absolutePath,
                passphrase,
                null,
                SQLiteDatabase.OPEN_READONLY,
                null,
            )
            db.use {
                it.rawQuery("SELECT count(*) FROM sqlite_master", null).use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(0) > 0
                }
            }
        }.getOrDefault(false)
    }

    private fun deleteQuietly(file: File) {
        runCatching { if (file.exists()) file.delete() }
    }
}
