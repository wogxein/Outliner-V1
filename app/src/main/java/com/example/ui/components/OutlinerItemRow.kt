package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TreeItemNode
import com.example.media.AudioPlayerController

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutlinerItemRow(
    node: TreeItemNode,
    isCurrentEditing: Boolean,
    audioController: AudioPlayerController,
    onTextChange: (String) -> Unit,
    onSplitItem: (String, String) -> Unit,
    onDeleteEmptyItem: () -> Unit,
    onAddSiblingAfter: () -> Unit,
    onAddChild: () -> Unit,
    onIndent: () -> Unit,
    onUnindent: () -> Unit,
    onToggleCheckbox: () -> Unit,
    onToggleCollapsed: () -> Unit,
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

    // Maintain local TextFieldValue to avoid Compose cursor reset and typing inversion bug
    var textFieldValue by remember(item.id) {
        mutableStateOf(TextFieldValue(text = item.text, selection = TextRange(item.text.length)))
    }

    LaunchedEffect(item.text) {
        if (item.text != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = item.text,
                selection = TextRange(item.text.length)
            )
        }
    }

    LaunchedEffect(isCurrentEditing) {
        if (isCurrentEditing) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus fallback
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onFocusGain() },
                onLongClick = { showContextMenu = true }
            )
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
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
                    contentDescription = if (node.isCollapsed) "Expand" else "Collapse",
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

        // Main Item Content: Editable Text Field, URLs, Media Embeds
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
                    value = textFieldValue,
                    onValueChange = { newTfv ->
                        if (newTfv.text.contains('\n')) {
                            val newlineIdx = newTfv.text.indexOf('\n')
                            val before = newTfv.text.substring(0, newlineIdx)
                            val after = newTfv.text.substring(newlineIdx + 1)
                            textFieldValue = TextFieldValue(text = before, selection = TextRange(before.length))
                            onSplitItem(before, after)
                        } else if (newTfv.text.isEmpty() && textFieldValue.text.isEmpty()) {
                            // Empty line backspace
                            onDeleteEmptyItem()
                        } else {
                            textFieldValue = newTfv
                            onTextChange(newTfv.text)
                        }
                    },
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
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
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                val currentText = textFieldValue.text
                                val cursor = textFieldValue.selection.start
                                val before = if (cursor in 0..currentText.length) currentText.substring(0, cursor) else currentText
                                val after = if (cursor in 0..currentText.length) currentText.substring(cursor) else ""
                                textFieldValue = TextFieldValue(text = before, selection = TextRange(before.length))
                                onSplitItem(before, after)
                                true
                            } else if ((event.key == Key.Backspace || event.key == Key.Delete) && event.type == KeyEventType.KeyDown) {
                                if (textFieldValue.text.isEmpty()) {
                                    onDeleteEmptyItem()
                                    true
                                } else {
                                    false
                                }
                            } else if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
                                onIndent()
                                true
                            } else {
                                false
                            }
                        },
                    decorationBox = { innerTextField ->
                        if (textFieldValue.text.isEmpty()) {
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

        // Expand / Collapse (+ / -) button on the right for outline lines with children
        if (node.hasChildren) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (node.isCollapsed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onToggleCollapsed() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (node.isCollapsed) Icons.Default.Add else Icons.Default.Remove,
                        contentDescription = if (node.isCollapsed) "Expand items" else "Unexpand items",
                        tint = if (node.isCollapsed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
