package de.danielgrebe.spinkingdom.ui

import android.app.Application
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.domain.Cards
import de.danielgrebe.spinkingdom.domain.Pets
import de.danielgrebe.spinkingdom.game.Ctx
import de.danielgrebe.spinkingdom.game.GameEngine
import de.danielgrebe.spinkingdom.models.*
import de.danielgrebe.spinkingdom.navigation.Routes
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the real Compose UI in Robolectric (native graphics) and stores PNG screenshots in out/screens. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "de-w400dp-h860dp-xxhdpi", application = Application::class)
class ScreenshotTest : ScreenshotTestBase() {

    private fun midGame(level: Int = 4): GameState {
        val now = FakeTime.START
        val c = Ctx(now, now / 86_400_000L, now / 86_400_000L - 2)
        var s = GameEngine.finishIntro(GameEngine.newGame("SK7Q2M4X", now), "Daniel", 3, c)
        s = s.copy(
            level = level, coins = 1_284_500, spins = 42, shields = 2, stars = 96,
            buildings = listOf(BuildingState(5), BuildingState(4), BuildingState(3), BuildingState(1), BuildingState(2, damaged = true)),
            cards = Cards.cardsOfSet(0).take(6).associateWith { 1 } + mapOf(9 to 2, 13 to 1),
            petTreats = 6, lastRegenMillis = now - 91_000L
        )
        return Pets.syncUnlocks(s)
    }

    @Test fun village() {
        launch(midGame(), Routes.VILLAGE)
        advance(1500)
        compose.onNodeWithTag("spin_button").assertExists()
        compose.onNodeWithTag("slot_machine").assertExists()
        shot("01_village")
    }

    @Test fun villageOtherThemes() {
        launch(midGame(level = 9).copy(buildings = List(5) { BuildingState(it + 1) }), Routes.VILLAGE)
        advance(1200)
        shot("02_village_space")
    }

    @Test fun villageProceduralLevel() {
        launch(midGame(level = 42).copy(buildings = List(5) { BuildingState(5 - it) }), Routes.VILLAGE)
        advance(1200)
        shot("03_village_level42")
    }

    @Test fun spinWithBigWin() {
        launch(midGame(), Routes.VILLAGE)
        advance(500)
        compose.onNodeWithTag("mult_3").performClick()
        advance(200)
        vm.spin(listOf(SlotSymbol.COIN, SlotSymbol.COIN, SlotSymbol.COIN))
        advance(900)
        shot("04_spinning")
        advance(4000)
        shot("05_big_win")
        assertTrue(repo.saved.coins > 1_284_500)
    }

    @Test fun buildMenu() {
        launch(midGame(), Routes.VILLAGE)
        advance(400)
        compose.onNodeWithTag("build_button").performClick()
        advance(800)
        shot("06_build_menu")
    }

    @Test fun attack() {
        launch(midGame().copy(pendingActions = listOf(PendingAction("attack", 2))), Routes.ATTACK) { it.prepareAttack() }
        advance(1500)
        shot("07_attack_choose")
    }

    @Test fun raid() {
        launch(midGame().copy(pendingActions = listOf(PendingAction("raid", 1))), Routes.RAID) { it.prepareRaid() }
        advance(1200)
        compose.onNodeWithTag("dig_4").performClick()
        advance(1500)
        shot("08_raid")
    }

    @Test fun chestOpening() {
        launch(midGame(), Routes.VILLAGE) { it.debugChest(ChestType.ROYAL) }
        advance(2500)
        shot("09_chest")
    }

    @Test fun levelComplete() {
        launch(midGame(), Routes.VILLAGE) { it.debugCompleteLevel() }
        advance(2000)
        shot("10_level_complete")
    }

    @Test fun daily() { launch(midGame(), Routes.DAILY); advance(1000); shot("11_daily_bonus") }
    @Test fun wheel() { launch(midGame(), Routes.WHEEL); advance(1000); shot("12_wheel") }
    @Test fun missions() { launch(midGame(), Routes.MISSIONS); advance(800); shot("13_missions") }
    @Test fun pets() { launch(midGame(level = 12), Routes.PETS); advance(1000); shot("14_pets") }
    @Test fun cards() { launch(midGame(), Routes.CARDS); advance(800); shot("15_cards") }
    @Test fun leaderboard() { launch(midGame(), Routes.LEADERBOARD) { it.loadLeaderboard() }; advance(1000); shot("16_leaderboard") }
    @Test fun profile() { launch(midGame(), Routes.PROFILE); advance(800); shot("17_profile") }
    @Test fun shop() { launch(midGame(), Routes.SHOP); advance(800); shot("18_shop") }
    @Test fun settings() { launch(midGame(), Routes.SETTINGS); advance(800); shot("19_settings") }
    @Test fun debugMenu() { launch(midGame(), Routes.DEBUG); advance(800); shot("20_debug_menu") }
    @Test fun splash() { launch(GameState(), Routes.SPLASH); advance(900); shot("00_splash") }

    // ---------------------------------------------------------------- 1.1: spin jackpot & lucky boost
    @Test fun spinJackpotOverlay() {
        // FixedRandom(0.01) forces the rare +30 spin jackpot on 3x Energie
        launch(midGame().copy(selectedMultiplier = 1), Routes.VILLAGE, random = de.danielgrebe.spinkingdom.game.FixedRandom(0.01))
        advance(400)
        val before = repo.saved.spins
        vm.spin(listOf(SlotSymbol.ENERGY, SlotSymbol.ENERGY, SlotSymbol.ENERGY))
        advance(5500)
        compose.onNodeWithTag("spin_jackpot", useUnmergedTree = true).assertExists()
        shot("40_spin_jackpot")
        assertEquals(before - 1 + 10 + 30, repo.saved.spins)
    }

    @Test fun luckyBoostAnnouncement() {
        launch(midGame(level = 3), Routes.VILLAGE) { it.debugLuckyBoost() }
        advance(1500)
        compose.onNodeWithTag("lucky_boost", useUnmergedTree = true).assertExists()
        shot("41_lucky_boost_announcement")
    }

    @Test fun multiplierRowWithLuckyBoost() {
        val until = FakeTime.START + 7 * 60_000L + 42_000L
        launch(midGame(level = 3).copy(luckyBoostUntil = until, selectedMultiplier = 30, spins = 120), Routes.VILLAGE)
        advance(1200)
        compose.onNodeWithTag("lucky_boost_badge").assertExists()
        compose.onNodeWithTag("mult_20").assertExists()
        compose.onNodeWithTag("mult_30").assertExists()
        shot("42_multiplier_lucky_boost")
    }
}
