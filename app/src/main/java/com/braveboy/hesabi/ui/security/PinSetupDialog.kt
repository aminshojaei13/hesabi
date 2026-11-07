package com.braveboy.hesabi.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.braveboy.hesabi.core.security.SecurityManager
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.theme.HesabiSpacing

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onPinConfirmed: (pinHash: String) -> Unit,
) {
    var step by remember { mutableStateOf(1) } // 1: enter pin, 2: confirm pin
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (step == 1) "تنظیم رمز عبور ۴ رقمی" else "تأیید رمز عبور",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
            ) {
                Text(
                    text = if (step == 1) "یک رمز عبور ۴ رقمی عددی برای ورود به حسابی وارد کنید."
                    else "لطفاً رمز عبور انتخابی را مجدداً وارد نمایید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = if (step == 1) firstPin else confirmPin,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }.take(4)
                        if (step == 1) firstPin = digitsOnly else confirmPin = digitsOnly
                        errorMessage = null
                    },
                    label = { Text("رمز عبور (۴ رقم)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1) {
                        if (firstPin.length == 4) {
                            step = 2
                        } else {
                            errorMessage = "رمز عبور باید دقیقاً ۴ رقم باشد."
                        }
                    } else {
                        if (confirmPin == firstPin) {
                            onPinConfirmed(SecurityManager.hashPin(firstPin))
                        } else {
                            errorMessage = "رمزهای وارد شده یکسان نیستند. دوباره تلاش کنید."
                            confirmPin = ""
                        }
                    }
                },
                enabled = if (step == 1) firstPin.length == 4 else confirmPin.length == 4,
            ) {
                Text(if (step == 1) "بعدی" else "تأیید و ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        },
    )
}

@Preview(name = "Pin Setup Dialog - Dark")
@Composable
private fun PinSetupDialogPreviewDark() {
    HesabiPreview(dark = true) {
        PinSetupDialog(onDismiss = {}, onPinConfirmed = {})
    }
}
