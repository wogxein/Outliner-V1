package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class InlineCodeVisualTransformation(
    private val codeColor: Color = Color(0xFFE05252),
    private val codeBackground: Color = Color(0x22888888)
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (!raw.contains('`')) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder(raw)
        
        // Find all spans between ` and `
        val regex = Regex("`([^`]+)`")
        val matches = regex.findAll(raw)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(
                style = SpanStyle(
                    color = codeColor,
                    background = codeBackground,
                    fontFamily = FontFamily.Monospace
                ),
                start = start,
                end = end
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
