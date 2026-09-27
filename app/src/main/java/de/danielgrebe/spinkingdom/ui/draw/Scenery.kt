package de.danielgrebe.spinkingdom.ui.draw

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import de.danielgrebe.spinkingdom.config.Decoration
import de.danielgrebe.spinkingdom.config.Palette
import de.danielgrebe.spinkingdom.models.SlotSymbol
import de.danielgrebe.spinkingdom.ui.theme.SK
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun rnd(i: Int, k: Int): Float { val x = sin(i * 12.9898f + k * 78.233f) * 43758.547f; return x - kotlin.math.floor(x) }

/** Sky, far hills and the theme decoration. [horizon] is the y where the ground starts (0..1 of height). */
fun DrawScope.drawScenery(p: Palette, deco: Decoration, t: Float, horizon: Float = 0.42f) {
    val w = size.width; val h = size.height; val hy = h * horizon
    drawRect(Brush.verticalGradient(listOf(p.skyTop.toColor(), p.skyBottom.toColor()), 0f, hy + h * 0.1f))
    val night = deco == Decoration.STARS || deco == Decoration.GHOSTS
    // sun or moon
    if (deco !in listOf(Decoration.BUBBLES)) {
        val sc = Offset(w * 0.82f, h * 0.12f)
        drawCircle(Brush.radialGradient(listOf((if (night) Color(0x66FFFFFF) else Color(0x88FFF3B0)), Color.Transparent), sc, w * 0.18f), w * 0.18f, sc)
        drawCircle(if (night) Color(0xFFF4F1E0) else Color(0xFFFFE680), w * 0.06f, sc)
        if (night) drawCircle(p.skyTop.toColor(), w * 0.05f, sc + Offset(w * 0.025f, -w * 0.015f))
    }
    // far hills
    val hill1 = Path().apply {
        moveTo(0f, hy)
        var x = 0f
        while (x <= w) { lineTo(x, hy - h * 0.06f - sin(x / w * 7f + 1f) * h * 0.035f); x += w / 24f }
        lineTo(w, hy + 2f); lineTo(0f, hy + 2f); close()
    }
    drawPath(hill1, p.groundDark.toColor().copy(alpha = 0.55f))
    // theme decoration in the sky
    when (deco) {
        Decoration.CLOUDS, Decoration.BLOSSOM, Decoration.PALMS, Decoration.PINES, Decoration.CACTI, Decoration.CANDY, Decoration.SAND -> clouds(w, h, t)
        Decoration.STARS -> stars(w, hy, t)
        Decoration.SNOW -> { clouds(w, h, t); snow(w, h, t) }
        Decoration.BUBBLES -> bubbles(w, h, t)
        Decoration.LAVA -> embers(w, h, t)
        Decoration.GHOSTS -> { stars(w, hy, t); ghosts(w, hy, t) }
    }
    // ground
    drawRect(Brush.verticalGradient(listOf(p.ground.toColor(), p.groundDark.toColor()), hy, h), Offset(0f, hy), Size(w, h - hy))
    // ground texture tufts
    for (i in 0 until 26) {
        val x = rnd(i, 1) * w; val y = hy + rnd(i, 2) * (h - hy)
        drawOval(p.groundDark.toColor().copy(alpha = 0.35f), Offset(x, y), Size(w * 0.05f, w * 0.012f))
    }
    // side decorations on the ground
    for (i in 0 until 6) {
        val x = if (i % 2 == 0) w * (0.02f + rnd(i, 3) * 0.08f) else w * (0.9f + rnd(i, 3) * 0.08f)
        val y = hy + (h - hy) * (0.1f + i * 0.15f)
        groundProp(deco, x, y, w * 0.07f, p, t, i)
    }
    if (deco == Decoration.BLOSSOM) petals(w, h, t)
}

