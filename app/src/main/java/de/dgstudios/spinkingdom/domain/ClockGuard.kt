package de.dgstudios.spinkingdom.domain

import de.dgstudios.spinkingdom.config.GameBalanceConfig
import de.dgstudios.spinkingdom.models.ClockState

/** Abstraction over the device clocks so logic stays testable. */
interface TimeSource {
    fun wallMillis(): Long
    /** Monotonic time since boot (SystemClock.elapsedRealtime on Android). */
    fun elapsedRealtime(): Long
}

data class ClockObservation(
    val clock: ClockState,
    /** Time that is safe to use for rewards (never goes backwards, ignores forward jumps). */
    val trustedNow: Long,
    val tampered: Boolean
)

/**
 * Basic clock manipulation detection.
 * - Wall clock going backwards compared to the highest trusted time -> tampered, trusted time is frozen.
 * - Within one boot session the wall clock advancing much faster than elapsedRealtime -> the difference
 *   is recorded as drift and never counts for rewards.
 */
object ClockGuard {
    fun observe(clock: ClockState, wall: Long, elapsed: Long): ClockObservation {
        if (clock.lastWallMillis == 0L) {
            val c = ClockState(maxTrustedMillis = wall, lastWallMillis = wall, lastElapsedRealtime = elapsed)
            return ClockObservation(c, wall, false)
        }
        var drift = clock.driftMillis
        var tampered = false
        val sameBoot = elapsed >= clock.lastElapsedRealtime
        if (sameBoot) {
            val wallDelta = wall - clock.lastWallMillis
            val realDelta = elapsed - clock.lastElapsedRealtime
            val jump = wallDelta - realDelta
            if (jump > GameBalanceConfig.CLOCK_FORWARD_JUMP_TOLERANCE_MS) {
                drift += jump
                tampered = true
            } else if (jump < -GameBalanceConfig.CLOCK_BACKWARDS_TOLERANCE_MS && drift > 0) {
                // clock was set back again: remove the previously recorded drift first
                drift = (drift + jump).coerceAtLeast(0)
            }
        }
        val candidate = wall - drift
        if (candidate < clock.maxTrustedMillis - GameBalanceConfig.CLOCK_BACKWARDS_TOLERANCE_MS) tampered = true
        val trusted = maxOf(candidate, clock.maxTrustedMillis)
        val newClock = clock.copy(
            maxTrustedMillis = trusted,
            lastWallMillis = wall,
            lastElapsedRealtime = elapsed,
            driftMillis = drift,
            tamperCount = clock.tamperCount + if (tampered) 1 else 0
        )
        return ClockObservation(newClock, trusted, tampered)
    }
}
