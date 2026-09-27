package de.danielgrebe.spinkingdom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object SK {
    val Gold = Color(0xFFFFC83D)
    val GoldDark = Color(0xFFD9901A)
    val GoldLight = Color(0xFFFFE9A0)
    val Royal = Color(0xFF3B2A8C)
    val RoyalDark = Color(0xFF231763)
    val RoyalLight = Color(0xFF5B47C7)
    val Night = Color(0xFF160E3D)
    val Panel = Color(0xFF2C1F6E)
    val PanelLight = Color(0xFF3E2F8F)
    val Red = Color(0xFFE8413C)
    val RedDark = Color(0xFFA8231F)
    val Green = Color(0xFF4CCB4F)
    val GreenDark = Color(0xFF238A2B)
    val Blue = Color(0xFF3AA6FF)
    val BlueDark = Color(0xFF1A63C4)
    val Purple = Color(0xFFB456FF)
    val Orange = Color(0xFFFF8A2B)
    val Cream = Color(0xFFFFF6E0)
    val TextDark = Color(0xFF2A1B0F)
    val Common = Color(0xFF9AA5B1)
    val Rare = Color(0xFF3AA6FF)
    val Epic = Color(0xFFB456FF)
    val Legendary = Color(0xFFFFB02E)
}

private val colors = darkColorScheme(
    primary = SK.Gold,
    onPrimary = SK.TextDark,
    secondary = SK.RoyalLight,
    background = SK.Night,
    surface = SK.Panel,
    onSurface = Color.White,
    onBackground = Color.White,
    error = SK.Red
)

private val base = TextStyle(fontFamily = FontFamily.SansSerif, color = Color.White)

private val typography = Typography(
    displayLarge = base.copy(fontSize = 44.sp, fontWeight = FontWeight.Black),
    headlineLarge = base.copy(fontSize = 30.sp, fontWeight = FontWeight.Black),
    headlineMedium = base.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = base.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = base.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
    bodyLarge = base.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyMedium = base.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodySmall = base.copy(fontSize = 12.sp),
    labelLarge = base.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
    labelMedium = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
    labelSmall = base.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
)

@Composable
fun SpinKingdomTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
