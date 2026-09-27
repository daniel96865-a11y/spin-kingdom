package de.danielgrebe.spinkingdom.config

import de.danielgrebe.spinkingdom.R

/** Visual archetypes drawn procedurally with Compose Canvas. */
enum class Archetype { COTTAGE, TOWER, CASTLE, TENT, STATUE, WINDMILL, PYRAMID, DOME, TREEHOUSE, ROCKET, PAGODA, SHIP, LIGHTHOUSE, HUT }

/** Background decoration families. */
enum class Decoration { CLOUDS, STARS, SNOW, BUBBLES, PALMS, PINES, CACTI, LAVA, CANDY, GHOSTS, SAND, BLOSSOM }

data class Palette(
    val skyTop: Long,
    val skyBottom: Long,
    val ground: Long,
    val groundDark: Long,
    val wall: Long,
    val roof: Long,
    val accent: Long
)

data class ThemeConfig(
    val index: Int,
    val nameRes: Int,
    val buildingNamesRes: Int,
    val palette: Palette,
    val decoration: Decoration,
    val archetypes: List<Archetype>
)

/** Everything needed to render and play one level. */
data class LevelDefinition(
    val level: Int,
    val theme: ThemeConfig,
    /** 0 for the 30 hand configured levels, 1.. for procedurally generated repetitions. */
    val cycle: Int,
    val palette: Palette,
    val archetypes: List<Archetype>
) {
    val upgradeCosts: List<List<Long>> = List(GameBalanceConfig.BUILDINGS_PER_LEVEL) { b ->
        List(GameBalanceConfig.MAX_STAGE) { s -> GameBalanceConfig.upgradeCost(level, b, s + 1) }
    }
}

object LevelConfig {
    const val MAX_LEVEL = 500

    private fun p(skyTop: Long, skyBottom: Long, ground: Long, groundDark: Long, wall: Long, roof: Long, accent: Long) =
        Palette(skyTop, skyBottom, ground, groundDark, wall, roof, accent)

    private val A = Archetype.entries.associateBy { it.name }
    private fun a(vararg names: String) = names.map { A.getValue(it) }

