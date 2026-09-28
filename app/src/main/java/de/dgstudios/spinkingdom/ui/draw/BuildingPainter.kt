package de.dgstudios.spinkingdom.ui.draw

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
import de.dgstudios.spinkingdom.config.Archetype
import de.dgstudios.spinkingdom.config.Palette
import de.dgstudios.spinkingdom.ui.theme.SK
import kotlin.math.PI
import kotlin.math.sin

fun Long.toColor() = Color(this.toInt())

/** Colors for one building, derived from the level palette. */
data class BuildingColors(val wall: Color, val roof: Color, val accent: Color) {
    companion object {
        fun of(p: Palette, index: Int): BuildingColors {
            val shift = index * 0.07f
            return BuildingColors(p.wall.toColor().lighten(shift * 0.5f), p.roof.toColor().darken(shift * 0.6f), p.accent.toColor())
        }
    }
}

/**
 * Draws a cartoon building with pseudo-3D depth. [cx]/[by] = bottom center on the ground, [s] = base size.
 * Stage 0 is a construction site, stages 1..5 grow in size and detail. [t] is an animation clock in seconds.
 */
fun DrawScope.drawBuilding(arch: Archetype, stage: Int, colors: BuildingColors, cx: Float, by: Float, s: Float, damaged: Boolean = false, t: Float = 0f) {
    // soft ground shadow
    drawOval(Color(0x40000000), Offset(cx - s * 0.62f, by - s * 0.09f), Size(s * 1.35f, s * 0.2f))
    if (stage <= 0) { drawConstruction(cx, by, s, colors, t); return }
    val grow = listOf(0f, 0.62f, 0.74f, 0.86f, 0.95f, 1.05f)[stage.coerceIn(0, 5)]
    val g = s * grow
    val c = colors
    when (arch) {
        Archetype.COTTAGE -> cottage(cx, by, g, stage, c)
        Archetype.TOWER -> tower(cx, by, g, stage, c)
        Archetype.CASTLE -> castle(cx, by, g, stage, c)
        Archetype.TENT -> tent(cx, by, g, stage, c, t)
        Archetype.STATUE -> statue(cx, by, g, stage, c)
        Archetype.WINDMILL -> windmill(cx, by, g, stage, c, t)
        Archetype.PYRAMID -> pyramid(cx, by, g, stage, c)
        Archetype.DOME -> dome(cx, by, g, stage, c)
        Archetype.TREEHOUSE -> treehouse(cx, by, g, stage, c)
        Archetype.ROCKET -> rocket(cx, by, g, stage, c, t)
        Archetype.PAGODA -> pagoda(cx, by, g, stage, c)
        Archetype.SHIP -> ship(cx, by, g, stage, c, t)
        Archetype.LIGHTHOUSE -> lighthouse(cx, by, g, stage, c, t)
        Archetype.HUT -> hut(cx, by, g, stage, c)
    }
    if (stage >= 5) sparkles(cx, by - g * 0.8f, g, t)
    if (damaged) damage(cx, by, g, t)
}

// ---------------------------------------------------------------- helpers
private fun DrawScope.box(left: Float, bottom: Float, w: Float, h: Float, color: Color, depth: Float = w * 0.22f) {
    // side face (right), top face, front face
    val side = Path().apply {
        moveTo(left + w, bottom); lineTo(left + w + depth, bottom - depth * 0.5f); lineTo(left + w + depth, bottom - h - depth * 0.5f); lineTo(left + w, bottom - h); close()
    }
    drawPath(side, color.darken(0.35f))
    val top = Path().apply {
        moveTo(left, bottom - h); lineTo(left + depth, bottom - h - depth * 0.5f); lineTo(left + w + depth, bottom - h - depth * 0.5f); lineTo(left + w, bottom - h); close()
    }
    drawPath(top, color.lighten(0.2f))
    drawRect(Brush.verticalGradient(listOf(color.lighten(0.08f), color.darken(0.1f)), bottom - h, bottom), Offset(left, bottom - h), Size(w, h))
    drawRect(color.darken(0.5f), Offset(left, bottom - h), Size(w, h), style = Stroke(w * 0.02f))
}

