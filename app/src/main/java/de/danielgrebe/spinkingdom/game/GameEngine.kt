package de.danielgrebe.spinkingdom.game

import de.danielgrebe.spinkingdom.config.EventModifiers
import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.config.GameBalanceConfig.MAX_STAGE
import de.danielgrebe.spinkingdom.config.LevelConfig
import de.danielgrebe.spinkingdom.domain.Buildings
import de.danielgrebe.spinkingdom.domain.Cards
import de.danielgrebe.spinkingdom.domain.ChestContents
import de.danielgrebe.spinkingdom.domain.ChestEngine
import de.danielgrebe.spinkingdom.domain.Opponent
import de.danielgrebe.spinkingdom.domain.OutcomeType
import de.danielgrebe.spinkingdom.domain.Pets
import de.danielgrebe.spinkingdom.domain.ShopProduct
import de.danielgrebe.spinkingdom.domain.SlotEngine
import de.danielgrebe.spinkingdom.domain.SlotOutcome
import de.danielgrebe.spinkingdom.domain.SpinRegen
import de.danielgrebe.spinkingdom.models.BuildingState
import de.danielgrebe.spinkingdom.models.ChestType
import de.danielgrebe.spinkingdom.models.GameState
import de.danielgrebe.spinkingdom.models.LogEntry
import de.danielgrebe.spinkingdom.models.MissionPeriod
import de.danielgrebe.spinkingdom.models.MissionState
import de.danielgrebe.spinkingdom.models.MissionType
import de.danielgrebe.spinkingdom.models.PendingAction
import de.danielgrebe.spinkingdom.models.PetType
import de.danielgrebe.spinkingdom.models.SlotSymbol
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

/** Everything time/randomness related that an action needs. */
data class Ctx(
    val now: Long,
    val today: Long,          // epoch day in the user's zone (trusted clock)
    val week: Long,           // epoch day of the Monday of the current week
    val mods: EventModifiers = EventModifiers.NONE,
    val random: Random = Random.Default
)

sealed interface GameEffect {
    data class Spun(
        val reels: List<SlotSymbol>,
        val outcome: SlotOutcome,
        val multiplier: Int,
        val coins: Long,
        val spins: Int,
        val shieldGained: Boolean,
        val freeSpin: Boolean
    ) : GameEffect
    data class ChestOpened(val contents: ChestContents, val newCards: List<Int>) : GameEffect
    data class BuildingUpgraded(val index: Int, val stage: Int, val cost: Long) : GameEffect
    data class BuildingRepaired(val index: Int, val cost: Long) : GameEffect
    data class LevelCompleted(val completedLevel: Int, val coins: Long, val spins: Int, val chest: ChestType) : GameEffect
    data class PetLevelUp(val pet: PetType, val level: Int) : GameEffect
    data class SetCompleted(val set: Int) : GameEffect
    data class AttackDone(val opponent: Opponent, val buildingIndex: Int, val blocked: Boolean, val coins: Long) : GameEffect
    data class RaidDone(val coins: Long, val spins: Int, val jackpot: Boolean) : GameEffect
    data class DailyClaimed(val dayIndex: Int, val coins: Long, val spins: Int) : GameEffect
    data class WheelSpun(val segment: Int, val coins: Long, val spins: Int, val treats: Int) : GameEffect
    data class MissionClaimed(val id: String, val coins: Long, val spins: Int) : GameEffect
    data class Purchased(val product: ShopProduct, val coins: Long, val spins: Int) : GameEffect
    data class Rewarded(val coins: Long, val spins: Int) : GameEffect
}

enum class GameError { NOT_ENOUGH_SPINS, NOT_ENOUGH_COINS, MAX_STAGE, ALREADY_CLAIMED, NOT_AVAILABLE, CLOCK_TAMPERED, NO_TREATS }

data class ActionResult(val state: GameState, val effects: List<GameEffect> = emptyList(), val error: GameError? = null) {
    val ok get() = error == null
}

enum class RaidPrizeKind { SMALL, LARGE, JACKPOT, SPINS }

object GameEngine {

