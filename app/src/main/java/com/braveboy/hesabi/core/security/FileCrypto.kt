package com.braveboy.hesabi.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption for files at rest, keyed by a non-exportable Android
 * Keystore entry. Used for receipt images and any future local attachment.
 *
 * File layout: `HSF1` magic + 12-byte IV + ciphertext. The magic lets the reader
 * recognise files written by older, unencrypted versions and migrate them lazily.
 */
object FileCrypto {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "hesabi_local_file_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private val MAGIC = byteArrayOf('H'.code.toByte(), 'S'.code.toByte(), 'F'.code.toByte(), '1'.code.toByte())

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size > MAGIC.size + IV_BYTES && MAGIC.indices.all { bytes[it] == MAGIC[it] }

    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        return MAGIC + iv + ciphertext
    }

    /** Returns null when the file is corrupt or the Keystore key was lost. */
    fun decryptOrNull(bytes: ByteArray): ByteArray? {
        if (!isEncrypted(bytes)) return null
        return runCatching {
            val iv = bytes.copyOfRange(MAGIC.size, MAGIC.size + IV_BYTES)
            val ciphertext = bytes.copyOfRange(MAGIC.size + IV_BYTES, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            cipher.doFinal(ciphertext)
        }.getOrNull()
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
