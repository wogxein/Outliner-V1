package com.example.export

import com.example.domain.model.Note
import com.example.domain.model.OutlineItem

object OpmlExporter {

    fun exportToOpml(note: Note, items: List<OutlineItem>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<opml version=\"2.0\">\n")
        sb.append("  <head>\n")
        sb.append("    <title>").append(escapeXml(note.title)).append("</title>\n")
        sb.append("    <dateCreated>").append(note.createdAt).append("</dateCreated>\n")
        sb.append("    <dateModified>").append(note.updatedAt).append("</dateModified>\n")
        sb.append("  </head>\n")
        sb.append("  <body>\n")

        val childrenMap = items.groupBy { it.parentId }
            .mapValues { (_, list) -> list.sortedBy { it.sortOrder } }

        fun renderNode(item: OutlineItem, indent: String) {
            val children = childrenMap[item.id] ?: emptyList()
            val textAttr = escapeXml(item.text)
            val checkboxAttr = if (item.hasCheckbox) " _hasCheckbox=\"true\" _isChecked=\"${item.isChecked}\"" else ""
            val headingAttr = if (item.headingLevel > 0) " _headingLevel=\"${item.headingLevel}\"" else ""
            val urlAttr = if (!item.url.isNullOrEmpty()) " url=\"${escapeXml(item.url)}\"" else ""
            val collapsedAttr = if (item.isCollapsed) " _isCollapsed=\"true\"" else ""

            if (children.isEmpty()) {
                sb.append("$indent<outline text=\"$textAttr\"$checkboxAttr$headingAttr$urlAttr$collapsedAttr />\n")
            } else {
                sb.append("$indent<outline text=\"$textAttr\"$checkboxAttr$headingAttr$urlAttr$collapsedAttr>\n")
                for (child in children) {
                    renderNode(child, "$indent  ")
                }
                sb.append("$indent</outline>\n")
            }
        }

        val rootItems = childrenMap[null] ?: emptyList()
        for (root in rootItems) {
            renderNode(root, "    ")
        }

        sb.append("  </body>\n")
        sb.append("</opml>")
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
