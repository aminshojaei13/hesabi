package com.braveboy.hesabi.core.sync

import com.braveboy.hesabi.data.backup.BackupCrypto
import kotlinx.serialization.json.Json

class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Encrypts and validates sync packages (6.2). The package is a UTF-8 JSON document
 * sealed with the same audited PBKDF2 + AES-256-GCM construction as the local backup,
 * using a passphrase that never leaves the user's devices.
 */
class SyncPackageCodec(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) {

    fun encode(packageValue: SyncPackage, passphrase: CharArray): ByteArray {
        validate(packageValue)
        if (passphrase.isEmpty()) throw SyncException("رمز همگام‌سازی نمی‌تواند خالی باشد.")
        val plain = json.encodeToString(SyncPackage.serializer(), packageValue).toByteArray(Charsets.UTF_8)
        return BackupCrypto.encrypt(plain, passphrase)
    }

    fun decode(bytes: ByteArray, passphrase: CharArray): SyncPackage {
        if (bytes.isEmpty()) throw SyncException("بستهٔ همگام‌سازی خالی است.")
        if (bytes.size > MAX_PACKAGE_BYTES) throw SyncException("بستهٔ همگام‌سازی بیش از حد بزرگ است.")
        if (passphrase.isEmpty()) throw SyncException("رمز همگام‌سازی نمی‌تواند خالی باشد.")
        val plain = try {
            BackupCrypto.decrypt(bytes, passphrase)
        } catch (error: Throwable) {
            throw SyncException("رمز همگام‌سازی درست نیست یا بسته خراب است.", error)
        }
        val packageValue = runCatching {
            json.decodeFromString(SyncPackage.serializer(), plain.toString(Charsets.UTF_8))
        }.getOrElse { throw SyncException("محتوای بستهٔ همگام‌سازی خوانده نشد.", it) }
        if (packageValue.magic != SyncPackage.MAGIC) {
            throw SyncException("این بسته مربوط به حسابی نیست.")
        }
        if (packageValue.schema > SyncPackage.SCHEMA) {
            throw SyncException("این بسته با نسخهٔ جدیدتر حسابی ساخته شده است.")
        }
        validate(packageValue)
        return packageValue
    }

    private fun validate(packageValue: SyncPackage) {
        if (packageValue.records.size > SyncPackage.MAX_RECORDS) {
            throw SyncException("تعداد رکوردهای بسته بیش از حد مجاز است.")
        }
        packageValue.records.forEach { record ->
            if (record.entity.isBlank() || record.id.isBlank()) {
                throw SyncException("رکورد همگام‌سازی شناسه ندارد.")
            }
            if (record.updatedAtMillis < 0) {
                throw SyncException("زمان رکورد همگام‌سازی معتبر نیست.")
            }
        }
    }

    companion object {
        const val MAX_PACKAGE_BYTES = 64 * 1024 * 1024
    }
}
