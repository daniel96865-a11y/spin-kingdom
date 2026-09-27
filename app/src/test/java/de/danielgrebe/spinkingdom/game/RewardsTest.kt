package de.danielgrebe.spinkingdom.game

import de.danielgrebe.spinkingdom.*
import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.domain.Cards
import de.danielgrebe.spinkingdom.domain.ChestEngine
import de.danielgrebe.spinkingdom.models.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class RewardsTest {

    @Test fun dailyBonusSevenDayCycle() {
        var s = startedGame()
        val got = mutableListOf<Int>()
        for (d in 0 until 9) {
            val r = GameEngine.claimDaily(s, ctx(T0 + d * DAY), clockTampered = false)
            assertNull("day $d", r.error)
            got += (r.effects.first() as GameEffect.DailyClaimed).dayIndex
            s = r.state
        }
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 0, 1), got)
        assertEquals(9, s.dailyBonus.totalClaims)
    }

    @Test fun dailyBonusRewardsMatchConfig() {
        val s = startedGame()
        val day1 = GameEngine.claimDaily(s, ctx(), false)
        assertEquals(s.coins + GameBalanceConfig.coins(1, 10.0), day1.state.coins)
        val day2 = GameEngine.claimDaily(day1.state, ctx(T0 + DAY), false)
        assertEquals(day1.state.spins + 15, day2.state.spins)
        val day3 = GameEngine.claimDaily(day2.state, ctx(T0 + 2 * DAY), false)
        assertTrue(day3.effects.any { it is GameEffect.ChestOpened && it.contents.type == ChestType.SILVER })
    }

    @Test fun dailyBonusOncePerDayAndStreakResets() {
        val s = GameEngine.claimDaily(startedGame(), ctx(), false).state
        assertEquals(GameError.ALREADY_CLAIMED, GameEngine.claimDaily(s, ctx(T0 + 1000), false).error)
        val s2 = GameEngine.claimDaily(s, ctx(T0 + DAY), false).state
        // skip two days -> back to day 1
        val r = GameEngine.claimDaily(s2, ctx(T0 + 4 * DAY), false)
        assertEquals(0, (r.effects.first() as GameEffect.DailyClaimed).dayIndex)
    }

    @Test fun clockTamperingBlocksDailyAndWheel() {
        val s = startedGame()
        assertEquals(GameError.CLOCK_TAMPERED, GameEngine.claimDaily(s, ctx(), true).error)
        assertEquals(GameError.CLOCK_TAMPERED, GameEngine.spinWheel(s, ctx(), true).error)
    }

    @Test fun wheelOncePerDay() {
        val s = startedGame()
        val r = GameEngine.spinWheel(s, ctx(), false)
        assertNull(r.error)
        assertEquals(ctx().today, r.state.lastWheelDay)
        assertEquals(GameError.ALREADY_CLAIMED, GameEngine.spinWheel(r.state, ctx(T0 + 5000), false).error)
        assertNull(GameEngine.spinWheel(r.state, ctx(T0 + DAY), false).error)
        // weights: jackpot is the rarest segment
        val counts = IntArray(GameBalanceConfig.WHEEL.size)
        val rnd = Random(5)
        repeat(100_000) { counts[GameEngine.pickWheelSegment(rnd)]++ }
        assertEquals(counts.indices.minBy { counts[it] }, GameBalanceConfig.WHEEL.indexOfFirst { it.kind == GameBalanceConfig.WheelPrizeKind.JACKPOT })
    }

    @Test fun missionsTrackClaimAndReset() {
        var s = startedGame()
        val spinMission = s.missions.first { it.period == MissionPeriod.DAILY && it.type == MissionType.SPINS }
        assertEquals(20, spinMission.target)
        assertEquals(GameError.NOT_AVAILABLE, GameEngine.claimMission(s, spinMission.id, ctx()).error)
        repeat(20) { s = GameEngine.spin(s.copy(spins = 50), ctx(seed = it)).state }
        val m = s.missions.first { it.id == spinMission.id }
        assertTrue(m.completed)
        val before = s.coins
        val r = GameEngine.claimMission(s, m.id, ctx())
        assertNull(r.error)
        assertTrue(r.state.coins > before)
        assertEquals(GameError.ALREADY_CLAIMED, GameEngine.claimMission(r.state, m.id, ctx()).error)
        // weekly spin mission progressed too
        assertEquals(20, r.state.missions.first { it.period == MissionPeriod.WEEKLY && it.type == MissionType.SPINS }.progress)
        // next day: daily missions renewed, weekly kept
        val next = GameEngine.tick(r.state, ctx(T0 + DAY))
        val daily = next.missions.filter { it.period == MissionPeriod.DAILY }
        assertEquals(5, daily.size)
        assertTrue(daily.all { it.progress == 0L && !it.claimed })
        val weeklyNext = next.missions.first { it.period == MissionPeriod.WEEKLY && it.type == MissionType.SPINS }
        val sameWeek = ctx(T0 + DAY).week == ctx().week
        assertEquals(if (sameWeek) 20L else 0L, weeklyNext.progress)
        // next week: weekly renewed
        val nw = GameEngine.tick(next, ctx(T0 + 8 * DAY))
        assertTrue(nw.missions.filter { it.period == MissionPeriod.WEEKLY }.all { it.progress == 0L })
    }

    @Test fun coinMissionTargetScalesWithLevel() {
        val l1 = startedGame().missions.first { it.type == MissionType.COLLECT_COINS }.target
        val l10 = GameEngine.refreshMissions(startedGame().copy(level = 10, dailyMissionDay = -1), ctx()).missions.first { it.type == MissionType.COLLECT_COINS }.target
        assertTrue(l10 > l1 * 4)
    }

    @Test fun chestContentsFollowSpecs() {
        val rnd = Random(11)
        for (type in ChestType.entries) {
            val spec = GameBalanceConfig.CHESTS.getValue(type)
            repeat(200) {
                val c = ChestEngine.roll(type, 20, rnd)
                assertEquals(spec.cards, c.cards.size)
                assertTrue(c.cards.any { Cards.rarity(it).ordinal >= spec.minRarity.ordinal })
                assertTrue(c.coins >= GameBalanceConfig.coins(20, spec.coinsMinU))
                assertTrue(c.spins == 0 || c.spins in spec.spinsMin..spec.spinsMax)
                assertTrue(c.treats in spec.treatsMin..spec.treatsMax)
            }
        }
        // before cards unlock, chests pay coins instead
        val early = ChestEngine.roll(ChestType.GOLD, 1, rnd)
        assertTrue(early.cards.isEmpty())
        assertEquals(0, early.treats)
        // better chests are worth more on average
        fun avg(t: ChestType) = (0 until 300).map { ChestEngine.roll(t, 10, Random(it)).coins }.average()
        assertTrue(avg(ChestType.WOOD) < avg(ChestType.SILVER))
        assertTrue(avg(ChestType.GOLD) < avg(ChestType.LEGENDARY))
    }

    @Test fun chestUpgradeFromEvents() {
        assertEquals(ChestType.GOLD, ChestEngine.upgraded(ChestType.SILVER, 1))
        assertEquals(ChestType.LEGENDARY, ChestEngine.upgraded(ChestType.ROYAL, 5))
    }

    @Test fun grantChestAddsCardsAndDetectsSetCompletion() {
        val s = startedGame().copy(level = 20)
        // all but one card of set 0
        val almost = s.copy(cards = Cards.cardsOfSet(0).dropLast(1).associateWith { 1 })
        var found = false
        for (seed in 0 until 400) {
            val r = GameEngine.grantChest(almost, ChestType.LEGENDARY, ctx(seed = seed))
            if (r.effects.any { it is GameEffect.SetCompleted && it.set == 0 }) { found = true; assertTrue(Cards.isSetComplete(r.state.cards, 0)); break }
        }
        assertTrue(found)
        val r = GameEngine.grantChest(s, ChestType.GOLD, ctx())
        assertEquals(5, r.state.cards.values.sum())
        assertEquals(1, r.state.stats.chestsOpened)
    }

    @Test fun cardCatalogueAndSetRewards() {
        assertEquals(54, Cards.TOTAL)
        assertEquals(Rarity.COMMON, Cards.rarity(0))
        assertEquals(Rarity.LEGENDARY, Cards.rarity(8))
        assertEquals(listOf(0), Cards.unlockedSets(3))
        assertEquals(6, Cards.unlockedSets(18).size)
        val s = startedGame().copy(level = 10)
        assertEquals(GameError.NOT_AVAILABLE, GameEngine.claimSet(s, 0, ctx()).error)
        val complete = s.copy(cards = Cards.cardsOfSet(0).associateWith { 1 })
        val r = GameEngine.claimSet(complete, 0, ctx())
        assertNull(r.error)
        assertEquals(setOf(0), r.state.claimedSets)
        assertTrue(r.state.spins >= complete.spins + GameBalanceConfig.setRewardSpins(0))
        assertEquals(GameError.ALREADY_CLAIMED, GameEngine.claimSet(r.state, 0, ctx()).error)
    }

    @Test fun rarityDistribution() {
        val rnd = Random(3)
        val n = 50_000
        val legendary = (0 until n).count { Cards.rollRarity(rnd) == Rarity.LEGENDARY }
        assertEquals(0.02, legendary.toDouble() / n, 0.005)
    }
}
