package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.domain.model.Folder
import com.example.domain.model.Note
import com.example.domain.model.OutlineItem
import com.example.domain.model.SearchResult
import com.example.domain.model.TreeItemNode
import com.example.domain.repository.OutlinerRepository
import com.example.domain.tree.TreeOperations
import com.example.export.MarkdownZipExporter
import com.example.export.OpmlExporter
import com.example.export.OpmlImporter
import com.example.export.OpmlZipExporter
import com.example.export.PlainTextExporter
import com.example.importer.MarkdownImporter
import com.example.importer.ZipImporter
import com.example.media.AudioPlayerController
import com.example.media.MediaRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Stack
import java.util.UUID

sealed class AppView {
    data class Editor(val noteId: String, val targetItemId: String? = null) : AppView()
    object AllNotes : AppView()
    data class FolderView(val folderId: String) : AppView()
    object Favorites : AppView()
    object Recent : AppView()
    object Trash : AppView()
    object Settings : AppView()
}

class OutlinerViewModel(
    application: Application,
    private val repository: OutlinerRepository
) : AndroidViewModel(application) {

    val audioController = AudioPlayerController(application)

    // Theme Management (Light / Dark only)
    private val prefs = application.getSharedPreferences("outliner_prefs", Context.MODE_PRIVATE)
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
    }

    // Navigation State
    private val _currentView = MutableStateFlow<AppView>(AppView.AllNotes)
    val currentView: StateFlow<AppView> = _currentView.asStateFlow()

    // Active Note ID
    private val _activeNoteId = MutableStateFlow<String?>(null)
    val activeNoteId: StateFlow<String?> = _activeNoteId.asStateFlow()

    // Focus / Zoom subtree ID
    private val _focusedRootId = MutableStateFlow<String?>(null)
    val focusedRootId: StateFlow<String?> = _focusedRootId.asStateFlow()

    // Multi-selection
    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedItemIds: StateFlow<Set<String>> = _selectedItemIds.asStateFlow()

    // Currently focused editing item ID
    private val _activeEditingItemId = MutableStateFlow<String?>(null)
    val activeEditingItemId: StateFlow<String?> = _activeEditingItemId.asStateFlow()

    // Search query & results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // User Message / Snackbar events
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Undo / Redo stacks for the active note
    private val undoStack = Stack<List<OutlineItem>>()
    private val redoStack = Stack<List<OutlineItem>>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Flows from repository
    val folders: StateFlow<List<Folder>> = repository.activeFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedFolders: StateFlow<List<Folder>> = repository.deletedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = repository.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteNotes: StateFlow<List<Note>> = repository.favoriteNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentNotes: StateFlow<List<Note>> = repository.recentNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedNotes: StateFlow<List<Note>> = repository.deletedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Note Details Flow
    val activeNote: StateFlow<Note?> = _activeNoteId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observeNote(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active Note's Outline Items Flow
    val outlineItems: StateFlow<List<OutlineItem>> = _activeNoteId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getItemsForNote(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flattened Visual Tree for rendering
    val flattenedTree: StateFlow<List<TreeItemNode>> = combine(
        outlineItems,
        _focusedRootId
    ) { items, focusedId ->
        TreeOperations.buildFlattenedTree(items, focusedId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Zoom Breadcrumbs Flow
    val breadcrumbs: StateFlow<List<OutlineItem>> = combine(
        outlineItems,
        _focusedRootId
    ) { items, focusedId ->
        TreeOperations.getBreadcrumbs(items, focusedId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var autosaveDebounceJob: Job? = null
    private var searchDebounceJob: Job? = null

    init {
        // App starts on "All Notes" view by default
        _currentView.value = AppView.AllNotes
        _activeNoteId.value = null
    }

    // ================= NAVIGATION ================= //

    fun navigateTo(view: AppView) {
        _currentView.value = view
        when (view) {
            is AppView.Editor -> {
                _activeNoteId.value = view.noteId
                _focusedRootId.value = view.targetItemId
                _selectedItemIds.value = emptySet()
                clearUndoRedo()
                viewModelScope.launch { repository.touchNote(view.noteId) }
            }
            else -> {
                _selectedItemIds.value = emptySet()
            }
        }
    }

    fun openNote(noteId: String, targetItemId: String? = null) {
        navigateTo(AppView.Editor(noteId, targetItemId))
    }

    // ================= NOTE OPERATIONS ================= //

    fun createNote(title: String = "Untitled", folderId: String? = null, initialItems: List<String> = emptyList()) {
        viewModelScope.launch {
            val noteId = repository.createNote(title, folderId, initialItems)
            openNote(noteId)
        }
    }

    fun renameActiveNote(newTitle: String) {
        val noteId = _activeNoteId.value ?: return
        viewModelScope.launch {
            repository.renameNote(noteId, newTitle)
        }
    }

    fun toggleNoteFavorite(noteId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleNoteFavorite(noteId, isFavorite)
        }
    }

    fun moveNoteToFolder(noteId: String, folderId: String?) {
        viewModelScope.launch {
            repository.moveNoteToFolder(noteId, folderId)
            _userMessage.emit("Note moved")
        }
    }

    fun duplicateNote(noteId: String) {
        viewModelScope.launch {
            val newId = repository.duplicateNote(noteId)
            if (newId.isNotEmpty()) {
                _userMessage.emit("Note duplicated")
                openNote(newId)
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.softDeleteNote(noteId)
            if (_activeNoteId.value == noteId) {
                _activeNoteId.value = null
                _currentView.value = AppView.AllNotes
            }
            _userMessage.emit("Note moved to Trash")
        }
    }

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            repository.restoreNote(noteId)
            _userMessage.emit("Note restored")
        }
    }

    fun hardDeleteNote(noteId: String) {
        viewModelScope.launch {
            repository.hardDeleteNote(noteId)
            _userMessage.emit("Note deleted permanently")
        }
    }

    // ================= FOLDER OPERATIONS ================= //

    fun createFolder(name: String, parentId: String? = null, color: String? = null) {
        viewModelScope.launch {
            repository.createFolder(name, parentId, color)
            _userMessage.emit("Folder created")
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            repository.renameFolder(folderId, newName)
        }
    }

    fun toggleFolderExpanded(folderId: String) {
        viewModelScope.launch {
            repository.toggleFolderExpanded(folderId)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.softDeleteFolder(folderId)
            _userMessage.emit("Folder moved to Trash")
        }
    }

    fun restoreFolder(folderId: String) {
        viewModelScope.launch {
            repository.restoreFolder(folderId)
            _userMessage.emit("Folder restored")
        }
    }

    fun hardDeleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.hardDeleteFolder(folderId)
            _userMessage.emit("Folder permanently deleted")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            _userMessage.emit("Trash emptied")
        }
    }

    // ================= OUTLINE ITEM OPERATIONS ================= //

    fun setActiveEditingItem(itemId: String?) {
        _activeEditingItemId.value = itemId
    }

    fun updateItemText(itemId: String, newText: String) {
        val currentItems = outlineItems.value
        val item = currentItems.find { it.id == itemId } ?: return
        if (item.text == newText) return

        val mediaInfo = MediaRecognizer.recognize(newText)
        val recognizedType = when (mediaInfo.type) {
            com.example.media.RecognizedMediaType.YOUTUBE_VIDEO -> "youtube_video"
            com.example.media.RecognizedMediaType.YOUTUBE_PLAYLIST -> "youtube_playlist"
            com.example.media.RecognizedMediaType.AUDIO_FILE -> "audio"
            com.example.media.RecognizedMediaType.URL -> "url"
            else -> null
        }

        val updatedItem = item.copy(
            text = newText,
            url = if (mediaInfo.type != com.example.media.RecognizedMediaType.NONE) mediaInfo.rawUrl else item.url,
            mediaType = recognizedType ?: item.mediaType,
            mediaUri = if (recognizedType == "audio") mediaInfo.rawUrl else item.mediaUri,
            mediaTitle = if (mediaInfo.type != com.example.media.RecognizedMediaType.NONE) mediaInfo.title else item.mediaTitle,
            updatedAt = System.currentTimeMillis()
        )

        val updatedList = currentItems.map { if (it.id == itemId) updatedItem else it }
        recordSnapshot(currentItems)

        viewModelScope.launch {
            repository.saveItem(updatedItem)
        }
    }

    fun toggleItemCheckbox(itemId: String) {
        val currentItems = outlineItems.value
        val item = currentItems.find { it.id == itemId } ?: return
        recordSnapshot(currentItems)

        val updatedItem = if (!item.hasCheckbox) {
            item.copy(hasCheckbox = true, isChecked = false, updatedAt = System.currentTimeMillis())
        } else if (!item.isChecked) {
            item.copy(isChecked = true, updatedAt = System.currentTimeMillis())
        } else {
            item.copy(hasCheckbox = false, isChecked = false, updatedAt = System.currentTimeMillis())
        }

        viewModelScope.launch {
            repository.saveItem(updatedItem)
        }
    }

    fun toggleItemCollapsed(itemId: String) {
        val currentItems = outlineItems.value
        val item = currentItems.find { it.id == itemId } ?: return
        val updatedItem = item.copy(isCollapsed = !item.isCollapsed, updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.saveItem(updatedItem)
        }
    }

    fun collapseAll() {
        val currentItems = outlineItems.value
        val updatedList = currentItems.map { it.copy(isCollapsed = true) }
        val noteId = _activeNoteId.value ?: return
        recordSnapshot(currentItems)
        viewModelScope.launch {
            repository.saveAllItems(noteId, updatedList)
        }
    }

    fun expandAll() {
        val currentItems = outlineItems.value
        val updatedList = currentItems.map { it.copy(isCollapsed = false) }
        val noteId = _activeNoteId.value ?: return
        recordSnapshot(currentItems)
        viewModelScope.launch {
            repository.saveAllItems(noteId, updatedList)
        }
    }

    fun addSiblingItem(afterItemId: String?) {
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        recordSnapshot(currentItems)

        val (updatedList, newItem) = TreeOperations.addSiblingAfter(currentItems, afterItemId, noteId)
        _activeEditingItemId.value = newItem.id
        viewModelScope.launch {
            repository.saveAllItems(noteId, updatedList)
        }
    }

    fun splitItem(itemId: String, textBefore: String, textAfter: String) {
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        recordSnapshot(currentItems)

        val (updatedList, newItem) = TreeOperations.splitItem(currentItems, itemId, textBefore, textAfter, noteId)
        _activeEditingItemId.value = newItem.id
        viewModelScope.launch {
            repository.saveAllItems(noteId, updatedList)
        }
    }

    fun addChildItem(parentItemId: String) {
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        recordSnapshot(currentItems)

        val (updatedList, newItem) = TreeOperations.addChildItem(currentItems, parentItemId, noteId)
        _activeEditingItemId.value = newItem.id
        viewModelScope.launch {
            repository.saveAllItems(noteId, updatedList)
        }
    }

    fun indentItem(itemId: String) {
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        val updatedList = TreeOperations.indentItem(currentItems, itemId) ?: return
        recordSnapshot(currentItems)

        viewModelScope.launch {
            repository.saveAllItems(noteId, updatedList)
        }
    }

    fun unindentItem(itemId: String) {
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        val updatedList = TreeOperations.unindentItem(currentItems, itemId) ?: return
        recordSnapshot(currentItems)

        viewModelScope.launch {
            repository.saveAllItems(noteId, updatedList)
        }
    }

    fun moveItemUp(itemId: String) {
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        val updatedList = TreeOperations.moveItemUp(currentItems, itemId) ?: return
        recordSnapshot(currentItems)

        viewModelScope.launch {
            repository.saveAllItems(noteId, updatedList)
        }
    }

    fun moveItemDown(itemId: String) {
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        val updatedList = TreeOperations.moveItemDown(currentItems, itemId) ?: return
        recordSnapshot(currentItems)

        viewModelScope.launch {
            repository.saveAllItems(noteId, updatedList)
        }
    }

    fun duplicateSubtree(itemId: String) {
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        recordSnapshot(currentItems)

        val updatedList = TreeOperations.duplicateSubtree(currentItems, itemId)
        viewModelScope.launch {
            repository.saveAllItems(noteId, updatedList)
            _userMessage.emit("Item duplicated")
        }
    }

    fun deleteSubtree(itemId: String) {
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        recordSnapshot(currentItems)

        val updatedList = TreeOperations.deleteSubtree(currentItems, itemId)
        if (updatedList.isEmpty()) {
            // Keep at least one empty root item so note isn't completely empty
            val (listWithEmpty, _) = TreeOperations.addSiblingAfter(emptyList(), null, noteId)
            viewModelScope.launch {
                repository.saveAllItems(noteId, listWithEmpty)
            }
        } else {
            viewModelScope.launch {
                repository.saveAllItems(noteId, updatedList)
            }
        }
    }

    // ================= FORMATTING ================= //

    fun toggleFormatBold(itemId: String) {
        val currentItems = outlineItems.value
        val item = currentItems.find { it.id == itemId } ?: return
        recordSnapshot(currentItems)
        val updated = item.copy(isBold = !item.isBold, updatedAt = System.currentTimeMillis())
        viewModelScope.launch { repository.saveItem(updated) }
    }

    fun toggleFormatItalic(itemId: String) {
        val currentItems = outlineItems.value
        val item = currentItems.find { it.id == itemId } ?: return
        recordSnapshot(currentItems)
        val updated = item.copy(isItalic = !item.isItalic, updatedAt = System.currentTimeMillis())
        viewModelScope.launch { repository.saveItem(updated) }
    }

    fun toggleFormatStrikethrough(itemId: String) {
        val currentItems = outlineItems.value
        val item = currentItems.find { it.id == itemId } ?: return
        recordSnapshot(currentItems)
        val updated = item.copy(isStrikethrough = !item.isStrikethrough, updatedAt = System.currentTimeMillis())
        viewModelScope.launch { repository.saveItem(updated) }
    }

    fun toggleFormatCode(itemId: String) {
        val currentItems = outlineItems.value
        val item = currentItems.find { it.id == itemId } ?: return
        recordSnapshot(currentItems)
        val updated = item.copy(isCode = !item.isCode, updatedAt = System.currentTimeMillis())
        viewModelScope.launch { repository.saveItem(updated) }
    }

    fun setHeadingLevel(itemId: String, level: Int) {
        val currentItems = outlineItems.value
        val item = currentItems.find { it.id == itemId } ?: return
        recordSnapshot(currentItems)
        val newLevel = if (item.headingLevel == level) 0 else level
        val updated = item.copy(headingLevel = newLevel, updatedAt = System.currentTimeMillis())
        viewModelScope.launch { repository.saveItem(updated) }
    }

    fun setItemColors(itemId: String, textColor: String?, backgroundColor: String?) {
        val currentItems = outlineItems.value
        val item = currentItems.find { it.id == itemId } ?: return
        recordSnapshot(currentItems)
        val updated = item.copy(
            textColor = textColor,
            backgroundColor = backgroundColor,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch { repository.saveItem(updated) }
    }

    fun setItemUrl(itemId: String, url: String?) {
        val currentItems = outlineItems.value
        val item = currentItems.find { it.id == itemId } ?: return
        recordSnapshot(currentItems)

        val mediaInfo = MediaRecognizer.recognize(url)
        val mediaType = when (mediaInfo.type) {
            com.example.media.RecognizedMediaType.YOUTUBE_VIDEO -> "youtube_video"
            com.example.media.RecognizedMediaType.YOUTUBE_PLAYLIST -> "youtube_playlist"
            com.example.media.RecognizedMediaType.AUDIO_FILE -> "audio"
            com.example.media.RecognizedMediaType.URL -> "url"
            else -> null
        }

        val updated = item.copy(
            url = url,
            mediaType = mediaType,
            mediaUri = if (mediaType == "audio") url else item.mediaUri,
            mediaTitle = if (mediaInfo.type != com.example.media.RecognizedMediaType.NONE) mediaInfo.title else item.mediaTitle,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch { repository.saveItem(updated) }
    }

    fun attachAudioUri(itemId: String, uriString: String, title: String? = null) {
        val currentItems = outlineItems.value
        val item = currentItems.find { it.id == itemId } ?: return
        recordSnapshot(currentItems)
        val updated = item.copy(
            mediaType = "audio",
            mediaUri = uriString,
            mediaTitle = title ?: uriString.substringAfterLast("/"),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch { repository.saveItem(updated) }
    }

    // ================= ZOOM / FOCUS ================= //

    fun focusOnItem(itemId: String) {
        _focusedRootId.value = itemId
    }

    fun unfocus() {
        _focusedRootId.value = null
    }

    // ================= MULTI-SELECTION ================= //

    fun toggleItemSelection(itemId: String) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (current.contains(itemId)) {
            current - itemId
        } else {
            current + itemId
        }
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun deleteSelectedItems() {
        val selected = _selectedItemIds.value
        if (selected.isEmpty()) return
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        recordSnapshot(currentItems)

        var updated = currentItems
        for (id in selected) {
            updated = TreeOperations.deleteSubtree(updated, id)
        }
        _selectedItemIds.value = emptySet()
        if (updated.isEmpty()) {
            val (listWithEmpty, _) = TreeOperations.addSiblingAfter(emptyList(), null, noteId)
            viewModelScope.launch { repository.saveAllItems(noteId, listWithEmpty) }
        } else {
            viewModelScope.launch { repository.saveAllItems(noteId, updated) }
        }
    }

    fun indentSelectedItems() {
        val selected = _selectedItemIds.value
        if (selected.isEmpty()) return
        val noteId = _activeNoteId.value ?: return
        var current = outlineItems.value
        recordSnapshot(current)

        for (id in selected) {
            TreeOperations.indentItem(current, id)?.let { current = it }
        }
        viewModelScope.launch { repository.saveAllItems(noteId, current) }
    }

    fun unindentSelectedItems() {
        val selected = _selectedItemIds.value
        if (selected.isEmpty()) return
        val noteId = _activeNoteId.value ?: return
        var current = outlineItems.value
        recordSnapshot(current)

        for (id in selected) {
            TreeOperations.unindentItem(current, id)?.let { current = it }
        }
        viewModelScope.launch { repository.saveAllItems(noteId, current) }
    }

    // ================= UNDO / REDO ================= //

    private fun recordSnapshot(items: List<OutlineItem>) {
        undoStack.push(items)
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    private fun clearUndoRedo() {
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        val previousItems = undoStack.pop()
        redoStack.push(currentItems)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true

        viewModelScope.launch {
            repository.saveAllItems(noteId, previousItems)
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        val nextItems = redoStack.pop()
        undoStack.push(currentItems)
        _canUndo.value = true
        _canRedo.value = redoStack.isNotEmpty()

        viewModelScope.launch {
            repository.saveAllItems(noteId, nextItems)
        }
    }

    // ================= SEARCH ================= //

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchDebounceJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        _isSearching.value = true
        searchDebounceJob = viewModelScope.launch {
            delay(150)
            val results = repository.search(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun deleteEmptyItemAndFocusPrevious(itemId: String) {
        val noteId = _activeNoteId.value ?: return
        val currentItems = outlineItems.value
        val flatNodes = flattenedTree.value
        val currentIndex = flatNodes.indexOfFirst { it.item.id == itemId }

        if (currentIndex > 0) {
            val prevNode = flatNodes[currentIndex - 1]
            val nodeToDelete = flatNodes[currentIndex]

            if (!nodeToDelete.hasChildren) {
                recordSnapshot(currentItems)
                val updated = TreeOperations.deleteSubtree(currentItems, itemId)
                _activeEditingItemId.value = prevNode.item.id
                viewModelScope.launch {
                    repository.saveAllItems(noteId, updated)
                }
            } else {
                _activeEditingItemId.value = prevNode.item.id
            }
        }
    }

    // ================= IMPORT / EXPORT ================= //

    fun getPlainTextExport(): String {
        val note = activeNote.value ?: return ""
        val items = outlineItems.value
        return PlainTextExporter.exportToPlainText(note, items)
    }

    fun getOpmlExport(): String {
        val note = activeNote.value ?: return ""
        val items = outlineItems.value
        return OpmlExporter.exportToOpml(note, items)
    }

    suspend fun exportAllNotesAsZip(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val folders = repository.getAllFoldersForBackup()
            val notes = repository.getAllNotesForBackup().filter { !it.isDeleted }
            val allItems = repository.getAllItemsForBackup()
            val itemsMap = allItems.groupBy { it.noteId }

            MarkdownZipExporter.createZipFile(context, folders, notes, itemsMap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportAllNotesAsOpmlZip(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val folders = repository.getAllFoldersForBackup()
            val notes = repository.getAllNotesForBackup().filter { !it.isDeleted }
            val allItems = repository.getAllItemsForBackup()
            val itemsMap = allItems.groupBy { it.noteId }

            OpmlZipExporter.createZipFile(context, folders, notes, itemsMap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importZipArchive(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val result = ZipImporter.importZip(context, uri, repository)
                _userMessage.emit("Imported ${result.notesCount} notes and ${result.foldersCount} folders from ZIP")
            } catch (e: Exception) {
                _userMessage.emit("Failed to import ZIP: ${e.localizedMessage}")
            }
        }
    }

    fun importMarkdownContent(mdText: String, folderId: String? = null) {
        viewModelScope.launch {
            try {
                val tempNoteId = UUID.randomUUID().toString()
                val parsed = MarkdownImporter.parseMarkdown(mdText, tempNoteId)
                val newNoteId = repository.createNote(parsed.title, folderId)
                val itemsWithCorrectId = parsed.items.map { it.copy(noteId = newNoteId) }
                repository.saveAllItems(newNoteId, itemsWithCorrectId)
                _userMessage.emit("Imported \"${parsed.title}\" (${itemsWithCorrectId.size} items)")
                openNote(newNoteId)
            } catch (e: Exception) {
                _userMessage.emit("Failed to import Markdown: ${e.localizedMessage}")
            }
        }
    }

    fun importOpmlContent(opmlXml: String, folderId: String? = null) {
        viewModelScope.launch {
            try {
                val tempNoteId = UUID.randomUUID().toString()
                val parsed = OpmlImporter.parseOpml(opmlXml, tempNoteId)
                val newNoteId = repository.createNote(parsed.title, folderId)
                val itemsWithCorrectId = parsed.items.map { it.copy(noteId = newNoteId) }
                repository.saveAllItems(newNoteId, itemsWithCorrectId)
                _userMessage.emit("Imported \"${parsed.title}\" (${itemsWithCorrectId.size} items)")
                openNote(newNoteId)
            } catch (e: Exception) {
                _userMessage.emit("Failed to import OPML: ${e.localizedMessage}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioController.release()
    }
}
