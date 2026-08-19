package com.example.importer

import com.example.domain.model.OutlineItem
import java.util.UUID

object MarkdownImporter {

    data class ParsedMarkdown(
        val title: String,
        val items: List<OutlineItem>
    )

    fun parseMarkdown(content: String, noteId: String): ParsedMarkdown {
        val lines = content.lines()
        var noteTitle = "Untitled Note"
        val rawItems = mutableListOf<RawLineItem>()
        var foundTitle = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // The very first non-empty line becomes the note title / heading
            if (!foundTitle) {
                var extractedTitle = trimmed
                if (extractedTitle.startsWith("# ")) {
                    extractedTitle = extractedTitle.removePrefix("# ").trim()
                } else if (extractedTitle.startsWith("- ") || extractedTitle.startsWith("* ")) {
                    extractedTitle = extractedTitle.substring(2).trim()
                }
                noteTitle = if (extractedTitle.isNotBlank()) extractedTitle else "Untitled Note"
                foundTitle = true
                continue
            }

            // Calculate indentation
            val leadingSpaces = line.takeWhile { it == ' ' || it == '\t' }
            val indentLevel = leadingSpaces.count { it == '\t' } * 2 + (leadingSpaces.count { it == ' ' } / 2)

            var lineText = trimmed
            var hasCheckbox = false
            var isChecked = false
            var headingLevel = 0

            if (lineText.startsWith("- [x] ") || lineText.startsWith("* [x] ")) {
                hasCheckbox = true
                isChecked = true
                lineText = lineText.substring(6).trim()
            } else if (lineText.startsWith("- [ ] ") || lineText.startsWith("* [ ] ")) {
                hasCheckbox = true
                isChecked = false
                lineText = lineText.substring(6).trim()
            } else if (lineText.startsWith("- ") || lineText.startsWith("* ") || lineText.startsWith("+ ")) {
                lineText = lineText.substring(2).trim()
            } else if (lineText.startsWith("### ")) {
                headingLevel = 3
                lineText = lineText.removePrefix("### ").trim()
            } else if (lineText.startsWith("## ")) {
                headingLevel = 2
                lineText = lineText.removePrefix("## ").trim()
            } else if (lineText.startsWith("# ")) {
                headingLevel = 1
                lineText = lineText.removePrefix("# ").trim()
            }

            var isBold = false
            var isItalic = false
            var isStrikethrough = false
            var isCode = false

            if (lineText.startsWith("**") && lineText.endsWith("**") && lineText.length >= 4) {
                isBold = true
                lineText = lineText.removeSurrounding("**")
            }
            if (lineText.startsWith("*") && lineText.endsWith("*") && lineText.length >= 2) {
                isItalic = true
                lineText = lineText.removeSurrounding("*")
            }
            if (lineText.startsWith("~~") && lineText.endsWith("~~") && lineText.length >= 4) {
                isStrikethrough = true
                lineText = lineText.removeSurrounding("~~")
            }
            if (lineText.startsWith("`") && lineText.endsWith("`") && lineText.length >= 2) {
                isCode = true
                lineText = lineText.removeSurrounding("`")
            }

            rawItems.add(
                RawLineItem(
                    text = lineText,
                    indentLevel = indentLevel,
                    hasCheckbox = hasCheckbox,
                    isChecked = isChecked,
                    headingLevel = headingLevel,
                    isBold = isBold,
                    isItalic = isItalic,
                    isStrikethrough = isStrikethrough,
                    isCode = isCode
                )
            )
        }

        if (rawItems.isEmpty()) {
            return ParsedMarkdown(
                title = noteTitle,
                items = listOf(
                    OutlineItem(
                        id = UUID.randomUUID().toString(),
                        noteId = noteId,
                        parentId = null,
                        sortOrder = 0,
                        text = ""
                    )
                )
            )
        }

        // Build parent-child relationships based on indentation level
        val items = mutableListOf<OutlineItem>()
        val parentStack = mutableListOf<Pair<Int, String>>() // indentLevel to itemId
        val sortOrderMap = mutableMapOf<String?, Int>()

        for (raw in rawItems) {
            val itemId = UUID.randomUUID().toString()

            // Pop stack until finding a parent with indentLevel < current
            while (parentStack.isNotEmpty() && parentStack.last().first >= raw.indentLevel) {
                parentStack.removeAt(parentStack.size - 1)
            }

            val parentId = if (parentStack.isNotEmpty()) parentStack.last().second else null
            val currentOrder = sortOrderMap[parentId] ?: 0
            sortOrderMap[parentId] = currentOrder + 1

            val outlineItem = OutlineItem(
                id = itemId,
                noteId = noteId,
                parentId = parentId,
                sortOrder = currentOrder,
                text = raw.text,
                hasCheckbox = raw.hasCheckbox,
                isChecked = raw.isChecked,
                headingLevel = raw.headingLevel,
                isBold = raw.isBold,
                isItalic = raw.isItalic,
                isStrikethrough = raw.isStrikethrough,
                isCode = raw.isCode
            )
            items.add(outlineItem)
            parentStack.add(Pair(raw.indentLevel, itemId))
        }

        return ParsedMarkdown(title = noteTitle, items = items)
    }

    private data class RawLineItem(
        val text: String,
        val indentLevel: Int,
        val hasCheckbox: Boolean,
        val isChecked: Boolean,
        val headingLevel: Int,
        val isBold: Boolean,
        val isItalic: Boolean,
        val isStrikethrough: Boolean,
        val isCode: Boolean
    )
}
