package com.example.importer

import android.content.Context
import android.net.Uri
import com.example.domain.repository.OutlinerRepository
import com.example.export.OpmlImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

object ZipImporter {

    data class ImportResult(
        val notesCount: Int,
        val foldersCount: Int
    )

    suspend fun importZip(
        context: Context,
        uri: Uri,
        repository: OutlinerRepository
    ): ImportResult = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ImportResult(0, 0)
        importFromStream(inputStream, repository)
    }

    suspend fun importFromStream(
        inputStream: InputStream,
        repository: OutlinerRepository
    ): ImportResult = withContext(Dispatchers.IO) {
        var notesCreated = 0
        val createdFoldersMap = mutableMapOf<String, String>() // full path to folderId

        suspend fun getOrCreateFolderHierarchy(folderPath: String): String? {
            if (folderPath.isBlank()) return null
            if (createdFoldersMap.containsKey(folderPath)) return createdFoldersMap[folderPath]

            val segments = folderPath.split("/").filter { it.isNotBlank() }
            var currentParentId: String? = null
            var accumulatedPath = ""

            for (segment in segments) {
                accumulatedPath = if (accumulatedPath.isEmpty()) segment else "$accumulatedPath/$segment"
                if (createdFoldersMap.containsKey(accumulatedPath)) {
                    currentParentId = createdFoldersMap[accumulatedPath]
                } else {
                    val folderId = repository.createFolder(name = segment, parentId = currentParentId)
                    createdFoldersMap[accumulatedPath] = folderId
                    currentParentId = folderId
                }
            }
            return currentParentId
        }

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name.replace('\\', '/')
                    val lastSlash = name.lastIndexOf('/')
                    val folderPath = if (lastSlash != -1) name.substring(0, lastSlash) else ""
                    val fileName = if (lastSlash != -1) name.substring(lastSlash + 1) else name

                    val isMd = fileName.endsWith(".md", ignoreCase = true)
                    val isOpml = fileName.endsWith(".opml", ignoreCase = true) || fileName.endsWith(".xml", ignoreCase = true)

                    if (isMd || isOpml) {
                        val content = zis.bufferedReader(Charsets.UTF_8).readText()
                        val folderId = getOrCreateFolderHierarchy(folderPath)
                        val titleFromFileName = fileName.substringBeforeLast(".")

                        val tempNoteId = UUID.randomUUID().toString()
                        if (isMd) {
                            val parsed = MarkdownImporter.parseMarkdown(content, tempNoteId)
                            val finalTitle = if (parsed.title == "Imported Note" && titleFromFileName.isNotBlank()) titleFromFileName else parsed.title
                            val newNoteId = repository.createNote(finalTitle, folderId)
                            val itemsWithCorrectId = parsed.items.map { it.copy(noteId = newNoteId) }
                            repository.saveAllItems(newNoteId, itemsWithCorrectId)
                            notesCreated++
                        } else {
                            val parsed = OpmlImporter.parseOpml(content, tempNoteId)
                            val finalTitle = if (parsed.title == "Imported Outline" && titleFromFileName.isNotBlank()) titleFromFileName else parsed.title
                            val newNoteId = repository.createNote(finalTitle, folderId)
                            val itemsWithCorrectId = parsed.items.map { it.copy(noteId = newNoteId) }
                            repository.saveAllItems(newNoteId, itemsWithCorrectId)
                            notesCreated++
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        ImportResult(notesCount = notesCreated, foldersCount = createdFoldersMap.size)
    }
}
