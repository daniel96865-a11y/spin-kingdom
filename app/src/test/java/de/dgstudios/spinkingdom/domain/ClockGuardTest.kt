package de.dgstudios.spinkingdom.domain

import de.dgstudios.spinkingdom.T0
import de.dgstudios.spinkingdom.models.ClockState
import org.junit.Assert.*
import org.junit.Test

class ClockGuardTest {
    private val h = 3_600_000L

    @Test fun normalTimeFlowIsTrusted() {
        val a = ClockGuard.observe(ClockState(), T0, 1_000)
        val b = ClockGuard.observe(a.clock, T0 + 2 * h, 1_000 + 2 * h)
        assertFalse(b.tampered)
        assertEquals(T0 + 2 * h, b.trustedNow)
    }

    @Test fun clockSetBackIsDetectedAndTimeFrozen() {
        val a = ClockGuard.observe(ClockState(), T0, 1_000)
        val b = ClockGuard.observe(a.clock, T0 - 26 * h, 1_000 + 60_000)
        assertTrue(b.tampered)
        assertEquals(T0, b.trustedNow)
        assertEquals(1, b.clock.tamperCount)
    }

    @Test fun forwardJumpWithinSameBootBecomesDrift() {
        val a = ClockGuard.observe(ClockState(), T0, 1_000)
        // wall clock jumps 2 days but only 1 minute really passed
        val b = ClockGuard.observe(a.clock, T0 + 48 * h + 60_000, 1_000 + 60_000)
        assertTrue(b.tampered)
        assertEquals(T0 + 60_000, b.trustedNow)
        // later, time continues normally: trusted time keeps ignoring the jump
        val c = ClockGuard.observe(b.clock, T0 + 49 * h + 60_000, 1_000 + 60_000 + h)
        assertFalse(c.tampered)
        assertEquals(T0 + h + 60_000, c.trustedNow)
    }

    @Test fun rebootWithNormalTimeIsFine() {
        val a = ClockGuard.observe(ClockState(), T0, 50 * h)
        // elapsedRealtime restarted (reboot), wall clock moved forward normally
        val b = ClockGuard.observe(a.clock, T0 + 5 * h, 10_000)
        assertFalse(b.tampered)
        assertEquals(T0 + 5 * h, b.trustedNow)
    }

    @Test fun smallNtpCorrectionsAreTolerated() {
        val a = ClockGuard.observe(ClockState(), T0, 1_000)
        val b = ClockGuard.observe(a.clock, T0 - 60_000, 1_000 + 1_000)
        assertFalse(b.tampered)
    }
}
