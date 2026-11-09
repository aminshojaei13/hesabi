package com.braveboy.hesabi.core.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

/**
 * Owns the SQLCipher passphrase for the local database (H3).
 *
 * A random 256-bit passphrase is generated once per install, wrapped with the
 * non-exportable Keystore key used by [FieldCrypto] and stored in app-private
 * preferences. The Keystore key cannot leave the phone, and the wrapped value is
 * useless without it — a copied `hesabi.db` cannot be opened on another device.
 */
object DatabaseKeyManager {

    private const val PREFS = "hesabi_db_security"
    private const val KEY_WRAPPED = "wrapped_db_passphrase"
    private const val KEY_BYTES = 32

    fun passphrase(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_WRAPPED, null)
        if (stored != null) {
            val opened = FieldCrypto.open(stored)
            if (opened.length >= 32) return opened
        }
        val random = ByteArray(KEY_BYTES).also { SecureRandom().nextBytes(it) }
        val fresh = Base64.encodeToString(random, Base64.NO_WRAP)
        prefs.edit().putString(KEY_WRAPPED, FieldCrypto.seal(fresh)).apply()
        return fresh
    }

    /**
     * Returns true only if [context] holds a database that was created by
     * [passphrase]. Used by the "data at rest" check in Settings/diagnostics.
     */
    fun isEncryptedDatabase(context: Context, name: String): Boolean {
        val file = context.getDatabasePath(name)
        if (!file.exists() || file.length() == 0L) return false
        return !EncryptedDatabaseMigrator.isPlaintext(file)
    }
}
