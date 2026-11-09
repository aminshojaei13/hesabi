package com.braveboy.hesabi.ui.importer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.data.importer.ImportMapping
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.ui.components.HesabiButton
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

/**
 * Migration wizard: pick a CSV/XLSX, review the automatic column mapping, then import
 * with a per-row error report. Nothing is written before the final confirmation.
 */
@Composable
fun ImportScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ImportViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                            ?: throw IOException("فایل قابل خواندن نیست.")
                        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
                        name to bytes
                    }
                }.onSuccess { (name, bytes) ->
                    viewModel.loadFile(name, bytes)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        HesabiTopBar(
            title = "ورود از فایل",
            subtitle = "CSV یا اکسل رقبا و بانک‌ها",
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(HesabiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
        ) {
            item {
                HesabiButton(
                    text = if (state.fileName == null) "انتخاب فایل" else "انتخاب فایل دیگر",
                    onClick = {
                        picker.launch(
                            arrayOf(
                                "text/csv",
                                "text/comma-separated-values",
                                "text/plain",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/octet-stream",
                            ),
                        )
                    },
                    icon = Icons.Rounded.FileOpen,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.error?.let { error ->
                item {
                    InfoBanner(
                        text = error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        actionLabel = "بستن",
                        onAction = viewModel::dismissError,
                    )
                }
            }
            if (state.hasFile) {
                item {
                    HesabiCard {
                        SectionHeader(state.fileName ?: "فایل")
                        Text(
                            text = "${state.rowCount} سطر خوانده شد و ${state.headers.size} ستون دارد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "پیش‌نمایش:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        state.previewRows.take(3).forEachIndexed { index, row ->
                            Text(
                                text = "سطر ${index + 1}: " + row.joinToString(" | "),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                            )
                        }
                    }
                }
                item {
                    HesabiCard {
                        SectionHeader("نگاشت ستون‌ها", actionLabel = "خودکار", onAction = {})
                        Text(
                            text = "اگر ستونی اشتباه تشخیص داده شد، از منوی هر ردیف عوضش کن.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val mapping = state.mapping
                        MappingRow("تاریخ", state.headers, mapping.date) {
                            viewModel.updateMapping(mapping.copy(date = it))
                        }
                        MappingRow("مبلغ", state.headers, mapping.amount) {
                            viewModel.updateMapping(mapping.copy(amount = it))
                        }
                        MappingRow("نوع (هزینه/درآمد)", state.headers, mapping.type) {
                            viewModel.updateMapping(mapping.copy(type = it))
                        }
                        MappingRow("دسته", state.headers, mapping.category) {
                            viewModel.updateMapping(mapping.copy(category = it))
                        }
                        MappingRow("حساب", state.headers, mapping.account) {
                            viewModel.updateMapping(mapping.copy(account = it))
                        }
                        MappingRow("توضیح", state.headers, mapping.note) {
                            viewModel.updateMapping(mapping.copy(note = it))
                        }
                        MappingRow("ستون درآمد", state.headers, mapping.income) {
                            viewModel.updateMapping(mapping.copy(income = it))
                        }
                        MappingRow("ستون هزینه", state.headers, mapping.expense) {
                            viewModel.updateMapping(mapping.copy(expense = it))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ToggleRow(
                            title = "ساخت دستهٔ جدید",
                            subtitle = "دسته‌های ناشناخته ساخته شوند",
                            checked = state.createCategories,
                            onCheckedChange = viewModel::setCreateCategories,
                        )
                        ToggleRow(
                            title = "ساخت حساب جدید",
                            subtitle = "اگر نام حساب در فایل با حساب‌های تو یکی نبود",
                            checked = state.createAccounts,
                            onCheckedChange = viewModel::setCreateAccounts,
                        )
                    }
                }
                item {
                    HesabiButton(
                        text = if (state.busy) "در حال ورود..." else "ورود داده",
                        onClick = { viewModel.runImport() },
                        enabled = state.mapping.hasAmount && !state.busy,
                        loading = state.busy,
                        icon = Icons.Rounded.UploadFile,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!state.mapping.hasAmount) {
                        Text(
                            text = "برای ادامه، ستون مبلغ را انتخاب کن.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            state.summary?.let { summary ->
                item {
                    HesabiCard {
                        SectionHeader("گزارش ورود")
                        Text(summary.summaryText, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "دسته‌های ساخته‌شده: ${summary.createdCategories} • " +
                                "حساب‌های ساخته‌شده: ${summary.createdAccounts}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (summary.issues.isNotEmpty()) {
                            Text(
                                text = "جزئیات خطاها:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            summary.issues.forEach { issue ->
                                Text(
                                    text = "سطر ${issue.rowNumber}: ${issue.message}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
            item {
                InfoBanner(
                    text = "هیچ سطری بی‌صدا رد نمی‌شود: تعداد ثبت‌شده، تکراری و خطادار با شمارهٔ سطر گزارش می‌شود.",
                )
            }
        }
    }
}

@Composable
private fun MappingRow(
    label: String,
    headers: List<String>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selected?.let { headers.getOrNull(it) } ?: "بدون ستون")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("بدون ستون") },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    },
                )
                headers.forEachIndexed { index, header ->
                    DropdownMenuItem(
                        text = { Text(header.ifBlank { "ستون ${index + 1}" }) },
                        onClick = {
                            onSelect(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
