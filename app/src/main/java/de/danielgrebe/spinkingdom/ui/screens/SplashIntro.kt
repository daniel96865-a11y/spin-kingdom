package de.danielgrebe.spinkingdom.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.danielgrebe.spinkingdom.R
import de.danielgrebe.spinkingdom.config.GameBalanceConfig
import de.danielgrebe.spinkingdom.config.LevelConfig
import de.danielgrebe.spinkingdom.models.SlotSymbol
import de.danielgrebe.spinkingdom.ui.components.BackgroundPattern
import de.danielgrebe.spinkingdom.ui.components.GameButton
import de.danielgrebe.spinkingdom.ui.components.GameText
import de.danielgrebe.spinkingdom.ui.components.Panel
import de.danielgrebe.spinkingdom.ui.components.ProgressBar
import de.danielgrebe.spinkingdom.ui.components.RewardRow
import de.danielgrebe.spinkingdom.ui.components.formatNumber
import de.danielgrebe.spinkingdom.ui.draw.AVATAR_COUNT
import de.danielgrebe.spinkingdom.ui.draw.Avatar
import de.danielgrebe.spinkingdom.ui.draw.CoinIcon
import de.danielgrebe.spinkingdom.ui.draw.EnergyIcon
import de.danielgrebe.spinkingdom.ui.draw.drawCoin
import de.danielgrebe.spinkingdom.ui.draw.drawSymbol
import de.danielgrebe.spinkingdom.ui.draw.starPath
import de.danielgrebe.spinkingdom.ui.theme.SK
import kotlin.math.sin

/** Game logo: crown over a golden coin with sparkles. */
@Composable
fun Logo(t: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = center; val r = size.minDimension * 0.3f
        drawCircle(Brush.radialGradient(listOf(Color(0x88FFE680), Color.Transparent), c, r * 2.2f), r * 2.2f, c)
        for (i in 0 until 12) {
            val a = (i * 30f + t * 20f) * Math.PI.toFloat() / 180f
            drawLine(SK.GoldLight.copy(alpha = 0.25f), c, c + Offset(kotlin.math.cos(a) * r * 2f, sin(a) * r * 2f), r * 0.12f)
        }
        drawCoin(c, r)
        // crown on top
        val w = r * 1.5f; val h = r * 0.8f; val left = c.x - w / 2; val top = c.y - r - h * 0.75f
        val crown = androidx.compose.ui.graphics.Path().apply {
            moveTo(left, top + h); lineTo(left - w * 0.05f, top + h * 0.15f); lineTo(left + w * 0.25f, top + h * 0.55f)
            lineTo(left + w * 0.5f, top); lineTo(left + w * 0.75f, top + h * 0.55f); lineTo(left + w * 1.05f, top + h * 0.15f); lineTo(left + w, top + h); close()
        }
        drawPath(crown, Brush.verticalGradient(listOf(SK.GoldLight, SK.Gold, SK.GoldDark), top, top + h))
        drawPath(crown, SK.GoldDark, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.05f))
        listOf(SK.Red, SK.Blue, SK.Green).forEachIndexed { i, col -> drawCircle(col, r * 0.09f, Offset(left + w * (0.25f + i * 0.25f), top + h * 0.72f)) }
        for (i in 0 until 5) {
            val ph = (t * 0.8f + i * 0.2f) % 1f
            val a = sin(ph * Math.PI.toFloat())
            drawPath(starPath(c + Offset(r * 1.4f * kotlin.math.cos(i * 1.3f), r * 1.2f * sin(i * 1.9f)), r * 0.16f * a + 1f, r * 0.06f * a + 0.5f, 4), Color.White.copy(alpha = a))
        }
    }
}

@Composable
fun SplashScreen(loaded: Boolean, t: Float, onDone: () -> Unit) {
    val scale = remember { Animatable(0.2f) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, tween(700, easing = CubicBezierEasing(0.3f, 1.6f, 0.5f, 1f))) }
    LaunchedEffect(Unit) { progress.animateTo(0.85f, tween(1300, easing = LinearEasing)) }
    LaunchedEffect(loaded, progress.value >= 0.85f) {
        if (loaded && progress.value >= 0.85f) { progress.animateTo(1f, tween(250)); onDone() }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SK.RoyalLight, SK.Royal, SK.Night))).testTag("splash"), contentAlignment = Alignment.Center) {
        BackgroundPattern()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(t, Modifier.size(220.dp).scale(scale.value))
            GameText(stringResource(R.string.app_name), Modifier.scale(scale.value), size = 44.sp, color = SK.GoldLight)
            GameText(stringResource(R.string.tagline), size = 15.sp, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(40.dp))
            ProgressBar(progress.value, Modifier.width(240.dp), color = SK.Gold, height = 18.dp)
            Spacer(Modifier.height(8.dp))
            GameText(stringResource(R.string.loading), size = 13.sp)
        }
    }
}

