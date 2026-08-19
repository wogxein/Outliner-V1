package com.example.ui.theme

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

fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(r, g, b, 255)
            }
            8 -> {
                val a = clean.substring(0, 2).toInt(16)
                val r = clean.substring(2, 4).toInt(16)
                val g = clean.substring(4, 6).toInt(16)
                val b = clean.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}

fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

@Composable
fun OutlinerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    customPrimaryHex: String? = null,
    customBgHex: String? = null,
    fontFamily: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) {
        val bg = if (!customBgHex.isNullOrBlank()) parseHexColor(customBgHex, Color(0xFF0B0F17)) else Color(0xFF0B0F17)
        val primary = if (!customPrimaryHex.isNullOrBlank()) parseHexColor(customPrimaryHex, Slate200) else Slate200
        darkColorScheme(
            primary = primary,
            onPrimary = if (primary.luminance() > 0.5f) Slate900 else PureWhite,
            primaryContainer = primary.copy(alpha = 0.2f),
            onPrimaryContainer = PureWhite,
            secondary = Sky500,
            onSecondary = Slate900,
            secondaryContainer = Slate700,
            onSecondaryContainer = PureWhite,
            tertiary = Amber500,
            background = bg,
            surface = if (!customBgHex.isNullOrBlank()) bg.copy(alpha = 0.95f) else Color(0xFF131B26),
            onBackground = Slate100,
            onSurface = Slate100,
            surfaceVariant = Slate800,
            onSurfaceVariant = Slate300,
            outline = Slate600,
            outlineVariant = Slate700,
            error = Rose500
        )
    } else {
        val bg = if (!customBgHex.isNullOrBlank()) parseHexColor(customBgHex, Slate50) else Slate50
        val primary = if (!customPrimaryHex.isNullOrBlank()) parseHexColor(customPrimaryHex, Slate800) else Slate800
        lightColorScheme(
            primary = primary,
            onPrimary = if (primary.luminance() > 0.5f) Slate900 else PureWhite,
            primaryContainer = primary.copy(alpha = 0.12f),
            onPrimaryContainer = Slate900,
            secondary = BrandBlue,
            onSecondary = PureWhite,
            secondaryContainer = BrandBlueLight,
            onSecondaryContainer = BrandBlue,
            tertiary = Amber500,
            onTertiary = PureWhite,
            background = bg,
            onBackground = Slate900,
            surface = if (!customBgHex.isNullOrBlank()) bg else PureWhite,
            onSurface = Slate900,
            surfaceVariant = Slate100,
            onSurfaceVariant = Slate700,
            outline = Slate300,
            outlineVariant = Slate200,
            error = Rose500,
            onError = PureWhite
        )
    }

    val dynamicTypography = Typography(
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
    )

    MaterialTheme(
        colorScheme = baseScheme,
        typography = dynamicTypography,
        content = content
    )
}

fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