    val THEMES: List<ThemeConfig> = listOf(
        ThemeConfig(0, R.string.theme_0, R.array.buildings_0, p(0xFF5EC8FF, 0xFFBDEBFF, 0xFF6CC24A, 0xFF3F8F2E, 0xFFF4E1C1, 0xFFD9453B, 0xFFFFC93C), Decoration.CLOUDS, a("COTTAGE", "WINDMILL", "TENT", "TOWER", "CASTLE")),
        ThemeConfig(1, R.string.theme_1, R.array.buildings_1, p(0xFF2FB8E8, 0xFFA8F0FF, 0xFFF2D38B, 0xFFC9A55C, 0xFFB5824F, 0xFF7A3E1D, 0xFFE8322E), Decoration.PALMS, a("HUT", "TENT", "SHIP", "LIGHTHOUSE", "TOWER")),
        ThemeConfig(2, R.string.theme_2, R.array.buildings_2, p(0xFF6D8FB5, 0xFFCFE0EE, 0xFF7FA56B, 0xFF4E7043, 0xFF9C6B3F, 0xFF5B3A21, 0xFF4AA3DF), Decoration.PINES, a("HUT", "STATUE", "SHIP", "TOWER", "CASTLE")),
        ThemeConfig(3, R.string.theme_3, R.array.buildings_3, p(0xFFFF9E57, 0xFFFFE0A3, 0xFFE0A15E, 0xFFB5773C, 0xFFC98B55, 0xFF7C4A26, 0xFFD6453D), Decoration.CACTI, a("TENT", "COTTAGE", "WINDMILL", "TOWER", "STATUE")),
        ThemeConfig(4, R.string.theme_4, R.array.buildings_4, p(0xFF46B98A, 0xFFB9F0C9, 0xFF3E9E47, 0xFF25702E, 0xFF9E7B4F, 0xFF4F8A2C, 0xFFFF7F2A), Decoration.PALMS, a("HUT", "TREEHOUSE", "STATUE", "TOWER", "PYRAMID")),
        ThemeConfig(5, R.string.theme_5, R.array.buildings_5, p(0xFFFFB347, 0xFFFFE8B0, 0xFFEBC77A, 0xFFC49B4E, 0xFFE8CF95, 0xFFC7963E, 0xFF2B7BD6), Decoration.SAND, a("TENT", "DOME", "TOWER", "STATUE", "PYRAMID")),
        ThemeConfig(6, R.string.theme_6, R.array.buildings_6, p(0xFF7DC4F0, 0xFFE6F6FF, 0xFFEAF6FF, 0xFFB8D8EE, 0xFFD6EEFF, 0xFF6FB3E8, 0xFF9B7BFF), Decoration.SNOW, a("HUT", "DOME", "STATUE", "TOWER", "CASTLE")),
        ThemeConfig(7, R.string.theme_7, R.array.buildings_7, p(0xFF3B2B6E, 0xFF8C6FD1, 0xFF3F8F5A, 0xFF28603A, 0xFFB99BE0, 0xFF6D3FB0, 0xFF6CFFB0), Decoration.STARS, a("COTTAGE", "DOME", "TREEHOUSE", "STATUE", "TOWER")),
        ThemeConfig(8, R.string.theme_8, R.array.buildings_8, p(0xFF0E5E9C, 0xFF3FC1D9, 0xFFE3CF9A, 0xFFB9A26B, 0xFF6ED6C8, 0xFFFF7F9E, 0xFFFFD447), Decoration.BUBBLES, a("DOME", "STATUE", "SHIP", "TOWER", "CASTLE")),
        ThemeConfig(9, R.string.theme_9, R.array.buildings_9, p(0xFF0B0F2E, 0xFF3A2F7A, 0xFF8A8FA8, 0xFF5B6078, 0xFFD8DEE9, 0xFF4E7BFF, 0xFFFF5DA2), Decoration.STARS, a("DOME", "STATUE", "TOWER", "ROCKET", "CASTLE")),
        ThemeConfig(10, R.string.theme_10, R.array.buildings_10, p(0xFF8E2B2B, 0xFFFF9E6B, 0xFF6B4A3A, 0xFF473026, 0xFF8C8C8C, 0xFFB5302A, 0xFFFFB02E), Decoration.LAVA, a("HUT", "DOME", "STATUE", "TOWER", "CASTLE")),
        ThemeConfig(11, R.string.theme_11, R.array.buildings_11, p(0xFFFFB6C9, 0xFFFFF0F3, 0xFF8CC063, 0xFF5E8E3C, 0xFFF6EBD9, 0xFF3A3A4A, 0xFFE0314B), Decoration.BLOSSOM, a("COTTAGE", "TENT", "STATUE", "TOWER", "PAGODA")),
        ThemeConfig(12, R.string.theme_12, R.array.buildings_12, p(0xFFFF9ED8, 0xFFFFE3F4, 0xFFFFC1E3, 0xFFE58DC0, 0xFFC98A5A, 0xFFFF5FA8, 0xFF74E0FF), Decoration.CANDY, a("COTTAGE", "TENT", "DOME", "STATUE", "TOWER")),
        ThemeConfig(13, R.string.theme_13, R.array.buildings_13, p(0xFFFFA86B, 0xFFFFE1A8, 0xFF7FAE45, 0xFF557A2B, 0xFFA88763, 0xFF6B8F3A, 0xFFE8652E), Decoration.PALMS, a("HUT", "TREEHOUSE", "DOME", "STATUE", "TOWER")),
        ThemeConfig(14, R.string.theme_14, R.array.buildings_14, p(0xFF2B2D42, 0xFF6C6F8F, 0xFF4F5A48, 0xFF343B30, 0xFF8D8574, 0xFF3F3A52, 0xFF9CFFB5), Decoration.GHOSTS, a("COTTAGE", "WINDMILL", "STATUE", "TOWER", "CASTLE")),
        ThemeConfig(15, R.string.theme_15, R.array.buildings_15, p(0xFF6FA8DC, 0xFFD4E6F5, 0xFF7BAF5A, 0xFF52803A, 0xFFBFB6A4, 0xFF3F5FA8, 0xFFE8B83A), Decoration.CLOUDS, a("COTTAGE", "TENT", "WINDMILL", "TOWER", "CASTLE")),
        ThemeConfig(16, R.string.theme_16, R.array.buildings_16, p(0xFFE85D3A, 0xFFFFC27A, 0xFF4A3A36, 0xFF2E2422, 0xFF7C6A62, 0xFF3E3230, 0xFFFF7A1F), Decoration.LAVA, a("HUT", "DOME", "STATUE", "TOWER", "PYRAMID")),
        ThemeConfig(17, R.string.theme_17, R.array.buildings_17, p(0xFFB28DFF, 0xFFFFE0F7, 0xFF8FD694, 0xFF5FAE66, 0xFFFFF4E0, 0xFF5E7CE2, 0xFFFFC0E0), Decoration.BLOSSOM, a("COTTAGE", "TREEHOUSE", "STATUE", "TOWER", "CASTLE")),
        ThemeConfig(18, R.string.theme_18, R.array.buildings_18, p(0xFF1E3A5F, 0xFF5C8DB8, 0xFF7D8894, 0xFF535D69, 0xFFB8C4CF, 0xFF2E86C1, 0xFF3CFFEA), Decoration.STARS, a("DOME", "STATUE", "TOWER", "ROCKET", "CASTLE")),
        ThemeConfig(19, R.string.theme_19, R.array.buildings_19, p(0xFF8FD3FF, 0xFFFFFFFF, 0xFFF4F8FF, 0xFFCAD8EE, 0xFFFFFFFF, 0xFFFFB84D, 0xFF7FD6FF), Decoration.CLOUDS, a("DOME", "WINDMILL", "STATUE", "TOWER", "CASTLE")),
        ThemeConfig(20, R.string.theme_20, R.array.buildings_20, p(0xFFFFB84D, 0xFFFFEAC2, 0xFFEFD08E, 0xFFC9A55C, 0xFFF1E3C6, 0xFF2E9E8F, 0xFF3AB0FF), Decoration.SAND, a("TENT", "DOME", "STATUE", "TOWER", "PYRAMID")),
        ThemeConfig(21, R.string.theme_21, R.array.buildings_21, p(0xFF8ED1FC, 0xFFE8F8FF, 0xFF8BD66B, 0xFF5FAE45, 0xFFFFF1D6, 0xFFFF7EB6, 0xFFFFE14D), Decoration.BLOSSOM, a("COTTAGE", "TENT", "WINDMILL", "TREEHOUSE", "STATUE")),
        ThemeConfig(22, R.string.theme_22, R.array.buildings_22, p(0xFF1A1033, 0xFF46307A, 0xFF4B3F66, 0xFF30284A, 0xFF9AD8FF, 0xFFB26BFF, 0xFF6BFFF0), Decoration.STARS, a("DOME", "STATUE", "TOWER", "PYRAMID", "CASTLE")),
        ThemeConfig(23, R.string.theme_23, R.array.buildings_23, p(0xFFB08968, 0xFFEAD7C3, 0xFF8C7B6B, 0xFF5E5146, 0xFFB87333, 0xFF5A4636, 0xFFFFC94A), Decoration.CLOUDS, a("COTTAGE", "WINDMILL", "DOME", "TOWER", "ROCKET")),
        ThemeConfig(24, R.string.theme_24, R.array.buildings_24, p(0xFF3D2C8D, 0xFFE0A3FF, 0xFFD94F4F, 0xFFA83232, 0xFFFFF4E6, 0xFFE63946, 0xFFFFD60A), Decoration.CANDY, a("TENT", "COTTAGE", "DOME", "STATUE", "TOWER")),
        ThemeConfig(25, R.string.theme_25, R.array.buildings_25, p(0xFF034078, 0xFF1282A2, 0xFFD9C8A0, 0xFFA8966E, 0xFFE6F2F2, 0xFF1FBFB8, 0xFFFFD166), Decoration.BUBBLES, a("DOME", "STATUE", "TOWER", "PYRAMID", "CASTLE")),
        ThemeConfig(26, R.string.theme_26, R.array.buildings_26, p(0xFF7FB2FF, 0xFFFFF8E1, 0xFFF5F0E6, 0xFFD2C8B4, 0xFFFFFFFF, 0xFFE8C15A, 0xFF6F9CEB), Decoration.CLOUDS, a("STATUE", "DOME", "TOWER", "PYRAMID", "CASTLE")),
        ThemeConfig(27, R.string.theme_27, R.array.buildings_27, p(0xFF0F2A3F, 0xFF2EC4B6, 0xFFE8F1F2, 0xFFB6CCD6, 0xFFC7A27C, 0xFF3D5A80, 0xFF7CFFB2), Decoration.SNOW, a("HUT", "DOME", "STATUE", "TOWER", "LIGHTHOUSE")),
        ThemeConfig(28, R.string.theme_28, R.array.buildings_28, p(0xFF2D1E4F, 0xFF8E6CC4, 0xFF6BA368, 0xFF467A45, 0xFFF3E9D2, 0xFFE63946, 0xFFFFE066), Decoration.STARS, a("DOME", "COTTAGE", "TREEHOUSE", "STATUE", "TOWER")),
        ThemeConfig(29, R.string.theme_29, R.array.buildings_29, p(0xFFFFA62B, 0xFFFFF1C1, 0xFFE9C46A, 0xFFC49A3C, 0xFFFFD86B, 0xFFB5838D, 0xFF2A9D8F), Decoration.SAND, a("STATUE", "DOME", "TOWER", "PYRAMID", "CASTLE"))
    )

