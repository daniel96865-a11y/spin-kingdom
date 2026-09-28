package de.dgstudios.spinkingdom.data

import de.dgstudios.spinkingdom.*
import de.dgstudios.spinkingdom.domain.AdResult
import de.dgstudios.spinkingdom.domain.PurchaseResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class RepositoriesTest {
    @Test fun leaderboardIncludesPlayerWithCorrectRanks() {
        val s = startedGame().copy(stars = 400, level = 10)
        val lb = LocalLeaderboardRepository.build(s, T0 + 5 * DAY, "Gast")
        assertEquals(31, lb.size)
        assertEquals((1..31).toList(), lb.map { it.rank })
        assertEquals(1, lb.count { it.isPlayer })
        for (i in 1 until lb.size) assertTrue(lb[i - 1].stars >= lb[i].stars)
        // deterministic
        assertEquals(lb, LocalLeaderboardRepository.build(s, T0 + 5 * DAY, "Gast"))
        assertEquals("Daniel", lb.first { it.isPlayer }.name)
    }

    @Test fun opponentsHaveVillagesAndShieldChance() = runBlocking {
        val repo = LocalOpponentRepository(Random(1))
        val list = (0 until 500).map { repo.findAttackTarget(5) }
        assertTrue(list.all { it.stages.size == 5 && it.stages.all { s -> s in 1..5 } })
        val shielded = list.count { it.hasShield } / 500.0
        assertEquals(0.30, shielded, 0.07)
        assertTrue((0 until 50).map { repo.findRaidTarget(5) }.none { it.hasShield })
        assertTrue(repo.botNames().containsAll(listOf("Alex", "Mia", "Leon", "Emma", "Noah", "Sophie")))
    }

    @Test fun fakeBillingIsTestOnly() = runBlocking {
        val b = FakeBillingRepository()
        assertEquals(10, b.products().size)
        val r = b.purchase("spins_small")
        assertTrue(r is PurchaseResult.Success)
        assertTrue(b.purchase("does_not_exist") !is PurchaseResult.Success)
    }
}
