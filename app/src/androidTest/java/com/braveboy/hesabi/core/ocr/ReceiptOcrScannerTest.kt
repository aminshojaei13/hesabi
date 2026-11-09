package com.braveboy.hesabi.core.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * End-to-end check of 3.4: a rendered receipt image goes through the bundled ML Kit
 * model and the local parser. No network and no receipt photo leaves the device.
 */
@RunWith(AndroidJUnit4::class)
class ReceiptOcrScannerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val image = File(context.cacheDir, "ocr-fixture.png")

    @After
    fun tearDown() {
        if (image.exists()) image.delete()
    }

    private fun renderReceipt() {
        val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 64f
            isAntiAlias = true
        }
        canvas.drawText("Cafe Hesabi", 60f, 140f, paint)
        canvas.drawText("Date 2024-07-02", 60f, 260f, paint)
        canvas.drawText("Coffee 2 x 40,000", 60f, 380f, paint)
        canvas.drawText("TOTAL 125,000", 60f, 520f, paint)
        FileOutputStream(image).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        bitmap.recycle()
    }

    @Test
    fun readsAmountAndMerchantFromAnImage() {
        renderReceipt()

        val result = runBlocking { ReceiptOcrScanner.scan(context, Uri.fromFile(image)) }

        assertNotNull("OCR returned no amount; text was: ${result.rawText}", result.amountToman)
        assertEquals(125_000L, result.amountToman)
        assertEquals("Cafe Hesabi", result.merchant)
    }

    @Test
    fun unreadableImageReturnsAnEmptyResultInsteadOfThrowing() {
        val missing = File(context.cacheDir, "does-not-exist-${System.nanoTime()}.png")

        val result = runBlocking { ReceiptOcrScanner.scan(context, Uri.fromFile(missing)) }

        assertEquals(null, result.amountToman)
        assertEquals(false, result.hasAnything)
    }
}