    val CONFIGURED_LEVELS: Int get() = THEMES.size

    /** Levels 1..30 are hand configured; beyond that themes repeat with shifted palettes and rotated buildings. */
    fun level(level: Int): LevelDefinition {
        val l = level.coerceIn(1, MAX_LEVEL)
        val idx = (l - 1) % THEMES.size
        val cycle = (l - 1) / THEMES.size
        val theme = THEMES[idx]
        if (cycle == 0) return LevelDefinition(l, theme, 0, theme.palette, theme.archetypes)
        val pal = theme.palette.let {
            Palette(
                shiftHue(it.skyTop, cycle * 23f), shiftHue(it.skyBottom, cycle * 23f),
                shiftHue(it.ground, cycle * 17f), shiftHue(it.groundDark, cycle * 17f),
                shiftHue(it.wall, cycle * 31f), shiftHue(it.roof, cycle * 41f), shiftHue(it.accent, cycle * 53f)
            )
        }
        val pool = Archetype.entries
        val arch = theme.archetypes.mapIndexed { i, a ->
            if ((i + cycle) % 3 == 0) pool[(a.ordinal + cycle * 5 + i) % pool.size] else a
        }
        return LevelDefinition(l, theme, cycle, pal, arch)
    }

    /** Pure-Kotlin HSV hue rotation so procedural levels get their own color identity. */
    fun shiftHue(argb: Long, degrees: Float): Long {
        val a = (argb shr 24) and 0xFF
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = max - min
        var h = when {
            d == 0f -> 0f
            max == r -> 60f * (((g - b) / d) % 6f)
            max == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }
        if (h < 0) h += 360f
        val s = if (max == 0f) 0f else d / max
        val v = max
        h = (h + degrees) % 360f
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60f) % 2f - 1))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(v: Float) = ((v + m) * 255f).toInt().coerceIn(0, 255).toLong()
        return (a shl 24) or (ch(r1) shl 16) or (ch(g1) shl 8) or ch(b1)
    }
}
