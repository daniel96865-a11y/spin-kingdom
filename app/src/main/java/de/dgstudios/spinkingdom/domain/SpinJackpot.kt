package de.dgstudios.spinkingdom.domain

import de.dgstudios.spinkingdom.config.GameBalanceConfig
import de.dgstudios.spinkingdom.models.GameState
import kotlin.random.Random

/** Rare big spin rewards ("Spin-Jackpot"). */
object SpinJackpot {
    /** Rolls a table of (spins, chance) entries in order; returns 0 if nothing hits. */
    fun roll(chances: List<Pair<Int, Double>>, random: Random): Int {
        val r = random.nextDouble()
        var acc = 0.0
        for ((spins, chance) in chances) {
            acc += chance
            if (r < acc) return spins
        }
        return 0
    }

    /** Final amount for a slot spin jackpot including multiplier and event modifier, capped. */
    fun scaled(base: Int, multiplier: Int, eventSpins: Double): Int {
        if (base <= 0) return 0
        val m = if (GameBalanceConfig.SPIN_JACKPOT_APPLY_MULTIPLIER) multiplier else 1
        return (base * m * eventSpins).toInt().coerceIn(base, maxOf(base, GameBalanceConfig.SPIN_JACKPOT_MAX_SPINS))
    }
}

/** "Glücks-Einsatz": temporary x20/x30 unlock. */
object LuckyBoost {
    fun isActive(state: GameState, now: Long) = now < state.luckyBoostUntil

    fun millisLeft(state: GameState, now: Long): Long = (state.luckyBoostUntil - now).coerceAtLeast(0)

    /** True if the boost would unlock something the player doesn't have by level. */
    fun isUseful(level: Int) = GameBalanceConfig.LUCKY_BOOST_MULTIPLIERS.any { it !in GameBalanceConfig.availableMultipliers(level) }

    fun canTrigger(state: GameState, now: Long): Boolean =
        state.level >= GameBalanceConfig.LUCKY_BOOST_MIN_LEVEL && isUseful(state.level) && !isActive(state, now) &&
            (state.luckyBoostUntil == 0L || now >= state.luckyBoostUntil + GameBalanceConfig.LUCKY_BOOST_COOLDOWN_MS)

    fun multipliers(state: GameState, now: Long) = GameBalanceConfig.availableMultipliers(state.level, isActive(state, now))
}
