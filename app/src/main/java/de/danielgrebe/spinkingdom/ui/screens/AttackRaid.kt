package de.danielgrebe.spinkingdom.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.danielgrebe.spinkingdom.R
import de.danielgrebe.spinkingdom.audio.Sfx
import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.config.LevelConfig
import de.danielgrebe.spinkingdom.domain.Opponent
import de.danielgrebe.spinkingdom.game.GameEffect
import de.danielgrebe.spinkingdom.game.RaidPrizeKind
import de.danielgrebe.spinkingdom.models.GameState
import de.danielgrebe.spinkingdom.ui.RaidSession
import de.danielgrebe.spinkingdom.ui.components.GameButton
import de.danielgrebe.spinkingdom.ui.components.GameText
import de.danielgrebe.spinkingdom.ui.components.Panel
import de.danielgrebe.spinkingdom.ui.components.ScreenScaffold
import de.danielgrebe.spinkingdom.ui.components.formatNumber
import de.danielgrebe.spinkingdom.ui.draw.Avatar
import de.danielgrebe.spinkingdom.ui.draw.CoinIcon
import de.danielgrebe.spinkingdom.ui.draw.EnergyIcon
import de.danielgrebe.spinkingdom.ui.draw.drawChestIcon
import de.danielgrebe.spinkingdom.ui.draw.drawCoin
import de.danielgrebe.spinkingdom.ui.draw.drawEnergy
import de.danielgrebe.spinkingdom.ui.draw.drawShieldIcon
import de.danielgrebe.spinkingdom.ui.draw.drawShovel
import de.danielgrebe.spinkingdom.ui.draw.drawScenery
import de.danielgrebe.spinkingdom.ui.effects.Explosion
import de.danielgrebe.spinkingdom.ui.theme.SK
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
private fun OpponentHeader(o: Opponent, subtitle: String, mult: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(o.avatar, 54.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            GameText(o.name, size = 20.sp)
            GameText(stringResource(R.string.level_n, o.level) + " · " + subtitle, size = 13.sp, color = SK.GoldLight)
        }
        if (mult > 1) Box(Modifier.clip(RoundedCornerShape(10.dp)).background(SK.Orange).padding(horizontal = 10.dp, vertical = 4.dp)) { GameText("x$mult", size = 18.sp) }
    }
}

