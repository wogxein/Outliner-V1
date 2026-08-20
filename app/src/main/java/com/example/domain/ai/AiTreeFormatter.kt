package com.example.domain.ai

import com.example.domain.model.OutlineItem
import com.example.importer.MarkdownImporter

object AiTreeFormatter {

    data class FormattedAiTreeResult(
        val suggestedTitle: String,
        val items: List<OutlineItem>
    )

    /**
     * Converts markdown content into structured OutlineItem tree hierarchy.
     */
    fun formatAiResponseToItems(
        content: String,
        targetNoteId: String,
        defaultTitle: String = "AI Research Note"
    ): FormattedAiTreeResult {
        val parsed = MarkdownImporter.parseMarkdown(content, targetNoteId)
        val mutableItems = parsed.items.toMutableList()
        val title = if (parsed.title != "Imported Note" && parsed.title.isNotBlank()) parsed.title else defaultTitle

        return FormattedAiTreeResult(suggestedTitle = title, items = mutableItems)
    }
}
