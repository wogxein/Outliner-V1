package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.remote.GeminiAiService
import com.example.domain.model.AIConversation
import com.example.domain.model.AIMessage
import com.example.domain.model.AiResearchState
import com.example.domain.model.Folder
import com.example.domain.model.GroundingCitation
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
import java.io.FileOutputStream
import java.util.Stack
import java.util.UUID

enum class AiQuickAction(val label: String, val promptInstruction: String) {
    MAKE_SHORTER("Make Shorter", "Please make your previous response more concise, highlighting only key takeaways."),
    EXPAND("Expand", "Please expand on the previous response with in-depth explanations, details, and context from the web."),
    SUMMARIZE("Summarize", "Please provide a concise executive summary of the previous topic."),
    TURN_INTO_OUTLINE("Turn into Outline", "Please structure the response as a clean hierarchical outline with main topics and indented sub-topics."),
    BULLET_POINTS("Bullet Points", "Please convert the response into clean, scannable bullet points."),
    EXPLAIN_SIMPLY("Explain Simply", "Please explain this topic in simple, beginner-friendly terms (ELI5 style)."),
    FIND_SOURCES("Find Sources", "Please find additional live web sources, authoritative articles, and references for this topic."),
    FACT_CHECK("Fact Check", "Please search the web to verify and fact-check all key claims and numbers in this response."),
    REGENERATE("Regenerate", "Please research this topic again from the live web and provide an improved, comprehensive answer.")
}

sealed class AppView {
    data class Editor(val noteId: String, val targetItemId: String? = null) : AppView()
    object AllNotes : AppView()
    data class FolderView(val folderId: String) : AppView()
    object Favorites : AppView()
    object Recent : AppView()
    object Trash : AppView()
    object Settings : AppView()
    data class AiResearch(val conversationId: String? = null, val folderId: String? = null, val noteId: String? = null) : AppView()
}

