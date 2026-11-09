package com.braveboy.hesabi.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.braveboy.hesabi.core.security.SecurityManager
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import kotlinx.coroutines.launch

/**
 * Asks for the current PIN before changing or disabling the app lock.
 * The callback also reports whether the stored hash is legacy so it can be upgraded.
 */
@Composable
fun CurrentPinDialog(
    pinCodeHash: String,
    onConfirmed: (pin: String, legacyHash: Boolean) -> Unit,
    onDismiss: () -> Unit,
    title: String = "تأیید رمز فعلی",
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                Text(
                    text = "برای ادامه، رمز عبور فعلی برنامه را وارد کنید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        pin = input.filter { it.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("رمز فعلی (۴ رقم)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pin.length != 4) {
                        error = "رمز باید دقیقاً ۴ رقم باشد."
                        return@TextButton
                    }
                    scope.launch {
                        checking = true
                        val matches = SecurityManager.verifyPin(pin, pinCodeHash)
                        checking = false
                        if (matches) {
                            onConfirmed(pin, SecurityManager.isLegacyHash(pinCodeHash))
                        } else {
                            error = "رمز فعلی درست نیست."
                            pin = ""
                        }
                    }
                },
                enabled = !checking && pin.length == 4,
            ) { Text("تأیید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}
