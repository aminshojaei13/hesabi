package com.braveboy.hesabi.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.domain.logic.AssetPortfolioCalculator
import com.braveboy.hesabi.domain.logic.PortfolioSummary
import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.AssetPriceSnapshot
import com.braveboy.hesabi.domain.repository.AssetRepository
import com.braveboy.hesabi.domain.repository.DiagnosticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AssetsUiState(
    val assets: List<Asset> = emptyList(),
    val snapshots: Map<String, List<AssetPriceSnapshot>> = emptyMap(),
    val summary: PortfolioSummary = AssetPortfolioCalculator.summarize(emptyList()),
    val message: String? = null,
    val error: String? = null,
    val loading: Boolean = true,
)

class AssetsViewModel(
    private val assetRepository: AssetRepository,
    private val diagnosticsRepository: DiagnosticsRepository,
) : ViewModel() {

    private val notice = MutableStateFlow<Pair<String?, String?>>(null to null)

    private data class Inputs(val assets: List<Asset>, val snapshots: List<AssetPriceSnapshot>)

    private val inputs = combine(
        assetRepository.observeAll(),
        assetRepository.observeSnapshots(),
    ) { assets, snapshots -> Inputs(assets, snapshots) }

    val state: StateFlow<AssetsUiState> = combine(inputs, notice) { data, message ->
        AssetsUiState(
            assets = data.assets,
            snapshots = data.snapshots.groupBy { it.assetId },
            summary = AssetPortfolioCalculator.summarize(data.assets),
            message = message.first,
            error = message.second,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssetsUiState())

    fun save(asset: Asset) {
        viewModelScope.launch {
            runCatching { assetRepository.save(asset) }
                .onSuccess {
                    notice.value = "دارایی ذخیره شد." to null
                    diagnosticsRepository.record("asset", "saved", asset.kind.name)
                }
                .onFailure { notice.value = null to "ذخیره نشد: ${it.message.orEmpty()}" }
        }
    }

    fun delete(asset: Asset) {
        viewModelScope.launch {
            runCatching { assetRepository.delete(asset.id) }
                .onSuccess { notice.value = "دارایی حذف شد." to null }
                .onFailure { notice.value = null to "حذف نشد: ${it.message.orEmpty()}" }
        }
    }

    fun updatePrice(asset: Asset, priceTomanPerUnit: Long, source: String) {
        viewModelScope.launch {
            runCatching {
                assetRepository.updatePrice(
                    assetId = asset.id,
                    priceTomanPerUnit = priceTomanPerUnit,
                    source = source.ifBlank { "ورود دستی" },
                    atMillis = System.currentTimeMillis(),
                )
            }.onSuccess {
                notice.value = "قیمت به‌روز شد." to null
            }.onFailure {
                notice.value = null to "به‌روزرسانی قیمت انجام نشد."
            }
        }
    }

    fun dismissNotice() {
        notice.value = null to null
    }
}
