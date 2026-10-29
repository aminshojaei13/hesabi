package com.braveboy.hesabi.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Password-based, authenticated encryption for local backup files.
 * Format: MAGIC(4) | VERSION(1) | saltLen(1) | salt | ivLen(1) | iv | AES-GCM(ciphertext)
 * The full header is used as AAD so tampering with the version or salt is detected.
 */
object BackupCrypto {

    private val MAGIC = byteArrayOf('H'.code.toByte(), 'S'.code.toByte(), 'B'.code.toByte(), 'I'.code.toByte())
    private const val VERSION: Byte = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val header = buildHeader(salt, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header)
        val ciphertext = cipher.doFinal(plain)
        return header + ciphertext
    }

    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        if (data.size < 7) throw BackupException("فایل پشتیبان معتبر نیست.")
        if (!data.copyOfRange(0, 4).contentEquals(MAGIC)) {
            throw BackupException("این فایل، پشتیبان حسابی نیست.")
        }
        val version = data[4]
        if (version != VERSION) throw BackupException("نسخهٔ فایل پشتیبان پشتیبانی نمی‌شود.")
        val saltLength = data[5].toInt() and 0xFF
        if (saltLength != SALT_BYTES) throw BackupException("ساختار فایل پشتیبان خراب است.")
        val saltStart = 6
        val saltEnd = saltStart + saltLength
        if (data.size <= saltEnd) throw BackupException("ساختار فایل پشتیبان خراب است.")
        val salt = data.copyOfRange(saltStart, saltEnd)
        val ivLength = data[saltEnd].toInt() and 0xFF
        if (ivLength != IV_BYTES) throw BackupException("ساختار فایل پشتیبان خراب است.")
        val ivStart = saltEnd + 1
        val ivEnd = ivStart + ivLength
        if (data.size <= ivEnd) throw BackupException("ساختار فایل پشتیبان خراب است.")
        val iv = data.copyOfRange(ivStart, ivEnd)
        val header = data.copyOfRange(0, ivEnd)
        val ciphertext = data.copyOfRange(ivEnd, data.size)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(header)
            cipher.doFinal(ciphertext)
        } catch (error: Exception) {
            throw BackupException("رمز اشتباه است یا فایل دستکاری شده.", error)
        }
    }

    private fun buildHeader(salt: ByteArray, iv: ByteArray): ByteArray {
        val header = ByteArray(4 + 1 + 1 + salt.size + 1 + iv.size)
        MAGIC.copyInto(header, 0)
        header[4] = VERSION
        header[5] = salt.size.toByte()
        salt.copyInto(header, 6)
        header[6 + salt.size] = iv.size.toByte()
        iv.copyInto(header, 6 + salt.size + 1)
        return header
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(key, "AES")
    }
}
