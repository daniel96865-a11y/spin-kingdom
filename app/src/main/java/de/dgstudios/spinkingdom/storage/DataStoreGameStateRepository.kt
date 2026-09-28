package de.dgstudios.spinkingdom.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import de.dgstudios.spinkingdom.domain.GameStateRepository
import de.dgstudios.spinkingdom.models.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DataStoreGameStateRepository(private val store: DataStore<GameState>) : GameStateRepository {
    override val state: Flow<GameState> = store.data
    override suspend fun load(): GameState = store.data.first()
    override suspend fun save(state: GameState) { store.updateData { state } }
    override suspend fun reset() { store.updateData { GameState() } }

    companion object {
        @Volatile private var instance: DataStore<GameState>? = null
        fun create(context: Context): DataStoreGameStateRepository {
            val ds = instance ?: synchronized(this) {
                instance ?: DataStoreFactory.create(
                    serializer = GameStateSerializer,
                    corruptionHandler = ReplaceFileCorruptionHandler { GameState() },
                    produceFile = { context.applicationContext.dataStoreFile("spinkingdom_save.json") }
                ).also { instance = it }
            }
            return DataStoreGameStateRepository(ds)
        }
    }
}
