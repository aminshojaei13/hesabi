package com.braveboy.hesabi.data.sms

import com.braveboy.hesabi.core.money.Digits

/**
 * Masks long numeric runs (card/account/IBAN/sayad numbers) before an SMS is stored.
 *
 * The amount and merchant stay readable so the confirmation dialog remains usable,
 * but a 16-digit card number is reduced to `•••• 1234`. This is the storage-side half
 * of M20; the raw body only lives in memory during parsing.
 */
object SmsTextMasker {

    private val sensitiveContext = listOf("کارت", "حساب", "شبا", "صیاد", "شناسه", "شماره")

    fun mask(body: String): String = Regex("""\d{8,}""").replace(body) { match ->
        val digits = match.value
        val prefix = body.substring((match.range.first - 20).coerceAtLeast(0), match.range.first)
        val looksLikeIdentifier = digits.length >= 12 || sensitiveContext.any { prefix.contains(it) }
        if (looksLikeIdentifier) "•••• " + digits.takeLast(4) else digits
    }

    /** Stable content key used for duplicate detection (ignores spacing and digits shape). */
    fun normalizeForHash(body: String): String = Digits.toAscii(body)
        .replace("\u200c", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
