package de.danielgrebe.spinkingdom.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.danielgrebe.spinkingdom.R
import de.danielgrebe.spinkingdom.audio.Sfx
import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.config.LevelConfig
import de.danielgrebe.spinkingdom.domain.AdOverlayState
import de.danielgrebe.spinkingdom.domain.OutcomeType
import de.danielgrebe.spinkingdom.models.ChestType
import de.danielgrebe.spinkingdom.models.GameState
import de.danielgrebe.spinkingdom.models.PetType
import de.danielgrebe.spinkingdom.ui.Overlay
import de.danielgrebe.spinkingdom.ui.components.GameButton
import de.danielgrebe.spinkingdom.ui.components.GameDialog
import de.danielgrebe.spinkingdom.ui.components.GameText
import de.danielgrebe.spinkingdom.ui.components.Panel
import de.danielgrebe.spinkingdom.ui.components.ProgressBar
import de.danielgrebe.spinkingdom.ui.components.RewardRow
import de.danielgrebe.spinkingdom.ui.components.formatNumber
import de.danielgrebe.spinkingdom.ui.draw.CardView
import de.danielgrebe.spinkingdom.ui.draw.CoinIcon
import de.danielgrebe.spinkingdom.ui.draw.EnergyIcon
import de.danielgrebe.spinkingdom.ui.draw.StarIcon
import de.danielgrebe.spinkingdom.ui.draw.TreatIcon
import de.danielgrebe.spinkingdom.ui.draw.chestColors
import de.danielgrebe.spinkingdom.ui.draw.drawChestIcon
import de.danielgrebe.spinkingdom.ui.draw.drawPet
import de.danielgrebe.spinkingdom.ui.draw.drawShieldIcon
import de.danielgrebe.spinkingdom.ui.draw.drawHammer
import de.danielgrebe.spinkingdom.ui.draw.starPath
import de.danielgrebe.spinkingdom.ui.effects.CoinRain
import de.danielgrebe.spinkingdom.ui.effects.Confetti
import de.danielgrebe.spinkingdom.ui.theme.SK
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import kotlin.math.sin

@Composable
fun chestName(type: ChestType): String = stringResource(
    when (type) {
        ChestType.WOOD -> R.string.chest_wood
        ChestType.SILVER -> R.string.chest_silver
        ChestType.GOLD -> R.string.chest_gold
        ChestType.ROYAL -> R.string.chest_royal
        ChestType.LEGENDARY -> R.string.chest_legendary
    }
)

@Composable
fun petName(type: PetType): String = stringResource(
    when (type) { PetType.FOX -> R.string.pet_fox; PetType.DRAGON -> R.string.pet_dragon; PetType.RACCOON -> R.string.pet_raccoon; PetType.PHOENIX -> R.string.pet_phoenix }
)

@Composable
private fun Rays(t: Float, color: Color = SK.GoldLight, modifier: Modifier = Modifier.size(320.dp)) {
    Canvas(modifier) {
        val c = center; val r = size.minDimension / 2
        for (i in 0 until 16) {
            val a = (i * 22.5f + t * 25f) * Math.PI.toFloat() / 180f
            val a2 = a + 0.12f
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(c.x, c.y); lineTo(c.x + kotlin.math.cos(a) * r, c.y + sin(a) * r); lineTo(c.x + kotlin.math.cos(a2) * r, c.y + sin(a2) * r); close()
            }
            drawPath(p, color.copy(alpha = 0.18f))
        }
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.5f), Color.Transparent), c, r * 0.6f), r * 0.6f, c)
    }
}

