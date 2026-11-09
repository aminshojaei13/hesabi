package com.braveboy.hesabi.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class SecurityManagerTest {

    @Test
    fun `hashPin uses pbkdf2 with a random salt`() {
        val hash1 = SecurityManager.hashPin("1370")
        val hash2 = SecurityManager.hashPin("1370")

        assertTrue(hash1.startsWith("pbkdf2$"))
        assertNotEquals(hash1, hash2) // a fresh random salt per hash
        assertTrue(SecurityManager.verifyPin("1370", hash1))
        assertTrue(SecurityManager.verifyPin("1370", hash2))
    }

    @Test
    fun `verifyPin rejects wrong pin and malformed hashes`() {
        val storedHash = SecurityManager.hashPin("4321")

        assertFalse(SecurityManager.verifyPin("1234", storedHash))
        assertFalse(SecurityManager.verifyPin("", storedHash))
        assertFalse(SecurityManager.verifyPin("4321", ""))
        assertFalse(SecurityManager.verifyPin("4321", "not-a-hash"))
        assertFalse(SecurityManager.verifyPin("4321", "pbkdf2\$100000\$bad"))
    }

    @Test
    fun `verifyPin still accepts legacy sha256 hashes and flags them`() {
        val legacy = legacyHash("4321")

        assertTrue(SecurityManager.verifyPin("4321", legacy))
        assertFalse(SecurityManager.verifyPin("1234", legacy))
        assertTrue(SecurityManager.isLegacyHash(legacy))
        assertFalse(SecurityManager.isLegacyHash(SecurityManager.hashPin("4321")))
    }

    @Test
    fun `hashPin output carries its parameters`() {
        val parts = SecurityManager.hashPin("1370").split("$")
        assertEquals(4, parts.size)
        assertEquals("pbkdf2", parts[0])
        assertTrue(parts[1].toInt() >= 100_000)
        assertTrue(parts[2].isNotBlank())
        assertTrue(parts[3].isNotBlank())
    }

    private fun legacyHash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(("hesabi_lumina_secure_salt_v1" + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }
}
