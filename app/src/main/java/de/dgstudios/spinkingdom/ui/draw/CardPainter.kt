package de.dgstudios.spinkingdom.ui.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dgstudios.spinkingdom.R
import de.dgstudios.spinkingdom.config.Archetype
import de.dgstudios.spinkingdom.config.LevelConfig
import de.dgstudios.spinkingdom.domain.Cards
import de.dgstudios.spinkingdom.models.PetType
import de.dgstudios.spinkingdom.models.Rarity
import de.dgstudios.spinkingdom.models.SlotSymbol
import de.dgstudios.spinkingdom.ui.components.Badge
import de.dgstudios.spinkingdom.ui.components.GameText
import de.dgstudios.spinkingdom.ui.theme.SK

fun rarityColor(r: Rarity): Color = when (r) {
    Rarity.COMMON -> SK.Common
    Rarity.RARE -> SK.Rare
    Rarity.EPIC -> SK.Epic
    Rarity.LEGENDARY -> SK.Legendary
}

val SET_NAMES = listOf(R.array.card_set_0, R.array.card_set_1, R.array.card_set_2, R.array.card_set_3, R.array.card_set_4, R.array.card_set_5)

/** Card artwork reuses the game's painters so every card is unique: set motif + index variation. */
private fun DrawScope.cardArt(cardId: Int, c: Offset, r: Float, t: Float) {
    val set = Cards.setOf(cardId); val idx = Cards.indexOf(cardId)
    val theme = LevelConfig.THEMES[(set * 5 + idx) % LevelConfig.THEMES.size]
    when (idx % 9) {
        0 -> drawAvatar(set * 3 + idx, c, r * 0.8f)
        1 -> drawSymbol(SlotSymbol.entries[(set + idx) % SlotSymbol.entries.size], c, r * 0.8f, t)
        2 -> drawBuilding(Archetype.entries[(set * 3 + idx) % Archetype.entries.size], 5, BuildingColors.of(theme.palette, 0), c.x, c.y + r * 0.75f, r * 1.3f, t = t)
        3 -> drawPet(PetType.entries[set % 4], c, r * 0.7f, t)
        4 -> drawChestIcon(c, r * 0.7f, chestColors(de.dgstudios.spinkingdom.models.ChestType.entries[set % 5]).first, chestColors(de.dgstudios.spinkingdom.models.ChestType.entries[set % 5]).second)
        5 -> drawBuilding(Archetype.entries[(set * 5 + idx + 3) % Archetype.entries.size], 4, BuildingColors.of(theme.palette, 2), c.x, c.y + r * 0.75f, r * 1.3f, t = t)
        6 -> drawAvatar(set * 5 + idx + 1, c, r * 0.8f)
        7 -> drawPet(PetType.entries[(set + 2) % 4], c, r * 0.75f, t)
        else -> { drawCircle(Brush.radialGradient(listOf(Color(0xAAFFE680), Color.Transparent), c, r * 1.2f), r * 1.2f, c); drawJoker(c, r * 0.75f, t) }
    }
}

fun DrawScope.drawCard(cardId: Int, owned: Boolean, t: Float) {
    val rarity = Cards.rarity(cardId)
    val col = rarityColor(rarity)
    val w = size.width; val h = size.height
    drawRoundRect(Color(0x55000000), Offset(3f, 5f), Size(w, h), CornerRadius(w * 0.1f))
    if (!owned) {
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF3A3450), Color(0xFF231E36))), size = size, cornerRadius = CornerRadius(w * 0.1f))
        drawRoundRect(Color.White.copy(alpha = 0.25f), size = size, cornerRadius = CornerRadius(w * 0.1f), style = Stroke(w * 0.03f))
        return
    }
    drawRoundRect(Brush.verticalGradient(listOf(col.lighten(0.45f), col, col.darken(0.35f))), size = size, cornerRadius = CornerRadius(w * 0.1f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFFBF0), Color(0xFFE9DFC8))), Offset(w * 0.08f, h * 0.08f), Size(w * 0.84f, h * 0.6f), CornerRadius(w * 0.07f))
    cardArt(cardId, Offset(w / 2, h * 0.38f), w * 0.3f, t)
    drawRoundRect(if (rarity == Rarity.LEGENDARY) SK.GoldLight else Color.White, size = size, cornerRadius = CornerRadius(w * 0.1f), style = Stroke(w * 0.035f))
    // rarity gems
    val gems = rarity.ordinal + 1
    for (g in 0 until gems) drawPath(starPath(Offset(w / 2 + (g - (gems - 1) / 2f) * w * 0.14f, h * 0.93f), w * 0.05f, w * 0.022f), Color.White)
}

@Composable
fun CardView(cardId: Int, count: Int, t: Float, modifier: Modifier = Modifier, width: Dp = 96.dp, flip: Float = 1f) {
    val names = stringArrayResource(SET_NAMES[Cards.setOf(cardId)])
    val owned = count > 0
    Box(modifier.width(width).aspectRatio(0.7f).graphicsLayer { rotationY = (1f - flip) * 90f; cameraDistance = 12f * density }) {
        Canvas(Modifier.fillMaxSize()) { drawCard(cardId, owned, t) }
        if (owned) {
            GameText(names.getOrElse(Cards.indexOf(cardId)) { "?" }, Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = width * 0.14f, start = 4.dp, end = 4.dp), size = (width.value * 0.12f).sp, align = TextAlign.Center, maxLines = 2)
            if (count > 1) Badge("x$count", Modifier.align(Alignment.TopEnd).padding(2.dp), color = SK.Green)
        } else {
            GameText("?", Modifier.align(Alignment.Center), size = (width.value * 0.4f).sp, color = Color.White.copy(alpha = 0.4f))
        }
    }
}
