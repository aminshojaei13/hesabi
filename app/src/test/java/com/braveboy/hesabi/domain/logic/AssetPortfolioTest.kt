package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.AssetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetPortfolioTest {

    @Test
    fun `summarize totals active assets and ignores archived ones`() {
        val assets = listOf(
            asset(id = "gold", kind = AssetKind.GOLD, quantityMilli = 2_000, buy = 5_000_000, current = 6_000_000),
            asset(id = "usd", kind = AssetKind.CURRENCY, quantityMilli = 1_000, buy = 90_000, current = 100_000),
            asset(
                id = "old",
                kind = AssetKind.OTHER,
                quantityMilli = 1_000,
                buy = 1_000,
                current = 1_000,
                archived = true,
            ),
        )

        val summary = AssetPortfolioCalculator.summarize(assets)

        assertEquals(10_090_000L, summary.totalCostToman)
        assertEquals(12_100_000L, summary.totalValueToman)
        assertEquals(2_010_000L, summary.profitToman)
        assertTrue(summary.hasProfitablePosition)
        assertEquals(2, summary.allocations.size)
        assertEquals(AssetKind.GOLD, summary.allocations.first().kind)
        assertEquals(1f, summary.allocations.sumOf { it.share.toDouble() }.toFloat(), 0.0001f)
        assertNull(summary.lastPriceUpdatedAtMillis)
    }

    @Test
    fun `profit percent is null without a cost basis`() {
        val summary = AssetPortfolioCalculator.summarize(
            listOf(asset(id = "gift", kind = AssetKind.OTHER, quantityMilli = 1_000, buy = 0, current = 500_000)),
        )

        assertEquals(500_000L, summary.profitToman)
        assertNull(summary.profitPercent)
    }

    @Test
    fun `price change uses first and last snapshot`() {
        assertEquals(20.0, AssetPortfolioCalculator.priceChangePercent(listOf(100, 110, 120))!!, 0.0001)
        assertNull(AssetPortfolioCalculator.priceChangePercent(listOf(100)))
        assertNull(AssetPortfolioCalculator.priceChangePercent(listOf(0, 100)))
    }

    private fun asset(
        id: String,
        kind: AssetKind,
        quantityMilli: Long,
        buy: Long,
        current: Long,
        archived: Boolean = false,
    ) = Asset(
        id = id,
        name = id,
        kind = kind,
        quantityMilli = quantityMilli,
        unitLabel = "واحد",
        buyPriceTomanPerUnit = buy,
        currentPriceTomanPerUnit = current,
        archived = archived,
    )
}
