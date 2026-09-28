package de.dgstudios.spinkingdom.navigation

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.dgstudios.spinkingdom.ui.GameViewModel
import de.dgstudios.spinkingdom.ui.components.GameText
import de.dgstudios.spinkingdom.ui.effects.rememberGameTime
import de.dgstudios.spinkingdom.ui.screens.AdOverlay
import de.dgstudios.spinkingdom.ui.screens.AttackScreen
import de.dgstudios.spinkingdom.ui.screens.CardsScreen
import de.dgstudios.spinkingdom.ui.screens.DailyBonusScreen
import de.dgstudios.spinkingdom.ui.screens.DebugScreen
import de.dgstudios.spinkingdom.ui.screens.IntroScreen
import de.dgstudios.spinkingdom.ui.screens.LeaderboardScreen
import de.dgstudios.spinkingdom.ui.screens.MainActions
import de.dgstudios.spinkingdom.ui.screens.MainScreen
import de.dgstudios.spinkingdom.ui.screens.MissionsScreen
import de.dgstudios.spinkingdom.ui.screens.OverlayHost
import de.dgstudios.spinkingdom.ui.screens.PetsScreen
import de.dgstudios.spinkingdom.ui.screens.ProfileScreen
import de.dgstudios.spinkingdom.ui.screens.RaidScreen
import de.dgstudios.spinkingdom.ui.screens.SettingsScreen
import de.dgstudios.spinkingdom.ui.screens.ShopScreen
import de.dgstudios.spinkingdom.ui.screens.SplashScreen
import de.dgstudios.spinkingdom.ui.screens.WheelScreen
import de.dgstudios.spinkingdom.ui.theme.SK
import kotlinx.coroutines.delay

object Routes {
    const val SPLASH = "splash"; const val INTRO = "intro"; const val VILLAGE = "village"
    const val ATTACK = "attack"; const val RAID = "raid"; const val PETS = "pets"; const val CARDS = "cards"
    const val DAILY = "daily"; const val WHEEL = "wheel"; const val MISSIONS = "missions"; const val LEADERBOARD = "leaderboard"
    const val PROFILE = "profile"; const val SHOP = "shop"; const val SETTINGS = "settings"; const val DEBUG = "debug"
}

