package com.braveboy.hesabi.ui.sms

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.SmsEvent
import com.braveboy.hesabi.domain.model.SmsStatus
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.components.AccountChips
import com.braveboy.hesabi.ui.components.CategoryChipRow
import com.braveboy.hesabi.ui.components.ConfirmDialog
import com.braveboy.hesabi.ui.components.DateField
import com.braveboy.hesabi.ui.components.EmptyState
import com.braveboy.hesabi.ui.components.FilterChipRow
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.JalaliDatePickerDialog
import com.braveboy.hesabi.ui.components.SectionHeader
import com.braveboy.hesabi.ui.components.SmsImportDialog
import com.braveboy.hesabi.ui.components.SmsReviewCard
import com.braveboy.hesabi.ui.components.TransactionTypeToggle
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

@Composable
fun SmsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    initialText: String? = null,
    onInitialTextConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: SmsViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showImport by remember { mutableStateOf(false) }
    var importSeed by remember { mutableStateOf("") }
    var accepting by remember { mutableStateOf<SmsEvent?>(null) }
    var showClear by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.setSmsCapture(true)
        } else {
            viewModel.showError("بدون اجازهٔ پیامک، ثبت خودکار ممکن نیست. می‌توانی متن را دستی بچسبانی.")
        }
    }

    LaunchedEffect(initialText) {
        if (!initialText.isNullOrBlank()) {
            importSeed = initialText
            showImport = true
            onInitialTextConsumed()
        }
    }

    SmsContent(
        state = state,
        onBack = onBack,
        onImport = {
            importSeed = ""
            showImport = true
        },
        onToggleCapture = { enabled ->
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED
            if (enabled && !granted) {
                permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
            } else {
                viewModel.setSmsCapture(enabled)
            }
        },
        onAccept = { accepting = it },
        onIgnore = viewModel::ignore,
        onIgnoreSender = viewModel::ignoreSender,
        onBulkAccept = viewModel::bulkAccept,
        onClearAll = { showClear = true },
        onDismissNotice = viewModel::dismissNotice,
        modifier = modifier,
    )

    if (showImport) {
        SmsImportDialog(
            initialText = importSeed,
            onDismiss = { showImport = false },
            onImport = { text ->
                viewModel.importText(text)
                showImport = false
            },
        )
    }

    accepting?.let { event ->
        SmsAcceptDialog(
            event = event,
            accounts = state.accounts,
            categories = state.categories,
            defaultAccountId = state.defaultAccount?.id,
            onDismiss = { accepting = null },
            onConfirm = { type, amount, accountId, categoryId, epochDay, note ->
                viewModel.accept(event, type, amount, accountId, categoryId, epochDay, note)
                accepting = null
            },
        )
    }

    if (showClear) {
        ConfirmDialog(
            title = "پاک کردن تاریخچه",
            message = "همهٔ پیامک‌های ذخیره‌شده (در انتظار و تاریخچه) حذف می‌شوند. تراکنش‌های ثبت‌شده دست نمی‌خورند.",
            confirmLabel = "پاک کن",
            dangerous = true,
            onConfirm = {
                viewModel.clearAll()
                showClear = false
            },
            onDismiss = { showClear = false },
        )
    }
}

