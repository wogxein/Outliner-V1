package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Slate800,
    onPrimary = PureWhite,
    primaryContainer = Slate100,
    onPrimaryContainer = Slate900,
    secondary = BrandBlue,
    onSecondary = PureWhite,
    secondaryContainer = BrandBlueLight,
    onSecondaryContainer = BrandBlue,
    tertiary = Amber500,
    onTertiary = PureWhite,
    background = Slate50,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Rose500,
    onError = PureWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = Slate200,
    onPrimary = Slate900,
    primaryContainer = Slate800,
    onPrimaryContainer = Slate100,
    secondary = Sky500,
    onSecondary = Slate900,
    secondaryContainer = Slate700,
    onSecondaryContainer = PureWhite,
    tertiary = Amber500,
    background = Color(0xFF0B0F17),
    surface = Color(0xFF131B26),
    onBackground = Slate100,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate300,
    outline = Slate600,
    outlineVariant = Slate700,
    error = Rose500
)

@Composable
fun OutlinerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Default to Light theme as requested by spec
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