@Composable
fun SpinKingdomRoot(vm: GameViewModel, nav: NavHostController = rememberNavController(), startRoute: String = Routes.SPLASH) {
    val state by vm.state.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val anim by vm.spinAnim.collectAsStateWithLifecycle()
    val overlays by vm.overlays.collectAsStateWithLifecycle()
    val mods by vm.mods.collectAsStateWithLifecycle()
    val tampered by vm.clockTampered.collectAsStateWithLifecycle()
    val ad by vm.ads.overlay.collectAsStateWithLifecycle()
    val attackTarget by vm.attackTarget.collectAsStateWithLifecycle()
    val raid by vm.raid.collectAsStateWithLifecycle()
    val board by vm.leaderboard.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val time by rememberGameTime()
    var toast by remember { mutableStateOf<Int?>(null) }
    val today = remember(now) { vm.todayEpochDay() }

    LaunchedEffect(Unit) {
        vm.nav.collect { route ->
            when (route) {
                Routes.INTRO -> nav.navigate(Routes.INTRO) { popUpTo(0) }
                else -> if (nav.currentDestination?.route == Routes.VILLAGE) nav.navigate(route)
            }
        }
    }
    LaunchedEffect(Unit) { vm.toast.collect { toast = it; delay(2200); toast = null } }

    val back: () -> Unit = { vm.sfx(de.dgstudios.spinkingdom.audio.Sfx.CLICK); if (!nav.popBackStack(Routes.VILLAGE, false)) nav.navigate(Routes.VILLAGE) }

    Box(Modifier.fillMaxSize().background(SK.Night)) {
        NavHost(nav, startDestination = startRoute, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
            composable(Routes.SPLASH) {
                SplashScreen(loaded, time) {
                    nav.navigate(if (state.introDone) Routes.VILLAGE else Routes.INTRO) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            }
            composable(Routes.INTRO) {
                IntroScreen(time) { name, avatar ->
                    vm.completeIntro(name, avatar)
                    nav.navigate(Routes.VILLAGE) { popUpTo(0) }
                }
            }
            composable(Routes.VILLAGE) {
                MainScreen(state, now, anim, mods, time, today, MainActions(
                    onSpin = { vm.spin() },
                    onReelsStopped = { vm.onReelsStopped() },
                    onMultiplier = { vm.selectMultiplier(it) },
                    onUpgrade = { vm.upgrade(it) },
                    onNavigate = { r -> vm.sfx(de.dgstudios.spinkingdom.audio.Sfx.CLICK); nav.navigate(r) { launchSingleTop = true } },
                    onWatchAd = { vm.watchAdForSpins() },
                    sfx = { vm.sfx(it) }
                ))
            }
            composable(Routes.ATTACK) {
                LaunchedEffect(attackTarget, state.pendingActions) { if (attackTarget == null) vm.prepareAttack() }
                AttackScreen(state, attackTarget, time, onAttack = { vm.attack(it) }, onDone = { vm.finishAttack() }, onBack = back, sfx = { vm.sfx(it) }, vibrate = { vm.vibrate(it) })
            }
            composable(Routes.RAID) {
                LaunchedEffect(Unit) { vm.prepareRaid() }
                RaidScreen(raid, state, time, prizeValue = { k, m -> vm.raidPrizeValue(k, m) }, onDig = { vm.dig(it) }, onDone = { vm.finishRaid() }, onBack = back)
            }
            composable(Routes.PETS) { PetsScreen(state, time, onFeed = { vm.feedPet(it) }, onActivate = { vm.setActivePet(it) }, onBack = back) }
            composable(Routes.CARDS) { CardsScreen(state, time, onClaim = { vm.claimSet(it) }, onBack = back) }
            composable(Routes.DAILY) { DailyBonusScreen(state, today, now, tampered, onClaim = { vm.claimDaily() }, onBack = back) }
            composable(Routes.WHEEL) { WheelScreen(state, today, now, tampered, time, onSpin = { vm.spinWheel() }, onResult = { vm.showEffects(it) }, sfx = { vm.sfx(it) }, onBack = back) }
            composable(Routes.MISSIONS) { MissionsScreen(state, now, onClaim = { vm.claimMission(it) }, onBack = back) }
            composable(Routes.LEADERBOARD) {
                LaunchedEffect(Unit) { vm.loadLeaderboard() }
                LeaderboardScreen(board, onBack = back)
            }
            composable(Routes.PROFILE) { ProfileScreen(state, onSave = { n, a -> vm.updateProfile(n, a) }, onBack = back) }
            composable(Routes.SHOP) { ShopScreen(state, vm.billing.products(), busy, onBuy = { vm.buy(it) }, onWatchAd = { vm.watchAdForSpins() }, onBack = back) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    state,
                    onUpdate = { vm.updateSettings(it) },
                    onLanguage = { code ->
                        vm.updateSettings { it.copy(language = code) }
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
                    },
                    onReset = { vm.resetGame() },
                    onDebug = { nav.navigate(Routes.DEBUG) },
                    onBack = back
                )
            }
            composable(Routes.DEBUG) {
                DebugScreen(state, vm.allEvents(), onAction = { vm.debug(it) }, onCompleteLevel = { vm.debugCompleteLevel() }, onChest = { vm.debugChest(it) },
                    onForceEvent = { vm.debugForceEvent(it) }, onReset = { vm.resetGame() }, onBack = { nav.popBackStack() },
                    onLuckyBoost = { vm.debugLuckyBoost() }, onSpinJackpot = { vm.debugSpinJackpot() })
            }
        }
        overlays.firstOrNull()?.let { o ->
            OverlayHost(o, state, time, onDismiss = { vm.sfx(de.dgstudios.spinkingdom.audio.Sfx.CLICK); vm.dismissOverlay() }, onDouble = { vm.doubleLastWin() }, sfx = { vm.sfx(it) })
        }
        ad?.let { AdOverlay(it, time) { vm.ads.closeOverlay() } }
        toast?.let {
            Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 90.dp), contentAlignment = Alignment.TopCenter) {
                GameText(stringResource(it), Modifier.background(Color(0xE6A8231F), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 8.dp).testTag("toast"), size = 15.sp)
            }
        }
    }
}
