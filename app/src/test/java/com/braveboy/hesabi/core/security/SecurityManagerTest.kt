package com.braveboy.hesabi.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityManagerTest {

    @Test
    fun `hashPin generates consistent sha256 output`() {
        val hash1 = SecurityManager.hashPin("1370")
        val hash2 = SecurityManager.hashPin("1370")
        val hash3 = SecurityManager.hashPin("1371")

        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hash3)
        assertTrue(hash1.length == 64) // SHA-256 hex string length
    }

    @Test
    fun `verifyPin correctly validates pin against stored hash`() {
        val storedHash = SecurityManager.hashPin("4321")

        assertTrue(SecurityManager.verifyPin("4321", storedHash))
        assertFalse(SecurityManager.verifyPin("1234", storedHash))
        assertFalse(SecurityManager.verifyPin("", storedHash))
        assertFalse(SecurityManager.verifyPin("4321", ""))
    }
}
