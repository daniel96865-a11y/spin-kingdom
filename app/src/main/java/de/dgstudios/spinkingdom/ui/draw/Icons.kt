package de.dgstudios.spinkingdom.ui.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.dgstudios.spinkingdom.ui.theme.SK
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Generic star path centered at c. */
fun starPath(c: Offset, outer: Float, inner: Float, points: Int = 5, rotation: Float = -90f): Path {
    val p = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val a = (rotation + i * 180f / points) * PI.toFloat() / 180f
        val x = c.x + r * cos(a); val y = c.y + r * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close(); return p
}

fun DrawScope.drawCoin(c: Offset, r: Float) {
    drawCircle(Color(0x55000000), r, c + Offset(0f, r * 0.12f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1A8), SK.Gold, SK.GoldDark), c - Offset(r * 0.3f, r * 0.3f), r * 1.6f), r, c)
    drawCircle(SK.GoldDark, r * 0.78f, c, style = Stroke(r * 0.1f))
    // crown emblem
    val w = r * 0.9f; val h = r * 0.55f
    val left = c.x - w / 2; val top = c.y - h / 2
    val crown = Path().apply {
        moveTo(left, top + h); lineTo(left, top + h * 0.2f); lineTo(left + w * 0.25f, top + h * 0.55f)
        lineTo(left + w * 0.5f, top); lineTo(left + w * 0.75f, top + h * 0.55f); lineTo(left + w, top + h * 0.2f)
        lineTo(left + w, top + h); close()
    }
    drawPath(crown, SK.GoldDark)
    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.18f, c - Offset(r * 0.45f, r * 0.45f))
}

fun DrawScope.drawEnergy(c: Offset, r: Float, color: Color = Color(0xFF45D7FF)) {
    drawCircle(Brush.radialGradient(listOf(Color(0xFF9EF0FF), color, Color(0xFF1377C9)), c, r), r, c)
    val bolt = Path().apply {
        moveTo(c.x + r * 0.15f, c.y - r * 0.75f); lineTo(c.x - r * 0.45f, c.y + r * 0.1f); lineTo(c.x - r * 0.02f, c.y + r * 0.1f)
        lineTo(c.x - r * 0.2f, c.y + r * 0.78f); lineTo(c.x + r * 0.48f, c.y - r * 0.12f); lineTo(c.x + r * 0.05f, c.y - r * 0.12f); close()
    }
    drawPath(bolt, Color(0xFFFFF36B))
    drawPath(bolt, Color(0xFFB07A00), style = Stroke(r * 0.06f))
}

fun shieldPath(c: Offset, r: Float): Path = Path().apply {
    moveTo(c.x, c.y - r); cubicTo(c.x + r * 0.6f, c.y - r * 0.75f, c.x + r * 0.9f, c.y - r * 0.8f, c.x + r * 0.9f, c.y - r * 0.8f)
    cubicTo(c.x + r * 0.95f, c.y + r * 0.1f, c.x + r * 0.6f, c.y + r * 0.7f, c.x, c.y + r)
    cubicTo(c.x - r * 0.6f, c.y + r * 0.7f, c.x - r * 0.95f, c.y + r * 0.1f, c.x - r * 0.9f, c.y - r * 0.8f)
    cubicTo(c.x - r * 0.9f, c.y - r * 0.8f, c.x - r * 0.6f, c.y - r * 0.75f, c.x, c.y - r); close()
}

fun DrawScope.drawShieldIcon(c: Offset, r: Float, filled: Boolean = true) {
    val p = shieldPath(c, r)
    if (filled) {
        drawPath(p, Brush.verticalGradient(listOf(Color(0xFF7FD3FF), Color(0xFF2A7BE0)), c.y - r, c.y + r))
        drawPath(shieldPath(c, r * 0.62f), Brush.verticalGradient(listOf(Color(0xFFFFE680), SK.GoldDark), c.y - r, c.y + r))
        drawPath(p, Color(0xFF123E80), style = Stroke(r * 0.1f))
    } else {
        drawPath(p, Color(0x33FFFFFF))
        drawPath(p, Color(0x88FFFFFF), style = Stroke(r * 0.08f))
    }
}

