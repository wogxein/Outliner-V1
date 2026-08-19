package com.example.export

import android.content.Context
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.OutlineItemEntity
import com.example.domain.model.Note
import com.example.domain.model.OutlineItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object OpmlZipExporter {

    fun createZipFile(
        context: Context,
        folders: List<FolderEntity>,
        notes: List<NoteEntity>,
        itemsMap: Map<String, List<OutlineItemEntity>>
    ): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val zipFile = File(exportDir, "Outliner_OPML_Export_$timeStamp.zip")
        val folderPathMap = buildFolderPathMap(folders)

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val usedPaths = mutableSetOf<String>()

            for (note in notes) {
                val folderPath = note.folderId?.let { folderPathMap[it] } ?: ""
                val safeTitle = sanitizeFileName(note.title.ifBlank { "Untitled" })
                var relativePath = if (folderPath.isNotEmpty()) "$folderPath/$safeTitle.opml" else "$safeTitle.opml"

                // Handle duplicates
                var counter = 1
                while (usedPaths.contains(relativePath)) {
                    val numberedTitle = "${safeTitle}_$counter"
                    relativePath = if (folderPath.isNotEmpty()) "$folderPath/$numberedTitle.opml" else "$numberedTitle.opml"
                    counter++
                }
                usedPaths.add(relativePath)

                val items = itemsMap[note.id] ?: emptyList()
                val domainNote = Note(
                    id = note.id,
                    folderId = note.folderId,
                    title = note.title,
                    isFavorite = note.isFavorite,
                    isDeleted = note.isDeleted,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    lastAccessedAt = note.lastAccessedAt
                )
                val domainItems = items.map { entity ->
                    OutlineItem(
                        id = entity.id,
                        noteId = entity.noteId,
                        parentId = entity.parentId,
                        sortOrder = entity.sortOrder,
                        text = entity.text,
                        isCollapsed = entity.isCollapsed,
                        hasCheckbox = entity.hasCheckbox,
                        isChecked = entity.isChecked,
                        headingLevel = entity.headingLevel,
                        isBold = entity.isBold,
                        isItalic = entity.isItalic,
                        isStrikethrough = entity.isStrikethrough,
                        isCode = entity.isCode,
                        textColor = entity.textColor,
                        backgroundColor = entity.backgroundColor,
                        url = entity.url,
                        mediaType = entity.mediaType,
                        mediaUri = entity.mediaUri,
                        mediaTitle = entity.mediaTitle,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                }

                val opmlContent = OpmlExporter.exportToOpml(domainNote, domainItems)
                val zipEntry = ZipEntry(relativePath)
                zos.putNextEntry(zipEntry)
                zos.write(opmlContent.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        return zipFile
    }

    private fun buildFolderPathMap(folders: List<FolderEntity>): Map<String, String> {
        val folderMap = folders.associateBy { it.id }
        val pathMap = mutableMapOf<String, String>()

        fun getPath(folderId: String): String {
            if (pathMap.containsKey(folderId)) return pathMap[folderId]!!
            val folder = folderMap[folderId] ?: return ""
            val parentPath = folder.parentId?.let { getPath(it) } ?: ""
            val cleanName = sanitizeFileName(folder.name)
            val fullPath = if (parentPath.isNotEmpty()) "$parentPath/$cleanName" else cleanName
            pathMap[folderId] = fullPath
            return fullPath
        }

        for (f in folders) {
            getPath(f.id)
        }
        return pathMap
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifEmpty { "untitled" }
    }
}