private fun DrawScope.gableRoof(left: Float, top: Float, w: Float, h: Float, color: Color, depth: Float = w * 0.22f) {
    val front = Path().apply { moveTo(left - w * 0.08f, top); lineTo(left + w / 2, top - h); lineTo(left + w * 1.08f, top); close() }
    val side = Path().apply {
        moveTo(left + w / 2, top - h); lineTo(left + w / 2 + depth, top - h - depth * 0.5f); lineTo(left + w * 1.08f + depth, top - depth * 0.5f); lineTo(left + w * 1.08f, top); close()
    }
    drawPath(side, color.darken(0.3f))
    drawPath(front, Brush.verticalGradient(listOf(color.lighten(0.15f), color), top - h, top))
    drawPath(front, color.darken(0.5f), style = Stroke(w * 0.02f))
}

private fun DrawScope.window(x: Float, y: Float, w: Float, h: Float, lit: Boolean = true) {
    drawRoundRect(Color(0xFF4A2E1A), Offset(x - w * 0.12f, y - h * 0.12f), Size(w * 1.24f, h * 1.24f), CornerRadius(w * 0.15f))
    drawRoundRect(if (lit) Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFB84D))) else Brush.verticalGradient(listOf(Color(0xFF8EC9FF), Color(0xFF3F7FC4))), Offset(x, y), Size(w, h), CornerRadius(w * 0.1f))
    drawLine(Color(0xFF4A2E1A), Offset(x + w / 2, y), Offset(x + w / 2, y + h), w * 0.1f)
    drawLine(Color(0xFF4A2E1A), Offset(x, y + h / 2), Offset(x + w, y + h / 2), w * 0.1f)
}

private fun DrawScope.door(cx: Float, bottom: Float, w: Float, h: Float, color: Color = Color(0xFF7A4A22)) {
    drawRoundRect(color, Offset(cx - w / 2, bottom - h), Size(w, h), CornerRadius(w * 0.5f, w * 0.5f))
    drawRoundRect(color.darken(0.4f), Offset(cx - w / 2, bottom - h), Size(w, h), CornerRadius(w * 0.5f, w * 0.5f), style = Stroke(w * 0.08f))
    drawCircle(SK.Gold, w * 0.08f, Offset(cx + w * 0.25f, bottom - h * 0.45f))
}

private fun DrawScope.flag(x: Float, top: Float, h: Float, color: Color, gold: Boolean) {
    drawLine(Color(0xFF5A3A1A), Offset(x, top), Offset(x, top - h), h * 0.06f)
    val f = Path().apply { moveTo(x, top - h); lineTo(x + h * 0.6f, top - h * 0.82f); lineTo(x, top - h * 0.62f); close() }
    drawPath(f, color)
    if (gold) drawCircle(SK.Gold, h * 0.08f, Offset(x, top - h))
}

private fun DrawScope.goldTrim(left: Float, y: Float, w: Float) {
    drawRect(Brush.horizontalGradient(listOf(SK.GoldDark, SK.GoldLight, SK.GoldDark)), Offset(left, y), Size(w, w * 0.05f))
}

private fun DrawScope.sparkles(cx: Float, cy: Float, g: Float, t: Float) {
    for (i in 0 until 4) {
        val phase = (t * 1.3f + i * 0.25f) % 1f
        val a = sin(phase * PI.toFloat())
        val x = cx + g * (-0.5f + i * 0.33f); val y = cy - g * 0.2f * sin(i * 1.7f) - g * 0.15f
        drawPath(starPath(Offset(x, y), g * 0.07f * a + 0.1f, g * 0.025f * a + 0.05f, 4), Color.White.copy(alpha = a))
    }
}

private fun DrawScope.damage(cx: Float, by: Float, g: Float, t: Float) {
    // cracks
    val crack = Path().apply { moveTo(cx - g * 0.2f, by - g * 0.6f); lineTo(cx - g * 0.05f, by - g * 0.45f); lineTo(cx - g * 0.15f, by - g * 0.32f); lineTo(cx + g * 0.02f, by - g * 0.18f) }
    drawPath(crack, Color(0xFF2A1A10), style = Stroke(g * 0.03f, cap = StrokeCap.Round))
    // rising smoke puffs
    for (i in 0 until 3) {
        val ph = (t * 0.5f + i / 3f) % 1f
        drawCircle(Color(0xFF555555).copy(alpha = (1f - ph) * 0.6f), g * (0.08f + ph * 0.12f), Offset(cx + g * 0.15f + ph * g * 0.1f, by - g * (0.7f + ph * 0.6f)))
    }
    // repair sign
    drawRoundRect(Color(0xFFFFD447), Offset(cx - g * 0.62f, by - g * 0.28f), Size(g * 0.26f, g * 0.2f), CornerRadius(g * 0.03f))
    drawLine(Color.Black, Offset(cx - g * 0.6f, by - g * 0.1f), Offset(cx - g * 0.38f, by - g * 0.26f), g * 0.03f)
}

