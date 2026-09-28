package de.danielgrebe.spinkingdom.models

import kotlinx.serialization.Serializable

/** Slot machine symbols. */
enum class SlotSymbol { COIN, ATTACK, RAID, SHIELD, ENERGY, CHEST, JOKER }

enum class Rarity { COMMON, RARE, EPIC, LEGENDARY }

enum class ChestType { WOOD, SILVER, GOLD, ROYAL, LEGENDARY }

enum class PetType { FOX, DRAGON, RACCOON, PHOENIX }

enum class MissionType { SPINS, ATTACKS, RAIDS, UPGRADES, COLLECT_COINS, OPEN_CHESTS, WIN_SHIELDS }

enum class MissionPeriod { DAILY, WEEKLY }

@Serializable
data class BuildingState(
    val stage: Int = 0,          // 0 = construction site, 5 = fully built
    val damaged: Boolean = false // damaged by a bot attack, needs a repair
)

@Serializable
data class PetState(
    val type: PetType,
    val level: Int = 1,
    val xp: Int = 0
)

@Serializable
data class MissionState(
    val id: String,
    val type: MissionType,
    val period: MissionPeriod,
    val target: Long,
    val progress: Long = 0,
    val rewardCoins: Long = 0,
    val rewardSpins: Int = 0,
    val rewardChest: ChestType? = null,
    val claimed: Boolean = false
) {
    val completed: Boolean get() = progress >= target
}

@Serializable
data class DailyBonusState(
    val streakDay: Int = 0,          // days already claimed in the current 7-day cycle (0..6)
    val lastClaimDay: Long = -1,     // epoch day of last claim
    val totalClaims: Int = 0
)

@Serializable
data class ClockState(
    val maxTrustedMillis: Long = 0,     // highest trusted time ever observed
    val lastWallMillis: Long = 0,
    val lastElapsedRealtime: Long = 0,
    val driftMillis: Long = 0,          // unexplained forward clock jumps (ignored for rewards)
    val tamperCount: Int = 0
)

@Serializable
data class Stats(
    val totalSpins: Long = 0,
    val attacksWon: Int = 0,
    val attacksBlocked: Int = 0,
    val raidsDone: Int = 0,
    val coinsEarned: Long = 0,
    val buildingsUpgraded: Int = 0,
    val levelsCompleted: Int = 0,
    val chestsOpened: Int = 0,
    val jackpots: Int = 0,
    val shieldsBlockedAttacks: Int = 0,
    val biggestWin: Long = 0
)

@Serializable
data class Settings(
    val music: Boolean = true,
    val sfx: Boolean = true,
    val vibration: Boolean = true,
    val notifications: Boolean = false,
    val language: String = "de"
)

@Serializable
data class LogEntry(
    val timeMillis: Long,
    val kind: String,            // "bot_attack_blocked", "bot_attack_hit"
    val attacker: String,
    val buildingIndex: Int = -1,
    val seen: Boolean = false
)

@Serializable
data class PendingAction(
    val kind: String,            // "attack" or "raid"
    val multiplier: Int
)

@Serializable
data class GameState(
    val version: Int = 1,
    val playerId: String = "",
    val playerName: String = "",
    val isGuest: Boolean = true,
    val avatar: Int = 0,
    val introDone: Boolean = false,
    val createdAt: Long = 0,

    val level: Int = 1,
    val coins: Long = 0,
    val spins: Int = 0,
    val lastRegenMillis: Long = 0,
    val stars: Int = 0,
    val shields: Int = 0,
    val buildings: List<BuildingState> = List(5) { BuildingState() },
    val selectedMultiplier: Int = 1,
    val luckyBoostUntil: Long = 0,                   // end of the current/last "Glücks-Einsatz"

    val pendingActions: List<PendingAction> = emptyList(),

    val cards: Map<Int, Int> = emptyMap(),          // cardId -> count
    val claimedSets: Set<Int> = emptySet(),

    val pets: List<PetState> = emptyList(),         // unlocked pets
    val activePet: PetType? = null,
    val petTreats: Int = 0,

    val missions: List<MissionState> = emptyList(),
    val dailyMissionDay: Long = -1,
    val weeklyMissionWeek: Long = -1,

    val dailyBonus: DailyBonusState = DailyBonusState(),
    val lastWheelDay: Long = -1,
    val clock: ClockState = ClockState(),

    val lastBotCheckMillis: Long = 0,
    val log: List<LogEntry> = emptyList(),

    val stats: Stats = Stats(),
    val settings: Settings = Settings(),
    val debugForcedEvent: String? = null,
    val purchases: Int = 0,
    val adsWatched: Int = 0
) {
    val cardsCollected: Int get() = cards.count { it.value > 0 }
}
