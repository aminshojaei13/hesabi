package com.braveboy.hesabi.data.sms

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.domain.model.TransactionType
import java.time.LocalDate

enum class SmsDirection { DEBIT, CREDIT, UNKNOWN }

enum class SmsIgnoreReason {
    OTP,
    PROMOTION,
    BALANCE_ONLY,
    NO_AMOUNT,
    IGNORED_SENDER,
}

data class ParsedBankSms(
    val amountToman: Long,
    val direction: SmsDirection,
    val type: TransactionType,
    val last4: String?,
    val occurredOnEpochDay: Long,
    val merchant: String?,
    val bankName: String?,
    val confidence: Float,
    val requiresReview: Boolean,
    val rawSnippet: String,
)

sealed interface SmsParseResult {
    data class Accepted(val parsed: ParsedBankSms) : SmsParseResult
    data class Ignored(val reason: SmsIgnoreReason, val bankName: String? = null) : SmsParseResult
}

/**
 * Pure, deterministic parser for Iranian bank SMS. It never touches Android APIs,
 * which keeps it fully unit-testable against a real-world corpus.
 *
 * Privacy: the caller decides whether to persist the raw body. This parser only
 * reads it in memory.
 */
object BankSmsParser {

    private val persianMonths = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    private val otpKeywords = listOf(
        "رمز", "کد تایید", "کد تأیید", "کد ورود", "otp", "cvv", "cv2", "پین", "رمز پویا", "رمز دوم",
        "رمز یکبار", "رمز یک‌بار",
    )

    private val promotionKeywords = listOf(
        "تبلیغ", "جشنواره", "قرعه", "وام", "تسهیلات", "بیمه", "اعتبار ویژه", "بسته اینترنت",
        "شارژ", "هدیه", "دعوت", "کد تخفیف", "فرصت ویژه",
    )

    private val debitKeywords = listOf(
        "برداشت", "خرید", "کسر", "پرداخت", "بدهکار", "انتقال از", "برداشت از", "خرید اینترنتی",
        "پرداخت اینترنتی", "کارت به کارت", "شماره کارت شما",
    )

    private val creditKeywords = listOf(
        "واریز", "افزایش موجودی", "بستانکار", "دریافت", "به حساب شما", "اضافه شد", "واریز به حساب",
    )

    private val balanceKeywords = listOf("مانده", "موجودی", "موجودي", "اعتبار", "balance")

    private val actionKeywords = debitKeywords + creditKeywords

    private val bankSenders = linkedMapOf(
        "MELLAT" to "بانک ملت",
        "MELI" to "بانک ملی",
        "MELLI" to "بانک ملی",
        "SADERAT" to "بانک صادرات",
        "TEJARAT" to "بانک تجارت",
        "PARSIAN" to "بانک پارسیان",
        "SAMAN" to "بانک سامان",
        "REFAH" to "بانک رفاه",
        "AYANDEH" to "بانک آینده",
        "PASARGAD" to "بانک پاسارگاد",
        "SEP" to "بانک سپه",
        "SEPAH" to "بانک سپه",
        "TOSEE" to "توسعه تعاون",
        "KARAFARIN" to "بانک کارآفرین",
        "RESALAT" to "بانک رسالت",
        "SARMAYEH" to "بانک سرمایه",
        "MEHR" to "بانک مهر ایران",
        "BLUE" to "بلوبانک",
        "SHHR" to "بانک شهر",
        "SHAHR" to "بانک شهر",
        "IRANZAMIN" to "ایران زمین",
        "POST" to "پست بانک",
        "EDBI" to "توسعه صادرات",
        "DAY" to "بانک دی",
        "DEY" to "بانک دی",
        "GARDESH" to "گردشگری",
        "SINA" to "سینا",
        "HEKMAT" to "حکمت",
        "KHAVAR" to "خاورمیانه",
        "MASKAN" to "مسکن",
    )

