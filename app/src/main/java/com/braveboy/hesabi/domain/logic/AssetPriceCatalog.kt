package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.AssetKind

/**
 * Maps a user's holding to a public TGJU quote. The mapping is deliberately strict:
 * when the app cannot be sure what a unit means (coins, "عدد", unsupported currency)
 * it returns null and the user keeps entering that price by hand instead of silently
 * writing a misleading number.
 */
object AssetPriceCatalog {

    const val SOURCE_LABEL = "TGJU"

    /** Grams in one gold mesghal, multiplied by 1000 to keep the conversion integer. */
    private const val MILLI_GRAMS_PER_MESGHAL = 4_608L

    data class Mapping(
        val quoteKey: String,
        val instrumentLabel: String,
        val convertQuoteToUnit: (Long) -> Long,
    )

    val supportedQuoteKeys: Set<String> = setOf(
        "geram18", "geram24", "mesghal",
        "price_dollar_rl", "price_eur", "price_gbp", "price_aed",
        "price_try", "price_cny", "price_jpy", "price_rub",
        "crypto-bitcoin-irr", "crypto-ethereum-irr", "usdt-irr", "crypto-binance-coin-irr",
    )

    private data class Rule(val key: String, val label: String, val aliases: List<String>)

    private val currencyRules = listOf(
        Rule("price_dollar_rl", "دلار آمریکا", listOf("دلار", "دالر", "usd")),
        Rule("price_eur", "یورو", listOf("یورو", "eur")),
        Rule("price_gbp", "پوند انگلیس", listOf("پوند", "gbp")),
        Rule("price_aed", "درهم امارات", listOf("درهم", "aed")),
        Rule("price_try", "لیر ترکیه", listOf("لیر", "try")),
        Rule("price_cny", "یوان چین", listOf("یوان", "cny")),
        Rule("price_jpy", "ین ژاپن", listOf("ژاپن", "jpy")),
        Rule("price_rub", "روبل روسیه", listOf("روبل", "rub")),
    )

    private val cryptoRules = listOf(
        Rule("crypto-bitcoin-irr", "بیت‌کوین", listOf("بیت‌کوین", "بیت کوین", "bitcoin", "btc")),
        Rule("crypto-ethereum-irr", "اتریوم", listOf("اتریوم", "ethereum", "eth")),
        Rule("usdt-irr", "تتر", listOf("تتر", "tether", "usdt")),
        Rule("crypto-binance-coin-irr", "بی‌ان‌اس", listOf("bnb", "بی‌ان‌اس", "بایننس")),
    )

    /** Currencies that also contain the word "دلار" but have no quote in the catalog. */
    private val unsupportedCurrencyWords = listOf(
        "کانادا", "کانادایی", "استرالیا", "نیوزیلند", "سنگاپور", "هنگ‌کنگ", "hong",
        "cad", "aud", "nzd", "sgd", "hkd",
    )

    fun resolve(asset: Asset): Mapping? = when (asset.kind) {
        AssetKind.GOLD -> resolveGold(asset)
        AssetKind.CURRENCY -> resolveCurrency(asset)
        AssetKind.CRYPTO -> resolveCrypto(asset)
        else -> null
    }

    private fun resolveGold(asset: Asset): Mapping? {
        val name = normalize(asset.name)
        val unit = normalize(asset.unitLabel)
        // Coins are traded per piece and TGJU does not expose a stable per-coin key.
        if (name.contains("سکه") || unit.contains("سکه") || unit.contains("عدد")) return null
        if (unit.contains("هزار") || unit.contains("میلیون")) return null

        val mesghal = name.contains("مثقال") || unit.contains("مثقال")
        if (mesghal) {
            val perGram = !unit.contains("مثقال")
            return Mapping(
                quoteKey = "mesghal",
                instrumentLabel = if (perGram) "مثقال طلا (تبدیل به گرم)" else "مثقال طلا",
                convertQuoteToUnit = if (perGram) {
                    { quote -> quote * 1000 / MILLI_GRAMS_PER_MESGHAL }
                } else {
                    { quote -> quote }
                },
            )
        }
        val unitIsGram = unit.contains("گرم") || unit.isBlank()
        if (!unitIsGram) return null
        val is24 = name.contains("24") || name.contains("۲۴") || name.contains("تمام")
        return if (is24) {
            Mapping("geram24", "طلای ۲۴ عیار") { quote -> quote }
        } else {
            Mapping("geram18", "طلای ۱۸ عیار") { quote -> quote }
        }
    }

    private fun resolveCurrency(asset: Asset): Mapping? {
        val text = normalize(asset.name + " " + asset.unitLabel)
        if (unsupportedCurrencyWords.any { text.contains(it) }) return null
        val unit = normalize(asset.unitLabel)
        if (unit.contains("هزار") || unit.contains("میلیون")) return null
        val rule = currencyRules.firstOrNull { rule -> rule.aliases.any { text.contains(normalize(it)) } }
            ?: return null
        return Mapping(rule.key, rule.label) { quote -> quote }
    }

    private fun resolveCrypto(asset: Asset): Mapping? {
        val text = normalize(asset.name + " " + asset.unitLabel)
        val rule = cryptoRules.firstOrNull { rule -> rule.aliases.any { text.contains(normalize(it)) } }
            ?: return null
        return Mapping(rule.key, rule.label) { quote -> quote }
    }

    private fun normalize(raw: String): String = raw
        .lowercase()
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace("\u200c", "")
        .trim()
}
