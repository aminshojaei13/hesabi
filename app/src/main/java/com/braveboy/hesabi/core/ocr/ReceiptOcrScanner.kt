package com.braveboy.hesabi.core.ocr

import android.content.Context
import android.net.Uri
import com.braveboy.hesabi.core.receipt.ReceiptStorageManager
import com.braveboy.hesabi.domain.logic.ReceiptOcrParser
import com.braveboy.hesabi.domain.logic.ReceiptOcrResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device receipt reader. The bundled ML Kit model is used (not the
 * Play-Services variant) because the target market has many devices without
 * Google Play, and receipt images must never leave the phone (3.4).
 */
object ReceiptOcrScanner {

    private val recognizer: TextRecognizer by lazy {
        // Persian receipts mix Latin digits and brand names, so the Latin model is
        // the most predictable one for numbers; the parser handles Persian digits.
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /** Returns an empty result instead of throwing when the image cannot be read. */
    suspend fun scan(context: Context, uri: Uri): ReceiptOcrResult = withContext(Dispatchers.IO) {
        val bitmap = runCatching { ReceiptStorageManager.loadBitmapForOcr(context, uri) }
            .getOrNull()
            ?: return@withContext ReceiptOcrResult()
        val text = runCatching { recognize(bitmap) }.getOrDefault("")
        ReceiptOcrParser.parse(text)
    }

    private suspend fun recognize(bitmap: android.graphics.Bitmap): String =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) continuation.resume(result.text)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
                .addOnCanceledListener {
                    if (continuation.isActive) continuation.resume("")
                }
        }
}
