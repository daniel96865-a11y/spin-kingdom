package de.dgstudios.spinkingdom.domain

import de.dgstudios.spinkingdom.config.GameBalanceConfig
import de.dgstudios.spinkingdom.models.SlotSymbol
import kotlin.random.Random

enum class OutcomeType {
    JACKPOT,        // 3x Joker
    COIN_TRIPLE, ATTACK, RAID, SHIELD, ENERGY, CHEST, // triples (Joker substitutes)
    COIN_PAIR, ENERGY_PAIR, OTHER_PAIR,
    SINGLE_COIN,
    NOTHING
}

data class SlotOutcome(val type: OutcomeType, val symbol: SlotSymbol?)

object SlotEngine {
    private val symbols = GameBalanceConfig.SYMBOL_WEIGHTS.keys.toList()
    private val totalWeight = GameBalanceConfig.SYMBOL_WEIGHTS.values.sum()

    fun rollSymbol(random: Random): SlotSymbol {
        var r = random.nextInt(totalWeight)
        for (s in symbols) {
            val w = GameBalanceConfig.SYMBOL_WEIGHTS.getValue(s)
            if (r < w) return s
            r -= w
        }
        return SlotSymbol.COIN
    }

    fun roll(random: Random): List<SlotSymbol> = List(3) { rollSymbol(random) }

    /** Priority used when a Joker can complete different triples/pairs: the most valuable wins. */
    private val priority = listOf(
        SlotSymbol.CHEST, SlotSymbol.RAID, SlotSymbol.ATTACK, SlotSymbol.COIN, SlotSymbol.ENERGY, SlotSymbol.SHIELD
    )

    private fun tripleType(s: SlotSymbol) = when (s) {
        SlotSymbol.COIN -> OutcomeType.COIN_TRIPLE
        SlotSymbol.ATTACK -> OutcomeType.ATTACK
        SlotSymbol.RAID -> OutcomeType.RAID
        SlotSymbol.SHIELD -> OutcomeType.SHIELD
        SlotSymbol.ENERGY -> OutcomeType.ENERGY
        SlotSymbol.CHEST -> OutcomeType.CHEST
        SlotSymbol.JOKER -> OutcomeType.JACKPOT
    }

    fun evaluate(reels: List<SlotSymbol>): SlotOutcome {
        require(reels.size == 3)
        val jokers = reels.count { it == SlotSymbol.JOKER }
        if (jokers == 3) return SlotOutcome(OutcomeType.JACKPOT, SlotSymbol.JOKER)
        val counts = reels.filter { it != SlotSymbol.JOKER }.groupingBy { it }.eachCount()
        // Triples (Joker is wild)
        for (s in priority) {
            if ((counts[s] ?: 0) + jokers >= 3) return SlotOutcome(tripleType(s), s)
        }
        // Pairs
        val pairSymbols = priority.filter { (counts[it] ?: 0) + jokers >= 2 && (counts[it] ?: 0) >= 1 }
        if (pairSymbols.isNotEmpty()) {
            return when {
                SlotSymbol.COIN in pairSymbols -> SlotOutcome(OutcomeType.COIN_PAIR, SlotSymbol.COIN)
                SlotSymbol.ENERGY in pairSymbols -> SlotOutcome(OutcomeType.ENERGY_PAIR, SlotSymbol.ENERGY)
                else -> SlotOutcome(OutcomeType.OTHER_PAIR, pairSymbols.first())
            }
        }
        if (SlotSymbol.COIN in counts) return SlotOutcome(OutcomeType.SINGLE_COIN, SlotSymbol.COIN)
        return SlotOutcome(OutcomeType.NOTHING, null)
    }

    /** Exact probability of each outcome type (used by tests and the balance sheet). */
    fun outcomeProbabilities(): Map<OutcomeType, Double> {
        val w = GameBalanceConfig.SYMBOL_WEIGHTS
        val total = totalWeight.toDouble()
        val result = mutableMapOf<OutcomeType, Double>()
        for (a in symbols) for (b in symbols) for (c in symbols) {
            val p = w.getValue(a) / total * w.getValue(b) / total * w.getValue(c) / total
            val o = evaluate(listOf(a, b, c)).type
            result[o] = (result[o] ?: 0.0) + p
        }
        return result
    }
}
