package de.danielgrebe.spinkingdom.game

import de.danielgrebe.spinkingdom.*
import de.danielgrebe.spinkingdom.config.EventModifiers
import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.domain.ChestEngine
import de.danielgrebe.spinkingdom.domain.LuckyBoost
import de.danielgrebe.spinkingdom.domain.SpinJackpot
import de.danielgrebe.spinkingdom.models.ChestType
import de.danielgrebe.spinkingdom.models.SlotSymbol.*
import de.danielgrebe.spinkingdom.storage.SaveCodec
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

/** Random whose nextDouble() always returns [d] – lets tests force rare outcomes. */
class FixedRandom(private val d: Double) : Random() {
    override fun nextBits(bitCount: Int) = 0
    override fun nextDouble() = d
}

class SpinJackpotAndLuckyBoostTest {
    private fun fixedCtx(d: Double, now: Long = T0, mods: EventModifiers = EventModifiers.NONE) =
        ctx(now, mods = mods).copy(random = FixedRandom(d))

    private fun energy(state: de.danielgrebe.spinkingdom.models.GameState, c: Ctx) =
        GameEngine.spin(state, c, listOf(ENERGY, ENERGY, ENERGY))

    // ---------------------------------------------------------------- spin jackpot
    @Test fun rollTableUsesConfiguredChances() {
        val t = GameBalanceConfig.SPIN_JACKPOT_CHANCES
        assertEquals(30, SpinJackpot.roll(t, FixedRandom(0.0)))
        assertEquals(30, SpinJackpot.roll(t, FixedRandom(0.049)))
        assertEquals(20, SpinJackpot.roll(t, FixedRandom(0.05)))
        assertEquals(20, SpinJackpot.roll(t, FixedRandom(0.169)))
        assertEquals(0, SpinJackpot.roll(t, FixedRandom(0.17)))
        val r = Random(1)
        val n = 200_000
        val res = List(n) { SpinJackpot.roll(t, r) }
        assertEquals(0.05, res.count { it == 30 }.toDouble() / n, 0.005)
        assertEquals(0.12, res.count { it == 20 }.toDouble() / n, 0.006)
    }

    @Test fun energyTripleCanGiveSpinJackpot() {
        val s = startedGame()
        val r30 = energy(s, fixedCtx(0.01))
        val spun = r30.effects.first() as GameEffect.Spun
        assertEquals(30, spun.spinJackpot)
        assertEquals(GameBalanceConfig.ENERGY_TRIPLE_SPINS + 30, spun.spins)
        assertEquals(50 - 1 + 10 + 30, r30.state.spins)
        val r20 = energy(s, fixedCtx(0.10)).effects.first() as GameEffect.Spun
        assertEquals(20, r20.spinJackpot)
        val none = energy(s, fixedCtx(0.9)).effects.first() as GameEffect.Spun
        assertEquals(0, none.spinJackpot)
        assertEquals(GameBalanceConfig.ENERGY_TRIPLE_SPINS, none.spins)
    }

    @Test fun spinJackpotIsNotScaledByLevel() {
        val low = energy(startedGame(), fixedCtx(0.01)).effects.first() as GameEffect.Spun
        val high = energy(startedGame().copy(level = 250), fixedCtx(0.01)).effects.first() as GameEffect.Spun
        assertEquals(low.spinJackpot, high.spinJackpot)
    }

    @Test fun onlyEnergyTriplesGiveTheSlotJackpot() {
        val s = startedGame()
        for (reels in listOf(listOf(COIN, COIN, COIN), listOf(ENERGY, ENERGY, RAID), listOf(SHIELD, SHIELD, SHIELD))) {
            assertEquals(0, (GameEngine.spin(s, fixedCtx(0.0), reels).effects.first() as GameEffect.Spun).spinJackpot)
        }
    }

    @Test fun multiplierAppliesAndIsCapped() {
        assertEquals(60, SpinJackpot.scaled(30, 2, 1.0))
        assertEquals(GameBalanceConfig.SPIN_JACKPOT_MAX_SPINS, SpinJackpot.scaled(30, 100, 1.0))
        assertEquals(45, SpinJackpot.scaled(30, 1, 1.5))
        assertEquals(0, SpinJackpot.scaled(0, 10, 1.0))
        val s = startedGame().copy(selectedMultiplier = 5)
        val r = energy(s, fixedCtx(0.01)).effects.first() as GameEffect.Spun
        assertEquals(150, r.spinJackpot)
    }

    @Test fun wheelSpinSegmentsCanBecomeSpinJackpot() {
        // FixedRandom.nextInt(...) returns 0 via nextBits -> first wheel segment (coins); find a spins segment by seed instead
        var found30 = false; var normal = false
        for (seed in 0 until 3000) {
            val r = GameEngine.spinWheel(startedGame(), ctx(seed = seed), false)
            val w = r.effects.first() as GameEffect.WheelSpun
            if (w.spinJackpot > 0) {
                assertTrue(w.spinJackpot == 20 || w.spinJackpot == 30 || w.spinJackpot == 25)
                assertEquals(startedGame().spins + w.spins, r.state.spins)
                if (w.spinJackpot == 30) found30 = true
            } else if (w.spins in listOf(10, 25)) normal = true
        }
        assertTrue(found30 && normal)
    }

