package com.braveboy.hesabi.ui.reports

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.logic.ReportCalculator
import com.braveboy.hesabi.ui.components.AmountText
import com.braveboy.hesabi.ui.components.BudgetProgressCard
import com.braveboy.hesabi.ui.components.CategoryBreakdown
import com.braveboy.hesabi.ui.components.ComparisonBadge
import com.braveboy.hesabi.ui.components.DailyBarChart
import com.braveboy.hesabi.ui.components.DonutChart
import com.braveboy.hesabi.ui.components.EmptyState
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiButtonVariant
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.core.export.PdfReportWriter
import com.braveboy.hesabi.core.export.XlsxWriter
import com.braveboy.hesabi.domain.export.ExportData
import com.braveboy.hesabi.domain.export.ReportExportBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.LoadingSkeleton
import com.braveboy.hesabi.ui.components.MonthSwitcher
import com.braveboy.hesabi.ui.components.SectionHeader
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme
import com.braveboy.hesabi.ui.theme.LocalHesabiSemantic
import kotlin.math.max

@Composable
fun ReportsScreen(
    container: AppContainer,
    onOpenBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ReportsViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<String?>(null) }

    fun exportData(): ExportData? {
        val report = state.report ?: return null
        return ExportData(
            period = state.period,
            report = report,
            transactions = state.transactions,
            categories = state.categories,
            accounts = state.accounts,
            budgets = state.budgets,
        )
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri: Uri? ->
        if (uri != null) {
            val data = exportData() ?: return@rememberLauncherForActivityResult
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val bytes = PdfReportWriter.write(ReportExportBuilder.pdf(data), context)
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: throw IOException("نوشتن فایل ممکن نشد.")
                    }
                }.onSuccess {
                    notice = "PDF گزارش ساخته شد."
                }.onFailure {
                    notice = "ساخت PDF انجام نشد: ${it.message.orEmpty()}"
                }
            }
        }
    }

    val xlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ),
    ) { uri: Uri? ->
        if (uri != null) {
            val data = exportData() ?: return@rememberLauncherForActivityResult
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            XlsxWriter.write(stream, ReportExportBuilder.xlsx(data))
                        } ?: throw IOException("نوشتن فایل ممکن نشد.")
                    }
                }.onSuccess {
                    notice = "فایل اکسل گزارش ساخته شد."
                }.onFailure {
                    notice = "ساخت اکسل انجام نشد: ${it.message.orEmpty()}"
                }
            }
        }
    }

    ReportsContent(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onCurrentMonth = viewModel::currentMonth,
        onOpenBudget = onOpenBudget,
        onExportPdf = { pdfLauncher.launch("hesabi-report-${state.period.key}.pdf") },
        onExportXlsx = { xlsxLauncher.launch("hesabi-report-${state.period.key}.xlsx") },
        notice = notice,
        onDismissNotice = { notice = null },
        modifier = modifier,
    )
}

