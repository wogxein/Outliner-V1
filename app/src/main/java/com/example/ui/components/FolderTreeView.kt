package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Folder

@Composable
fun FolderTreeView(
    folders: List<Folder>,
    selectedFolderId: String?,
    onSelectFolder: (String?) -> Unit,
    onCreateSubfolder: (parentId: String) -> Unit,
    onCreateNoteInFolder: (folderId: String) -> Unit,
    onRenameFolder: (Folder) -> Unit,
    onToggleExpand: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rootFolders = folders.filter { it.parentId == null }.sortedBy { it.sortOrder }
    val childrenMap = folders.groupBy { it.parentId }

    Column(modifier = modifier.fillMaxWidth()) {
        // "All Notes" root option
        FolderItemRow(
            name = "All Notes",
            icon = Icons.Default.FolderOpen,
            level = 0,
            isSelected = selectedFolderId == null,
            noteCount = folders.sumOf { it.noteCount },
            hasChildren = false,
            isExpanded = true,
            onToggleExpand = {},
            onClick = { onSelectFolder(null) },
            actionsContent = null
        )

        rootFolders.forEach { folder ->
            RenderFolderNode(
                folder = folder,
                level = 1,
                childrenMap = childrenMap,
                selectedFolderId = selectedFolderId,
                onSelectFolder = onSelectFolder,
                onCreateSubfolder = onCreateSubfolder,
                onCreateNoteInFolder = onCreateNoteInFolder,
                onRenameFolder = onRenameFolder,
                onToggleExpand = onToggleExpand,
                onDeleteFolder = onDeleteFolder
            )
        }
    }
}

@Composable
private fun RenderFolderNode(
    folder: Folder,
    level: Int,
    childrenMap: Map<String?, List<Folder>>,
    selectedFolderId: String?,
    onSelectFolder: (String?) -> Unit,
    onCreateSubfolder: (parentId: String) -> Unit,
    onCreateNoteInFolder: (folderId: String) -> Unit,
    onRenameFolder: (Folder) -> Unit,
    onToggleExpand: (String) -> Unit,
    onDeleteFolder: (String) -> Unit
) {
    val children = childrenMap[folder.id] ?: emptyList()
    val hasChildren = children.isNotEmpty()

    FolderItemRow(
        name = folder.name,
        icon = if (folder.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
        level = level,
        isSelected = selectedFolderId == folder.id,
        noteCount = folder.noteCount,
        hasChildren = hasChildren,
        isExpanded = folder.isExpanded,
        onToggleExpand = { onToggleExpand(folder.id) },
        onClick = { onSelectFolder(folder.id) },
        actionsContent = {
            FolderActionsMenu(
                folder = folder,
                onCreateSubfolder = { onCreateSubfolder(folder.id) },
                onCreateNote = { onCreateNoteInFolder(folder.id) },
                onRename = { onRenameFolder(folder) },
                onDelete = { onDeleteFolder(folder.id) }
            )
        }
    )

    if (hasChildren && folder.isExpanded) {
        children.sortedBy { it.sortOrder }.forEach { child ->
            RenderFolderNode(
                folder = child,
                level = level + 1,
                childrenMap = childrenMap,
                selectedFolderId = selectedFolderId,
                onSelectFolder = onSelectFolder,
                onCreateSubfolder = onCreateSubfolder,
                onCreateNoteInFolder = onCreateNoteInFolder,
                onRenameFolder = onRenameFolder,
                onToggleExpand = onToggleExpand,
                onDeleteFolder = onDeleteFolder
            )
        }
    }
}

@Composable
private fun FolderItemRow(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    level: Int,
    isSelected: Boolean,
    noteCount: Int,
    hasChildren: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    actionsContent: (@Composable () -> Unit)?
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indentation spacer
        if (level > 0) {
            Spacer(modifier = Modifier.width((level * 14).dp))
        }

        // Expand / collapse arrow
        if (hasChildren) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { onToggleExpand() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = "Expand folder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFD97706),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (noteCount > 0) {
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "$noteCount",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        actionsContent?.invoke()
    }
}

@Composable
private fun FolderActionsMenu(
    folder: Folder,
    onCreateSubfolder: () -> Unit,
    onCreateNote: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Folder options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("New Note Inside") },
                leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCreateNote()
                }
            )
            DropdownMenuItem(
                text = { Text("New Subfolder") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCreateSubfolder()
                }
            )
            DropdownMenuItem(
                text = { Text("Rename Folder") },
                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}
