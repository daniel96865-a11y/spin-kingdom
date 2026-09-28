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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dgstudios.spinkingdom.R
import de.dgstudios.spinkingdom.audio.Sfx
import de.dgstudios.spinkingdom.domain.OutcomeType
import de.dgstudios.spinkingdom.models.SlotSymbol
import de.dgstudios.spinkingdom.ui.SpinAnim
import de.dgstudios.spinkingdom.ui.components.GameButton
import de.dgstudios.spinkingdom.ui.components.GameText
import de.dgstudios.spinkingdom.ui.components.formatNumber
import de.dgstudios.spinkingdom.ui.components.formatTimer
import de.dgstudios.spinkingdom.ui.draw.darken
import de.dgstudios.spinkingdom.ui.draw.drawSymbol
import de.dgstudios.spinkingdom.ui.draw.lighten
import de.dgstudios.spinkingdom.ui.theme.SK
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val STRIP = listOf(
    SlotSymbol.COIN, SlotSymbol.ATTACK, SlotSymbol.SHIELD, SlotSymbol.COIN, SlotSymbol.RAID, SlotSymbol.ENERGY,
    SlotSymbol.COIN, SlotSymbol.CHEST, SlotSymbol.SHIELD, SlotSymbol.JOKER, SlotSymbol.COIN, SlotSymbol.ATTACK,
    SlotSymbol.RAID, SlotSymbol.COIN, SlotSymbol.ENERGY, SlotSymbol.SHIELD
)

private class ReelState(start: Int) {
    val pos = Animatable(start.toFloat())
    val overrides = HashMap<Int, SlotSymbol>()
    fun symbolAt(k: Int): SlotSymbol = overrides[k] ?: STRIP[((k % STRIP.size) + STRIP.size) % STRIP.size]
}

/**
 * Three reel slot machine. Reels scroll through a virtual strip and land on the symbols rolled by the engine.
 * [onReelCenter] reports the machine center in root coordinates (used for flying coins).
 */
@Composable
fun SlotReels(anim: SpinAnim?, lastWin: SpinAnim?, t: Float, onStopped: () -> Unit, sfx: (Sfx) -> Unit, modifier: Modifier = Modifier, onReelCenter: (Offset) -> Unit = {}) {
    val reels = remember { List(3) { i -> ReelState(i * 5) } }
    var stoppedFlash by remember { mutableStateOf(0L) }
    val glow = rememberInfiniteTransition(label = "glow").animateFloat(0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "g")

    LaunchedEffect(anim?.id) {
        val a = anim ?: return@LaunchedEffect
        coroutineScope {
            reels.mapIndexed { i, r ->
                async {
                    val start = kotlin.math.round(r.pos.value).toInt()
                    val target = start + 18 + i * 6
                    r.overrides.keys.retainAll { it == start }
                    r.overrides[target] = a.reels[i]
                    r.pos.animateTo(target.toFloat(), tween(1300 + i * 420, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.08f)))
                    r.pos.snapTo(target.toFloat())
                    sfx(Sfx.REEL_STOP)
                }
            }.awaitAll()
        }
        stoppedFlash = a.id
        onStopped()
    }

    val win = lastWin?.takeIf { it.id == stoppedFlash && anim == null }
    Box(modifier.onGloballyPositioned { c -> onReelCenter(c.positionInRoot() + Offset(c.size.width / 2f, c.size.height / 2f)) }) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            // machine body
            drawRoundRect(Brush.verticalGradient(listOf(SK.GoldLight, SK.Gold, SK.GoldDark)), size = size, cornerRadius = CornerRadius(h * 0.14f))
            drawRoundRect(SK.GoldDark.darken(0.3f), size = size, cornerRadius = CornerRadius(h * 0.14f), style = Stroke(4f))
            // bulbs around the frame
            val bulbs = 18
            for (b in 0 until bulbs) {
                val x = w * (0.04f + 0.92f * b / (bulbs - 1))
                val on = ((t * 4).toInt() + b) % 2 == 0 || win != null
                drawCircle(if (on) Color(0xFFFFFFE0) else Color(0xFFB07A10), h * 0.025f, Offset(x, h * 0.045f))
                drawCircle(if (on) Color(0xFFFFFFE0) else Color(0xFFB07A10), h * 0.025f, Offset(x, h * 0.955f))
            }
            val pad = w * 0.04f
            val gap = w * 0.025f
            val rw = (w - pad * 2 - gap * 2) / 3f
            val top = h * 0.1f; val rh = h * 0.8f
            for (i in 0 until 3) {
                val left = pad + i * (rw + gap)
                drawRoundRect(Color(0xFF3A2410), Offset(left - 3f, top - 3f), Size(rw + 6f, rh + 6f), CornerRadius(rw * 0.14f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFDADDE8), Color.White, Color(0xFFDADDE8)), top, top + rh), Offset(left, top), Size(rw, rh), CornerRadius(rw * 0.12f))
                val r = reels[i]
                val p = r.pos.value
                val base = kotlin.math.floor(p).toInt()
                val frac = p - base
                val cell = rh / 2.2f
                val speed = if (anim != null) 1f else 0f
                clipRect(left, top, left + rw, top + rh) {
                    for (k in -2..2) {
                        val idx = base + k
                        val cy = top + rh / 2 + (k - frac) * cell
                        val sym = r.symbolAt(idx)
                        drawSymbol(sym, Offset(left + rw / 2, cy), cell * 0.4f, t)
                    }
                    if (speed > 0f && r.pos.isRunning) {
                        drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent, Color.White.copy(alpha = 0.35f)), top, top + rh), Offset(left, top), Size(rw, rh))
                    }
                    // top/bottom shading for the cylinder look
                    drawRect(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent), top, top + rh * 0.28f), Offset(left, top), Size(rw, rh * 0.28f))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)), top + rh * 0.72f, top + rh), Offset(left, top + rh * 0.72f), Size(rw, rh * 0.28f))
                }
            }
            // pay line
            val lineColor = if (win != null && win.outcome != OutcomeType.NOTHING) SK.Red.copy(alpha = glow.value) else SK.Red.copy(alpha = 0.35f)
            drawLine(lineColor, Offset(pad * 0.4f, h / 2), Offset(w - pad * 0.4f, h / 2), if (win != null) 6f else 3f)
            drawCircle(SK.Red, h * 0.03f, Offset(pad * 0.45f, h / 2))
            drawCircle(SK.Red, h * 0.03f, Offset(w - pad * 0.45f, h / 2))
        }
        if (win != null && (win.coins > 0 || win.spins > 0)) {
            val label = if (win.coins > 0) "+" + formatNumber(win.coins) else "+${win.spins} " + stringResource(R.string.spins_short)
            val pop = remember(win.id) { Animatable(0.3f) }
            LaunchedEffect(win.id) { pop.animateTo(1f, tween(350, easing = CubicBezierEasing(0.3f, 1.6f, 0.5f, 1f))) }
            GameText(label, Modifier.align(Alignment.Center).scale(pop.value).background(Color(0xCC2A1B0F), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 2.dp), size = 26.sp, color = SK.GoldLight)
        }
    }
}


