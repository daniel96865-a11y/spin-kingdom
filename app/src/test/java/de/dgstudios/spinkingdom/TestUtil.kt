package de.dgstudios.spinkingdom

import de.dgstudios.spinkingdom.config.EventModifiers
import de.dgstudios.spinkingdom.game.Ctx
import de.dgstudios.spinkingdom.game.GameEngine
import de.dgstudios.spinkingdom.models.GameState
import kotlin.random.Random

const val T0 = 1_790_000_000_000L // fixed epoch millis used by all tests
const val DAY = 86_400_000L

fun ctx(now: Long = T0, seed: Int = 1, mods: EventModifiers = EventModifiers.NONE): Ctx {
    val today = now / DAY
    val week = today - ((today + 3) % 7) // Monday of the week (1970-01-01 was a Thursday)
    return Ctx(now, today, week, mods, Random(seed))
}

fun startedGame(now: Long = T0): GameState =
    GameEngine.finishIntro(GameEngine.newGame("test-player", now), "Daniel", 2, ctx(now))
