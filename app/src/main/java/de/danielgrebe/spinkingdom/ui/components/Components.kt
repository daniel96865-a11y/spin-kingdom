package de.danielgrebe.spinkingdom.ui.components

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.danielgrebe.spinkingdom.R
import de.danielgrebe.spinkingdom.ui.draw.darken
import de.danielgrebe.spinkingdom.ui.draw.lighten
import de.danielgrebe.spinkingdom.ui.theme.SK
import java.util.Locale

/** Compact number formatting for coins: 12.345 / 1,2 Mio. style (locale aware separators). */
fun formatNumber(n: Long): String {
    val abs = kotlin.math.abs(n)
    return when {
        abs >= 1_000_000_000_000L -> String.format(Locale.getDefault(), "%.1fT", n / 1e12)
        abs >= 1_000_000_000L -> String.format(Locale.getDefault(), "%.2fB", n / 1e9)
        abs >= 10_000_000L -> String.format(Locale.getDefault(), "%.1fM", n / 1e6)
        abs >= 1_000_000L -> String.format(Locale.getDefault(), "%.2fM", n / 1e6)
        else -> String.format(Locale.getDefault(), "%,d", n)
    }
}

fun formatTimer(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%02d:%02d", m, s)
}

val OutlinedText = TextStyle(shadow = Shadow(Color(0xAA000000), Offset(0f, 3f), 4f))

@Composable
fun GameText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 16.sp,
    color: Color = Color.White,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = MaterialTheme.typography.labelLarge
) {
    Text(text, modifier, color = color, fontSize = size, textAlign = align, maxLines = maxLines, style = style.merge(OutlinedText))
}

/** Chunky 3D mobile-game button with press animation. */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SK.Green,
    enabled: Boolean = true,
    textSize: TextUnit = 18.sp,
    height: Dp = 54.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp),
    leading: (@Composable RowScope.() -> Unit)? = null
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.94f else 1f, spring(dampingRatio = 0.45f), label = "btn")
    val base = if (enabled) color else Color(0xFF6B6680)
    Box(
        modifier
            .scale(scale)
            .height(height)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(base.darken(0.35f))
            .padding(bottom = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(base.lighten(0.35f), base, base.darken(0.1f))))
            .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        // glossy highlight
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(size.width * 0.04f, size.height * 0.08f), androidx.compose.ui.geometry.Size(size.width * 0.92f, size.height * 0.35f), androidx.compose.ui.geometry.CornerRadius(size.height * 0.2f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (leading != null) { leading(); Spacer(Modifier.width(6.dp)) }
            GameText(text, size = textSize, maxLines = 1)
        }
    }
}


/** Rounded panel with gradient and golden border. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = SK.Panel,
    border: Color = SK.Gold.copy(alpha = 0.7f),
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(color.lighten(0.12f), color.darken(0.15f))))
            .border(2.5.dp, border, RoundedCornerShape(20.dp))
            .padding(padding),
        content = content
    )
}

/** Resource pill used in the top bar: icon + animated value. */
@Composable
fun ResourcePill(icon: @Composable () -> Unit, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xAA120A30))
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { icon() }
        GameText(value, size = 14.sp, maxLines = 1)
    }
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = SK.Green, height: Dp = 14.dp, label: String? = null) {
    Box(
        modifier.height(height).clip(RoundedCornerShape(height / 2)).background(Color(0xFF120A30)).border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(height / 2)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(height).clip(RoundedCornerShape(height / 2))
                .background(Brush.verticalGradient(listOf(color.lighten(0.4f), color, color.darken(0.2f))))
        )
        if (label != null) GameText(label, Modifier.align(Alignment.Center), size = (height.value * 0.72f).sp)
    }
}

/** Standard screen frame: title bar with back button over a royal gradient. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SK.RoyalLight, SK.Royal, SK.Night)))
    ) {
        BackgroundPattern()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(onClick = onBack, contentDescription = stringResource(R.string.back)) {
                    Canvas(Modifier.size(20.dp)) {
                        drawLine(Color.White, Offset(size.width * 0.7f, size.height * 0.1f), Offset(size.width * 0.25f, size.height * 0.5f), 6f, StrokeCap.Round)
                        drawLine(Color.White, Offset(size.width * 0.25f, size.height * 0.5f), Offset(size.width * 0.7f, size.height * 0.9f), 6f, StrokeCap.Round)
                    }
                }
                GameText(title, Modifier.weight(1f).padding(horizontal = 8.dp), size = 24.sp, align = TextAlign.Center, maxLines = 1)
                if (trailing != null) trailing() else Spacer(Modifier.size(44.dp))
            }
            Box(Modifier.fillMaxSize(), content = content)
        }
    }
}

@Composable
fun BackgroundPattern() {
    Canvas(Modifier.fillMaxSize()) {
        val step = size.width / 6f
        var y = 0f; var row = 0
        while (y < size.height + step) {
            var x = if (row % 2 == 0) 0f else step / 2
            while (x < size.width + step) {
                drawPath(de.danielgrebe.spinkingdom.ui.draw.starPath(Offset(x, y), step * 0.08f, step * 0.035f, 4), Color.White.copy(alpha = 0.05f))
                x += step
            }
            y += step * 0.8f; row++
        }
    }
}

@Composable
fun RoundIconButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier, color: Color = SK.RoyalLight, size: Dp = 44.dp, content: @Composable BoxScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.5f), label = "rib")
    Box(
        modifier.scale(scale).size(size).shadow(4.dp, CircleShape).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(color.lighten(0.3f), color.darken(0.2f))))
            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Modal overlay with dimmed background, used for all popups. */
@Composable
fun GameDialog(onDismiss: (() -> Unit)?, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xCC0A0520))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.padding(20.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}, content = content)
    }
}

@Composable
fun RewardRow(icon: @Composable () -> Unit, text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(8.dp))
        GameText(text, size = 18.sp)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    GameText(text, modifier.padding(vertical = 6.dp), size = 18.sp, color = SK.GoldLight)

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color = SK.Red) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(color).border(1.5.dp, Color.White, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
        GameText(text, size = 11.sp)
    }
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))
@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))