    @Test fun royalAndLegendaryChestsSometimesContainSpinJackpot() {
        assertEquals(0, ChestEngine.roll(ChestType.GOLD, 10, FixedRandom(0.0)).bonusSpins)
        val r = Random(9)
        val legendary = List(20_000) { ChestEngine.roll(ChestType.LEGENDARY, 10, r).bonusSpins }
        assertTrue(legendary.all { it in setOf(0, 20, 30) })
        assertEquals(0.16, legendary.count { it > 0 } / 20_000.0, 0.015)
        val royal = List(20_000) { ChestEngine.roll(ChestType.ROYAL, 10, r).bonusSpins }
        assertEquals(0.08, royal.count { it > 0 } / 20_000.0, 0.012)
        assertTrue(List(2000) { ChestEngine.roll(ChestType.SILVER, 10, r).bonusSpins }.all { it == 0 })
        // bonus spins are credited
        var s = startedGame()
        for (seed in 0 until 500) {
            val res = GameEngine.grantChest(s, ChestType.LEGENDARY, ctx(seed = seed))
            val c = (res.effects.first() as GameEffect.ChestOpened).contents
            if (c.bonusSpins > 0) { assertEquals(s.spins + c.spins + c.bonusSpins, res.state.spins); return }
        }
        fail("no legendary chest spin jackpot in 500 tries")
    }

    // ---------------------------------------------------------------- multipliers & lucky boost
    @Test fun x30IsAMultiplier() {
        assertFalse(30 in GameBalanceConfig.availableMultipliers(14))
        assertTrue(30 in GameBalanceConfig.availableMultipliers(15))
        assertEquals(listOf(1, 2, 3, 5, 10, 20, 30, 50, 100), GameBalanceConfig.availableMultipliers(20))
        assertEquals(listOf(1, 2, 3, 5, 10, 20, 30), GameBalanceConfig.availableMultipliers(1, luckyBoost = true))
    }

    @Test fun luckyBoostTriggersRarelyAndLastsTenMinutes() {
        val s = startedGame().copy(level = 3, spins = 500)
        val r = GameEngine.spin(s, fixedCtx(0.001), listOf(ATTACK, RAID, SHIELD))
        assertEquals(T0 + GameBalanceConfig.LUCKY_BOOST_DURATION_MS, r.state.luckyBoostUntil)
        assertTrue(r.effects.any { it is GameEffect.LuckyBoostStarted })
        assertTrue(LuckyBoost.isActive(r.state, T0 + 9 * 60_000L))
        assertFalse(LuckyBoost.isActive(r.state, T0 + 10 * 60_000L))
        assertEquals(listOf(1, 2, 3, 5, 10, 20, 30), LuckyBoost.multipliers(r.state, T0 + 60_000))
        assertEquals(listOf(1, 2, 3, 5, 10), LuckyBoost.multipliers(r.state, T0 + 11 * 60_000))
        // no boost when the roll misses
        assertEquals(0L, GameEngine.spin(s, fixedCtx(0.5), listOf(ATTACK, RAID, SHIELD)).state.luckyBoostUntil)
        // statistical frequency ≈ configured chance
        var hits = 0
        val rnd = Random(4)
        repeat(50_000) { if (GameEngine.maybeStartLuckyBoost(s, ctx().copy(random = rnd)).second != null) hits++ }
        assertEquals(GameBalanceConfig.LUCKY_BOOST_CHANCE_PER_SPIN, hits / 50_000.0, 0.003)
    }

    @Test fun luckyBoostRespectsCooldownLevelAndUsefulness() {
        val base = startedGame().copy(level = 3)
        val active = base.copy(luckyBoostUntil = T0 + 60_000)
        assertFalse(LuckyBoost.canTrigger(active, T0))
        val ended = base.copy(luckyBoostUntil = T0 - 60_000)
        assertFalse(LuckyBoost.canTrigger(ended, T0))                                    // cooldown running
        assertTrue(LuckyBoost.canTrigger(ended, T0 - 60_000 + GameBalanceConfig.LUCKY_BOOST_COOLDOWN_MS))
        assertFalse(LuckyBoost.canTrigger(startedGame().copy(level = 1), T0))           // too early
        assertFalse(LuckyBoost.canTrigger(startedGame().copy(level = 15), T0))          // x20+x30 already unlocked
        assertTrue(LuckyBoost.canTrigger(base, T0))
    }

    @Test fun boostedMultiplierCanBeSelectedAndUsedThenFallsBack() {
        val s = startedGame().copy(level = 3, spins = 200, luckyBoostUntil = T0 + 600_000)
        val sel = GameEngine.selectMultiplier(s, 30, T0)
        assertEquals(30, sel.selectedMultiplier)
        assertEquals(30, GameEngine.effectiveMultiplier(sel, T0))
        val r = GameEngine.spin(sel, fixedCtx(0.9), listOf(COIN, COIN, COIN))
        assertEquals(200 - 30, r.state.spins)
        assertEquals(30, (r.effects.first() as GameEffect.Spun).multiplier)
        // after expiry the selection falls back to the largest allowed multiplier (x10), not x1
        assertEquals(10, GameEngine.effectiveMultiplier(sel, T0 + 600_000))
        // cannot select x30 without boost
        assertEquals(1, GameEngine.selectMultiplier(s.copy(luckyBoostUntil = 0), 30, T0).selectedMultiplier)
    }

    @Test fun boostEndTimeIsPersisted() {
        val s = startedGame().copy(luckyBoostUntil = T0 + 123_456)
        assertEquals(T0 + 123_456, SaveCodec.decode(SaveCodec.encode(s)).luckyBoostUntil)
        // old saves without the field still load
        assertEquals(0L, SaveCodec.decode("""{"playerId":"x"}""").luckyBoostUntil)
    }
}
