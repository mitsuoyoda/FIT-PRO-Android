package com.fittrainer.pro.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object FitColors {
    val Green = Color(0xFF216D4D)
    val GreenDark = Color(0xFF17543A)
    val Mint = Color(0xFFEAF4ED)
    val Ink = Color(0xFF18251F)
    val Muted = Color(0xFF68766E)
    val Canvas = Color(0xFFF7F8F5)
    val Line = Color(0xFFE5E9E3)
    val Paper = Color(0xFFFFFFFF)
    val Sidebar = Color(0xFFF0F3EE)
    val SidebarBottom = Color(0xFFE5EEE5)
    val NavIdle = Color(0xFF536159)
    val NavLabel = Color(0xFF849087)
    val Orange = Color(0xFFD98545)
    val Sand = Color(0xFFF2EEE6)
    val SandInk = Color(0xFF876642)
    val StatusBg = Color(0xFFEDF5EE)
    val StatusInk = Color(0xFF39734E)
    val Dobra3Bg = Color(0xFFEAF4ED)
    val Dobra3Ink = Color(0xFF216D4D)
    val Dobra4Bg = Color(0xFFF2EEE6)
    val Dobra4Ink = Color(0xFF876642)
    val Dobra7Bg = Color(0xFFE8EEF2)
    val Dobra7Ink = Color(0xFF3D5A6C)
}

private val colors = lightColorScheme(
    primary = FitColors.Green,
    onPrimary = Color.White,
    primaryContainer = FitColors.Mint,
    onPrimaryContainer = FitColors.GreenDark,
    secondary = FitColors.GreenDark,
    onSecondary = Color.White,
    tertiary = FitColors.Orange,
    onTertiary = Color.White,
    background = FitColors.Canvas,
    onBackground = FitColors.Ink,
    surface = FitColors.Paper,
    onSurface = FitColors.Ink,
    surfaceVariant = FitColors.Sidebar,
    onSurfaceVariant = FitColors.Muted,
    outline = FitColors.Line
)

private val typography = Typography().copy(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontSize = 27.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(11.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun FitTrainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        shapes = shapes,
        typography = typography,
        content = content
    )
}