fun DrawScope.drawStarIcon(c: Offset, r: Float, color: Color = SK.Gold) {
    val p = starPath(c, r, r * 0.45f)
    drawPath(starPath(c + Offset(0f, r * 0.1f), r, r * 0.45f), Color(0x55000000))
    drawPath(p, Brush.radialGradient(listOf(Color(0xFFFFF6C2), color, SK.GoldDark), c, r * 1.2f))
    drawPath(p, SK.GoldDark, style = Stroke(r * 0.08f))
}

fun DrawScope.drawHammer(c: Offset, r: Float) {
    rotate(-35f, c) {
        drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFB67A45), Color(0xFF7A4A22))), Offset(c.x - r * 0.12f, c.y - r * 0.3f), Size(r * 0.24f, r * 1.2f), CornerRadius(r * 0.1f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE6E9EF), Color(0xFF7C8594)), c.y - r * 0.85f, c.y - r * 0.3f), Offset(c.x - r * 0.62f, c.y - r * 0.85f), Size(r * 1.24f, r * 0.55f), CornerRadius(r * 0.12f))
        drawRoundRect(Color(0xFF4A515C), Offset(c.x - r * 0.62f, c.y - r * 0.85f), Size(r * 1.24f, r * 0.55f), CornerRadius(r * 0.12f), style = Stroke(r * 0.06f))
    }
}

fun DrawScope.drawShovel(c: Offset, r: Float) {
    rotate(30f, c) {
        drawRoundRect(Color(0xFF8A5A2B), Offset(c.x - r * 0.08f, c.y - r * 0.95f), Size(r * 0.16f, r * 1.05f), CornerRadius(r * 0.08f))
        drawRoundRect(Color(0xFF6B3F18), Offset(c.x - r * 0.3f, c.y - r * 1.0f), Size(r * 0.6f, r * 0.14f), CornerRadius(r * 0.07f))
        val blade = Path().apply {
            moveTo(c.x - r * 0.38f, c.y + r * 0.1f); lineTo(c.x + r * 0.38f, c.y + r * 0.1f)
            quadraticBezierTo(c.x + r * 0.4f, c.y + r * 0.75f, c.x, c.y + r * 0.98f)
            quadraticBezierTo(c.x - r * 0.4f, c.y + r * 0.75f, c.x - r * 0.38f, c.y + r * 0.1f); close()
        }
        drawPath(blade, Brush.verticalGradient(listOf(Color(0xFFE8EDF2), Color(0xFF8D98A6)), c.y, c.y + r))
        drawPath(blade, Color(0xFF4A515C), style = Stroke(r * 0.06f))
    }
}

fun DrawScope.drawChestIcon(c: Offset, r: Float, body: Color = Color(0xFFB0682C), trim: Color = SK.Gold, lidOpen: Float = 0f) {
    val w = r * 1.7f; val h = r * 1.15f
    val left = c.x - w / 2; val top = c.y - h * 0.2f
    drawOval(Color(0x44000000), Offset(left, top + h * 0.72f), Size(w, h * 0.35f))
    drawRoundRect(Brush.verticalGradient(listOf(body, body.darken(0.35f)), top, top + h * 0.8f), Offset(left, top), Size(w, h * 0.8f), CornerRadius(r * 0.12f))
    drawRect(trim, Offset(left, top), Size(w, h * 0.12f))
    drawRect(trim, Offset(c.x - w * 0.08f, top), Size(w * 0.16f, h * 0.8f))
    if (lidOpen > 0.01f) {
        drawOval(Brush.radialGradient(listOf(Color(0xFFFFF7C0), Color(0x00FFE066)), Offset(c.x, top), r * 1.5f * lidOpen), Offset(left - r * 0.2f, top - r * 1.2f), Size(w + r * 0.4f, r * 1.6f))
    }
    rotate(-lidOpen * 70f, Offset(left, top)) {
        drawRoundRect(Brush.verticalGradient(listOf(body.lighten(0.25f), body), top - h * 0.45f, top), Offset(left - r * 0.04f, top - h * 0.45f), Size(w + r * 0.08f, h * 0.47f), CornerRadius(r * 0.35f, r * 0.35f))
        drawRect(trim, Offset(c.x - w * 0.08f, top - h * 0.45f), Size(w * 0.16f, h * 0.47f))
        drawRoundRect(trim, Offset(c.x - r * 0.16f, top - r * 0.12f), Size(r * 0.32f, r * 0.3f), CornerRadius(r * 0.06f))
    }
}

