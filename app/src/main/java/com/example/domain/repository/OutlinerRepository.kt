package com.example.domain.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.ItemTagCrossRef
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.OutlineItemEntity
import com.example.data.local.entity.TagEntity
import com.example.domain.model.Folder
import com.example.domain.model.Note
import com.example.domain.model.OutlineItem
import com.example.domain.model.SearchResult
import com.example.domain.model.Tag
import com.example.domain.tree.TreeOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class OutlinerRepository(private val db: AppDatabase) {

    private val folderDao = db.folderDao()
    private val noteDao = db.noteDao()
    private val outlineItemDao = db.outlineItemDao()
    private val tagDao = db.tagDao()

    // ================= FOLDERS ================= //

    val activeFolders: Flow<List<Folder>> = combine(
        folderDao.getActiveFolders(),
        noteDao.getActiveNotes()
    ) { folders, notes ->
        val noteCounts = notes.groupBy { it.folderId }.mapValues { it.value.size }
        folders.map { entity ->
            entity.toDomain(noteCounts[entity.id] ?: 0)
        }
    }.flowOn(Dispatchers.IO)

    val deletedFolders: Flow<List<Folder>> = folderDao.getDeletedFolders()
        .map { list -> list.map { it.toDomain(0) } }
        .flowOn(Dispatchers.IO)

    suspend fun createFolder(name: String, parentId: String? = null, color: String? = null): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val siblings = folderDao.getChildrenFolders(parentId ?: "")
        val folder = FolderEntity(
            id = id,
            name = name.trim().ifEmpty { "New Folder" },
            parentId = parentId,
            sortOrder = siblings.size,
            color = color,
            isExpanded = true
        )
        folderDao.insertFolder(folder)
        id
    }

    suspend fun updateFolder(folder: Folder) = withContext(Dispatchers.IO) {
        val entity = folderDao.getFolderById(folder.id) ?: return@withContext
        folderDao.updateFolder(
            entity.copy(
                name = folder.name,
                parentId = folder.parentId,
                sortOrder = folder.sortOrder,
                color = folder.color,
                isExpanded = folder.isExpanded,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun renameFolder(id: String, newName: String) = withContext(Dispatchers.IO) {
        val entity = folderDao.getFolderById(id) ?: return@withContext
        folderDao.updateFolder(entity.copy(name = newName.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleFolderExpanded(id: String) = withContext(Dispatchers.IO) {
        val entity = folderDao.getFolderById(id) ?: return@withContext
        folderDao.updateFolder(entity.copy(isExpanded = !entity.isExpanded))
    }

    suspend fun softDeleteFolder(id: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        folderDao.softDeleteFolder(id, now)
        // Also soft-delete all notes inside this folder
        val notes = noteDao.getAllNotesForBackup().filter { it.folderId == id && !it.isDeleted }
        notes.forEach { noteDao.softDeleteNote(it.id, now) }
    }

    suspend fun restoreFolder(id: String) = withContext(Dispatchers.IO) {
        folderDao.restoreFolder(id)
        // Restore notes inside folder
        val notes = noteDao.getAllNotesForBackup().filter { it.folderId == id && it.isDeleted }
        notes.forEach { noteDao.restoreNote(it.id) }
    }

    suspend fun hardDeleteFolder(id: String) = withContext(Dispatchers.IO) {
        folderDao.hardDeleteFolder(id)
    }

    // ================= NOTES ================= //

    val activeNotes: Flow<List<Note>> = noteDao.getActiveNotes()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    val favoriteNotes: Flow<List<Note>> = noteDao.getFavoriteNotes()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    val recentNotes: Flow<List<Note>> = noteDao.getRecentNotes()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    val deletedNotes: Flow<List<Note>> = noteDao.getDeletedNotes()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    fun getNotesInFolder(folderId: String?): Flow<List<Note>> = noteDao.getNotesInFolder(folderId)
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    fun observeNote(id: String): Flow<Note?> = noteDao.observeNoteById(id)
        .map { it?.toDomain() }
        .flowOn(Dispatchers.IO)

    suspend fun getNote(id: String): Note? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(id)?.toDomain()
    }

    suspend fun createNote(title: String, folderId: String? = null, initialItems: List<String> = emptyList()): String = withContext(Dispatchers.IO) {
        val noteId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            id = noteId,
            folderId = folderId,
            title = title.trim().ifEmpty { "Untitled" },
            isFavorite = false,
            createdAt = now,
            updatedAt = now,
            lastAccessedAt = now
        )
        noteDao.insertNote(note)

        if (initialItems.isNotEmpty()) {
            val entities = initialItems.mapIndexed { index, text ->
                OutlineItemEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = noteId,
                    parentId = null,
                    sortOrder = index,
                    text = text,
                    createdAt = now,
                    updatedAt = now
                )
            }
            outlineItemDao.insertItems(entities)
        } else {
            // Create one empty root item so user can immediately type
            val rootItem = OutlineItemEntity(
                id = UUID.randomUUID().toString(),
                noteId = noteId,
                parentId = null,
                sortOrder = 0,
                text = "",
                createdAt = now,
                updatedAt = now
            )
            outlineItemDao.insertItem(rootItem)
        }

        noteId
    }

    suspend fun renameNote(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        val entity = noteDao.getNoteById(id) ?: return@withContext
        noteDao.updateNote(
            entity.copy(
                title = newTitle.trim().ifEmpty { "Untitled" },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun touchNote(id: String) = withContext(Dispatchers.IO) {
        noteDao.updateLastAccessed(id, System.currentTimeMillis())
    }

    suspend fun toggleNoteFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        noteDao.toggleFavorite(id, isFavorite)
    }

    suspend fun moveNoteToFolder(noteId: String, folderId: String?) = withContext(Dispatchers.IO) {
        noteDao.moveNote(noteId, folderId)
    }

    suspend fun softDeleteNote(id: String) = withContext(Dispatchers.IO) {
        noteDao.softDeleteNote(id, System.currentTimeMillis())
    }

    suspend fun restoreNote(id: String) = withContext(Dispatchers.IO) {
        noteDao.restoreNote(id)
    }

    suspend fun hardDeleteNote(id: String) = withContext(Dispatchers.IO) {
        outlineItemDao.deleteItemsForNote(id)
        noteDao.hardDeleteNote(id)
    }

    suspend fun duplicateNote(noteId: String): String = withContext(Dispatchers.IO) {
        val original = noteDao.getNoteById(noteId) ?: return@withContext ""
        val newNoteId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val duplicatedNote = original.copy(
            id = newNoteId,
            title = "${original.title} (Copy)",
            isFavorite = false,
            createdAt = now,
            updatedAt = now,
            lastAccessedAt = now
        )
        noteDao.insertNote(duplicatedNote)

        val originalItems = outlineItemDao.getItemsForNoteList(noteId)
        val idMapping = mutableMapOf<String, String>()
        val duplicatedEntities = mutableListOf<OutlineItemEntity>()

        for (item in originalItems) {
            val newId = UUID.randomUUID().toString()
            idMapping[item.id] = newId
        }

        for (item in originalItems) {
            val newParentId = item.parentId?.let { idMapping[it] }
            val newEntity = item.copy(
                id = idMapping[item.id] ?: UUID.randomUUID().toString(),
                noteId = newNoteId,
                parentId = newParentId,
                createdAt = now,
                updatedAt = now
            )
            duplicatedEntities.add(newEntity)
        }

        outlineItemDao.insertItems(duplicatedEntities)
        newNoteId
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val deletedNotes = noteDao.getAllNotesForBackup().filter { it.isDeleted }
        for (note in deletedNotes) {
            outlineItemDao.deleteItemsForNote(note.id)
            noteDao.hardDeleteNote(note.id)
        }
        folderDao.emptyTrashFolders()
    }

    // ================= OUTLINE ITEMS ================= //

    fun getItemsForNote(noteId: String): Flow<List<OutlineItem>> = outlineItemDao.getItemsForNote(noteId)
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    suspend fun getItemsForNoteList(noteId: String): List<OutlineItem> = withContext(Dispatchers.IO) {
        outlineItemDao.getItemsForNoteList(noteId).map { it.toDomain() }
    }

    suspend fun saveItem(item: OutlineItem) = withContext(Dispatchers.IO) {
        outlineItemDao.insertItem(item.toEntity())
        extractAndPersistTags(item.id, item.text)
        noteDao.getNoteById(item.noteId)?.let {
            noteDao.updateNote(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun saveAllItems(noteId: String, items: List<OutlineItem>) = withContext(Dispatchers.IO) {
        val entities = items.map { it.toEntity() }
        outlineItemDao.replaceItemsForNote(noteId, entities)
        items.forEach { extractAndPersistTags(it.id, it.text) }
        noteDao.getNoteById(noteId)?.let {
            noteDao.updateNote(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun insertItem(item: OutlineItem) = withContext(Dispatchers.IO) {
        outlineItemDao.insertItem(item.toEntity())
        extractAndPersistTags(item.id, item.text)
        noteDao.getNoteById(item.noteId)?.let {
            noteDao.updateNote(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteSubtree(noteId: String, itemId: String) = withContext(Dispatchers.IO) {
        val currentItems = outlineItemDao.getItemsForNoteList(noteId).map { it.toDomain() }
        val updated = TreeOperations.deleteSubtree(currentItems, itemId)
        outlineItemDao.replaceItemsForNote(noteId, updated.map { it.toEntity() })
        noteDao.getNoteById(noteId)?.let {
            noteDao.updateNote(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    // ================= TAGS & SEARCH ================= //

    val allTags: Flow<List<Tag>> = tagDao.getAllTags()
        .map { list -> list.map { Tag(it.id, it.name, it.color, it.createdAt) } }
        .flowOn(Dispatchers.IO)

    private suspend fun extractAndPersistTags(itemId: String, text: String) {
        val tagRegex = Regex("""#([a-zA-Z0-9_\-]+)""")
        val matches = tagRegex.findAll(text).map { it.groupValues[1] }.toSet()

        tagDao.removeAllTagsFromItem(itemId)
        for (tagName in matches) {
            val existing = tagDao.getTagByName(tagName)
            val tagId = if (existing != null) {
                existing.id
            } else {
                val newId = UUID.randomUUID().toString()
                tagDao.insertTag(TagEntity(id = newId, name = tagName))
                newId
            }
            tagDao.insertCrossRef(ItemTagCrossRef(itemId = itemId, tagId = tagId))
        }
    }

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<SearchResult>()
        val folders = folderDao.getAllFoldersForBackup().associateBy { it.id }
        val activeNotes = noteDao.getAllNotesForBackup().filter { !it.isDeleted }.associateBy { it.id }

        // 1. Search in note titles
        val titleNotes = noteDao.searchNotesByTitle(trimmed)
        for (note in titleNotes) {
            results.add(
                SearchResult(
                    noteId = note.id,
                    noteTitle = note.title,
                    folderName = note.folderId?.let { id -> folders[id]?.name },
                    matchedItemId = null,
                    matchedText = note.title,
                    isTitleMatch = true,
                    updatedAt = note.updatedAt
                )
            )
        }

        // 2. Search in outline items
        val matchedItems = outlineItemDao.searchItems(trimmed)
        for (item in matchedItems) {
            val note = activeNotes[item.noteId] ?: continue
            val folderTitle = note.folderId?.let { id -> folders[id]?.name }
            results.add(
                SearchResult(
                    noteId = note.id,
                    noteTitle = note.title,
                    folderName = folderTitle,
                    matchedItemId = item.id,
                    matchedText = item.text,
                    isTitleMatch = false,
                    updatedAt = item.updatedAt
                )
            )
        }

        results.distinctBy { "${it.noteId}_${it.matchedItemId ?: "title"}" }
    }

    // ================= BACKUP & RESTORE ================= //

    suspend fun getAllFoldersForBackup(): List<FolderEntity> = withContext(Dispatchers.IO) {
        folderDao.getAllFoldersForBackup()
    }

    suspend fun getAllNotesForBackup(): List<NoteEntity> = withContext(Dispatchers.IO) {
        noteDao.getAllNotesForBackup()
    }

    suspend fun getAllItemsForBackup(): List<OutlineItemEntity> = withContext(Dispatchers.IO) {
        outlineItemDao.getAllItemsForBackup()
    }

    suspend fun getAllTagsForBackup(): List<TagEntity> = withContext(Dispatchers.IO) {
        tagDao.getAllTagsForBackup()
    }

    suspend fun getAllCrossRefsForBackup(): List<ItemTagCrossRef> = withContext(Dispatchers.IO) {
        tagDao.getAllCrossRefsForBackup()
    }

    suspend fun restoreDatabase(
        folders: List<FolderEntity>,
        notes: List<NoteEntity>,
        items: List<OutlineItemEntity>,
        tags: List<TagEntity>,
        crossRefs: List<ItemTagCrossRef>,
        replaceExisting: Boolean
    ) = withContext(Dispatchers.IO) {
        if (replaceExisting) {
            tagDao.clearAllCrossRefs()
            tagDao.clearAllTags()
            outlineItemDao.clearAllItems()
            noteDao.clearAllNotes()
            folderDao.clearAllFolders()
        }

        folderDao.insertFolders(folders)
        noteDao.insertNotes(notes)
        outlineItemDao.insertItems(items)
        tagDao.insertTags(tags)
        tagDao.insertCrossRefs(crossRefs)
    }

    // ================= ENTITY MAPPERS ================= //

    private fun FolderEntity.toDomain(noteCount: Int = 0) = Folder(
        id = id,
        name = name,
        parentId = parentId,
        sortOrder = sortOrder,
        color = color,
        isExpanded = isExpanded,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        noteCount = noteCount
    )

    private fun NoteEntity.toDomain(itemCount: Int = 0) = Note(
        id = id,
        folderId = folderId,
        title = title,
        isFavorite = isFavorite,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastAccessedAt = lastAccessedAt,
        itemCount = itemCount
    )

    private fun OutlineItemEntity.toDomain() = OutlineItem(
        id = id,
        noteId = noteId,
        parentId = parentId,
        sortOrder = sortOrder,
        text = text,
        isCollapsed = isCollapsed,
        hasCheckbox = hasCheckbox,
        isChecked = isChecked,
        headingLevel = headingLevel,
        isBold = isBold,
        isItalic = isItalic,
        isStrikethrough = isStrikethrough,
        isCode = isCode,
        textColor = textColor,
        backgroundColor = backgroundColor,
        url = url,
        mediaType = mediaType,
        mediaUri = mediaUri,
        mediaTitle = mediaTitle,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun OutlineItem.toEntity() = OutlineItemEntity(
        id = id,
        noteId = noteId,
        parentId = parentId,
        sortOrder = sortOrder,
        text = text,
        isCollapsed = isCollapsed,
        hasCheckbox = hasCheckbox,
        isChecked = isChecked,
        headingLevel = headingLevel,
        isBold = isBold,
        isItalic = isItalic,
        isStrikethrough = isStrikethrough,
        isCode = isCode,
        textColor = textColor,
        backgroundColor = backgroundColor,
        url = url,
        mediaType = mediaType,
        mediaUri = mediaUri,
        mediaTitle = mediaTitle,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
