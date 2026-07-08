package com.smithware.tidypilot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Charcoal = Color(0xFF1A1815)
val Graphite = Color(0xFF24221D)
val Cream = Color(0xFFFFF7EA)
val CleanWhite = Color(0xFFFFFCF5)
val TidyAqua = Color(0xFF28C7B5)
val TidyMint = Color(0xFFB9F2DF)
val TidyDeepTeal = Color(0xFF0F8F83)
val TidyLeaf = Color(0xFF7FBE4B)
val TidySun = Color(0xFFFFD25E)
val TidyCoral = Color(0xFFF47C73)
val Sage = TidyLeaf
val DeepSage = Color(0xFF4F8A48)
val MutedOrange = TidySun
val SoftClay = Color(0xFFFFC1A6)
val Ink = Color(0xFF171512)

private val LightColors = lightColorScheme(
    primary = TidyAqua,
    onPrimary = Ink,
    primaryContainer = TidyMint,
    onPrimaryContainer = Ink,
    secondary = TidyLeaf,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFDFF5D3),
    onSecondaryContainer = Ink,
    tertiary = TidySun,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFFFFE8A8),
    onTertiaryContainer = Ink,
    background = Cream,
    onBackground = Ink,
    surface = CleanWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE4F4EB),
    onSurfaceVariant = Graphite,
    outline = Color(0xFF78A69C)
)

private val DarkColors = darkColorScheme(
    primary = TidyAqua,
    onPrimary = Ink,
    primaryContainer = Color(0xFF12675F),
    onPrimaryContainer = Cream,
    secondary = TidyLeaf,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF2D4C2D),
    onSecondaryContainer = Cream,
    tertiary = TidySun,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF6D5520),
    onTertiaryContainer = Cream,
    background = Color(0xFF121612),
    onBackground = Cream,
    surface = Color(0xFF1C211D),
    onSurface = Cream,
    surfaceVariant = Color(0xFF26342F),
    onSurfaceVariant = Color(0xFFD2E4D8),
    outline = Color(0xFF67BDB2)
)

@Composable
fun TidyPilotTheme(
    themeMode: String,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colors: ColorScheme = if (dark) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = androidx.compose.material3.Typography(), content = content)
}
