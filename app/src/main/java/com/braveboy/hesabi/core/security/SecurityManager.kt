package com.braveboy.hesabi.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityManager {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private const val PREFIX = "pbkdf2"

    /** Only used to verify hashes created by versions before PBKDF2. */
    private const val LEGACY_SALT = "hesabi_lumina_secure_salt_v1"
    private val LEGACY_HASH_REGEX = Regex("^[0-9a-f]{64}$")

    /**
     * Returns `pbkdf2$iterations$salt$hash`, with a fresh random salt per call.
     * The format is self-describing so parameters can be raised later.
     */
    fun hashPin(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt, PBKDF2_ITERATIONS)
        return listOf(
            PREFIX,
            PBKDF2_ITERATIONS.toString(),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(hash),
        ).joinToString("$")
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || pin.isBlank()) return false
        return when {
            storedHash.startsWith("$PREFIX$") -> verifyPbkdf2(pin, storedHash)
            LEGACY_HASH_REGEX.matches(storedHash) -> constantTimeEquals(legacyHash(pin), storedHash)
            else -> false
        }
    }

    /** True when the stored value predates PBKDF2 and should be upgraded after a successful login. */
    fun isLegacyHash(storedHash: String): Boolean =
        storedHash.isNotBlank() &&
            !storedHash.startsWith("$PREFIX$") &&
            LEGACY_HASH_REGEX.matches(storedHash)

    private fun verifyPbkdf2(pin: String, storedHash: String): Boolean = runCatching {
        val parts = storedHash.split("$")
        if (parts.size != 4) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        if (iterations < 100_000) return false
        val salt = Base64.getDecoder().decode(parts[2])
        val expected = Base64.getDecoder().decode(parts[3])
        MessageDigest.isEqual(expected, derive(pin, salt, iterations))
    }.getOrDefault(false)

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun legacyHash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((LEGACY_SALT + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(left.toByteArray(), right.toByteArray())

    fun isBiometricSupported(context: android.content.Context): Boolean {
        val hasFingerprint = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
        val keyguardManager = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        val isDeviceSecure = keyguardManager?.isDeviceSecure == true
        return hasFingerprint || isDeviceSecure
    }

    fun showBiometricPrompt(
        context: android.content.Context,
        title: String = "تأیید هویت بیومتریک",
        subtitle: String = "برای ورود به حساب انگشت خود را روی حسگر قرار دهید",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val cancellationSignal = android.os.CancellationSignal()
            val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
            val prompt = android.hardware.biometrics.BiometricPrompt.Builder(context)
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
                object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
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
