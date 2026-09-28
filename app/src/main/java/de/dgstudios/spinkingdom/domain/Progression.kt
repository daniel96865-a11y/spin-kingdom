package de.dgstudios.spinkingdom.domain

import de.dgstudios.spinkingdom.config.GameBalanceConfig
import de.dgstudios.spinkingdom.config.GameBalanceConfig.MAX_STAGE
import de.dgstudios.spinkingdom.config.LevelConfig
import de.dgstudios.spinkingdom.models.GameState
import de.dgstudios.spinkingdom.models.PetState
import de.dgstudios.spinkingdom.models.PetType
import de.dgstudios.spinkingdom.models.Rarity
import kotlin.random.Random

/** Card catalogue: 6 sets x 9 cards, id = set * 9 + index. */
object Cards {
    val TOTAL = GameBalanceConfig.CARD_SETS * GameBalanceConfig.CARDS_PER_SET
    fun setOf(cardId: Int) = cardId / GameBalanceConfig.CARDS_PER_SET
    fun indexOf(cardId: Int) = cardId % GameBalanceConfig.CARDS_PER_SET
    fun rarity(cardId: Int): Rarity = GameBalanceConfig.cardRarity(indexOf(cardId))
    fun cardsOfSet(set: Int) = (0 until GameBalanceConfig.CARDS_PER_SET).map { set * GameBalanceConfig.CARDS_PER_SET + it }
    fun unlockedSets(level: Int) = (0 until GameBalanceConfig.CARD_SETS).filter { level >= GameBalanceConfig.cardSetUnlockLevel(it) }
    fun cardsUnlocked(level: Int) = level >= GameBalanceConfig.CARDS_UNLOCK_LEVEL

    fun rollRarity(random: Random, min: Rarity = Rarity.COMMON): Rarity {
        val pool = GameBalanceConfig.RARITY_WEIGHTS.filterKeys { it.ordinal >= min.ordinal }
        var r = random.nextInt(pool.values.sum())
        for ((k, w) in pool) { if (r < w) return k; r -= w }
        return min
    }

    fun randomCard(random: Random, level: Int, min: Rarity = Rarity.COMMON): Int {
        val sets = unlockedSets(level).ifEmpty { listOf(0) }
        val rarity = rollRarity(random, min)
        val candidates = sets.flatMap { cardsOfSet(it) }.filter { rarity(it) == rarity }
        return candidates[random.nextInt(candidates.size)]
    }

    fun isSetComplete(cards: Map<Int, Int>, set: Int) = cardsOfSet(set).all { (cards[it] ?: 0) > 0 }
}

object Pets {
    fun unlockedTypes(level: Int): List<PetType> = GameBalanceConfig.PET_UNLOCK_LEVEL.filterValues { level >= it }.keys.toList()
    fun petsUnlocked(level: Int) = unlockedTypes(level).isNotEmpty()

    /** Adds newly unlocked pets for the current level; the first pet becomes active automatically. */
    fun syncUnlocks(state: GameState): GameState {
        val have = state.pets.map { it.type }.toSet()
        val add = unlockedTypes(state.level).filter { it !in have }.map { PetState(it) }
        if (add.isEmpty()) return state
        val pets = state.pets + add
        return state.copy(pets = pets, activePet = state.activePet ?: pets.first().type)
    }

    data class XpResult(val state: GameState, val levelUps: List<Pair<PetType, Int>>)

    fun addXp(state: GameState, xp: Int, target: PetType? = null): XpResult {
        val type = target ?: state.activePet ?: state.pets.firstOrNull()?.type ?: return XpResult(state, emptyList())
        val ups = mutableListOf<Pair<PetType, Int>>()
        val pets = state.pets.map { p ->
            if (p.type != type) p else {
                var lvl = p.level; var cur = p.xp + xp
                while (lvl < GameBalanceConfig.PET_MAX_LEVEL && cur >= GameBalanceConfig.petXpForNext(lvl)) {
                    cur -= GameBalanceConfig.petXpForNext(lvl); lvl++; ups += type to lvl
                }
                if (lvl >= GameBalanceConfig.PET_MAX_LEVEL) cur = 0
                p.copy(level = lvl, xp = cur)
            }
        }
        return XpResult(state.copy(pets = pets), ups)
    }

    fun activeEffect(state: GameState, type: PetType): Double {
        if (state.activePet != type) return 0.0
        val pet = state.pets.firstOrNull { it.type == type } ?: return 0.0
        return GameBalanceConfig.petEffect(type, pet.level)
    }
}

object Buildings {
    fun isLevelComplete(state: GameState) =
        state.buildings.size == GameBalanceConfig.BUILDINGS_PER_LEVEL && state.buildings.all { it.stage >= MAX_STAGE && !it.damaged }

    fun nextCost(state: GameState, index: Int): Long? {
        val b = state.buildings.getOrNull(index) ?: return null
        if (b.damaged) return GameBalanceConfig.repairCost(state.level, index, b.stage)
        if (b.stage >= MAX_STAGE) return null
        return LevelConfig.level(state.level).upgradeCosts[index][b.stage]
    }

    fun progress(state: GameState): Float =
        state.buildings.sumOf { it.stage }.toFloat() / (GameBalanceConfig.BUILDINGS_PER_LEVEL * MAX_STAGE)
}
