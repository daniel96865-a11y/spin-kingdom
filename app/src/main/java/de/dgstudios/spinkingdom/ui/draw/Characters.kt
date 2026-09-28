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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.dgstudios.spinkingdom.models.ChestType
import de.dgstudios.spinkingdom.models.PetType
import de.dgstudios.spinkingdom.ui.theme.SK
import kotlin.math.sin

const val AVATAR_COUNT = 8

private val avatarBg = listOf(0xFF3AA6FF, 0xFFE8413C, 0xFF4CCB4F, 0xFFB456FF, 0xFFFF8A2B, 0xFF2EC4B6, 0xFFFF5FA8, 0xFF5B47C7)
private val skin = listOf(0xFFFFD7B0, 0xFFF1C27D, 0xFFE0AC69, 0xFFFFD7B0, 0xFFC68642, 0xFFFFE0C4, 0xFFF1C27D, 0xFF8D5524)

/** 8 hand drawn avatars: king, pirate, viking, wizard, cowboy, ninja, princess, astronaut. */
fun DrawScope.drawAvatar(index: Int, c: Offset, r: Float) {
    val i = ((index % AVATAR_COUNT) + AVATAR_COUNT) % AVATAR_COUNT
    drawCircle(Brush.radialGradient(listOf(avatarBg[i].toColor().lighten(0.3f), avatarBg[i].toColor().darken(0.2f)), c, r), r, c)
    val face = skin[i].toColor()
    val fc = c + Offset(0f, r * 0.12f)
    val fr = r * 0.52f
    // shoulders
    drawArc(avatarBg[i].toColor().darken(0.45f), 180f, 180f, true, Offset(c.x - r * 0.75f, c.y + r * 0.62f), Size(r * 1.5f, r * 0.9f))
    drawCircle(face, fr, fc)
    // eyes and smile
    drawCircle(Color.White, fr * 0.2f, fc + Offset(-fr * 0.35f, -fr * 0.1f))
    drawCircle(Color.White, fr * 0.2f, fc + Offset(fr * 0.35f, -fr * 0.1f))
    drawCircle(Color(0xFF2A1B0F), fr * 0.11f, fc + Offset(-fr * 0.32f, -fr * 0.08f))
    drawCircle(Color(0xFF2A1B0F), fr * 0.11f, fc + Offset(fr * 0.38f, -fr * 0.08f))
    drawArc(Color(0xFF8A2A1A), 20f, 140f, false, Offset(fc.x - fr * 0.35f, fc.y + fr * 0.05f), Size(fr * 0.7f, fr * 0.5f), style = Stroke(fr * 0.1f, cap = StrokeCap.Round))
    drawCircle(Color(0x33FF4A4A), fr * 0.14f, fc + Offset(-fr * 0.6f, fr * 0.25f))
    drawCircle(Color(0x33FF4A4A), fr * 0.14f, fc + Offset(fr * 0.6f, fr * 0.25f))
    val top = fc.y - fr
    when (i) {
        0 -> { // king crown + beard
            drawPath(Path().apply { moveTo(fc.x - fr * 0.8f, top + fr * 0.2f); lineTo(fc.x - fr * 0.85f, top - fr * 0.55f); lineTo(fc.x - fr * 0.4f, top - fr * 0.15f); lineTo(fc.x, top - fr * 0.7f); lineTo(fc.x + fr * 0.4f, top - fr * 0.15f); lineTo(fc.x + fr * 0.85f, top - fr * 0.55f); lineTo(fc.x + fr * 0.8f, top + fr * 0.2f); close() }, Brush.verticalGradient(listOf(SK.GoldLight, SK.GoldDark), top - fr * 0.7f, top + fr * 0.2f))
            drawCircle(SK.Red, fr * 0.12f, Offset(fc.x, top - fr * 0.05f))
            drawArc(Color(0xFF8A5A2B), 0f, 180f, true, Offset(fc.x - fr * 0.7f, fc.y + fr * 0.1f), Size(fr * 1.4f, fr * 1.1f))
            drawArc(Color(0xFF8A2A1A), 20f, 140f, false, Offset(fc.x - fr * 0.3f, fc.y + fr * 0.05f), Size(fr * 0.6f, fr * 0.4f), style = Stroke(fr * 0.1f, cap = StrokeCap.Round))
        }
        1 -> { // pirate hat + eye patch
            drawArc(Color(0xFF222222), 180f, 180f, true, Offset(fc.x - fr * 1.1f, top - fr * 0.5f), Size(fr * 2.2f, fr * 1.2f))
            drawCircle(Color.White, fr * 0.15f, Offset(fc.x, top - fr * 0.1f))
            drawCircle(Color(0xFF111111), fr * 0.22f, fc + Offset(fr * 0.36f, -fr * 0.1f))
            drawLine(Color(0xFF111111), fc + Offset(-fr * 0.9f, -fr * 0.5f), fc + Offset(fr * 0.9f, fr * 0.05f), fr * 0.07f)
        }
        2 -> { // viking helmet with horns + braids
            drawArc(Color(0xFF9AA5B1), 180f, 180f, true, Offset(fc.x - fr, top - fr * 0.1f), Size(fr * 2, fr * 1.4f))
            drawRect(Color(0xFF6B7580), Offset(fc.x - fr, top + fr * 0.5f), Size(fr * 2, fr * 0.15f))
            listOf(-1f, 1f).forEach { s -> drawPath(Path().apply { moveTo(fc.x + s * fr * 0.8f, top + fr * 0.3f); quadraticBezierTo(fc.x + s * fr * 1.5f, top, fc.x + s * fr * 1.2f, top - fr * 0.7f); lineTo(fc.x + s * fr * 0.95f, top); close() }, Color(0xFFF5E6C8)) }
            drawRoundRect(Color(0xFFE8A33A), Offset(fc.x - fr * 1.05f, fc.y), Size(fr * 0.25f, fr * 0.9f), CornerRadius(fr * 0.1f))
            drawRoundRect(Color(0xFFE8A33A), Offset(fc.x + fr * 0.8f, fc.y), Size(fr * 0.25f, fr * 0.9f), CornerRadius(fr * 0.1f))
        }
        3 -> { // wizard hat + white beard
            drawPath(Path().apply { moveTo(fc.x - fr * 1.1f, top + fr * 0.35f); lineTo(fc.x + fr * 0.2f, top - fr * 1.4f); lineTo(fc.x + fr * 1.1f, top + fr * 0.35f); close() }, Color(0xFF4B2F9E))
            drawPath(starPath(Offset(fc.x + fr * 0.05f, top - fr * 0.3f), fr * 0.2f, fr * 0.08f), SK.Gold)
            drawArc(Color.White, 0f, 180f, true, Offset(fc.x - fr * 0.75f, fc.y), Size(fr * 1.5f, fr * 1.4f))
        }
        4 -> { // cowboy hat
            drawOval(Color(0xFF8A5A2B), Offset(fc.x - fr * 1.35f, top + fr * 0.05f), Size(fr * 2.7f, fr * 0.5f))
            drawRoundRect(Color(0xFFA0692F), Offset(fc.x - fr * 0.65f, top - fr * 0.6f), Size(fr * 1.3f, fr * 0.8f), CornerRadius(fr * 0.3f))
            drawRect(Color(0xFF5A3A1A), Offset(fc.x - fr * 0.65f, top), Size(fr * 1.3f, fr * 0.14f))
        }
        5 -> { // ninja mask
            drawCircle(Color(0xFF2A2A3A), fr * 1.05f, fc)
            drawRoundRect(face, Offset(fc.x - fr * 0.8f, fc.y - fr * 0.35f), Size(fr * 1.6f, fr * 0.5f), CornerRadius(fr * 0.25f))
            drawCircle(Color(0xFF2A1B0F), fr * 0.12f, fc + Offset(-fr * 0.32f, -fr * 0.1f))
            drawCircle(Color(0xFF2A1B0F), fr * 0.12f, fc + Offset(fr * 0.32f, -fr * 0.1f))
            drawRect(SK.Red, Offset(fc.x - fr * 1.05f, fc.y - fr * 0.6f), Size(fr * 2.1f, fr * 0.16f))
        }
        6 -> { // princess hair + tiara
            drawArc(Color(0xFFFFD447), 180f, 180f, true, Offset(fc.x - fr * 1.1f, top - fr * 0.1f), Size(fr * 2.2f, fr * 1.4f))
            drawRoundRect(Color(0xFFFFD447), Offset(fc.x - fr * 1.1f, fc.y - fr * 0.3f), Size(fr * 0.35f, fr * 1.3f), CornerRadius(fr * 0.15f))
            drawRoundRect(Color(0xFFFFD447), Offset(fc.x + fr * 0.75f, fc.y - fr * 0.3f), Size(fr * 0.35f, fr * 1.3f), CornerRadius(fr * 0.15f))
            drawPath(Path().apply { moveTo(fc.x - fr * 0.5f, top + fr * 0.15f); lineTo(fc.x - fr * 0.25f, top - fr * 0.25f); lineTo(fc.x, top + fr * 0.05f); lineTo(fc.x + fr * 0.25f, top - fr * 0.25f); lineTo(fc.x + fr * 0.5f, top + fr * 0.15f); close() }, Color(0xFFE6F0FF))
            drawCircle(Color(0xFFFF5FA8), fr * 0.08f, Offset(fc.x, top - fr * 0.02f))
        }
        else -> { // astronaut helmet
            drawCircle(Color.White.copy(alpha = 0.25f), fr * 1.25f, fc)
            drawCircle(Color.White, fr * 1.25f, fc, style = Stroke(fr * 0.18f))
            drawArc(Color.White.copy(alpha = 0.7f), 200f, 60f, false, Offset(fc.x - fr, fc.y - fr), Size(fr * 2, fr * 2), style = Stroke(fr * 0.1f))
        }
    }
    drawCircle(Color.White, r, c, style = Stroke(r * 0.08f))
}