@Composable
fun IntroScreen(t: Float, onFinish: (name: String?, avatar: Int) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SK.RoyalLight, SK.Royal, SK.Night))).testTag("intro")) {
        BackgroundPattern()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0 until 3) Box(Modifier.size(if (i == page) 14.dp else 10.dp).clip(CircleShape).background(if (i == page) SK.Gold else Color.White.copy(alpha = 0.4f)))
            }
            AnimatedContent(page, transitionSpec = { (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut()) }, modifier = Modifier.weight(1f), label = "intro") { p ->
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    when (p) {
                        0 -> {
                            val def = LevelConfig.level(1)
                            Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(24.dp)).border(3.dp, SK.Gold, RoundedCornerShape(24.dp))) {
                                VillageCanvas(def, listOf(5, 3, 4, 2, 1), List(5) { false }, t)
                            }
                            Spacer(Modifier.height(20.dp))
                            GameText(stringResource(R.string.intro1_title), size = 28.sp, align = TextAlign.Center, color = SK.GoldLight)
                            Spacer(Modifier.height(8.dp))
                            GameText(stringResource(R.string.intro1_text), size = 16.sp, align = TextAlign.Center)
                        }
                        1 -> {
                            Canvas(Modifier.fillMaxWidth().height(200.dp)) {
                                val syms = listOf(SlotSymbol.COIN, SlotSymbol.ATTACK, SlotSymbol.RAID, SlotSymbol.SHIELD, SlotSymbol.ENERGY, SlotSymbol.CHEST, SlotSymbol.JOKER)
                                syms.forEachIndexed { i, s ->
                                    val row = i / 4; val col = i % 4
                                    val cols = if (row == 0) 4 else 3
                                    val x = size.width * (col + 0.5f) / cols + if (row == 1) 0f else 0f
                                    val y = size.height * (0.28f + row * 0.48f) + sin(t * 3 + i) * 6f
                                    drawSymbol(s, Offset(x, y), size.minDimension * 0.2f, t)
                                }
                            }
                            GameText(stringResource(R.string.intro2_title), size = 28.sp, align = TextAlign.Center, color = SK.GoldLight)
                            Spacer(Modifier.height(8.dp))
                            GameText(stringResource(R.string.intro2_text), size = 15.sp, align = TextAlign.Center)
                        }
                        else -> {
                            GameText(stringResource(R.string.intro3_title), size = 28.sp, align = TextAlign.Center, color = SK.GoldLight)
                            Spacer(Modifier.height(12.dp))
                            Avatar(avatar, 110.dp)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (i in 0 until AVATAR_COUNT) Box(Modifier.clip(CircleShape).border(if (i == avatar) 3.dp else 0.dp, SK.Gold, CircleShape).clickable { avatar = i }.testTag("avatar_$i")) { Avatar(i, 38.dp) }
                            }
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = name, onValueChange = { name = it.take(16) }, singleLine = true,
                                label = { Text(stringResource(R.string.player_name)) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = SK.Gold, unfocusedBorderColor = Color.White.copy(alpha = 0.6f), focusedLabelColor = SK.Gold, unfocusedLabelColor = Color.White),
                                modifier = Modifier.fillMaxWidth().testTag("name_field")
                            )
                            Spacer(Modifier.height(14.dp))
                            Panel(Modifier.fillMaxWidth()) {
                                GameText(stringResource(R.string.start_gifts), size = 16.sp, color = SK.GoldLight)
                                RewardRow({ CoinIcon(26.dp) }, formatNumber(GameBalanceConfig.START_COINS) + " " + stringResource(R.string.coins))
                                RewardRow({ EnergyIcon(26.dp) }, "${GameBalanceConfig.START_SPINS} " + stringResource(R.string.free_spins))
                            }
                        }
                    }
                }
            }
            if (page < 2) {
                GameButton(stringResource(R.string.next), { page++ }, Modifier.fillMaxWidth().testTag("intro_next"))
            } else {
                GameButton(stringResource(R.string.lets_go), { onFinish(name.ifBlank { null }, avatar) }, Modifier.fillMaxWidth().testTag("intro_start"), enabled = name.isNotBlank())
                Spacer(Modifier.height(8.dp))
                GameButton(stringResource(R.string.play_as_guest), { onFinish(null, avatar) }, Modifier.fillMaxWidth().testTag("intro_guest"), color = SK.Blue)
            }
        }
    }
}
