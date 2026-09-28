package de.dgstudios.spinkingdom.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dgstudios.spinkingdom.R
import de.dgstudios.spinkingdom.audio.Sfx
import de.dgstudios.spinkingdom.config.GameBalanceConfig
import de.dgstudios.spinkingdom.config.GameBalanceConfig.WheelPrizeKind
import de.dgstudios.spinkingdom.domain.LeaderboardEntry
import de.dgstudios.spinkingdom.game.GameEffect
import de.dgstudios.spinkingdom.game.GameEngine
import de.dgstudios.spinkingdom.models.GameState
import de.dgstudios.spinkingdom.models.MissionPeriod
import de.dgstudios.spinkingdom.models.MissionState
import de.dgstudios.spinkingdom.models.MissionType
import de.dgstudios.spinkingdom.ui.components.GameButton
import de.dgstudios.spinkingdom.ui.components.GameText
import de.dgstudios.spinkingdom.ui.components.Panel
import de.dgstudios.spinkingdom.ui.components.ProgressBar
import de.dgstudios.spinkingdom.ui.components.ScreenScaffold
import de.dgstudios.spinkingdom.ui.components.formatNumber
import de.dgstudios.spinkingdom.ui.components.formatTimer
import de.dgstudios.spinkingdom.ui.draw.Avatar
import de.dgstudios.spinkingdom.ui.draw.ChestIcon
import de.dgstudios.spinkingdom.ui.draw.CoinIcon
import de.dgstudios.spinkingdom.ui.draw.EnergyIcon
import de.dgstudios.spinkingdom.ui.draw.MenuIcon
import de.dgstudios.spinkingdom.ui.draw.MenuIconView
import de.dgstudios.spinkingdom.ui.draw.StarIcon
import de.dgstudios.spinkingdom.ui.draw.chestColors
import de.dgstudios.spinkingdom.ui.draw.darken
import de.dgstudios.spinkingdom.ui.draw.drawChestIcon
import de.dgstudios.spinkingdom.ui.draw.drawCoin
import de.dgstudios.spinkingdom.ui.draw.drawEnergy
import de.dgstudios.spinkingdom.ui.draw.drawJoker
import de.dgstudios.spinkingdom.ui.draw.lighten
import de.dgstudios.spinkingdom.ui.effects.Confetti
import de.dgstudios.spinkingdom.ui.theme.SK
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun millisUntilMidnight(now: Long): Long {
    val zone = ZoneId.systemDefault()
    val next = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return next - now
}