    // ------------------------------------------------------------------ setup
    fun newGame(playerId: String, now: Long): GameState =
        GameState(playerId = playerId, createdAt = now, lastRegenMillis = now, lastBotCheckMillis = now)

    fun finishIntro(state: GameState, name: String?, avatar: Int, ctx: Ctx): GameState {
        val s = state.copy(
            introDone = true,
            playerName = name?.trim()?.take(16).orEmpty(),
            isGuest = name.isNullOrBlank(),
            avatar = avatar,
            coins = GameBalanceConfig.START_COINS,
            spins = GameBalanceConfig.START_SPINS,
            lastRegenMillis = ctx.now,
            lastBotCheckMillis = ctx.now,
            shields = 0
        )
        return refreshMissions(s, ctx)
    }

    /** Periodic housekeeping: spin regen, mission resets, pet unlocks. */
    fun tick(state: GameState, ctx: Ctx): GameState {
        if (!state.introDone) return state
        var s = SpinRegen.apply(state, ctx.now)
        s = refreshMissions(s, ctx)
        s = Pets.syncUnlocks(s)
        return s
    }

    // ------------------------------------------------------------------ helpers
    private fun coinFactor(state: GameState, ctx: Ctx, mult: Int): Double =
        mult * ctx.mods.coin * (1.0 + Pets.activeEffect(state, PetType.RACCOON))

    private fun scaledSpins(base: Int, mult: Int, ctx: Ctx) = (base * mult * ctx.mods.spins).roundToInt()

    private fun earn(state: GameState, coins: Long): GameState {
        if (coins <= 0) return state
        val st = state.stats.copy(coinsEarned = state.stats.coinsEarned + coins)
        return track(state.copy(coins = state.coins + coins, stats = st), MissionType.COLLECT_COINS, coins)
    }

    /** Rolls and applies a chest; returns updated state and the effects for the opening animation. */
    fun grantChest(state: GameState, type: ChestType, ctx: Ctx): ActionResult {
        val t = ChestEngine.upgraded(type, ctx.mods.chestUpgrade)
        val contents = ChestEngine.roll(t, state.level, ctx.random)
        val effects = mutableListOf<GameEffect>()
        var s = earn(state, contents.coins)
        s = s.copy(spins = s.spins + contents.spins, petTreats = s.petTreats + contents.treats)
        val newCards = contents.cards.filter { (s.cards[it] ?: 0) == 0 }.distinct()
        if (contents.cards.isNotEmpty()) {
            val cards = s.cards.toMutableMap()
            contents.cards.forEach { cards[it] = (cards[it] ?: 0) + 1 }
            val before = (0 until GameBalanceConfig.CARD_SETS).filter { Cards.isSetComplete(s.cards, it) }.toSet()
            s = s.copy(cards = cards)
            (0 until GameBalanceConfig.CARD_SETS).filter { it !in before && Cards.isSetComplete(cards, it) }
                .forEach { effects += GameEffect.SetCompleted(it) }
        }
        if (contents.petXp > 0) {
            val r = Pets.addXp(s, contents.petXp)
            s = r.state
            r.levelUps.forEach { effects += GameEffect.PetLevelUp(it.first, it.second) }
        }
        s = s.copy(stats = s.stats.copy(chestsOpened = s.stats.chestsOpened + 1))
        s = track(s, MissionType.OPEN_CHESTS, 1)
        return ActionResult(s, listOf(GameEffect.ChestOpened(contents, newCards)) + effects)
    }

    fun effectiveMultiplier(state: GameState): Int {
        val allowed = GameBalanceConfig.availableMultipliers(state.level)
        val sel = if (state.selectedMultiplier in allowed) state.selectedMultiplier else 1
        return allowed.filter { it <= sel && it <= state.spins }.maxOrNull() ?: 1
    }

    fun selectMultiplier(state: GameState, m: Int): GameState =
        if (m in GameBalanceConfig.availableMultipliers(state.level)) state.copy(selectedMultiplier = m) else state