@Composable
fun AttackScreen(state: GameState, target: Opponent?, t: Float, onAttack: (Int) -> GameEffect.AttackDone?, onDone: () -> Unit, onBack: () -> Unit, sfx: (Sfx) -> Unit, vibrate: (Long) -> Unit) {
    val mult = state.pendingActions.firstOrNull { it.kind == "attack" }?.multiplier ?: 1
    var result by remember { mutableStateOf<GameEffect.AttackDone?>(null) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }
    var phase by remember { mutableStateOf(0) } // 0 choose, 1 flying, 2 impact, 3 result
    val ball = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val dome = remember { Animatable(0f) }
    var areaSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()

    ScreenScaffold(stringResource(R.string.attack_title), onBack = { if (phase == 0 || phase == 3) onBack() }) {
        if (target == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (state.pendingActions.none { it.kind == "attack" } && result == null) {
                    GameText(stringResource(R.string.no_attack_pending), size = 18.sp, align = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    GameButton(stringResource(R.string.back_to_village), onBack)
                } else GameText(stringResource(R.string.searching_opponent), size = 18.sp)
            }
            return@ScreenScaffold
        }
        val def = remember(target.id) { LevelConfig.level(target.level) }
        Column(Modifier.fillMaxSize()) {
            OpponentHeader(target, levelName(def), mult)
            Spacer(Modifier.height(6.dp))
            GameText(
                when (phase) { 0 -> stringResource(R.string.choose_building); 3 -> ""; else -> stringResource(R.string.firing) },
                Modifier.fillMaxWidth(), size = 16.sp, align = TextAlign.Center
            )
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(8.dp).clip(RoundedCornerShape(20.dp)).border(3.dp, SK.Gold, RoundedCornerShape(20.dp))
                    .onSizeChanged { areaSize = it }
                    .offset { IntOffset((sin(shake.value * 40f) * 18f * (1f - shake.value)).toInt(), 0) }
            ) {
                val damagedIdx = if (phase >= 2 && result?.blocked == false) targetIndex else null
                VillageCanvas(
                    def, target.stages, List(5) { it == damagedIdx }, t,
                    highlight = if (phase == 0) null else targetIndex,
                    shieldDome = dome.value,
                    onTap = if (phase == 0) { i ->
                        targetIndex = i
                        phase = 1
                        sfx(Sfx.ATTACK); vibrate(60)
                        scope.launch {
                            ball.snapTo(0f)
                            ball.animateTo(1f, tween(800, easing = LinearEasing))
                            val r = onAttack(i)
                            result = r
                            phase = 2
                            if (r?.blocked == true) {
                                sfx(Sfx.SHIELD); vibrate(80)
                                dome.animateTo(1f, spring(dampingRatio = 0.4f))
                            } else {
                                sfx(Sfx.EXPLOSION); vibrate(250)
                                shake.snapTo(0f); shake.animateTo(1f, tween(500))
                            }
                            delay(700)
                            phase = 3
                            sfx(Sfx.COIN)
                        }
                    } else null
                )
                // cannon + ball
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val cannon = Offset(w * 0.5f, h * 1.02f)
                    val ti = targetIndex
                    if (phase == 0) {
                        // hint crosshairs over buildings
                        for (i in 0 until 5) {
                            val a = buildingAnchor(i, w, h); val s = buildingSize(w, h)
                            val c = Offset(a.x, a.y - s * 0.45f)
                            val pulse = 1f + 0.1f * sin(t * 4 + i)
                            drawCircle(SK.Red.copy(alpha = 0.8f), s * 0.22f * pulse, c, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                            drawLine(SK.Red, c - Offset(s * 0.3f * pulse, 0f), c + Offset(s * 0.3f * pulse, 0f), 3f)
                            drawLine(SK.Red, c - Offset(0f, s * 0.3f * pulse), c + Offset(0f, s * 0.3f * pulse), 3f)
                        }
                    }
                    if (ti != null && phase == 1) {
                        val a = buildingAnchor(ti, w, h); val s = buildingSize(w, h)
                        val target2 = Offset(a.x, a.y - s * 0.45f)
                        val p = ball.value
                        val pos = Offset(cannon.x + (target2.x - cannon.x) * p, cannon.y + (target2.y - cannon.y) * p - sin(p * Math.PI.toFloat()) * h * 0.45f)
                        drawCircle(Color(0x55000000), w * 0.03f, pos + Offset(4f, 6f))
                        drawCircle(Brush.radialGradient(listOf(Color(0xFF7A7A8A), Color(0xFF1A1A22)), pos - Offset(4f, 4f), w * 0.04f), w * 0.03f, pos)
                        for (k in 1..4) {
                            val pp = (p - k * 0.04f).coerceAtLeast(0f)
                            val tp = Offset(cannon.x + (target2.x - cannon.x) * pp, cannon.y + (target2.y - cannon.y) * pp - sin(pp * Math.PI.toFloat()) * h * 0.45f)
                            drawCircle(Color.White.copy(alpha = 0.3f - k * 0.06f), w * 0.02f, tp)
                        }
                    }
                    // cannon
                    val ang = if (ti != null) {
                        val a = buildingAnchor(ti, w, h)
                        Math.toDegrees(kotlin.math.atan2((a.x - cannon.x).toDouble(), (cannon.y - a.y).toDouble())).toFloat() * 0.6f
                    } else sin(t) * 15f
                    rotate(ang, cannon) {
                        drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF5A5A6A), Color(0xFF2A2A33)), cannon.x - w * 0.05f, cannon.x + w * 0.05f), Offset(cannon.x - w * 0.045f, cannon.y - h * 0.2f), Size(w * 0.09f, h * 0.2f), CornerRadius(w * 0.02f))
                    }
                    drawCircle(Color(0xFF6B4A2A), w * 0.07f, cannon)
                }
                val ti = targetIndex
                if (phase >= 2 && ti != null && result?.blocked == false && areaSize.width > 0) {
                    val w = areaSize.width.toFloat(); val h = areaSize.height.toFloat()
                    val a = buildingAnchor(ti, w, h); val s = buildingSize(w, h)
                    Explosion(result!!, Offset(a.x, a.y - s * 0.45f), s * 0.8f)
                }
            }
            if (phase == 3 && result != null) {
                val r = result!!
                Panel(Modifier.fillMaxWidth().padding(12.dp)) {
                    GameText(stringResource(if (r.blocked) R.string.attack_blocked else R.string.attack_success), Modifier.fillMaxWidth(), size = 24.sp, align = TextAlign.Center, color = if (r.blocked) Color(0xFF7FD3FF) else SK.GoldLight)
                    GameText(stringResource(if (r.blocked) R.string.attack_blocked_text else R.string.attack_success_text, r.opponent.name), Modifier.fillMaxWidth(), size = 14.sp, align = TextAlign.Center)
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CoinIcon(30.dp); Spacer(Modifier.width(6.dp)); GameText("+" + formatNumber(r.coins), size = 26.sp, color = SK.GoldLight)
                    }
                    val more = state.pendingActions.any { it.kind == "attack" }
                    GameButton(stringResource(if (more) R.string.next_attack else R.string.back_to_village), {
                        onDone()
                        result = null; targetIndex = null; phase = 0
                        scope.launch { dome.snapTo(0f); ball.snapTo(0f) }
                        if (!more) onBack()
                    }, Modifier.fillMaxWidth().testTag("attack_done"))
                }
            }
        }
    }
}

