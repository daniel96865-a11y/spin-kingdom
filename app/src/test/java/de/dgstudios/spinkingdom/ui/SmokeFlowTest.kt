package de.dgstudios.spinkingdom.ui

import android.app.Application
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.dgstudios.spinkingdom.models.GameState
import de.dgstudios.spinkingdom.models.SlotSymbol
import de.dgstudios.spinkingdom.navigation.Routes
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** End-to-end flow through the real UI: first launch -> intro -> village -> spin -> build -> menu screens. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "de-w400dp-h860dp-xxhdpi", application = Application::class)
class SmokeFlowTest : ScreenshotTestBase() {

    @Test fun firstLaunchToVillageAndMainFlows() {
        launch(GameState(), Routes.SPLASH)
        advance(4000) // splash + loading bar
        compose.onNodeWithTag("intro").assertExists()
        shot("21_intro")
        compose.onNodeWithTag("intro_next").performClick(); advance(600)
        compose.onNodeWithTag("intro_next").performClick(); advance(600)
        compose.onNodeWithTag("name_field").performTextInput("Daniel")
        advance(300)
        shot("22_intro_name")
        compose.onNodeWithTag("intro_start").performClick()
        advance(2500)
        assertTrue(repo.saved.introDone)
        assertEquals("Daniel", repo.saved.playerName)
        assertEquals(50, repo.saved.spins)
        compose.onNodeWithTag("spin_button").assertExists()

        // spin via the real button
        compose.onNodeWithTag("spin_button").performClick()
        advance(5000)
        assertEquals(1, repo.saved.stats.totalSpins)
        // dismiss any popup that may have appeared
        repeat(3) {
            if (compose.onAllNodesWithTag("overlay_ok").fetchSemanticsNodes().isNotEmpty()) {
                compose.onAllNodesWithTag("overlay_ok")[0].performClick(); advance(800)
            }
        }
        // build something
        compose.onNodeWithTag("build_button").performClick(); advance(700)
        compose.onNodeWithTag("upgrade_0").performClick(); advance(1200)
        assertEquals(1, repo.saved.buildings[0].stage)
        shot("23_build_after_upgrade")
        compose.onNodeWithContentDescription("Schließen").performClick(); advance(600)

        // open daily bonus via the menu and claim it
        vm.dismissOverlay()
        advance(300)
        compose.onNodeWithTag("menu_button").performClick(); advance(600)
        compose.onNodeWithTag("menu_daily").performClick(); advance(900)
        compose.onNodeWithTag("daily_claim").performClick(); advance(1500)
        assertEquals(1, repo.saved.dailyBonus.totalClaims)
        shot("25_daily_claimed")
        // leaderboard + settings reachable through the menu
        vm.dismissOverlay(); advance(300)
        compose.onNodeWithContentDescription("Zurück").performClick(); advance(800)
        compose.onNodeWithTag("menu_button").performClick(); advance(600)
        compose.onNodeWithTag("menu_leaderboard").performClick(); advance(900)
        assertTrue(compose.onAllNodesWithText("Emilia", substring = true).fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Level", substring = true).fetchSemanticsNodes().size > 5)
        shot("26_leaderboard_from_menu")
    }

    @Test fun rewardedAdGivesSpins() {
        launch(GameState(), Routes.SPLASH)
        vm.completeIntro(null, 1)
        advance(500)
        val before = repo.saved.spins
        vm.watchAdForSpins()
        advance(1000)
        shot("24_ad_overlay")
        advance(6000)
        compose.onNodeWithTag("ad_close").performClick()
        advance(1500)
        assertEquals(before + 10, repo.saved.spins)
    }

    @Test fun forcedAttackFlow() {
        launch(GameState(), Routes.SPLASH)
        vm.completeIntro("Tester", 0)
        advance(500)
        vm.spin(listOf(SlotSymbol.ATTACK, SlotSymbol.ATTACK, SlotSymbol.ATTACK))
        advance(5000)
        assertEquals("attack", repo.saved.pendingActions.firstOrNull()?.kind)
    }

    @Test fun stateSurvivesAppRestart() {
        launch(GameState(), Routes.SPLASH)
        vm.completeIntro("Resume", 4)
        advance(300)
        vm.spin(listOf(SlotSymbol.COIN, SlotSymbol.COIN, SlotSymbol.SHIELD)); advance(5000)
        vm.upgrade(0); advance(500)
        val saved = repo.saved
        assertTrue(saved.introDone && saved.buildings[0].stage == 1 && saved.stats.totalSpins == 1L)
        // "restart": a brand new ViewModel reading the same repository
        val vm2 = GameViewModel(repo, de.dgstudios.spinkingdom.data.LocalOpponentRepository(), de.dgstudios.spinkingdom.data.LocalLeaderboardRepository("Gast"),
            de.dgstudios.spinkingdom.data.FakeBillingRepository(), de.dgstudios.spinkingdom.data.SimulatedAdsRepository(), time, emptyList(), null)
        advance(300)
        val restored = vm2.state.value
        assertEquals(saved.copy(clock = restored.clock), restored)
        // 25 minutes later: two regen ticks were applied from timestamps
        time.wall += 25 * 60_000L
        val vm3 = GameViewModel(repo, de.dgstudios.spinkingdom.data.LocalOpponentRepository(), de.dgstudios.spinkingdom.data.LocalLeaderboardRepository("Gast"),
            de.dgstudios.spinkingdom.data.FakeBillingRepository(), de.dgstudios.spinkingdom.data.SimulatedAdsRepository(), time, emptyList(), null)
        advance(300)
        assertEquals(minOf(50, saved.spins + 10), vm3.state.value.spins)
    }
}