    // ------------------------------------------------------------------ slot machine
    fun spin(state: GameState, ctx: Ctx, forcedReels: List<SlotSymbol>? = null): ActionResult {
        if (state.spins <= 0) return ActionResult(state, error = GameError.NOT_ENOUGH_SPINS)
        val mult = effectiveMultiplier(state)
        val free = ctx.random.nextDouble() < Pets.activeEffect(state, PetType.PHOENIX)
        var s = state
        val wasFull = s.spins >= GameBalanceConfig.MAX_AUTO_SPINS
        if (!free) {
            s = s.copy(spins = s.spins - mult)
            if (wasFull && s.spins < GameBalanceConfig.MAX_AUTO_SPINS) s = s.copy(lastRegenMillis = ctx.now)
        }
        val reels = forcedReels ?: SlotEngine.roll(ctx.random)
        val outcome = SlotEngine.evaluate(reels)
        val cf = coinFactor(s, ctx, mult)
        var coins = 0L
        var spins = 0
        var shield = false
        val effects = mutableListOf<GameEffect>()
        val lvl = s.level
        when (outcome.type) {
            OutcomeType.COIN_TRIPLE -> coins = (GameBalanceConfig.coins(lvl, GameBalanceConfig.COIN_TRIPLE_U) * cf).roundToLong()
            OutcomeType.COIN_PAIR -> coins = (GameBalanceConfig.coins(lvl, GameBalanceConfig.COIN_PAIR_U) * cf).roundToLong()
            OutcomeType.OTHER_PAIR -> coins = (GameBalanceConfig.coins(lvl, GameBalanceConfig.OTHER_PAIR_U) * cf).roundToLong()
            OutcomeType.SINGLE_COIN -> coins = (GameBalanceConfig.coins(lvl, GameBalanceConfig.SINGLE_COIN_U) * cf).roundToLong()
            OutcomeType.ENERGY -> spins = scaledSpins(GameBalanceConfig.ENERGY_TRIPLE_SPINS, mult, ctx)
            OutcomeType.ENERGY_PAIR -> spins = scaledSpins(GameBalanceConfig.ENERGY_PAIR_SPINS, mult, ctx)
            OutcomeType.SHIELD -> {
                if (s.shields < GameBalanceConfig.maxShields(lvl)) { s = s.copy(shields = s.shields + 1); shield = true }
                else coins = (GameBalanceConfig.coins(lvl, GameBalanceConfig.SHIELD_FULL_CONSOLATION_U) * cf).roundToLong()
            }
            OutcomeType.ATTACK -> s = s.copy(pendingActions = s.pendingActions + PendingAction("attack", mult))
            OutcomeType.RAID -> s = s.copy(pendingActions = s.pendingActions + PendingAction("raid", mult))
            OutcomeType.CHEST -> {
                val type = when { mult >= 50 -> ChestType.ROYAL; mult >= 10 -> ChestType.GOLD; else -> ChestType.SILVER }
                val r = grantChest(s, type, ctx); s = r.state; effects += r.effects
            }
            OutcomeType.JACKPOT -> {
                coins = (GameBalanceConfig.coins(lvl, GameBalanceConfig.JACKPOT_U) * cf).roundToLong()
                s = s.copy(stats = s.stats.copy(jackpots = s.stats.jackpots + 1))
                val r = grantChest(s, ChestType.GOLD, ctx); s = r.state; effects += r.effects
            }
            OutcomeType.NOTHING -> Unit
        }
        s = earn(s, coins)
        s = s.copy(
            spins = s.spins + spins,
            stats = s.stats.copy(totalSpins = s.stats.totalSpins + 1, biggestWin = maxOf(s.stats.biggestWin, coins))
        )
        s = track(s, MissionType.SPINS, 1)
        return ActionResult(s, listOf(GameEffect.Spun(reels, outcome, mult, coins, spins, shield, free)) + effects)
    }

