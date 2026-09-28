package de.danielgrebe.spinkingdom.config

import de.danielgrebe.spinkingdom.models.ChestType
import de.danielgrebe.spinkingdom.models.MissionType
import de.danielgrebe.spinkingdom.models.PetType
import de.danielgrebe.spinkingdom.models.Rarity
import de.danielgrebe.spinkingdom.models.SlotSymbol
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Central place for every balancing number in the game.
 * All payouts are expressed in "coin units" (U) which scale with the village level,
 * so the same config stays balanced from level 1 up to level 500.
 */
object GameBalanceConfig {
    // ---------- Start values ----------
    const val START_COINS = 3_000L
    const val START_SPINS = 50

    // ---------- Spin regeneration ----------
    const val SPIN_REGEN_INTERVAL_MS = 10 * 60 * 1000L
    const val SPIN_REGEN_AMOUNT = 5
    const val MAX_AUTO_SPINS = 50

    // ---------- Multipliers ----------
    val BASE_MULTIPLIERS = listOf(1, 2, 3, 5, 10)
    /** level -> multiplier unlocked at that level */
    val EXTRA_MULTIPLIERS = listOf(5 to 20, 15 to 30, 10 to 50, 20 to 100)

    fun availableMultipliers(level: Int, luckyBoost: Boolean = false): List<Int> =
        (BASE_MULTIPLIERS + EXTRA_MULTIPLIERS.filter { level >= it.first }.map { it.second } +
            (if (luckyBoost) LUCKY_BOOST_MULTIPLIERS else emptyList())).distinct().sorted()

    // ---------- "Glücks-Einsatz": time limited boost that unlocks x20/x30 early ----------
    val LUCKY_BOOST_MULTIPLIERS = listOf(20, 30)
    /** Chance per spin that the boost appears (only while x20/x30 are not all unlocked by level). */
    const val LUCKY_BOOST_CHANCE_PER_SPIN = 0.012
    const val LUCKY_BOOST_DURATION_MS = 10 * 60 * 1000L
    /** Minimum time between the end of one boost and the next one (roughly "once every few hours"). */
    const val LUCKY_BOOST_COOLDOWN_MS = 3 * 60 * 60 * 1000L
    const val LUCKY_BOOST_MIN_LEVEL = 2

    // ---------- Spin jackpot: rare big spin rewards ----------
    /** On 3x Energie: (spins, chance) evaluated in order; absolute chances, not scaled by level. */
    val SPIN_JACKPOT_CHANCES = listOf(30 to 0.05, 20 to 0.12)
    /** Whether the spin multiplier also multiplies the spin jackpot. */
    const val SPIN_JACKPOT_APPLY_MULTIPLIER = true
    /** Upper bound of a single slot spin jackpot after multiplier/events (keeps x100 sane). */
    const val SPIN_JACKPOT_MAX_SPINS = 300
    /** Daily wheel: chance that a spins segment is upgraded to a spin jackpot. */
    val WHEEL_SPIN_JACKPOT_CHANCES = listOf(30 to 0.04, 20 to 0.08)
    /** Royal/legendary chests: rare extra spin jackpot. */
    val CHEST_SPIN_JACKPOT_CHANCES: Map<ChestType, List<Pair<Int, Double>>> = mapOf(
        ChestType.ROYAL to listOf(30 to 0.02, 20 to 0.06),
        ChestType.LEGENDARY to listOf(30 to 0.06, 20 to 0.10)
    )

    // ---------- Slot symbol weights (per reel) ----------
    val SYMBOL_WEIGHTS: Map<SlotSymbol, Int> = linkedMapOf(
        SlotSymbol.COIN to 30,
        SlotSymbol.ATTACK to 12,
        SlotSymbol.RAID to 10,
        SlotSymbol.SHIELD to 11,
        SlotSymbol.ENERGY to 9,
        SlotSymbol.CHEST to 6,
        SlotSymbol.JOKER to 4
    )

    // ---------- Payouts (in coin units U) ----------
    const val COIN_TRIPLE_U = 25.0
    const val COIN_PAIR_U = 5.0
    const val OTHER_PAIR_U = 2.0
    const val SINGLE_COIN_U = 1.0
    const val JACKPOT_U = 250.0
    const val ENERGY_TRIPLE_SPINS = 10
    const val ENERGY_PAIR_SPINS = 1
    const val SHIELD_FULL_CONSOLATION_U = 12.0

    // ---------- Attacks ----------
    const val ATTACK_REWARD_U = 60.0
    const val ATTACK_BLOCKED_U = 15.0
    const val OPPONENT_SHIELD_CHANCE = 0.30

    // ---------- Raids ----------
    const val RAID_SPOTS = 9
    const val RAID_PICKS = 3
    const val RAID_SMALL_U = 15.0
    const val RAID_LARGE_U = 45.0
    const val RAID_JACKPOT_U = 150.0
    const val RAID_BONUS_SPINS = 5
    /** weights: small, large, jackpot, bonus spins */
    val RAID_WEIGHTS = listOf(50, 30, 5, 15)

