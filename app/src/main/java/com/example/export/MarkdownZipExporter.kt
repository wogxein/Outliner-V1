package com.example.export

import android.content.Context
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.OutlineItemEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object MarkdownZipExporter {

    fun generateMarkdownContent(note: NoteEntity, items: List<OutlineItemEntity>): String {
        val sb = StringBuilder()
        sb.append("# ").append(note.title).append("\n\n")

        // Build item tree
        val sortedItems = items.sortedWith(compareBy({ it.parentId == null }, { it.sortOrder }))
        val childrenMap = items.groupBy { it.parentId }
        val rootItems = items.filter { it.parentId == null }.sortedBy { it.sortOrder }

        fun appendItem(item: OutlineItemEntity, indentLevel: Int) {
            val indent = "  ".repeat(indentLevel)
            val prefix = when {
                item.hasCheckbox && item.isChecked -> "- [x] "
                item.hasCheckbox && !item.isChecked -> "- [ ] "
                item.headingLevel == 1 -> "# "
                item.headingLevel == 2 -> "## "
                item.headingLevel == 3 -> "### "
                else -> "- "
            }

            var text = item.text
            if (item.isBold && !text.startsWith("**")) text = "**$text**"
            if (item.isItalic && !text.startsWith("*")) text = "*$text*"
            if (item.isStrikethrough && !text.startsWith("~~")) text = "~~$text~~"
            if (item.isCode && !text.startsWith("`")) text = "`$text`"

            sb.append(indent).append(prefix).append(text).append("\n")

            if (!item.url.isNullOrBlank()) {
                sb.append(indent).append("  ").append("[Link](").append(item.url).append(")\n")
            }

            val children = childrenMap[item.id] ?: emptyList()
            children.sortedBy { it.sortOrder }.forEach { child ->
                appendItem(child, indentLevel + 1)
            }
        }

        rootItems.forEach { rootItem ->
            appendItem(rootItem, 0)
        }

        return sb.toString()
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "Untitled" }
    }

    fun createZipFile(
        context: Context,
        folders: List<FolderEntity>,
        notes: List<NoteEntity>,
        itemsMap: Map<String, List<OutlineItemEntity>>
    ): File {
        val zipFile = File(context.cacheDir, "outliner_notes_export_${System.currentTimeMillis()}.zip")
        
        // Build folder path hierarchy
        val folderMap = folders.associateBy { it.id }
        fun getFolderPath(folderId: String?): String {
            if (folderId == null) return ""
            val folder = folderMap[folderId] ?: return ""
            val parentPath = getFolderPath(folder.parentId)
            val folderName = sanitizeFilename(folder.name)
            return if (parentPath.isEmpty()) folderName else "$parentPath/$folderName"
        }

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val usedPaths = mutableSetOf<String>()

            for (note in notes) {
                if (note.isDeleted) continue

                val folderPath = getFolderPath(note.folderId)
                val baseFilename = sanitizeFilename(note.title)
                var filename = "$baseFilename.md"
                var entryPath = if (folderPath.isEmpty()) filename else "$folderPath/$filename"

                var counter = 1
                while (usedPaths.contains(entryPath)) {
                    filename = "$baseFilename ($counter).md"
                    entryPath = if (folderPath.isEmpty()) filename else "$folderPath/$filename"
                    counter++
                }
                usedPaths.add(entryPath)

                val items = itemsMap[note.id] ?: emptyList()
                val mdContent = generateMarkdownContent(note, items)

                val entry = ZipEntry(entryPath)
                zos.putNextEntry(entry)
                zos.write(mdContent.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        return zipFile
    }
}