    fun parse(
        sender: String,
        body: String,
        receivedAtMillis: Long,
        todayEpochDay: Long,
        amountsAreRial: Boolean = true,
        ignoredSenders: Set<String> = emptySet(),
    ): SmsParseResult {
        val bank = detectBank(sender)
        val normalizedSender = normalizeSender(sender)
        if (ignoredSenders.any { normalizeSender(it) == normalizedSender }) {
            return SmsParseResult.Ignored(SmsIgnoreReason.IGNORED_SENDER, bank)
        }

        val text = normalize(body)
        if (text.isBlank()) return SmsParseResult.Ignored(SmsIgnoreReason.NO_AMOUNT, bank)

        if (otpKeywords.any { text.contains(it) }) {
            return SmsParseResult.Ignored(SmsIgnoreReason.OTP, bank)
        }

        val hasAction = actionKeywords.any { text.contains(it) }
        if (!hasAction && promotionKeywords.any { text.contains(it) }) {
            return SmsParseResult.Ignored(SmsIgnoreReason.PROMOTION, bank)
        }

        if (balanceKeywords.any { text.contains(it) } && !hasAction) {
            return SmsParseResult.Ignored(SmsIgnoreReason.BALANCE_ONLY, bank)
        }

        val amounts = extractAmounts(text)
        if (amounts.isEmpty()) return SmsParseResult.Ignored(SmsIgnoreReason.NO_AMOUNT, bank)

        val best = amounts.maxByOrNull { it.score } ?: return SmsParseResult.Ignored(SmsIgnoreReason.NO_AMOUNT, bank)
        if (best.value <= 0L) return SmsParseResult.Ignored(SmsIgnoreReason.NO_AMOUNT, bank)

        val direction = detectDirection(text)
        val last4 = extractLast4(text)
        val date = extractDate(text, todayEpochDay)
        val merchant = extractMerchant(text)
        // Banks print Rial by default. A message that explicitly says Toman is kept as-is.
        val isToman = best.unit == AmountUnit.TOMAN
        val toman = when {
            isToman -> best.value
            best.unit == AmountUnit.RIAL -> best.value / 10
            amountsAreRial -> best.value / 10
            else -> best.value
        }
        if (toman <= 0L) return SmsParseResult.Ignored(SmsIgnoreReason.NO_AMOUNT, bank)

        val type = when (direction) {
            SmsDirection.CREDIT -> TransactionType.INCOME
            SmsDirection.DEBIT -> TransactionType.EXPENSE
            SmsDirection.UNKNOWN -> TransactionType.EXPENSE
        }

        var confidence = 0.20f
        if (best.hasCurrency) confidence += 0.30f
        if (direction != SmsDirection.UNKNOWN) confidence += 0.20f else confidence -= 0.25f
        if (last4 != null) confidence += 0.10f
        if (merchant != null) confidence += 0.05f
        confidence = confidence.coerceIn(0f, 1f)

        return SmsParseResult.Accepted(
            ParsedBankSms(
                amountToman = toman,
                direction = direction,
                type = type,
                last4 = last4,
                occurredOnEpochDay = date,
                merchant = merchant,
                bankName = bank,
                confidence = confidence,
                requiresReview = confidence < 0.80f || direction == SmsDirection.UNKNOWN,
                rawSnippet = body.take(160),
            ),
        )
    }

    fun normalizeSender(sender: String): String = Digits.toAscii(sender).uppercase().filter { it.isLetterOrDigit() }

    fun detectBank(sender: String): String? {
        val key = normalizeSender(sender)
        val match = bankSenders.entries.firstOrNull { key.contains(it.key) }
        if (match != null) return match.value
        return when {
            key.contains("BANK") -> "بانک"
            else -> null
        }
    }

    /**
     * Extracts the account balance mentioned in an SMS after keywords like "مانده", "موجودی", "اعتبار".
     * Converts to Toman based on unit or fallback to [amountsAreRial].
     */
    fun extractStatedBalanceToman(body: String, amountsAreRial: Boolean = true): Long? {
        val text = normalize(body)
        val regex = Regex("""(?<![\d,])(\d{1,3}(?:[,]\d{3})+|\d{4,})(?:\s*(ریال|ريال|تومان|تومن|IRR|IRT))?""")
        for (keyword in balanceKeywords) {
            val idx = text.indexOf(keyword)
            if (idx != -1) {
                val window = text.substring(idx, (idx + 60).coerceAtMost(text.length))
                val match = regex.find(window)
                if (match != null) {
                    val raw = match.groupValues[1].replace(",", "")
                    val value = raw.toLongOrNull() ?: continue
                    val currency = match.groupValues[2]
                    val isToman = currency in listOf("تومان", "تومن", "IRT")
                    val isRial = currency in listOf("ریال", "ريال", "IRR")
                    val toman = when {
                        isToman -> value
                        isRial -> value / 10
                        amountsAreRial -> value / 10
                        else -> value
                    }
                    if (toman >= 0) return toman
                }
            }
        }
        return null
    }