class OutlinerViewModel(
    application: Application,
    private val repository: OutlinerRepository
) : AndroidViewModel(application) {

    val audioController = AudioPlayerController(application)

    // Theme & Appearance Preferences
    private val prefs = application.getSharedPreferences("outliner_prefs", Context.MODE_PRIVATE)
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _customPrimaryColorHex = MutableStateFlow<String?>(prefs.getString("custom_primary_hex", null))
    val customPrimaryColorHex: StateFlow<String?> = _customPrimaryColorHex.asStateFlow()

    private val _customBgColorHex = MutableStateFlow<String?>(prefs.getString("custom_bg_hex", null))
    val customBgColorHex: StateFlow<String?> = _customBgColorHex.asStateFlow()

    private val _appFontFamily = MutableStateFlow(prefs.getString("app_font_family", "inter") ?: "inter")
    val appFontFamily: StateFlow<String> = _appFontFamily.asStateFlow()

    private val _customFontPath = MutableStateFlow<String?>(prefs.getString("custom_font_path", null))
    val customFontPath: StateFlow<String?> = _customFontPath.asStateFlow()

    private val _appFontSizeIndex = MutableStateFlow(prefs.getInt("app_font_size_idx", 2))
    val appFontSizeIndex: StateFlow<Int> = _appFontSizeIndex.asStateFlow()

    private val _appDensity = MutableStateFlow(prefs.getString("app_density", "COMFORTABLE") ?: "COMFORTABLE")
    val appDensity: StateFlow<String> = _appDensity.asStateFlow()

    // Sidebar & View Customization Preferences
    private val _showFavoritesInSidebar = MutableStateFlow(prefs.getBoolean("show_favorites_sidebar", true))
    val showFavoritesInSidebar: StateFlow<Boolean> = _showFavoritesInSidebar.asStateFlow()

    private val _showRecentInSidebar = MutableStateFlow(prefs.getBoolean("show_recent_sidebar", true))
    val showRecentInSidebar: StateFlow<Boolean> = _showRecentInSidebar.asStateFlow()

    private val _showTrashInSidebar = MutableStateFlow(prefs.getBoolean("show_trash_sidebar", true))
    val showTrashInSidebar: StateFlow<Boolean> = _showTrashInSidebar.asStateFlow()

    private val _showItemCounts = MutableStateFlow(prefs.getBoolean("show_item_counts", true))
    val showItemCounts: StateFlow<Boolean> = _showItemCounts.asStateFlow()

    private val _showWordCountInNote = MutableStateFlow(prefs.getBoolean("show_word_count_note", true))
    val showWordCountInNote: StateFlow<Boolean> = _showWordCountInNote.asStateFlow()

    // Custom AI Configuration
    private val _aiApiKey = MutableStateFlow(prefs.getString("custom_ai_api_key", "") ?: "")
    val aiApiKey: StateFlow<String> = _aiApiKey.asStateFlow()

    private val _aiBaseUrl = MutableStateFlow(prefs.getString("custom_ai_base_url", GeminiAiService.DEFAULT_BASE_URL) ?: GeminiAiService.DEFAULT_BASE_URL)
    val aiBaseUrl: StateFlow<String> = _aiBaseUrl.asStateFlow()

    private val _aiModelId = MutableStateFlow(prefs.getString("custom_ai_model_id", GeminiAiService.DEFAULT_MODEL) ?: GeminiAiService.DEFAULT_MODEL)
    val aiModelId: StateFlow<String> = _aiModelId.asStateFlow()

    private val _aiCustomTitle = MutableStateFlow(prefs.getString("custom_ai_title", "Research Assistant") ?: "Research Assistant")
    val aiCustomTitle: StateFlow<String> = _aiCustomTitle.asStateFlow()

    private val _aiCustomSystemPrompt = MutableStateFlow(prefs.getString("custom_ai_system_prompt", "") ?: "")
    val aiCustomSystemPrompt: StateFlow<String> = _aiCustomSystemPrompt.asStateFlow()

    // Multi-selection of notes in AllNotes/Folders
    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNoteIds: StateFlow<Set<String>> = _selectedNoteIds.asStateFlow()

    fun setAiApiKey(key: String) {
        _aiApiKey.value = key.trim()
        prefs.edit().putString("custom_ai_api_key", key.trim()).apply()
    }

    fun setAiBaseUrl(url: String) {
        _aiBaseUrl.value = url.trim()
        prefs.edit().putString("custom_ai_base_url", url.trim()).apply()
    }

    fun setAiModelId(model: String) {
        _aiModelId.value = model.trim()
        prefs.edit().putString("custom_ai_model_id", model.trim()).apply()
    }

    fun setAiCustomTitle(title: String) {
        _aiCustomTitle.value = title.trim()
        prefs.edit().putString("custom_ai_title", title.trim()).apply()
    }

    fun setAiCustomSystemPrompt(prompt: String) {
        _aiCustomSystemPrompt.value = prompt.trim()
        prefs.edit().putString("custom_ai_system_prompt", prompt.trim()).apply()
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
    }

    fun setCustomPrimaryColor(hex: String?) {
        _customPrimaryColorHex.value = hex
        prefs.edit().putString("custom_primary_hex", hex).apply()
    }

    fun setCustomBgColor(hex: String?) {
        _customBgColorHex.value = hex
        prefs.edit().putString("custom_bg_hex", hex).apply()
    }

    fun setAppFontFamily(key: String) {
        _appFontFamily.value = key
        prefs.edit().putString("app_font_family", key).apply()
    }

    fun setAppFontSizeIndex(idx: Int) {
        val clamped = idx.coerceIn(0, 4)
        _appFontSizeIndex.value = clamped
        prefs.edit().putInt("app_font_size_idx", clamped).apply()
    }

    fun setAppDensity(densityName: String) {
        _appDensity.value = densityName
        prefs.edit().putString("app_density", densityName).apply()
    }

    fun setShowFavoritesInSidebar(show: Boolean) {
        _showFavoritesInSidebar.value = show
        prefs.edit().putBoolean("show_favorites_sidebar", show).apply()
    }

    fun setShowRecentInSidebar(show: Boolean) {
        _showRecentInSidebar.value = show
        prefs.edit().putBoolean("show_recent_sidebar", show).apply()
    }

    fun setShowTrashInSidebar(show: Boolean) {
        _showTrashInSidebar.value = show
        prefs.edit().putBoolean("show_trash_sidebar", show).apply()
    }

    fun setShowItemCounts(show: Boolean) {
        _showItemCounts.value = show
        prefs.edit().putBoolean("show_item_counts", show).apply()
    }

    fun setShowWordCountInNote(show: Boolean) {
        _showWordCountInNote.value = show
        prefs.edit().putBoolean("show_word_count_note", show).apply()
    }

    fun importCustomFont(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val fontDir = File(context.filesDir, "fonts")
                if (!fontDir.exists()) fontDir.mkdirs()
                val fontFile = File(fontDir, "custom_font_${System.currentTimeMillis()}.ttf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(fontFile).use { output ->
                        input.copyTo(output)
                    }
                }
                _customFontPath.value = fontFile.absolutePath
                _appFontFamily.value = "custom"
                prefs.edit()
                    .putString("custom_font_path", fontFile.absolutePath)
                    .putString("app_font_family", "custom")
                    .apply()
                _userMessage.emit("Custom font imported successfully")
            } catch (e: Exception) {
                _userMessage.emit("Failed to load font: ${e.localizedMessage}")
            }
        }
    }

    // Multi-note operations
    fun toggleNoteSelection(noteId: String) {
        val current = _selectedNoteIds.value
        _selectedNoteIds.value = if (current.contains(noteId)) current - noteId else current + noteId
    }

    fun selectAllNotes(allIds: List<String>) {
        _selectedNoteIds.value = allIds.toSet()
    }

    fun clearNoteSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun deleteSelectedNotes() {
        val idsToDelete = _selectedNoteIds.value.toList()
        if (idsToDelete.isEmpty()) return
        viewModelScope.launch {
            idsToDelete.forEach { id ->
                repository.softDeleteNote(id)
            }
            _selectedNoteIds.value = emptySet()
            _userMessage.emit("Moved ${idsToDelete.size} notes to trash")
        }
    }

    fun moveSelectedNotesToFolder(folderId: String?) {
        val idsToMove = _selectedNoteIds.value.toList()
        if (idsToMove.isEmpty()) return
        viewModelScope.launch {
            idsToMove.forEach { id ->
                repository.moveNoteToFolder(id, folderId)
            }
            _selectedNoteIds.value = emptySet()
            _userMessage.emit("Moved ${idsToMove.size} notes to folder")
        }
    }

    fun getMarkdownForNote(noteId: String): String {
        val note = notes.value.find { it.id == noteId } ?: activeNote.value ?: return ""
        val items = outlineItems.value
        return PlainTextExporter.exportToPlainText(note, items)
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

    // AI Research & Assistant State
    private val _activeAiConversationId = MutableStateFlow<String?>(null)
    val activeAiConversationId: StateFlow<String?> = _activeAiConversationId.asStateFlow()

    private val _aiResearchState = MutableStateFlow<AiResearchState>(AiResearchState.Idle)
    val aiResearchState: StateFlow<AiResearchState> = _aiResearchState.asStateFlow()

    private val _isNoteAiPanelOpen = MutableStateFlow(false)
    val isNoteAiPanelOpen: StateFlow<Boolean> = _isNoteAiPanelOpen.asStateFlow()

    private val _attachedAiUrl = MutableStateFlow<String?>(null)
    val attachedAiUrl: StateFlow<String?> = _attachedAiUrl.asStateFlow()

    val allAiConversations: StateFlow<List<AIConversation>> = repository.allAiConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAiConversation: StateFlow<AIConversation?> = _activeAiConversationId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observeAiConversation(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentAiMessages: StateFlow<List<AIMessage>> = _activeAiConversationId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getAiMessages(id)
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

    fun navigateBack() {
        when (val v = _currentView.value) {
            is AppView.AiResearch -> {
                if (v.folderId != null) {
                    navigateTo(AppView.FolderView(v.folderId))
                } else if (v.noteId != null) {
                    openNote(v.noteId)
                } else {
                    navigateTo(AppView.AllNotes)
                }
            }
            is AppView.Editor -> {
                val active = activeNote.value
                if (active?.folderId != null) {
                    navigateTo(AppView.FolderView(active.folderId))
                } else {
                    navigateTo(AppView.AllNotes)
                }
            }
            is AppView.FolderView, is AppView.Favorites, is AppView.Recent, is AppView.Trash, is AppView.Settings -> {
                navigateTo(AppView.AllNotes)
            }
            else -> {}
        }
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

    // ================= AI RESEARCH & ASSISTANT METHODS ================= //

    fun openAiResearch(
        conversationId: String? = null,
        folderId: String? = null,
        noteId: String? = null
    ) {
        if (conversationId != null) {
            _activeAiConversationId.value = conversationId
        } else {
            // Find existing or create new
            viewModelScope.launch {
                val newId = repository.createAiConversation(
                    title = "New AI Research",
                    noteId = noteId,
                    folderId = folderId
                )
                _activeAiConversationId.value = newId
            }
        }
        _currentView.value = AppView.AiResearch(
            conversationId = conversationId,
            folderId = folderId,
            noteId = noteId
        )
    }

    fun startNewAiConversation(folderId: String? = null, noteId: String? = null) {
        viewModelScope.launch {
            val newId = repository.createAiConversation(
                title = "New AI Research",
                noteId = noteId ?: _activeNoteId.value,
                folderId = folderId
            )
            _activeAiConversationId.value = newId
            _aiResearchState.value = AiResearchState.Idle
            _attachedAiUrl.value = null
        }
    }

    fun selectAiConversation(conversationId: String) {
        _activeAiConversationId.value = conversationId
        _aiResearchState.value = AiResearchState.Idle
    }

    fun deleteAiConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteAiConversation(conversationId)
            if (_activeAiConversationId.value == conversationId) {
                val remaining = repository.allAiConversations
                _activeAiConversationId.value = null
            }
            _userMessage.emit("Conversation deleted")
        }
    }

    fun toggleNoteAiPanel(open: Boolean? = null) {
        val next = open ?: !_isNoteAiPanelOpen.value
        _isNoteAiPanelOpen.value = next
        if (next && _activeAiConversationId.value == null) {
            startNewAiConversation(noteId = _activeNoteId.value)
        }
    }

    fun setAttachedAiUrl(url: String?) {
        _attachedAiUrl.value = url?.trim()?.ifBlank { null }
    }

    fun sendAiMessage(
        prompt: String,
        customSystemPrompt: String? = null
    ) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            try {
                // Ensure conversation exists
                var convId = _activeAiConversationId.value
                if (convId == null) {
                    convId = repository.createAiConversation(
                        title = trimmed.take(40),
                        noteId = _activeNoteId.value
                    )
                    _activeAiConversationId.value = convId
                }

                // Save user message
                repository.saveAiMessage(
                    conversationId = convId,
                    role = "user",
                    content = trimmed
                )

                _aiResearchState.value = AiResearchState.Loading("Searching Google & analyzing...")

                // Build context from active note if present
                val noteContextBuilder = StringBuilder()
                val currentNote = activeNote.value
                val currentItems = outlineItems.value
                if (currentNote != null) {
                    noteContextBuilder.append("Active Note Title: \"${currentNote.title}\"\n")
                    if (currentItems.isNotEmpty()) {
                        noteContextBuilder.append("Note Content Outline:\n")
                        currentItems.take(50).forEach { item ->
                            val indent = if (item.parentId != null) "  - " else "- "
                            noteContextBuilder.append("$indent${item.text}\n")
                        }
                    }
                }

                val currentUrl = _attachedAiUrl.value
                if (!currentUrl.isNullOrBlank()) {
                    noteContextBuilder.append("\nUser requested research/analysis specifically for URL: $currentUrl\n")
                }

                val customPrompt = buildString {
                    if (_aiCustomSystemPrompt.value.isNotBlank()) {
                        append("${_aiCustomSystemPrompt.value}\n\n")
                    }
                    if (!customSystemPrompt.isNullOrBlank()) {
                        append("$customSystemPrompt\n\n")
                    }
                    append(noteContextBuilder.toString())
                }.ifBlank { null }

                // Fetch conversation history from DB (up to last 10 messages)
                val messagesHistory = repository.getAiMessagesList(convId)

                // Call Gemini with Search Grounding & User's Custom API Settings
                val result = GeminiAiService.research(
                    messages = messagesHistory,
                    systemPrompt = customPrompt,
                    enableSearchGrounding = true,
                    customApiKey = _aiApiKey.value.ifBlank { null },
                    customBaseUrl = _aiBaseUrl.value.ifBlank { null },
                    customModelId = _aiModelId.value.ifBlank { null }
                )

                result.onSuccess { aiResponse ->
                    repository.saveAiMessage(
                        conversationId = convId,
                        role = "model",
                        content = aiResponse.text,
                        citations = aiResponse.citations,
                        searchQueries = aiResponse.searchQueries
                    )
                    _attachedAiUrl.value = null
                    _aiResearchState.value = AiResearchState.Idle
                }.onFailure { error ->
                    _aiResearchState.value = AiResearchState.Error(error.localizedMessage ?: "Failed to generate AI research response")
                    _userMessage.emit("AI Research Error: ${error.localizedMessage}")
                }
            } catch (e: Exception) {
                _aiResearchState.value = AiResearchState.Error(e.localizedMessage ?: "Unexpected error")
                _userMessage.emit("AI Error: ${e.localizedMessage}")
            }
        }
    }

    fun executeAiQuickAction(action: AiQuickAction, targetMessage: AIMessage? = null) {
        val lastModelMessage = targetMessage ?: currentAiMessages.value.lastOrNull { it.role == "model" }
        val prompt = if (lastModelMessage != null) {
            "${action.promptInstruction}\n\nContext:\n${lastModelMessage.content}"
        } else {
            action.promptInstruction
        }
        sendAiMessage(prompt)
    }

    fun insertAiContentIntoActiveNote(message: AIMessage, atCursor: Boolean = false) {
        val noteId = _activeNoteId.value
        if (noteId == null) {
            // If no active note, create a new one!
            createNoteFromAiMessage(message)
            return
        }

        viewModelScope.launch {
            try {
                val targetItemId = if (atCursor) _activeEditingItemId.value else null
                repository.insertAiContentIntoNote(
                    noteId = noteId,
                    content = message.content,
                    citations = message.citations,
                    targetItemId = targetItemId
                )
                _userMessage.emit("Added AI content and citations to note")
            } catch (e: Exception) {
                _userMessage.emit("Failed to add to note: ${e.localizedMessage}")
            }
        }
    }

    fun createNoteFromAiMessage(message: AIMessage, customTitle: String? = null) {
        viewModelScope.launch {
            try {
                val folderId = when (val view = _currentView.value) {
                    is AppView.FolderView -> view.folderId
                    is AppView.AiResearch -> view.folderId
                    else -> null
                }

                val newNoteId = repository.createNoteFromAiContent(
                    content = message.content,
                    citations = message.citations,
                    folderId = folderId,
                    customTitle = customTitle
                )
                _userMessage.emit("Created new note from AI research")
                openNote(newNoteId)
            } catch (e: Exception) {
                _userMessage.emit("Failed to create note: ${e.localizedMessage}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioController.release()
    }
}