    // ------------------------------------------------------------------ buildings
    fun upgrade(state: GameState, index: Int, ctx: Ctx): ActionResult {
        val b = state.buildings.getOrNull(index) ?: return ActionResult(state, error = GameError.NOT_AVAILABLE)
        if (b.damaged) {
            val cost = GameBalanceConfig.repairCost(state.level, index, b.stage)
            if (state.coins < cost) return ActionResult(state, error = GameError.NOT_ENOUGH_COINS)
            val list = state.buildings.toMutableList().also { it[index] = b.copy(damaged = false) }
            val s = state.copy(coins = state.coins - cost, buildings = list)
            val eff = mutableListOf<GameEffect>(GameEffect.BuildingRepaired(index, cost))
            return if (Buildings.isLevelComplete(s)) completeLevel(s, ctx).let { it.copy(effects = eff + it.effects) } else ActionResult(s, eff)
        }
        if (b.stage >= MAX_STAGE) return ActionResult(state, error = GameError.MAX_STAGE)
        val cost = LevelConfig.level(state.level).upgradeCosts[index][b.stage]
        if (state.coins < cost) return ActionResult(state, error = GameError.NOT_ENOUGH_COINS)
        val list = state.buildings.toMutableList().also { it[index] = b.copy(stage = b.stage + 1) }
        var s = state.copy(
            coins = state.coins - cost,
            buildings = list,
            stars = state.stars + GameBalanceConfig.STARS_PER_UPGRADE,
            stats = state.stats.copy(buildingsUpgraded = state.stats.buildingsUpgraded + 1)
        )
        s = track(s, MissionType.UPGRADES, 1)
        val eff = mutableListOf<GameEffect>(GameEffect.BuildingUpgraded(index, b.stage + 1, cost))
        if (Buildings.isLevelComplete(s)) {
            val r = completeLevel(s, ctx)
            return r.copy(effects = eff + r.effects)
        }
        return ActionResult(s, eff)
    }

    fun completeLevel(state: GameState, ctx: Ctx): ActionResult {
        val done = state.level
        val coins = GameBalanceConfig.coins(done, GameBalanceConfig.LEVEL_COMPLETE_U)
        val spins = GameBalanceConfig.levelCompleteSpins(done)
        val chest = GameBalanceConfig.levelCompleteChest(done)
        var s = earn(state, coins)
        s = s.copy(
            level = (done + 1).coerceAtMost(LevelConfig.MAX_LEVEL),
            spins = s.spins + spins,
            stars = s.stars + GameBalanceConfig.STARS_PER_LEVEL_COMPLETE,
            buildings = List(GameBalanceConfig.BUILDINGS_PER_LEVEL) { BuildingState() },
            stats = s.stats.copy(levelsCompleted = s.stats.levelsCompleted + 1)
        )
        s = Pets.syncUnlocks(s)
        val c = grantChest(s, chest, ctx)
        return ActionResult(c.state, listOf(GameEffect.LevelCompleted(done, coins, spins, chest)) + c.effects)
    }

    // ------------------------------------------------------------------ attack & raid
    fun nextPending(state: GameState): PendingAction? = state.pendingActions.firstOrNull()

    private fun popPending(state: GameState, kind: String): Pair<GameState, PendingAction?> {
        val idx = state.pendingActions.indexOfFirst { it.kind == kind }
        if (idx < 0) return state to null
        val p = state.pendingActions[idx]
        return state.copy(pendingActions = state.pendingActions.toMutableList().also { it.removeAt(idx) }) to p
    }

    fun resolveAttack(state: GameState, opponent: Opponent, buildingIndex: Int, ctx: Ctx): ActionResult {
        val (s0, p) = popPending(state, "attack")
        if (p == null) return ActionResult(state, error = GameError.NOT_AVAILABLE)
        val mult = p.multiplier
        val units = if (opponent.hasShield) GameBalanceConfig.ATTACK_BLOCKED_U else GameBalanceConfig.ATTACK_REWARD_U
        val bonus = if (opponent.hasShield) 1.0 else 1.0 + Pets.activeEffect(s0, PetType.DRAGON)
        val coins = (GameBalanceConfig.coins(s0.level, units) * mult * ctx.mods.attack * bonus).roundToLong()
        var s = earn(s0, coins)
        s = s.copy(stats = if (opponent.hasShield) s.stats.copy(attacksBlocked = s.stats.attacksBlocked + 1) else s.stats.copy(attacksWon = s.stats.attacksWon + 1))
        s = track(s, MissionType.ATTACKS, 1)
        return ActionResult(s, listOf(GameEffect.AttackDone(opponent, buildingIndex, opponent.hasShield, coins)))
    }