private fun DrawScope.drawConstruction(cx: Float, by: Float, s: Float, c: BuildingColors, t: Float) {
    drawOval(Color(0xFF9C6B3F), Offset(cx - s * 0.5f, by - s * 0.14f), Size(s, s * 0.22f))
    drawOval(Color(0xFFB8864F), Offset(cx - s * 0.42f, by - s * 0.14f), Size(s * 0.84f, s * 0.14f))
    val wood = Color(0xFFC8955A)
    // scaffold
    for (i in 0..2) drawLine(wood, Offset(cx - s * 0.36f + i * s * 0.36f, by - s * 0.05f), Offset(cx - s * 0.36f + i * s * 0.36f, by - s * 0.62f), s * 0.035f)
    drawLine(wood, Offset(cx - s * 0.4f, by - s * 0.34f), Offset(cx + s * 0.4f, by - s * 0.34f), s * 0.03f)
    drawLine(wood, Offset(cx - s * 0.4f, by - s * 0.6f), Offset(cx + s * 0.4f, by - s * 0.6f), s * 0.03f)
    drawLine(wood.darken(0.2f), Offset(cx - s * 0.36f, by - s * 0.05f), Offset(cx, by - s * 0.34f), s * 0.025f)
    // brick pile
    for (i in 0 until 3) drawRoundRect(c.roof, Offset(cx + s * 0.08f + i * s * 0.09f, by - s * 0.16f), Size(s * 0.08f, s * 0.06f), CornerRadius(s * 0.01f))
    // plus sign bubble (bounce)
    val bounce = sin(t * 3f) * s * 0.03f
    drawCircle(SK.Green, s * 0.14f, Offset(cx, by - s * 0.85f + bounce))
    drawCircle(Color.White, s * 0.14f, Offset(cx, by - s * 0.85f + bounce), style = Stroke(s * 0.02f))
    drawLine(Color.White, Offset(cx - s * 0.07f, by - s * 0.85f + bounce), Offset(cx + s * 0.07f, by - s * 0.85f + bounce), s * 0.035f, StrokeCap.Round)
    drawLine(Color.White, Offset(cx, by - s * 0.92f + bounce), Offset(cx, by - s * 0.78f + bounce), s * 0.035f, StrokeCap.Round)
}

// ---------------------------------------------------------------- archetypes
private fun DrawScope.cottage(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors) {
    val w = g * 0.8f; val h = g * 0.45f; val left = cx - w / 2 - g * 0.08f
    if (st >= 4) { box(left + w * 0.72f, by, w * 0.45f, h * 0.8f, c.wall.darken(0.08f)); gableRoof(left + w * 0.72f, by - h * 0.8f, w * 0.45f, h * 0.5f, c.roof) }
    box(left, by, w, h, c.wall)
    gableRoof(left, by - h, w, g * 0.38f, c.roof)
    if (st >= 3) { drawRect(Color(0xFF8A5A3A), Offset(left + w * 0.68f, by - h - g * 0.38f), Size(w * 0.12f, g * 0.22f)) }
    door(left + w * 0.3f, by, w * 0.2f, h * 0.55f)
    if (st >= 2) window(left + w * 0.58f, by - h * 0.75f, w * 0.2f, h * 0.35f, st >= 3)
    if (st >= 5) { goldTrim(left, by - h, w); flag(left + w / 2, by - h - g * 0.38f, g * 0.28f, c.accent, true) }
}

private fun DrawScope.tower(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors) {
    val w = g * 0.42f; val h = g * (0.7f + st * 0.06f); val left = cx - w / 2
    box(left, by, w, h, c.wall)
    // battlements or cone
    if (st >= 3) {
        val cone = Path().apply { moveTo(left - w * 0.15f, by - h); lineTo(cx + w * 0.1f, by - h - g * 0.45f); lineTo(left + w * 1.35f, by - h); close() }
        drawPath(cone, Brush.horizontalGradient(listOf(c.roof.lighten(0.2f), c.roof.darken(0.3f)), left, left + w * 1.35f))
    } else {
        for (i in 0 until 3) box(left + i * w * 0.38f, by - h, w * 0.24f, g * 0.08f, c.wall)
    }
    window(cx - w * 0.15f, by - h * 0.75f, w * 0.3f, h * 0.16f, st >= 2)
    if (st >= 2) window(cx - w * 0.15f, by - h * 0.45f, w * 0.3f, h * 0.16f, st >= 3)
    door(cx, by, w * 0.4f, h * 0.2f)
    if (st >= 4) flag(cx + w * 0.1f, by - h - g * (if (st >= 3) 0.45f else 0.08f), g * 0.25f, c.accent, st >= 5)
    if (st >= 5) goldTrim(left, by - h * 0.95f, w)
}

