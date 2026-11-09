package com.braveboy.hesabi.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.price.AssetPriceSyncManager
import com.braveboy.hesabi.domain.logic.AssetPortfolioCalculator
import com.braveboy.hesabi.domain.logic.PortfolioSummary
import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.AssetPriceSnapshot
import com.braveboy.hesabi.domain.model.AssetKind
import com.braveboy.hesabi.domain.repository.AssetRepository
import com.braveboy.hesabi.domain.repository.DiagnosticsRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
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
    val syncing: Boolean = false,
    val autoUpdateAvailable: Boolean = false,
)

class AssetsViewModel(
    private val assetRepository: AssetRepository,
    private val diagnosticsRepository: DiagnosticsRepository,
    private val priceSyncManager: AssetPriceSyncManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val notice = MutableStateFlow<Pair<String?, String?>>(null to null)
    private val syncing = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val settings = runCatching { settingsRepository.current() }.getOrNull()
            if (settings?.assetPricesAutoUpdate == true) syncPrices()
        }
    }

    private data class Inputs(val assets: List<Asset>, val snapshots: List<AssetPriceSnapshot>)

    private val inputs = combine(
        assetRepository.observeAll(),
        assetRepository.observeSnapshots(),
    ) { assets, snapshots -> Inputs(assets, snapshots) }

    val state: StateFlow<AssetsUiState> = combine(inputs, notice, syncing) { data, message, busy ->
        AssetsUiState(
            assets = data.assets,
            snapshots = data.snapshots.groupBy { it.assetId },
            summary = AssetPortfolioCalculator.summarize(data.assets),
            message = message.first,
            error = message.second,
            loading = false,
            syncing = busy,
            autoUpdateAvailable = data.assets.any {
                !it.archived && it.kind in setOf(AssetKind.GOLD, AssetKind.CURRENCY, AssetKind.CRYPTO)
            },
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

    /** 5.2: pull the public TGJU snapshot and label every changed price with its time. */
    fun syncPrices() {
        if (syncing.value) return
        syncing.value = true
        notice.value = null to null
        viewModelScope.launch {
            val result = runCatching { priceSyncManager.sync() }.getOrNull()
            syncing.value = false
            when {
                result == null -> notice.value = null to "به‌روزرسانی قیمت انجام نشد."
                result.hasError -> notice.value = null to (result.error ?: "منبع قیمت در دسترس نیست.")
                result.updated == 0 ->
                    notice.value = "قیمت خودکاری برای این دارایی‌ها پیدا نشد؛ آن را دستی وارد کن." to null
                else -> {
                    val count = Digits.toPersian(result.updated.toString())
                    val suffix = if (result.fromCache) " (از کش آفلاین)" else ""
                    notice.value = "$count قیمت از منبع TGJU به‌روز شد$suffix." to null
                    diagnosticsRepository.record("asset", "price_sync", "tgju")
                }
            }
        }
    }

    fun dismissNotice() {
        notice.value = null to null
    }
}
