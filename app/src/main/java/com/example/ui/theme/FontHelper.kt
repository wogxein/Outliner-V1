package com.example.ui.theme

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream

enum class AppFontFamily(val displayName: String, val key: String) {
    INTER("Inter", "inter"),
    LORA("Lora", "lora"),
    JETBRAINS_MONO("JetBrains Mono", "jetbrains_mono"),
    SYSTEM("System Default", "system"),
    CUSTOM("Custom Font (TTF/OTF)", "custom")
}

enum class AppDensity(val displayName: String, val rowVerticalPadding: Dp, val itemSpacing: Dp, val fontSizeScale: Float) {
    COMFORTABLE("Comfortable", 10.dp, 8.dp, 1.05f),
    COZY("Cozy", 6.dp, 4.dp, 1.0f),
    COMPACT("Compact", 2.dp, 2.dp, 0.92f)
}

enum class AppFontSize(val displayName: String, val baseSp: Float, val notchIndex: Int, val scale: Float) {
    EXTRA_SMALL("Extra Small", 13f, 0, 0.85f),
    SMALL("Small", 14.5f, 1, 0.92f),
    NORMAL("Normal", 16f, 2, 1.0f),
    LARGE("Large", 18f, 3, 1.15f),
    EXTRA_LARGE("Extra Large", 20.5f, 4, 1.30f);

    companion object {
        fun fromIndex(index: Int): AppFontSize = values().find { it.notchIndex == index } ?: NORMAL
        fun getScale(index: Int): Float = fromIndex(index).scale
    }
}

object FontManager {
    private var customFontFamilyCache: FontFamily? = null
    private var customFontFile: File? = null

    fun getFontFamily(context: Context, fontKey: String, customPath: String? = null): FontFamily {
        return when (fontKey.lowercase()) {
            "inter" -> FontFamily.SansSerif
            "roboto" -> FontFamily.SansSerif
            "lora" -> FontFamily.Serif
            "source_serif" -> FontFamily.Serif
            "jetbrains_mono" -> FontFamily.Monospace
            "mono" -> FontFamily.Monospace
            "system" -> FontFamily.Default
            "custom" -> {
                if (customFontFamilyCache != null) return customFontFamilyCache!!
                val fontFile = if (customPath != null) File(customPath) else File(context.filesDir, "custom_font.ttf")
                if (fontFile.exists()) {
                    try {
                        val family = FontFamily(Font(fontFile))
                        customFontFamilyCache = family
                        family
                    } catch (e: Exception) {
                        FontFamily.Default
                    }
                } else {
                    FontFamily.Default
                }
            }
            else -> FontFamily.Default
        }
    }

    fun saveCustomFont(context: Context, uri: Uri): String? {
        return try {
            val destFile = File(context.filesDir, "custom_font.ttf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            customFontFamilyCache = FontFamily(Font(destFile))
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