private fun DrawScope.castle(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors) {
    val w = g * 0.95f; val h = g * 0.42f; val left = cx - w / 2 - g * 0.05f
    val tw = w * 0.24f; val th = h * (1.3f + st * 0.08f)
    box(left, by, tw, th, c.wall)
    box(left + w - tw, by, tw, th, c.wall)
    box(left + tw * 0.9f, by, w - tw * 1.8f, h, c.wall.darken(0.05f))
    for (i in 0 until 4) box(left + tw * 0.9f + i * (w - tw * 1.8f) / 4f, by - h, (w - tw * 1.8f) / 8f, g * 0.06f, c.wall)
    door(cx - g * 0.03f, by, w * 0.18f, h * 0.6f, Color(0xFF5A3A1A))
    val cones = st >= 2
    listOf(left, left + w - tw).forEach { x ->
        if (cones) {
            val cone = Path().apply { moveTo(x - tw * 0.12f, by - th); lineTo(x + tw * 0.5f, by - th - g * 0.32f); lineTo(x + tw * 1.12f, by - th); close() }
            drawPath(cone, Brush.horizontalGradient(listOf(c.roof.lighten(0.2f), c.roof.darken(0.3f)), x, x + tw))
            if (st >= 4) flag(x + tw * 0.5f, by - th - g * 0.32f, g * 0.2f, c.accent, st >= 5)
        }
        if (st >= 3) window(x + tw * 0.3f, by - th * 0.7f, tw * 0.4f, th * 0.18f)
    }
    if (st >= 5) { goldTrim(left + tw * 0.9f, by - h, w - tw * 1.8f); drawCircle(SK.Gold, g * 0.05f, Offset(cx - g * 0.03f, by - h * 0.8f)) }
}

private fun DrawScope.tent(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors, t: Float) {
    val w = g * 0.9f; val h = g * 0.6f; val left = cx - w / 2
    val stripes = 3 + st
    val body = Path().apply { moveTo(left, by); lineTo(cx, by - h); lineTo(left + w, by); close() }
    drawPath(body, c.wall)
    for (i in 0 until stripes) if (i % 2 == 0) {
        val x0 = left + w * i / stripes; val x1 = left + w * (i + 1) / stripes
        drawPath(Path().apply { moveTo(x0, by); lineTo(cx, by - h); lineTo(x1, by); close() }, c.roof)
    }
    drawPath(body, c.roof.darken(0.5f), style = Stroke(g * 0.015f))
    val entrance = Path().apply { moveTo(cx - w * 0.12f, by); lineTo(cx, by - h * 0.4f); lineTo(cx + w * 0.12f, by); close() }
    drawPath(entrance, Color(0xFF3A2410))
    if (st >= 3) { // market counter
        box(left + w * 0.72f, by, w * 0.4f, h * 0.3f, Color(0xFFB07A45))
        for (i in 0 until 3) drawCircle(listOf(SK.Red, SK.Gold, SK.Green)[i], g * 0.035f, Offset(left + w * 0.8f + i * w * 0.1f, by - h * 0.33f))
    }
    if (st >= 2) flag(cx, by - h, g * 0.25f, c.accent, st >= 5)
    if (st >= 4) { // bunting
        val sway = sin(t * 2f) * g * 0.01f
        for (i in 0 until 5) {
            val x = left - g * 0.05f + i * w * 0.12f
            drawPath(Path().apply { moveTo(x, by - h * 0.55f + sway); lineTo(x + w * 0.06f, by - h * 0.4f + sway); lineTo(x + w * 0.12f, by - h * 0.55f + sway); close() }, listOf(SK.Red, SK.Gold, SK.Blue)[i % 3])
        }
    }
}

