package com.example.domain.ai

import com.example.domain.model.GroundingCitation
import com.example.domain.model.OutlineItem
import com.example.importer.MarkdownImporter
import java.util.UUID

object AiTreeFormatter {

    data class FormattedAiTreeResult(
        val suggestedTitle: String,
        val items: List<OutlineItem>
    )

    /**
     * Converts markdown content + grounding citations into structured OutlineItem tree hierarchy.
     */
    fun formatAiResponseToItems(
        content: String,
        citations: List<GroundingCitation>,
        targetNoteId: String,
        defaultTitle: String = "AI Research Note"
    ): FormattedAiTreeResult {
        val parsed = MarkdownImporter.parseMarkdown(content, targetNoteId)
        val mutableItems = parsed.items.toMutableList()
        val title = if (parsed.title != "Imported Note" && parsed.title.isNotBlank()) parsed.title else defaultTitle

        if (citations.isNotEmpty()) {
            val sourcesHeaderId = UUID.randomUUID().toString()
            val maxSortOrder = (mutableItems.filter { it.parentId == null }.maxOfOrNull { it.sortOrder } ?: -1) + 1

            // Add "Sources" root node
            mutableItems.add(
                OutlineItem(
                    id = sourcesHeaderId,
                    noteId = targetNoteId,
                    parentId = null,
                    sortOrder = maxSortOrder,
                    text = "Sources & Citations",
                    headingLevel = 2,
                    isBold = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Add each citation as child node
            citations.forEachIndexed { index, citation ->
                val citeId = UUID.randomUUID().toString()
                mutableItems.add(
                    OutlineItem(
                        id = citeId,
                        noteId = targetNoteId,
                        parentId = sourcesHeaderId,
                        sortOrder = index,
                        text = citation.title,
                        url = citation.url,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        return FormattedAiTreeResult(suggestedTitle = title, items = mutableItems)
    }
}
