package com.braveboy.hesabi.core.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.braveboy.hesabi.core.security.FileCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Receipt attachments are encrypted at rest with a Keystore key ([FileCrypto]) and
 * removed when the transaction is hard-deleted. Old plaintext `.webp` files created
 * by earlier versions are still readable and get encrypted again on the next save.
 */
object ReceiptStorageManager {

    private const val MAX_DIMENSION = 1600
    private const val WEBP_QUALITY = 82
    private const val PREVIEW_DIMENSION = 900

    suspend fun saveReceiptFromUri(context: Context, sourceUri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val original = decodeBounded(context, sourceUri, MAX_DIMENSION) ?: return@runCatching null
                val rotated = fixOrientation(context, sourceUri, original)
                val scaled = scaleDownIfNeeded(rotated)
                val compressed = ByteArrayOutputStream().use { stream ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, stream)
                    } else {
                        @Suppress("DEPRECATION")
                        scaled.compress(Bitmap.CompressFormat.WEBP, WEBP_QUALITY, stream)
                    }
                    stream.toByteArray()
                }
                writeEncrypted(context, compressed)
            }.getOrNull()
        }

    /** Used by the backup restore path: bytes are already a compressed image. */
    suspend fun saveReceiptBytes(context: Context, bytes: ByteArray): String? =
        withContext(Dispatchers.IO) {
            runCatching { writeEncrypted(context, bytes) }.getOrNull()
        }

    suspend fun deleteReceiptFile(path: String?) = withContext(Dispatchers.IO) {
        if (!path.isNullOrBlank()) {
            runCatching {
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
    }

    /** Returns decrypted image bytes for backup. */
    suspend fun readDecrypted(path: String?): ByteArray? = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext null
        runCatching {
            val file = File(path)
            if (!file.exists()) return@runCatching null
            val raw = file.readBytes()
            if (FileCrypto.isEncrypted(raw)) FileCrypto.decryptOrNull(raw) else raw
        }.getOrNull()
    }

    fun loadBitmap(path: String?): ImageBitmap? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val file = File(path)
            if (!file.exists()) return null
            val raw = file.readBytes()
            val bytes = if (FileCrypto.isEncrypted(raw)) FileCrypto.decryptOrNull(raw) ?: return null else raw
            decodeBounded(bytes, PREVIEW_DIMENSION)?.asImageBitmap()
        }.getOrNull()
    }

    /**
     * Full-resolution, orientation-corrected bitmap for on-device OCR. Kept separate
     * from [loadBitmap] because text recognition needs more pixels than a preview.
     */
    fun loadBitmapForOcr(context: Context, uri: Uri): Bitmap? {
        val original = decodeBounded(context, uri, MAX_DIMENSION) ?: return null
        return runCatching { fixOrientation(context, uri, original) }.getOrDefault(original)
    }

    /** Deletes files under `filesDir/receipts` that no live transaction references. */
    suspend fun deleteOrphaned(context: Context, referencedPaths: Set<String>): Int =
        withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "receipts")
            if (!directory.exists()) return@withContext 0
            var removed = 0
            directory.listFiles()?.forEach { file ->
                if (file.isFile && file.absolutePath !in referencedPaths) {
                    if (file.delete()) removed++
                }
            }
            removed
        }

    private fun writeEncrypted(context: Context, compressed: ByteArray): String {
        val receiptsDir = File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }
        val outputFile = File(receiptsDir, "${UUID.randomUUID()}.hsr")
        val encrypted = FileCrypto.encrypt(compressed)
        FileOutputStream(outputFile).use { it.write(encrypted) }
        return outputFile.absolutePath
    }

    private fun decodeBounded(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = calculateSample(bounds.outWidth, bounds.outHeight, maxDimension)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun decodeBounded(bytes: ByteArray, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sample = calculateSample(bounds.outWidth, bounds.outHeight, maxDimension)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun calculateSample(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth / 2 >= maxDimension || currentHeight / 2 >= maxDimension) {
            sample *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return sample
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = MAX_DIMENSION
            targetHeight = (MAX_DIMENSION / ratio).toInt()
        } else {
            targetHeight = MAX_DIMENSION
            targetWidth = (MAX_DIMENSION * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
