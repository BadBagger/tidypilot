package com.smithware.tidypilot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Charcoal = Color(0xFF252420)
val Graphite = Color(0xFF34332E)
val Cream = Color(0xFFF7EBDD)
val CleanWhite = Color(0xFFFFFCF7)
val Sage = Color(0xFF87A878)
val DeepSage = Color(0xFF4F6F52)
val MutedOrange = Color(0xFFD8894C)
val SoftClay = Color(0xFFE7C7A8)
val Ink = Color(0xFF181715)

private val LightColors = lightColorScheme(
    primary = MutedOrange,
    onPrimary = Ink,
    secondary = DeepSage,
    onSecondary = Ink,
    tertiary = Sage,
    background = Cream,
    onBackground = Ink,
    surface = CleanWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9DFCF),
    onSurfaceVariant = Graphite,
    outline = Color(0xFF8C8171)
)

private val DarkColors = darkColorScheme(
    primary = MutedOrange,
    onPrimary = Ink,
    secondary = Sage,
    onSecondary = Ink,
    tertiary = SoftClay,
    background = Color(0xFF12110F),
    onBackground = Cream,
    surface = Color(0xFF1F1E1A),
    onSurface = Cream,
    surfaceVariant = Color(0xFF2B2A25),
    onSurfaceVariant = Color(0xFFD7CCBC),
    outline = Color(0xFF9A8B78)
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
