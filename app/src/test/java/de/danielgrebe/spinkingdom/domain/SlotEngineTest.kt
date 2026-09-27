package de.danielgrebe.spinkingdom.domain

import de.danielgrebe.spinkingdom.models.SlotSymbol.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class SlotEngineTest {
    private fun eval(a: de.danielgrebe.spinkingdom.models.SlotSymbol, b: de.danielgrebe.spinkingdom.models.SlotSymbol, c: de.danielgrebe.spinkingdom.models.SlotSymbol) =
        SlotEngine.evaluate(listOf(a, b, c)).type

    @Test fun triples() {
        assertEquals(OutcomeType.COIN_TRIPLE, eval(COIN, COIN, COIN))
        assertEquals(OutcomeType.ATTACK, eval(ATTACK, ATTACK, ATTACK))
        assertEquals(OutcomeType.RAID, eval(RAID, RAID, RAID))
        assertEquals(OutcomeType.SHIELD, eval(SHIELD, SHIELD, SHIELD))
        assertEquals(OutcomeType.ENERGY, eval(ENERGY, ENERGY, ENERGY))
        assertEquals(OutcomeType.CHEST, eval(CHEST, CHEST, CHEST))
        assertEquals(OutcomeType.JACKPOT, eval(JOKER, JOKER, JOKER))
    }

    @Test fun jokerIsWild() {
        assertEquals(OutcomeType.ATTACK, eval(ATTACK, JOKER, ATTACK))
        assertEquals(OutcomeType.RAID, eval(JOKER, RAID, JOKER))
        assertEquals(OutcomeType.COIN_TRIPLE, eval(COIN, COIN, JOKER))
    }

    @Test fun jokerPicksMostValuableTriple() {
        // Joker + Joker + Coin -> coin triple; Joker completes the best pair into a triple
        assertEquals(OutcomeType.COIN_TRIPLE, eval(JOKER, JOKER, COIN))
        assertEquals(OutcomeType.CHEST, eval(JOKER, JOKER, CHEST))
    }

    @Test fun pairsAndSingles() {
        assertEquals(OutcomeType.COIN_PAIR, eval(COIN, COIN, SHIELD))
        assertEquals(OutcomeType.COIN_PAIR, eval(COIN, JOKER, SHIELD))
        assertEquals(OutcomeType.ENERGY_PAIR, eval(ENERGY, RAID, ENERGY))
        assertEquals(OutcomeType.OTHER_PAIR, eval(SHIELD, SHIELD, RAID))
        assertEquals(OutcomeType.SINGLE_COIN, eval(COIN, ATTACK, RAID))
        assertEquals(OutcomeType.NOTHING, eval(ATTACK, RAID, SHIELD))
    }

    @Test fun probabilitiesSumToOneAndCoinsAreMostCommonWin() {
        val p = SlotEngine.outcomeProbabilities()
        assertEquals(1.0, p.values.sum(), 1e-9)
        assertTrue("jackpot must be rare", (p[OutcomeType.JACKPOT] ?: 0.0) < 0.001)
        assertTrue(p.getValue(OutcomeType.COIN_TRIPLE) > p.getValue(OutcomeType.RAID))
        assertTrue(p.getValue(OutcomeType.COIN_TRIPLE) > p.getValue(OutcomeType.CHEST))
        // some reward (anything but NOTHING) on the vast majority of spins
        assertTrue((p[OutcomeType.NOTHING] ?: 0.0) < 0.25)
    }

    @Test fun rollingMatchesWeights() {
        val r = Random(42)
        val n = 200_000
        val coins = (0 until n).count { SlotEngine.rollSymbol(r) == COIN }
        assertEquals(30.0 / 82.0, coins.toDouble() / n, 0.01)
    }
}
