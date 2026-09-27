package de.danielgrebe.spinkingdom

import android.content.Context
import android.os.SystemClock
import de.danielgrebe.spinkingdom.audio.AudioManager
import de.danielgrebe.spinkingdom.config.EventConfig
import de.danielgrebe.spinkingdom.config.GameEvent
import de.danielgrebe.spinkingdom.data.FakeBillingRepository
import de.danielgrebe.spinkingdom.data.LocalLeaderboardRepository
import de.danielgrebe.spinkingdom.data.LocalOpponentRepository
import de.danielgrebe.spinkingdom.data.SimulatedAdsRepository
import de.danielgrebe.spinkingdom.domain.AdsRepository
import de.danielgrebe.spinkingdom.domain.BillingRepository
import de.danielgrebe.spinkingdom.domain.GameStateRepository
import de.danielgrebe.spinkingdom.domain.LeaderboardRepository
import de.danielgrebe.spinkingdom.domain.OpponentRepository
import de.danielgrebe.spinkingdom.domain.TimeSource
import de.danielgrebe.spinkingdom.storage.DataStoreGameStateRepository

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
