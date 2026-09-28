package de.danielgrebe.spinkingdom.ui.screens

import de.danielgrebe.spinkingdom.domain.LuckyBoost

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.danielgrebe.spinkingdom.R
import de.danielgrebe.spinkingdom.audio.Sfx
import de.danielgrebe.spinkingdom.config.EventModifiers
import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.config.LevelConfig
import de.danielgrebe.spinkingdom.domain.Buildings
import de.danielgrebe.spinkingdom.domain.OutcomeType
import de.danielgrebe.spinkingdom.domain.SpinRegen
import de.danielgrebe.spinkingdom.game.GameEngine
import de.danielgrebe.spinkingdom.models.GameState
import de.danielgrebe.spinkingdom.ui.SpinAnim
import de.danielgrebe.spinkingdom.ui.components.Badge
import de.danielgrebe.spinkingdom.ui.components.GameButton
import de.danielgrebe.spinkingdom.ui.components.GameDialog
import de.danielgrebe.spinkingdom.ui.components.GameText
import de.danielgrebe.spinkingdom.ui.components.Panel
import de.danielgrebe.spinkingdom.ui.components.ProgressBar
import de.danielgrebe.spinkingdom.ui.components.ResourcePill
import de.danielgrebe.spinkingdom.ui.components.RoundIconButton
import de.danielgrebe.spinkingdom.ui.components.formatNumber
import de.danielgrebe.spinkingdom.ui.components.formatTimer
import de.danielgrebe.spinkingdom.ui.draw.Avatar
import de.danielgrebe.spinkingdom.ui.draw.BuildingColors
import de.danielgrebe.spinkingdom.ui.draw.CoinIcon
import de.danielgrebe.spinkingdom.ui.draw.EnergyIcon
import de.danielgrebe.spinkingdom.ui.draw.MenuIcon
import de.danielgrebe.spinkingdom.ui.draw.MenuIconView
import de.danielgrebe.spinkingdom.ui.draw.ShieldIcon
import de.danielgrebe.spinkingdom.ui.draw.StarIcon
import de.danielgrebe.spinkingdom.ui.draw.drawBuilding
import de.danielgrebe.spinkingdom.ui.effects.CoinFly
import de.danielgrebe.spinkingdom.ui.effects.CoinRain
import de.danielgrebe.spinkingdom.ui.effects.StarBurst
import de.danielgrebe.spinkingdom.ui.theme.SK
import kotlinx.coroutines.launch

@Composable
fun rememberAnimatedLong(target: Long): Long {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val p = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target != to) {
            from = from + ((to - from) * p.value).toLong()
            to = target
            p.snapTo(0f)
            p.animateTo(1f, tween(900))
        }
    }
    return from + ((to - from) * p.value).toLong()
}

data class MainActions(
    val onSpin: () -> Unit,
    val onReelsStopped: () -> Unit,
    val onMultiplier: (Int) -> Unit,
    val onUpgrade: (Int) -> Unit,
    val onNavigate: (String) -> Unit,
    val onWatchAd: () -> Unit,
    val sfx: (Sfx) -> Unit
)