private fun DrawScope.statue(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors) {
    val pw = g * 0.55f; val ph = g * 0.22f
    box(cx - pw / 2, by, pw, ph, c.wall.darken(0.1f))
    if (st >= 3) box(cx - pw * 0.4f, by - ph, pw * 0.8f, ph * 0.4f, c.wall)
    val baseY = by - ph - (if (st >= 3) ph * 0.4f else 0f)
    val mat = if (st >= 5) SK.Gold else c.accent.lighten(0.2f)
    // figure: body + head + raised arm/sword
    drawRoundRect(Brush.verticalGradient(listOf(mat.lighten(0.2f), mat.darken(0.25f))), Offset(cx - g * 0.1f, baseY - g * 0.38f), Size(g * 0.2f, g * 0.38f), CornerRadius(g * 0.06f))
    drawCircle(mat, g * 0.09f, Offset(cx, baseY - g * 0.46f))
    if (st >= 2) drawLine(mat.darken(0.2f), Offset(cx + g * 0.08f, baseY - g * 0.32f), Offset(cx + g * 0.2f, baseY - g * 0.62f), g * 0.045f, StrokeCap.Round)
    if (st >= 4) drawPath(starPath(Offset(cx + g * 0.21f, baseY - g * 0.66f), g * 0.07f, g * 0.03f), SK.GoldLight)
    if (st >= 4) { // crown
        drawPath(Path().apply { val y = baseY - g * 0.54f; moveTo(cx - g * 0.08f, y); lineTo(cx - g * 0.08f, y - g * 0.07f); lineTo(cx - g * 0.03f, y - g * 0.03f); lineTo(cx, y - g * 0.09f); lineTo(cx + g * 0.03f, y - g * 0.03f); lineTo(cx + g * 0.08f, y - g * 0.07f); lineTo(cx + g * 0.08f, y); close() }, SK.Gold)
    }
    if (st >= 2) { // fountain water
        drawOval(Color(0xAA6EC6FF), Offset(cx - pw * 0.7f, by - g * 0.08f), Size(pw * 1.4f, g * 0.12f))
    }
}

private fun DrawScope.windmill(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors, t: Float) {
    val w = g * 0.46f; val h = g * 0.62f
    val body = Path().apply { moveTo(cx - w / 2, by); lineTo(cx - w * 0.32f, by - h); lineTo(cx + w * 0.32f, by - h); lineTo(cx + w / 2, by); close() }
    drawPath(body, Brush.horizontalGradient(listOf(c.wall.lighten(0.1f), c.wall.darken(0.3f)), cx - w / 2, cx + w / 2))
    drawPath(body, c.wall.darken(0.5f), style = Stroke(g * 0.015f))
    gableRoof(cx - w * 0.36f, by - h, w * 0.72f, g * 0.2f, c.roof)
    door(cx, by, w * 0.3f, h * 0.28f)
    if (st >= 3) window(cx - w * 0.1f, by - h * 0.65f, w * 0.2f, h * 0.15f)
    val hub = Offset(cx, by - h - g * 0.02f)
    val speed = 40f + st * 25f
    rotate(t * speed, hub) {
        for (i in 0 until 4) rotate(i * 90f, hub) {
            val len = g * (0.3f + st * 0.04f)
            drawLine(Color(0xFF6B4A2A), hub, hub + Offset(0f, -len), g * 0.025f)
            drawRect(if (st >= 5) SK.GoldLight else Color(0xFFF2E6D0), Offset(hub.x, hub.y - len), Size(g * 0.08f, len * 0.75f))
            drawRect(Color(0xFF6B4A2A), Offset(hub.x, hub.y - len), Size(g * 0.08f, len * 0.75f), style = Stroke(g * 0.01f))
        }
    }
    drawCircle(if (st >= 5) SK.Gold else Color(0xFF6B4A2A), g * 0.04f, hub)
}

private fun DrawScope.pyramid(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors) {
    val w = g * 1.0f; val h = g * (0.5f + st * 0.04f)
    val front = Path().apply { moveTo(cx - w / 2, by); lineTo(cx, by - h); lineTo(cx + w * 0.2f, by); close() }
    val side = Path().apply { moveTo(cx + w * 0.2f, by); lineTo(cx, by - h); lineTo(cx + w / 2, by - g * 0.06f); close() }
    drawPath(front, Brush.verticalGradient(listOf(c.wall.lighten(0.15f), c.wall), by - h, by))
    drawPath(side, c.wall.darken(0.3f))
    for (i in 1 until 3 + st) {
        val y = by - h * i / (3 + st)
        val half = (w / 2) * (1f - i.toFloat() / (3 + st))
        drawLine(c.wall.darken(0.35f), Offset(cx - half, y), Offset(cx + half * 0.4f, y), g * 0.008f)
    }
    if (st >= 2) door(cx - w * 0.1f, by, w * 0.1f, h * 0.18f, Color(0xFF3A2410))
    if (st >= 4) drawPath(Path().apply { moveTo(cx - w * 0.07f, by - h * 0.86f); lineTo(cx, by - h); lineTo(cx + w * 0.05f, by - h * 0.86f); close() }, SK.Gold)
    if (st >= 3) drawCircle(c.accent, g * 0.035f, Offset(cx - w * 0.06f, by - h * 0.5f))
}