    fun rollRaidBoard(random: Random): List<RaidPrizeKind> {
        val kinds = RaidPrizeKind.entries
        val weights = GameBalanceConfig.RAID_WEIGHTS
        return List(GameBalanceConfig.RAID_SPOTS) {
            var r = random.nextInt(weights.sum()); var k = kinds.first()
            for (i in kinds.indices) { if (r < weights[i]) { k = kinds[i]; break }; r -= weights[i] }
            k
        }
    }

    fun raidPrizeValue(state: GameState, kind: RaidPrizeKind, mult: Int, ctx: Ctx): Pair<Long, Int> {
        val f = mult * ctx.mods.raid * (1.0 + Pets.activeEffect(state, PetType.FOX))
        return when (kind) {
            RaidPrizeKind.SMALL -> (GameBalanceConfig.coins(state.level, GameBalanceConfig.RAID_SMALL_U) * f).roundToLong() to 0
            RaidPrizeKind.LARGE -> (GameBalanceConfig.coins(state.level, GameBalanceConfig.RAID_LARGE_U) * f).roundToLong() to 0
            RaidPrizeKind.JACKPOT -> (GameBalanceConfig.coins(state.level, GameBalanceConfig.RAID_JACKPOT_U) * f).roundToLong() to 0
            RaidPrizeKind.SPINS -> 0L to scaledSpins(GameBalanceConfig.RAID_BONUS_SPINS, mult, ctx)
        }
    }

    fun pendingMultiplier(state: GameState, kind: String) = state.pendingActions.firstOrNull { it.kind == kind }?.multiplier ?: 1

    fun completeRaid(state: GameState, board: List<RaidPrizeKind>, picks: List<Int>, ctx: Ctx): ActionResult {
        require(picks.size <= GameBalanceConfig.RAID_PICKS && picks.distinct().size == picks.size)
        val (s0, p) = popPending(state, "raid")
        if (p == null) return ActionResult(state, error = GameError.NOT_AVAILABLE)
        var coins = 0L; var spins = 0; var jackpot = false
        picks.forEach { i ->
            val (c, sp) = raidPrizeValue(s0, board[i], p.multiplier, ctx)
            coins += c; spins += sp
            if (board[i] == RaidPrizeKind.JACKPOT) jackpot = true
        }
        var s = earn(s0, coins)
        s = s.copy(spins = s.spins + spins, stats = s.stats.copy(raidsDone = s.stats.raidsDone + 1))
        s = track(s, MissionType.RAIDS, 1)
        return ActionResult(s, listOf(GameEffect.RaidDone(coins, spins, jackpot)))
    }

    // ------------------------------------------------------------------ daily bonus & wheel
    fun dailyIndexFor(state: GameState, today: Long): Int {
        val d = state.dailyBonus
        return if (d.lastClaimDay == today - 1 || d.lastClaimDay == today) d.streakDay % 7 else 0
    }

    fun canClaimDaily(state: GameState, today: Long) = state.dailyBonus.lastClaimDay < today

    fun claimDaily(state: GameState, ctx: Ctx, clockTampered: Boolean): ActionResult {
        if (clockTampered) return ActionResult(state, error = GameError.CLOCK_TAMPERED)
        if (!canClaimDaily(state, ctx.today)) return ActionResult(state, error = GameError.ALREADY_CLAIMED)
        val idx = dailyIndexFor(state, ctx.today)
        val reward = GameBalanceConfig.DAILY_BONUS[idx]
        val coins = GameBalanceConfig.coins(state.level, reward.coinsU)
        val spins = (reward.spins * ctx.mods.spins).roundToInt()
        var s = earn(state, coins).let { it.copy(spins = it.spins + spins) }
        s = s.copy(dailyBonus = s.dailyBonus.copy(streakDay = (idx + 1) % 7, lastClaimDay = ctx.today, totalClaims = s.dailyBonus.totalClaims + 1))
        val effects = mutableListOf<GameEffect>(GameEffect.DailyClaimed(idx, coins, spins))
        if (reward.chest != null) { val r = grantChest(s, reward.chest, ctx); s = r.state; effects += r.effects }
        return ActionResult(s, effects)
    }

    fun canSpinWheel(state: GameState, today: Long) = state.lastWheelDay < today

