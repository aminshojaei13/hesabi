package com.braveboy.hesabi.ui.security

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.security.PinAttemptPolicy
import com.braveboy.hesabi.core.security.PinAttemptState
import com.braveboy.hesabi.core.security.SecurityManager
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.LocalHesabiLumina
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AppLockScreen(
    pinCodeHash: String,
    biometricEnabled: Boolean,
    failedAttempts: Int = 0,
    lockedUntilMillis: Long = 0L,
    onUnlocked: () -> Unit,
    onAttemptStateChanged: (failedAttempts: Int, lockedUntilMillis: Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var enteredPin by remember { mutableStateOf("") }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var hasWrongAttempt by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lumina = LocalHesabiLumina.current

    val attemptState = PinAttemptState(failedAttempts, lockedUntilMillis)
    val effectiveAttempts = PinAttemptPolicy.effectiveAttempts(attemptState, nowMillis)
    val remainingLockMillis = PinAttemptPolicy.remainingLockMillis(attemptState, nowMillis)
    val isLocked = remainingLockMillis > 0L

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }

    // Clear the persisted lockout and counter once the cooldown has passed.
    LaunchedEffect(lockedUntilMillis) {
        if (lockedUntilMillis > 0L) {
            val remaining = lockedUntilMillis - System.currentTimeMillis()
            if (remaining > 0L) delay(remaining)
            hasWrongAttempt = false
            onAttemptStateChanged(0, 0L)
        }
    }

    // Automatically prompt biometric if enabled
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && SecurityManager.isBiometricSupported(context)) {
            SecurityManager.showBiometricPrompt(
                context = context,
                onSuccess = {
                    onAttemptStateChanged(0, 0L)
                    onUnlocked()
                },
                onError = {},
            )
        }
    }

    fun onDigit(digit: Int) {
        if (isLocked || enteredPin.length >= 4) return
        val next = enteredPin + digit
        enteredPin = next
        if (next.length == 4) {
            scope.launch {
                val waitMillis = PinAttemptPolicy.delayBeforeAttempt(effectiveAttempts)
                if (waitMillis > 0L) delay(waitMillis)
                if (SecurityManager.verifyPin(next, pinCodeHash)) {
                    hasWrongAttempt = false
                    onAttemptStateChanged(0, 0L)
                    onUnlocked()
                } else {
                    val newState = PinAttemptPolicy.afterFailure(
                        PinAttemptState(effectiveAttempts, lockedUntilMillis),
                        System.currentTimeMillis(),
                    )
                    hasWrongAttempt = true
                    onAttemptStateChanged(newState.failedAttempts, newState.lockedUntilMillis)
                    shakeOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 400
                            -20f at 50
                            20f at 100
                            -15f at 150
                            15f at 200
                            -10f at 250
                            10f at 300
                            0f at 400
                        },
                    )
                    enteredPin = ""
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
        }
    }

    fun onBiometricClick() {
        SecurityManager.showBiometricPrompt(
            context = context,
            onSuccess = {
                onAttemptStateChanged(0, 0L)
                onUnlocked()
            },
            onError = {},
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 64.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header: Lock icon & title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(lumina.glowPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "قفل برنامه",
                        tint = lumina.glowPrimary,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Text(
                    text = "قفل امنیتی حسابی",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "رمز عبور ۴ رقمی خود را وارد کنید",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(HesabiSpacing.md))

                // PIN indicator dots
                Row(
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) lumina.glowPrimary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        )
                    }
                }

                when {
                    isLocked -> Text(
                        text = "تلاش بیش از حد. " +
                            Digits.toPersian(((remainingLockMillis + 999) / 1_000).toString()) +
                            " ثانیه صبر کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    hasWrongAttempt -> {
                        val remaining = (PinAttemptPolicy.MAX_ATTEMPTS - effectiveAttempts).coerceAtLeast(0)
                        Text(
                            text = if (remaining > 0) {
                                "رمز اشتباه است. " + Digits.toPersian(remaining.toString()) + " تلاش باقی مانده."
                            } else {
                                "رمز اشتباه است."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val rows = listOf(
                    listOf(1, 2, 3),
                    listOf(4, 5, 6),
                    listOf(7, 8, 9),
                )

                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { digit ->
                            KeypadButton(
                                text = Digits.toPersian(digit.toString()),
                                onClick = { onDigit(digit) },
                                enabled = !isLocked,
                            )
                        }
                    }
                }

                // Bottom row: Biometric / 0 / Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Biometric button (if supported & enabled)
                    if (biometricEnabled) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable { onBiometricClick() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = "اثر انگشت",
                                tint = lumina.glowPrimary,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    } else {
                        Spacer(Modifier.size(72.dp))
                    }

                    // Zero digit
                    KeypadButton(
                        text = Digits.toPersian("0"),
                        onClick = { onDigit(0) },
                        enabled = !isLocked,
                    )

                    // Backspace button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isLocked) { onBackspace() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = "پاک کردن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val lumina = LocalHesabiLumina.current

    Surface(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        shape = CircleShape,
        color = lumina.glassBackground,
        border = BorderStroke(1.dp, lumina.glassBorder),
        enabled = enabled,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                },
            )
        }
    }
}

@Preview(name = "App Lock - Dark")
@Composable
private fun AppLockDarkPreview() {
    HesabiPreview(dark = true) {
        AppLockScreen(
            pinCodeHash = SecurityManager.hashPin("1234"),
            biometricEnabled = true,
            onUnlocked = {},
        )
    }
}

@Preview(name = "App Lock - Light")
@Composable
private fun AppLockLightPreview() {
    HesabiPreview(dark = false) {
        AppLockScreen(
            pinCodeHash = SecurityManager.hashPin("1234"),
            biometricEnabled = false,
            onUnlocked = {},
        )
    }
}
