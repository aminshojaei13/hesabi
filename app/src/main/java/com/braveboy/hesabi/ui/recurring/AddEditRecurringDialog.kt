package com.braveboy.hesabi.ui.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountKind
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.RecurringFrequency
import com.braveboy.hesabi.domain.model.RecurringTransaction
import com.braveboy.hesabi.domain.model.ThemeMode
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

@Composable
fun AddEditRecurringDialog(
    initial: RecurringTransaction?,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        type: TransactionType,
        amountToman: Long,
        accountId: String,
        toAccountId: String?,
        categoryId: String?,
        frequency: RecurringFrequency,
        interval: Int,
        dayOfMonth: Int?,
        startEpochDay: Long,
        autoApply: Boolean,
        note: String,
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf(if (initial != null) initial.amountToman.toString() else "") }
    var selectedAccountId by remember { mutableStateOf(initial?.accountId ?: accounts.firstOrNull()?.id.orEmpty()) }
    var selectedToAccountId by remember { mutableStateOf(initial?.toAccountId ?: accounts.getOrNull(1)?.id) }
    var selectedCategoryId by remember { mutableStateOf(initial?.categoryId) }
    var frequency by remember { mutableStateOf(initial?.frequency ?: RecurringFrequency.MONTHLY) }
    var dayOfMonth by remember { mutableIntStateOf(initial?.dayOfMonth ?: JalaliDate.today().day) }
    var autoApply by remember { mutableStateOf(initial?.autoApply ?: true) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }

    val parsedAmount = Digits.toAscii(amountText).filter { it.isDigit() }.toLongOrNull() ?: 0L

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = if (initial == null) "تراکنش دوره‌ای جدید" else "ویرایش تراکنش دوره‌ای",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
                ) {
                    // Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                    ) {
                        TransactionType.entries.forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.label) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Title
                    HesabiTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "عنوان (مثلاً اجاره، اینترنت)",
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Amount
                    HesabiTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = "مبلغ (تومان)",
                        placeholder = "مثلاً ۵۰۰,۰۰۰",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (parsedAmount > 0) {
                        Text(
                            text = Money.format(parsedAmount, withUnit = true),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // Frequency selector
                    Text("دوره تکرار:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                    ) {
                        RecurringFrequency.entries.forEach { f ->
                            FilterChip(
                                selected = frequency == f,
                                onClick = { frequency = f },
                                label = { Text(f.labelFa) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // If Monthly, day of month picker
                    if (frequency == RecurringFrequency.MONTHLY) {
                        HesabiTextField(
                            value = dayOfMonth.toString(),
                            onValueChange = { dayOfMonth = it.toIntOrNull()?.coerceIn(1, 31) ?: 1 },
                            label = "روز ماه شمسی (۱ تا ۳۱)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Account Selection
                    Text("حساب مبدأ:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                    ) {
                        accounts.take(4).forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text(acc.name) },
                            )
                        }
                    }

                    // Category Selection (for Income/Expense)
                    if (type != TransactionType.TRANSFER) {
                        val filteredCategories = categories.filter {
                            (type == TransactionType.EXPENSE && it.kind == CategoryKind.EXPENSE) ||
                                (type == TransactionType.INCOME && it.kind == CategoryKind.INCOME)
                        }
                        if (filteredCategories.isNotEmpty()) {
                            Text("دسته‌بندی:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                            ) {
                                filteredCategories.take(4).forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategoryId == cat.id,
                                        onClick = { selectedCategoryId = cat.id },
                                        label = { Text(cat.name) },
                                    )
                                }
                            }
                        }
                    }

                    // Auto apply toggle
                    Surface(
                        shape = RoundedCornerShape(HesabiRadius.input),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(HesabiSpacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ثبت خودکار در موعد", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    "در تاریخ سررسید بدون نیاز به تایید در حساب ثبت شود",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = autoApply, onCheckedChange = { autoApply = it })
                        }
                    }

                    // Note
                    HesabiTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "یادداشت (اختیاری)",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                HesabiButton(
                    text = "ذخیره",
                    onClick = {
                        val startEpoch = initial?.nextOccurrenceEpochDay ?: JalaliDate.today().toEpochDay()
                        onSave(
                            title,
                            type,
                            parsedAmount,
                            selectedAccountId,
                            if (type == TransactionType.TRANSFER) selectedToAccountId else null,
                            if (type != TransactionType.TRANSFER) selectedCategoryId else null,
                            frequency,
                            1,
                            if (frequency == RecurringFrequency.MONTHLY) dayOfMonth else null,
                            startEpoch,
                            autoApply,
                            note,
                        )
                    },
                )
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "حذف",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                }
            },
        )
    }
}

@Preview(name = "Light Mode")
@Composable
private fun AddEditRecurringDialogPreviewLight() {
    HesabiTheme(themeMode = ThemeMode.LIGHT) {
        AddEditRecurringDialog(
            initial = null,
            accounts = listOf(
                Account("1", "بلوبانک", AccountKind.BANK, 0xFF2563EB, 1000000),
                Account("2", "کیف پول", AccountKind.CASH, 0xFF10B981, 200000),
            ),
            categories = listOf(
                Category("1", "مسکن و اجاره", CategoryKind.EXPENSE, "home", 0xFF6366F1),
                Category("2", "حقوق", CategoryKind.INCOME, "work", 0xFF10B981),
            ),
            onDismiss = {},
            onSave = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
        )
    }
}

@Preview(name = "Dark Mode")
@Composable
private fun AddEditRecurringDialogPreviewDark() {
    HesabiTheme(themeMode = ThemeMode.DARK) {
        AddEditRecurringDialog(
            initial = RecurringTransaction(
                id = "1",
                title = "اجاره آپارتمان",
                type = TransactionType.EXPENSE,
                amountToman = 12_000_000L,
                accountId = "1",
                frequency = RecurringFrequency.MONTHLY,
                dayOfMonth = 1,
                nextOccurrenceEpochDay = JalaliDate.today().toEpochDay(),
                autoApply = true,
                isActive = true,
            ),
            accounts = listOf(Account("1", "بلوبانک", AccountKind.BANK, 0xFF2563EB, 1000000)),
            categories = listOf(Category("1", "مسکن", CategoryKind.EXPENSE, "home", 0xFF6366F1)),
            onDismiss = {},
            onSave = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onDelete = {},
        )
    }
}
