package com.example.export

import com.example.domain.model.Note
import com.example.domain.model.OutlineItem

object HtmlExporter {

    fun exportToHtml(note: Note, items: List<OutlineItem>): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
        sb.append("<title>").append(escapeHtml(note.title)).append("</title>\n")
        sb.append("""
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #1E293B; max-width: 800px; margin: 0 auto; padding: 2rem; background: #FAFAFA; }
                h1.title { color: #0F172A; border-bottom: 2px solid #E2E8F0; padding-bottom: 0.5rem; margin-bottom: 1.5rem; font-size: 2rem; }
                ul { list-style-type: disc; padding-left: 1.5rem; margin: 0.25rem 0; }
                li { margin: 0.35rem 0; }
                .checkbox { margin-right: 0.4rem; }
                .heading-1 { font-size: 1.4rem; font-weight: bold; }
                .heading-2 { font-size: 1.2rem; font-weight: bold; }
                .heading-3 { font-size: 1.05rem; font-weight: bold; }
                code { background: #F1F5F9; padding: 0.1rem 0.3rem; border-radius: 4px; font-family: monospace; font-size: 0.9em; }
                a { color: #2563EB; text-decoration: none; }
                a:hover { text-decoration: underline; }
            </style>
        """.trimIndent())
        sb.append("\n</head>\n<body>\n")
        sb.append("<h1 class=\"title\">").append(escapeHtml(note.title)).append("</h1>\n")

        val childrenMap = items.groupBy { it.parentId }
            .mapValues { (_, list) -> list.sortedBy { it.sortOrder } }

        fun renderNode(item: OutlineItem) {
            sb.append("<li>")
            var text = escapeHtml(item.text)
            if (item.isBold) text = "<strong>$text</strong>"
            if (item.isItalic) text = "<em>$text</em>"
            if (item.isStrikethrough) text = "<del>$text</del>"
            if (item.isCode) text = "<code>$text</code>"

            val styleParts = mutableListOf<String>()
            item.textColor?.let { styleParts.add("color: $it;") }
            item.backgroundColor?.let { styleParts.add("background-color: $it; padding: 2px 4px; border-radius: 3px;") }
            val styleAttr = if (styleParts.isNotEmpty()) " style=\"${styleParts.joinToString(" ")}\"" else ""

            val headingClass = when (item.headingLevel) {
                1 -> " class=\"heading-1\""
                2 -> " class=\"heading-2\""
                3 -> " class=\"heading-3\""
                else -> ""
            }

            val prefix = when {
                item.hasCheckbox -> "<input type=\"checkbox\" class=\"checkbox\" ${if (item.isChecked) "checked" else ""} disabled> "
                else -> ""
            }

            sb.append("<span$headingClass$styleAttr>$prefix$text</span>")

            item.url?.let {
                sb.append(" <a href=\"").append(escapeHtml(it)).append("\" target=\"_blank\">[Link]</a>")
            }

            val children = childrenMap[item.id] ?: emptyList()
            if (children.isNotEmpty()) {
                sb.append("\n<ul>\n")
                for (child in children) {
                    renderNode(child)
                }
                sb.append("</ul>\n")
            }

            sb.append("</li>\n")
        }

        val rootItems = childrenMap[null] ?: emptyList()
        if (rootItems.isNotEmpty()) {
            sb.append("<ul>\n")
            for (root in rootItems) {
                renderNode(root)
            }
            sb.append("</ul>\n")
        }

        sb.append("</body>\n</html>")
        return sb.toString()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