private fun DrawScope.clouds(w: Float, h: Float, t: Float) {
    for (i in 0 until 4) {
        val speed = 6f + i * 3f
        val x = ((rnd(i, 5) * w + t * speed) % (w + w * 0.4f)) - w * 0.2f
        val y = h * (0.05f + rnd(i, 6) * 0.2f)
        val s = w * (0.07f + rnd(i, 7) * 0.05f)
        listOf(Offset(0f, 0f), Offset(s * 0.9f, -s * 0.3f), Offset(s * 1.8f, 0f), Offset(s * 0.9f, s * 0.2f)).forEach {
            drawCircle(Color.White.copy(alpha = 0.9f), s * 0.75f, Offset(x + it.x, y + it.y))
        }
    }
}

private fun DrawScope.stars(w: Float, hy: Float, t: Float) {
    for (i in 0 until 40) {
        val a = 0.4f + 0.6f * (0.5f + 0.5f * sin(t * 2f + i))
        drawCircle(Color.White.copy(alpha = a), 1.5f + rnd(i, 9) * 2.5f, Offset(rnd(i, 10) * w, rnd(i, 11) * hy * 0.9f))
    }
}

private fun DrawScope.snow(w: Float, h: Float, t: Float) {
    for (i in 0 until 50) {
        val y = (rnd(i, 12) * h + t * (20f + rnd(i, 13) * 30f)) % h
        val x = rnd(i, 14) * w + sin(t + i) * 10f
        drawCircle(Color.White.copy(alpha = 0.85f), 2f + rnd(i, 15) * 3f, Offset(x, y))
    }
}

private fun DrawScope.bubbles(w: Float, h: Float, t: Float) {
    for (i in 0 until 26) {
        val y = h - ((rnd(i, 16) * h + t * (25f + rnd(i, 17) * 25f)) % h)
        val x = rnd(i, 18) * w + sin(t * 1.5f + i) * 8f
        val r = 3f + rnd(i, 19) * 8f
        drawCircle(Color.White.copy(alpha = 0.35f), r, Offset(x, y), style = Stroke(1.5f))
        drawCircle(Color.White.copy(alpha = 0.5f), r * 0.25f, Offset(x - r * 0.3f, y - r * 0.3f))
    }
    // light rays
    for (i in 0 until 3) {
        val x = w * (0.2f + i * 0.3f)
        drawPath(Path().apply { moveTo(x, 0f); lineTo(x + w * 0.08f, 0f); lineTo(x + w * 0.2f, h * 0.45f); lineTo(x + w * 0.05f, h * 0.45f); close() }, Color.White.copy(alpha = 0.06f + 0.03f * sin(t + i)))
    }
}

private fun DrawScope.embers(w: Float, h: Float, t: Float) {
    for (i in 0 until 30) {
        val y = h - ((rnd(i, 20) * h + t * (30f + rnd(i, 21) * 30f)) % h)
        val x = rnd(i, 22) * w + sin(t * 2f + i) * 12f
        drawCircle(Color(0xFFFF9E2E).copy(alpha = 0.7f), 2f + rnd(i, 23) * 2.5f, Offset(x, y))
    }
    // volcano silhouette
    val vx = size.width * 0.18f; val vy = size.height * 0.42f
    drawPath(Path().apply { moveTo(vx - w * 0.2f, vy); lineTo(vx - w * 0.04f, vy - h * 0.16f); lineTo(vx + w * 0.04f, vy - h * 0.16f); lineTo(vx + w * 0.2f, vy); close() }, Color(0xFF4A2A22))
    drawOval(Color(0xFFFF6A1F), Offset(vx - w * 0.045f, vy - h * 0.17f), Size(w * 0.09f, h * 0.02f))
}

