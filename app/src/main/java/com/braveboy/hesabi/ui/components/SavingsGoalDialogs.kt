package com.braveboy.hesabi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import java.util.UUID

private val availableGoalIcons = listOf(
    "savings" to Icons.Rounded.Savings,
    "flight" to Icons.Rounded.Flight,
    "laptop" to Icons.Rounded.Laptop,
    "car" to Icons.Rounded.DirectionsCar,
    "home" to Icons.Rounded.Home,
    "gift" to Icons.Rounded.CardGiftcard,
)

private val availableGoalColors = listOf(
    0xFF10B981L, // Emerald
    0xFF8B5CF6L, // Violet
    0xFF3B82F6L, // Blue
    0xFFF59E0BL, // Amber
    0xFFEC4899L, // Pink
    0xFF06B6D4L, // Cyan
)

@Composable
fun AddEditGoalDialog(
    goal: SavingsGoal? = null,
    onSave: (SavingsGoal) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(goal?.title ?: "") }
    var targetText by remember { mutableStateOf(goal?.targetAmountToman?.toString() ?: "") }
    var monthlyTargetText by remember {
        mutableStateOf(goal?.monthlyTargetToman?.takeIf { it > 0 }?.toString() ?: "")
    }
    var selectedIcon by remember { mutableStateOf(goal?.iconKey ?: "savings") }
    var selectedColor by remember { mutableLongStateOf(goal?.colorArgb ?: 0xFF10B981L) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (goal == null) "قلک جدید" else "ویرایش قلک")
                if (goal != null && onDelete != null) {
                    IconButton(onClick = { onDelete(goal.id); onDismiss() }) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("عنوان هدف (مثلاً: سفر شمال)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HesabiRadius.input),
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = Digits.toAscii(it).filter { ch -> ch.isDigit() }; error = null },
                    label = { Text("مبلغ هدف (تومان)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HesabiRadius.input),
                )

                OutlinedTextField(
                    value = monthlyTargetText,
                    onValueChange = { monthlyTargetText = Digits.toAscii(it).filter { ch -> ch.isDigit() } },
                    label = { Text("پس‌انداز ماهانه (اختیاری)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HesabiRadius.input),
                )

                // Icon Picker
                Text(text = "انتخاب آیکون", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                    items(availableGoalIcons) { (key, icon) ->
                        val isSelected = selectedIcon == key
                        Surface(
                            onClick = { selectedIcon = key },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(42.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                // Color Picker
                Text(text = "انتخاب رنگ", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                    items(availableGoalColors) { argb ->
                        val isSelected = selectedColor == argb
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toInt()))
                                .clickable { selectedColor = argb }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier
                                ),
                        )
                    }
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetAmount = targetText.toLongOrNull() ?: 0L
                    if (title.isBlank()) {
                        error = "لطفاً عنوانی برای قلک وارد کنید."
                        return@TextButton
                    }
                    if (targetAmount <= 0) {
                        error = "مبلغ هدف باید بیشتر از صفر باشد."
                        return@TextButton
                    }
                    val monthlyTarget = monthlyTargetText.toLongOrNull() ?: 0L
                    val newGoal = (goal ?: SavingsGoal(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        targetAmountToman = targetAmount,
                    )).copy(
                        title = title,
                        targetAmountToman = targetAmount,
                        iconKey = selectedIcon,
                        colorArgb = selectedColor,
                        monthlyTargetToman = monthlyTarget,
                    )
                    onSave(newGoal)
                    onDismiss()
                },
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        },
    )
}

@Composable
fun GoalDepositWithdrawDialog(
    goal: SavingsGoal,
    onDeposit: (Long) -> Unit,
    onWithdraw: (Long) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var isDepositMode by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(goal.title)
                TextButton(onClick = { onEdit(); onDismiss() }) {
                    Text("ویرایش")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
            ) {
                // Current balance & target
                Surface(
                    shape = RoundedCornerShape(HesabiRadius.card),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(HesabiSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "موجودی فعلی: ${Money.format(goal.savedAmountToman)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "مبلغ هدف: ${Money.format(goal.targetAmountToman)} (${Digits.toPersian((goal.progress * 100).toInt().toString())}٪)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Mode toggle (واریز / برداشت)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    Surface(
                        onClick = { isDepositMode = true },
                        shape = RoundedCornerShape(HesabiRadius.chip),
                        color = if (isDepositMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("واریز به قلک", fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        onClick = { isDepositMode = false },
                        shape = RoundedCornerShape(HesabiRadius.chip),
                        color = if (!isDepositMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("برداشت از قلک", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = Digits.toAscii(it).filter { ch -> ch.isDigit() }; error = null },
                    label = { Text(if (isDepositMode) "مبلغ واریز (تومان)" else "مبلغ برداشت (تومان)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HesabiRadius.input),
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount <= 0) {
                        error = "لطفاً مبلغ معتبری وارد کنید."
                        return@TextButton
                    }
                    if (!isDepositMode && amount > goal.savedAmountToman) {
                        error = "مبلغ برداشت نمی‌تواند بیشتر از موجودی قلک باشد."
                        return@TextButton
                    }
                    if (isDepositMode) {
                        onDeposit(amount)
                    } else {
                        onWithdraw(amount)
                    }
                    onDismiss()
                },
            ) {
                Text(if (isDepositMode) "واریز" else "برداشت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        },
    )
}
