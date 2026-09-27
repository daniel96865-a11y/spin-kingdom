package de.danielgrebe.spinkingdom.storage

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import de.danielgrebe.spinkingdom.models.GameState
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object SaveCodec {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
    fun encode(state: GameState): String = json.encodeToString(GameState.serializer(), state)
    fun decode(text: String): GameState = json.decodeFromString(GameState.serializer(), text)
}

/** Stores the whole save game as JSON in a DataStore file (atomic writes, survives restarts). */
object GameStateSerializer : Serializer<GameState> {
    override val defaultValue: GameState = GameState()

    override suspend fun readFrom(input: InputStream): GameState {
        val text = input.readBytes().decodeToString()
        if (text.isBlank()) return defaultValue
        return try { SaveCodec.decode(text) } catch (e: SerializationException) {
            throw CorruptionException("Save game corrupted", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Save game corrupted", e)
        }
    }

    override suspend fun writeTo(t: GameState, output: OutputStream) {
        output.write(SaveCodec.encode(t).encodeToByteArray())
    }
}