@Composable
fun OverlayHost(overlay: Overlay, state: GameState, t: Float, onDismiss: () -> Unit, onDouble: () -> Unit, sfx: (Sfx) -> Unit) {
    when (overlay) {
        is Overlay.BigWin -> BigWinOverlay(overlay, t, onDismiss, onDouble)
        is Overlay.Chest -> ChestOverlay(overlay, t, onDismiss, sfx)
        is Overlay.LevelComplete -> LevelCompleteOverlay(overlay, t, onDismiss)
        is Overlay.PetLevelUp -> SimpleCelebration(stringResource(R.string.pet_level_up), stringResource(R.string.pet_level_up_text, petName(overlay.pet), overlay.level), t, onDismiss) {
            Canvas(Modifier.size(140.dp)) { drawPet(overlay.pet, center, size.minDimension * 0.4f, t) }
        }
        is Overlay.SetComplete -> SimpleCelebration(stringResource(R.string.set_complete), stringResource(R.string.set_complete_text), t, onDismiss) {
            Row(horizontalArrangement = Arrangement.spacedBy((-30).dp)) {
                for (i in 0 until 3) CardView(overlay.set * GameBalanceConfig.CARDS_PER_SET + 6 + i, 1, t, Modifier.rotate((i - 1) * 12f), width = 90.dp)
            }
        }
        is Overlay.BotLog -> BotLogOverlay(overlay, onDismiss)
        is Overlay.Reward -> SimpleCelebration(stringResource(overlay.titleRes), null, t, onDismiss) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (overlay.coins > 0) RewardRow({ CoinIcon(30.dp) }, "+" + formatNumber(overlay.coins))
                if (overlay.spins > 0) RewardRow({ EnergyIcon(30.dp) }, "+${overlay.spins} " + stringResource(R.string.spins_short))
                if (overlay.coins == 0L && overlay.spins == 0) RewardRow({ StarIcon(30.dp) }, stringResource(R.string.reward_received))
            }
        }
        is Overlay.Message -> GameDialog(onDismiss) {
            Panel(Modifier.fillMaxWidth()) {
                GameText(stringResource(overlay.titleRes), Modifier.fillMaxWidth(), size = 22.sp, align = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                GameText(stringResource(overlay.textRes), Modifier.fillMaxWidth(), size = 15.sp, align = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                GameButton(stringResource(R.string.ok), onDismiss, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun popIn(key: Any): Float {
    val s = remember(key) { Animatable(0.3f) }
    LaunchedEffect(key) { s.animateTo(1f, tween(450, easing = CubicBezierEasing(0.3f, 1.7f, 0.5f, 1f))) }
    return s.value
}

@Composable
private fun SimpleCelebration(title: String, text: String?, t: Float, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val s = popIn(title + text)
    GameDialog(onDismiss) {
        Box(contentAlignment = Alignment.Center) {
            Rays(t)
            Panel(Modifier.fillMaxWidth().scale(s)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    GameText(title, size = 26.sp, color = SK.GoldLight, align = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    content()
                    if (text != null) { Spacer(Modifier.height(8.dp)); GameText(text, size = 15.sp, align = TextAlign.Center) }
                    Spacer(Modifier.height(14.dp))
                    GameButton(stringResource(R.string.great), onDismiss, Modifier.fillMaxWidth().testTag("overlay_ok"))
                }
            }
        }
    }
}

@Composable
private fun BigWinOverlay(o: Overlay.BigWin, t: Float, onDismiss: () -> Unit, onDouble: () -> Unit) {
    val s = popIn(o)
    val title = when (o.type) {
        OutcomeType.JACKPOT -> stringResource(R.string.jackpot)
        OutcomeType.ENERGY -> stringResource(R.string.spin_bonus)
        else -> stringResource(R.string.big_win)
    }
    GameDialog(onDismiss) {
        Box(contentAlignment = Alignment.Center) {
            Rays(t, modifier = Modifier.size(360.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(s)) {
                val wob = rememberInfiniteTransition(label = "w").animateFloat(-4f, 4f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "wr")
                GameText(title, Modifier.rotate(wob.value), size = 46.sp, color = SK.GoldLight, align = TextAlign.Center)
                if (o.multiplier > 1) GameText("x${o.multiplier}", size = 22.sp, color = SK.Orange)
                Spacer(Modifier.height(12.dp))
                if (o.coins > 0) RewardRow({ CoinIcon(40.dp) }, "+" + formatNumber(o.coins))
                if (o.spins > 0) RewardRow({ EnergyIcon(40.dp) }, "+${o.spins} " + stringResource(R.string.spins_short))
                Spacer(Modifier.height(18.dp))
                if (o.canDouble) {
                    GameButton(stringResource(R.string.double_reward), onDouble, color = SK.Purple, modifier = Modifier.testTag("double_reward"))
                    Spacer(Modifier.height(8.dp))
                }
                GameButton(stringResource(R.string.collect), onDismiss, Modifier.testTag("overlay_ok"))
            }
        }
    }
    if (o.type == OutcomeType.JACKPOT) Confetti(o)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChestOverlay(o: Overlay.Chest, t: Float, onDismiss: () -> Unit, sfx: (Sfx) -> Unit) {
    var opened by remember(o) { mutableStateOf(false) }
    val lid = remember(o) { Animatable(0f) }
    val shake = remember(o) { Animatable(0f) }
    val items = remember(o) { Animatable(0f) }
    LaunchedEffect(o) {
        shake.animateTo(1f, tween(900))
        opened = true
        sfx(Sfx.CHEST)
        lid.animateTo(1f, tween(450, easing = CubicBezierEasing(0.3f, 1.5f, 0.5f, 1f)))
        items.animateTo(1f, tween(900))
    }
    val c = o.contents
    val (body, trim) = chestColors(c.type)
    GameDialog(onDismiss = { if (items.value >= 1f) onDismiss() }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            GameText(chestName(c.type), size = 28.sp, color = SK.GoldLight)
            Box(contentAlignment = Alignment.Center) {
                if (opened) Rays(t, trim, Modifier.size(260.dp))
                Canvas(Modifier.size(200.dp).rotate(if (!opened) sin(shake.value * 60f) * 8f * shake.value else 0f)) {
                    drawChestIcon(center + Offset(0f, size.height * 0.1f), size.minDimension * 0.32f, body, trim, lid.value)
                }
            }
            val p = items.value
            Column(Modifier.alpha(p).scale(0.6f + 0.4f * p), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (c.coins > 0) RewardRow({ CoinIcon(28.dp) }, formatNumber(c.coins))
                    if (c.spins > 0) RewardRow({ EnergyIcon(28.dp) }, "${c.spins}")
                    if (c.treats > 0) RewardRow({ TreatIcon(28.dp) }, "${c.treats}")
                    if (c.petXp > 0) RewardRow({ StarIcon(24.dp) }, "${c.petXp} XP")
                }
                if (c.cards.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                        c.cards.forEachIndexed { i, id ->
                            val flip = ((p * c.cards.size) - i).coerceIn(0f, 1f)
                            Box {
                                CardView(id, 1, t, width = 62.dp, flip = flip)
                                if (id in o.newCards) GameText(stringResource(R.string.new_card), Modifier.align(Alignment.TopCenter).background(SK.Red).padding(horizontal = 4.dp), size = 9.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            GameButton(stringResource(R.string.collect), onDismiss, enabled = p >= 1f, modifier = Modifier.testTag("overlay_ok"))
        }
    }
}

@Composable
private fun LevelCompleteOverlay(o: Overlay.LevelComplete, t: Float, onDismiss: () -> Unit) {
    val s = popIn(o)
    val next = LevelConfig.level(o.level + 1)
    GameDialog(null) {
        Box(contentAlignment = Alignment.Center) {
            Rays(t, modifier = Modifier.size(380.dp))
            Panel(Modifier.fillMaxWidth().scale(s)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    GameText(stringResource(R.string.village_complete), size = 30.sp, color = SK.GoldLight, align = TextAlign.Center)
                    GameText(stringResource(R.string.village_complete_text, levelName(LevelConfig.level(o.level))), size = 15.sp, align = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Row { repeat(3) { StarIcon(if (it == 1) 54.dp else 40.dp) } }
                    Spacer(Modifier.height(8.dp))
                    RewardRow({ CoinIcon(28.dp) }, "+" + formatNumber(o.coins))
                    RewardRow({ EnergyIcon(28.dp) }, "+${o.spins} " + stringResource(R.string.spins_short))
                    RewardRow({ StarIcon(26.dp) }, "+${GameBalanceConfig.STARS_PER_LEVEL_COMPLETE}")
                    RewardRow({ Canvas(Modifier.size(28.dp)) { val (b, tr) = chestColors(o.chest); drawChestIcon(center, size.minDimension / 2.3f, b, tr) } }, chestName(o.chest))
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(120.dp)) {
                        VillageCanvas(next, List(5) { 0 }, List(5) { false }, t)
                        GameText(stringResource(R.string.next_village, levelName(next)), Modifier.align(Alignment.TopCenter).padding(4.dp), size = 15.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    GameButton(stringResource(R.string.to_next_village), onDismiss, Modifier.fillMaxWidth().testTag("overlay_ok"))
                }
            }
        }
    }
    Confetti(o)
    CoinRain(o, count = 30)
}

@Composable
private fun BotLogOverlay(o: Overlay.BotLog, onDismiss: () -> Unit) {
    GameDialog(onDismiss) {
        Panel(Modifier.fillMaxWidth()) {
            GameText(stringResource(R.string.while_away), Modifier.fillMaxWidth(), size = 22.sp, align = TextAlign.Center, color = SK.GoldLight)
            Spacer(Modifier.height(8.dp))
            val fmt = DateFormat.getTimeInstance(DateFormat.SHORT)
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                o.entries.forEach { e ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(30.dp)) {
                            if (e.kind == "bot_attack_blocked") drawShieldIcon(center, size.minDimension / 2.2f)
                            else drawHammer(center, size.minDimension / 2.4f)
                        }
                        Spacer(Modifier.width(8.dp))
                        GameText(
                            fmt.format(Date(e.timeMillis)) + "  " + stringResource(if (e.kind == "bot_attack_blocked") R.string.log_blocked else R.string.log_hit, e.attacker),
                            size = 14.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            GameButton(stringResource(R.string.ok), onDismiss, Modifier.fillMaxWidth().testTag("overlay_ok"))
        }
    }
}

/** Simulated rewarded ad with countdown. */
@Composable
fun AdOverlay(s: AdOverlayState, t: Float, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xF0000000)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            GameText(stringResource(R.string.ad_simulated), size = 14.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(260.dp, 180.dp).background(Brush.linearGradient(listOf(SK.Purple, SK.Blue))), contentAlignment = Alignment.Center) {
                Logo(t, Modifier.size(150.dp))
                GameText(stringResource(R.string.ad_fake_title), Modifier.align(Alignment.BottomCenter).padding(6.dp), size = 14.sp)
            }
            Spacer(Modifier.height(16.dp))
            ProgressBar(1f - s.secondsLeft / s.totalSeconds.toFloat(), Modifier.width(260.dp), color = SK.Gold)
            Spacer(Modifier.height(10.dp))
            if (!s.canClose) {
                GameText(stringResource(R.string.ad_seconds_left, s.secondsLeft), size = 16.sp)
                Spacer(Modifier.height(10.dp))
                GameButton(stringResource(R.string.ad_skip_no_reward), onClose, color = Color(0xFF6B6680), height = 44.dp, textSize = 14.sp)
            } else {
                GameButton(stringResource(R.string.ad_collect_reward), onClose, color = SK.Green, modifier = Modifier.testTag("ad_close"))
            }
        }
    }
}
