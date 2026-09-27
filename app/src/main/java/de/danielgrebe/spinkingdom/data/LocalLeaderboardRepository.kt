package de.danielgrebe.spinkingdom.data

import de.danielgrebe.spinkingdom.domain.LeaderboardEntry
import de.danielgrebe.spinkingdom.domain.LeaderboardRepository
import de.danielgrebe.spinkingdom.models.GameState
import kotlin.random.Random

/**
 * Offline leaderboard with deterministic bots. Bots keep progressing with real time since install,
 * so the ranking changes over days. Replace with a server leaderboard later.
 */
class LocalLeaderboardRepository(private val playerFallbackName: String) : LeaderboardRepository {
    override suspend fun leaderboard(player: GameState, now: Long): List<LeaderboardEntry> = build(player, now, playerFallbackName)

    companion object {
        fun build(player: GameState, now: Long, fallbackName: String): List<LeaderboardEntry> {
            val days = ((now - player.createdAt).coerceAtLeast(0) / 86_400_000L).toInt()
            val seed = player.playerId.hashCode().toLong()
            val bots = LocalOpponentRepository.NAMES.mapIndexed { i, name ->
                val r = Random(seed * 31 + i)
                val speed = 0.3 + r.nextDouble() * 1.4           // levels per day
                val start = 1 + r.nextInt(12)
                val level = (start + (days * speed).toInt()).coerceAtMost(500)
                val stars = (level - 1) * 35 + r.nextInt(0, 25)
                Triple(name, level, stars) to r.nextInt(LocalOpponentRepository.AVATARS)
            }
            val playerName = player.playerName.ifBlank { fallbackName }
            val all = bots.map { (t, av) -> LeaderboardEntry(0, t.first, t.second, t.third, av, false) } +
                LeaderboardEntry(0, playerName, player.level, player.stars, player.avatar, true)
            return all.sortedWith(compareByDescending<LeaderboardEntry> { it.stars }.thenByDescending { it.level })
                .mapIndexed { i, e -> e.copy(rank = i + 1) }
        }
    }
}
