package com.braveboy.hesabi.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keystore-backed encryption for sensitive free-text columns (notes, names, SMS bodies,
 * goal titles). Amounts stay numeric because the ledger sums them in SQL; the text around
 * them is what leaks the story, and that is what this layer protects.
 *
 * Format: `fc1:` + Base64(IV + AES-GCM ciphertext). Values written by older versions have
 * no prefix and are returned unchanged, so the migration is lazy and never blocks a read.
 * If the Keystore is unavailable (previews, JVM tests), the helpers degrade to plaintext
 * instead of crashing the app.
 */
object FieldCrypto {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "hesabi_field_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX = "fc1:"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun seal(plain: String?): String {
        if (plain.isNullOrEmpty()) return plain.orEmpty()
        if (plain.startsWith(PREFIX)) return plain
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
        }.getOrDefault(plain)
    }

    fun open(stored: String?): String {
        if (stored.isNullOrEmpty() || !stored.startsWith(PREFIX)) return stored.orEmpty()
        return runCatching {
            val bytes = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            if (bytes.size <= IV_BYTES) return stored
            val iv = bytes.copyOfRange(0, IV_BYTES)
            val ciphertext = bytes.copyOfRange(IV_BYTES, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrDefault(stored)
    }

    @Volatile
    private var cachedKey: SecretKey? = null

    private fun secretKey(): SecretKey {
        cachedKey?.let { return it }
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existing != null) {
            cachedKey = existing
            return existing
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey().also { cachedKey = it }
    }
}
