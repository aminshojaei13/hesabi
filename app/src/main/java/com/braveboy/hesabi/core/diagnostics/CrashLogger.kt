package com.braveboy.hesabi.core.diagnostics

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight crash log that stays on the device.
 *
 * We deliberately store only the exception type and the first line of the message:
 * enough to debug, without ever writing amounts, notes or SMS content to disk.
 * A future release can plug a crash reporting SDK in here without touching callers.
 */
object CrashLogger {

    private const val FILE_NAME = "last-crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(appContext, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun lastCrash(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists()) file.readText().takeIf { it.isNotBlank() } else null
    }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }

    private fun write(context: Context, error: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val message = error.message.orEmpty().lineSequence().firstOrNull().orEmpty().take(160)
        val report = buildString {
            appendLine("زمان: $timestamp")
            appendLine("نوع: ${error::class.java.simpleName}")
            appendLine("پیام: $message")
            appendLine(error.stackTrace.take(8).joinToString("\n") { "  at $it" })
        }
        File(context.filesDir, FILE_NAME).writeText(report)
    }
}