@Composable
fun MainScreen(
    state: GameState,
    now: Long,
    anim: SpinAnim?,
    mods: EventModifiers,
    t: Float,
    today: Long,
    actions: MainActions
) {
    val def = remember(state.level) { LevelConfig.level(state.level) }
    var lastWin by remember { mutableStateOf<SpinAnim?>(null) }
    var showBuild by remember { mutableStateOf(false) }
    var focusBuilding by remember { mutableStateOf<Int?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var coinTarget by remember { mutableStateOf(Offset.Zero) }
    var reelCenter by remember { mutableStateOf(Offset.Zero) }
    var villageOrigin by remember { mutableStateOf(Offset.Zero) }
    var villageSize by remember { mutableStateOf(IntSize.Zero) }
    var flyKey by remember { mutableStateOf<Long?>(null) }
    var rainKey by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    // building upgrade bounce + star burst
    val bounces = remember { List(5) { Animatable(1f) } }
    var burst by remember { mutableStateOf<Pair<Int, Long>?>(null) }
    var prevStages by remember { mutableStateOf(state.buildings.map { it.stage }) }
    var prevLevel by remember { mutableIntStateOf(state.level) }
    LaunchedEffect(state.buildings, state.level) {
        val stages = state.buildings.map { it.stage }
        if (state.level == prevLevel) {
            stages.forEachIndexed { i, s ->
                if (s > prevStages.getOrElse(i) { 0 }) {
                    burst = i to System.nanoTime()
                    scope.launch { bounces[i].snapTo(0.6f); bounces[i].animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f)) }
                }
            }
        }
        prevStages = stages; prevLevel = state.level
    }

    Box(Modifier.fillMaxSize().background(SK.Night)) {
        Column(Modifier.fillMaxSize()) {
            // ---------------- village area with top bar
            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .onGloballyPositioned { villageOrigin = it.positionInRoot(); villageSize = it.size }
            ) {
                VillageCanvas(
                    def, state.buildings.map { it.stage }, state.buildings.map { it.damaged }, t,
                    bounce = { bounces[it].value },
                    onTap = { i -> actions.sfx(Sfx.CLICK); focusBuilding = i; showBuild = true }
                )
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    TopBar(state, onProfile = { actions.onNavigate("profile") }, onShop = { actions.onNavigate("shop") }, onMenu = { showMenu = true }, onCoinPos = { coinTarget = it })
                    LevelHeader(state, def, mods)
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        Column(Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SideButton(MenuIcon.GIFT, stringResource(R.string.menu_daily), badge = GameEngine.canClaimDaily(state, today)) { actions.onNavigate("daily") }
                            SideButton(MenuIcon.WHEEL, stringResource(R.string.menu_wheel), badge = GameEngine.canSpinWheel(state, today)) { actions.onNavigate("wheel") }
                            SideButton(MenuIcon.SCROLL, stringResource(R.string.menu_missions), badge = state.missions.any { it.completed && !it.claimed }) { actions.onNavigate("missions") }
                        }
                        Column(Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SideButton(MenuIcon.CART, stringResource(R.string.menu_shop)) { actions.onNavigate("shop") }
                            SideButton(MenuIcon.TV, stringResource(R.string.ad_short), testTag = "ad_button") { actions.onWatchAd() }
                            SideButton(MenuIcon.TROPHY, stringResource(R.string.menu_leaderboard)) { actions.onNavigate("leaderboard") }
                        }
                        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SideButton(MenuIcon.CARDS, stringResource(R.string.menu_cards), locked = state.level < GameBalanceConfig.CARDS_UNLOCK_LEVEL) { actions.onNavigate("cards") }
                            GameButton(stringResource(R.string.build), { actions.sfx(Sfx.CLICK); focusBuilding = null; showBuild = true }, color = SK.Blue, height = 50.dp, modifier = Modifier.testTag("build_button"),
                                leading = { MenuIconView(MenuIcon.HAMMER, 26.dp) })
                            SideButton(MenuIcon.PAW, stringResource(R.string.menu_pets), locked = state.pets.isEmpty()) { actions.onNavigate("pets") }
                        }
                    }
                }
                val pending = state.pendingActions.firstOrNull()
                if (pending != null && anim == null) {
                    val pulse = rememberInfiniteTransition(label = "pend").animateFloat(0.95f, 1.06f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "pp")
                    GameButton(
                        stringResource(if (pending.kind == "attack") R.string.attack_ready else R.string.raid_ready),
                        { actions.onNavigate(pending.kind) }, color = if (pending.kind == "attack") SK.Red else SK.Orange,
                        modifier = Modifier.align(Alignment.Center).scale(pulse.value), height = 50.dp,
                        leading = { MenuIconView(if (pending.kind == "attack") MenuIcon.HAMMER else MenuIcon.SWORD, 26.dp) }
                    )
                }
                burst?.let { (i, key) ->
                    val w = villageSize.width.toFloat(); val h = villageSize.height.toFloat()
                    val a = buildingAnchor(i, w, h); val s = buildingSize(w, h)
                    StarBurst(key, Offset(a.x, a.y - s * 0.5f), s * 0.7f)
                }
            }
            // ---------------- slot machine area
            SlotPanel(state, now, anim, lastWin, t, actions, onStopped = {
                lastWin = anim
                if (anim != null && anim.coins > 0) flyKey = anim.id
                if (anim != null && (anim.outcome == OutcomeType.COIN_TRIPLE || anim.outcome == OutcomeType.JACKPOT)) rainKey = anim.id
                actions.onReelsStopped()
            }, onReelCenter = { reelCenter = it })
        }
        flyKey?.let { CoinFly(it, reelCenter, coinTarget) }
        rainKey?.let { CoinRain(it) }

        if (showBuild) BuildSheet(state, focusBuilding, onUpgrade = actions.onUpgrade, onClose = { showBuild = false }, t = t)
        if (showMenu) MenuSheet(onNavigate = { showMenu = false; actions.onNavigate(it) }, onClose = { showMenu = false }, state = state)
    }
}

