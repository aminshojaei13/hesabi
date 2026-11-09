package com.braveboy.hesabi.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinAttemptPolicyTest {

    @Test
    fun `five failures lock the pin for one minute`() {
        var state = PinAttemptState()
        repeat(PinAttemptPolicy.MAX_ATTEMPTS - 1) { index ->
            state = PinAttemptPolicy.afterFailure(state, nowMillis = 1_000L + index)
            assertEquals(0L, state.lockedUntilMillis)
        }
        state = PinAttemptPolicy.afterFailure(state, nowMillis = 5_000L)

        assertEquals(5, state.failedAttempts)
        assertEquals(5_000L + PinAttemptPolicy.LOCKOUT_MILLIS, state.lockedUntilMillis)
        assertTrue(PinAttemptPolicy.isLocked(state, nowMillis = 5_001L))
        assertEquals(PinAttemptPolicy.LOCKOUT_MILLIS - 1L, PinAttemptPolicy.remainingLockMillis(state, 5_001L))
    }

    @Test
    fun `lockout expires and the counter resets`() {
        val state = PinAttemptState(
            failedAttempts = PinAttemptPolicy.MAX_ATTEMPTS,
            lockedUntilMillis = 10_000L,
        )

        assertFalse(PinAttemptPolicy.isLocked(state, nowMillis = 10_000L))
        assertEquals(0, PinAttemptPolicy.effectiveAttempts(state, nowMillis = 10_000L))
        assertEquals(
            1,
            PinAttemptPolicy.afterFailure(
                PinAttemptState(PinAttemptPolicy.effectiveAttempts(state, 10_000L), 0L),
                nowMillis = 10_001L,
            ).failedAttempts,
        )
    }

    @Test
    fun `success clears the counter`() {
        assertEquals(PinAttemptState(), PinAttemptPolicy.afterSuccess())
    }

    @Test
    fun `delay grows with failed attempts`() {
        assertEquals(0L, PinAttemptPolicy.delayBeforeAttempt(0))
        assertTrue(PinAttemptPolicy.delayBeforeAttempt(1) < PinAttemptPolicy.delayBeforeAttempt(2))
        assertTrue(PinAttemptPolicy.delayBeforeAttempt(2) < PinAttemptPolicy.delayBeforeAttempt(3))
        assertTrue(PinAttemptPolicy.delayBeforeAttempt(3) < PinAttemptPolicy.delayBeforeAttempt(4))
    }
}
