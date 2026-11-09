package com.braveboy.hesabi.ui.receipt

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.core.receipt.ReceiptStorageManager
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.entry.ReceiptScanState
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Confirmation sheet for a scanned receipt. OCR is only a suggestion: the user can
 * fix the amount, the note and the category, and nothing is written before «استفاده».
 */
@Composable
fun ReceiptScanDialog(
    scan: ReceiptScanState,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onUse: (amountToman: Long, note: String, categoryId: String?) -> Unit,
) {
    val suggestedAmount = scan.result.amountToman
    var amountText by remember(scan.imagePath, scan.scanning) {
        mutableStateOf(suggestedAmount?.toString().orEmpty())
    }
    var note by remember(scan.imagePath, scan.scanning) {
        mutableStateOf(scan.result.merchant.orEmpty())
    }
    var categoryId by remember(scan.imagePath, scan.scanning) {
        mutableStateOf<String?>(null)
    }
    val parsedAmount = Money.parseToman(Digits.toAscii(amountText))
    val preview by produceState<ImageBitmap?>(initialValue = null, scan.imagePath) {
        value = withContext(Dispatchers.IO) { ReceiptStorageManager.loadBitmap(scan.imagePath) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اسکن رسید") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
            ) {
                if (scan.scanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("در حال خواندن رسید روی همین گوشی…")
                    }
                } else {
                    preview?.let { image ->
                        Image(
                            bitmap = image,
                            contentDescription = "تصویر رسید",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                    HesabiTextField(
                        value = amountText,
                        onValueChange = { input -> amountText = input.take(18) },
                        label = "مبلغ (تومان)",
                        placeholder = "مبلغ را با رسید مقایسه کن",
                        error = if (amountText.isNotBlank() && parsedAmount == null) {
                            "مبلغ معتبر وارد کن."
                        } else {
                            null
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                    )
                    HesabiTextField(
                        value = note,
                        onValueChange = { note = it.take(200) },
                        label = "فروشگاه یا یادداشت",
                        placeholder = "مثلاً سوپرمارکت",
                    )
                    if (categories.isNotEmpty()) {
                        Text(
                            text = "دسته",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
                            items(categories, key = { it.id }) { category ->
                                FilterChip(
                                    selected = categoryId == category.id,
                                    onClick = {
                                        categoryId = if (categoryId == category.id) null else category.id
                                    },
                                    label = { Text(category.name) },
                                )
                            }
                        }
                    }
                    scan.suggestion?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    scan.error?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        text = "هیچ داده‌ای بدون تأیید تو ثبت نمی‌شود.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (!scan.scanning) {
                HesabiButton(
                    text = "استفاده",
                    onClick = {
                        val amount = parsedAmount ?: return@HesabiButton
                        onUse(amount, note.trim(), categoryId)
                    },
                    enabled = (parsedAmount ?: 0L) > 0L,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لغو") }
        },
        modifier = Modifier.padding(horizontal = HesabiSpacing.xs),
    )
}
