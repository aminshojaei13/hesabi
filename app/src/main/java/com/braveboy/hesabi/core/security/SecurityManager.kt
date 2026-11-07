package com.braveboy.hesabi.core.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import java.security.MessageDigest

object SecurityManager {

    private const val SALT = "hesabi_lumina_secure_salt_v1"

    fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest((SALT + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || pin.isBlank()) return false
        return hashPin(pin) == storedHash
    }

    fun isBiometricSupported(context: Context): Boolean {
        val hasFingerprint = context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceSecure = keyguardManager?.isDeviceSecure == true
        return hasFingerprint || isDeviceSecure
    }

    fun showBiometricPrompt(
        context: Context,
        title: String = "تأیید هویت بیومتریک",
        subtitle: String = "برای ورود به حساب انگشت خود را روی حسگر قرار دهید",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cancellationSignal = CancellationSignal()
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt.Builder(context)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton("استفاده از رمز عبور", executor) { _, _ ->
                    cancellationSignal.cancel()
                    onError("canceled")
                }
                .build()

            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errString?.toString() ?: "Authentication error")
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Authentication failed")
                    }
                },
            )
        } else {
            onError("Biometric not supported on this OS version")
        }
    }
}
