package de.dgstudios.spinkingdom.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalInspectionMode
import de.dgstudios.spinkingdom.ui.draw.drawCoin
import de.dgstudios.spinkingdom.ui.draw.starPath
import de.dgstudios.spinkingdom.ui.theme.SK
import kotlinx.coroutines.isActive
import androidx.compose.runtime.withFrameNanos
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Continuous animation clock in seconds (drives idle animations like windmills, clouds, sparkles). */
@Composable
fun rememberGameTime(): State<Float> {
    val inspection = LocalInspectionMode.current
    return produceState(0f) {
        if (inspection) return@produceState
        var start = -1L
        while (isActive) {
            withFrameNanos { n ->
                if (start < 0) start = n
                value = (n - start) / 1_000_000_000f
            }
        }
    }
}

private fun hash(i: Int, k: Int): Float { val x = sin(i * 91.345f + k * 47.853f) * 24634.6345f; return x - kotlin.math.floor(x) }

/** Full screen coin rain for big wins. Restarts whenever [key] changes. */
@Composable
fun CoinRain(key: Any, count: Int = 45, durationMs: Int = 2600, modifier: Modifier = Modifier.fillMaxSize()) {
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.snapTo(0f); p.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    if (p.value >= 1f) return
    Canvas(modifier) {
        val w = size.width; val h = size.height
        for (i in 0 until count) {
            val delay = hash(i, 1) * 0.45f
            val local = ((p.value - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (local <= 0f || local >= 1f) continue
            val x = hash(i, 2) * w + sin(local * 6f + i) * w * 0.03f
            val y = -h * 0.1f + local * local * h * 1.2f
            val r = w * (0.025f + hash(i, 3) * 0.02f)
            val squash = kotlin.math.abs(cos(local * 12f + i))
            scale(0.3f + 0.7f * squash, 1f, Offset(x, y)) { drawCoin(Offset(x, y), r) }
        }
    }
}

/** Coins flying from [from] to [to] (in root coordinates of the overlay). */
@Composable
fun CoinFly(key: Any, from: Offset, to: Offset, count: Int = 12, durationMs: Int = 1000, modifier: Modifier = Modifier.fillMaxSize()) {
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.snapTo(0f); p.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    if (p.value >= 1f) return
    Canvas(modifier) {
        for (i in 0 until count) {
            val delay = i * 0.04f
            val t = ((p.value - delay) / (1f - delay * 1.5f)).coerceIn(0f, 1f)
            if (t <= 0f || t >= 1f) continue
            val spread = Offset((hash(i, 4) - 0.5f) * size.width * 0.35f, (hash(i, 5) - 0.8f) * size.height * 0.12f)
            val ctrl = Offset((from.x + to.x) / 2f, minOf(from.y, to.y)) + spread
            val e = t * t * (3 - 2 * t)
            val a = from + (ctrl - from) * e
            val b = ctrl + (to - ctrl) * e
            val pos = a + (b - a) * e
            drawCoin(pos, size.width * 0.03f * (1.1f - t * 0.4f))
        }
    }
}

/** Colorful confetti burst (level complete, jackpot). */
@Composable
fun Confetti(key: Any, count: Int = 70, durationMs: Int = 3000, modifier: Modifier = Modifier.fillMaxSize()) {
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.snapTo(0f); p.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    if (p.value >= 1f) return
    val colors = listOf(SK.Gold, SK.Red, SK.Blue, SK.Green, SK.Purple, SK.Orange, Color.White)
    Canvas(modifier) {
        val w = size.width; val h = size.height
        for (i in 0 until count) {
            val ang = (-90f + (hash(i, 6) - 0.5f) * 140f) * PI.toFloat() / 180f
            val speed = h * (0.9f + hash(i, 7) * 0.9f)
            val t = p.value * 2.2f
            val x = w / 2 + cos(ang) * speed * t * 0.5f + sin(t * 5 + i) * 12f
            val y = h * 0.55f + sin(ang) * speed * t * 0.6f + 0.5f * h * 1.6f * t * t
            if (y > h + 20) continue
            rotate(t * 400f + i * 30f, Offset(x, y)) {
                drawRect(colors[i % colors.size].copy(alpha = (1f - p.value).coerceIn(0f, 1f) + 0.3f), Offset(x - 6f, y - 3f), Size(12f, 6f))
            }
        }
    }
}

/** Explosion with shock ring, fire balls and debris at [center]. */
@Composable
fun Explosion(key: Any, center: Offset, radius: Float, durationMs: Int = 900, modifier: Modifier = Modifier.fillMaxSize()) {
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.snapTo(0f); p.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    if (p.value >= 1f) return
    Canvas(modifier) {
        val t = p.value
        drawCircle(Color.White.copy(alpha = (1 - t) * 0.8f), radius * (0.3f + t * 1.6f), center, style = androidx.compose.ui.graphics.drawscope.Stroke(radius * 0.08f * (1 - t) + 1f))
        for (i in 0 until 10) {
            val ang = i * 36f * PI.toFloat() / 180f + hash(i, 8)
            val d = radius * (0.2f + t * (0.6f + hash(i, 9) * 0.6f))
            val c = center + Offset(cos(ang) * d, sin(ang) * d)
            drawCircle(listOf(Color(0xFFFFE066), Color(0xFFFF8A2B), Color(0xFFE8413C))[i % 3].copy(alpha = (1 - t)), radius * 0.28f * (1 - t * 0.7f), c)
        }
        for (i in 0 until 12) {
            val ang = hash(i, 10) * 2 * PI.toFloat()
            val d = radius * t * (1.2f + hash(i, 11))
            val c = center + Offset(cos(ang) * d, sin(ang) * d + radius * t * t * 1.5f)
            drawRect(Color(0xFF5A3A1A).copy(alpha = 1 - t), c, Size(radius * 0.1f, radius * 0.1f))
        }
        for (i in 0 until 4) {
            val c = center + Offset((hash(i, 12) - 0.5f) * radius, -t * radius * (0.8f + hash(i, 13)))
            drawCircle(Color(0xFF666666).copy(alpha = (1 - t) * 0.6f), radius * (0.2f + t * 0.4f), c)
        }
    }
}

/** Twinkling star burst used for upgrades and card reveals. */
@Composable
fun StarBurst(key: Any, center: Offset, radius: Float, durationMs: Int = 800, modifier: Modifier = Modifier.fillMaxSize()) {
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.snapTo(0f); p.animateTo(1f, tween(durationMs)) }
    if (p.value >= 1f) return
    Canvas(modifier) {
        val t = p.value
        for (i in 0 until 10) {
            val ang = i * 36f * PI.toFloat() / 180f
            val d = radius * (0.3f + t)
            val c = center + Offset(cos(ang) * d, sin(ang) * d)
            drawPath(starPath(c, radius * 0.16f * (1 - t) + 2f, radius * 0.06f * (1 - t) + 1f, 4), SK.GoldLight.copy(alpha = 1 - t))
        }
    }
}
