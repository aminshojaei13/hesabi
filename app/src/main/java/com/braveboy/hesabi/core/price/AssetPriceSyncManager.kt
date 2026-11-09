package com.braveboy.hesabi.core.price

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.domain.logic.AssetPriceCatalog
import com.braveboy.hesabi.domain.repository.AssetRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class PriceSyncResult(
    val updated: Int,
    val skipped: Int,
    val fromCache: Boolean,
    val fetchedAtMillis: Long,
    val error: String? = null,
) {
    val hasError: Boolean get() = error != null
}

/**
 * Fetches the public TGJU snapshot, maps each holding through [AssetPriceCatalog]
 * and writes the result through the normal repository path, so every automatic
 * update also records a price snapshot for the profit/trend chart (5.2, 5.3).
 */
class AssetPriceSyncManager(
    private val assetRepository: AssetRepository,
    private val client: PriceSource = TgjuPriceClient(),
    private val cache: PriceCache,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun sync(): PriceSyncResult {
        val assets = runCatching { assetRepository.getAll() }.getOrElse {
            return PriceSyncResult(0, 0, false, 0L, "فهرست دارایی‌ها خوانده نشد.")
        }
        val active = assets.filterNot { it.archived }
        val targets = active.mapNotNull { asset ->
            AssetPriceCatalog.resolve(asset)?.let { asset to it }
        }
        if (targets.isEmpty()) {
            return PriceSyncResult(updated = 0, skipped = active.size, fromCache = false, fetchedAtMillis = 0L)
        }

        val keys = targets.map { it.second.quoteKey }.toSet()
        var fromCache = false
        val fetch = client.fetch(keys).getOrElse { fetchError ->
            val cached = cache.load()
                ?: return PriceSyncResult(
                    updated = 0,
                    skipped = active.size,
                    fromCache = false,
                    fetchedAtMillis = 0L,
                    error = "اتصال به منبع قیمت برقرار نشد. ${fetchError.message.orEmpty()}".trim(),
                )
            fromCache = true
            cached
        }

        var updated = 0
        var skipped = 0
        for ((asset, mapping) in targets) {
            val quote = fetch.quotes[mapping.quoteKey]
            if (quote == null) {
                skipped++
                continue
            }
            val price = runCatching { mapping.convertQuoteToUnit(quote.tomanPerQuoteUnit) }.getOrNull()
            if (price == null || price <= 0) {
                skipped++
                continue
            }
            val atMillis = quote.updatedAtMillis.takeIf { it > 0 } ?: fetch.fetchedAtMillis
            val label = sourceLabel(atMillis, fromCache)
            val saved = runCatching {
                assetRepository.updatePrice(
                    assetId = asset.id,
                    priceTomanPerUnit = price,
                    source = label,
                    atMillis = atMillis,
                )
            }.isSuccess
            if (saved) updated++ else skipped++
        }

        if (!fromCache) runCatching { cache.save(fetch) }

        return PriceSyncResult(
            updated = updated,
            // Anything that did not get a number (coins, unsupported currencies,
            // missing keys) is reported as skipped so the UI can explain it.
            skipped = active.size - updated,
            fromCache = fromCache,
            fetchedAtMillis = fetch.fetchedAtMillis,
            error = null,
        )
    }

    /** "TGJU (کش) · ۱۴۰۵/۰۸/۱۸ ۱۲:۳۰" — the UI always shows where a number came from. */
    fun sourceLabel(atMillis: Long, fromCache: Boolean): String {
        val time = if (atMillis <= 0) clock() else atMillis
        val date = JalaliDate.fromEpochDay(
            Instant.ofEpochMilli(time).atZone(TEHRAN).toLocalDate().toEpochDay(),
        )
        val clockText = Instant.ofEpochMilli(time).atZone(TEHRAN).format(TIME_FORMAT)
        val prefix = if (fromCache) "${AssetPriceCatalog.SOURCE_LABEL} (کش)" else AssetPriceCatalog.SOURCE_LABEL
        return "$prefix · ${date.formatShort()} ${Digits.toPersian(clockText)}"
    }

    private companion object {
        val TEHRAN: ZoneId = ZoneId.of("Asia/Tehran")
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
