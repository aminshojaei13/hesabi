package com.braveboy.hesabi.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.braveboy.hesabi.domain.model.AccountWithBalance
import com.braveboy.hesabi.domain.model.ThemeMode
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme
import kotlin.math.abs

@Composable
fun ReconciliationDialog(
    item: AccountWithBalance,
    onDismiss: () -> Unit,
    onConfirmReconcile: (actualBankBalanceToman: Long) -> Unit,
) {
    var actualInput by remember { mutableStateOf(if (item.balanceToman > 0) item.balanceToman.toString() else "") }
    val parsedActual = Digits.toAscii(actualInput).filter { it.isDigit() }.toLongOrNull() ?: 0L
    val diff = parsedActual - item.balanceToman

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "مغایرت‌گیری با بانک",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = item.account.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Current Book Balance Card
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
                            Text(
                                text = "موجودی ثبت‌شده در اپلیکیشن:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = Money.format(item.balanceToman, withUnit = true),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Bank Input
                    HesabiTextField(
                        value = actualInput,
                        onValueChange = { actualInput = it },
                        label = "موجودی اعلامی بانک / پیامک (تومان)",
                        placeholder = "مثلاً ۵,۲۰۰,۰۰۰",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Difference Summary
                    if (actualInput.isNotBlank()) {
                        val (containerColor, contentColor, icon, diffText) = when {
                            diff == 0L -> Quad(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.primary,
                                Icons.Rounded.CheckCircle,
                                "حساب تراز است (بدون مغایرت)",
                            )
                            diff > 0L -> Quad(
                                Color(0xFF10B981).copy(alpha = 0.15f),
                                Color(0xFF10B981),
                                Icons.AutoMirrored.Rounded.TrendingUp,
                                "مازاد در بانک: +${Money.format(diff, withUnit = true)} (سند درآمد تعدیلی ثبت خواهد شد)",
                            )
                            else -> Quad(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.error,
                                Icons.AutoMirrored.Rounded.TrendingDown,
                                "کسری در بانک: -${Money.format(abs(diff), withUnit = true)} (سند هزینه تعدیلی ثبت خواهد شد)",
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(HesabiRadius.input),
                            color = containerColor,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(HesabiSpacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                            ) {
                                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                                Text(
                                    text = diffText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                HesabiButton(
                    text = "ثبت تعدیل و همگام‌سازی",
                    onClick = {
                        onConfirmReconcile(parsedActual)
                    },
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

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Preview(name = "Light Mode")
@Composable
private fun ReconciliationDialogPreviewLight() {
    HesabiTheme(themeMode = ThemeMode.LIGHT) {
        ReconciliationDialog(
            item = AccountWithBalance(
                account = Account(
                    id = "1",
                    name = "بانک ملت",
                    kind = AccountKind.BANK,
                    colorArgb = 0xFFDC2626,
                    openingBalanceToman = 1_000_000L,
                ),
                balanceToman = 4_500_000L,
            ),
            onDismiss = {},
            onConfirmReconcile = {},
        )
    }
}

@Preview(name = "Dark Mode")
@Composable
private fun ReconciliationDialogPreviewDark() {
    HesabiTheme(themeMode = ThemeMode.DARK) {
        ReconciliationDialog(
            item = AccountWithBalance(
                account = Account(
                    id = "1",
                    name = "بلوبانک",
                    kind = AccountKind.BANK,
                    colorArgb = 0xFF2563EB,
                    openingBalanceToman = 2_000_000L,
                ),
                balanceToman = 6_800_000L,
            ),
            onDismiss = {},
            onConfirmReconcile = {},
        )
    }
}