private fun DrawScope.dome(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors) {
    val w = g * 0.8f; val h = g * 0.28f; val left = cx - w / 2
    box(left, by, w, h, c.wall)
    val r = w * 0.45f
    drawArc(Brush.radialGradient(listOf(c.roof.lighten(0.35f), c.roof, c.roof.darken(0.3f)), Offset(cx - r * 0.3f, by - h - r * 0.6f), r * 1.5f), 180f, 180f, true, Offset(cx - r, by - h - r), Size(r * 2, r * 2))
    drawArc(c.roof.darken(0.5f), 180f, 180f, false, Offset(cx - r, by - h - r), Size(r * 2, r * 2), style = Stroke(g * 0.015f))
    if (st >= 3) for (i in 0 until 3) drawCircle(Color(0xFFFFF3B0), g * 0.035f, Offset(left + w * (0.2f + i * 0.3f), by - h * 0.5f))
    door(cx, by, w * 0.18f, h * 0.7f)
    if (st >= 2) drawLine(c.roof.darken(0.4f), Offset(cx, by - h - r), Offset(cx, by - h - r - g * 0.12f), g * 0.02f)
    if (st >= 4) drawCircle(c.accent, g * 0.05f, Offset(cx, by - h - r - g * 0.14f))
    if (st >= 5) { goldTrim(left, by - h, w); drawCircle(SK.Gold, g * 0.06f, Offset(cx, by - h - r - g * 0.14f)) }
}

private fun DrawScope.treehouse(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors) {
    // trunk
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF9A6A3A), Color(0xFF5E3A1A))), Offset(cx - g * 0.08f, by - g * 0.55f), Size(g * 0.16f, g * 0.55f), CornerRadius(g * 0.04f))
    // canopy
    val leaf = Color(0xFF3FAE4A)
    listOf(Offset(-0.25f, -0.75f), Offset(0.25f, -0.78f), Offset(0f, -0.95f), Offset(-0.35f, -0.55f), Offset(0.35f, -0.58f)).forEach {
        drawCircle(Brush.radialGradient(listOf(leaf.lighten(0.25f), leaf.darken(0.25f)), Offset(cx + it.x * g, by + it.y * g), g * 0.25f), g * 0.22f, Offset(cx + it.x * g, by + it.y * g))
    }
    // hut on the tree
    val w = g * 0.46f; val h = g * 0.22f
    box(cx - w / 2, by - g * 0.45f, w, h, c.wall)
    gableRoof(cx - w / 2, by - g * 0.45f - h, w, g * 0.16f, c.roof)
    window(cx - w * 0.12f, by - g * 0.45f - h * 0.75f, w * 0.24f, h * 0.45f, st >= 3)
    if (st >= 2) for (i in 0 until 4) drawLine(Color(0xFF8A5A2B), Offset(cx + g * 0.12f, by - i * g * 0.1f), Offset(cx + g * 0.22f, by - i * g * 0.1f), g * 0.02f)
    if (st >= 4) flag(cx, by - g * 0.45f - h - g * 0.16f, g * 0.2f, c.accent, st >= 5)
}

private fun DrawScope.rocket(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors, t: Float) {
    val w = g * 0.3f; val h = g * (0.7f + st * 0.05f)
    // launch pad
    box(cx - g * 0.35f, by, g * 0.7f, g * 0.06f, Color(0xFF7D8894))
    drawRoundRect(Brush.horizontalGradient(listOf(c.wall.lighten(0.2f), c.wall.darken(0.3f)), cx - w / 2, cx + w / 2), Offset(cx - w / 2, by - h), Size(w, h * 0.85f), CornerRadius(w * 0.5f, w * 0.5f))
    val nose = Path().apply { moveTo(cx - w / 2, by - h * 0.8f); quadraticBezierTo(cx, by - h * 1.25f, cx + w / 2, by - h * 0.8f); close() }
    drawPath(nose, c.roof)
    listOf(-1f, 1f).forEach { sx ->
        drawPath(Path().apply { moveTo(cx + sx * w / 2, by - h * 0.35f); lineTo(cx + sx * w * 0.95f, by - g * 0.06f); lineTo(cx + sx * w / 2, by - g * 0.1f); close() }, c.roof.darken(0.1f))
    }
    drawCircle(Color(0xFF8EC9FF), w * 0.22f, Offset(cx, by - h * 0.6f))
    drawCircle(Color(0xFF3A4A5A), w * 0.22f, Offset(cx, by - h * 0.6f), style = Stroke(w * 0.06f))
    if (st >= 4) { // flame idle
        val f = 0.7f + 0.3f * sin(t * 12f)
        drawOval(Color(0xFFFFB02E).copy(alpha = 0.8f), Offset(cx - w * 0.2f, by - g * 0.09f), Size(w * 0.4f, g * 0.1f * f))
    }
    if (st >= 5) goldTrim(cx - w / 2, by - h * 0.3f, w)
    if (st >= 3) drawLine(c.accent, Offset(cx - w / 2, by - h * 0.45f), Offset(cx + w / 2, by - h * 0.45f), w * 0.08f)
}

