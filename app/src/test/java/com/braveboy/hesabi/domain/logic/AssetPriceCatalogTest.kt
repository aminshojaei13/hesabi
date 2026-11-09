package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.AssetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssetPriceCatalogTest {

    private fun asset(
        name: String,
        kind: AssetKind,
        unit: String,
    ) = Asset(
        id = "id",
        name = name,
        kind = kind,
        quantityMilli = 1_000,
        unitLabel = unit,
        buyPriceTomanPerUnit = 1_000,
        currentPriceTomanPerUnit = 1_000,
    )

    @Test
    fun `gram of 18 karat gold maps to geram18`() {
        val mapping = AssetPriceCatalog.resolve(asset("طلای ۱۸ عیار", AssetKind.GOLD, "گرم"))

        assertEquals("geram18", mapping?.quoteKey)
        assertEquals(25_000_000L, mapping?.convertQuoteToUnit?.invoke(25_000_000L))
    }

    @Test
    fun `24 karat gold maps to geram24`() {
        val mapping = AssetPriceCatalog.resolve(asset("طلای تمام", AssetKind.GOLD, "گرم"))

        assertEquals("geram24", mapping?.quoteKey)
    }

    @Test
    fun `mesghal per gram is converted with integer math`() {
        val perGram = AssetPriceCatalog.resolve(asset("مثقال طلا", AssetKind.GOLD, "گرم"))
        val perMesghal = AssetPriceCatalog.resolve(asset("مثقال طلا", AssetKind.GOLD, "مثقال"))

        assertEquals("mesghal", perGram?.quoteKey)
        assertEquals(1_000_000L, perGram?.convertQuoteToUnit?.invoke(4_608_000L))
        assertEquals(4_608_000L, perMesghal?.convertQuoteToUnit?.invoke(4_608_000L))
    }

    @Test
    fun `coins are left for manual pricing`() {
        assertNull(AssetPriceCatalog.resolve(asset("سکه امامی", AssetKind.GOLD, "عدد")))
        assertNull(AssetPriceCatalog.resolve(asset("سکه بهار آزادی", AssetKind.GOLD, "سکه")))
    }

    @Test
    fun `dollar and euro map to their quotes`() {
        assertEquals(
            "price_dollar_rl",
            AssetPriceCatalog.resolve(asset("دلار آمریکا", AssetKind.CURRENCY, "دلار"))?.quoteKey,
        )
        assertEquals(
            "price_eur",
            AssetPriceCatalog.resolve(asset("یورو", AssetKind.CURRENCY, "یورو"))?.quoteKey,
        )
    }

    @Test
    fun `unsupported currencies stay manual`() {
        assertNull(AssetPriceCatalog.resolve(asset("دلار کانادا", AssetKind.CURRENCY, "دلار")))
        assertNull(AssetPriceCatalog.resolve(asset("دلار استرالیا", AssetKind.CURRENCY, "دلار")))
    }

    @Test
    fun `crypto names map even with a zero width non joiner`() {
        assertEquals(
            "crypto-bitcoin-irr",
            AssetPriceCatalog.resolve(asset("بیت‌کوین", AssetKind.CRYPTO, "بیت‌کوین"))?.quoteKey,
        )
        assertEquals(
            "usdt-irr",
            AssetPriceCatalog.resolve(asset("تتر", AssetKind.CRYPTO, "USDT"))?.quoteKey,
        )
    }

    @Test
    fun `non priceable kinds are skipped`() {
        assertNull(AssetPriceCatalog.resolve(asset("آپارتمان", AssetKind.PROPERTY, "واحد")))
        assertNull(AssetPriceCatalog.resolve(asset("پژو ۲۰۶", AssetKind.VEHICLE, "دستگاه")))
    }
}
