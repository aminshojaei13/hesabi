package com.braveboy.hesabi.core.price

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One quote from the public TGJU feed, already converted to Toman. */
data class PriceQuote(
    val key: String,
    val tomanPerQuoteUnit: Long,
    val updatedAtMillis: Long,
)

data class PriceFetch(
    val quotes: Map<String, PriceQuote>,
    val fetchedAtMillis: Long,
)

/** Anything that can deliver a fresh quote snapshot; the app has one HTTP implementation. */
interface PriceSource {
    suspend fun fetch(keys: Set<String> = AssetPriceKeys.ALL): Result<PriceFetch>
}

/**
 * Reads the free, key-less TGJU snapshot (`ajax.json`). It is a plain HTTPS GET with
 * a browser user-agent; no account, no tracking and no identifiers are sent. The
 * response is parsed here and now, and only the handful of keys we need is kept.
 */
class TgjuPriceClient(
    private val endpoint: String = DEFAULT_ENDPOINT,
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 10_000,
) : PriceSource {

    override suspend fun fetch(keys: Set<String>): Result<PriceFetch> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = download()
                val quotes = TgjuPriceParser.parse(body, keys)
                if (quotes.isEmpty()) error("پاسخی از منبع قیمت دریافت نشد.")
                PriceFetch(quotes = quotes, fetchedAtMillis = System.currentTimeMillis())
            }
        }

    private fun download(): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Hesabi/1.0 (Android; offline-first finance app)")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("کد پاسخ منبع قیمت: $code")
            val stream = BufferedInputStream(connection.inputStream)
            val buffer = ByteArray(16 * 1024)
            val builder = StringBuilder()
            var total = 0
            stream.use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    // The whole snapshot is a few hundred kilobytes; refuse anything huge.
                    if (total > MAX_RESPONSE_BYTES) error("پاسخ منبع قیمت بیش از حد بزرگ است.")
                    builder.append(String(buffer, 0, read, Charsets.UTF_8))
                }
            }
            return builder.toString()
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://call1.tgju.org/ajax.json"
        private const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024
    }
}

/** The exact TGJU keys the app understands, kept in one place for the client and UI. */
object AssetPriceKeys {
    val ALL: Set<String> = setOf(
        "geram18", "geram24", "mesghal",
        "price_dollar_rl", "price_eur", "price_gbp", "price_aed",
        "price_try", "price_cny", "price_jpy", "price_rub",
        "crypto-bitcoin-irr", "crypto-ethereum-irr", "usdt-irr", "crypto-binance-coin-irr",
    )
}

object TgjuPriceParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val tehran: ZoneId = ZoneId.of("Asia/Tehran")

    /**
     * TGJU publishes Rial amounts as formatted strings ("256,247,000") and a
     * "ts" in Tehran local time. Everything is converted to Toman and epoch millis
     * at this boundary so nothing downstream has to remember the source units.
     */
    fun parse(
        body: String,
        keys: Set<String> = AssetPriceKeys.ALL,
        zone: ZoneId = tehran,
    ): Map<String, PriceQuote> {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return emptyMap()
        val current = root["current"]?.asObjectOrNull() ?: return emptyMap()
        val result = LinkedHashMap<String, PriceQuote>()
        for (key in keys) {
            val entry = current[key]?.asObjectOrNull() ?: continue
            val rawPrice = entry["p"]?.asStringOrNull() ?: continue
            val rial = parseAmount(rawPrice) ?: continue
            if (rial <= 0) continue
            val toman = rial / 10L
            if (toman <= 0) continue
            val updatedAt = entry["ts"]?.asStringOrNull()?.let { parseTimestamp(it, zone) } ?: 0L
            result[key] = PriceQuote(key = key, tomanPerQuoteUnit = toman, updatedAtMillis = updatedAt)
        }
        return result
    }

    fun parseAmount(raw: String): Long? {
        val cleaned = raw.trim().replace(",", "").replace("٬", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        return cleaned.toLongOrNull()
    }

    fun parseTimestamp(raw: String, zone: ZoneId = tehran): Long? = runCatching {
        LocalDateTime.parse(raw.trim(), timestampFormat).atZone(zone).toInstant().toEpochMilli()
    }.getOrNull()

    private fun kotlinx.serialization.json.JsonElement.asObjectOrNull(): JsonObject? =
        runCatching { jsonObject }.getOrNull()

    private fun kotlinx.serialization.json.JsonElement.asStringOrNull(): String? =
        runCatching { jsonPrimitive.content }.getOrNull()
            ?.takeIf { it.isNotBlank() && it != "null" }
}