    private fun normalize(body: String): String {
        val ascii = Digits.toAscii(body)
        return ascii
            .replace('\u200c', ' ')
            .replace('\n', ' ')
            .replace('\t', ' ')
            .replace("ـ", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private enum class AmountUnit { RIAL, TOMAN }

    private data class AmountCandidate(
        val value: Long,
        val hasCurrency: Boolean,
        val unit: AmountUnit?,
        val score: Int,
        val start: Int,
    )

    private fun extractAmounts(text: String): List<AmountCandidate> {
        val regex = Regex("""(?<![\d,])(\d{1,3}(?:[,]\d{3})+|\d{4,})(?:\s*(ریال|ريال|تومان|تومن|IRR|IRT))?""")
        return regex.findAll(text).mapNotNull { match ->
            val raw = match.groupValues[1].replace(",", "")
            val value = raw.toLongOrNull() ?: return@mapNotNull null
            val currency = match.groupValues[2]
            val hasCurrency = currency.isNotBlank()
            val unit = when {
                currency in listOf("تومان", "تومن", "IRT") -> AmountUnit.TOMAN
                currency in listOf("ریال", "ريال", "IRR") -> AmountUnit.RIAL
                else -> null
            }
            val start = match.range.first
            if (!hasCurrency) {
                if (value in 1300L..1500L) return@mapNotNull null // Jalali year
                if (value in 1900L..2100L) return@mapNotNull null // Gregorian year
                if (raw.length >= 12) return@mapNotNull null // card/account number
                if (value < 1_000L) return@mapNotNull null
            }
            val before = text.substring((start - 24).coerceAtLeast(0), start)
            val after = text.substring(match.range.last + 1, (match.range.last + 24).coerceAtMost(text.length))
            if (balanceKeywords.any { before.contains(it) || after.take(14).contains(it) }) {
                return@mapNotNull null
            }
            var score = 0
            if (hasCurrency) score += 3
            if (raw.contains(",")) score += 1
            val proximity = actionKeywords.any { before.contains(it) }
            if (proximity) score += 4
            AmountCandidate(value = value, hasCurrency = hasCurrency, unit = unit, score = score, start = start)
        }.toList()
    }

    private fun detectDirection(text: String): SmsDirection {
        // "انتقال ... از حساب X به حساب Y" is a debit from the sender,
        // while "انتقال ... به حساب شما" is money arriving.
        if (text.contains("انتقال") && text.contains("از حساب")) {
            return if (text.contains("به حساب شما") || text.contains("به حساب شما واریز")) {
                SmsDirection.CREDIT
            } else {
                SmsDirection.DEBIT
            }
        }
        val debitIndex = debitKeywords.mapNotNull { kw -> text.indexOf(kw).takeIf { it >= 0 } }.minOrNull()
        val creditIndex = creditKeywords.mapNotNull { kw -> text.indexOf(kw).takeIf { it >= 0 } }.minOrNull()
        return when {
            debitIndex == null && creditIndex == null -> SmsDirection.UNKNOWN
            creditIndex == null -> SmsDirection.DEBIT
            debitIndex == null -> SmsDirection.CREDIT
            // Whichever action appears first usually describes the movement.
            debitIndex <= creditIndex -> SmsDirection.DEBIT
            else -> SmsDirection.CREDIT
        }
    }

    private fun extractLast4(text: String): String? {
        val patterns = listOf(
            Regex("""(?:کارت|حساب)[^\d]{0,18}(?<![\d])(\d{4})(?![\d])"""),
            Regex("""[*…\.]{2,}\s*(\d{4})(?![\d])"""),
            Regex("""(?<![\d])(\d{4})\*{2,}"""),
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val value = match.groupValues[1]
                if (value.length == 4) return value
            }
        }
        // Full account/card numbers: keep only the last four digits for matching.
        val fullNumber = Regex("""(?:کارت|حساب)[^\d]{0,12}(\d{6,})""").find(text)
        return fullNumber?.groupValues?.getOrNull(1)?.takeLast(4)
    }

    private fun extractDate(text: String, todayEpochDay: Long): Long {
        val jalali = Regex("""(\d{4})[/-](\d{1,2})[/-](\d{1,2})""").find(text)
        if (jalali != null) {
            val year = jalali.groupValues[1].toIntOrNull()
            if (year != null && year in 1300..1500) {
                JalaliDate.parse(jalali.value)?.let { return it.toEpochDay() }
            } else if (year != null && year in 1900..2100) {
                runCatching {
                    LocalDate.of(
                        year,
                        jalali.groupValues[2].toInt(),
                        jalali.groupValues[3].toInt(),
                    ).toEpochDay()
                }.getOrNull()?.let { return it }
            }
        }
        val monthMatch = Regex("""(\d{1,2})\s*(${persianMonths.joinToString("|")})\s*(\d{4})""").find(text)
        if (monthMatch != null) {
            val day = monthMatch.groupValues[1].toIntOrNull()
            val month = persianMonths.indexOf(monthMatch.groupValues[2]) + 1
            val year = monthMatch.groupValues[3].toIntOrNull()
            if (day != null && month > 0 && year != null) {
                runCatching { JalaliDate(year, month, day).toEpochDay() }.getOrNull()?.let { return it }
            }
        }
        return todayEpochDay
    }

    private fun extractMerchant(text: String): String? {
        val patterns = listOf(
            Regex("""بابت\s+(.{2,40}?)(?=\s+\d|\s+(?:با|در|به|از|مبلغ)\s|\.|،|$)"""),
            Regex("""خرید(?: اینترنتی)?\s+(?:[\d,.]+\s*(?:ریال|تومان)?\s+)?از\s+(.{2,40})$"""),
            Regex("""خرید(?: اینترنتی)?\s+از\s+(.{2,40}?)(?=\s+\d|\.|،|$)"""),
            Regex("""در\s+(.{2,40}?)\s+به مبلغ"""),
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            val value = match?.groupValues?.getOrNull(1)?.trim()?.trim('.', '،', ' ')
            if (!value.isNullOrBlank() && value.any { it.isLetter() }) return value.take(40)
        }
        return null
    }
}
