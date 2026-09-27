package de.danielgrebe.spinkingdom.ui.draw

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.danielgrebe.spinkingdom.ui.theme.SK

enum class MenuIcon { GIFT, WHEEL, SCROLL, CART, TV, TROPHY, HAMMER, PAW, CARDS, GEAR, PERSON, MENU, SWORD }

@Composable
fun MenuIconView(icon: MenuIcon, size: Dp = 30.dp, modifier: Modifier = Modifier) = Canvas(modifier.size(size)) {
    val s = this.size.minDimension; val c = center
    when (icon) {
        MenuIcon.GIFT -> {
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFF6B8A), SK.Red)), Offset(s * 0.15f, s * 0.4f), Size(s * 0.7f, s * 0.5f), CornerRadius(s * 0.06f))
            drawRoundRect(Color(0xFFFF8FA8), Offset(s * 0.1f, s * 0.3f), Size(s * 0.8f, s * 0.16f), CornerRadius(s * 0.05f))
            drawRect(SK.Gold, Offset(s * 0.44f, s * 0.3f), Size(s * 0.12f, s * 0.6f))
            drawOval(SK.Gold, Offset(s * 0.22f, s * 0.12f), Size(s * 0.28f, s * 0.2f), style = Stroke(s * 0.07f))
            drawOval(SK.Gold, Offset(s * 0.5f, s * 0.12f), Size(s * 0.28f, s * 0.2f), style = Stroke(s * 0.07f))
        }
        MenuIcon.WHEEL -> {
            val cols = listOf(SK.Red, SK.Gold, SK.Blue, SK.Green, SK.Purple, SK.Orange)
            for (i in 0 until 6) drawArc(cols[i], i * 60f, 60f, true, Offset(s * 0.1f, s * 0.1f), Size(s * 0.8f, s * 0.8f))
            drawCircle(Color.White, s * 0.4f, c, style = Stroke(s * 0.06f))
            drawCircle(SK.GoldDark, s * 0.08f, c)
            drawPath(Path().apply { moveTo(c.x - s * 0.1f, 0f); lineTo(c.x + s * 0.1f, 0f); lineTo(c.x, s * 0.2f); close() }, Color.White)
        }
        MenuIcon.SCROLL -> {
            drawRoundRect(Color(0xFFF5E6C8), Offset(s * 0.2f, s * 0.15f), Size(s * 0.6f, s * 0.7f), CornerRadius(s * 0.04f))
            drawRoundRect(Color(0xFFC9A66B), Offset(s * 0.12f, s * 0.08f), Size(s * 0.76f, s * 0.12f), CornerRadius(s * 0.06f))
            drawRoundRect(Color(0xFFC9A66B), Offset(s * 0.12f, s * 0.8f), Size(s * 0.76f, s * 0.12f), CornerRadius(s * 0.06f))
            for (i in 0 until 3) drawLine(Color(0xFF8A6A3A), Offset(s * 0.3f, s * (0.35f + i * 0.14f)), Offset(s * 0.7f, s * (0.35f + i * 0.14f)), s * 0.05f)
            drawPath(Path().apply { moveTo(s * 0.62f, s * 0.62f); lineTo(s * 0.7f, s * 0.72f); lineTo(s * 0.88f, s * 0.5f) }, SK.Green, style = Stroke(s * 0.08f, cap = StrokeCap.Round))
        }
        MenuIcon.CART -> {
            drawPath(Path().apply { moveTo(s * 0.08f, s * 0.2f); lineTo(s * 0.22f, s * 0.2f); lineTo(s * 0.32f, s * 0.65f); lineTo(s * 0.8f, s * 0.65f); lineTo(s * 0.9f, s * 0.32f); lineTo(s * 0.26f, s * 0.32f) }, Color.White, style = Stroke(s * 0.08f, cap = StrokeCap.Round))
            drawCircle(Color.White, s * 0.07f, Offset(s * 0.38f, s * 0.8f)); drawCircle(Color.White, s * 0.07f, Offset(s * 0.74f, s * 0.8f))
            drawCoin(Offset(s * 0.58f, s * 0.38f), s * 0.14f)
        }
        MenuIcon.TV -> {
            drawRoundRect(Color(0xFF2A2A3A), Offset(s * 0.08f, s * 0.2f), Size(s * 0.84f, s * 0.6f), CornerRadius(s * 0.1f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF8EC9FF), Color(0xFF3F7FC4))), Offset(s * 0.14f, s * 0.26f), Size(s * 0.72f, s * 0.48f), CornerRadius(s * 0.06f))
            drawPath(Path().apply { moveTo(s * 0.42f, s * 0.36f); lineTo(s * 0.64f, s * 0.5f); lineTo(s * 0.42f, s * 0.64f); close() }, Color.White)
            drawLine(Color(0xFF2A2A3A), Offset(s * 0.35f, s * 0.06f), Offset(s * 0.5f, s * 0.2f), s * 0.05f); drawLine(Color(0xFF2A2A3A), Offset(s * 0.65f, s * 0.06f), Offset(s * 0.5f, s * 0.2f), s * 0.05f)
        }
        MenuIcon.TROPHY -> {
            drawPath(Path().apply { moveTo(s * 0.25f, s * 0.12f); lineTo(s * 0.75f, s * 0.12f); lineTo(s * 0.7f, s * 0.45f); quadraticBezierTo(s * 0.5f, s * 0.62f, s * 0.3f, s * 0.45f); close() }, Brush.verticalGradient(listOf(SK.GoldLight, SK.GoldDark)))
            drawArc(SK.Gold, 90f, 180f, false, Offset(s * 0.1f, s * 0.16f), Size(s * 0.25f, s * 0.25f), style = Stroke(s * 0.06f))
            drawArc(SK.Gold, -90f, 180f, false, Offset(s * 0.65f, s * 0.16f), Size(s * 0.25f, s * 0.25f), style = Stroke(s * 0.06f))
            drawRect(SK.GoldDark, Offset(s * 0.45f, s * 0.55f), Size(s * 0.1f, s * 0.2f))
            drawRoundRect(SK.GoldDark, Offset(s * 0.28f, s * 0.74f), Size(s * 0.44f, s * 0.12f), CornerRadius(s * 0.03f))
        }
        MenuIcon.HAMMER -> drawHammer(Offset(c.x, c.y + s * 0.12f), s * 0.42f)
        MenuIcon.PAW -> {
            drawOval(Color(0xFFFF9E57), Offset(s * 0.28f, s * 0.45f), Size(s * 0.44f, s * 0.38f))
            listOf(Offset(0.2f, 0.32f), Offset(0.38f, 0.18f), Offset(0.62f, 0.18f), Offset(0.8f, 0.32f)).forEach { drawCircle(Color(0xFFFF9E57), s * 0.1f, Offset(s * it.x, s * it.y)) }
        }
        MenuIcon.CARDS -> {
            rotate(-12f, c) { drawRoundRect(SK.Blue, Offset(s * 0.2f, s * 0.15f), Size(s * 0.45f, s * 0.65f), CornerRadius(s * 0.06f)); drawRoundRect(Color.White, Offset(s * 0.2f, s * 0.15f), Size(s * 0.45f, s * 0.65f), CornerRadius(s * 0.06f), style = Stroke(s * 0.04f)) }
            rotate(10f, c) { drawRoundRect(SK.Purple, Offset(s * 0.38f, s * 0.2f), Size(s * 0.45f, s * 0.65f), CornerRadius(s * 0.06f)); drawRoundRect(Color.White, Offset(s * 0.38f, s * 0.2f), Size(s * 0.45f, s * 0.65f), CornerRadius(s * 0.06f), style = Stroke(s * 0.04f)) }
            drawPath(starPath(Offset(s * 0.6f, s * 0.52f), s * 0.12f, s * 0.05f), SK.Gold)
        }
        MenuIcon.GEAR -> {
            for (i in 0 until 8) rotate(i * 45f, c) { drawRect(Color(0xFFDADDE8), Offset(c.x - s * 0.07f, s * 0.08f), Size(s * 0.14f, s * 0.2f)) }
            drawCircle(Color(0xFFDADDE8), s * 0.3f, c); drawCircle(SK.Royal, s * 0.12f, c)
        }
        MenuIcon.PERSON -> {
            drawCircle(Color.White, s * 0.18f, Offset(c.x, s * 0.32f))
            drawArc(Color.White, 180f, 180f, true, Offset(s * 0.18f, s * 0.55f), Size(s * 0.64f, s * 0.6f))
        }
        MenuIcon.MENU -> for (i in 0 until 3) drawLine(Color.White, Offset(s * 0.2f, s * (0.3f + i * 0.2f)), Offset(s * 0.8f, s * (0.3f + i * 0.2f)), s * 0.1f, StrokeCap.Round)
        MenuIcon.SWORD -> rotate(45f, c) {
            drawRect(Brush.horizontalGradient(listOf(Color(0xFFE8EDF2), Color(0xFF8D98A6))), Offset(c.x - s * 0.06f, s * 0.05f), Size(s * 0.12f, s * 0.6f))
            drawRect(SK.Gold, Offset(c.x - s * 0.2f, s * 0.63f), Size(s * 0.4f, s * 0.08f))
            drawRect(Color(0xFF7A4A22), Offset(c.x - s * 0.05f, s * 0.7f), Size(s * 0.1f, s * 0.22f))
        }
    }
}
