package de.danielgrebe.spinkingdom.storage

import de.danielgrebe.spinkingdom.*
import de.danielgrebe.spinkingdom.game.GameEngine
import de.danielgrebe.spinkingdom.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SaveCodecTest {
    private fun richState(): GameState {
        var s = startedGame().copy(level = 12, shields = 2, stars = 77, petTreats = 4, selectedMultiplier = 5)
        s = de.danielgrebe.spinkingdom.domain.Pets.syncUnlocks(s)
        s = s.copy(
            buildings = listOf(BuildingState(1), BuildingState(5), BuildingState(3, damaged = true), BuildingState(0), BuildingState(2)),
            cards = mapOf(0 to 2, 5 to 1, 53 to 1),
            claimedSets = setOf(1),
            pendingActions = listOf(PendingAction("raid", 10)),
            log = listOf(LogEntry(T0, "bot_attack_hit", "Mia", 2)),
            settings = Settings(music = false, sfx = true, vibration = false, notifications = true, language = "en"),
            dailyBonus = DailyBonusState(streakDay = 3, lastClaimDay = 20_000, totalClaims = 10),
            clock = ClockState(maxTrustedMillis = T0, lastWallMillis = T0, lastElapsedRealtime = 123, driftMillis = 5, tamperCount = 1),
            activePet = PetType.DRAGON
        )
        return s
    }

    @Test fun roundTripKeepsEverything() {
        val s = richState()
        val decoded = SaveCodec.decode(SaveCodec.encode(s))
        assertEquals(s, decoded)
    }

    @Test fun dataStoreSerializerRoundTrip() = runBlocking {
        val s = richState()
        val out = ByteArrayOutputStream()
        GameStateSerializer.writeTo(s, out)
        val back = GameStateSerializer.readFrom(ByteArrayInputStream(out.toByteArray()))
        assertEquals(s, back)
    }

    @Test fun emptyFileGivesDefaultAndUnknownFieldsAreIgnored() = runBlocking {
        assertEquals(GameState(), GameStateSerializer.readFrom(ByteArrayInputStream(ByteArray(0))))
        val json = """{"playerId":"abc","level":4,"coins":99,"futureField":true}"""
        val s = SaveCodec.decode(json)
        assertEquals(4, s.level)
        assertEquals(99L, s.coins)
        assertEquals(5, s.buildings.size)
    }

    @Test(expected = androidx.datastore.core.CorruptionException::class)
    fun corruptFileIsReported() {
        runBlocking { GameStateSerializer.readFrom(ByteArrayInputStream("{not json".toByteArray())) }
    }

    @Test fun newGameIsResumable() {
        val s = GameEngine.newGame("id", T0)
        assertEquals(s, SaveCodec.decode(SaveCodec.encode(s)))
    }
}
