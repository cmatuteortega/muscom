package com.muscom.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.muscom.app.data.ThemeMode

object Brand {
    val Felt = Color(0xFF0E5A36)
    val FeltDark = Color(0xFF08331F)
    val FeltLight = Color(0xFF1B7A4A)
    val Gold = Color(0xFFE0A526)
    val GoldDark = Color(0xFF9C6A10)
    val Burgundy = Color(0xFF8E1B1B)
    val Cream = Color(0xFFFFFBF1)
    val Ink = Color(0xFF1E1A16)
    val Oros = Color(0xFFD99A1E)
    val Copas = Color(0xFFB8322A)
    val Espadas = Color(0xFF2E5FA8)
    val Bastos = Color(0xFF2F7A3A)
    val Wood = Color(0xFF7A4E2C)
    val TeamUs = Color(0xFF3FA7D6)
    val TeamThem = Color(0xFFE76F51)
}

/** Colores propios de la mesa, que cambian con alto contraste / modo oscuro. */
@Immutable
data class TableColors(
    val feltCenter: Color,
    val feltEdge: Color,
    val cardFace: Color,
    val cardInk: Color,
    val cardBack: Color,
    val cardBackAccent: Color,
    val teamUs: Color,
    val teamThem: Color,
    val highlight: Color,
    val onFelt: Color,
    val suit: List<Color>,
)

val LocalTableColors = staticCompositionLocalOf {
    TableColors(
        Brand.FeltLight, Brand.FeltDark, Brand.Cream, Brand.Ink, Brand.Burgundy, Brand.Gold,
        Brand.TeamUs, Brand.TeamThem, Brand.Gold, Color.White,
        listOf(Brand.Oros, Brand.Copas, Brand.Espadas, Brand.Bastos),
    )
}

private val Light = lightColorScheme(
    primary = Brand.Felt,
    onPrimary = Color.White,
    secondary = Brand.Burgundy,
    onSecondary = Color.White,
    tertiary = Brand.GoldDark,
    background = Color(0xFFF7F3EA),
    surface = Color(0xFFFFFCF5),
    surfaceVariant = Color(0xFFEDE6D6),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF6FD39B),
    onPrimary = Color(0xFF00391D),
    secondary = Color(0xFFFFB3A8),
    tertiary = Brand.Gold,
    background = Color(0xFF111412),
    surface = Color(0xFF181C19),
    surfaceVariant = Color(0xFF2A302C),
)
private val HighContrastLight = lightColorScheme(
    primary = Color(0xFF003D1F),
    onPrimary = Color.White,
    secondary = Color(0xFF5A0000),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
)
private val HighContrastDark = darkColorScheme(
    primary = Color(0xFFFFE066),
    onPrimary = Color.Black,
    secondary = Color(0xFF9AE6FF),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color.White,
    outline = Color.White,
)

private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.2.sp),
    )
}

@Composable
fun MusTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    highContrast: Boolean = false,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme: ColorScheme = when {
        highContrast && dark -> HighContrastDark
        highContrast -> HighContrastLight
        dark -> Dark
        else -> Light
    }
    val table = when {
        highContrast -> TableColors(
            feltCenter = Color(0xFF0A3D24), feltEdge = Color(0xFF021A0E), cardFace = Color.White, cardInk = Color.Black,
            cardBack = Color(0xFF5A0000), cardBackAccent = Color(0xFFFFE066), teamUs = Color(0xFF00C8FF),
            teamThem = Color(0xFFFF8A3D), highlight = Color(0xFFFFE066), onFelt = Color.White,
            suit = listOf(Color(0xFFB07800), Color(0xFFC00000), Color(0xFF0040C0), Color(0xFF006010)),
        )
        dark -> TableColors(
            feltCenter = Color(0xFF145C38), feltEdge = Color(0xFF051F13), cardFace = Color(0xFFF3EDE0), cardInk = Brand.Ink,
            cardBack = Color(0xFF6E1414), cardBackAccent = Brand.Gold, teamUs = Brand.TeamUs, teamThem = Brand.TeamThem,
            highlight = Brand.Gold, onFelt = Color.White,
            suit = listOf(Brand.Oros, Brand.Copas, Brand.Espadas, Brand.Bastos),
        )
        else -> LocalTableColors.current
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalTableColors provides table,
        LocalDensity provides Density(density.density, density.fontScale * textScale),
    ) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
