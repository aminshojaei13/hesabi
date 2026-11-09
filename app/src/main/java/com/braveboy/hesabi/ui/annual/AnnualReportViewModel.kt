package com.braveboy.hesabi.ui.annual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.domain.logic.AnnualReport
import com.braveboy.hesabi.domain.logic.AnnualReportCalculator
import com.braveboy.hesabi.domain.logic.AssetPortfolioCalculator
import com.braveboy.hesabi.domain.logic.PortfolioSummary
import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.repository.AssetRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AnnualUiState(
    val year: Int = JalaliDate.today().year,
    val report: AnnualReport? = null,
    val categories: List<Category> = emptyList(),
    val yearTransactions: List<Transaction> = emptyList(),
    val assets: List<Asset> = emptyList(),
    val portfolio: PortfolioSummary = AssetPortfolioCalculator.summarize(emptyList()),
    val loading: Boolean = true,
)

class AnnualReportViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    assetRepository: AssetRepository,
) : ViewModel() {

    private val year = MutableStateFlow(JalaliDate.today().year)

    val state: StateFlow<AnnualUiState> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        assetRepository.observeAll(),
        year,
    ) { transactions, categories, assets, selectedYear ->
        val yearTransactions = transactions.filter { it.occurredOn.year == selectedYear }
        AnnualUiState(
            year = selectedYear,
            report = AnnualReportCalculator.build(selectedYear, transactions, categories),
            categories = categories,
            yearTransactions = yearTransactions,
            assets = assets,
            portfolio = AssetPortfolioCalculator.summarize(assets),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnnualUiState())

    fun previousYear() {
        year.value -= 1
    }

    fun nextYear() {
        if (year.value < JalaliDate.today().year + 1) year.value += 1
    }
}
