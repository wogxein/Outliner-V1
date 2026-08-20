package com.example.domain.repository

import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AIConversationEntity
import com.example.data.local.entity.AIMessageEntity
import com.example.data.local.entity.AiBackendEntity
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.ItemTagCrossRef
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.OutlineItemEntity
import com.example.data.local.entity.TagEntity
import com.example.domain.ai.AiTreeFormatter
import com.example.domain.model.AIConversation
import com.example.domain.model.AIMessage
import com.example.domain.model.AiBackend
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
    private val aiChatDao = db.aiChatDao()
    private val aiBackendDao = db.aiBackendDao()

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
            isExpanded = true,
            isDeleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        folderDao.insertFolder(folder)
        id
    }

    suspend fun renameFolder(id: String, newName: String) = withContext(Dispatchers.IO) {
        val entity = folderDao.getFolderById(id) ?: return@withContext
        folderDao.updateFolder(
            entity.copy(
                name = newName.trim().ifEmpty { entity.name },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setFolderColor(id: String, color: String?) = withContext(Dispatchers.IO) {
        val entity = folderDao.getFolderById(id) ?: return@withContext
        folderDao.updateFolder(
            entity.copy(
                color = color,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun moveFolder(id: String, newParentId: String?) = withContext(Dispatchers.IO) {
        if (id == newParentId) return@withContext // Cannot be own parent
        val entity = folderDao.getFolderById(id) ?: return@withContext
        folderDao.updateFolder(
            entity.copy(
                parentId = newParentId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleFolderExpansion(id: String) = withContext(Dispatchers.IO) {
        val entity = folderDao.getFolderById(id) ?: return@withContext
        folderDao.updateFolder(entity.copy(isExpanded = !entity.isExpanded))
    }

    suspend fun toggleFolderExpanded(id: String) = toggleFolderExpansion(id)

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
            // Add one default item
            val defaultItem = OutlineItemEntity(
                id = UUID.randomUUID().toString(),
                noteId = noteId,
                parentId = null,
                sortOrder = 0,
                text = "",
                createdAt = now,
                updatedAt = now
            )
            outlineItemDao.insertItem(defaultItem)
        }
        noteId
    }

    suspend fun updateNoteTitle(id: String, title: String) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(id) ?: return@withContext
        noteDao.updateNote(
            note.copy(
                title = title.trim().ifEmpty { "Untitled" },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun renameNote(id: String, title: String) = updateNoteTitle(id, title)

    suspend fun setNoteFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(id) ?: return@withContext
        noteDao.updateNote(note.copy(isFavorite = isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleNoteFavorite(id: String, isFavorite: Boolean) = setNoteFavorite(id, isFavorite)

    suspend fun moveNoteToFolder(id: String, folderId: String?) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(id) ?: return@withContext
        noteDao.updateNote(note.copy(folderId = folderId, updatedAt = System.currentTimeMillis()))
    }

    suspend fun touchNote(id: String) = withContext(Dispatchers.IO) {
        noteDao.updateLastAccessed(id, System.currentTimeMillis())
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

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        noteDao.emptyTrashNotes()
        folderDao.emptyTrashFolders()
    }

    suspend fun duplicateNote(noteId: String): String = withContext(Dispatchers.IO) {
        val originalNote = noteDao.getNoteById(noteId) ?: return@withContext ""
        val originalItems = outlineItemDao.getItemsForNoteList(noteId)

        val newNoteId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val duplicateNote = originalNote.copy(
            id = newNoteId,
            title = "${originalNote.title} (Copy)",
            createdAt = now,
            updatedAt = now,
            lastAccessedAt = now
        )
        noteDao.insertNote(duplicateNote)

        // Mapping old itemId -> new itemId for proper parentId preservation
        val idMap = mutableMapOf<String, String>()
        originalItems.forEach { idMap[it.id] = UUID.randomUUID().toString() }

        val duplicatedItems = originalItems.map { item ->
            item.copy(
                id = idMap[item.id] ?: UUID.randomUUID().toString(),
                noteId = newNoteId,
                parentId = item.parentId?.let { idMap[it] ?: it },
                createdAt = now,
                updatedAt = now
            )
        }
        outlineItemDao.insertItems(duplicatedItems)
        newNoteId
    }

    // ================= OUTLINE ITEMS ================= //

    fun getItemsForNote(noteId: String): Flow<List<OutlineItem>> =
        outlineItemDao.getItemsForNote(noteId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    suspend fun getItemsForNoteList(noteId: String): List<OutlineItem> = withContext(Dispatchers.IO) {
        outlineItemDao.getItemsForNoteList(noteId).map { it.toDomain() }
    }

    suspend fun saveAllItems(noteId: String, items: List<OutlineItem>) = withContext(Dispatchers.IO) {
        val entities = items.map { it.toEntity() }
        outlineItemDao.replaceItemsForNote(noteId, entities)
        noteDao.getNoteById(noteId)?.let {
            noteDao.updateNote(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun insertItem(item: OutlineItem) = withContext(Dispatchers.IO) {
        outlineItemDao.insertItem(item.toEntity())
        noteDao.getNoteById(item.noteId)?.let {
            noteDao.updateNote(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun updateItem(item: OutlineItem) = withContext(Dispatchers.IO) {
        outlineItemDao.updateItem(item.toEntity())
        noteDao.getNoteById(item.noteId)?.let {
            noteDao.updateNote(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun saveItem(item: OutlineItem) = updateItem(item)

    suspend fun updateItemText(id: String, noteId: String, text: String) = withContext(Dispatchers.IO) {
        val entity = outlineItemDao.getItemById(id) ?: return@withContext
        outlineItemDao.updateItem(entity.copy(text = text, updatedAt = System.currentTimeMillis()))
        noteDao.getNoteById(noteId)?.let {
            noteDao.updateNote(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun toggleItemChecked(id: String, noteId: String) = withContext(Dispatchers.IO) {
        val entity = outlineItemDao.getItemById(id) ?: return@withContext
        outlineItemDao.updateItem(entity.copy(isChecked = !entity.isChecked, updatedAt = System.currentTimeMillis()))
        noteDao.getNoteById(noteId)?.let {
            noteDao.updateNote(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun toggleItemCollapsed(id: String, noteId: String) = withContext(Dispatchers.IO) {
        val entity = outlineItemDao.getItemById(id) ?: return@withContext
        outlineItemDao.updateItem(entity.copy(isCollapsed = !entity.isCollapsed, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteItem(id: String, noteId: String) = withContext(Dispatchers.IO) {
        val allItems = outlineItemDao.getItemsForNoteList(noteId).map { it.toDomain() }
        val updated = TreeOperations.deleteSubtree(allItems, id)
        outlineItemDao.replaceItemsForNote(noteId, updated.map { it.toEntity() })
        noteDao.getNoteById(noteId)?.let {
            noteDao.updateNote(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    // ================= SEARCH ================= //

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@withContext emptyList()

        val matchedNotes = noteDao.searchNotesByTitle(cleanQuery)
        val matchedItems = outlineItemDao.searchItems(cleanQuery)
        val allFoldersMap = folderDao.getAllFoldersForBackup().associateBy { it.id }

        val noteResults = matchedNotes.map { entity ->
            SearchResult(
                noteId = entity.id,
                noteTitle = entity.title,
                folderName = entity.folderId?.let { allFoldersMap[it]?.name },
                matchedItemId = null,
                matchedText = entity.title,
                isTitleMatch = true,
                updatedAt = entity.updatedAt
            )
        }

        val allActiveNotesMap = noteDao.getAllNotesForBackup().associateBy { it.id }
        val itemResults = matchedItems.mapNotNull { item ->
            val parentNote = allActiveNotesMap[item.noteId]
            if (parentNote == null || parentNote.isDeleted) return@mapNotNull null
            SearchResult(
                noteId = item.noteId,
                noteTitle = parentNote.title,
                folderName = parentNote.folderId?.let { allFoldersMap[it]?.name },
                matchedItemId = item.id,
                matchedText = item.text,
                isTitleMatch = false,
                updatedAt = item.updatedAt
            )
        }

        noteResults + itemResults
    }

    // ================= TAGS ================= //

    val allTags: Flow<List<Tag>> = tagDao.getAllTags()
        .map { list -> list.map { Tag(it.id, it.name, it.color) } }
        .flowOn(Dispatchers.IO)

    suspend fun createTag(name: String, color: String? = null): String = withContext(Dispatchers.IO) {
        val existing = tagDao.getTagByName(name.trim())
        if (existing != null) return@withContext existing.id
        val id = UUID.randomUUID().toString()
        tagDao.insertTag(TagEntity(id = id, name = name.trim(), color = color))
        id
    }

    suspend fun addTagToItem(itemId: String, tagId: String) = withContext(Dispatchers.IO) {
        tagDao.insertCrossRef(ItemTagCrossRef(itemId, tagId))
    }

    suspend fun removeTagFromItem(itemId: String, tagId: String) = withContext(Dispatchers.IO) {
        tagDao.deleteCrossRef(itemId, tagId)
    }

    suspend fun getTagsForItem(itemId: String): List<Tag> = withContext(Dispatchers.IO) {
        tagDao.getTagsForItem(itemId).map { Tag(it.id, it.name, it.color) }
    }

    // ================= BACKUP & EXPORT ================= //

    suspend fun getAllFoldersForBackup(): List<Folder> = withContext(Dispatchers.IO) {
        folderDao.getAllFoldersForBackup().map { it.toDomain() }
    }

    suspend fun getAllFolderEntitiesForBackup(): List<FolderEntity> = withContext(Dispatchers.IO) {
        folderDao.getAllFoldersForBackup()
    }

    suspend fun getAllNotesForBackup(): List<Note> = withContext(Dispatchers.IO) {
        noteDao.getAllNotesForBackup().map { it.toDomain() }
    }

    suspend fun getAllNoteEntitiesForBackup(): List<NoteEntity> = withContext(Dispatchers.IO) {
        noteDao.getAllNotesForBackup()
    }

    suspend fun getAllItemsForBackup(): List<OutlineItem> = withContext(Dispatchers.IO) {
        outlineItemDao.getAllItemsForBackup().map { it.toDomain() }
    }

    suspend fun getAllOutlineItemEntitiesForBackup(): List<OutlineItemEntity> = withContext(Dispatchers.IO) {
        outlineItemDao.getAllItemsForBackup()
    }

    suspend fun getAllTagsForBackup(): List<TagEntity> = withContext(Dispatchers.IO) {
        tagDao.getAllTagsForBackup()
    }

    suspend fun getAllCrossRefsForBackup(): List<ItemTagCrossRef> = withContext(Dispatchers.IO) {
        tagDao.getAllCrossRefsForBackup()
    }

    // ================= AI BACKENDS ================= //

    val allAiBackends: Flow<List<AiBackend>> = aiBackendDao.getAllBackends()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    suspend fun getAllAiBackendsList(): List<AiBackend> = withContext(Dispatchers.IO) {
        val list = aiBackendDao.getAllBackendsList().map { it.toDomain() }
        if (list.isEmpty()) {
            val defaultKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
            val defaultBackend = AiBackend.createDefault(defaultKey)
            aiBackendDao.insertBackend(defaultBackend.toEntity())
            listOf(defaultBackend)
        } else {
            list
        }
    }

    suspend fun getDefaultBackend(): AiBackend? = withContext(Dispatchers.IO) {
        aiBackendDao.getDefaultBackend()?.toDomain()
    }

    suspend fun saveAiBackend(backend: AiBackend) = withContext(Dispatchers.IO) {
        if (backend.isDefault) {
            // Unset other defaults
            val current = aiBackendDao.getAllBackendsList()
            current.filter { it.isDefault && it.id != backend.id }.forEach {
                aiBackendDao.updateBackend(it.copy(isDefault = false))
            }
        }
        aiBackendDao.insertBackend(backend.toEntity())
    }

    suspend fun deleteAiBackend(id: String) = withContext(Dispatchers.IO) {
        aiBackendDao.deleteBackend(id)
    }

    suspend fun setDefaultBackend(id: String) = withContext(Dispatchers.IO) {
        val current = aiBackendDao.getAllBackendsList()
        current.forEach {
            aiBackendDao.updateBackend(it.copy(isDefault = (it.id == id)))
        }
    }

    // ================= AI CONVERSATIONS & CHAT ================= //

    val allAiConversations: Flow<List<AIConversation>> =
        aiChatDao.getAllConversations()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    fun getAiConversationsForNote(noteId: String): Flow<List<AIConversation>> =
        aiChatDao.getConversationsForNote(noteId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    fun getAiConversationsForFolder(folderId: String): Flow<List<AIConversation>> =
        aiChatDao.getConversationsForFolder(folderId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    fun observeAiConversation(id: String): Flow<AIConversation?> =
        aiChatDao.observeConversationById(id)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)

    fun getAiMessages(conversationId: String): Flow<List<AIMessage>> =
        aiChatDao.getMessagesForConversation(conversationId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    suspend fun getAiMessagesList(conversationId: String): List<AIMessage> = withContext(Dispatchers.IO) {
        aiChatDao.getMessagesForConversationList(conversationId).map { it.toDomain() }
    }

    suspend fun createAiConversation(
        title: String,
        noteId: String? = null,
        folderId: String? = null
    ): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val conv = AIConversationEntity(
            id = id,
            title = title.trim().ifEmpty { "New AI Research" },
            noteId = noteId,
            folderId = folderId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        aiChatDao.insertConversation(conv)
        id
    }

    suspend fun updateAiConversationTitle(id: String, title: String) = withContext(Dispatchers.IO) {
        val existing = aiChatDao.getConversationById(id) ?: return@withContext
        aiChatDao.updateConversation(
            existing.copy(
                title = title.trim().ifEmpty { existing.title },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteAiConversation(id: String) = withContext(Dispatchers.IO) {
        aiChatDao.deleteMessagesForConversation(id)
        aiChatDao.deleteConversation(id)
    }

    suspend fun saveAiMessage(
        conversationId: String,
        role: String,
        content: String
    ): String = withContext(Dispatchers.IO) {
        val msgId = UUID.randomUUID().toString()

        val entity = AIMessageEntity(
            id = msgId,
            conversationId = conversationId,
            role = role,
            content = content,
            createdAt = System.currentTimeMillis()
        )
        aiChatDao.insertMessage(entity)

        // Update conversation timestamp & title if first message
        val conv = aiChatDao.getConversationById(conversationId)
        if (conv != null) {
            val newTitle = if (conv.title == "New AI Research" && role == "user") {
                content.take(40).trim().ifEmpty { "AI Research" }
            } else {
                conv.title
            }
            aiChatDao.updateConversation(conv.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }

        msgId
    }

    // ================= AI NOTE INSERTION ================= //

    /**
     * Inserts structured AI research result into an existing note.
     * If targetItemId is provided, inserts items relative to that item.
     */
    suspend fun insertAiContentIntoNote(
        noteId: String,
        content: String,
        targetItemId: String? = null
    ) = withContext(Dispatchers.IO) {
        val currentItems = outlineItemDao.getItemsForNoteList(noteId).map { it.toDomain() }
        val formatted = AiTreeFormatter.formatAiResponseToItems(content, noteId)
        val newAiItems = formatted.items

        if (currentItems.isEmpty()) {
            outlineItemDao.replaceItemsForNote(noteId, newAiItems.map { it.toEntity() })
        } else if (targetItemId != null) {
            val targetIndex = currentItems.indexOfFirst { it.id == targetItemId }
            if (targetIndex != -1) {
                val target = currentItems[targetIndex]
                val updatedItems = mutableListOf<OutlineItem>()
                updatedItems.addAll(currentItems)

                val startOrder = target.sortOrder + 1
                val mappedNewItems = newAiItems.mapIndexed { idx, item ->
                    if (item.parentId == null) {
                        item.copy(parentId = target.parentId, sortOrder = startOrder + idx)
                    } else {
                        item
                    }
                }

                // Shift subsequent siblings
                val finalItems = mutableListOf<OutlineItem>()
                for (it in updatedItems) {
                    if (it.parentId == target.parentId && it.sortOrder > target.sortOrder) {
                        finalItems.add(it.copy(sortOrder = it.sortOrder + newAiItems.size))
                    } else {
                        finalItems.add(it)
                    }
                }
                finalItems.addAll(mappedNewItems)
                outlineItemDao.replaceItemsForNote(noteId, finalItems.map { it.toEntity() })
            } else {
                val maxOrder = (currentItems.filter { it.parentId == null }.maxOfOrNull { it.sortOrder } ?: -1) + 1
                val mapped = newAiItems.map { item ->
                    if (item.parentId == null) item.copy(sortOrder = item.sortOrder + maxOrder) else item
                }
                outlineItemDao.insertItems(mapped.map { it.toEntity() })
            }
        } else {
            // Append at the end of root
            val maxOrder = (currentItems.filter { it.parentId == null }.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val mapped = newAiItems.map { item ->
                if (item.parentId == null) item.copy(sortOrder = item.sortOrder + maxOrder) else item
            }
            val combined = currentItems + mapped
            outlineItemDao.replaceItemsForNote(noteId, combined.map { it.toEntity() })
        }

        noteDao.getNoteById(noteId)?.let {
            noteDao.updateNote(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Creates a brand new note from AI research content.
     */
    suspend fun createNoteFromAiContent(
        content: String,
        folderId: String? = null,
        customTitle: String? = null
    ): String = withContext(Dispatchers.IO) {
        val noteId = UUID.randomUUID().toString()
        val formatted = AiTreeFormatter.formatAiResponseToItems(
            content = content,
            targetNoteId = noteId,
            defaultTitle = customTitle ?: "AI Research Note"
        )

        val noteTitle = customTitle?.ifBlank { null } ?: formatted.suggestedTitle
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            id = noteId,
            folderId = folderId,
            title = noteTitle,
            isFavorite = false,
            createdAt = now,
            updatedAt = now,
            lastAccessedAt = now
        )
        noteDao.insertNote(note)
        outlineItemDao.insertItems(formatted.items.map { it.toEntity() })
        noteId
    }

    // ================= ENTITY MAPPERS ================= //

    private fun AiBackendEntity.toDomain() = AiBackend(
        id = id,
        name = name,
        url = url,
        apiKey = apiKey,
        models = models,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun AiBackend.toEntity() = AiBackendEntity(
        id = id,
        name = name,
        url = url,
        apiKey = apiKey,
        models = models,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun AIConversationEntity.toDomain(messageCount: Int = 0) = AIConversation(
        id = id,
        title = title,
        noteId = noteId,
        folderId = folderId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        messageCount = messageCount
    )

    private fun AIMessageEntity.toDomain(): AIMessage {
        return AIMessage(
            id = id,
            conversationId = conversationId,
            role = role,
            content = content,
            createdAt = createdAt
        )
    }

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
