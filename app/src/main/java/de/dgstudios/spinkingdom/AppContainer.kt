package de.dgstudios.spinkingdom

import android.content.Context
import android.os.SystemClock
import de.dgstudios.spinkingdom.audio.AudioManager
import de.dgstudios.spinkingdom.config.EventConfig
import de.dgstudios.spinkingdom.config.GameEvent
import de.dgstudios.spinkingdom.data.FakeBillingRepository
import de.dgstudios.spinkingdom.data.LocalLeaderboardRepository
import de.dgstudios.spinkingdom.data.LocalOpponentRepository
import de.dgstudios.spinkingdom.data.SimulatedAdsRepository
import de.dgstudios.spinkingdom.domain.AdsRepository
import de.dgstudios.spinkingdom.domain.BillingRepository
import de.dgstudios.spinkingdom.domain.GameStateRepository
import de.dgstudios.spinkingdom.domain.LeaderboardRepository
import de.dgstudios.spinkingdom.domain.OpponentRepository
import de.dgstudios.spinkingdom.domain.TimeSource
import de.dgstudios.spinkingdom.storage.DataStoreGameStateRepository

object SystemTimeSource : TimeSource {
    override fun wallMillis() = System.currentTimeMillis()
    override fun elapsedRealtime() = SystemClock.elapsedRealtime()
}

/**
 * Manual dependency injection. Every backend-facing piece is an interface so it can be replaced
 * by a server / Firebase / Play Billing / AdMob implementation without touching the game logic.
 */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    val gameRepository: GameStateRepository = DataStoreGameStateRepository.create(app)
    val opponentRepository: OpponentRepository = LocalOpponentRepository()
    val leaderboardRepository: LeaderboardRepository = LocalLeaderboardRepository(app.getString(R.string.guest))
    val billingRepository: BillingRepository = FakeBillingRepository()
    val adsRepository: AdsRepository = SimulatedAdsRepository()
    val timeSource: TimeSource = SystemTimeSource
    val events: List<GameEvent> = runCatching {
        app.assets.open("events.json").bufferedReader().use { EventConfig.parse(it.readText()) }
    }.getOrDefault(emptyList())
    val audio: AudioManager by lazy { AudioManager(app) }
}
