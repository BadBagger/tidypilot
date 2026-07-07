package com.smithware.mvpstarter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Charcoal = Color(0xFF1F1E1B)
val Graphite = Color(0xFF32302B)
val WarmOrange = Color(0xFFFF8A3D)
val Cream = Color(0xFFF7EBD3)
val Lime = Color(0xFFC9F24D)
val Ink = Color(0xFF151412)

private val LightColors = lightColorScheme(
    primary = WarmOrange,
    onPrimary = Ink,
    secondary = Lime,
    onSecondary = Ink,
    tertiary = Graphite,
    background = Cream,
    onBackground = Ink,
    surface = Color(0xFFFFF8EA),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9DDC8),
    onSurfaceVariant = Graphite,
    outline = Color(0xFF8B8172)
)

private val DarkColors = darkColorScheme(
    primary = WarmOrange,
    onPrimary = Ink,
    secondary = Lime,
    onSecondary = Ink,
    tertiary = Cream,
    background = Color(0xFF141311),
    onBackground = Cream,
    surface = Charcoal,
    onSurface = Cream,
    surfaceVariant = Graphite,
    onSurfaceVariant = Color(0xFFD6CCB8),
    outline = Color(0xFF8D8374)
)

@Composable
fun MvpStarterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors: ColorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = androidx.compose.material3.Typography(), content = content)
}