    // ---------- Shields ----------
    fun maxShields(level: Int): Int = 3 + (if (level >= 15) 1 else 0) + (if (level >= 30) 1 else 0)

    // ---------- Level / building economy ----------
    const val BUILDINGS_PER_LEVEL = 5
    const val MAX_STAGE = 5
    const val BASE_UPGRADE_COST = 1_200.0
    val STAGE_FACTORS = listOf(1.0, 1.5, 2.2, 3.1, 4.3)
    const val BUILDING_INDEX_FACTOR = 0.12
    val EARLY_LEVEL_DISCOUNT = listOf(0.35, 0.55, 0.75)
    const val REPAIR_COST_FRACTION = 0.25
    const val STARS_PER_UPGRADE = 1
    const val STARS_PER_LEVEL_COMPLETE = 10

    /** Geometric growth for the first 45 levels, then linear growth so level 500 stays within Long range. */
    fun levelScale(level: Int): Double {
        val l = level.coerceAtLeast(1)
        return 1.22.pow(min(l, 45) - 1) * (1.0 + 0.08 * (l - 45).coerceAtLeast(0))
    }

    /** One coin unit U for a level. */
    fun coinUnit(level: Int): Double = 100.0 * levelScale(level)

    fun coins(level: Int, units: Double): Long = (coinUnit(level) * units).roundToLong()

    fun upgradeCost(level: Int, buildingIndex: Int, nextStage: Int): Long {
        require(nextStage in 1..MAX_STAGE)
        val discount = EARLY_LEVEL_DISCOUNT.getOrElse(level - 1) { 1.0 }
        val difficulty = 1.0 + 0.02 * (level - 1)
        val raw = BASE_UPGRADE_COST * levelScale(level) * difficulty * discount *
            STAGE_FACTORS[nextStage - 1] * (1.0 + BUILDING_INDEX_FACTOR * buildingIndex)
        return (raw / 10.0).roundToLong() * 10L
    }

    fun repairCost(level: Int, buildingIndex: Int, stage: Int): Long =
        if (stage <= 0) 0 else ((upgradeCost(level, buildingIndex, stage) * REPAIR_COST_FRACTION) / 10.0).roundToLong() * 10L

    fun levelTotalCost(level: Int): Long =
        (0 until BUILDINGS_PER_LEVEL).sumOf { b -> (1..MAX_STAGE).sumOf { s -> upgradeCost(level, b, s) } }

    // ---------- Level completion rewards ----------
    const val LEVEL_COMPLETE_U = 30.0
    fun levelCompleteSpins(level: Int): Int = min(15 + level, 60)
    fun levelCompleteChest(level: Int): ChestType = when {
        level % 10 == 0 -> ChestType.LEGENDARY
        level % 5 == 0 -> ChestType.ROYAL
        else -> ChestType.GOLD
    }

    // ---------- Chests ----------
    data class ChestSpec(
        val cards: Int,
        val minRarity: Rarity,
        val coinsMinU: Double,
        val coinsMaxU: Double,
        val spinChance: Double,
        val spinsMin: Int,
        val spinsMax: Int,
        val treatsMin: Int,
        val treatsMax: Int,
        val petXp: Int
    )

    val CHESTS: Map<ChestType, ChestSpec> = mapOf(
        ChestType.WOOD to ChestSpec(2, Rarity.COMMON, 3.0, 6.0, 0.20, 2, 5, 0, 1, 0),
        ChestType.SILVER to ChestSpec(3, Rarity.COMMON, 8.0, 15.0, 0.40, 5, 10, 1, 2, 0),
        ChestType.GOLD to ChestSpec(5, Rarity.RARE, 20.0, 40.0, 0.60, 10, 20, 2, 3, 50),
        ChestType.ROYAL to ChestSpec(7, Rarity.EPIC, 50.0, 90.0, 0.80, 20, 35, 3, 5, 100),
        ChestType.LEGENDARY to ChestSpec(10, Rarity.LEGENDARY, 120.0, 200.0, 1.0, 40, 60, 5, 8, 250)
    )
    const val CARD_TO_COINS_U = 2.0

    // ---------- Cards ----------
    const val CARDS_UNLOCK_LEVEL = 3
    const val CARD_SETS = 6
    const val CARDS_PER_SET = 9
    val RARITY_WEIGHTS: Map<Rarity, Int> = linkedMapOf(
        Rarity.COMMON to 62, Rarity.RARE to 26, Rarity.EPIC to 10, Rarity.LEGENDARY to 2
    )
    fun cardRarity(indexInSet: Int): Rarity = when (indexInSet) {
        in 0..3 -> Rarity.COMMON
        in 4..5 -> Rarity.RARE
        in 6..7 -> Rarity.EPIC
        else -> Rarity.LEGENDARY
    }
    fun cardSetUnlockLevel(set: Int): Int = CARDS_UNLOCK_LEVEL + 3 * set
    fun setRewardSpins(set: Int): Int = 10 + 5 * set
    fun setRewardCoinsU(set: Int): Double = 40.0 * (set + 1)
    fun setRewardPetXp(set: Int): Int = 100 * (set + 1)
    fun setRewardChest(set: Int): ChestType = when {
        set < 2 -> ChestType.SILVER
        set < 4 -> ChestType.GOLD
        else -> ChestType.ROYAL
    }

