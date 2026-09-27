package de.danielgrebe.spinkingdom.domain

import de.danielgrebe.spinkingdom.T0
import de.danielgrebe.spinkingdom.models.GameState
import org.junit.Assert.*
import org.junit.Test

class SpinRegenTest {
    private val min = 60_000L

    @Test fun regeneratesFiveEveryTenMinutes() {
        val s = GameState(introDone = true, spins = 10, lastRegenMillis = T0)
        assertEquals(10, SpinRegen.apply(s, T0 + 9 * min).spins)
        val a = SpinRegen.apply(s, T0 + 10 * min)
        assertEquals(15, a.spins)
        assertEquals(T0 + 10 * min, a.lastRegenMillis)
        // partial interval is kept
        val b = SpinRegen.apply(s, T0 + 25 * min)
        assertEquals(20, b.spins)
        assertEquals(T0 + 20 * min, b.lastRegenMillis)
        assertEquals(5 * min, SpinRegen.millisUntilNext(b, T0 + 25 * min))
    }

    @Test fun offlineRegenIsCappedAtFifty() {
        val s = GameState(spins = 3, lastRegenMillis = T0)
        val r = SpinRegen.apply(s, T0 + 3 * 86_400_000L)
        assertEquals(50, r.spins)
        assertNull(SpinRegen.millisUntilNext(r, T0))
    }

    @Test fun spinsAboveCapAreKept() {
        val s = GameState(spins = 180, lastRegenMillis = T0)
        assertEquals(180, SpinRegen.apply(s, T0 + 60 * min).spins)
    }

    @Test fun clockGoingBackDoesNotGrant() {
        val s = GameState(spins = 10, lastRegenMillis = T0)
        assertEquals(10, SpinRegen.apply(s, T0 - 100 * min).spins)
    }

    @Test fun timeUntilFull() {
        val s = GameState(spins = 40, lastRegenMillis = T0)
        // 2 ticks needed: 10 min + 10 min
        assertEquals(20 * min, SpinRegen.millisUntilFull(s, T0))
        assertEquals(17 * min, SpinRegen.millisUntilFull(s, T0 + 3 * min))
    }
}
