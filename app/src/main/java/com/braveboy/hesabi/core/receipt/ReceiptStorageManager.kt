package com.braveboy.hesabi.core.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ReceiptStorageManager {

    private const val MAX_DIMENSION = 1600
    private const val WEBP_QUALITY = 85

    suspend fun saveReceiptFromUri(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val receiptsDir = File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }
            val outputFile = File(receiptsDir, "${UUID.randomUUID()}.webp")

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@use null

                val rotatedBitmap = fixOrientation(context, sourceUri, originalBitmap)
                val scaledBitmap = scaleDownIfNeeded(rotatedBitmap)

                FileOutputStream(outputFile).use { outStream ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, outStream)
                    } else {
                        @Suppress("DEPRECATION")
                        scaledBitmap.compress(Bitmap.CompressFormat.WEBP, WEBP_QUALITY, outStream)
                    }
                }
                outputFile.absolutePath
            }
        }.getOrNull()
    }

    suspend fun deleteReceiptFile(path: String?) = withContext(Dispatchers.IO) {
        if (!path.isNullOrBlank()) {
            runCatching {
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
    }

    fun loadBitmap(path: String?): ImageBitmap? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } else null
        }.getOrNull()
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