@Composable
private fun TopBar(state: GameState, onProfile: () -> Unit, onShop: () -> Unit, onMenu: () -> Unit, onCoinPos: (Offset) -> Unit) {
    val coins = rememberAnimatedLong(state.coins)
    val spins = rememberAnimatedLong(state.spins.toLong())
    Row(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xCC160E3D), Color(0x00160E3D)))).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.clickable(onClick = onProfile).testTag("profile_button")) {
            Avatar(state.avatar, 46.dp)
            Box(Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(22.dp).clip(CircleShape).background(SK.Gold).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                GameText("${state.level}", size = 11.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ResourcePill({ CoinIcon(24.dp, Modifier.onGloballyPositioned { onCoinPos(it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)) }) }, formatNumber(coins), Modifier.weight(1.3f).testTag("coins"), onClick = onShop)
                ResourcePill({ EnergyIcon(24.dp) }, "$spins", Modifier.weight(1f).testTag("spins"), onClick = onShop)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                val max = GameBalanceConfig.maxShields(state.level)
                for (i in 0 until max) ShieldIcon(20.dp, filled = i < state.shields)
                Spacer(Modifier.width(8.dp))
                StarIcon(18.dp)
                GameText(" ${state.stars}", size = 13.sp)
            }
        }
        Spacer(Modifier.width(6.dp))
        RoundIconButton(onClick = onMenu, contentDescription = stringResource(R.string.menu), modifier = Modifier.testTag("menu_button")) { MenuIconView(MenuIcon.MENU, 24.dp) }
    }
}

@Composable
private fun LevelHeader(state: GameState, def: de.danielgrebe.spinkingdom.config.LevelDefinition, mods: EventModifiers) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        GameText(stringResource(R.string.level_title, state.level, levelName(def)), size = 16.sp, align = TextAlign.Center, maxLines = 1)
        ProgressBar(Buildings.progress(state), Modifier.fillMaxWidth().padding(top = 2.dp), color = SK.Gold, height = 12.dp)
        if (mods.active.isNotEmpty()) {
            val e = mods.active.first()
            Box(Modifier.padding(top = 4.dp).clip(RoundedCornerShape(10.dp)).background(Brush.horizontalGradient(listOf(SK.Purple, SK.Red))).padding(horizontal = 10.dp, vertical = 2.dp)) {
                GameText(eventName(e.id) + eventBonusText(mods), size = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun eventName(id: String): String = stringResource(
    when (id) {
        "pirates" -> R.string.event_pirates
        "halloween" -> R.string.event_halloween
        "christmas" -> R.string.event_christmas
        "summer" -> R.string.event_summer
        "treasure_hunt" -> R.string.event_treasure
        "double_coins" -> R.string.event_double_coins
        "bonus_spins" -> R.string.event_bonus_spins
        else -> R.string.event_generic
    }
)

@Composable
fun eventBonusText(m: EventModifiers): String {
    val parts = mutableListOf<String>()
    if (m.coin != 1.0) parts += stringResource(R.string.event_bonus_coins, fmtMult(m.coin))
    if (m.spins != 1.0) parts += stringResource(R.string.event_bonus_spins_x, fmtMult(m.spins))
    if (m.raid != 1.0) parts += stringResource(R.string.event_bonus_raid, fmtMult(m.raid))
    if (m.attack != 1.0) parts += stringResource(R.string.event_bonus_attack, fmtMult(m.attack))
    if (m.chestUpgrade > 0) parts += stringResource(R.string.event_bonus_chest)
    return if (parts.isEmpty()) "" else " · " + parts.joinToString(" · ")
}

private fun fmtMult(d: Double) = if (d == kotlin.math.floor(d)) "x${d.toInt()}" else "x" + String.format(java.util.Locale.ROOT, "%.1f", d).replace(".0", "")

@Composable
fun SideButton(icon: MenuIcon, label: String, badge: Boolean = false, locked: Boolean = false, testTag: String? = null, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(62.dp).then(if (testTag != null) Modifier.testTag(testTag) else Modifier)) {
        Box {
            RoundIconButton(onClick = onClick, contentDescription = label, size = 48.dp, color = if (locked) Color(0xFF4A4460) else SK.RoyalLight) {
                MenuIconView(icon, 30.dp, Modifier.then(if (locked) Modifier.scale(0.85f) else Modifier))
            }
            if (badge) Box(Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(SK.Red).border(2.dp, Color.White, CircleShape))
        }
        GameText(label, size = 11.sp, maxLines = 1, align = TextAlign.Center)
    }
}

@Composable
private fun SlotPanel(state: GameState, now: Long, anim: SpinAnim?, lastWin: SpinAnim?, t: Float, actions: MainActions, onStopped: () -> Unit, onReelCenter: (Offset) -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(SK.RoyalLight, SK.Royal, SK.RoyalDark)))
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SlotReels(anim, lastWin, t, onStopped, actions.sfx, Modifier.fillMaxWidth().height(150.dp).testTag("slot_machine"), onReelCenter)
        Spacer(Modifier.height(8.dp))
        val options = LuckyBoost.multipliers(state, now)
        val boostLeft = if (LuckyBoost.isActive(state, now)) LuckyBoost.millisLeft(state, now) else null
        val boosted = if (boostLeft != null) GameBalanceConfig.LUCKY_BOOST_MULTIPLIERS.filter { it !in GameBalanceConfig.availableMultipliers(state.level) }.toSet() else emptySet()
        if (boostLeft != null) LuckyBoostBadge(boostLeft, t)
        MultiplierSelector(options, state.selectedMultiplier, state.spins, anim == null, actions.onMultiplier, boosted = boosted, t = t)
        Spacer(Modifier.height(8.dp))
        val mult = GameEngine.effectiveMultiplier(state, now)
        SpinButton(state.spins > 0, anim != null, mult, actions.onSpin, Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        val next = SpinRegen.millisUntilNext(state, now)
        GameText(
            if (next == null) stringResource(R.string.spins_full) else stringResource(R.string.next_spin_in, GameBalanceConfig.SPIN_REGEN_AMOUNT, formatTimer(next)),
            size = 13.sp, color = SK.GoldLight, modifier = Modifier.testTag("spin_timer")
        )
    }
}

