package com.braveboy.hesabi.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPackageCodecTest {

    private val codec = SyncPackageCodec()

    private fun sample() = SyncPackage(
        deviceId = "device-a",
        deviceName = "گوشی من",
        baseRevision = 7,
        createdAtMillis = 1_700_000_000_000,
        records = listOf(
            SyncRecord(
                entity = "transaction",
                id = "t-1",
                updatedAtMillis = 1_700_000_000_000,
                deviceId = "device-a",
                payload = """{"amount":"125000"}""",
            ),
            SyncRecord(
                entity = "account",
                id = "a-1",
                updatedAtMillis = 1_700_000_100_000,
                deviceId = "device-a",
                deleted = true,
            ),
        ),
    )

    @Test
    fun roundTripKeepsEveryRecord() {
        val passphrase = "یک-رمز-قوی-۱۲۳".toCharArray()

        val decoded = codec.decode(codec.encode(sample(), passphrase), passphrase)

        assertEquals("device-a", decoded.deviceId)
        assertEquals("گوشی من", decoded.deviceName)
        assertEquals(2, decoded.records.size)
        assertEquals("t-1", decoded.records.first().id)
        assertTrue(decoded.records[1].deleted)
    }

    @Test
    fun thePackageIsEncrypted() {
        val encoded = codec.encode(sample(), "secret".toCharArray())

        val text = encoded.toString(Charsets.ISO_8859_1)
        assertTrue(!text.contains("transaction"))
        assertTrue(!text.contains("125000"))
        assertTrue(!text.contains("گوشی"))
    }

    @Test
    fun wrongPassphraseIsRejected() {
        val encoded = codec.encode(sample(), "right".toCharArray())

        val failure = runCatching { codec.decode(encoded, "wrong".toCharArray()) }

        assertTrue(failure.exceptionOrNull() is SyncException)
    }

    @Test
    fun tamperingIsDetected() {
        val encoded = codec.encode(sample(), "secret".toCharArray())
        encoded[encoded.lastIndex] = (encoded.last().toInt() xor 0x01).toByte()

        val failure = runCatching { codec.decode(encoded, "secret".toCharArray()) }

        assertTrue(failure.exceptionOrNull() is SyncException)
    }

    @Test
    fun emptyPassphraseIsRejected() {
        assertTrue(runCatching { codec.encode(sample(), CharArray(0)) }.isFailure)
        assertTrue(runCatching { codec.decode(ByteArray(10), CharArray(0)) }.isFailure)
    }

    @Test
    fun emptyPayloadIsRejected() {
        assertTrue(runCatching { codec.decode(ByteArray(0), "x".toCharArray()) }.isFailure)
    }

    @Test
    fun recordsWithoutIdAreRejectedBeforeEncryption() {
        val broken = sample().copy(
            records = listOf(SyncRecord("transaction", " ", 1, "device-a")),
        )

        assertTrue(runCatching { codec.encode(broken, "secret".toCharArray()) }.isFailure)
    }
}
