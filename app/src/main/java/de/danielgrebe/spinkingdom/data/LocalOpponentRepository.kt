package de.danielgrebe.spinkingdom.data

import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.domain.Opponent
import de.danielgrebe.spinkingdom.domain.OpponentRepository
import kotlin.random.Random

/** Offline bots. Swap for a server implementation to attack real players. */
class LocalOpponentRepository(private val random: Random = Random.Default) : OpponentRepository {

    override fun botNames(): List<String> = NAMES

    private fun make(playerLevel: Int, shieldChance: Double): Opponent {
        val name = NAMES[random.nextInt(NAMES.size)]
        val level = (playerLevel + random.nextInt(-2, 4)).coerceAtLeast(1)
        val stages = List(GameBalanceConfig.BUILDINGS_PER_LEVEL) { random.nextInt(1, GameBalanceConfig.MAX_STAGE + 1) }
        return Opponent(
            id = "bot_${name.lowercase()}_${random.nextInt(1000, 9999)}",
            name = name,
            avatar = random.nextInt(AVATARS),
            level = level,
            stages = stages,
            hasShield = random.nextDouble() < shieldChance,
            stars = level * 30 + stages.sum()
        )
    }

    override suspend fun findAttackTarget(playerLevel: Int) = make(playerLevel, GameBalanceConfig.OPPONENT_SHIELD_CHANCE)
    override suspend fun findRaidTarget(playerLevel: Int) = make(playerLevel, 0.0)

    companion object {
        const val AVATARS = 8
        val NAMES = listOf(
            "Alex", "Mia", "Leon", "Emma", "Noah", "Sophie", "Finn", "Hannah", "Paul", "Lina",
            "Ben", "Clara", "Elias", "Lea", "Jonas", "Marie", "Luca", "Emilia", "Felix", "Ida",
            "Max", "Lena", "Anton", "Nora", "Theo", "Mila", "Jakob", "Ella", "Oskar", "Frieda"
        )
    }
}
