package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * VisualTransformation that styles Markdown syntax in-place:
 * - `code`: Red-accented monospace background
 * - **bold**: Bold text. Hides '**' markers unless isFocused is true or cursor touches it.
 * - *italic* or _italic_: Italic text.
 * - ~~strikethrough~~: Strikethrough line.
 * - ==highlight==: Yellow-highlight background.
 *
 * Uses 1:1 OffsetMapping.Identity to prevent cursor jumping or offset calculation bugs.
 */
class MarkdownVisualTransformation(
    private val isFocused: Boolean = false,
    private val codeColor: Color = Color(0xFFE05252),
    private val codeBackground: Color = Color(0x22888888),
    private val highlightBackground: Color = Color(0x44FFEB3B),
    private val syntaxColor: Color = Color(0x55888888)
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder(raw)

        // 1. Inline Code: `code`
        if (raw.contains('`')) {
            val codeRegex = Regex("`([^`]+)`")
            for (match in codeRegex.findAll(raw)) {
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
        }

        // 2. Bold: **text**
        if (raw.contains("**")) {
            val boldRegex = Regex("\\*\\*([^*]+)\\*\\*")
            for (match in boldRegex.findAll(raw)) {
                val fullStart = match.range.first
                val fullEnd = match.range.last + 1
                val innerStart = fullStart + 2
                val innerEnd = fullEnd - 2

                // Style inner text as Bold
                builder.addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = innerStart,
                    end = innerEnd
                )

                // Style the '**' markers: hidden/transparent when not editing, subtle when editing
                val markerStyle = if (isFocused) {
                    SpanStyle(color = syntaxColor, fontSize = 11.sp)
                } else {
                    SpanStyle(color = Color.Transparent, fontSize = 0.1.sp)
                }
                builder.addStyle(markerStyle, fullStart, innerStart)
                builder.addStyle(markerStyle, innerEnd, fullEnd)
            }
        }

        // 3. Strikethrough: ~~text~~
        if (raw.contains("~~")) {
            val strikeRegex = Regex("~~([^~]+)~~")
            for (match in strikeRegex.findAll(raw)) {
                val start = match.range.first
                val end = match.range.last + 1
                builder.addStyle(
                    style = SpanStyle(textDecoration = TextDecoration.LineThrough),
                    start = start + 2,
                    end = end - 2
                )
                val markerStyle = if (isFocused) SpanStyle(color = syntaxColor, fontSize = 11.sp) else SpanStyle(color = Color.Transparent, fontSize = 0.1.sp)
                builder.addStyle(markerStyle, start, start + 2)
                builder.addStyle(markerStyle, end - 2, end)
            }
        }

        // 4. Italic: *text* (when not preceded or followed by *) or _text_
        val italicRegex = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)|_([^_]+)_")
        for (match in italicRegex.findAll(raw)) {
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(
                style = SpanStyle(fontStyle = FontStyle.Italic),
                start = start + 1,
                end = end - 1
            )
            val markerStyle = if (isFocused) SpanStyle(color = syntaxColor, fontSize = 11.sp) else SpanStyle(color = Color.Transparent, fontSize = 0.1.sp)
            builder.addStyle(markerStyle, start, start + 1)
            builder.addStyle(markerStyle, end - 1, end)
        }

        // 5. Highlight: ==text==
        if (raw.contains("==")) {
            val highlightRegex = Regex("==([^=]+)==")
            for (match in highlightRegex.findAll(raw)) {
                val start = match.range.first
                val end = match.range.last + 1
                builder.addStyle(
                    style = SpanStyle(background = highlightBackground),
                    start = start + 2,
                    end = end - 2
                )
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