@Composable
fun RaidScreen(session: RaidSession?, state: GameState, t: Float, prizeValue: (RaidPrizeKind, Int) -> Pair<Long, Int>, onDig: (Int) -> Unit, onDone: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold(stringResource(R.string.raid_title), onBack = { if (session == null || session.finished || session.picks.isEmpty()) onBack() }) {
        if (session == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (state.pendingActions.none { it.kind == "raid" }) {
                    GameText(stringResource(R.string.no_raid_pending), size = 18.sp, align = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    GameButton(stringResource(R.string.back_to_village), onBack)
                } else GameText(stringResource(R.string.searching_opponent), size = 18.sp)
            }
            return@ScreenScaffold
        }
        val def = remember(session.opponent.id) { LevelConfig.level(session.opponent.level) }
        Column(Modifier.fillMaxSize()) {
            OpponentHeader(session.opponent, levelName(def), session.multiplier)
            GameText(
                if (session.finished) stringResource(R.string.raid_finished) else stringResource(R.string.raid_pick, GameBalanceConfig.RAID_PICKS - session.picks.size),
                Modifier.fillMaxWidth().padding(6.dp), size = 16.sp, align = TextAlign.Center
            )
            Box(Modifier.fillMaxWidth().weight(1f).padding(8.dp).clip(RoundedCornerShape(20.dp)).border(3.dp, SK.Gold, RoundedCornerShape(20.dp))) {
                Canvas(Modifier.fillMaxSize()) {
                    drawScenery(def.palette, def.theme.decoration, t, horizon = 0.22f)
                }
                BoxWithConstraints(Modifier.fillMaxSize().padding(top = 70.dp, start = 10.dp, end = 10.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
                    val cell = minOf(maxWidth / 3, maxHeight / 3)
                    Column {
                        for (row in 0 until 3) Row {
                            for (col in 0 until 3) {
                                val idx = row * 3 + col
                                DigSpot(idx, session, cell, t, prizeValue, onDig)
                            }
                        }
                    }
                }
            }
            if (session.finished) {
                Panel(Modifier.fillMaxWidth().padding(12.dp)) {
                    GameText(stringResource(R.string.raid_loot), Modifier.fillMaxWidth(), size = 22.sp, align = TextAlign.Center, color = SK.GoldLight)
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CoinIcon(28.dp); GameText(" +" + formatNumber(session.coins), size = 22.sp)
                        if (session.spins > 0) { Spacer(Modifier.width(14.dp)); EnergyIcon(28.dp); GameText(" +${session.spins}", size = 22.sp) }
                    }
                    GameButton(stringResource(R.string.collect), { onDone(); onBack() }, Modifier.fillMaxWidth().testTag("raid_done"))
                }
            }
        }
    }
}

