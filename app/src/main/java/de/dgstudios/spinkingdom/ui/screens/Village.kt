package de.dgstudios.spinkingdom.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import de.dgstudios.spinkingdom.R
import de.dgstudios.spinkingdom.config.LevelDefinition
import de.dgstudios.spinkingdom.ui.draw.BuildingColors
import de.dgstudios.spinkingdom.ui.draw.drawBuilding
import de.dgstudios.spinkingdom.ui.draw.drawScenery
import de.dgstudios.spinkingdom.ui.theme.SK

/** Relative building anchor points (bottom center) inside the village area, back row first. */
val BUILDING_SLOTS = listOf(
    Offset(0.5f, 0.56f), Offset(0.2f, 0.66f), Offset(0.8f, 0.66f), Offset(0.32f, 0.9f), Offset(0.7f, 0.9f)
)

fun buildingSize(w: Float, h: Float) = minOf(w * 0.3f, h * 0.36f)

fun buildingAnchor(i: Int, w: Float, h: Float) = Offset(BUILDING_SLOTS[i].x * w, BUILDING_SLOTS[i].y * h)

/** Draws the village of [def] with the given building stages. */
@Composable
fun VillageCanvas(
    def: LevelDefinition,
    stages: List<Int>,
    damaged: List<Boolean>,
    t: Float,
    modifier: Modifier = Modifier.fillMaxSize(),
    bounce: (Int) -> Float = { 1f },
    highlight: Int? = null,
    shieldDome: Float = 0f,
    onTap: ((Int) -> Unit)? = null
) {
    val tapMod = if (onTap != null) Modifier.pointerInput(def.level, stages) {
        detectTapGestures { pos ->
            val w = size.width.toFloat(); val h = size.height.toFloat(); val s = buildingSize(w, h)
            val hit = BUILDING_SLOTS.indices.reversed().firstOrNull { i ->
                val a = buildingAnchor(i, w, h)
                pos.x in (a.x - s * 0.6f)..(a.x + s * 0.6f) && pos.y in (a.y - s * 1.15f)..(a.y + s * 0.1f)
            }
            if (hit != null) onTap(hit)
        }
    } else Modifier
    Canvas(modifier.then(tapMod)) {
        drawScenery(def.palette, def.theme.decoration, t)
        val w = size.width; val h = size.height; val s = buildingSize(w, h)
        // dirt path connecting the buildings
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h); cubicTo(w * 0.45f, h * 0.85f, w * 0.55f, h * 0.7f, w * 0.5f, h * 0.56f)
        }
        drawPath(path, Color(0x55FFF3D0), style = Stroke(w * 0.07f))
        stages.indices.sortedBy { BUILDING_SLOTS[it].y }.forEach { i ->
            val a = buildingAnchor(i, w, h)
            if (highlight == i) drawCircle(SK.Red.copy(alpha = 0.35f), s * 0.6f, Offset(a.x, a.y - s * 0.4f))
            scale(bounce(i), bounce(i), pivot = a) {
                drawBuilding(def.archetypes[i], stages[i], BuildingColors.of(def.palette, i), a.x, a.y, s, damaged.getOrElse(i) { false }, t + i * 0.37f)
            }
        }
        if (shieldDome > 0f) {
            val c = Offset(w / 2, h * 0.72f)
            drawCircle(Color(0xFF7FD3FF).copy(alpha = 0.25f * shieldDome), w * 0.62f * shieldDome, c)
            drawCircle(Color.White.copy(alpha = 0.7f * shieldDome), w * 0.62f * shieldDome, c, style = Stroke(6f))
            drawCircle(Color(0xFF7FD3FF).copy(alpha = 0.5f * shieldDome), w * 0.55f * shieldDome, c, style = Stroke(3f))
        }
    }
}

private val ROMAN = listOf("", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII")

@Composable
fun levelName(def: LevelDefinition): String {
    val base = stringResource(def.theme.nameRes)
    return if (def.cycle == 0) base else "$base ${ROMAN.getOrElse(def.cycle) { (def.cycle + 1).toString() }}"
}

@Composable
fun buildingNames(def: LevelDefinition): Array<String> = stringArrayResource(def.theme.buildingNamesRes)

@Composable
fun stageName(stage: Int): String = stringArrayResource(R.array.stage_names).getOrElse(stage) { "" }