@Composable
fun MultiplierSelector(options: List<Int>, selected: Int, spins: Int, enabled: Boolean, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, boosted: Set<Int> = emptySet(), t: Float = 0f) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEach { m ->
            val sel = m == selected
            val affordable = m <= maxOf(spins, 1)
            val lucky = m in boosted
            val glow = if (lucky) 0.5f + 0.5f * kotlin.math.sin(t * 6f) else 0f
            Box(
                Modifier.size(width = 46.dp, height = 34.dp).clip(RoundedCornerShape(10.dp))
                    .background(Brush.verticalGradient(when {
                        sel -> listOf(SK.Orange.lighten(0.3f), SK.Orange, SK.Orange.darken(0.2f))
                        lucky -> listOf(SK.GoldLight, SK.Gold, SK.GoldDark)
                        else -> listOf(SK.PanelLight, SK.Panel)
                    }))
                    .border(2.dp, when { sel -> Color.White; lucky -> Color.White.copy(alpha = 0.5f + 0.5f * glow); else -> Color.White.copy(alpha = 0.25f) }, RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled) { onSelect(m) }
                    .testTag("mult_$m"),
                contentAlignment = Alignment.Center
            ) { GameText("x$m", size = 15.sp, color = if (affordable) Color.White else Color.White.copy(alpha = 0.45f)) }
        }
    }
}

/** Countdown badge shown above the multiplier row while the "Glücks-Einsatz" is active. */
@Composable
fun LuckyBoostBadge(millisLeft: Long, t: Float) {
    val pulse = 1f + 0.04f * kotlin.math.sin(t * 5f)
    Row(
        Modifier.padding(bottom = 6.dp).scale(pulse).clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(SK.Purple, SK.Orange)))
            .border(2.dp, SK.GoldLight, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .testTag("lucky_boost_badge"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GameText(stringResource(R.string.lucky_boost_badge, formatTimer(millisLeft)), size = 13.sp)
    }
}

@Composable
fun SpinButton(enabled: Boolean, spinning: Boolean, multiplier: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(1f, 1.04f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "p")
    GameButton(
        text = if (spinning) stringResource(R.string.spinning) else stringResource(R.string.spin_button) + if (multiplier > 1) "  x$multiplier" else "",
        onClick = onClick,
        enabled = enabled && !spinning,
        color = SK.Red,
        textSize = 26.sp,
        height = 68.dp,
        modifier = modifier.scale(if (enabled && !spinning) pulse.value else 1f).testTag("spin_button")
    )
}
