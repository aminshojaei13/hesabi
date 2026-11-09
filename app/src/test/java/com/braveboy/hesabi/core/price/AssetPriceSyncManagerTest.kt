package com.braveboy.hesabi.core.price

import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.AssetKind
import com.braveboy.hesabi.domain.model.AssetPriceSnapshot
import com.braveboy.hesabi.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetPriceSyncManagerTest {

    private class FakeAssetRepository(private val assets: List<Asset>) : AssetRepository {
        val updates = mutableListOf<Triple<String, Long, String>>()

        override fun observeAll(): Flow<List<Asset>> = emptyFlow()
        override fun observeSnapshots(): Flow<List<AssetPriceSnapshot>> = emptyFlow()
        override suspend fun getAll(): List<Asset> = assets
        override suspend fun getById(id: String): Asset? = assets.firstOrNull { it.id == id }
        override suspend fun save(asset: Asset, recordSnapshot: Boolean) = Unit
        override suspend fun delete(id: String) = Unit
        override suspend fun updatePrice(
            assetId: String,
            priceTomanPerUnit: Long,
            source: String,
            atMillis: Long,
        ) {
            updates += Triple(assetId, priceTomanPerUnit, source)
        }
    }

    private class FakeSource(private val result: Result<PriceFetch>) : PriceSource {
        var calls = 0
        override suspend fun fetch(keys: Set<String>): Result<PriceFetch> {
            calls++
            return result
        }
    }

    private class FakeCache(var payload: PriceFetch? = null) : PriceCache {
        override suspend fun save(fetch: PriceFetch) {
            payload = fetch
        }

        override suspend fun load(): PriceFetch? = payload
    }

    private fun asset(id: String, name: String, kind: AssetKind, unit: String) = Asset(
        id = id,
        name = name,
        kind = kind,
        quantityMilli = 1_000,
        unitLabel = unit,
        buyPriceTomanPerUnit = 1_000,
        currentPriceTomanPerUnit = 1_000,
    )

    private val gold = asset("gold", "طلای ۱۸ عیار", AssetKind.GOLD, "گرم")
    private val dollar = asset("usd", "دلار آمریکا", AssetKind.CURRENCY, "دلار")
    private val coin = asset("coin", "سکه امامی", AssetKind.GOLD, "عدد")

    private fun quote(key: String, toman: Long) = PriceQuote(key, toman, 1_000L)

    @Test
    fun `updates mapped assets and skips coins`() = runTest {
        val repository = FakeAssetRepository(listOf(gold, dollar, coin))
        val cache = FakeCache()
        val source = FakeSource(
            Result.success(
                PriceFetch(
                    quotes = mapOf(
                        "geram18" to quote("geram18", 25_000_000L),
                        "price_dollar_rl" to quote("price_dollar_rl", 265_000L),
                    ),
                    fetchedAtMillis = 1_000L,
                ),
            ),
        )
        val manager = AssetPriceSyncManager(repository, source, cache, clock = { 2_000L })

        val result = manager.sync()

        assertEquals(2, result.updated)
        assertEquals(1, result.skipped)
        assertTrue(!result.hasError)
        assertEquals(25_000_000L, repository.updates.first { it.first == "gold" }.second)
        assertEquals(265_000L, repository.updates.first { it.first == "usd" }.second)
        assertTrue(repository.updates.all { it.third.startsWith("TGJU") })
        assertEquals(2, cache.payload?.quotes?.size)
    }

    @Test
    fun `falls back to the cached snapshot when the network fails`() = runTest {
        val repository = FakeAssetRepository(listOf(gold))
        val cache = FakeCache(
            PriceFetch(
                quotes = mapOf("geram18" to quote("geram18", 24_000_000L)),
                fetchedAtMillis = 500L,
            ),
        )
        val source = FakeSource(Result.failure(IllegalStateException("offline")))
        val manager = AssetPriceSyncManager(repository, source, cache)

        val result = manager.sync()

        assertEquals(1, result.updated)
        assertTrue(result.fromCache)
        assertTrue(repository.updates.single().third.contains("کش"))
    }

    @Test
    fun `reports a clear error when neither network nor cache is available`() = runTest {
        val repository = FakeAssetRepository(listOf(gold))
        val source = FakeSource(Result.failure(IllegalStateException("offline")))
        val manager = AssetPriceSyncManager(repository, source, FakeCache())

        val result = manager.sync()

        assertEquals(0, result.updated)
        assertTrue(result.hasError)
        assertTrue(repository.updates.isEmpty())
    }

    @Test
    fun `missing quote for an asset is skipped and reported`() = runTest {
        val repository = FakeAssetRepository(listOf(gold, dollar))
        val source = FakeSource(
            Result.success(
                PriceFetch(
                    quotes = mapOf("geram18" to quote("geram18", 25_000_000L)),
                    fetchedAtMillis = 1_000L,
                ),
            ),
        )
        val manager = AssetPriceSyncManager(repository, source, FakeCache())

        val result = manager.sync()

        assertEquals(1, result.updated)
        assertEquals(1, result.skipped)
    }

    @Test
    fun `source label carries the Jalali date and the cache marker`() {
        val manager = AssetPriceSyncManager(
            FakeAssetRepository(emptyList()),
            FakeSource(Result.failure(IllegalStateException())),
            FakeCache(),
        )

        val label = manager.sourceLabel(1_790_000_000_000L, fromCache = false)

        assertTrue(label.startsWith("TGJU · "))
        assertTrue(manager.sourceLabel(1_790_000_000_000L, fromCache = true).startsWith("TGJU (کش) · "))
    }
}