    fun pickWheelSegment(random: Random): Int {
        val w = GameBalanceConfig.WHEEL
        var r = random.nextInt(w.sumOf { it.weight })
        for (i in w.indices) { if (r < w[i].weight) return i; r -= w[i].weight }
        return 0
    }

    fun spinWheel(state: GameState, ctx: Ctx, clockTampered: Boolean): ActionResult {
        if (clockTampered) return ActionResult(state, error = GameError.CLOCK_TAMPERED)
        if (!canSpinWheel(state, ctx.today)) return ActionResult(state, error = GameError.ALREADY_CLAIMED)
        val idx = pickWheelSegment(ctx.random)
        val seg = GameBalanceConfig.WHEEL[idx]
        val petsOn = state.pets.isNotEmpty()
        val treats = if (seg.kind == GameBalanceConfig.WheelPrizeKind.TREATS && petsOn) seg.treats else 0
        val coinsU = if (seg.kind == GameBalanceConfig.WheelPrizeKind.TREATS && petsOn) 0.0 else seg.coinsU
        val coins = GameBalanceConfig.coins(state.level, coinsU)
        val spins = (seg.spins * ctx.mods.spins).roundToInt()
        var s = earn(state, coins).let { it.copy(spins = it.spins + spins, petTreats = it.petTreats + treats, lastWheelDay = ctx.today) }
        val effects = mutableListOf<GameEffect>(GameEffect.WheelSpun(idx, coins, spins, treats))
        if (seg.chest != null) { val r = grantChest(s, seg.chest, ctx); s = r.state; effects += r.effects }
        return ActionResult(s, effects)
    }

    // ------------------------------------------------------------------ missions
    fun refreshMissions(state: GameState, ctx: Ctx): GameState {
        var missions = state.missions
        var s = state
        if (state.dailyMissionDay != ctx.today) {
            missions = missions.filter { it.period != MissionPeriod.DAILY } + GameBalanceConfig.DAILY_MISSIONS.mapIndexed { i, t ->
                MissionState(
                    id = "d${ctx.today}_$i", type = t.type, period = MissionPeriod.DAILY,
                    target = if (t.coinsTarget) GameBalanceConfig.coins(state.level, t.target.toDouble()) else t.target,
                    rewardCoins = GameBalanceConfig.coins(state.level, t.rewardCoinsU), rewardSpins = t.rewardSpins, rewardChest = t.rewardChest
                )
            }
            s = s.copy(dailyMissionDay = ctx.today)
        }
        if (state.weeklyMissionWeek != ctx.week) {
            missions = missions.filter { it.period != MissionPeriod.WEEKLY } + GameBalanceConfig.WEEKLY_MISSIONS.mapIndexed { i, t ->
                MissionState(
                    id = "w${ctx.week}_$i", type = t.type, period = MissionPeriod.WEEKLY,
                    target = if (t.coinsTarget) GameBalanceConfig.coins(state.level, t.target.toDouble()) else t.target,
                    rewardCoins = GameBalanceConfig.coins(state.level, t.rewardCoinsU), rewardSpins = t.rewardSpins, rewardChest = t.rewardChest
                )
            }
            s = s.copy(weeklyMissionWeek = ctx.week)
        }
        return if (missions === state.missions) s else s.copy(missions = missions)
    }

    fun track(state: GameState, type: MissionType, amount: Long): GameState {
        if (state.missions.none { it.type == type && !it.claimed }) return state
        return state.copy(missions = state.missions.map {
            if (it.type == type && !it.claimed && it.progress < it.target) it.copy(progress = minOf(it.target, it.progress + amount)) else it
        })
    }

    fun claimMission(state: GameState, id: String, ctx: Ctx): ActionResult {
        val m = state.missions.firstOrNull { it.id == id } ?: return ActionResult(state, error = GameError.NOT_AVAILABLE)
        if (m.claimed) return ActionResult(state, error = GameError.ALREADY_CLAIMED)
        if (!m.completed) return ActionResult(state, error = GameError.NOT_AVAILABLE)
        var s = state.copy(missions = state.missions.map { if (it.id == id) it.copy(claimed = true) else it })
        s = earn(s, m.rewardCoins).let { it.copy(spins = it.spins + m.rewardSpins) }
        val effects = mutableListOf<GameEffect>(GameEffect.MissionClaimed(id, m.rewardCoins, m.rewardSpins))
        if (m.rewardChest != null) { val r = grantChest(s, m.rewardChest, ctx); s = r.state; effects += r.effects }
        return ActionResult(s, effects)
    }

