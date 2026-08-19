package com.example.domain.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AIConversationEntity
import com.example.data.local.entity.AIMessageEntity
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.ItemTagCrossRef
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.OutlineItemEntity
import com.example.data.local.entity.TagEntity
import com.example.domain.ai.AiTreeFormatter
import com.example.domain.model.AIConversation
import com.example.domain.model.AIMessage
import com.example.domain.model.Folder
import com.example.domain.model.GroundingCitation
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class OutlinerRepository(private val db: AppDatabase) {

    private val folderDao = db.folderDao()
    private val noteDao = db.noteDao()
    private val outlineItemDao = db.outlineItemDao()
    private val tagDao = db.tagDao()
    private val aiChatDao = db.aiChatDao()

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

    // ================= AI RESEARCH & CHAT ================= //

    val allAiConversations: Flow<List<AIConversation>> = aiChatDao.getAllConversations()
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

    suspend fun getAiConversation(id: String): AIConversation? = withContext(Dispatchers.IO) {
        aiChatDao.getConversationById(id)?.toDomain()
    }

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
        content: String,
        citations: List<GroundingCitation> = emptyList(),
        searchQueries: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val msgId = UUID.randomUUID().toString()
        val sourcesJson = if (citations.isNotEmpty()) {
            val arr = JSONArray()
            for (c in citations) {
                val obj = JSONObject()
                obj.put("title", c.title)
                obj.put("url", c.url)
                if (c.snippet != null) obj.put("snippet", c.snippet)
                arr.put(obj)
            }
            arr.toString()
        } else null

        val queriesJson = if (searchQueries.isNotEmpty()) {
            val arr = JSONArray()
            searchQueries.forEach { arr.put(it) }
            arr.toString()
        } else null

        val entity = AIMessageEntity(
            id = msgId,
            conversationId = conversationId,
            role = role,
            content = content,
            sourcesJson = sourcesJson,
            searchQueriesJson = queriesJson,
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
     * Inserts structured AI research result and citations into an existing note.
     * If targetItemId is provided, inserts items relative to that item.
     */
    suspend fun insertAiContentIntoNote(
        noteId: String,
        content: String,
        citations: List<GroundingCitation>,
        targetItemId: String? = null
    ) = withContext(Dispatchers.IO) {
        val currentItems = outlineItemDao.getItemsForNoteList(noteId).map { it.toDomain() }
        val formatted = AiTreeFormatter.formatAiResponseToItems(content, citations, noteId)
        val newAiItems = formatted.items

        if (currentItems.isEmpty()) {
            outlineItemDao.replaceItemsForNote(noteId, newAiItems.map { it.toEntity() })
        } else if (targetItemId != null) {
            val targetIndex = currentItems.indexOfFirst { it.id == targetItemId }
            if (targetIndex != -1) {
                val target = currentItems[targetIndex]
                // Insert as children of target or siblings
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
     * Creates a brand new note from AI research content and citations.
     */
    suspend fun createNoteFromAiContent(
        content: String,
        citations: List<GroundingCitation>,
        folderId: String? = null,
        customTitle: String? = null
    ): String = withContext(Dispatchers.IO) {
        val noteId = UUID.randomUUID().toString()
        val formatted = AiTreeFormatter.formatAiResponseToItems(
            content = content,
            citations = citations,
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
        val citations = mutableListOf<GroundingCitation>()
        if (!sourcesJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(sourcesJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    citations.add(
                        GroundingCitation(
                            title = obj.optString("title", ""),
                            url = obj.optString("url", ""),
                            snippet = obj.optString("snippet").ifEmpty { null }
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        val queries = mutableListOf<String>()
        if (!searchQueriesJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(searchQueriesJson)
                for (i in 0 until arr.length()) {
                    val q = arr.optString(i)
                    if (q.isNotBlank()) queries.add(q)
                }
            } catch (_: Exception) {}
        }

        return AIMessage(
            id = id,
            conversationId = conversationId,
            role = role,
            content = content,
            citations = citations,
            searchQueries = queries,
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