fun DrawScope.drawJoker(c: Offset, r: Float, t: Float = 0f) {
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFF9EF5), Color(0xFFB456FF), Color(0xFF5A1FB0)), c, r), r, c)
    drawPath(starPath(c, r * 0.78f, r * 0.34f, rotation = -90f + t * 20f), Brush.radialGradient(listOf(Color.White, SK.Gold), c, r))
    drawPath(starPath(c, r * 0.78f, r * 0.34f, rotation = -90f + t * 20f), SK.GoldDark, style = Stroke(r * 0.06f))
    drawCircle(Color.White, r * 0.1f, c + Offset(-r * 0.12f, -r * 0.08f))
    drawCircle(Color.White, r * 0.1f, c + Offset(r * 0.12f, -r * 0.08f))
    drawArc(Color(0xFF7A2A00), 20f, 140f, false, Offset(c.x - r * 0.2f, c.y - r * 0.05f), Size(r * 0.4f, r * 0.3f), style = Stroke(r * 0.06f, cap = StrokeCap.Round))
}

fun Color.darken(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)
fun Color.lighten(f: Float) = Color(red + (1 - red) * f, green + (1 - green) * f, blue + (1 - blue) * f, alpha)

@Composable
fun CoinIcon(size: Dp = 22.dp, modifier: Modifier = Modifier) =
    Canvas(modifier.size(size)) { drawCoin(center, this.size.minDimension / 2.2f) }

@Composable
fun EnergyIcon(size: Dp = 22.dp, modifier: Modifier = Modifier) =
    Canvas(modifier.size(size)) { drawEnergy(center, this.size.minDimension / 2.2f) }

@Composable
fun ShieldIcon(size: Dp = 22.dp, filled: Boolean = true, modifier: Modifier = Modifier) =
    Canvas(modifier.size(size)) { drawShieldIcon(center, this.size.minDimension / 2.2f, filled) }

@Composable
fun StarIcon(size: Dp = 22.dp, modifier: Modifier = Modifier) =
    Canvas(modifier.size(size)) { drawStarIcon(center, this.size.minDimension / 2.2f) }

@Composable
fun TreatIcon(size: Dp = 22.dp, modifier: Modifier = Modifier) = Canvas(modifier.size(size)) {
    val r = this.size.minDimension / 2.4f
    // bone shaped treat
    val c = center
    drawRoundRect(Color(0xFFF5D6A8), Offset(c.x - r * 0.7f, c.y - r * 0.22f), Size(r * 1.4f, r * 0.44f), CornerRadius(r * 0.2f))
    listOf(-1f, 1f).forEach { sx -> listOf(-1f, 1f).forEach { sy -> drawCircle(Color(0xFFF5D6A8), r * 0.3f, c + Offset(sx * r * 0.72f, sy * r * 0.22f)) } }
    drawRoundRect(Color(0xFFB98A4E), Offset(c.x - r * 0.7f, c.y - r * 0.22f), Size(r * 1.4f, r * 0.44f), CornerRadius(r * 0.2f), style = Stroke(r * 0.06f))
}

@Composable
fun HammerIcon(size: Dp = 22.dp, modifier: Modifier = Modifier) =
    Canvas(modifier.size(size)) { drawHammer(center + Offset(0f, this.size.height * 0.1f), this.size.minDimension / 2.4f) }

@Composable
fun ShovelIcon(size: Dp = 22.dp, modifier: Modifier = Modifier) =
    Canvas(modifier.size(size)) { drawShovel(center, this.size.minDimension / 2.4f) }

@Composable
fun ChestIcon(size: Dp = 22.dp, modifier: Modifier = Modifier, body: Color = Color(0xFFB0682C), trim: Color = SK.Gold) =
    Canvas(modifier.size(size)) { drawChestIcon(center + Offset(0f, this.size.height * 0.05f), this.size.minDimension / 2.3f, body, trim) }