private fun DrawScope.pagoda(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors) {
    val floors = 1 + (st + 1) / 2
    var y = by
    for (f in 0 until floors) {
        val w = g * (0.62f - f * 0.1f); val h = g * 0.18f
        box(cx - w / 2, y, w, h, c.wall)
        if (f == 0) door(cx, y, w * 0.2f, h * 0.8f, Color(0xFFB02A2A))
        else window(cx - w * 0.1f, y - h * 0.8f, w * 0.2f, h * 0.55f, st >= 3)
        y -= h
        val rw = w * 1.35f
        val roof = Path().apply { moveTo(cx - rw / 2, y + g * 0.02f); quadraticBezierTo(cx - rw * 0.3f, y - g * 0.02f, cx - w * 0.3f, y - g * 0.1f); lineTo(cx + w * 0.3f, y - g * 0.1f); quadraticBezierTo(cx + rw * 0.3f, y - g * 0.02f, cx + rw / 2, y + g * 0.02f); close() }
        drawPath(roof, c.roof)
        drawPath(roof, c.roof.darken(0.5f), style = Stroke(g * 0.012f))
        y -= g * 0.1f
    }
    drawLine(if (st >= 5) SK.Gold else c.roof.darken(0.3f), Offset(cx, y), Offset(cx, y - g * 0.15f), g * 0.025f)
    if (st >= 4) drawCircle(c.accent, g * 0.035f, Offset(cx, y - g * 0.16f))
}

private fun DrawScope.ship(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors, t: Float) {
    val bob = sin(t * 1.5f) * g * 0.015f
    val w = g * 1.0f; val h = g * 0.22f
    // water
    drawOval(Color(0x996EC6FF), Offset(cx - w * 0.6f, by - g * 0.06f), Size(w * 1.2f, g * 0.12f))
    val hull = Path().apply { moveTo(cx - w / 2, by - h + bob); lineTo(cx + w / 2, by - h + bob); lineTo(cx + w * 0.38f, by + bob); lineTo(cx - w * 0.38f, by + bob); close() }
    drawPath(hull, Brush.verticalGradient(listOf(c.wall, c.wall.darken(0.35f)), by - h, by))
    drawPath(hull, c.wall.darken(0.6f), style = Stroke(g * 0.015f))
    for (i in 0 until st) drawCircle(Color(0xFF2A1A10), g * 0.02f, Offset(cx - w * 0.3f + i * w * 0.14f, by - h * 0.5f + bob))
    val masts = if (st >= 3) 2 else 1
    for (m in 0 until masts) {
        val mx = cx + (if (masts == 2) (m - 0.5f) * w * 0.4f else 0f)
        val mh = g * (0.45f + st * 0.05f)
        drawLine(Color(0xFF6B4A2A), Offset(mx, by - h + bob), Offset(mx, by - h - mh + bob), g * 0.025f)
        val sw = g * 0.3f
        val sail = Path().apply { moveTo(mx - sw / 2, by - h - mh * 0.9f + bob); quadraticBezierTo(mx, by - h - mh * 0.55f + bob, mx + sw / 2, by - h - mh * 0.9f + bob); lineTo(mx + sw / 2, by - h - mh * 0.25f + bob); quadraticBezierTo(mx, by - h - mh * 0.1f + bob, mx - sw / 2, by - h - mh * 0.25f + bob); close() }
        drawPath(sail, if (st >= 5) SK.Cream else Color(0xFFF4EFE6))
        drawPath(sail, Color(0xFF8A7A6A), style = Stroke(g * 0.01f))
        if (st >= 4) flag(mx, by - h - mh + bob, g * 0.16f, c.accent, st >= 5)
    }
}

