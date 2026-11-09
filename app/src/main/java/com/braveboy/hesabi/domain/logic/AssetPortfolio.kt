package com.braveboy.hesabi.domain.logic

import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.AssetKind

data class AssetAllocation(
    val kind: AssetKind,
    val valueToman: Long,
    val share: Float,
)

data class PortfolioSummary(
    val totalCostToman: Long,
    val totalValueToman: Long,
    val profitToman: Long,
    val profitPercent: Double?,
    val allocations: List<AssetAllocation>,
    val lastPriceUpdatedAtMillis: Long?,
) {
    val hasProfitablePosition: Boolean get() = profitToman > 0
}

object AssetPortfolioCalculator {

    fun summarize(assets: List<Asset>): PortfolioSummary {
        val active = assets.filterNot { it.archived }
        val totalCost = active.sumOf { it.costToman }
        val totalValue = active.sumOf { it.currentValueToman }
        val profit = totalValue - totalCost
        val byKind = active.groupBy { it.kind }
            .map { (kind, list) ->
                val value = list.sumOf { it.currentValueToman }
                AssetAllocation(
                    kind = kind,
                    valueToman = value,
                    share = if (totalValue == 0L) 0f else value.toFloat() / totalValue,
                )
            }
            .sortedByDescending { it.valueToman }
        return PortfolioSummary(
            totalCostToman = totalCost,
            totalValueToman = totalValue,
            profitToman = profit,
            profitPercent = if (totalCost > 0) profit * 100.0 / totalCost else null,
            allocations = byKind,
            lastPriceUpdatedAtMillis = active.maxOfOrNull { it.priceUpdatedAtMillis }?.takeIf { it > 0 },
        )
    }

    /**
     * Compares the current price with the oldest snapshot, so the UI can show a
     * simple "since you added it" trend without any network dependency.
     */
    fun priceChangePercent(snapshots: List<Long>): Double? {
        if (snapshots.size < 2) return null
        val first = snapshots.first()
        val last = snapshots.last()
        if (first <= 0) return null
        return (last - first) * 100.0 / first
    }
}