// =================================================================== DAILY BONUS
@Composable
fun DailyBonusScreen(state: GameState, today: Long, now: Long, tampered: Boolean, onClaim: () -> Unit, onBack: () -> Unit) {
    val idx = GameEngine.dailyIndexFor(state, today)
    val canClaim = GameEngine.canClaimDaily(state, today)
    // index of the tile representing "today": if already claimed today the streak pointer moved on
    val todayTile = if (canClaim) idx else (idx + 6) % 7
    ScreenScaffold(stringResource(R.string.daily_title), onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GameText(stringResource(R.string.daily_sub), size = 15.sp, align = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            val pulse = rememberInfiniteTransition(label = "d").animateFloat(1f, 1.07f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dp")
            (0 until 7).chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    row.forEach { d ->
                        val claimed = d < todayTile || (d == todayTile && !canClaim)
                        val isToday = d == todayTile && canClaim
                        val reward = GameBalanceConfig.DAILY_BONUS[d]
                        Column(
                            Modifier.width(if (d == 6) 120.dp else 78.dp).scale(if (isToday) pulse.value else 1f).clip(RoundedCornerShape(14.dp))
                                .background(Brush.verticalGradient(when { isToday -> listOf(SK.Gold, SK.GoldDark); claimed -> listOf(SK.GreenDark, Color(0xFF155A1A)); else -> listOf(SK.PanelLight, SK.Panel) }))
                                .border(2.dp, if (isToday) Color.White else Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            GameText(stringResource(R.string.day_n, d + 1), size = 13.sp)
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                when {
                                    reward.chest != null && reward.coinsU > 0 -> Row { CoinIcon(22.dp); EnergyIcon(22.dp) }
                                    reward.chest != null -> { val (b, tr) = chestColors(reward.chest); ChestIcon(40.dp, body = b, trim = tr) }
                                    reward.spins > 0 -> EnergyIcon(36.dp)
                                    else -> CoinIcon(36.dp)
                                }
                                if (claimed) Canvas(Modifier.size(40.dp)) {
                                    drawCircle(Color(0xAA000000), size.minDimension / 2)
                                    drawPath(Path().apply { moveTo(size.width * 0.25f, size.height * 0.5f); lineTo(size.width * 0.45f, size.height * 0.7f); lineTo(size.width * 0.78f, size.height * 0.3f) }, SK.Green, style = Stroke(6f))
                                }
                            }
                            GameText(
                                when {
                                    d == 6 -> stringResource(R.string.jackpot)
                                    reward.chest != null -> chestName(reward.chest)
                                    reward.spins > 0 -> "${reward.spins}"
                                    else -> formatNumber(GameBalanceConfig.coins(state.level, reward.coinsU))
                                }, size = 12.sp, maxLines = 1
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (tampered) GameText(stringResource(R.string.err_clock), size = 14.sp, color = SK.Red, align = TextAlign.Center)
            if (canClaim) GameButton(stringResource(R.string.claim), onClaim, Modifier.fillMaxWidth().testTag("daily_claim"), enabled = !tampered)
            else GameText(stringResource(R.string.come_back_in, formatTimer(millisUntilMidnight(now))), size = 16.sp, color = SK.GoldLight)
            Spacer(Modifier.height(10.dp))
            GameText(stringResource(R.string.daily_rule), size = 12.sp, color = Color.White.copy(alpha = 0.7f), align = TextAlign.Center)
        }
    }
}

// =================================================================== WHEEL
private val WHEEL_COLORS = listOf(SK.Red, SK.Blue, SK.Green, SK.Purple, SK.Orange, Color(0xFF2EC4B6), Color(0xFFFF5FA8), SK.Gold)

@Composable
fun WheelScreen(state: GameState, today: Long, now: Long, tampered: Boolean, t: Float, onSpin: () -> Pair<Int, List<GameEffect>>?, onResult: (List<GameEffect>) -> Unit, sfx: (Sfx) -> Unit, onBack: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var confettiKey by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val segs = GameBalanceConfig.WHEEL
    val n = segs.size
    val canSpin = GameEngine.canSpinWheel(state, today)
    ScreenScaffold(stringResource(R.string.menu_wheel), onBack = { if (!spinning) onBack() }) {
        Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GameText(stringResource(R.string.wheel_sub), size = 15.sp, align = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize(0.92f).rotate(rotation.value)) {
                    val r = size.minDimension / 2; val c = center
                    drawCircle(SK.GoldDark, r, c)
                    val sweep = 360f / n
                    for (i in 0 until n) {
                        // segment i is centered at angle -90 + i*sweep (top at rotation 0)
                        val start = -90f + i * sweep - sweep / 2
                        drawArc(Brush.radialGradient(listOf(WHEEL_COLORS[i].lighten(0.3f), WHEEL_COLORS[i].darken(0.1f)), c, r), start, sweep, true, Offset(c.x - r * 0.94f, c.y - r * 0.94f), Size(r * 1.88f, r * 1.88f))
                        rotate(i * sweep, c) {
                            val ic = Offset(c.x, c.y - r * 0.62f)
                            val s = segs[i]
                            when (s.kind) {
                                WheelPrizeKind.COINS -> drawCoin(ic, r * 0.11f)
                                WheelPrizeKind.SPINS -> drawEnergy(ic, r * 0.11f)
                                WheelPrizeKind.CHEST -> { val (b, tr) = chestColors(s.chest!!); drawChestIcon(ic, r * 0.1f, b, tr) }
                                WheelPrizeKind.TREATS -> { drawCircle(Color(0xFFF5D6A8), r * 0.08f, ic); drawCircle(Color(0xFFB98A4E), r * 0.08f, ic, style = Stroke(3f)) }
                                WheelPrizeKind.JACKPOT -> drawJoker(ic, r * 0.12f, t)
                            }
                        }
                        rotate(start + 90f, c) { drawLine(Color.White.copy(alpha = 0.7f), c, Offset(c.x, c.y - r * 0.94f), 4f) }
                    }
                    for (b in 0 until 24) rotate(b * 15f, c) { drawCircle(if ((b + (t * 4).toInt()) % 2 == 0) Color.White else SK.GoldLight, r * 0.025f, Offset(c.x, c.y - r * 0.97f)) }
                    drawCircle(Brush.radialGradient(listOf(SK.GoldLight, SK.GoldDark), c, r * 0.16f), r * 0.14f, c)
                }
                // labels overlay rotates with the wheel
                Box(Modifier.fillMaxSize(0.92f).rotate(rotation.value)) {
                    for (i in 0 until n) {
                        val s = segs[i]
                        val label = when (s.kind) {
                            WheelPrizeKind.COINS -> formatNumber(GameBalanceConfig.coins(state.level, s.coinsU))
                            WheelPrizeKind.SPINS -> "${s.spins}"
                            WheelPrizeKind.CHEST -> chestName(s.chest!!)
                            WheelPrizeKind.TREATS -> if (state.pets.isNotEmpty()) "${s.treats} " + stringResource(R.string.treats) else formatNumber(GameBalanceConfig.coins(state.level, s.coinsU))
                            WheelPrizeKind.JACKPOT -> stringResource(R.string.jackpot)
                        }
                        Box(Modifier.fillMaxSize().rotate(i * 360f / n), contentAlignment = Alignment.TopCenter) {
                            GameText(label, Modifier.padding(top = 72.dp), size = 11.sp, maxLines = 1)
                        }
                    }
                }
                // pointer
                Canvas(Modifier.size(44.dp).align(Alignment.TopCenter)) {
                    drawPath(Path().apply { moveTo(size.width * 0.15f, 0f); lineTo(size.width * 0.85f, 0f); lineTo(size.width / 2, size.height); close() }, SK.Red)
                    drawPath(Path().apply { moveTo(size.width * 0.15f, 0f); lineTo(size.width * 0.85f, 0f); lineTo(size.width / 2, size.height); close() }, Color.White, style = Stroke(4f))
                }
            }
            Spacer(Modifier.height(16.dp))
            if (tampered) GameText(stringResource(R.string.err_clock), size = 14.sp, color = SK.Red, align = TextAlign.Center)
            if (canSpin || spinning) {
                GameButton(stringResource(if (spinning) R.string.spinning else R.string.spin_free), {
                    val res = onSpin() ?: return@GameButton
                    spinning = true
                    val (seg, effects) = res
                    scope.launch {
                        val current = rotation.value % 360f
                        val target = rotation.value - current + 360f * 6 + (360f - seg * 360f / n)
                        var lastTick = -1
                        rotation.animateTo(target, tween(4200, easing = CubicBezierEasing(0.15f, 0.6f, 0.2f, 1f))) {
                            val tick = (value / (360f / n)).toInt()
                            if (tick != lastTick) { lastTick = tick; sfx(Sfx.TICK) }
                        }
                        spinning = false
                        if (segs[seg].kind == WheelPrizeKind.JACKPOT) confettiKey = System.nanoTime()
                        onResult(effects)
                    }
                }, Modifier.fillMaxWidth().testTag("wheel_spin"), enabled = !spinning && !tampered, color = SK.Red, textSize = 22.sp)
            } else {
                GameText(stringResource(R.string.next_free_spin, formatTimer(millisUntilMidnight(now))), size = 16.sp, color = SK.GoldLight)
            }
        }
        confettiKey?.let { Confetti(it) }
    }
}

// =================================================================== MISSIONS
@Composable
fun missionText(m: MissionState): String = when (m.type) {
    MissionType.SPINS -> stringResource(R.string.mission_spins, m.target)
    MissionType.ATTACKS -> stringResource(R.string.mission_attacks, m.target)
    MissionType.RAIDS -> stringResource(R.string.mission_raids, m.target)
    MissionType.UPGRADES -> stringResource(R.string.mission_upgrades, m.target)
    MissionType.COLLECT_COINS -> stringResource(R.string.mission_coins, formatNumber(m.target))
    MissionType.OPEN_CHESTS -> stringResource(R.string.mission_chests, m.target)
    MissionType.WIN_SHIELDS -> stringResource(R.string.mission_shields, m.target)
}

@Composable
fun MissionsScreen(state: GameState, now: Long, onClaim: (String) -> Unit, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    ScreenScaffold(stringResource(R.string.menu_missions), onBack) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.daily, R.string.weekly).forEachIndexed { i, res ->
                    GameButton(stringResource(res), { tab = i }, Modifier.weight(1f).testTag("mission_tab_$i"), color = if (tab == i) SK.Gold else SK.PanelLight, height = 44.dp, textSize = 16.sp)
                }
            }
            val period = if (tab == 0) MissionPeriod.DAILY else MissionPeriod.WEEKLY
            val reset = if (tab == 0) millisUntilMidnight(now) else {
                val zone = ZoneId.systemDefault()
                val d = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                d.plusDays((8 - d.dayOfWeek.value).toLong()).atStartOfDay(zone).toInstant().toEpochMilli() - now
            }
            GameText(stringResource(R.string.resets_in, formatTimer(reset)), Modifier.padding(vertical = 8.dp), size = 13.sp, color = SK.GoldLight)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.missions.filter { it.period == period }, key = { it.id }) { m ->
                    Panel(Modifier.fillMaxWidth(), padding = 10.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(SK.RoyalLight), contentAlignment = Alignment.Center) {
                                MenuIconView(when (m.type) { MissionType.SPINS -> MenuIcon.WHEEL; MissionType.ATTACKS -> MenuIcon.HAMMER; MissionType.RAIDS -> MenuIcon.SWORD; MissionType.UPGRADES -> MenuIcon.HAMMER; MissionType.COLLECT_COINS -> MenuIcon.CART; MissionType.OPEN_CHESTS -> MenuIcon.GIFT; MissionType.WIN_SHIELDS -> MenuIcon.TROPHY }, 30.dp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                GameText(missionText(m), size = 15.sp)
                                ProgressBar(m.progress / m.target.toFloat(), Modifier.fillMaxWidth().padding(vertical = 4.dp), label = "${formatNumber(m.progress)} / ${formatNumber(m.target)}", height = 16.dp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GameText(stringResource(R.string.reward) + ": ", size = 12.sp)
                                    if (m.rewardCoins > 0) { CoinIcon(18.dp); GameText(formatNumber(m.rewardCoins) + " ", size = 12.sp) }
                                    if (m.rewardSpins > 0) { EnergyIcon(18.dp); GameText("${m.rewardSpins} ", size = 12.sp) }
                                    m.rewardChest?.let { val (b, tr) = chestColors(it); ChestIcon(20.dp, body = b, trim = tr); GameText(chestName(it), size = 12.sp) }
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            GameButton(
                                stringResource(if (m.claimed) R.string.claimed else R.string.claim), { onClaim(m.id) },
                                enabled = m.completed && !m.claimed, height = 44.dp, textSize = 13.sp,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.testTag("claim_${m.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

// =================================================================== LEADERBOARD
@Composable
fun LeaderboardScreen(entries: List<LeaderboardEntry>, onBack: () -> Unit) {
    ScreenScaffold(stringResource(R.string.menu_leaderboard), onBack) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            GameText(stringResource(R.string.leaderboard_sub), size = 13.sp, color = SK.GoldLight)
            Spacer(Modifier.height(8.dp))
            val playerRank = entries.firstOrNull { it.isPlayer }?.rank
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                items(entries, key = { it.rank }) { e ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (e.isPlayer) Brush.horizontalGradient(listOf(SK.Gold, SK.GoldDark)) else Brush.horizontalGradient(listOf(SK.PanelLight, SK.Panel)))
                            .border(if (e.isPlayer) 2.dp else 0.dp, Color.White, RoundedCornerShape(14.dp)).padding(8.dp).then(if (e.isPlayer) Modifier.testTag("leaderboard_player") else Modifier),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(when (e.rank) { 1 -> SK.Gold; 2 -> Color(0xFFC0C8D2); 3 -> Color(0xFFCD7F32); else -> Color(0x33000000) }), contentAlignment = Alignment.Center) {
                            GameText("${e.rank}", size = 15.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Avatar(e.avatar, 40.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            GameText(e.name + if (e.isPlayer) " (" + stringResource(R.string.you) + ")" else "", size = 15.sp, maxLines = 1)
                            GameText(stringResource(R.string.level_n, e.level), size = 12.sp)
                        }
                        StarIcon(22.dp); GameText(" ${e.stars}", size = 16.sp)
                    }
                }
            }
            if (playerRank != null) GameText(stringResource(R.string.your_rank, playerRank), Modifier.padding(8.dp), size = 16.sp)
        }
    }
}