private fun DrawScope.lighthouse(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors, t: Float) {
    val h = g * (0.75f + st * 0.05f); val wb = g * 0.36f; val wt = g * 0.22f
    val body = Path().apply { moveTo(cx - wb / 2, by); lineTo(cx - wt / 2, by - h); lineTo(cx + wt / 2, by - h); lineTo(cx + wb / 2, by); close() }
    drawPath(body, Color.White)
    for (i in 0 until 3) {
        val y0 = by - h * (0.15f + i * 0.3f); val y1 = y0 - h * 0.13f
        val f0 = (by - y0) / h; val f1 = (by - y1) / h
        val w0 = wb + (wt - wb) * f0; val w1 = wb + (wt - wb) * f1
        drawPath(Path().apply { moveTo(cx - w0 / 2, y0); lineTo(cx - w1 / 2, y1); lineTo(cx + w1 / 2, y1); lineTo(cx + w0 / 2, y0); close() }, c.roof)
    }
    drawPath(body, Color(0xFF3A3A3A), style = Stroke(g * 0.012f))
    drawRect(Color(0xFF3A3A4A), Offset(cx - wt * 0.6f, by - h - g * 0.14f), Size(wt * 1.2f, g * 0.14f))
    val lampOn = st >= 2
    drawRect(if (lampOn) Color(0xFFFFF3B0) else Color(0xFF8EC9FF), Offset(cx - wt * 0.45f, by - h - g * 0.12f), Size(wt * 0.9f, g * 0.1f))
    drawPath(Path().apply { moveTo(cx - wt * 0.7f, by - h - g * 0.14f); lineTo(cx, by - h - g * 0.26f); lineTo(cx + wt * 0.7f, by - h - g * 0.14f); close() }, c.roof.darken(0.2f))
    if (st >= 3) {
        val a = (t * 60f) % 360f
        rotate(a, Offset(cx, by - h - g * 0.07f)) {
            drawPath(Path().apply { moveTo(cx, by - h - g * 0.07f); lineTo(cx + g * 0.6f, by - h - g * 0.17f); lineTo(cx + g * 0.6f, by - h + g * 0.03f); close() }, Color(0x55FFF3B0))
        }
    }
    door(cx, by, wb * 0.3f, h * 0.14f)
    if (st >= 5) goldTrim(cx - wt * 0.6f, by - h - g * 0.02f, wt * 1.2f)
}

private fun DrawScope.hut(cx: Float, by: Float, g: Float, st: Int, c: BuildingColors) {
    val w = g * 0.7f; val h = g * 0.3f; val left = cx - w / 2
    box(left, by, w, h, c.wall)
    // thatched roof
    val roof = Path().apply { moveTo(left - w * 0.15f, by - h); quadraticBezierTo(cx, by - h - g * 0.55f, left + w * 1.15f, by - h); close() }
    drawPath(roof, Brush.verticalGradient(listOf(c.roof.lighten(0.2f), c.roof.darken(0.2f)), by - h - g * 0.4f, by - h))
    for (i in 0 until 5) drawLine(c.roof.darken(0.4f), Offset(left + w * (0.05f + i * 0.22f), by - h), Offset(cx + (i - 2) * w * 0.05f, by - h - g * 0.3f), g * 0.01f)
    door(cx, by, w * 0.22f, h * 0.75f, Color(0xFF3A2410))
    if (st >= 3) { window(left + w * 0.72f, by - h * 0.75f, w * 0.14f, h * 0.35f); window(left + w * 0.12f, by - h * 0.75f, w * 0.14f, h * 0.35f) }
    if (st >= 2) { // fence
        for (i in 0 until 4) drawLine(Color(0xFF8A5A2B), Offset(left - w * 0.3f + i * w * 0.08f, by), Offset(left - w * 0.3f + i * w * 0.08f, by - g * 0.1f), g * 0.02f)
    }
    if (st >= 4) { // torch
        drawLine(Color(0xFF6B4A2A), Offset(left + w * 1.1f, by), Offset(left + w * 1.1f, by - g * 0.25f), g * 0.02f)
        drawCircle(Color(0xFFFF9E2E), g * 0.04f, Offset(left + w * 1.1f, by - g * 0.28f))
    }
    if (st >= 5) flag(cx, by - h - g * 0.3f, g * 0.2f, c.accent, true)
}
