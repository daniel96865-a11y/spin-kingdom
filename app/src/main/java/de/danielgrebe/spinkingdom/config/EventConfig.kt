package de.danielgrebe.spinkingdom.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * Time limited events. Loaded from assets/events.json so events can be switched on/off and
 * re-dated without touching code. Later this file can be delivered by a server.
 */
@Serializable
data class GameEvent(
    val id: String,
    val start: String,               // ISO date, inclusive
    val end: String,                 // ISO date, inclusive
    val weekdays: List<Int> = emptyList(), // optional ISO weekdays (1=Mon..7=Sun); empty = every day
    val enabled: Boolean = true,
    val coinMultiplier: Double = 1.0,
    val spinRewardMultiplier: Double = 1.0,
    val raidMultiplier: Double = 1.0,
    val attackMultiplier: Double = 1.0,
    val chestUpgrade: Int = 0        // chest tiers added to rewarded chests
) {
    fun isActive(date: LocalDate): Boolean {
        if (!enabled) return false
        val s = runCatching { LocalDate.parse(start) }.getOrNull() ?: return false
        val e = runCatching { LocalDate.parse(end) }.getOrNull() ?: return false
        if (date.isBefore(s) || date.isAfter(e)) return false
        return weekdays.isEmpty() || date.dayOfWeek.value in weekdays
    }
}

@Serializable
data class EventsFile(val events: List<GameEvent> = emptyList())

/** Combined multipliers of every currently active event. */
data class EventModifiers(
    val active: List<GameEvent> = emptyList(),
    val coin: Double = 1.0,
    val spins: Double = 1.0,
    val raid: Double = 1.0,
    val attack: Double = 1.0,
    val chestUpgrade: Int = 0
) {
    companion object {
        val NONE = EventModifiers()
        fun of(events: List<GameEvent>) = EventModifiers(
            active = events,
            coin = events.fold(1.0) { a, e -> a * e.coinMultiplier },
            spins = events.fold(1.0) { a, e -> a * e.spinRewardMultiplier },
            raid = events.fold(1.0) { a, e -> a * e.raidMultiplier },
            attack = events.fold(1.0) { a, e -> a * e.attackMultiplier },
            chestUpgrade = events.sumOf { it.chestUpgrade }
        )
    }
}

object EventConfig {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<GameEvent> =
        runCatching { json.decodeFromString(EventsFile.serializer(), text).events }.getOrDefault(emptyList())

    fun modifiers(events: List<GameEvent>, date: LocalDate, forcedId: String? = null): EventModifiers {
        val active = events.filter { it.isActive(date) || (forcedId != null && it.id == forcedId) }
        return EventModifiers.of(active)
    }
}
