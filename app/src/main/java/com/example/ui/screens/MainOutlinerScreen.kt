package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Folder
import com.example.ui.components.BreadcrumbsBar
import com.example.ui.components.ColorPickerSheet
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FolderTreeView
import com.example.ui.components.LinkInputDialog
import com.example.ui.components.OutlinerItemRow
import com.example.ui.components.OutlinerToolbar
import com.example.ui.components.SearchDialog
import com.example.ui.components.SimpleInputDialog
import com.example.ui.viewmodel.AppView
import com.example.ui.viewmodel.OutlinerViewModel
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter

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
    val activeEditingItemId by viewModel.activeEditingItemId.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

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
    var showRenameNoteDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf<Folder?>(null) }
    var showMoveNoteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isMultiSelectMode by remember { mutableStateOf(false) }

    // User message toasts / snackbar
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val activeEditingItem = outlineItems.find { it.id == activeEditingItemId }

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
                        label = { Text("All Notes (${notes.size})") },
                        icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                        selected = currentView is AppView.AllNotes || currentView is AppView.Editor,
                        onClick = {
                            viewModel.navigateTo(AppView.AllNotes)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors()
                    )

                    NavigationDrawerItem(
                        label = { Text("Favorites") },
                        icon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B)) },
                        selected = currentView is AppView.Favorites,
                        onClick = {
                            viewModel.navigateTo(AppView.Favorites)
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("Recent") },
                        icon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                        selected = currentView is AppView.Recent,
                        onClick = {
                            viewModel.navigateTo(AppView.Recent)
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("Trash") },
                        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        selected = currentView is AppView.Trash,
                        onClick = {
                            viewModel.navigateTo(AppView.Trash)
                            scope.launch { drawerState.close() }
                        }
                    )

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
                                    viewModel.createNote("New Note", folderId)
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
                TopAppBar(
                    title = {
                        if (currentView is AppView.Editor && activeNote != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showRenameNoteDialog = true }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            ) {
                                Text(
                                    text = activeNote?.title ?: "Untitled",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.DriveFileRenameOutline,
                                    contentDescription = "Rename",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
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
                                    is AppView.TagFilter -> "#${(currentView as AppView.TagFilter).tagName}"
                                    else -> "Outliner"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
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

                            // Multi-Select Toggle
                            IconButton(onClick = {
                                isMultiSelectMode = !isMultiSelectMode
                                if (!isMultiSelectMode) viewModel.clearSelection()
                            }) {
                                Icon(
                                    imageVector = if (isMultiSelectMode) Icons.Default.Close else Icons.Default.SelectAll,
                                    contentDescription = "Multi Select",
                                    tint = if (isMultiSelectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // More Actions
                            var menuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Export Note") },
                                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            showExportDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Move to Folder") },
                                        leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            showMoveNoteDialog = true
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
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (currentView is AppView.Editor && activeNote != null) {
                    OutlinerToolbar(
                        activeItem = activeEditingItem,
                        isMultiSelect = isMultiSelectMode,
                        selectedCount = selectedItemIds.size,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onAddSibling = { viewModel.addSiblingItem(activeEditingItemId) },
                        onAddChild = { activeEditingItemId?.let { viewModel.addChildItem(it) } },
                        onIndent = {
                            if (isMultiSelectMode) viewModel.indentSelectedItems()
                            else activeEditingItemId?.let { viewModel.indentItem(it) }
                        },
                        onUnindent = {
                            if (isMultiSelectMode) viewModel.unindentSelectedItems()
                            else activeEditingItemId?.let { viewModel.unindentItem(it) }
                        },
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
                            if (isMultiSelectMode) viewModel.deleteSelectedItems()
                            else activeEditingItemId?.let { viewModel.deleteSubtree(it) }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Zoom Breadcrumbs Header
                                if (focusedRootId != null && breadcrumbs.isNotEmpty()) {
                                    BreadcrumbsBar(
                                        noteTitle = activeNote?.title ?: "Note",
                                        breadcrumbs = breadcrumbs,
                                        onNavigateToRoot = { viewModel.unfocus() },
                                        onNavigateToItem = { id -> viewModel.focusOnItem(id) }
                                    )
                                }

                                if (flattenedTree.isEmpty()) {
                                    EmptyStateView(
                                        icon = Icons.Default.Checklist,
                                        title = "Empty Note",
                                        subtitle = "Tap '+ Add Item' below or start typing your first outline item.",
                                        actionLabel = "+ Add First Item",
                                        onActionClick = { viewModel.addSiblingItem(null) }
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        items(
                                            items = flattenedTree,
                                            key = { it.item.id }
                                        ) { node ->
                                            OutlinerItemRow(
                                                node = node,
                                                isSelected = selectedItemIds.contains(node.item.id),
                                                isMultiSelectMode = isMultiSelectMode,
                                                isCurrentEditing = activeEditingItemId == node.item.id,
                                                audioController = viewModel.audioController,
                                                onTextChange = { newText ->
                                                    viewModel.updateItemText(node.item.id, newText)
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
                                                onToggleCheckbox = {
                                                    viewModel.toggleItemCheckbox(node.item.id)
                                                },
                                                onToggleCollapsed = {
                                                    viewModel.toggleItemCollapsed(node.item.id)
                                                },
                                                onToggleSelection = {
                                                    viewModel.toggleItemSelection(node.item.id)
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
                                                }
                                            )
                                        }

                                        item {
                                            Spacer(modifier = Modifier.height(80.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is AppView.AllNotes, is AppView.FolderView, is AppView.Favorites, is AppView.Recent -> {
                        val displayedNotes = when (currentView) {
                            is AppView.Favorites -> notes.filter { it.isFavorite }
                            is AppView.Recent -> notes.sortedByDescending { it.lastAccessedAt }
                            is AppView.FolderView -> {
                                val fId = (currentView as AppView.FolderView).folderId
                                notes.filter { it.folderId == fId }
                            }
                            else -> notes
                        }

                        if (displayedNotes.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.Checklist,
                                title = "No notes found",
                                subtitle = "Create your first note to get started.",
                                actionLabel = "New Note",
                                onActionClick = {
                                    val fId = if (currentView is AppView.FolderView) (currentView as AppView.FolderView).folderId else null
                                    viewModel.createNote(folderId = fId)
                                }
                            )
                        } else {
                            NotesListScreen(
                                notes = displayedNotes,
                                folders = folders,
                                onOpenNote = { viewModel.openNote(it.id) },
                                onToggleFavorite = { note -> viewModel.toggleNoteFavorite(note.id, !note.isFavorite) },
                                onDeleteNote = { note -> viewModel.deleteNote(note.id) },
                                onDuplicateNote = { note -> viewModel.duplicateNote(note.id) }
                            )
                        }
                    }
                    is AppView.Trash -> {
                        TrashScreen(viewModel = viewModel)
                    }
                    is AppView.Settings -> {
                        SettingsScreen(viewModel = viewModel)
                    }
                    is AppView.TagFilter -> {
                        val tagName = (currentView as AppView.TagFilter).tagName
                        // Search for notes with this tag
                        EmptyStateView(
                            icon = Icons.Default.Checklist,
                            title = "Tag: #$tagName",
                            subtitle = "Use the global search to jump directly to all items marked #$tagName",
                            actionLabel = "Search #$tagName",
                            onActionClick = {
                                viewModel.onSearchQueryChanged("#$tagName")
                                showSearchDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // ================= MODALS & DIALOGS ================= //

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

    if (showRenameNoteDialog && activeNote != null) {
        SimpleInputDialog(
            title = "Rename Note",
            initialValue = activeNote!!.title,
            label = "Note Title",
            confirmLabel = "Rename",
            onConfirm = { newTitle ->
                viewModel.renameActiveNote(newTitle)
                showRenameNoteDialog = false
            },
            onDismiss = { showRenameNoteDialog = false }
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

    if (showMoveNoteDialog && activeNote != null) {
        MoveNoteDialog(
            folders = folders,
            currentFolderId = activeNote!!.folderId,
            onSelectFolder = { folderId ->
                viewModel.moveNoteToFolder(activeNote!!.id, folderId)
                showMoveNoteDialog = false
            },
            onDismiss = { showMoveNoteDialog = false }
        )
    }

    if (showExportDialog && activeNote != null) {
        ExportNoteDialog(
            noteTitle = activeNote!!.title,
            onExportPlainText = {
                val text = viewModel.getPlainTextExport()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Note Plain Text", text))
                Toast.makeText(context, "Plain text copied to clipboard", Toast.LENGTH_SHORT).show()
                showExportDialog = false
            },
            onExportHtml = {
                val html = viewModel.getHtmlExport()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Note HTML", html))
                Toast.makeText(context, "HTML exported & copied to clipboard", Toast.LENGTH_SHORT).show()
                showExportDialog = false
            },
            onExportOpml = {
                val opml = viewModel.getOpmlExport()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Note OPML", opml))
                Toast.makeText(context, "OPML XML copied to clipboard", Toast.LENGTH_SHORT).show()
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
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
        title = { Text("Move Note to Folder", fontWeight = FontWeight.Bold) },
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
    onExportPlainText: () -> Unit,
    onExportHtml: () -> Unit,
    onExportOpml: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export \"$noteTitle\"", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select an export format to copy to clipboard or use in other outliner apps:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(onClick = onExportPlainText, modifier = Modifier.fillMaxWidth()) {
                    Text("Plain Text (Indented Outline)")
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(onClick = onExportHtml, modifier = Modifier.fillMaxWidth()) {
                    Text("Formatted HTML (Web View)")
                }
                Spacer(modifier = Modifier.height(8.dp))

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