@Composable
private fun BuildSheet(state: GameState, focus: Int?, onUpgrade: (Int) -> Unit, onClose: () -> Unit, t: Float) {
    val def = LevelConfig.level(state.level)
    val names = buildingNames(def)
    GameDialog(onDismiss = onClose) {
        Panel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameText(stringResource(R.string.build_title), Modifier.weight(1f), size = 22.sp)
                RoundIconButton(onClick = onClose, contentDescription = stringResource(R.string.close), size = 36.dp, color = SK.Red) { GameText("✕", size = 16.sp) }
            }
            GameText(levelName(def), size = 14.sp, color = SK.GoldLight)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.height(430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.buildings.indices.toList()) { i ->
                    val b = state.buildings[i]
                    val cost = Buildings.nextCost(state, i)
                    val focused = focus == i
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (focused) SK.PanelLight else Color(0x33000000))
                            .border(if (focused) 2.dp else 0.dp, if (focused) SK.Gold else Color.Transparent, RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(Modifier.size(76.dp)) {
                            drawBuilding(def.archetypes[i], b.stage, BuildingColors.of(def.palette, i), size.width / 2, size.height * 0.92f, size.width * 0.8f, b.damaged, t)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            GameText(names.getOrElse(i) { "#${i + 1}" }, size = 16.sp, maxLines = 1)
                            Row { for (s in 1..GameBalanceConfig.MAX_STAGE) StarIcon(16.dp, Modifier.alpha(if (s <= b.stage) 1f else 0.25f)) }
                            GameText(if (b.damaged) stringResource(R.string.damaged) else stageName(b.stage), size = 12.sp, color = if (b.damaged) SK.Red else Color.White.copy(alpha = 0.8f))
                        }
                        if (cost != null) {
                            GameButton(
                                text = formatNumber(cost),
                                onClick = { onUpgrade(i) },
                                enabled = state.coins >= cost,
                                color = if (b.damaged) SK.Orange else SK.Green,
                                height = 46.dp, textSize = 14.sp,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.testTag("upgrade_$i"),
                                leading = { CoinIcon(18.dp) }
                            )
                        } else {
                            Badge(stringResource(R.string.max), color = SK.GoldDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuSheet(onNavigate: (String) -> Unit, onClose: () -> Unit, state: GameState) {
    GameDialog(onDismiss = onClose) {
        Panel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameText(stringResource(R.string.menu), Modifier.weight(1f), size = 22.sp)
                RoundIconButton(onClick = onClose, contentDescription = stringResource(R.string.close), size = 36.dp, color = SK.Red) { GameText("✕", size = 16.sp) }
            }
            Spacer(Modifier.height(10.dp))
            val entries = listOf(
                Triple(MenuIcon.PERSON, R.string.menu_profile, "profile"),
                Triple(MenuIcon.CARDS, R.string.menu_cards, "cards"),
                Triple(MenuIcon.PAW, R.string.menu_pets, "pets"),
                Triple(MenuIcon.SCROLL, R.string.menu_missions, "missions"),
                Triple(MenuIcon.TROPHY, R.string.menu_leaderboard, "leaderboard"),
                Triple(MenuIcon.CART, R.string.menu_shop, "shop"),
                Triple(MenuIcon.GIFT, R.string.menu_daily, "daily"),
                Triple(MenuIcon.WHEEL, R.string.menu_wheel, "wheel"),
                Triple(MenuIcon.GEAR, R.string.menu_settings, "settings")
            )
            entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { (icon, label, route) ->
                        Column(
                            Modifier.width(90.dp).clip(RoundedCornerShape(14.dp)).clickable { onNavigate(route) }.padding(6.dp).testTag("menu_$route"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(SK.RoyalLight, SK.Royal))).border(2.dp, SK.Gold, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                MenuIconView(icon, 34.dp)
                            }
                            GameText(stringResource(label), size = 12.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
