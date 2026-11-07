package com.braveboy.hesabi.ui.debts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.model.ChequeDirection
import com.braveboy.hesabi.domain.model.Debt
import com.braveboy.hesabi.domain.model.DebtDirection
import com.braveboy.hesabi.domain.model.Person
import com.braveboy.hesabi.ui.components.JalaliDatePickerDialog
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing

@Composable
fun AddPersonDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, notes: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن طرف حساب / شخص", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام و نام خانوادگی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره تماس (اختیاری)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت (اختیاری)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onConfirm(name, phone, notes) },
                enabled = isValid,
            ) {
                Text("ثبت شخص")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}

@Composable
fun AddDebtDialog(
    person: Person,
    onDismiss: () -> Unit,
    onConfirm: (amountToman: Long, direction: DebtDirection, dueEpochDay: Long?, note: String) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(DebtDirection.LENT) }
    var dueDate by remember { mutableStateOf<JalaliDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    val amountToman = Money.parseToman(amountText) ?: 0L
    val isValid = amountToman > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت بدهی یا طلب برای ${person.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    FilterChip(
                        selected = direction == DebtDirection.LENT,
                        onClick = { direction = DebtDirection.LENT },
                        label = { Text("طلبکارم (داده‌ام)") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = direction == DebtDirection.BORROWED,
                        onClick = { direction = DebtDirection.BORROWED },
                        label = { Text("بدهکارم (گرفته‌ام)") },
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ (تومان)") },
                    supportingText = {
                        if (amountToman > 0) {
                            Text(Money.format(amountToman))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HesabiRadius.input),
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = dueDate?.let { "موعد: ${it.formatShort()}" } ?: "تعیین تاریخ موعد (اختیاری)",
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات و بابت (اختیاری)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onConfirm(amountToman, direction, dueDate?.toEpochDay(), note) },
                enabled = isValid,
            ) {
                Text("ثبت تعهد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = dueDate ?: JalaliDate.today(),
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                dueDate = date
                showDatePicker = false
            },
        )
    }
}

@Composable
fun SettleDebtDialog(
    debt: Debt,
    onDismiss: () -> Unit,
    onConfirm: (amountToman: Long) -> Unit,
) {
    val remaining = debt.remainingAmountToman
    var amountText by remember { mutableStateOf(remaining.toString()) }
    val settleAmount = Money.parseToman(amountText) ?: 0L
    val isValid = settleAmount in 1..remaining

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسویه حساب", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
            ) {
                Text(
                    text = "مبلغ باقیمانده: ${Money.format(remaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ پرداختی برای تسویه (تومان)") },
                    supportingText = {
                        if (settleAmount > 0) {
                            Text(Money.format(settleAmount))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                TextButton(
                    onClick = { amountText = remaining.toString() },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("تسویه کل مبلغ (${Money.format(remaining)})")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onConfirm(settleAmount) },
                enabled = isValid,
            ) {
                Text("ثبت تسویه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}

@Composable
fun AddChequeDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        sayadNumber: String,
        bankName: String,
        amountToman: Long,
        dueEpochDay: Long,
        payeeOrDrawerName: String,
        direction: ChequeDirection,
        note: String,
    ) -> Unit,
) {
    var sayadNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var payeeOrDrawerName by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(ChequeDirection.ISSUED) }
    var dueDate by remember { mutableStateOf(JalaliDate.today().plusMonths(1)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    val amountToman = Money.parseToman(amountText) ?: 0L
    val isValid = bankName.isNotBlank() && amountToman > 0 && payeeOrDrawerName.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت چک جدید صیادی", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    FilterChip(
                        selected = direction == ChequeDirection.ISSUED,
                        onClick = { direction = ChequeDirection.ISSUED },
                        label = { Text("چک پرداختی (صادره)") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = direction == ChequeDirection.RECEIVED,
                        onClick = { direction = ChequeDirection.RECEIVED },
                        label = { Text("چک دریافتی") },
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("نام بانک (مثال: ملت، صادرات)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = payeeOrDrawerName,
                    onValueChange = { payeeOrDrawerName = it },
                    label = {
                        Text(if (direction == ChequeDirection.ISSUED) "در وجه (گیرنده چک)" else "صادرکننده چک")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ چک (تومان)") },
                    supportingText = {
                        if (amountToman > 0) {
                            Text(Money.format(amountToman))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = sayadNumber,
                    onValueChange = { sayadNumber = it },
                    label = { Text("شناسه صیادی ۱۶ رقمی (اختیاری)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HesabiRadius.input),
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(text = "سررسید چک: ${dueDate.formatShort()}")
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت یا بابت (اختیاری)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(
                            sayadNumber,
                            bankName,
                            amountToman,
                            dueDate.toEpochDay(),
                            payeeOrDrawerName,
                            direction,
                            note,
                        )
                    }
                },
                enabled = isValid,
            ) {
                Text("ثبت چک")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = dueDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                dueDate = date
                showDatePicker = false
            },
        )
    }
}

@Composable
fun AddLoanDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        totalAmountToman: Long,
        installmentCount: Int,
        installmentAmountToman: Long,
        firstDueEpochDay: Long,
        note: String,
    ) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var totalAmountText by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf("12") }
    var customInstallmentText by remember { mutableStateOf("") }
    var firstDueDate by remember { mutableStateOf(JalaliDate.today().plusMonths(1)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    val totalAmount = Money.parseToman(totalAmountText) ?: 0L
    val count = countText.toIntOrNull() ?: 0
    val autoCalculatedInstallment = if (count > 0 && totalAmount > 0) totalAmount / count else 0L
    val installmentAmount = Money.parseToman(customInstallmentText) ?: autoCalculatedInstallment
    val isValid = title.isNotBlank() && totalAmount > 0 && count > 0 && installmentAmount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت وام و اقساط جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان وام (مثال: وام مسکن، رسالت)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = { totalAmountText = it },
                    label = { Text("مبلغ کل وام (تومان)") },
                    supportingText = {
                        if (totalAmount > 0) {
                            Text(Money.format(totalAmount))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it },
                        label = { Text("تعداد اقساط") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = customInstallmentText.ifEmpty {
                            if (autoCalculatedInstallment > 0) autoCalculatedInstallment.toString() else ""
                        },
                        onValueChange = { customInstallmentText = it },
                        label = { Text("مبلغ هر قسط") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                    )
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HesabiRadius.input),
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(text = "سررسید اولین قسط: ${firstDueDate.formatShort()}")
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات (اختیاری)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(
                            title,
                            totalAmount,
                            count,
                            installmentAmount,
                            firstDueDate.toEpochDay(),
                            note,
                        )
                    }
                },
                enabled = isValid,
            ) {
                Text("ثبت وام و برنامه اقساط")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = firstDueDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                firstDueDate = date
                showDatePicker = false
            },
        )
    }
}

@Preview(name = "Add Person Dialog - Light")
@Composable
private fun AddPersonDialogPreviewLight() {
    HesabiPreview(dark = false) {
        AddPersonDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
    }
}

@Preview(name = "Add Debt Dialog - Dark")
@Composable
private fun AddDebtDialogPreviewDark() {
    HesabiPreview(dark = true) {
        AddDebtDialog(
            person = Person(id = "1", name = "رضا محمدی"),
            onDismiss = {},
            onConfirm = { _, _, _, _ -> },
        )
    }
}