    // ------------------------------------------------------------------ cards & pets
    fun claimSet(state: GameState, set: Int, ctx: Ctx): ActionResult {
        if (set in state.claimedSets) return ActionResult(state, error = GameError.ALREADY_CLAIMED)
        if (!Cards.isSetComplete(state.cards, set)) return ActionResult(state, error = GameError.NOT_AVAILABLE)
        val coins = GameBalanceConfig.coins(state.level, GameBalanceConfig.setRewardCoinsU(set))
        val spins = GameBalanceConfig.setRewardSpins(set)
        var s = earn(state, coins).let { it.copy(spins = it.spins + spins, claimedSets = it.claimedSets + set) }
        val effects = mutableListOf<GameEffect>(GameEffect.Rewarded(coins, spins))
        val xp = Pets.addXp(s, GameBalanceConfig.setRewardPetXp(set)); s = xp.state
        xp.levelUps.forEach { effects += GameEffect.PetLevelUp(it.first, it.second) }
        val r = grantChest(s, GameBalanceConfig.setRewardChest(set), ctx)
        return ActionResult(r.state, effects + r.effects)
    }

    fun feedPet(state: GameState, type: PetType): ActionResult {
        if (state.petTreats <= 0) return ActionResult(state, error = GameError.NO_TREATS)
        val pet = state.pets.firstOrNull { it.type == type } ?: return ActionResult(state, error = GameError.NOT_AVAILABLE)
        if (pet.level >= GameBalanceConfig.PET_MAX_LEVEL) return ActionResult(state, error = GameError.MAX_STAGE)
        val r = Pets.addXp(state.copy(petTreats = state.petTreats - 1), GameBalanceConfig.TREAT_XP, type)
        return ActionResult(r.state, r.levelUps.map { GameEffect.PetLevelUp(it.first, it.second) })
    }

    fun setActivePet(state: GameState, type: PetType): GameState =
        if (state.pets.any { it.type == type }) state.copy(activePet = type) else state

    // ------------------------------------------------------------------ bots attacking the player
    /**
     * Simulates bot attacks that happened while the player was away. Fair rules: at most 3 per return,
     * shields always block first, a hit only damages (never destroys) one building which can be repaired cheaply.
     */
    fun simulateBotAttacks(state: GameState, ctx: Ctx, attackers: List<String>): GameState {
        if (!state.introDone || state.level < GameBalanceConfig.BOT_ATTACK_MIN_LEVEL || attackers.isEmpty()) return state.copy(lastBotCheckMillis = maxOf(state.lastBotCheckMillis, ctx.now))
        if (ctx.now <= state.lastBotCheckMillis) return state
        val windows = ((ctx.now - state.lastBotCheckMillis) / GameBalanceConfig.BOT_ATTACK_INTERVAL_MS).toInt()
        if (windows <= 0) return state
        var s = state.copy(lastBotCheckMillis = state.lastBotCheckMillis + windows * GameBalanceConfig.BOT_ATTACK_INTERVAL_MS)
        var events = 0
        val log = s.log.toMutableList()
        repeat(minOf(windows, 16)) { w ->
            if (events >= GameBalanceConfig.BOT_ATTACK_MAX_PER_RETURN) return@repeat
            if (ctx.random.nextDouble() >= GameBalanceConfig.BOT_ATTACK_CHANCE) return@repeat
            val attacker = attackers[ctx.random.nextInt(attackers.size)]
            val time = state.lastBotCheckMillis + (w + 1) * GameBalanceConfig.BOT_ATTACK_INTERVAL_MS
            if (s.shields > 0) {
                s = s.copy(shields = s.shields - 1, stats = s.stats.copy(shieldsBlockedAttacks = s.stats.shieldsBlockedAttacks + 1))
                log += LogEntry(time, "bot_attack_blocked", attacker)
                events++
            } else {
                val targets = s.buildings.indices.filter { s.buildings[it].stage > 0 && !s.buildings[it].damaged }
                if (targets.isNotEmpty()) {
                    val t = targets[ctx.random.nextInt(targets.size)]
                    s = s.copy(buildings = s.buildings.toMutableList().also { it[t] = it[t].copy(damaged = true) })
                    log += LogEntry(time, "bot_attack_hit", attacker, t)
                    events++
                }
            }
        }
        return s.copy(log = log.takeLast(GameBalanceConfig.MAX_LOG_ENTRIES))
    }