@Composable
fun SmsContent(
    state: SmsUiState,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onToggleCapture: (Boolean) -> Unit,
    onAccept: (SmsEvent) -> Unit,
    onIgnore: (SmsEvent) -> Unit,
    onIgnoreSender: (String) -> Unit,
    onBulkAccept: () -> Unit,
    onClearAll: () -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val autoCategory = { type: TransactionType ->
        val kind = if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        state.categories.firstOrNull { it.kind == kind }?.name
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HesabiSpacing.lg,
            end = HesabiSpacing.lg,
            top = HesabiSpacing.sm,
            bottom = HesabiSpacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
    ) {
        item {
            HesabiTopBar(
                title = "پیامک‌های بانکی",
                subtitle = "بدون خواندن دفتر پیامک",
                onBack = onBack,
                actions = {
                    androidx.compose.material3.IconButton(onClick = onImport) {
                        androidx.compose.material3.Icon(Icons.Rounded.Add, contentDescription = "چسباندن پیامک")
                    }
                },
            )
        }
        if (state.error != null || state.message != null) {
            item {
                InfoBanner(
                    text = state.error ?: state.message.orEmpty(),
                    containerColor = if (state.error != null) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (state.error != null) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    actionLabel = "باشه",
                    onAction = onDismissNotice,
                )
            }
        }
        item {
            HesabiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("دریافت خودکار پیامک", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (state.settings.smsCaptureEnabled) {
                                "فعال است؛ فقط پیامک‌های بانکی به صندوق بررسی می‌روند."
                            } else {
                                "خاموش است. می‌توانی متن پیامک را دستی بچسبانی."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.settings.smsCaptureEnabled,
                        onCheckedChange = onToggleCapture,
                    )
                }
            }
        }
        if (state.pending.isEmpty()) {
            item {
                EmptyState(
                    title = "پیامکی در انتظار نیست",
                    message = "پیامک بانکی را بچسبان یا دریافت خودکار را روشن کن. هیچ پیامکی بدون تأیید تو ثبت نمی‌شود.",
                    icon = Icons.Rounded.Sms,
                    actionLabel = "چسباندن پیامک",
                    onAction = onImport,
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionHeader(
                        title = "در انتظار تأیید (${state.pending.size})",
                        modifier = Modifier.weight(1f),
                    )
                    if (state.pending.any { it.confidence >= 0.7f && it.amountToman != null }) {
                        TextButton(onClick = onBulkAccept, enabled = !state.busy) {
                            Text("ثبت همه", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            items(state.pending, key = { it.id }) { event ->
                SmsReviewCard(
                    event = event,
                    categoryName = event.type?.let(autoCategory),
                    onAccept = { onAccept(event) },
                    onIgnore = { onIgnore(event) },
                    onIgnoreSender = { onIgnoreSender(event.sender) },
                )
            }
        }
        if (state.recent.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = HesabiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionHeader(title = "تاریخچه", modifier = Modifier.weight(1f))
                    TextButton(onClick = onClearAll) {
                        androidx.compose.material3.Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.padding(end = HesabiSpacing.xxs),
                        )
                        Text("پاک کردن", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            items(state.recent.filter { it.status != SmsStatus.PENDING }, key = { "recent-${it.id}" }) { event ->
                SmsHistoryRow(event)
            }
        }
        item {
            InfoBanner(
                text = "متن پیامک فقط روی همین گوشی ذخیره می‌شود و هیچ‌جا ارسال نمی‌شود.",
            )
        }
    }
}

@Composable
private fun SmsHistoryRow(event: SmsEvent) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(HesabiSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.sender,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = event.merchant ?: event.body.take(40),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            event.amountToman?.let {
                Text(
                    text = Money.compact(it),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = when (event.status) {
                        SmsStatus.IMPORTED -> "ثبت شد"
                        SmsStatus.IGNORED -> "نادیده"
                        SmsStatus.PENDING -> "در انتظار"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SmsAcceptDialog(
    event: SmsEvent,
    accounts: List<com.braveboy.hesabi.domain.model.Account>,
    categories: List<com.braveboy.hesabi.domain.model.Category>,
    defaultAccountId: String?,
    onDismiss: () -> Unit,
    onConfirm: (TransactionType, Long, String, String?, Long, String) -> Unit,
) {
    var type by remember { mutableStateOf(event.type ?: TransactionType.EXPENSE) }
    var amountText by remember {
        mutableStateOf(event.amountToman?.let { Money.format(it, withUnit = false) }.orEmpty())
    }
    var accountId by remember { mutableStateOf(defaultAccountId ?: accounts.firstOrNull()?.id.orEmpty()) }
    var categoryId by remember {
        mutableStateOf(
            categories.firstOrNull {
                it.kind == if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
            }?.id,
        )
    }
    var epochDay by remember { mutableStateOf(event.occurredOnEpochDay) }
    var note by remember { mutableStateOf(event.merchant ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    val amount = Money.parseToman(amountText)
    val relevantCategories = categories.filter {
        it.kind == if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت از پیامک") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                TransactionTypeToggle(
                    type = type,
                    onTypeChange = { newType ->
                        type = newType
                        categoryId = categories.firstOrNull {
                            it.kind == if (newType == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
                        }?.id
                    },
                )
                HesabiTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "مبلغ (تومان)",
                    error = if (amountText.isNotBlank() && amount == null) "مبلغ قابل خواندن نیست." else null,
                )
                Text("حساب", style = MaterialTheme.typography.labelMedium)
                FilterChipRow(
                    options = accounts,
                    selected = accounts.firstOrNull { it.id == accountId },
                    onSelect = { accountId = it.id },
                    label = { it.name },
                )
                Text("دسته", style = MaterialTheme.typography.labelMedium)
                CategoryChipRow(
                    categories = relevantCategories,
                    selectedId = categoryId,
                    onSelect = { categoryId = it.id },
                )
                DateField(date = JalaliDate.fromEpochDay(epochDay), onClick = { showDatePicker = true })
                HesabiTextField(value = note, onValueChange = { note = it }, label = "توضیح")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val safeAmount = amount ?: return@TextButton
                    onConfirm(type, safeAmount, accountId, categoryId, epochDay, note)
                },
                enabled = amount != null && amount > 0 && accountId.isNotBlank() && categoryId != null,
            ) { Text("ثبت") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = JalaliDate.fromEpochDay(epochDay),
            onDismiss = { showDatePicker = false },
            onConfirm = {
                epochDay = it.toEpochDay()
                showDatePicker = false
            },
        )
    }
}

@Preview(name = "پیامک‌ها", heightDp = 1000, showBackground = true)
@Composable
private fun SmsPreview() {
    val state = SmsUiState(
        pending = listOf(PreviewData.sms, PreviewData.smsLowConfidence),
        recent = listOf(
            PreviewData.sms.copy(id = "r1", status = SmsStatus.IMPORTED),
            PreviewData.sms.copy(id = "r2", status = SmsStatus.IGNORED),
        ),
        accounts = PreviewData.accounts,
        categories = PreviewData.categories,
        settings = com.braveboy.hesabi.domain.model.AppSettings(smsCaptureEnabled = true, defaultAccountId = "acc_card"),
        loading = false,
    )
    HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                SmsContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
    }
}

@Preview(
    name = "پیامک‌ها - خالی",
    heightDp = 800,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun SmsEmptyPreview() {
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                SmsContent(
                    SmsUiState(accounts = PreviewData.accounts, categories = PreviewData.categories, loading = false),
                    {}, {}, {}, {}, {}, {}, {}, {}, {},
                )
            }
        }
    }
}
