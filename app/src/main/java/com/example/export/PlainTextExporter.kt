package com.example.export

import com.example.domain.model.Note
import com.example.domain.model.OutlineItem

object PlainTextExporter {

    fun exportToPlainText(note: Note, items: List<OutlineItem>): String {
        val sb = StringBuilder()
        sb.append(note.title).append("\n\n")

        val childrenMap = items.groupBy { it.parentId }
            .mapValues { (_, list) -> list.sortedBy { it.sortOrder } }

        fun appendNode(item: OutlineItem, depth: Int) {
            val indent = "    ".repeat(depth)
            val prefix = when {
                item.hasCheckbox -> if (item.isChecked) "[x] " else "[ ] "
                else -> "- "
            }
            sb.append(indent).append(prefix).append(item.text).append("\n")

            val children = childrenMap[item.id] ?: emptyList()
            for (child in children) {
                appendNode(child, depth + 1)
            }
        }

        val rootItems = childrenMap[null] ?: emptyList()
        for (root in rootItems) {
            appendNode(root, 0)
        }

        return sb.toString()
    }
}