    // ---------- Pets ----------
    val PET_UNLOCK_LEVEL: Map<PetType, Int> = linkedMapOf(
        PetType.FOX to 5, PetType.RACCOON to 7, PetType.DRAGON to 9, PetType.PHOENIX to 12
    )
    const val PET_MAX_LEVEL = 10
    const val TREAT_XP = 40
    fun petXpForNext(level: Int): Int = 100 * level

    /** Returns the effect strength of a pet at a level: a fraction (0.15 = +15 %) or a chance. */
    fun petEffect(type: PetType, level: Int): Double = when (type) {
        PetType.FOX -> 0.10 + 0.05 * (level - 1)
        PetType.DRAGON -> 0.10 + 0.05 * (level - 1)
        PetType.RACCOON -> 0.05 + 0.03 * (level - 1)
        PetType.PHOENIX -> 0.03 + 0.01 * (level - 1)
    }

    // ---------- Daily bonus (7 day cycle) ----------
    data class DailyReward(val coinsU: Double = 0.0, val spins: Int = 0, val chest: ChestType? = null)
    val DAILY_BONUS = listOf(
        DailyReward(coinsU = 10.0),
        DailyReward(spins = 15),
        DailyReward(chest = ChestType.SILVER),
        DailyReward(coinsU = 25.0),
        DailyReward(spins = 30),
        DailyReward(chest = ChestType.GOLD),
        DailyReward(coinsU = 60.0, spins = 50, chest = ChestType.ROYAL)
    )

    // ---------- Daily wheel ----------
    enum class WheelPrizeKind { COINS, SPINS, CHEST, TREATS, JACKPOT }
    data class WheelSegment(val kind: WheelPrizeKind, val coinsU: Double = 0.0, val spins: Int = 0, val chest: ChestType? = null, val treats: Int = 0, val weight: Int)
    val WHEEL = listOf(
        WheelSegment(WheelPrizeKind.COINS, coinsU = 8.0, weight = 25),
        WheelSegment(WheelPrizeKind.SPINS, spins = 10, weight = 20),
        WheelSegment(WheelPrizeKind.CHEST, chest = ChestType.WOOD, weight = 15),
        WheelSegment(WheelPrizeKind.COINS, coinsU = 20.0, weight = 12),
        WheelSegment(WheelPrizeKind.SPINS, spins = 25, weight = 10),
        WheelSegment(WheelPrizeKind.TREATS, treats = 3, coinsU = 12.0, weight = 8),
        WheelSegment(WheelPrizeKind.CHEST, chest = ChestType.SILVER, weight = 7),
        WheelSegment(WheelPrizeKind.JACKPOT, coinsU = 100.0, spins = 50, weight = 3)
    )

    // ---------- Missions ----------
    data class MissionTemplate(val type: MissionType, val target: Long, val coinsTarget: Boolean = false, val rewardCoinsU: Double = 0.0, val rewardSpins: Int = 0, val rewardChest: ChestType? = null)
    val DAILY_MISSIONS = listOf(
        MissionTemplate(MissionType.SPINS, 20, rewardCoinsU = 10.0),
        MissionTemplate(MissionType.ATTACKS, 3, rewardSpins = 10),
        MissionTemplate(MissionType.RAIDS, 1, rewardCoinsU = 8.0),
        MissionTemplate(MissionType.UPGRADES, 2, rewardSpins = 8),
        MissionTemplate(MissionType.COLLECT_COINS, 50, coinsTarget = true, rewardChest = ChestType.WOOD)
    )
    val WEEKLY_MISSIONS = listOf(
        MissionTemplate(MissionType.SPINS, 300, rewardChest = ChestType.GOLD),
        MissionTemplate(MissionType.ATTACKS, 20, rewardSpins = 50),
        MissionTemplate(MissionType.RAIDS, 10, rewardCoinsU = 80.0),
        MissionTemplate(MissionType.UPGRADES, 15, rewardChest = ChestType.ROYAL),
        MissionTemplate(MissionType.OPEN_CHESTS, 5, rewardSpins = 30)
    )

    // ---------- Bot attacks while offline ----------
    const val BOT_ATTACK_INTERVAL_MS = 90 * 60 * 1000L
    const val BOT_ATTACK_CHANCE = 0.35
    const val BOT_ATTACK_MAX_PER_RETURN = 3
    const val BOT_ATTACK_MIN_LEVEL = 2

    // ---------- Clock protection ----------
    const val CLOCK_BACKWARDS_TOLERANCE_MS = 5 * 60 * 1000L
    const val CLOCK_FORWARD_JUMP_TOLERANCE_MS = 60 * 60 * 1000L

    // ---------- Ads / shop ----------
    const val AD_REWARD_SPINS = 10
    const val AD_DURATION_SECONDS = 5
    const val MAX_LOG_ENTRIES = 30
}