@Composable
fun Avatar(index: Int, size: Dp = 48.dp, modifier: Modifier = Modifier) =
    Canvas(modifier.size(size)) { drawAvatar(index, center, this.size.minDimension / 2f) }

/** Pets: fox, dragon, raccoon, phoenix. [t] animates breathing/wings. */
fun DrawScope.drawPet(type: PetType, c: Offset, r: Float, t: Float = 0f, locked: Boolean = false) {
    val breathe = 1f + 0.03f * sin(t * 3f)
    val tint: (Color) -> Color = { if (locked) Color(0xFF3A3450) else it }
    drawOval(Color(0x33000000), Offset(c.x - r * 0.7f, c.y + r * 0.72f), Size(r * 1.4f, r * 0.25f))
    when (type) {
        PetType.FOX -> {
            val o = tint(Color(0xFFFF8A2B))
            drawPath(Path().apply { moveTo(c.x + r * 0.4f, c.y + r * 0.5f); quadraticBezierTo(c.x + r * 1.2f, c.y + r * 0.2f + sin(t * 2) * r * 0.1f, c.x + r * 0.9f, c.y - r * 0.4f); quadraticBezierTo(c.x + r * 0.8f, c.y + r * 0.2f, c.x + r * 0.2f, c.y + r * 0.3f); close() }, o)
            drawCircle(tint(Color.White), r * 0.12f, Offset(c.x + r * 0.9f, c.y - r * 0.35f))
            drawOval(o, Offset(c.x - r * 0.5f, c.y - r * 0.05f * breathe), Size(r * 1.0f, r * 0.8f * breathe))
            drawCircle(o, r * 0.42f, Offset(c.x - r * 0.1f, c.y - r * 0.35f))
            listOf(-1f, 1f).forEach { s -> drawPath(Path().apply { moveTo(c.x - r * 0.1f + s * r * 0.15f, c.y - r * 0.65f); lineTo(c.x - r * 0.1f + s * r * 0.38f, c.y - r * 1.0f); lineTo(c.x - r * 0.1f + s * r * 0.42f, c.y - r * 0.5f); close() }, o) }
            drawOval(tint(Color.White), Offset(c.x - r * 0.35f, c.y - r * 0.3f), Size(r * 0.5f, r * 0.3f))
            if (!locked) { eyes(c + Offset(-r * 0.1f, -r * 0.42f), r * 0.2f); drawCircle(Color.Black, r * 0.06f, Offset(c.x - r * 0.1f, c.y - r * 0.2f)) }
        }
        PetType.DRAGON -> {
            val g = tint(Color(0xFF3FBF5A))
            val wing = sin(t * 4f) * r * 0.15f
            drawPath(Path().apply { moveTo(c.x, c.y - r * 0.1f); lineTo(c.x + r * 0.9f, c.y - r * 0.8f - wing); lineTo(c.x + r * 0.6f, c.y); close() }, tint(Color(0xFF2E8A45)))
            drawPath(Path().apply { moveTo(c.x, c.y - r * 0.1f); lineTo(c.x - r * 0.9f, c.y - r * 0.8f - wing); lineTo(c.x - r * 0.6f, c.y); close() }, tint(Color(0xFF2E8A45)))
            drawOval(g, Offset(c.x - r * 0.5f, c.y - r * 0.2f), Size(r, r * 0.95f * breathe))
            drawOval(tint(Color(0xFFFFE08A)), Offset(c.x - r * 0.28f, c.y), Size(r * 0.56f, r * 0.65f))
            drawCircle(g, r * 0.4f, Offset(c.x, c.y - r * 0.5f))
            listOf(-1f, 1f).forEach { s -> drawPath(Path().apply { moveTo(c.x + s * r * 0.2f, c.y - r * 0.8f); lineTo(c.x + s * r * 0.3f, c.y - r * 1.1f); lineTo(c.x + s * r * 0.35f, c.y - r * 0.75f); close() }, tint(Color(0xFFFFE08A))) }
            if (!locked) eyes(c + Offset(0f, -r * 0.55f), r * 0.2f)
        }
        PetType.RACCOON -> {
            val gr = tint(Color(0xFF8D8F9A))
            for (k in 0 until 4) drawCircle(if (k % 2 == 0) gr else tint(Color(0xFF3A3A44)), r * 0.2f, Offset(c.x + r * (0.55f + k * 0.12f), c.y + r * (0.35f - k * 0.18f)))
            drawOval(gr, Offset(c.x - r * 0.5f, c.y - r * 0.1f), Size(r, r * 0.85f * breathe))
            drawCircle(gr, r * 0.45f, Offset(c.x, c.y - r * 0.4f))
            listOf(-1f, 1f).forEach { s -> drawCircle(gr, r * 0.15f, Offset(c.x + s * r * 0.33f, c.y - r * 0.78f)) }
            drawOval(tint(Color(0xFF2A2A33)), Offset(c.x - r * 0.42f, c.y - r * 0.55f), Size(r * 0.84f, r * 0.24f))
            drawOval(tint(Color.White), Offset(c.x - r * 0.2f, c.y - r * 0.32f), Size(r * 0.4f, r * 0.25f))
            if (!locked) { eyes(c + Offset(0f, -r * 0.44f), r * 0.2f); drawCircle(Color.Black, r * 0.06f, Offset(c.x, c.y - r * 0.24f)) }
        }
        PetType.PHOENIX -> {
            val f = sin(t * 5f) * r * 0.12f
            for (k in 0 until 3) drawPath(Path().apply { moveTo(c.x, c.y + r * 0.3f); quadraticBezierTo(c.x + (k - 1) * r * 0.6f, c.y + r * 0.9f, c.x + (k - 1) * r * 0.5f, c.y + r * 1.0f); lineTo(c.x + (k - 1) * r * 0.2f, c.y + r * 0.35f); close() }, tint(listOf(Color(0xFFFF4A1F), Color(0xFFFFB02E), Color(0xFFFF4A1F))[k]))
            drawPath(Path().apply { moveTo(c.x, c.y - r * 0.1f); quadraticBezierTo(c.x + r * 1.1f, c.y - r * 0.9f - f, c.x + r * 1.0f, c.y - r * 0.1f); close() }, tint(Color(0xFFFF7A1F)))
            drawPath(Path().apply { moveTo(c.x, c.y - r * 0.1f); quadraticBezierTo(c.x - r * 1.1f, c.y - r * 0.9f - f, c.x - r * 1.0f, c.y - r * 0.1f); close() }, tint(Color(0xFFFF7A1F)))
            drawOval(Brush.verticalGradient(listOf(tint(Color(0xFFFFD447)), tint(Color(0xFFFF5A1F)))), Offset(c.x - r * 0.4f, c.y - r * 0.3f), Size(r * 0.8f, r * 0.8f * breathe))
            drawCircle(tint(Color(0xFFFFB02E)), r * 0.32f, Offset(c.x, c.y - r * 0.55f))
            drawPath(Path().apply { moveTo(c.x - r * 0.05f, c.y - r * 0.45f); lineTo(c.x + r * 0.25f, c.y - r * 0.4f); lineTo(c.x - r * 0.05f, c.y - r * 0.35f); close() }, tint(Color(0xFFFFE08A)))
            for (k in 0 until 3) drawLine(tint(Color(0xFFFF4A1F)), Offset(c.x, c.y - r * 0.85f), Offset(c.x + (k - 1) * r * 0.2f, c.y - r * 1.15f), r * 0.07f, StrokeCap.Round)
            if (!locked) eyes(c + Offset(-r * 0.05f, -r * 0.6f), r * 0.16f)
        }
    }
}

private fun DrawScope.eyes(c: Offset, s: Float) {
    listOf(-1f, 1f).forEach { k ->
        drawCircle(Color.White, s * 0.55f, c + Offset(k * s, 0f))
        drawCircle(Color(0xFF1A1A1A), s * 0.32f, c + Offset(k * s + s * 0.08f, s * 0.05f))
        drawCircle(Color.White, s * 0.12f, c + Offset(k * s + s * 0.15f, -s * 0.1f))
    }
}

fun chestColors(type: ChestType): Pair<Color, Color> = when (type) {
    ChestType.WOOD -> Color(0xFFB0682C) to Color(0xFF7A7A7A)
    ChestType.SILVER -> Color(0xFF8C9BAD) to Color(0xFFE6ECF2)
    ChestType.GOLD -> Color(0xFFD9901A) to Color(0xFFFFF1A8)
    ChestType.ROYAL -> Color(0xFF6D3FB0) to SK.Gold
    ChestType.LEGENDARY -> Color(0xFFE8413C) to Color(0xFFFFE066)
}
