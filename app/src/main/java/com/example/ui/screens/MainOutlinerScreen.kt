package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Folder
import com.example.domain.model.Note
import com.example.ui.components.AiNoteAssistantSheet
import com.example.ui.components.BreadcrumbsBar
import com.example.ui.components.ColorPickerSheet
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FlashcardsView
import com.example.ui.components.FolderTreeView
import com.example.ui.components.LinkInputDialog
import com.example.ui.components.MeaningPopoverDialog
import com.example.ui.components.MindMapView
import com.example.ui.components.NoteDetailsSheet
import com.example.ui.components.OutlinerItemRow
import com.example.ui.components.OutlinerToolbar
import com.example.ui.components.SearchDialog
import com.example.ui.components.SimpleInputDialog
import com.example.ui.components.SlidesView
import com.example.ui.viewmodel.NoteSortOption
import com.example.ui.screens.AiResearchScreen
import com.example.ui.theme.AppDensity
import com.example.ui.theme.FontManager
import com.example.ui.viewmodel.AppView
import com.example.ui.viewmodel.OutlinerViewModel
import com.example.util.PdfExporter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainOutlinerScreen(
    viewModel: OutlinerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val currentView by viewModel.currentView.collectAsState()
    val activeNote by viewModel.activeNote.collectAsState()
    val outlineItems by viewModel.outlineItems.collectAsState()
    val flattenedTree by viewModel.flattenedTree.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val focusedRootId by viewModel.focusedRootId.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val selectedFolderIds by viewModel.selectedFolderIds.collectAsState()
    val activeEditingItemId by viewModel.activeEditingItemId.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val notes by viewModel.notes.collectAsState()

    val isMultiSelectMode = selectedNoteIds.isNotEmpty() || selectedFolderIds.isNotEmpty()

    // Sidebar and interface preferences
    val showFavoritesInSidebar by viewModel.showFavoritesInSidebar.collectAsState()
    val showRecentInSidebar by viewModel.showRecentInSidebar.collectAsState()
    val showTrashInSidebar by viewModel.showTrashInSidebar.collectAsState()
    val showItemCounts by viewModel.showItemCounts.collectAsState()
    val hideWordCount by viewModel.hideWordCount.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isAiFeatureEnabled by viewModel.isAiFeatureEnabled.collectAsState()
    val appDensityName by viewModel.appDensity.collectAsState()
    val appFontFamilyKey by viewModel.appFontFamily.collectAsState()
    val customFontPath by viewModel.customFontPath.collectAsState()
    val activeFontFamily = FontManager.getFontFamily(LocalContext.current, appFontFamilyKey, customFontPath)

    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var showSearchDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var parentFolderForNewDialog by remember { mutableStateOf<String?>(null) }
    var showRenameFolderDialog by remember { mutableStateOf<Folder?>(null) }
    var noteToMove by remember { mutableStateOf<Note?>(null) }
    var showBatchMoveFolderDialog by remember { mutableStateOf(false) }
    var noteToExport by remember { mutableStateOf<Note?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var meaningPopoverText by remember { mutableStateOf<String?>(null) }
    var showMindMap by remember { mutableStateOf(false) }
    var showFlashcards by remember { mutableStateOf(false) }
    var showSlides by remember { mutableStateOf(false) }
    var showNoteDetailsSheet by remember { mutableStateOf(false) }

    // Title focus requester for auto-focusing on note opening
    val titleFocusRequester = remember { FocusRequester() }
    val isNoteAiPanelOpen by viewModel.isNoteAiPanelOpen.collectAsState()

    // Android back navigation handler
    BackHandler(enabled = isNoteAiPanelOpen || drawerState.isOpen || isMultiSelectMode || currentView !is AppView.AllNotes) {
        when {
            isNoteAiPanelOpen -> viewModel.toggleNoteAiPanel(false)
            drawerState.isOpen -> scope.launch { drawerState.close() }
            isMultiSelectMode -> viewModel.clearOverviewSelection()
            else -> viewModel.navigateBack()
        }
    }

    // Auto-focus title if newly opened note has empty or placeholder title
    LaunchedEffect(activeNote?.id) {
        if (activeNote != null && (activeNote?.title.isNullOrBlank() || activeNote?.title == "Untitled" || activeNote?.title == "New Note")) {
            try {
                titleFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus requester not attached yet
            }
        }
    }

    // User message toasts / snackbar
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val activeEditingItem = outlineItems.find { it.id == activeEditingItemId }

    if (currentView is AppView.AiResearch) {
        val aiView = currentView as AppView.AiResearch
        AiResearchScreen(
            viewModel = viewModel,
            folderId = aiView.folderId,
            noteId = aiView.noteId,
            onBack = { viewModel.navigateBack() }
        )
        return
    }

    val rowVerticalPadding = when (appDensityName) {
        AppDensity.COMPACT.name -> 2.dp
        AppDensity.COZY.name -> 5.dp
        else -> 8.dp
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // App Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Logo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Outliner",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Offline & Local First",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Primary Navigation Items
                    NavigationDrawerItem(
                        label = {
                            Text(if (showItemCounts) "All Notes (${notes.size})" else "All Notes")
                        },
                        icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                        selected = currentView is AppView.AllNotes,
                        onClick = {
                            viewModel.navigateTo(AppView.AllNotes)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors()
                    )

                    if (isAiFeatureEnabled) {
                        NavigationDrawerItem(
                            label = { Text("AI Web Research") },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            selected = currentView is AppView.AiResearch,
                            onClick = {
                                viewModel.openAiResearch()
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors()
                        )
                    }

                    if (showFavoritesInSidebar) {
                        NavigationDrawerItem(
                            label = { Text("Favorites") },
                            icon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B)) },
                            selected = currentView is AppView.Favorites,
                            onClick = {
                                viewModel.navigateTo(AppView.Favorites)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (showRecentInSidebar) {
                        NavigationDrawerItem(
                            label = { Text("Recent") },
                            icon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                            selected = currentView is AppView.Recent,
                            onClick = {
                                viewModel.navigateTo(AppView.Recent)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (showTrashInSidebar) {
                        NavigationDrawerItem(
                            label = { Text("Trash") },
                            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            selected = currentView is AppView.Trash,
                            onClick = {
                                viewModel.navigateTo(AppView.Trash)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    NavigationDrawerItem(
                        label = { Text("Settings & Backup") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        selected = currentView is AppView.Settings,
                        onClick = {
                            viewModel.navigateTo(AppView.Settings)
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Folders Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FOLDERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                parentFolderForNewDialog = null
                                showNewFolderDialog = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "New Folder",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Folder Tree View in Drawer
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        item {
                            FolderTreeView(
                                folders = folders,
                                selectedFolderId = when (currentView) {
                                    is AppView.FolderView -> (currentView as AppView.FolderView).folderId
                                    else -> null
                                },
                                showItemCounts = showItemCounts,
                                onSelectFolder = { folderId ->
                                    if (folderId == null) {
                                        viewModel.navigateTo(AppView.AllNotes)
                                    } else {
                                        viewModel.navigateTo(AppView.FolderView(folderId))
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                onCreateSubfolder = { parentId ->
                                    parentFolderForNewDialog = parentId
                                    showNewFolderDialog = true
                                },
                                onCreateNoteInFolder = { folderId ->
                                    viewModel.createNote("Untitled", folderId)
                                    scope.launch { drawerState.close() }
                                },
                                onRenameFolder = { folder ->
                                    showRenameFolderDialog = folder
                                },
                                onToggleExpand = { id ->
                                    viewModel.toggleFolderExpanded(id)
                                },
                                onDeleteFolder = { id ->
                                    viewModel.deleteFolder(id)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Create Note Button
                    OutlinedButton(
                        onClick = {
                            viewModel.createNote()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Note")
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (currentView !is AppView.AiResearch) {
                    val isMultiSelectMode = selectedNoteIds.isNotEmpty()

                TopAppBar(
                    title = {
                        if (isMultiSelectMode) {
                            val totalSelected = selectedNoteIds.size + selectedFolderIds.size
                            Text(
                                text = "$totalSelected selected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        } else if (currentView is AppView.Editor && activeNote != null) {
                            val folderName = activeNote?.folderId?.let { fId -> folders.find { it.id == fId }?.name }
                            Text(
                                text = if (folderName != null) "Folder: $folderName" else "Note Outline",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = when (currentView) {
                                    is AppView.AllNotes -> "All Notes"
                                    is AppView.Favorites -> "Favorites"
                                    is AppView.Recent -> "Recent Notes"
                                    is AppView.Trash -> "Trash"
                                    is AppView.Settings -> "Settings"
                                    is AppView.FolderView -> {
                                        val fId = (currentView as AppView.FolderView).folderId
                                        folders.find { it.id == fId }?.name ?: "Folder"
                                    }
                                    else -> "Outliner"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    },
                    navigationIcon = {
                        if (isMultiSelectMode) {
                            IconButton(onClick = { viewModel.clearOverviewSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                            }
                        } else if (currentView !is AppView.AllNotes) {
                            IconButton(onClick = { viewModel.navigateBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    actions = {
                        if (isMultiSelectMode) {
                            // Select All Action
                            IconButton(onClick = {
                                val currentNotes = when (currentView) {
                                    is AppView.Favorites -> notes.filter { it.isFavorite }
                                    is AppView.Recent -> notes.sortedByDescending { it.lastAccessedAt }
                                    is AppView.FolderView -> {
                                        val fId = (currentView as AppView.FolderView).folderId
                                        notes.filter { it.folderId == fId }
                                    }
                                    is AppView.AllNotes -> notes.filter { it.folderId == null }
                                    else -> notes
                                }
                                val currentFolders = if (currentView is AppView.AllNotes) folders else emptyList()
                                viewModel.selectAllOverview(currentNotes.map { it.id }, currentFolders.map { it.id })
                            }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                            }

                            // Move Multiple Notes to Folder Action
                            if (selectedNoteIds.isNotEmpty()) {
                                IconButton(onClick = { showBatchMoveFolderDialog = true }) {
                                    Icon(Icons.Default.DriveFileMove, contentDescription = "Move to Folder", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // Delete Selected Notes & Folders
                            IconButton(onClick = { viewModel.deleteSelectedOverviewItems() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            val isOverviewView = currentView is AppView.AllNotes || currentView is AppView.FolderView || currentView is AppView.Favorites || currentView is AppView.Recent

                            // Sort Option Button (beside search on homescreen / overview)
                            if (isOverviewView) {
                                var sortMenuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { sortMenuExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Sort,
                                            contentDescription = "Sort Notes & Folders"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = sortMenuExpanded,
                                        onDismissRequest = { sortMenuExpanded = false }
                                    ) {
                                        Text(
                                            text = "ARRANGE & SORT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                        NoteSortOption.values().forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = option.displayName,
                                                            fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                        if (sortOption == option) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setSortOption(option)
                                                    sortMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Search Button
                            IconButton(onClick = { showSearchDialog = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }

                            if (currentView is AppView.Editor && activeNote != null) {
                                // Star Favorite
                                IconButton(onClick = {
                                    viewModel.toggleNoteFavorite(activeNote!!.id, !activeNote!!.isFavorite)
                                }) {
                                    Icon(
                                        imageVector = if (activeNote!!.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (activeNote!!.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // AI Assistant Button immediately to the LEFT of three-dot menu (if enabled)
                                if (isAiFeatureEnabled) {
                                    IconButton(onClick = {
                                        viewModel.toggleNoteAiPanel(true)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "AI Note Assistant",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Right Sidebar / More Menu
                                var menuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        // Section: TRANSFORM
                                        Text(
                                            text = "TRANSFORM",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Mind Map") },
                                            leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.openMindMap(activeNote!!.id)
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Flashcards") },
                                            leadingIcon = { Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.openFlashcards(activeNote!!.id)
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Slides") },
                                            leadingIcon = { Icon(Icons.Default.Slideshow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.openSlides(activeNote!!.id)
                                            }
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        DropdownMenuItem(
                                            text = { Text("Find Meaning") },
                                            leadingIcon = { Icon(Icons.Default.AutoStories, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                val selText = activeEditingItem?.text?.trim() ?: ""
                                                meaningPopoverText = selText
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Export Note") },
                                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                noteToExport = activeNote
                                                showExportDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Move to Folder") },
                                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                noteToMove = activeNote
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Duplicate Note") },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.duplicateNote(activeNote!!.id)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Collapse All") },
                                            leadingIcon = { Icon(Icons.Default.UnfoldLess, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.collapseAll()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Expand All") },
                                            leadingIcon = { Icon(Icons.Default.UnfoldMore, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.expandAll()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.deleteNote(activeNote!!.id)
                                            }
                                        )

                                        if (!hideWordCount) {
                                            // Grey small word count line just beneath Move to Trash button
                                            val totalWordCount = outlineItems.sumOf {
                                                it.text.trim().split("\\s+".toRegex()).count { w -> w.isNotBlank() }
                                            }
                                            val formattedWordCount = NumberFormat.getNumberInstance(Locale.US).format(totalWordCount) + " words"

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Text(
                                                text = formattedWordCount,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                }
            },
            floatingActionButton = {
                val isNotesOverview = currentView is AppView.AllNotes || currentView is AppView.FolderView || currentView is AppView.Favorites || currentView is AppView.Recent
                if (isNotesOverview && !isMultiSelectMode) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // AI Web Research FAB positioned just above (+) button (if enabled)
                        if (isAiFeatureEnabled) {
                            SmallFloatingActionButton(
                                onClick = {
                                    val fId = if (currentView is AppView.FolderView) (currentView as AppView.FolderView).folderId else null
                                    viewModel.openAiResearch(folderId = fId)
                                },
                                shape = RoundedCornerShape(12.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Web Research",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // (+) New Note Main FAB
                        FloatingActionButton(
                            onClick = {
                                val fId = if (currentView is AppView.FolderView) (currentView as AppView.FolderView).folderId else null
                                viewModel.createNote("Untitled", folderId = fId)
                            },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Note",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (currentView is AppView.Editor && activeNote != null) {
                    OutlinerToolbar(
                        activeItem = activeEditingItem,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onAddChild = { activeEditingItemId?.let { viewModel.addChildItem(it) } },
                        onIndent = { activeEditingItemId?.let { viewModel.indentItem(it) } },
                        onUnindent = { activeEditingItemId?.let { viewModel.unindentItem(it) } },
                        onMoveUp = { activeEditingItemId?.let { viewModel.moveItemUp(it) } },
                        onMoveDown = { activeEditingItemId?.let { viewModel.moveItemDown(it) } },
                        onToggleCheckbox = { activeEditingItemId?.let { viewModel.toggleItemCheckbox(it) } },
                        onToggleBold = { activeEditingItemId?.let { viewModel.toggleFormatBold(it) } },
                        onToggleItalic = { activeEditingItemId?.let { viewModel.toggleFormatItalic(it) } },
                        onToggleStrikethrough = { activeEditingItemId?.let { viewModel.toggleFormatStrikethrough(it) } },
                        onToggleCode = { activeEditingItemId?.let { viewModel.toggleFormatCode(it) } },
                        onSetHeading = { level -> activeEditingItemId?.let { viewModel.setHeadingLevel(it, level) } },
                        onOpenColorPicker = { showColorPicker = true },
                        onOpenLinkDialog = { showLinkDialog = true },
                        onFocusItem = { activeEditingItemId?.let { viewModel.focusOnItem(it) } },
                        onDuplicate = {
                            activeEditingItemId?.let { viewModel.duplicateSubtree(it) }
                        },
                        onDelete = {
                            activeEditingItemId?.let { viewModel.deleteSubtree(it) }
                        },
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        modifier = Modifier
                            .imePadding()
                            .navigationBarsPadding()
                    )
                }
            }
        ) { paddingValues ->
            val effectiveContentPadding = if (currentView is AppView.AiResearch) androidx.compose.foundation.layout.PaddingValues(0.dp) else paddingValues
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(effectiveContentPadding)
            ) {
                when (currentView) {
                    is AppView.Editor -> {
                        if (activeNote == null) {
                            EmptyStateView(
                                icon = Icons.Default.Checklist,
                                title = "No Note Selected",
                                subtitle = "Select a note from the drawer or create a new one.",
                                actionLabel = "Create Note",
                                onActionClick = { viewModel.createNote() }
                            )
                        } else {
                            val editorListState = androidx.compose.foundation.lazy.rememberLazyListState()
                            var swipeDragX by remember { mutableFloatStateOf(0f) }

                            // Auto-scroll to currently edited item so it is never hidden behind keyboard
                            LaunchedEffect(activeEditingItemId) {
                                if (activeEditingItemId != null) {
                                    val index = flattenedTree.indexOfFirst { it.item.id == activeEditingItemId }
                                    if (index >= 0) {
                                        editorListState.animateScrollToItem(index + 1)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(isAiFeatureEnabled) {
                                        if (isAiFeatureEnabled) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { swipeDragX = 0f },
                                                onDragEnd = {
                                                    if (swipeDragX < -40f) {
                                                        viewModel.toggleNoteAiPanel(true)
                                                    }
                                                    swipeDragX = 0f
                                                },
                                                onDragCancel = { swipeDragX = 0f },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    swipeDragX += dragAmount
                                                }
                                            )
                                        }
                                    }
                            ) {
                                // Zoom Breadcrumbs Header
                                if (focusedRootId != null && breadcrumbs.isNotEmpty()) {
                                    BreadcrumbsBar(
                                        noteTitle = activeNote?.title?.ifBlank { "Untitled" } ?: "Note",
                                        breadcrumbs = breadcrumbs,
                                        onNavigateToRoot = { viewModel.unfocus() },
                                        onNavigateToItem = { id -> viewModel.focusOnItem(id) }
                                    )
                                }

                                LazyColumn(
                                    state = editorListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    // Note Title as the very first line / Heading 1 with grey line beneath
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            var titleText by remember(activeNote?.id) {
                                                val raw = activeNote?.title ?: ""
                                                mutableStateOf(if (raw == "Untitled" || raw == "New Note") "" else raw)
                                            }

                                            BasicTextField(
                                                value = titleText,
                                                onValueChange = { newTitle ->
                                                    titleText = newTitle
                                                    viewModel.renameActiveNote(newTitle.ifBlank { "Untitled" })
                                                },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.displaySmall.copy(
                                                    fontFamily = activeFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                keyboardOptions = KeyboardOptions(
                                                    imeAction = ImeAction.Next
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onNext = {
                                                        // Move focus to first item or create first item
                                                        if (flattenedTree.isNotEmpty()) {
                                                            viewModel.setActiveEditingItem(flattenedTree.first().item.id)
                                                        } else {
                                                            viewModel.addSiblingItem(null)
                                                        }
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(titleFocusRequester)
                                                    .padding(vertical = 4.dp)
                                                    .onPreviewKeyEvent { keyEvent ->
                                                        if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                                                            if (flattenedTree.isNotEmpty()) {
                                                                viewModel.setActiveEditingItem(flattenedTree.first().item.id)
                                                            } else {
                                                                viewModel.addSiblingItem(null)
                                                            }
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    },
                                                decorationBox = { innerTextField ->
                                                    Box {
                                                        if (titleText.isEmpty()) {
                                                            Text(
                                                                text = "Untitled",
                                                                style = MaterialTheme.typography.displaySmall.copy(
                                                                    fontFamily = activeFontFamily,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                                                )
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                }
                                            )

                                            // Grey line beneath title
                                            HorizontalDivider(
                                                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                                                thickness = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    if (flattenedTree.isEmpty()) {
                                        item {
                                            EmptyStateView(
                                                icon = Icons.Default.Checklist,
                                                title = "Empty Note",
                                                subtitle = "Tap '+ Add Item' below or start typing your first outline item.",
                                                actionLabel = "+ Add First Item",
                                                onActionClick = { viewModel.addSiblingItem(null) }
                                            )
                                        }
                                    } else {
                                        items(
                                            items = flattenedTree,
                                            key = { it.item.id }
                                        ) { node ->
                                            val isFirst = flattenedTree.firstOrNull()?.item?.id == node.item.id
                                            OutlinerItemRow(
                                                node = node,
                                                isCurrentEditing = activeEditingItemId == node.item.id,
                                                isFirstItem = isFirst,
                                                audioController = viewModel.audioController,
                                                verticalPadding = rowVerticalPadding,
                                                onTextChange = { newText ->
                                                    viewModel.updateItemText(node.item.id, newText)
                                                },
                                                onDeleteEmptyItem = {
                                                    viewModel.deleteEmptyItemAndFocusPrevious(node.item.id)
                                                },
                                                onSplitItem = { before, after ->
                                                    viewModel.splitItem(node.item.id, before, after)
                                                },
                                                onAddSiblingAfter = {
                                                    viewModel.addSiblingItem(node.item.id)
                                                },
                                                onAddChild = {
                                                    viewModel.addChildItem(node.item.id)
                                                },
                                                onIndent = {
                                                    viewModel.indentItem(node.item.id)
                                                },
                                                onUnindent = {
                                                    viewModel.unindentItem(node.item.id)
                                                },
                                                onMoveUp = {
                                                    viewModel.moveItemUp(node.item.id)
                                                },
                                                onMoveDown = {
                                                    viewModel.moveItemDown(node.item.id)
                                                },
                                                onToggleCheckbox = {
                                                    viewModel.toggleItemCheckbox(node.item.id)
                                                },
                                                onToggleCollapsed = {
                                                    viewModel.toggleItemCollapsed(node.item.id)
                                                },
                                                onFocusGain = {
                                                    viewModel.setActiveEditingItem(node.item.id)
                                                },
                                                onZoomFocus = {
                                                    viewModel.focusOnItem(node.item.id)
                                                },
                                                onDuplicate = {
                                                    viewModel.duplicateSubtree(node.item.id)
                                                },
                                                onDelete = {
                                                    viewModel.deleteSubtree(node.item.id)
                                                },
                                                onOpenColorPicker = {
                                                    viewModel.setActiveEditingItem(node.item.id)
                                                    showColorPicker = true
                                                },
                                                onOpenLinkDialog = {
                                                    viewModel.setActiveEditingItem(node.item.id)
                                                    showLinkDialog = true
                                                },
                                                onSetHeading = { level ->
                                                    viewModel.setHeadingLevel(node.item.id, level)
                                                },
                                                onNavigateBackToTitle = {
                                                    try {
                                                        titleFocusRequester.requestFocus()
                                                    } catch (e: Exception) {
                                                        // Fallback
                                                    }
                                                },
                                                onLookupWord = { word ->
                                                    meaningPopoverText = word
                                                }
                                            )
                                        }

                                        item {
                                            Spacer(modifier = Modifier.height(120.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is AppView.AllNotes, is AppView.FolderView, is AppView.Favorites, is AppView.Recent -> {
                        val isAllNotesView = currentView is AppView.AllNotes
                        val displayedNotes = when (currentView) {
                            is AppView.Favorites -> notes.filter { it.isFavorite }
                            is AppView.Recent -> notes.sortedByDescending { it.lastAccessedAt }
                            is AppView.FolderView -> {
                                val fId = (currentView as AppView.FolderView).folderId
                                notes.filter { it.folderId == fId }
                            }
                            is AppView.AllNotes -> notes.filter { it.folderId == null }
                            else -> notes
                        }

                        val hasFoldersToShow = isAllNotesView && folders.isNotEmpty()

                        if (displayedNotes.isEmpty() && !hasFoldersToShow) {
                            EmptyStateView(
                                icon = Icons.Default.Checklist,
                                title = "No notes found",
                                subtitle = "Create your first note to get started.",
                                actionLabel = "New Note",
                                onActionClick = {
                                    val fId = if (currentView is AppView.FolderView) (currentView as AppView.FolderView).folderId else null
                                    viewModel.createNote("Untitled", folderId = fId)
                                }
                            )
                        } else {
                            NotesListScreen(
                                notes = displayedNotes,
                                folders = if (isAllNotesView) folders else emptyList(),
                                showFolders = isAllNotesView,
                                sortOption = sortOption,
                                selectedNoteIds = selectedNoteIds,
                                selectedFolderIds = selectedFolderIds,
                                onOpenNote = { viewModel.openNote(it.id) },
                                onOpenFolder = { folder -> viewModel.navigateTo(AppView.FolderView(folder.id)) },
                                onToggleSelectNote = { noteId -> viewModel.toggleNoteSelection(noteId) },
                                onToggleSelectFolder = { folderId -> viewModel.toggleFolderSelection(folderId) },
                                onToggleFavorite = { note -> viewModel.toggleNoteFavorite(note.id, !note.isFavorite) },
                                onDeleteNote = { note -> viewModel.deleteNote(note.id) },
                                onDuplicateNote = { note -> viewModel.duplicateNote(note.id) },
                                onMoveNote = { note -> noteToMove = note },
                                onExportNote = { note ->
                                    noteToExport = note
                                    showExportDialog = true
                                },
                                onDeleteFolder = { folder -> viewModel.deleteFolder(folder.id) },
                                onRenameFolder = { folder -> showRenameFolderDialog = folder }
                            )
                        }
                    }
                    is AppView.Trash -> {
                        TrashScreen(viewModel = viewModel)
                    }
                    is AppView.Settings -> {
                        SettingsScreen(viewModel = viewModel)
                    }
                    is AppView.AiResearch -> {
                        val aiView = currentView as AppView.AiResearch
                        AiResearchScreen(
                            viewModel = viewModel,
                            folderId = aiView.folderId,
                            noteId = aiView.noteId,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    is AppView.MindMapView -> {
                        if (activeNote != null) {
                            MindMapView(
                                note = activeNote!!,
                                allNodes = flattenedTree,
                                onClose = { viewModel.navigateBack() }
                            )
                        }
                    }
                    is AppView.FlashcardsView -> {
                        if (activeNote != null) {
                            FlashcardsView(
                                note = activeNote!!,
                                allNodes = flattenedTree,
                                onClose = { viewModel.navigateBack() }
                            )
                        }
                    }
                    is AppView.SlidesView -> {
                        if (activeNote != null) {
                            SlidesView(
                                note = activeNote!!,
                                allNodes = flattenedTree,
                                onClose = { viewModel.navigateBack() }
                            )
                        }
                    }
                }
            }
        }
    }

    // ================= MODALS & DIALOGS ================= //

    // Floating Meaning Popover Dialog
    if (meaningPopoverText != null) {
        MeaningPopoverDialog(
            initialText = meaningPopoverText ?: "",
            onDismiss = { meaningPopoverText = null }
        )
    }

    AiNoteAssistantSheet(
        isOpen = isNoteAiPanelOpen,
        viewModel = viewModel,
        onDismiss = { viewModel.toggleNoteAiPanel(false) }
    )

    if (showSearchDialog) {
        SearchDialog(
            query = searchQuery,
            results = searchResults,
            isSearching = isSearching,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            onSelectResult = { result ->
                viewModel.openNote(result.noteId, result.matchedItemId)
            },
            onDismiss = { showSearchDialog = false }
        )
    }

    if (showColorPicker && activeEditingItem != null) {
        ColorPickerSheet(
            currentTextColor = activeEditingItem.textColor,
            currentBgColor = activeEditingItem.backgroundColor,
            onApplyColors = { textColor, bgColor ->
                viewModel.setItemColors(activeEditingItem.id, textColor, bgColor)
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showLinkDialog && activeEditingItem != null) {
        LinkInputDialog(
            initialUrl = activeEditingItem.url ?: activeEditingItem.mediaUri,
            onConfirm = { url ->
                viewModel.setItemUrl(activeEditingItem.id, url)
                showLinkDialog = false
            },
            onRemove = {
                viewModel.setItemUrl(activeEditingItem.id, null)
                showLinkDialog = false
            },
            onDismiss = { showLinkDialog = false }
        )
    }

    if (showNewFolderDialog) {
        SimpleInputDialog(
            title = if (parentFolderForNewDialog != null) "New Subfolder" else "New Folder",
            label = "Folder Name",
            confirmLabel = "Create",
            onConfirm = { name ->
                viewModel.createFolder(name, parentFolderForNewDialog)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }

    if (showRenameFolderDialog != null) {
        val folder = showRenameFolderDialog!!
        SimpleInputDialog(
            title = "Rename Folder",
            initialValue = folder.name,
            label = "Folder Name",
            confirmLabel = "Rename",
            onConfirm = { newName ->
                viewModel.renameFolder(folder.id, newName)
                showRenameFolderDialog = null
            },
            onDismiss = { showRenameFolderDialog = null }
        )
    }

    // Move single note
    if (noteToMove != null) {
        MoveNoteDialog(
            folders = folders,
            currentFolderId = noteToMove!!.folderId,
            onSelectFolder = { folderId ->
                viewModel.moveNoteToFolder(noteToMove!!.id, folderId)
                noteToMove = null
            },
            onDismiss = { noteToMove = null }
        )
    }

    // Move batch selected notes
    if (showBatchMoveFolderDialog && selectedNoteIds.isNotEmpty()) {
        MoveNoteDialog(
            folders = folders,
            currentFolderId = null,
            onSelectFolder = { folderId ->
                viewModel.moveSelectedNotesToFolder(folderId)
                showBatchMoveFolderDialog = false
            },
            onDismiss = { showBatchMoveFolderDialog = false }
        )
    }

    // Export note dialog
    if (showExportDialog && noteToExport != null) {
        val exportTarget = noteToExport!!
        ExportNoteDialog(
            noteTitle = exportTarget.title.ifBlank { "Untitled" },
            onExportPdf = {
                val pdf = PdfExporter.exportNoteToPdf(context, exportTarget, flattenedTree)
                if (pdf != null) {
                    PdfExporter.sharePdf(context, pdf, "Outline - ${exportTarget.title}")
                }
                showExportDialog = false
                noteToExport = null
            },
            onExportPlainText = {
                val text = viewModel.getPlainTextExport()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Note Plain Text", text))
                Toast.makeText(context, "Plain text copied to clipboard", Toast.LENGTH_SHORT).show()
                showExportDialog = false
                noteToExport = null
            },
            onExportOpml = {
                val opml = viewModel.getOpmlExport()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Note OPML", opml))
                Toast.makeText(context, "OPML XML copied to clipboard", Toast.LENGTH_SHORT).show()
                showExportDialog = false
                noteToExport = null
            },
            onDismiss = {
                showExportDialog = false
                noteToExport = null
            }
        )
    }

    // Note Details Sheet
    if (showNoteDetailsSheet && activeNote != null) {
        NoteDetailsSheet(
            note = activeNote!!,
            items = outlineItems,
            showWordCount = true,
            onDismiss = { showNoteDetailsSheet = false },
            onOpenDictionary = {
                val selText = activeEditingItem?.text?.trim() ?: ""
                meaningPopoverText = selText
            },
            onCopyAsMarkdown = {
                val md = viewModel.getPlainTextExport()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Markdown Note", md))
                Toast.makeText(context, "Copied note as Markdown", Toast.LENGTH_SHORT).show()
            },
            onOpenMindMap = { showMindMap = true },
            onOpenFlashcards = { showFlashcards = true },
            onOpenSlides = { showSlides = true },
            onExportPdf = {
                val pdf = PdfExporter.exportNoteToPdf(context, activeNote!!, flattenedTree)
                if (pdf != null) {
                    PdfExporter.sharePdf(context, pdf, "Outline - ${activeNote!!.title}")
                }
            }
        )
    }

    // Transform Views
    if (showMindMap && activeNote != null) {
        MindMapView(
            note = activeNote!!,
            allNodes = flattenedTree,
            onClose = { showMindMap = false }
        )
    }

    if (showFlashcards && activeNote != null) {
        FlashcardsView(
            note = activeNote!!,
            allNodes = flattenedTree,
            onClose = { showFlashcards = false }
        )
    }

    if (showSlides && activeNote != null) {
        SlidesView(
            note = activeNote!!,
            allNodes = flattenedTree,
            onClose = { showSlides = false }
        )
    }
}

@Composable
private fun MoveNoteDialog(
    folders: List<Folder>,
    currentFolderId: String?,
    onSelectFolder: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Folder", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectFolder(null) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Root (No folder)", fontWeight = if (currentFolderId == null) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                items(folders) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectFolder(folder.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = folder.name, fontWeight = if (currentFolderId == folder.id) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ExportNoteDialog(
    noteTitle: String,
    onExportPdf: () -> Unit,
    onExportPlainText: () -> Unit,
    onExportOpml: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export \"$noteTitle\"", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select an export format to save as PDF or copy to clipboard:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(onClick = onExportPdf, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF Document (.pdf)")
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(onClick = onExportPlainText, modifier = Modifier.fillMaxWidth()) {
                    Text("Plain Text (Indented Outline)")
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(onClick = onExportOpml, modifier = Modifier.fillMaxWidth()) {
                    Text("OPML (Dynalist / OmniOutliner / Workflowy)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
