package com.braveboy.hesabi.core.price

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Last-known prices so an offline launch can still show a labelled number. */
interface PriceCache {
    suspend fun save(fetch: PriceFetch)
    suspend fun load(): PriceFetch?
}

private val Context.priceCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "price_cache")

/**
 * Keeps the last successful TGJU snapshot so the assets screen can still show a
 * labelled price when the phone is offline. The cache is plain prices and the time
 * they were fetched — no account id, no device id, no location.
 */
class PriceCacheStore(private val context: Context) : PriceCache {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun save(fetch: PriceFetch) {
        val payload = CachedPayload(
            fetchedAtMillis = fetch.fetchedAtMillis,
            quotes = fetch.quotes.values.map { CachedQuote(it.key, it.tomanPerQuoteUnit, it.updatedAtMillis) },
        )
        context.priceCacheDataStore.edit { prefs ->
            prefs[KEY_CACHE] = json.encodeToString(CachedPayload.serializer(), payload)
        }
    }

    override suspend fun load(): PriceFetch? {
        val raw = runCatching { context.priceCacheDataStore.data.first()[KEY_CACHE] }.getOrNull() ?: return null
        val payload = runCatching {
            json.decodeFromString(CachedPayload.serializer(), raw)
        }.getOrNull() ?: return null
        if (payload.quotes.isEmpty()) return null
        return PriceFetch(
            quotes = payload.quotes.associate {
                it.key to PriceQuote(it.key, it.priceTomanPerUnit, it.updatedAtMillis)
            },
            fetchedAtMillis = payload.fetchedAtMillis,
        )
    }

    @Serializable
    private data class CachedPayload(
        val fetchedAtMillis: Long,
        val quotes: List<CachedQuote>,
    )

    @Serializable
    private data class CachedQuote(
        val key: String,
        val priceTomanPerUnit: Long,
        val updatedAtMillis: Long,
    )

    private companion object {
        val KEY_CACHE = stringPreferencesKey("tgju_snapshot")
    }
}
