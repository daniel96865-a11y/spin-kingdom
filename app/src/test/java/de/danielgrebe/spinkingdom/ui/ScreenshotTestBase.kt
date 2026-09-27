package de.danielgrebe.spinkingdom.ui

import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureScreenRoboImage
import de.danielgrebe.spinkingdom.config.EventConfig
import de.danielgrebe.spinkingdom.data.FakeBillingRepository
import de.danielgrebe.spinkingdom.data.LocalLeaderboardRepository
import de.danielgrebe.spinkingdom.data.LocalOpponentRepository
import de.danielgrebe.spinkingdom.data.SimulatedAdsRepository
import de.danielgrebe.spinkingdom.domain.GameStateRepository
import de.danielgrebe.spinkingdom.domain.TimeSource
import de.danielgrebe.spinkingdom.models.GameState
import de.danielgrebe.spinkingdom.navigation.SpinKingdomRoot
import de.danielgrebe.spinkingdom.ui.theme.SpinKingdomTheme
import org.junit.Rule
import java.io.File
import java.time.ZoneId
import kotlin.random.Random

class InMemoryRepo(var saved: GameState) : GameStateRepository {
    var saves = 0
    override val state: kotlinx.coroutines.flow.Flow<GameState> get() = kotlinx.coroutines.flow.flowOf(saved)
    override suspend fun load() = saved
    override suspend fun save(state: GameState) { saved = state; saves++ }
    override suspend fun reset() { saved = GameState() }
}

class FakeTime(var wall: Long) : TimeSource {
    override fun wallMillis() = wall
    override fun elapsedRealtime() = wall - START + 10_000
    companion object { val START = 1_791_374_400_000L } // 2026-10-07 12:00 local-ish
}

abstract class ScreenshotTestBase {
    @get:Rule val compose = createComposeRule()

    protected val time = FakeTime(FakeTime.START)
    protected lateinit var repo: InMemoryRepo
    protected lateinit var vm: GameViewModel

    private fun events() = EventConfig.parse(
        listOf(File("src/main/assets/events.json"), File("app/src/main/assets/events.json")).first { it.exists() }.readText()
    )

    protected fun launch(state: GameState, route: String, seed: Int = 7, setup: (GameViewModel) -> Unit = {}) {
        repo = InMemoryRepo(state)
        vm = GameViewModel(
            repo, LocalOpponentRepository(Random(seed)), LocalLeaderboardRepository("Gast"), FakeBillingRepository(),
            SimulatedAdsRepository(), time, events(), audio = null, random = Random(seed), zone = { ZoneId.of("Europe/Berlin") }
        )
        compose.mainClock.autoAdvance = false
        compose.setContent { SpinKingdomTheme { SpinKingdomRoot(vm, startRoute = route) } }
        advance(100)
        setup(vm)
        advance(100)
    }

    protected fun advance(ms: Long) {
        var left = ms
        while (left > 0) {
            val step = minOf(left, 100L)
            compose.mainClock.advanceTimeBy(step)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(step))
            left -= step
        }
    }

    protected fun shot(name: String) {
        val dir = System.getProperty("spinkingdom.screenshotDir") ?: "build/screens"
        File(dir).mkdirs()
        captureScreenRoboImage("$dir/$name.png")
    }
}