private fun DrawScope.ghosts(w: Float, hy: Float, t: Float) {
    for (i in 0 until 3) {
        val x = w * (0.15f + i * 0.3f) + sin(t * 0.7f + i * 2f) * w * 0.05f
        val y = hy * (0.35f + 0.15f * i) + sin(t * 1.3f + i) * 8f
        val r = w * 0.035f
        drawPath(Path().apply {
            moveTo(x - r, y); quadraticBezierTo(x - r, y - r * 1.6f, x, y - r * 1.6f); quadraticBezierTo(x + r, y - r * 1.6f, x + r, y)
            lineTo(x + r, y + r); lineTo(x + r * 0.5f, y + r * 0.7f); lineTo(x, y + r); lineTo(x - r * 0.5f, y + r * 0.7f); lineTo(x - r, y + r); close()
        }, Color.White.copy(alpha = 0.55f))
        drawCircle(Color(0xFF2B2D42), r * 0.18f, Offset(x - r * 0.35f, y - r * 0.6f))
        drawCircle(Color(0xFF2B2D42), r * 0.18f, Offset(x + r * 0.35f, y - r * 0.6f))
    }
}

private fun DrawScope.petals(w: Float, h: Float, t: Float) {
    for (i in 0 until 22) {
        val y = (rnd(i, 24) * h + t * (18f + rnd(i, 25) * 15f)) % h
        val x = (rnd(i, 26) * w + t * 10f + sin(t + i) * 15f) % w
        drawOval(Color(0xFFFFB6C9), Offset(x, y), Size(8f, 5f))
    }
}

private fun DrawScope.groundProp(deco: Decoration, x: Float, y: Float, s: Float, p: Palette, t: Float, i: Int) {
    when (deco) {
        Decoration.PALMS -> {
            drawLine(Color(0xFF8A5A2B), Offset(x, y), Offset(x + s * 0.2f, y - s * 1.4f), s * 0.18f)
            for (k in 0 until 5) {
                val a = (-160f + k * 35f + sin(t + i) * 4f) * PI.toFloat() / 180f
                drawLine(Color(0xFF2E9E4A), Offset(x + s * 0.2f, y - s * 1.4f), Offset(x + s * 0.2f + cos(a) * s * 0.9f, y - s * 1.4f + sin(a) * s * 0.5f + s * 0.3f), s * 0.16f)
            }
        }
        Decoration.PINES, Decoration.SNOW -> {
            drawRect(Color(0xFF6B4A2A), Offset(x - s * 0.08f, y - s * 0.3f), Size(s * 0.16f, s * 0.3f))
            for (k in 0 until 3) {
                val ty = y - s * 0.3f - k * s * 0.35f
                drawPath(Path().apply { moveTo(x - s * (0.55f - k * 0.12f), ty); lineTo(x, ty - s * 0.6f); lineTo(x + s * (0.55f - k * 0.12f), ty); close() }, if (deco == Decoration.SNOW) Color(0xFF3F7A5A) else Color(0xFF2E7A3E))
                if (deco == Decoration.SNOW) drawPath(Path().apply { moveTo(x - s * 0.15f, ty - s * 0.42f); lineTo(x, ty - s * 0.6f); lineTo(x + s * 0.15f, ty - s * 0.42f); close() }, Color.White)
            }
        }
        Decoration.CACTI, Decoration.SAND -> {
            val c = Color(0xFF4F9A3A)
            drawRoundRect(c, Offset(x - s * 0.12f, y - s * 1.1f), Size(s * 0.24f, s * 1.1f), CornerRadius(s * 0.12f))
            drawRoundRect(c, Offset(x - s * 0.42f, y - s * 0.8f), Size(s * 0.18f, s * 0.45f), CornerRadius(s * 0.09f))
            drawRoundRect(c, Offset(x - s * 0.42f, y - s * 0.42f), Size(s * 0.35f, s * 0.14f), CornerRadius(s * 0.07f))
            drawRoundRect(c, Offset(x + s * 0.24f, y - s * 0.95f), Size(s * 0.18f, s * 0.4f), CornerRadius(s * 0.09f))
            drawRoundRect(c, Offset(x + s * 0.06f, y - s * 0.62f), Size(s * 0.36f, s * 0.14f), CornerRadius(s * 0.07f))
        }
        Decoration.CANDY -> {
            drawLine(Color.White, Offset(x, y), Offset(x, y - s * 1.1f), s * 0.1f)
            drawCircle(listOf(Color(0xFFFF5FA8), Color(0xFF74E0FF), Color(0xFFFFD447))[i % 3], s * 0.38f, Offset(x, y - s * 1.2f))
            drawCircle(Color.White.copy(alpha = 0.6f), s * 0.38f, Offset(x, y - s * 1.2f), style = Stroke(s * 0.08f))
        }
        Decoration.BUBBLES -> {
            for (k in 0 until 3) drawLine(Color(0xFF2EAD6A), Offset(x + k * s * 0.2f, y), Offset(x + k * s * 0.2f + sin(t * 2 + k + i) * s * 0.2f, y - s * (0.8f + k * 0.2f)), s * 0.1f)
        }
        Decoration.LAVA -> {
            drawOval(Color(0xFF3A2A26), Offset(x - s * 0.5f, y - s * 0.3f), Size(s, s * 0.45f))
            drawOval(Color(0xFFFF6A1F).copy(alpha = 0.6f + 0.3f * sin(t * 3 + i)), Offset(x - s * 0.3f, y - s * 0.2f), Size(s * 0.6f, s * 0.2f))
        }
        Decoration.GHOSTS -> {
            drawRoundRect(Color(0xFF6C6F7F), Offset(x - s * 0.25f, y - s * 0.6f), Size(s * 0.5f, s * 0.6f), CornerRadius(s * 0.25f, s * 0.25f))
            drawLine(Color(0xFF3A3A4A), Offset(x, y - s * 0.5f), Offset(x, y - s * 0.2f), s * 0.05f)
            drawLine(Color(0xFF3A3A4A), Offset(x - s * 0.12f, y - s * 0.4f), Offset(x + s * 0.12f, y - s * 0.4f), s * 0.05f)
        }
        Decoration.STARS -> {
            drawCircle(Color(0xFF6CFFB0).copy(alpha = 0.5f + 0.5f * sin(t * 3 + i)), s * 0.08f, Offset(x, y - s * 0.6f))
            drawRoundRect(Color(0xFF3E6A5A), Offset(x - s * 0.3f, y - s * 0.25f), Size(s * 0.6f, s * 0.25f), CornerRadius(s * 0.12f))
        }
        else -> { // bushes and flowers
            drawCircle(p.groundDark.toColor().darken(0.1f), s * 0.35f, Offset(x, y - s * 0.2f))
            drawCircle(p.groundDark.toColor(), s * 0.28f, Offset(x + s * 0.3f, y - s * 0.15f))
            drawCircle(listOf(SK.Red, SK.Gold, Color(0xFFFF7EB6))[i % 3], s * 0.07f, Offset(x + s * 0.1f, y - s * 0.35f))
        }
    }
}

