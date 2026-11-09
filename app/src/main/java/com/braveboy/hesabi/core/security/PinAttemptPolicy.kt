package com.braveboy.hesabi.core.security

/** Persistent counter for wrong PIN entries. */
data class PinAttemptState(
    val failedAttempts: Int = 0,
    val lockedUntilMillis: Long = 0L,
)

object PinAttemptPolicy {

    const val MAX_ATTEMPTS = 5
    const val LOCKOUT_MILLIS = 60_000L

    fun afterFailure(state: PinAttemptState, nowMillis: Long): PinAttemptState {
        val attempts = state.failedAttempts + 1
        val lockedUntil = if (attempts >= MAX_ATTEMPTS) nowMillis + LOCKOUT_MILLIS else 0L
        return PinAttemptState(attempts, lockedUntil)
    }

    fun afterSuccess(): PinAttemptState = PinAttemptState()

    fun remainingLockMillis(state: PinAttemptState, nowMillis: Long): Long =
        (state.lockedUntilMillis - nowMillis).coerceAtLeast(0L)

    fun isLocked(state: PinAttemptState, nowMillis: Long): Boolean =
        remainingLockMillis(state, nowMillis) > 0L

    /** Adds a small progressive delay before checking a PIN after previous failures. */
    fun delayBeforeAttempt(failedAttempts: Int): Long = when {
        failedAttempts <= 0 -> 0L
        failedAttempts == 1 -> 500L
        failedAttempts == 2 -> 1_000L
        failedAttempts == 3 -> 2_000L
        else -> 4_000L
    }

    /** Once the lockout has elapsed the user starts again with a clean counter. */
    fun effectiveAttempts(state: PinAttemptState, nowMillis: Long): Int =
        if (state.lockedUntilMillis > 0L && nowMillis >= state.lockedUntilMillis) 0 else state.failedAttempts
}
