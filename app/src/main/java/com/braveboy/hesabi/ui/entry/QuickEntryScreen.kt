package com.braveboy.hesabi.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.QuickTemplate
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.components.AccountChips
import com.braveboy.hesabi.ui.components.AmountEntryDisplay
import com.braveboy.hesabi.ui.components.CategoryGrid
import com.braveboy.hesabi.ui.components.FormSection
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.TransactionTypeToggle
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.receipt.ReceiptScanDialog
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import java.io.File

/**
 * The "حساب سریع" screen. Default path is: type amount, tap category, save — the
 * calculator keys and templates are optional accelerators, not extra steps.
 */
@Composable
fun QuickEntryScreen(
    container: AppContainer,
    initialType: TransactionType?,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenFullForm: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: QuickEntryViewModel = hesabiViewModel(container, initialType = initialType)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showTemplateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.scanReceipt(context, uri) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        val uri = pendingCaptureUri
        pendingCaptureUri = null
        if (saved && uri != null) viewModel.scanReceipt(context, uri)
    }

    fun captureReceipt() {
        runCatching {
            val directory = File(context.cacheDir, "captures").apply { mkdirs() }
            val file = File(directory, "receipt-${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        }.onSuccess { uri ->
            pendingCaptureUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    Column(modifier = modifier.fillMaxSize()) {
        HesabiTopBar(
            title = "ثبت سریع",
            subtitle = "مبلغ و دسته؛ همین!",
            onBack = onBack,
            actions = {
                TextButton(onClick = { onOpenFullForm(state.type) }) {
                    Text("فرم کامل")
                }
            },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(HesabiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
        ) {
            item {
                TransactionTypeToggle(type = state.type, onTypeChange = viewModel::setType)
            }
            item {
                AmountEntryDisplay(amountToman = state.amountToman, type = state.type)
            }
            if (state.type != TransactionType.TRANSFER) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                    ) {
                        HesabiButton(
                            text = "اسکن رسید",
                            onClick = { captureReceipt() },
                            variant = com.braveboy.hesabi.ui.components.HesabiButtonVariant.SECONDARY,
                            icon = Icons.Rounded.PhotoCamera,
                            modifier = Modifier.weight(1f),
                        )
                        HesabiButton(
                            text = "از گالری",
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            variant = com.braveboy.hesabi.ui.components.HesabiButtonVariant.SECONDARY,
                            icon = Icons.Rounded.Image,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (state.receiptImagePath != null) {
                item {
                    InfoBanner(text = "رسید به این تراکنش پیوست می‌شود.")
                }
            }
            if (state.expression.any { it in "+-*/÷×%٪" }) {
                item {
                    Text(
                        text = "محاسبه: ${Digits.toPersian(state.expression)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item {
                CalculatorPad(
                    onKey = { key ->
                        when (key) {
                            "⌫" -> viewModel.backspace()
                            "C" -> viewModel.clearAmount()
                            "=" -> Unit
                            else -> viewModel.append(key)
                        }
                    },
                )
            }
            if (state.templates.isNotEmpty()) {
                item {
                    FormSection(
                        title = "الگوهای پرتکرار",
                        subtitle = "یک لمس برای شروع",
                        modifier = Modifier,
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
                            items(state.templates, key = { it.id }) { template ->
                                TemplateChip(
                                    template = template,
                                    onClick = { viewModel.applyTemplate(template) },
                                    onDelete = { viewModel.deleteTemplate(template) },
                                )
                            }
                        }
                    }
                }
            }
            if (state.type == TransactionType.TRANSFER) {
                item {
                    FormSection(title = "از حساب") {
                        AccountChips(
                            accounts = state.accounts,
                            selectedId = state.accountId,
                            onSelect = viewModel::setAccount,
                        )
                    }
                }
                item {
                    FormSection(title = "به حساب") {
                        AccountChips(
                            accounts = state.accounts.filter { it.id != state.accountId },
                            selectedId = state.toAccountId,
                            onSelect = viewModel::setToAccount,
                        )
                    }
                }
            } else {
                item {
                    FormSection(
                        title = "دسته",
                        subtitle = if (state.type == TransactionType.EXPENSE) {
                            "با یک لمس انتخاب می‌شود."
                        } else {
                            "درآمد را هم دسته‌بندی کن."
                        },
                    ) {
                        CategoryGrid(
                            categories = state.filteredCategories,
                            selectedId = state.categoryId,
                            onSelect = viewModel::setCategory,
                        )
                    }
                }
                item {
                    FormSection(title = "حساب") {
                        AccountChips(
                            accounts = state.accounts,
                            selectedId = state.accountId,
                            onSelect = viewModel::setAccount,
                        )
                    }
                }
            }
            item {
                HesabiTextField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    label = "یادداشت (اختیاری)",
                    placeholder = "مثلاً قهوه",
                )
            }
            state.suggestion?.let { suggestion ->
                item {
                    InfoBanner(
                        text = suggestion,
                        actionLabel = "انتخاب کن",
                        onAction = viewModel::applySuggestedCategory,
                    )
                }
            }
            if (state.errors.isNotEmpty()) {
                item {
                    InfoBanner(
                        text = state.errors.joinToString("\n"),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    HesabiButton(
                        text = "ذخیرهٔ الگو",
                        onClick = { showTemplateDialog = true },
                        variant = com.braveboy.hesabi.ui.components.HesabiButtonVariant.SECONDARY,
                        enabled = state.amountToman > 0,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Surface(tonalElevation = 3.dp) {
            HesabiButton(
                text = if (state.saving) "در حال ثبت..." else "ثبت",
                onClick = viewModel::save,
                enabled = state.canSave && !state.saving,
                loading = state.saving,
                icon = Icons.Rounded.Check,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HesabiSpacing.lg, vertical = HesabiSpacing.md),
            )
        }
    }

    if (showTemplateDialog) {
        SaveTemplateDialog(
            onDismiss = { showTemplateDialog = false },
            onSave = { label ->
                viewModel.saveCurrentAsTemplate(label)
                showTemplateDialog = false
            },
        )
    }

    state.receiptScan?.let { scan ->
        ReceiptScanDialog(
            scan = scan,
            categories = state.filteredCategories,
            onDismiss = viewModel::dismissReceiptScan,
            onUse = viewModel::applyReceiptScan,
        )
    }

    if (state.duplicateOf != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDuplicate,
            title = { Text("تراکنش مشابه پیدا شد") },
            text = {
                Text(
                    "امروز تراکنشی با همین مبلغ و حساب ثبت شده است. " +
                        "اگر این یکی جداست، یک‌بار دیگر تأیید کن.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.save(force = true) }) {
                    Text("ثبت دوباره")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDuplicate) {
                    Text("انصراف")
                }
            },
        )
    }
}

@Composable
private fun CalculatorPad(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "−"),
        listOf("0", "00", "%", "+"),
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    CalculatorKey(
                        label = when (key) {
                            "÷" -> "÷"
                            "×" -> "×"
                            "−" -> "−"
                            "%" -> "٪"
                            else -> Digits.toPersian(key)
                        },
                        onClick = {
                            onKey(
                                when (key) {
                                    "÷" -> "/"
                                    "×" -> "*"
                                    "−" -> "-"
                                    "%" -> "%"
                                    else -> key
                                },
                            )
                        },
                        modifier = Modifier.weight(1f),
                        isOperator = key in listOf("÷", "×", "−", "%", "+"),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalculatorKey(
                label = "پاک",
                onClick = { onKey("C") },
                modifier = Modifier.weight(1f),
                isOperator = true,
            )
            CalculatorKey(
                label = "",
                icon = true,
                onClick = { onKey("⌫") },
                modifier = Modifier.weight(1f),
                isOperator = true,
            )
            CalculatorKey(
                label = "=",
                onClick = { onKey("=") },
                modifier = Modifier.weight(2f),
                isOperator = true,
            )
        }
    }
}

@Composable
private fun CalculatorKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    icon: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (isOperator) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon) {
                Icon(
                    Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "پاک کردن یک رقم",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TemplateChip(
    template: QuickTemplate,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent) {
                Row(
                    modifier = Modifier.padding(start = HesabiSpacing.md, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(template.label, style = MaterialTheme.typography.labelMedium)
                    if (template.amountToman > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = com.braveboy.hesabi.core.money.Money.compact(template.amountToman),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "حذف الگو",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SaveTemplateDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var label by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ذخیرهٔ الگو") },
        text = {
            HesabiTextField(
                value = label,
                onValueChange = { label = it.take(24) },
                label = "نام الگو",
                placeholder = "مثلاً قهوهٔ صبح",
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(label) }, enabled = label.isNotBlank()) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}
