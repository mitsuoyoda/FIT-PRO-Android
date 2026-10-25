package com.fittrainer.pro.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(11.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(18.dp)
)

@Composable
fun FitTrainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        shapes = shapes,
        content = content
    )
}
