package com.braveboy.hesabi.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountKind
import com.braveboy.hesabi.domain.model.ThemeMode
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

@Composable
fun SalarySetupDialog(
    initialSalaryToman: Long,
    initialPayday: Int,
    initialAccountId: String?,
    initialAutoDeposit: Boolean,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (salaryToman: Long, payday: Int, accountId: String?, autoDeposit: Boolean) -> Unit,
) {
    var salaryText by remember { mutableStateOf(if (initialSalaryToman > 0) initialSalaryToman.toString() else "") }
    var payday by remember { mutableIntStateOf(initialPayday.coerceIn(1, 31)) }
    var selectedAccountId by remember { mutableStateOf(initialAccountId ?: accounts.firstOrNull()?.id) }
    var autoDeposit by remember { mutableStateOf(initialAutoDeposit) }

    val parsedSalary = Money.parseToman(salaryText) ?: 0L

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "حقوق ماهیانه و کنترل نقدینگی",
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
                    Text(
                        text = "با ثبت حقوق ماهانه، اپلیکیشن نقدینگی خالص پس از کسر تعهدات و اقساط را محاسبه کرده و سقف خرج امن روزانه را دقیق‌تر تخمین می‌زند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Salary Input
                    HesabiTextField(
                        value = salaryText,
                        onValueChange = { salaryText = it },
                        label = "مبلغ حقوق ماهانه (تومان)",
                        placeholder = "مثلاً ۲۵,۰۰۰,۰۰۰",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        error = if (salaryText.isNotBlank() && parsedSalary <= 0) {
                            "مبلغ حقوق را معتبر وارد کن یا خالی بگذار."
                        } else {
                            null
                        },
                    )
                    if (parsedSalary > 0) {
                        Text(
                            text = Money.format(parsedSalary, withUnit = true),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Payday Selection
                    Text("روز دریافت حقوق در ماه:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items((1..31).toList()) { d ->
                            FilterChip(
                                selected = payday == d,
                                onClick = { payday = d },
                                label = { Text(if (d == 1) "یکم" else Digits.toPersian(d.toString())) },
                            )
                        }
                    }

                    // Salary Target Account
                    if (accounts.isNotEmpty()) {
                        Text("حساب واریز حقوق:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(accounts, key = { it.id }) { acc ->
                                FilterChip(
                                    selected = selectedAccountId == acc.id,
                                    onClick = { selectedAccountId = acc.id },
                                    label = { Text(acc.name) },
                                )
                            }
                        }
                    }

                    // Auto Deposit Switch
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
                                Text("ثبت خودکار درآمد حقوق", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    "در روز سررسید، سند واریز حقوق خودکار ثبت شود",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = autoDeposit, onCheckedChange = { autoDeposit = it })
                        }
                    }
                }
            },
            confirmButton = {
                HesabiButton(
                    text = "ذخیره تنظیمات",
                    onClick = {
                        onSave(parsedSalary, payday, selectedAccountId, autoDeposit)
                    },
                    enabled = salaryText.isBlank() || parsedSalary > 0,
                )
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("انصراف")
                }
            },
        )
    }
}

@Preview(name = "Light Mode")
@Composable
private fun SalarySetupDialogPreviewLight() {
    HesabiTheme(themeMode = ThemeMode.LIGHT) {
        SalarySetupDialog(
            initialSalaryToman = 25_000_000L,
            initialPayday = 25,
            initialAccountId = "1",
            initialAutoDeposit = true,
            accounts = listOf(
                Account("1", "بلوبانک", AccountKind.BANK, 0xFF2563EB, 5000000),
                Account("2", "بانک ملت", AccountKind.BANK, 0xFFDC2626, 2000000),
            ),
            onDismiss = {},
            onSave = { _, _, _, _ -> },
        )
    }
}

@Preview(name = "Dark Mode")
@Composable
private fun SalarySetupDialogPreviewDark() {
    HesabiTheme(themeMode = ThemeMode.DARK) {
        SalarySetupDialog(
            initialSalaryToman = 35_000_000L,
            initialPayday = 28,
            initialAccountId = "1",
            initialAutoDeposit = false,
            accounts = listOf(
                Account("1", "بلوبانک", AccountKind.BANK, 0xFF2563EB, 5000000),
            ),
            onDismiss = {},
            onSave = { _, _, _, _ -> },
        )
    }
}
