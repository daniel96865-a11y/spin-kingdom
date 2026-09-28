package de.danielgrebe.spinkingdom.game

import de.danielgrebe.spinkingdom.*
import de.danielgrebe.spinkingdom.config.EventModifiers
import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.config.LevelConfig
import de.danielgrebe.spinkingdom.domain.Buildings
import de.danielgrebe.spinkingdom.domain.Opponent
import de.danielgrebe.spinkingdom.domain.OutcomeType
import de.danielgrebe.spinkingdom.domain.SlotEngine
import de.danielgrebe.spinkingdom.models.*
import de.danielgrebe.spinkingdom.models.SlotSymbol.*
import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {

    @Test fun introGivesStartResources() {
        val s = startedGame()
        assertTrue(s.introDone)
        assertEquals("Daniel", s.playerName)
        assertFalse(s.isGuest)
        assertEquals(GameBalanceConfig.START_COINS, s.coins)
        assertEquals(50, s.spins)
        assertEquals(10, s.missions.size) // 5 daily + 5 weekly
        val guest = GameEngine.finishIntro(GameEngine.newGame("x", T0), "  ", 0, ctx())
        assertTrue(guest.isGuest)
    }

    @Test fun spinConsumesMultiplierSpins() {
        var s = startedGame().copy(selectedMultiplier = 5)
        val r = GameEngine.spin(s, ctx(), listOf(ATTACK, RAID, SHIELD))
        assertNull(r.error)
        assertEquals(45, r.state.spins)
        assertEquals(1, r.state.stats.totalSpins)
        // regen timer starts when dropping below cap
        assertEquals(T0, r.state.lastRegenMillis)
        s = s.copy(spins = 3, selectedMultiplier = 10)
        assertEquals(3, GameEngine.effectiveMultiplier(s, T0)) // falls back to largest affordable
        s = s.copy(spins = 0)
        assertEquals(GameError.NOT_ENOUGH_SPINS, GameEngine.spin(s, ctx()).error)
    }

    @Test fun coinRewardsScaleWithMultiplierAndLevel() {
        val base = startedGame()
        val x1 = GameEngine.spin(base, ctx(), listOf(COIN, COIN, COIN)).effects.filterIsInstance<GameEffect.Spun>().first().coins
        val x10 = GameEngine.spin(base.copy(selectedMultiplier = 10), ctx(), listOf(COIN, COIN, COIN)).effects.filterIsInstance<GameEffect.Spun>().first().coins
        assertEquals(GameBalanceConfig.coins(1, GameBalanceConfig.COIN_TRIPLE_U), x1)
        assertEquals(x1 * 10, x10)
        val lvl8 = GameEngine.spin(base.copy(level = 8), ctx(), listOf(COIN, COIN, COIN)).effects.filterIsInstance<GameEffect.Spun>().first().coins
        assertTrue(lvl8 > x1 * 3)
        // pair pays less than triple but more than nothing
        val pair = GameEngine.spin(base, ctx(), listOf(COIN, COIN, RAID)).effects.filterIsInstance<GameEffect.Spun>().first().coins
        assertTrue(pair in 1 until x1)
    }

    @Test fun eventDoublesCoins() {
        val base = startedGame()
        val normal = GameEngine.spin(base, ctx(), listOf(COIN, COIN, COIN)).effects.filterIsInstance<GameEffect.Spun>().first().coins
        val ev = GameEngine.spin(base, ctx(mods = EventModifiers(coin = 2.0)), listOf(COIN, COIN, COIN)).effects.filterIsInstance<GameEffect.Spun>().first().coins
        assertEquals(normal * 2, ev)
    }

    @Test fun energyAndShieldOutcomes() {
        val s = startedGame()
        val e = GameEngine.spin(s.copy(selectedMultiplier = 2), ctx(), listOf(ENERGY, ENERGY, ENERGY))
        assertEquals(50 - 2 + 20, e.state.spins)
        val sh = GameEngine.spin(s, ctx(), listOf(SHIELD, SHIELD, SHIELD))
        assertEquals(1, sh.state.shields)
        // shields are capped; excess gives a coin consolation
        val full = s.copy(shields = GameBalanceConfig.maxShields(1))
        val r = GameEngine.spin(full, ctx(), listOf(SHIELD, SHIELD, SHIELD))
        assertEquals(3, r.state.shields)
        assertTrue(r.state.coins > full.coins)
        assertEquals(3, GameBalanceConfig.maxShields(1))
        assertEquals(5, GameBalanceConfig.maxShields(30))
    }

    @Test fun attackTripleCreatesPendingAttackWithMultiplier() {
        val s = startedGame().copy(selectedMultiplier = 3)
        val r = GameEngine.spin(s, ctx(), listOf(ATTACK, ATTACK, JOKER))
        assertEquals(PendingAction("attack", 3), r.state.pendingActions.single())
        val raid = GameEngine.spin(s, ctx(), listOf(RAID, RAID, RAID))
        assertEquals("raid", raid.state.pendingActions.single().kind)
    }

    @Test fun jackpotGivesCoinsAndChest() {
        val r = GameEngine.spin(startedGame(), ctx(), listOf(JOKER, JOKER, JOKER))
        val spun = r.effects.filterIsInstance<GameEffect.Spun>().single()
        assertEquals(OutcomeType.JACKPOT, spun.outcome.type)
        assertTrue(r.effects.any { it is GameEffect.ChestOpened })
        assertEquals(1, r.state.stats.jackpots)
    }

    @Test fun attackRewardsAndShieldBlock() {
        val s = startedGame().copy(pendingActions = listOf(PendingAction("attack", 2)))
        val opp = Opponent("o", "Mia", 1, 3, listOf(2, 2, 2, 2, 2), hasShield = false, stars = 10)
        val hit = GameEngine.resolveAttack(s, opp, 1, ctx())
        val done = hit.effects.single() as GameEffect.AttackDone
        assertFalse(done.blocked)
        assertEquals(GameBalanceConfig.coins(1, GameBalanceConfig.ATTACK_REWARD_U) * 2, done.coins)
        assertTrue(hit.state.pendingActions.isEmpty())
        assertEquals(1, hit.state.stats.attacksWon)

        val blocked = GameEngine.resolveAttack(s, opp.copy(hasShield = true), 1, ctx())
        val b = blocked.effects.single() as GameEffect.AttackDone
        assertTrue(b.blocked)
        assertTrue(b.coins in 1 until done.coins)
        assertEquals(1, blocked.state.stats.attacksBlocked)

        assertEquals(GameError.NOT_AVAILABLE, GameEngine.resolveAttack(startedGame(), opp, 0, ctx()).error)
    }

    @Test fun raidPaysSumOfThreePicks() {
        val s = startedGame().copy(pendingActions = listOf(PendingAction("raid", 1)))
        val board = listOf(RaidPrizeKind.SMALL, RaidPrizeKind.LARGE, RaidPrizeKind.JACKPOT, RaidPrizeKind.SPINS) + List(5) { RaidPrizeKind.SMALL }
        val r = GameEngine.completeRaid(s, board, listOf(1, 2, 3), ctx())
        val done = r.effects.single() as GameEffect.RaidDone
        assertEquals(GameBalanceConfig.coins(1, GameBalanceConfig.RAID_LARGE_U + GameBalanceConfig.RAID_JACKPOT_U), done.coins)
        assertEquals(GameBalanceConfig.RAID_BONUS_SPINS, done.spins)
        assertTrue(done.jackpot)
        assertEquals(s.coins + done.coins, r.state.coins)
        assertEquals(1, r.state.stats.raidsDone)
        assertEquals(9, GameEngine.rollRaidBoard(kotlin.random.Random(3)).size)
    }

    @Test fun upgradeCostsCoinsAndGivesStars() {
        val s = startedGame()
        val cost = LevelConfig.level(1).upgradeCosts[0][0]
        val r = GameEngine.upgrade(s, 0, ctx())
        assertNull(r.error)
        assertEquals(s.coins - cost, r.state.coins)
        assertEquals(1, r.state.buildings[0].stage)
        assertEquals(1, r.state.stars)
        assertEquals(GameError.NOT_ENOUGH_COINS, GameEngine.upgrade(s.copy(coins = 0), 0, ctx()).error)
        val maxed = s.copy(buildings = s.buildings.toMutableList().also { it[0] = BuildingState(5) })
        assertEquals(GameError.MAX_STAGE, GameEngine.upgrade(maxed, 0, ctx()).error)
    }

    @Test fun costsAreProgressive() {
        for (lvl in listOf(1, 2, 10, 30, 31, 100, 500)) {
            val costs = LevelConfig.level(lvl).upgradeCosts
            assertEquals(5, costs.size)
            costs.forEach { row ->
                assertEquals(5, row.size)
                for (i in 1 until row.size) assertTrue("stage costs rise", row[i] > row[i - 1])
            }
            for (b in 1 until 5) assertTrue("later buildings cost more", costs[b][0] >= costs[b - 1][0])
        }
        var prev = 0L
        for (lvl in 1..500) {
            val total = GameBalanceConfig.levelTotalCost(lvl)
            assertTrue("level $lvl must cost more than level ${lvl - 1}", total > prev)
            assertTrue(total < Long.MAX_VALUE / 1000)
            prev = total
        }
    }

    @Test fun completingAllBuildingsCompletesLevel() {
        var s = startedGame()
        s = s.copy(coins = GameBalanceConfig.levelTotalCost(1) + 10)
        val effects = mutableListOf<GameEffect>()
        for (b in 0 until 5) for (st in 0 until 5) {
            val r = GameEngine.upgrade(s, b, ctx())
            assertNull("b=$b st=$st", r.error)
            s = r.state; effects += r.effects
        }
        val lc = effects.filterIsInstance<GameEffect.LevelCompleted>().single()
        assertEquals(1, lc.completedLevel)
        assertEquals(2, s.level)
        assertTrue(s.buildings.all { it.stage == 0 })
        assertEquals(25 + GameBalanceConfig.STARS_PER_LEVEL_COMPLETE, s.stars)
        assertTrue(effects.any { it is GameEffect.ChestOpened })
        assertEquals(1, s.stats.levelsCompleted)
    }

    @Test fun damagedBuildingBlocksCompletionUntilRepaired() {
        var s = startedGame().copy(buildings = List(5) { BuildingState(5) }.toMutableList().also { it[2] = BuildingState(5, damaged = true) }, coins = 1_000_000)
        assertFalse(Buildings.isLevelComplete(s))
        val r = GameEngine.upgrade(s, 2, ctx())
        assertTrue(r.effects.first() is GameEffect.BuildingRepaired)
        assertTrue(r.effects.any { it is GameEffect.LevelCompleted })
        assertEquals(2, r.state.level)
        val cost = GameBalanceConfig.repairCost(1, 2, 5)
        assertTrue(cost < GameBalanceConfig.upgradeCost(1, 2, 5))
    }

    @Test fun botAttacksUseShieldsFirstAndAreLimited() {
        val base = startedGame().copy(level = 3, shields = 3, buildings = List(5) { BuildingState(3) })
        // 2 days away: many attack windows, but at most 3 events
        val later = ctx(T0 + 2 * DAY, seed = 7)
        val s = GameEngine.simulateBotAttacks(base, later, listOf("Alex", "Mia"))
        assertTrue(s.log.size <= GameBalanceConfig.BOT_ATTACK_MAX_PER_RETURN)
        val blocked = s.log.count { it.kind == "bot_attack_blocked" }
        val hits = s.log.count { it.kind == "bot_attack_hit" }
        assertEquals(3 - blocked, s.shields)
        if (hits > 0) assertEquals(0, s.shields) // hits only after shields are gone
        assertEquals(hits, s.buildings.count { it.damaged })
        // no double processing
        assertEquals(s, GameEngine.simulateBotAttacks(s, later, listOf("Alex")))
        // level 1 is protected
        val newbie = startedGame()
        assertTrue(GameEngine.simulateBotAttacks(newbie, later, listOf("Alex")).log.isEmpty())
    }

    @Test fun botAttacksHappenStatistically() {
        var hits = 0
        repeat(200) { seed ->
            val base = startedGame().copy(level = 3, shields = 0, buildings = List(5) { BuildingState(2) })
            hits += GameEngine.simulateBotAttacks(base, ctx(T0 + DAY, seed = seed), listOf("Leon")).log.size
        }
        assertTrue(hits in 200..600)
    }

    @Test fun petEffectsApply() {
        val base = startedGame().copy(level = 12)
        val s = de.danielgrebe.spinkingdom.domain.Pets.syncUnlocks(base)
        assertEquals(4, s.pets.size)
        val raccoon = GameEngine.setActivePet(s, PetType.RACCOON)
        val plain = GameEngine.spin(s.copy(activePet = null), ctx(), listOf(COIN, COIN, COIN)).effects.filterIsInstance<GameEffect.Spun>().first().coins
        val boosted = GameEngine.spin(raccoon, ctx(), listOf(COIN, COIN, COIN)).effects.filterIsInstance<GameEffect.Spun>().first().coins
        assertTrue(boosted > plain)
        // feeding
        val fed = GameEngine.feedPet(raccoon.copy(petTreats = 3), PetType.RACCOON)
        assertEquals(2, fed.state.petTreats)
        assertEquals(GameBalanceConfig.TREAT_XP, fed.state.pets.first { it.type == PetType.RACCOON }.xp)
        assertEquals(GameError.NO_TREATS, GameEngine.feedPet(raccoon.copy(petTreats = 0), PetType.RACCOON).error)
        // level up after enough treats
        var p = raccoon.copy(petTreats = 10)
        val ups = mutableListOf<GameEffect>()
        repeat(3) { val r = GameEngine.feedPet(p, PetType.RACCOON); p = r.state; ups += r.effects }
        assertEquals(2, p.pets.first { it.type == PetType.RACCOON }.level)
        assertTrue(ups.any { it is GameEffect.PetLevelUp })
    }

    @Test fun petsUnlockByLevel() {
        assertTrue(de.danielgrebe.spinkingdom.domain.Pets.syncUnlocks(startedGame()).pets.isEmpty())
        val fox = de.danielgrebe.spinkingdom.domain.Pets.syncUnlocks(startedGame().copy(level = 5))
        assertEquals(listOf(PetType.FOX), fox.pets.map { it.type })
        assertEquals(PetType.FOX, fox.activePet)
    }

    @Test fun shopAndAds() {
        val s = startedGame()
        val product = de.danielgrebe.spinkingdom.data.FakeBillingRepository().products().first { it.id == "spins_small" }
        val r = GameEngine.grantProduct(s, product, ctx())
        assertEquals(s.spins + 150, r.state.spins)
        assertEquals(1, r.state.purchases)
        val ad = GameEngine.grantAdSpins(s)
        assertEquals(s.spins + 10, ad.state.spins)
        val dbl = GameEngine.doubleReward(s, 500, 5)
        assertEquals(s.coins + 500, dbl.state.coins)
        assertEquals(s.spins + 5, dbl.state.spins)
    }

    @Test fun longSimulationStaysConsistent() {
        // play 3000 random spins with auto-building: state must stay valid and the player progresses
        var s = startedGame()
        val c = ctx(seed = 99)
        var now = T0
        repeat(3000) {
            now += 60_000
            val cc = c.copy(now = now, today = now / DAY, week = (now / DAY) - (((now / DAY) + 3) % 7))
            s = GameEngine.tick(s, cc)
            if (s.spins <= 0) s = s.copy(spins = 50)
            s = GameEngine.spin(s, cc).state
            s.pendingActions.firstOrNull()?.let { p ->
                s = if (p.kind == "attack") GameEngine.resolveAttack(s, Opponent("b", "Bot", 0, 1, List(5) { 1 }, false, 0), 0, cc).state
                else GameEngine.completeRaid(s, GameEngine.rollRaidBoard(cc.random), listOf(0, 4, 8), cc).state
            }
            for (b in 0 until 5) {
                val r = GameEngine.upgrade(s, b, cc)
                if (r.error == null) s = r.state
            }
            assertTrue(s.coins >= 0 && s.spins >= 0)
            assertTrue(s.shields <= GameBalanceConfig.maxShields(s.level))
            assertTrue(s.buildings.all { it.stage in 0..5 })
        }
        assertTrue("player should complete several villages, was level ${s.level}", s.level >= 3)
    }

    @Test fun slotProbabilitiesGiveReasonableEconomy() {
        // expected coin units per x1 spin should allow finishing level 1 in a sensible number of spins
        val p = SlotEngine.outcomeProbabilities()
        val ev = p.getValue(OutcomeType.COIN_TRIPLE) * GameBalanceConfig.COIN_TRIPLE_U +
            p.getValue(OutcomeType.COIN_PAIR) * GameBalanceConfig.COIN_PAIR_U +
            p.getValue(OutcomeType.OTHER_PAIR) * GameBalanceConfig.OTHER_PAIR_U +
            p.getValue(OutcomeType.SINGLE_COIN) * GameBalanceConfig.SINGLE_COIN_U
        val spinsForLevel1 = GameBalanceConfig.levelTotalCost(1) / (ev * GameBalanceConfig.coinUnit(1))
        assertTrue("spins needed for level 1: $spinsForLevel1", spinsForLevel1 in 30.0..400.0)
    }
}
