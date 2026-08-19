package com.example.export

import android.util.Xml
import com.example.domain.model.OutlineItem
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.UUID

object OpmlImporter {

    data class ParsedOpmlResult(
        val title: String,
        val items: List<OutlineItem>
    )

    fun parseOpml(opmlContent: String, targetNoteId: String): ParsedOpmlResult {
        var noteTitle = "Imported Outline"
        val items = mutableListOf<OutlineItem>()
        val parentStack = mutableListOf<String>() // Stack of parent IDs
        val sortOrderMap = mutableMapOf<String?, Int>() // ParentId -> nextSortOrder

        val parser = Xml.newPullParser()
        parser.setInput(StringReader(opmlContent))

        var eventType = parser.eventType
        var inHead = false
        var inTitle = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name?.lowercase()) {
                        "head" -> inHead = true
                        "title" -> if (inHead) inTitle = true
                        "outline" -> {
                            val text = parser.getAttributeValue(null, "text")
                                ?: parser.getAttributeValue(null, "_text")
                                ?: parser.getAttributeValue(null, "title")
                                ?: ""
                            val hasCheckbox = parser.getAttributeValue(null, "_hasCheckbox")?.toBoolean() ?: false
                            val isChecked = parser.getAttributeValue(null, "_isChecked")?.toBoolean() ?: false
                            val headingLevel = parser.getAttributeValue(null, "_headingLevel")?.toIntOrNull() ?: 0
                            val url = parser.getAttributeValue(null, "url") ?: parser.getAttributeValue(null, "xmlUrl")
                            val isCollapsed = parser.getAttributeValue(null, "_isCollapsed")?.toBoolean() ?: false

                            val currentParentId = parentStack.lastOrNull()
                            val currentSortOrder = sortOrderMap[currentParentId] ?: 0
                            sortOrderMap[currentParentId] = currentSortOrder + 1

                            val itemId = UUID.randomUUID().toString()
                            val item = OutlineItem(
                                id = itemId,
                                noteId = targetNoteId,
                                parentId = currentParentId,
                                sortOrder = currentSortOrder,
                                text = text,
                                hasCheckbox = hasCheckbox,
                                isChecked = isChecked,
                                headingLevel = headingLevel,
                                url = url,
                                isCollapsed = isCollapsed,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            items.add(item)
                            parentStack.add(itemId)
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inTitle) {
                        val parsed = parser.text?.trim()
                        if (!parsed.isNullOrEmpty()) {
                            noteTitle = parsed
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name?.lowercase()) {
                        "head" -> inHead = false
                        "title" -> inTitle = false
                        "outline" -> {
                            if (parentStack.isNotEmpty()) {
                                parentStack.removeAt(parentStack.size - 1)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return ParsedOpmlResult(title = noteTitle, items = items)
    }
}
