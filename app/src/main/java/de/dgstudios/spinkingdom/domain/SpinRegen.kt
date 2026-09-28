package de.dgstudios.spinkingdom.domain

import de.dgstudios.spinkingdom.config.GameBalanceConfig
import de.dgstudios.spinkingdom.models.GameState

object SpinRegen {
    /** Adds regenerated spins based on timestamps (works for offline time too). */
    fun apply(state: GameState, now: Long): GameState {
        val interval = GameBalanceConfig.SPIN_REGEN_INTERVAL_MS
        val cap = GameBalanceConfig.MAX_AUTO_SPINS
        if (state.spins >= cap) return state.copy(lastRegenMillis = now)
        if (now < state.lastRegenMillis) return state // clock went back: wait until it catches up
        val intervals = (now - state.lastRegenMillis) / interval
        if (intervals <= 0) return state
        val gained = (intervals * GameBalanceConfig.SPIN_REGEN_AMOUNT).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val newSpins = minOf(cap, state.spins + gained)
        val newLast = if (newSpins >= cap) now else state.lastRegenMillis + intervals * interval
        return state.copy(spins = newSpins, lastRegenMillis = newLast)
    }

    /** Milliseconds until the next regeneration tick, or null if the automatic cap is reached. */
    fun millisUntilNext(state: GameState, now: Long): Long? {
        if (state.spins >= GameBalanceConfig.MAX_AUTO_SPINS) return null
        val elapsed = (now - state.lastRegenMillis).coerceAtLeast(0)
        return (GameBalanceConfig.SPIN_REGEN_INTERVAL_MS - elapsed % GameBalanceConfig.SPIN_REGEN_INTERVAL_MS)
    }

    /** Milliseconds until spins are refilled to the automatic cap. */
    fun millisUntilFull(state: GameState, now: Long): Long? {
        val missing = GameBalanceConfig.MAX_AUTO_SPINS - state.spins
        if (missing <= 0) return null
        val ticks = (missing + GameBalanceConfig.SPIN_REGEN_AMOUNT - 1) / GameBalanceConfig.SPIN_REGEN_AMOUNT
        val next = millisUntilNext(state, now) ?: return null
        return next + (ticks - 1) * GameBalanceConfig.SPIN_REGEN_INTERVAL_MS
    }
}
