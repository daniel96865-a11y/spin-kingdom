package de.dgstudios.spinkingdom.domain

import de.dgstudios.spinkingdom.config.GameBalanceConfig
import de.dgstudios.spinkingdom.models.ChestType
import de.dgstudios.spinkingdom.models.Rarity
import kotlin.random.Random

data class ChestContents(
    val type: ChestType,
    val coins: Long = 0,
    val spins: Int = 0,
    val treats: Int = 0,
    val petXp: Int = 0,
    val cards: List<Int> = emptyList(),
    val bonusSpins: Int = 0          // rare spin jackpot (royal/legendary)
)

object ChestEngine {
    fun upgraded(type: ChestType, tiers: Int): ChestType =
        ChestType.entries[(type.ordinal + tiers).coerceIn(0, ChestType.entries.size - 1)]

    fun roll(type: ChestType, level: Int, random: Random): ChestContents {
        val spec = GameBalanceConfig.CHESTS.getValue(type)
        val cardsOn = Cards.cardsUnlocked(level)
        val petsOn = Pets.petsUnlocked(level)
        var coinsU = spec.coinsMinU + random.nextDouble() * (spec.coinsMaxU - spec.coinsMinU)
        val cards = mutableListOf<Int>()
        if (cardsOn) {
            // one guaranteed card of the minimum rarity, the rest random
            cards += Cards.randomCard(random, level, spec.minRarity)
            repeat(spec.cards - 1) { cards += Cards.randomCard(random, level, Rarity.COMMON) }
        } else {
            coinsU += spec.cards * GameBalanceConfig.CARD_TO_COINS_U
        }
        val spins = if (random.nextDouble() < spec.spinChance) spec.spinsMin + random.nextInt(spec.spinsMax - spec.spinsMin + 1) else 0
        var treats = 0; var petXp = 0
        if (petsOn) {
            treats = spec.treatsMin + random.nextInt(spec.treatsMax - spec.treatsMin + 1)
            petXp = spec.petXp
        } else {
            coinsU += (spec.treatsMax + spec.petXp / 50) * 2.0
        }
        val bonus = GameBalanceConfig.CHEST_SPIN_JACKPOT_CHANCES[type]?.let { SpinJackpot.roll(it, random) } ?: 0
        return ChestContents(type, GameBalanceConfig.coins(level, coinsU), spins, treats, petXp, cards, bonus)
    }
}