@Composable
private fun DigSpot(idx: Int, s: RaidSession, cell: androidx.compose.ui.unit.Dp, t: Float, prizeValue: (RaidPrizeKind, Int) -> Pair<Long, Int>, onDig: (Int) -> Unit) {
    val picked = idx in s.picks
    val revealed = picked || s.finished
    val dig = remember { Animatable(0f) }
    LaunchedEffect(picked) { if (picked) { dig.snapTo(0f); dig.animateTo(1f, tween(700)) } }
    val kind = s.board[idx]
    Box(
        Modifier.size(cell).padding(4.dp).clickable(enabled = !s.finished && !picked) { onDig(idx) }.testTag("dig_$idx"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val c = Offset(w / 2, h * 0.62f)
            val p = dig.value
            val showPrize = revealed && (!picked || p > 0.6f)
            // hole or mound
            if (showPrize) {
                drawOval(Color(0xFF3A2410), Offset(w * 0.12f, h * 0.55f), Size(w * 0.76f, h * 0.3f))
                val alpha = if (picked) 1f else 0.45f
                val pop = if (picked) ((p - 0.6f) / 0.4f).coerceIn(0f, 1f) else 1f
                val pc = Offset(c.x, c.y - h * 0.15f * pop)
                when (kind) {
                    RaidPrizeKind.SMALL -> drawCoin(pc, w * 0.16f * pop.coerceAtLeast(0.3f))
                    RaidPrizeKind.LARGE -> { drawCoin(pc + Offset(-w * 0.12f, h * 0.04f), w * 0.13f); drawCoin(pc + Offset(w * 0.12f, h * 0.04f), w * 0.13f); drawCoin(pc - Offset(0f, h * 0.06f), w * 0.15f) }
                    RaidPrizeKind.JACKPOT -> { drawCircle(Color(0x88FFE680), w * 0.35f, pc); drawChestIcon(pc, w * 0.22f, lidOpen = 0.8f) }
                    RaidPrizeKind.SPINS -> drawEnergy(pc, w * 0.17f)
                }
                if (alpha < 1f) drawRect(Color.Black.copy(alpha = 0.45f), size = size)
            } else {
                drawOval(Color(0x44000000), Offset(w * 0.1f, h * 0.62f), Size(w * 0.8f, h * 0.22f))
                drawOval(Brush.verticalGradient(listOf(Color(0xFFB8864F), Color(0xFF7A5230))), Offset(w * 0.12f, h * 0.45f), Size(w * 0.76f, h * 0.35f))
                // X mark
                val bob = sin(t * 3 + idx) * h * 0.02f
                drawLine(SK.Red, Offset(w * 0.38f, h * 0.52f + bob), Offset(w * 0.62f, h * 0.68f + bob), w * 0.05f)
                drawLine(SK.Red, Offset(w * 0.62f, h * 0.52f + bob), Offset(w * 0.38f, h * 0.68f + bob), w * 0.05f)
            }
            if (picked && p < 1f) {
                // shovel digging twice + flying dirt
                val stroke = sin(p * Math.PI.toFloat() * 3f)
                drawShovel(Offset(w * 0.62f, h * 0.35f + stroke * h * 0.12f), w * 0.28f)
                for (k in 0 until 8) {
                    val a = k * 0.8f
                    val d = p * w * 0.45f
                    drawCircle(Color(0xFF7A5230).copy(alpha = 1f - p), w * 0.03f, Offset(c.x + kotlin.math.cos(a) * d, c.y - kotlin.math.abs(sin(a)) * d * 1.2f + p * p * h * 0.3f))
                }
            }
        }
        if (picked && dig.value >= 1f) {
            val (coins, spins) = prizeValue(kind, s.multiplier)
            GameText(if (coins > 0) "+" + formatNumber(coins) else "+$spins", Modifier.align(Alignment.TopCenter), size = 14.sp, color = SK.GoldLight)
        }
    }
}