    fun markLogSeen(state: GameState) = state.copy(log = state.log.map { it.copy(seen = true) })

    // ------------------------------------------------------------------ shop & ads
    fun grantProduct(state: GameState, product: ShopProduct, ctx: Ctx): ActionResult {
        val coins = GameBalanceConfig.coins(state.level, product.coinsU)
        var s = earn(state, coins).let { it.copy(spins = it.spins + product.spins, purchases = it.purchases + 1) }
        val effects = mutableListOf<GameEffect>(GameEffect.Purchased(product, coins, product.spins))
        if (product.chest != null) { val r = grantChest(s, product.chest, ctx); s = r.state; effects += r.effects }
        return ActionResult(s, effects)
    }

    fun grantAdSpins(state: GameState): ActionResult {
        val s = state.copy(spins = state.spins + GameBalanceConfig.AD_REWARD_SPINS, adsWatched = state.adsWatched + 1)
        return ActionResult(s, listOf(GameEffect.Rewarded(0, GameBalanceConfig.AD_REWARD_SPINS)))
    }

    fun doubleReward(state: GameState, coins: Long, spins: Int): ActionResult {
        val s = earn(state, coins).let { it.copy(spins = it.spins + spins, adsWatched = it.adsWatched + 1) }
        return ActionResult(s, listOf(GameEffect.Rewarded(coins, spins)))
    }

    // ------------------------------------------------------------------ debug helpers
    object Debug {
        fun addCoins(s: GameState, amount: Long) = s.copy(coins = s.coins + amount)
        fun addSpins(s: GameState, amount: Int) = s.copy(spins = s.spins + amount)
        fun addShields(s: GameState) = s.copy(shields = GameBalanceConfig.maxShields(s.level))
        fun setLevel(s: GameState, level: Int) = Pets.syncUnlocks(s.copy(level = level.coerceIn(1, LevelConfig.MAX_LEVEL), buildings = List(GameBalanceConfig.BUILDINGS_PER_LEVEL) { BuildingState() }))
        fun finishBuildingsExceptLast(s: GameState) = s.copy(buildings = List(GameBalanceConfig.BUILDINGS_PER_LEVEL) { i -> BuildingState(if (i == GameBalanceConfig.BUILDINGS_PER_LEVEL - 1) MAX_STAGE - 1 else MAX_STAGE) }, coins = s.coins + GameBalanceConfig.upgradeCost(s.level, GameBalanceConfig.BUILDINGS_PER_LEVEL - 1, MAX_STAGE))
        fun unlockAllCards(s: GameState) = s.copy(cards = (0 until Cards.TOTAL).associateWith { maxOf(1, s.cards[it] ?: 0) })
        fun resetDaily(s: GameState) = s.copy(dailyBonus = s.dailyBonus.copy(lastClaimDay = if (s.dailyBonus.lastClaimDay >= 0) s.dailyBonus.lastClaimDay - 1 else -1), lastWheelDay = -1)
        fun addTreats(s: GameState) = s.copy(petTreats = s.petTreats + 20)
        fun damageBuilding(s: GameState) = s.buildings.indexOfFirst { it.stage > 0 && !it.damaged }.let { i -> if (i < 0) s else s.copy(buildings = s.buildings.toMutableList().also { it[i] = it[i].copy(damaged = true) }) }
        fun pendingAttack(s: GameState) = s.copy(pendingActions = s.pendingActions + PendingAction("attack", 1))
        fun pendingRaid(s: GameState) = s.copy(pendingActions = s.pendingActions + PendingAction("raid", 1))
    }
}
