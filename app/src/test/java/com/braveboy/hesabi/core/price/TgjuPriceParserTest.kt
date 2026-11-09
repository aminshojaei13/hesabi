package com.braveboy.hesabi.core.price

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TgjuPriceParserTest {

    private val sample = """
        {
          "current": {
            "geram18": { "p": "256,247,000", "ts": "2026-11-09 12:14:41" },
            "price_dollar_rl": { "p": "2,653,650", "ts": "2026-11-09 12:14:45" },
            "price_eur": { "p": "", "ts": "" },
            "price_gbp": { "p": "zero", "ts": "" },
            "crypto-bitcoin-irr": { "p": "195,899,891,000", "ts": "2026-11-09 12:12:49" }
          }
        }
    """.trimIndent()

    @Test
    fun `converts Rial quotes to Toman`() {
        val quotes = TgjuPriceParser.parse(sample)

        assertEquals(25_624_700L, quotes.getValue("geram18").tomanPerQuoteUnit)
        assertEquals(265_365L, quotes.getValue("price_dollar_rl").tomanPerQuoteUnit)
        assertEquals(19_589_989_100L, quotes.getValue("crypto-bitcoin-irr").tomanPerQuoteUnit)
    }

    @Test
    fun `skips empty and malformed prices`() {
        val quotes = TgjuPriceParser.parse(sample)

        assertNull(quotes["price_eur"])
        assertNull(quotes["price_gbp"])
    }

    @Test
    fun `parses the Tehran timestamp`() {
        val quotes = TgjuPriceParser.parse(sample)

        val expected = LocalDateTime.of(2026, 11, 9, 12, 14, 41)
            .atZone(ZoneId.of("Asia/Tehran"))
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, quotes.getValue("geram18").updatedAtMillis)
    }

    @Test
    fun `only requested keys are kept`() {
        val quotes = TgjuPriceParser.parse(sample, setOf("price_dollar_rl"))

        assertEquals(setOf("price_dollar_rl"), quotes.keys)
    }

    @Test
    fun `broken json yields an empty map instead of crashing`() {
        assertTrue(TgjuPriceParser.parse("{not json").isEmpty())
        assertTrue(TgjuPriceParser.parse("""{"current":null}""").isEmpty())
    }
}
