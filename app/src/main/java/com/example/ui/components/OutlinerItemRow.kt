package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.OutlineItem
import com.example.domain.model.TreeItemNode
import com.example.media.AudioPlayerController

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutlinerItemRow(
    node: TreeItemNode,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    isCurrentEditing: Boolean,
    audioController: AudioPlayerController,
    onTextChange: (String) -> Unit,
    onAddSiblingAfter: () -> Unit,
    onAddChild: () -> Unit,
    onIndent: () -> Unit,
    onUnindent: () -> Unit,
    onToggleCheckbox: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onToggleSelection: () -> Unit,
    onFocusGain: () -> Unit,
    onZoomFocus: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenLinkDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = node.item
    var showContextMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isCurrentEditing) {
        if (isCurrentEditing) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus request fallback
            }
        }
    }

    // Text styling based on properties
    val textStyle = when (item.headingLevel) {
        1 -> TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = parseColor(item.textColor, MaterialTheme.colorScheme.onSurface)
        )
        2 -> TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = parseColor(item.textColor, MaterialTheme.colorScheme.onSurface)
        )
        3 -> TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = parseColor(item.textColor, MaterialTheme.colorScheme.onSurface)
        )
        else -> TextStyle(
            fontSize = 14.sp,
            fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (item.isItalic) FontStyle.Italic else FontStyle.Normal,
            fontFamily = if (item.isCode) FontFamily.Monospace else FontFamily.Default,
            textDecoration = if (item.isStrikethrough || (item.hasCheckbox && item.isChecked)) TextDecoration.LineThrough else TextDecoration.None,
            color = parseColor(
                item.textColor,
                if (item.hasCheckbox && item.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface
            )
        )
    }

    val backgroundColor = parseColor(item.backgroundColor, Color.Transparent)
    val rowBackground = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onClick = {
                    if (isMultiSelectMode) {
                        onToggleSelection()
                    } else {
                        onFocusGain()
                    }
                },
                onLongClick = {
                    showContextMenu = true
                }
            )
            .padding(vertical = 3.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Multi-select Checkbox if in multi-select mode
        if (isMultiSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Tree Indentation spacer with optional subtle branch line
        val indentWidth = (node.level * 20).dp
        if (node.level > 0) {
            Box(
                modifier = Modifier
                    .width(indentWidth)
                    .height(24.dp)
            ) {
                // Subtle vertical guide line
                Box(
                    modifier = Modifier
                        .padding(start = (node.level * 20 - 10).dp)
                        .width(1.5.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                )
            }
        }

        // Bullet / Expand Collapse Icon
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = {
                        if (node.hasChildren) onToggleCollapsed() else onZoomFocus()
                    },
                    onLongClick = {
                        showContextMenu = true
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (node.hasChildren) {
                Icon(
                    imageVector = if (node.isCollapsed) Icons.Default.ChevronRight else Icons.Default.ExpandMore,
                    contentDescription = if (node.isCollapsed) "Expand (${node.directChildrenCount})" else "Collapse",
                    tint = if (node.isCollapsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                // Leaf item dot bullet
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Outline item Checkbox (if enabled for this item)
        if (item.hasCheckbox) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggleCheckbox() },
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // Main Item Content: Editable Text Field, Tags, URLs, Media Embeds
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(backgroundColor)
                    .padding(horizontal = if (backgroundColor != Color.Transparent) 6.dp else 0.dp, vertical = 2.dp)
            ) {
                BasicTextField(
                    value = item.text,
                    onValueChange = onTextChange,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions.Default,
                    keyboardActions = KeyboardActions(
                        onDone = { onAddSiblingAfter() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onFocusGain()
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.Enter) {
                                onAddSiblingAfter()
                                true
                            } else if (event.key == Key.Tab) {
                                onIndent()
                                true
                            } else {
                                false
                            }
                        },
                    decorationBox = { innerTextField ->
                        if (item.text.isEmpty()) {
                            Text(
                                text = "Empty item",
                                style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Media & URL embed preview card
            if (!item.url.isNullOrBlank() || !item.mediaUri.isNullOrBlank()) {
                MediaEmbedCard(
                    url = item.url,
                    mediaType = item.mediaType,
                    mediaUri = item.mediaUri,
                    mediaTitle = item.mediaTitle,
                    audioController = audioController
                )
            }
        }

        // Collapsed indicator badge (e.g. +3 items)
        if (node.hasChildren && node.isCollapsed) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleCollapsed() }
            ) {
                Text(
                    text = "${node.totalDescendantsCount}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Context Menu Dropdown
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Zoom / Focus into this branch") },
                onClick = {
                    showContextMenu = false
                    onZoomFocus()
                }
            )
            DropdownMenuItem(
                text = { Text("Add Sub-item") },
                onClick = {
                    showContextMenu = false
                    onAddChild()
                }
            )
            DropdownMenuItem(
                text = { Text(if (item.hasCheckbox) "Remove Checkbox" else "Add Checkbox") },
                onClick = {
                    showContextMenu = false
                    onToggleCheckbox()
                }
            )
            DropdownMenuItem(
                text = { Text("Indent") },
                onClick = {
                    showContextMenu = false
                    onIndent()
                }
            )
            DropdownMenuItem(
                text = { Text("Unindent") },
                onClick = {
                    showContextMenu = false
                    onUnindent()
                }
            )
            DropdownMenuItem(
                text = { Text("Colors & Highlighting") },
                onClick = {
                    showContextMenu = false
                    onOpenColorPicker()
                }
            )
            DropdownMenuItem(
                text = { Text("Add / Edit Link") },
                onClick = {
                    showContextMenu = false
                    onOpenLinkDialog()
                }
            )
            DropdownMenuItem(
                text = { Text("Duplicate Subtree") },
                onClick = {
                    showContextMenu = false
                    onDuplicate()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete Subtree", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showContextMenu = false
                    onDelete()
                }
            )
        }
    }
}

private fun parseColor(hex: String?, defaultColor: Color): Color {
    if (hex.isNullOrBlank()) return defaultColor
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}