@Composable
fun ReportsContent(
    state: ReportsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onOpenBudget: () -> Unit,
    onExportPdf: () -> Unit = {},
    onExportXlsx: () -> Unit = {},
    notice: String? = null,
    onDismissNotice: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val report = state.report
    val semantic = LocalHesabiSemantic.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HesabiSpacing.lg,
            end = HesabiSpacing.lg,
            top = HesabiSpacing.md,
            bottom = 108.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.lg),
    ) {
        item {
            Text(
                text = "گزارش",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = HesabiSpacing.xs),
            )
            MonthSwitcher(
                period = state.period,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onGoToCurrentMonth = onCurrentMonth,
                currentPeriod = MonthPeriod.current(),
            )
        }
        if (state.loading) {
            item { LoadingSkeleton(rows = 5) }
            return@LazyColumn
        }
        if (report == null || (report.transactionCount == 0 && report.totalExpenseToman == 0L)) {
            item {
                EmptyState(
                    title = "گزارشی برای این ماه نیست",
                    message = "با ثبت هزینه و درآمد، نمودارها و مقایسهٔ ماهانه ساخته می‌شوند.",
                    icon = Icons.Rounded.Insights,
                )
            }
        }
        report?.let { monthReport ->
            item {
                HesabiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text(
                                "هزینهٔ ماه",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            AmountText(
                                amountToman = monthReport.totalExpenseToman,
                                type = com.braveboy.hesabi.domain.model.TransactionType.EXPENSE,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "درآمد",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = Money.compact(monthReport.totalIncomeToman),
                                style = MaterialTheme.typography.titleMedium,
                                color = semantic.income,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.height(HesabiSpacing.sm))
                    ComparisonBadge(deltaPercent = monthReport.expenseDeltaPercent)
                    Spacer(Modifier.height(HesabiSpacing.sm))
                    Text(
                        text = "ماندهٔ ماه: " + Money.format(monthReport.netToman, signed = true),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (monthReport.netToman >= 0) semantic.income else semantic.expense,
                    )
                }
            }
            item {
                HesabiCard {
                    SectionHeader("سهم دسته‌ها")
                    if (monthReport.slices.isEmpty()) {
                        Text(
                            text = "این ماه هزینه‌ای ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        DonutChart(
                            slices = monthReport.slices,
                            centerLabel = "هزینه",
                            centerValue = Money.compact(monthReport.totalExpenseToman),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(HesabiSpacing.md))
                        CategoryBreakdown(
                            slices = monthReport.slices,
                            totalToman = monthReport.totalExpenseToman,
                        )
                    }
                }
            }
            item {
                HesabiCard {
                    SectionHeader("روند روزانه")
                    DailyBarChart(points = monthReport.daily)
                }
            }
            item {
                BudgetProgressCard(
                    spentToman = monthReport.budgetSpentToman,
                    limitToman = monthReport.budgetLimitToman,
                    title = "بودجهٔ ${monthReport.period.label}",
                    onEdit = onOpenBudget,
                )
            }
            item {
                HesabiCard {
                    SectionHeader("خروجی گزارش")
                    Text(
                        text = "PDF برای چاپ و اشتراک، اکسل برای باز کردن در Google Sheets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                        HesabiButton(
                            text = "PDF",
                            onClick = onExportPdf,
                            icon = Icons.Rounded.PictureAsPdf,
                            variant = HesabiButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f),
                        )
                        HesabiButton(
                            text = "Excel",
                            onClick = onExportXlsx,
                            icon = Icons.Rounded.TableChart,
                            variant = HesabiButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        notice?.let { text ->
            item { InfoBanner(text = text, actionLabel = "باشه", onAction = onDismissNotice) }
        }
        if (state.trend.any { it.expenseToman > 0 }) {
            item {
                HesabiCard {
                    SectionHeader("شش ماه اخیر")
                    MonthlyTrend(state.trend)
                }
            }
        }
        item {
            Text(
                text = "انتقال بین حساب‌ها در درآمد و هزینه حساب نمی‌شود.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthlyTrend(points: List<MonthPoint>) {
    val max = max(1L, points.maxOfOrNull { it.expenseToman } ?: 0L)
    Column(verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
        points.forEach { point ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
            ) {
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1.1f),
                    maxLines = 1,
                )
                LinearProgressIndicator(
                    progress = { (point.expenseToman.toFloat() / max).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(2f).height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    text = Money.compact(point.expenseToman),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(name = "گزارش", heightDp = 1000, showBackground = true)
@Composable
private fun ReportsPreview() {
    val period = MonthPeriod.of(PreviewData.today)
    val report = remember {
        ReportCalculator.build(
            period = period,
            transactions = PreviewData.transactions,
            categories = PreviewData.categories,
            previousTransactions = PreviewData.transactions.map {
                it.copy(occurredOnEpochDay = it.occurredOnEpochDay - 30, amountToman = (it.amountToman * 0.8).toLong())
            },
            budgets = emptyList(),
        ).copy(budgetLimitToman = 6_000_000, budgetSpentToman = 3_200_000)
    }
    val trend = listOf(
        "خرداد" to 2_400_000L,
        "تیر" to 3_100_000L,
        "مرداد" to 2_700_000L,
        "شهریور" to 4_200_000L,
        "مهر" to 3_600_000L,
        "آبان" to report.totalExpenseToman,
    ).map { MonthPoint(it.first, it.second, 20_000_000) }
    val state = ReportsUiState(
        period = period,
        report = report,
        trend = trend,
        transactionCount = report.transactionCount,
        loading = false,
    )
    HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) { ReportsContent(state, {}, {}, {}, {}) }
        }
    }
}

@Preview(
    name = "گزارش - خالی",
    heightDp = 800,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun ReportsEmptyPreview() {
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                ReportsContent(ReportsUiState(loading = false), {}, {}, {}, {})
            }
        }
    }
}
