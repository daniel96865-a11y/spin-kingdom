package de.danielgrebe.spinkingdom.config

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class ConfigTest {
    private fun assetsEvents(): String {
        val candidates = listOf(File("src/main/assets/events.json"), File("app/src/main/assets/events.json"))
        return candidates.first { it.exists() }.readText()
    }

    @Test fun eventsFileParsesAndHasAllEvents() {
        val events = EventConfig.parse(assetsEvents())
        val ids = events.map { it.id }.toSet()
        assertTrue(ids.containsAll(listOf("pirates", "halloween", "christmas", "summer", "treasure_hunt", "double_coins", "bonus_spins")))
    }

    @Test fun eventsActivateByDateAndWeekday() {
        val events = listOf(
            GameEvent("a", "2026-10-01", "2026-10-05", coinMultiplier = 2.0),
            GameEvent("b", "2026-01-01", "2026-12-31", weekdays = listOf(7), spinRewardMultiplier = 1.5),
            GameEvent("c", "2026-01-01", "2026-12-31", enabled = false, raidMultiplier = 3.0)
        )
        val m = EventConfig.modifiers(events, LocalDate.parse("2026-10-04")) // a Sunday
        assertEquals(2.0, m.coin, 1e-9)
        assertEquals(1.5, m.spins, 1e-9)
        assertEquals(1.0, m.raid, 1e-9)
        val none = EventConfig.modifiers(events, LocalDate.parse("2026-10-07"))
        assertEquals(1.0, none.coin, 1e-9)
        assertTrue(none.active.isEmpty())
        val forced = EventConfig.modifiers(events, LocalDate.parse("2026-10-07"), forcedId = "a")
        assertEquals(2.0, forced.coin, 1e-9)
    }

    @Test fun brokenEventsFileIsIgnoredSafely() {
        assertTrue(EventConfig.parse("garbage").isEmpty())
    }

    @Test fun levelConfigCoversFiveHundredLevels() {
        assertEquals(30, LevelConfig.CONFIGURED_LEVELS)
        assertEquals(500, LevelConfig.MAX_LEVEL)
        for (l in 1..500) {
            val d = LevelConfig.level(l)
            assertEquals(l, d.level)
            assertEquals(5, d.archetypes.size)
        }
        // procedural levels differ from their base theme
        assertNotEquals(LevelConfig.level(1).palette, LevelConfig.level(31).palette)
        assertEquals(LevelConfig.level(31).theme, LevelConfig.level(1).theme)
        // every configured theme is unique
        assertEquals(30, (1..30).map { LevelConfig.level(it).theme.nameRes }.toSet().size)
    }

    @Test fun multipliersUnlockByLevel() {
        assertEquals(listOf(1, 2, 3, 5, 10), GameBalanceConfig.availableMultipliers(1))
        assertTrue(20 in GameBalanceConfig.availableMultipliers(5))
        assertTrue(50 in GameBalanceConfig.availableMultipliers(10))
        assertTrue(100 in GameBalanceConfig.availableMultipliers(20))
        assertFalse(100 in GameBalanceConfig.availableMultipliers(19))
    }

    @Test fun hueShiftKeepsAlpha() {
        val c = LevelConfig.shiftHue(0xFF336699, 120f)
        assertEquals(0xFF, ((c shr 24) and 0xFF).toInt())
        assertNotEquals(0xFF336699, c)
    }
}
