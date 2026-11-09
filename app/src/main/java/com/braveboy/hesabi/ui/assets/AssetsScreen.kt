package com.braveboy.hesabi.ui.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.AssetKind
import com.braveboy.hesabi.ui.components.AmountText
import com.braveboy.hesabi.ui.components.ConfirmDialog
import com.braveboy.hesabi.ui.components.EmptyState
import com.braveboy.hesabi.ui.components.FilterChipRow
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.SectionHeader
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Composable
fun AssetsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AssetsViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Asset?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Asset?>(null) }
    var priceEditing by remember { mutableStateOf<Asset?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        HesabiTopBar(
            title = "دارایی‌ها",
            subtitle = "طلا، ارز، ملک و هر چیز غیرنقدی",
            onBack = onBack,
            actions = {
                IconButton(onClick = { creating = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "افزودن دارایی")
                }
            },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(HesabiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
        ) {
            state.message?.let { message ->
                item {
                    InfoBanner(
                        text = message,
                        actionLabel = "باشه",
                        onAction = viewModel::dismissNotice,
                    )
                }
            }
            state.error?.let { error ->
                item {
                    InfoBanner(
                        text = error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        actionLabel = "باشه",
                        onAction = viewModel::dismissNotice,
                    )
                }
            }
            item {
                PortfolioHeader(
                    totalValue = state.summary.totalValueToman,
                    totalCost = state.summary.totalCostToman,
                    profit = state.summary.profitToman,
                    profitPercent = state.summary.profitPercent,
                )
            }
            if (state.autoUpdateAvailable) {
                item {
                    InfoBanner(
                        text = if (state.syncing) {
                            "در حال گرفتن قیمت طلا و ارز از TGJU…"
                        } else {
                            "قیمت طلا و ارز را می‌توانی از منبع عمومی TGJU بگیر؛ منبع و ساعتش روی هر دارایی ثبت می‌شود."
                        },
                        actionLabel = if (state.syncing) null else "به‌روزرسانی",
                        onAction = viewModel::syncPrices,
                    )
                }
            }
            if (state.summary.allocations.isNotEmpty()) {
                item {
                    HesabiCard {
                        SectionHeader("ترکیب دارایی")
                        state.summary.allocations.forEach { allocation ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(allocation.kind.labelFa, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${(allocation.share * 100).toInt()}٪ — " +
                                        Money.format(allocation.valueToman, withUnit = false),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            if (state.assets.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = "دارایی‌ات را ثبت کن",
                        message = "طلا، دلار، خودرو یا ملک؛ ارزش کل و سود/زیانت یک‌جا دیده می‌شود.",
                        actionLabel = "افزودن دارایی",
                        onAction = { creating = true },
                    )
                }
            } else {
                items(state.assets, key = { it.id }) { asset ->
                    AssetRow(
                        asset = asset,
                        onEdit = { editing = asset },
                        onPrice = { priceEditing = asset },
                        onDelete = { deleting = asset },
                    )
                }
            }
            item {
                InfoBanner(
                    text = "قیمت‌ها را خودت وارد می‌کنی و منبع و زمان آخرین به‌روزرسانی کنار هر دارایی نمایش داده می‌شود.",
                    icon = Icons.Rounded.TrendingUp,
                )
            }
        }
    }

    if (creating || editing != null) {
        AssetEditorDialog(
            asset = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { asset ->
                viewModel.save(asset)
                creating = false
                editing = null
            },
            onDelete = editing?.let { asset ->
                {
                    viewModel.delete(asset)
                    editing = null
                }
            },
        )
    }

    priceEditing?.let { asset ->
        PriceUpdateDialog(
            asset = asset,
            onDismiss = { priceEditing = null },
            onSave = { price, source ->
                viewModel.updatePrice(asset, price, source)
                priceEditing = null
            },
        )
    }

    deleting?.let { asset ->
        ConfirmDialog(
            title = "حذف دارایی",
            message = "«${asset.name}» و تاریخچهٔ قیمتش حذف می‌شود. مطمئنی؟",
            confirmLabel = "حذف",
            dangerous = true,
            onConfirm = {
                viewModel.delete(asset)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun PortfolioHeader(
    totalValue: Long,
    totalCost: Long,
    profit: Long,
    profitPercent: Double?,
) {
    HesabiCard(containerColor = MaterialTheme.colorScheme.primary) {
        Text(
            text = "ارزش کل دارایی",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
        )
        AmountText(
            amountToman = totalValue,
            style = MaterialTheme.typography.displaySmall,
            hidden = null,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.lg)) {
            Column {
                Text(
                    "بهای خرید",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                )
                Text(
                    text = Money.format(totalCost, withUnit = false),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column {
                Text(
                    "سود / زیان",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                )
                Text(
                    text = (if (profit >= 0) "+" else "−") + Money.format(kotlin.math.abs(profit), withUnit = false),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
                profitPercent?.let { percent ->
                    Text(
                        text = "${percent.toInt()}٪",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetRow(
    asset: Asset,
    onEdit: () -> Unit,
    onPrice: () -> Unit,
    onDelete: () -> Unit,
) {
    HesabiCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(asset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "${formatQuantity(asset.quantityMilli)} ${asset.unitLabel} • ${asset.kind.labelFa}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                AmountText(amountToman = asset.currentValueToman, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (asset.profitToman >= 0) {
                        "+" + Money.format(asset.profitToman, withUnit = false)
                    } else {
                        "−" + Money.format(kotlin.math.abs(asset.profitToman), withUnit = false)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (asset.profitToman >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
        Text(
            text = "قیمت واحد: " + Money.format(asset.currentPriceTomanPerUnit) +
                " • " + asset.priceSourceLabel +
                if (asset.priceUpdatedAtMillis > 0) {
                    " • " + formatTimestamp(asset.priceUpdatedAtMillis)
                } else {
                    ""
                },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onPrice) { Text("به‌روزرسانی قیمت") }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "ویرایش")
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AssetEditorDialog(
    asset: Asset?,
    onDismiss: () -> Unit,
    onSave: (Asset) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(asset?.name.orEmpty()) }
    var kind by remember { mutableStateOf(asset?.kind ?: AssetKind.GOLD) }
    var quantityText by remember {
        mutableStateOf(asset?.let { formatQuantity(it.quantityMilli) }.orEmpty())
    }
    var unitLabel by remember { mutableStateOf(asset?.unitLabel ?: "گرم") }
    var buyPriceText by remember {
        mutableStateOf(asset?.buyPriceTomanPerUnit?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var currentPriceText by remember {
        mutableStateOf(asset?.currentPriceTomanPerUnit?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var note by remember { mutableStateOf(asset?.note.orEmpty()) }

    val quantity = parseQuantityMilli(quantityText)
    val buyPrice = Money.parseToman(buyPriceText)
    val currentPrice = Money.parseToman(currentPriceText)
    val valid = name.isNotBlank() && quantity != null && quantity > 0 && buyPrice != null && currentPrice != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (asset == null) "دارایی جدید" else "ویرایش دارایی") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
            ) {
                HesabiTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = "نام",
                    placeholder = "مثلاً سکهٔ تمام",
                )
                FilterChipRow(
                    options = AssetKind.entries,
                    selected = kind,
                    onSelect = { kind = it },
                    label = { it.labelFa },
                )
                HesabiTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.take(16) },
                    label = "مقدار",
                    error = if (quantityText.isNotBlank() && quantity == null) "عدد معتبر وارد کن." else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                HesabiTextField(
                    value = unitLabel,
                    onValueChange = { unitLabel = it.take(12) },
                    label = "واحد",
                    placeholder = "گرم، عدد، دلار…",
                )
                HesabiTextField(
                    value = buyPriceText,
                    onValueChange = { buyPriceText = it.take(18) },
                    label = "قیمت خرید هر واحد (تومان)",
                    error = if (buyPriceText.isNotBlank() && buyPrice == null) "مبلغ معتبر وارد کن." else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                HesabiTextField(
                    value = currentPriceText,
                    onValueChange = { currentPriceText = it.take(18) },
                    label = "قیمت فعلی هر واحد (تومان)",
                    error = if (currentPriceText.isNotBlank() && currentPrice == null) "مبلغ معتبر وارد کن." else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                HesabiTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = "یادداشت (اختیاری)",
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!valid) return@TextButton
                    onSave(
                        Asset(
                            id = asset?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            kind = kind,
                            quantityMilli = quantity ?: 0,
                            unitLabel = unitLabel.ifBlank { "واحد" },
                            buyPriceTomanPerUnit = buyPrice ?: 0,
                            currentPriceTomanPerUnit = currentPrice ?: 0,
                            priceUpdatedAtMillis = System.currentTimeMillis(),
                            priceSourceLabel = asset?.priceSourceLabel ?: "ورود دستی",
                            note = note.trim(),
                            archived = asset?.archived ?: false,
                            createdAtMillis = asset?.createdAtMillis ?: System.currentTimeMillis(),
                        ),
                    )
                },
                enabled = valid,
            ) { Text("ذخیره") }
        },
        dismissButton = {
            Row {
                onDelete?.let {
                    TextButton(onClick = it) {
                        Text("حذف", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        },
    )
}

@Composable
private fun PriceUpdateDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit,
) {
    var priceText by remember { mutableStateOf(asset.currentPriceTomanPerUnit.toString()) }
    var source by remember { mutableStateOf(asset.priceSourceLabel) }
    val price = Money.parseToman(priceText)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("قیمت جدید ${asset.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                HesabiTextField(
                    value = priceText,
                    onValueChange = { priceText = it.take(18) },
                    label = "قیمت هر ${asset.unitLabel} (تومان)",
                    error = if (priceText.isNotBlank() && price == null) "مبلغ معتبر وارد کن." else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                HesabiTextField(
                    value = source,
                    onValueChange = { source = it.take(30) },
                    label = "منبع قیمت",
                    placeholder = "مثلاً سایت طلا",
                )
                Text(
                    text = "زمان ثبت: امروز " + JalaliDate.today().formatShort(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { price?.let { onSave(it, source) } }, enabled = price != null) {
                Text("ثبت")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

private fun parseQuantityMilli(text: String): Long? {
    if (text.isBlank()) return null
    val normalized = Digits.toAscii(text).replace("٬", "").replace(",", "").replace("٫", ".").trim()
    return runCatching {
        BigDecimal(normalized).multiply(BigDecimal(1000))
            .setScale(0, RoundingMode.DOWN)
            .longValueExact()
    }.getOrNull()
}

private fun formatQuantity(quantityMilli: Long): String {
    val whole = quantityMilli / 1000
    val fraction = quantityMilli % 1000
    val text = if (fraction == 0L) {
        whole.toString()
    } else {
        "$whole." + fraction.toString().padStart(3, '0').trimEnd('0')
    }
    return Digits.toPersian(text)
}

private fun formatTimestamp(millis: Long): String = Instant.ofEpochMilli(millis)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()
    .let { JalaliDate.fromGregorian(it).formatShort() }
