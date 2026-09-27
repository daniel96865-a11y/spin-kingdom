package de.danielgrebe.spinkingdom.ui

import android.app.Application
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.danielgrebe.spinkingdom.game.Ctx
import de.danielgrebe.spinkingdom.game.GameEngine
import de.danielgrebe.spinkingdom.models.BuildingState
import de.danielgrebe.spinkingdom.navigation.Routes
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Verifies the English resources are complete enough to render the main screens. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "en-w400dp-h860dp-xxhdpi", application = Application::class)
class EnglishScreenshotTest : ScreenshotTestBase() {
    private fun state() = GameEngine.finishIntro(GameEngine.newGame("SK7Q2M4X", FakeTime.START), "Daniel", 1, Ctx(FakeTime.START, FakeTime.START / 86_400_000L, 0))
        .copy(level = 7, coins = 88_400, spins = 50, shields = 1, buildings = List(5) { BuildingState((it * 2) % 6) })

    @Test fun villageEnglish() {
        launch(state(), Routes.VILLAGE)
        advance(1200)
        compose.onNodeWithText("SPIN").assertExists()
        shot("30_village_en")
    }

    @Test fun settingsEnglish() {
        launch(state(), Routes.SETTINGS)
        advance(800)
        compose.onNodeWithText("Settings").assertExists()
        shot("31_settings_en")
    }
}