/** Slot machine symbols. */
fun DrawScope.drawSymbol(symbol: SlotSymbol, c: Offset, r: Float, t: Float = 0f) {
    when (symbol) {
        SlotSymbol.COIN -> {
            drawCoin(c + Offset(-r * 0.25f, r * 0.12f), r * 0.55f)
            drawCoin(c + Offset(r * 0.28f, r * 0.2f), r * 0.5f)
            drawCoin(c + Offset(0f, -r * 0.22f), r * 0.58f)
        }
        SlotSymbol.ATTACK -> {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFF8A7A), SK.Red, SK.RedDark), c, r), r * 0.92f, c)
            drawHammer(c + Offset(0f, r * 0.15f), r * 0.75f)
        }
        SlotSymbol.RAID -> {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD28A), SK.Orange, Color(0xFFB5520F)), c, r), r * 0.92f, c)
            drawShovel(c, r * 0.72f)
        }
        SlotSymbol.SHIELD -> drawShieldIcon(c, r * 0.85f)
        SlotSymbol.ENERGY -> drawEnergy(c, r * 0.85f)
        SlotSymbol.CHEST -> drawChestIcon(c + Offset(0f, r * 0.1f), r * 0.72f)
        SlotSymbol.JOKER -> drawJoker(c, r * 0.9f, t)
    }
}
