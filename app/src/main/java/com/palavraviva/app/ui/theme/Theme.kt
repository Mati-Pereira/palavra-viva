package com.palavraviva.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkScheme = darkColorScheme(
    primary = Gold,
    onPrimary = NightInk,
    secondary = GoldLight,
    background = NightInk,
    onBackground = Color(0xFFF0EDF7),
    surface = NightCard,
    onSurface = Color(0xFFF0EDF7),
    surfaceVariant = NightCard,
    onSurfaceVariant = Color(0xFF9A97B8),
    outline = NightLine,
)

private val LightScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Gold,
    background = Paper,
    onBackground = Color(0xFF1B1B33),
    surface = CardLight,
    onSurface = Color(0xFF1B1B33),
    surfaceVariant = CardLight,
    onSurfaceVariant = Muted,
    outline = LineLight,
)

// Typography — serifada para a Escritura, sans para a interface
val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 17.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.5.sp),
)

@Composable
fun PalavraVivaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content,
    )
}
