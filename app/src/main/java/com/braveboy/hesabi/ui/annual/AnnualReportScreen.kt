package com.braveboy.hesabi.ui.annual

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.export.PdfReportWriter
import com.braveboy.hesabi.core.export.XlsxWriter
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.export.AnnualExportBuilder
import com.braveboy.hesabi.ui.components.AmountText
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiButtonVariant
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.SectionHeader
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun AnnualReportScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AnnualReportViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<String?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            val report = state.report ?: return@rememberLauncherForActivityResult
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val bytes = PdfReportWriter.write(
                            AnnualExportBuilder.pdf(
                                annual = report,
                                yearTransactions = state.yearTransactions,
                                categories = state.categories,
                                portfolio = state.portfolio,
                            ),
                            context,
                        )
                        writeBytes(context, uri, bytes)
                    }
                }.onSuccess {
                    notice = "PDF سال ${Digits.toPersian(report.year.toString())} ساخته شد."
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
    ) { uri ->
        if (uri != null) {
            val report = state.report ?: return@rememberLauncherForActivityResult
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val sheets = AnnualExportBuilder.xlsx(
                            annual = report,
                            yearTransactions = state.yearTransactions,
                            categories = state.categories,
                            portfolio = state.portfolio,
                        )
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            XlsxWriter.write(stream, sheets)
                        } ?: throw IOException("نوشتن فایل ممکن نشد.")
                    }
                }.onSuccess {
                    notice = "فایل اکسل سال ${Digits.toPersian(report.year.toString())} ساخته شد."
                }.onFailure {
                    notice = "ساخت اکسل انجام نشد: ${it.message.orEmpty()}"
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(HesabiSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
    ) {
        item {
            HesabiTopBar(
                title = "حساب سال من",
                subtitle = "مرور یک سال پول",
                onBack = onBack,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = viewModel::previousYear) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "سال قبل")
                }
                Text(
                    text = "سال ${Digits.toPersian(state.year.toString())}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = viewModel::nextYear) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "سال بعد")
                }
            }
        }
        notice?.let {
            item { InfoBanner(text = it, actionLabel = "باشه", onAction = { notice = null }) }
        }
        state.report?.let { report ->
            item {
                HesabiCard(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(
                        "ماندهٔ سال",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    )
                    AmountText(
                        amountToman = report.totalNetToman,
                        style = MaterialTheme.typography.displaySmall,
                        hidden = null,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.lg)) {
                        Column {
                            Text(
                                "درآمد",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                            )
                            Text(
                                Money.format(report.totalIncomeToman, withUnit = false),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Column {
                            Text(
                                "هزینه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                            )
                            Text(
                                Money.format(report.totalExpenseToman, withUnit = false),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Column {
                            Text(
                                "نرخ پس‌انداز",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                            )
                            Text(
                                "${Digits.toPersian(report.savingsRatePercent.toString())}٪",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
            item {
                HesabiCard {
                    SectionHeader("ماه‌به‌ماه")
                    val maxValue = maxOf(
                        1L,
                        report.months.maxOfOrNull { maxOf(it.incomeToman, it.expenseToman) } ?: 1L,
                    )
                    report.months.forEach { month ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                        ) {
                            Text(
                                text = month.label.substringBefore(' '),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(62.dp),
                                maxLines = 1,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((month.incomeToman.toFloat() / maxValue).coerceIn(0f, 1f))
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((month.expenseToman.toFloat() / maxValue).coerceIn(0f, 1f))
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.75f)),
                                )
                            }
                            Text(
                                text = Money.compact(month.netToman),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                HesabiCard {
                    SectionHeader("نکته‌های سال")
                    report.insights.forEach { insight ->
                        Text("• $insight", style = MaterialTheme.typography.bodySmall)
                    }
                    report.topCategory?.let {
                        Text(
                            "بیشترین دسته: ${it.name} — ${Money.format(it.amountToman)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    report.biggestExpense?.let {
                        Text(
                            "بزرگ‌ترین هزینه: ${Money.format(it.amountToman)} در ${it.occurredOn.formatShort()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                    HesabiButton(
                        text = "خروجی PDF",
                        onClick = {
                            pdfLauncher.launch("hesabi-year-${report.year}.pdf")
                        },
                        icon = Icons.Rounded.PictureAsPdf,
                        variant = HesabiButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                    )
                    HesabiButton(
                        text = "خروجی اکسل",
                        onClick = {
                            xlsxLauncher.launch("hesabi-year-${report.year}.xlsx")
                        },
                        icon = Icons.Rounded.TableChart,
                        variant = HesabiButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                InfoBanner(
                    text = "PDF با چیدمان راست‌به‌چپ ساخته می‌شود و فایل اکسل در Google Sheets هم باز می‌شود.",
                )
            }
        }
    }
}

private suspend fun writeBytes(context: Context, uri: android.net.Uri, bytes: ByteArray) {
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        ?: throw IOException("نوشتن فایل ممکن نشد.")
}
