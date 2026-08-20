package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TreeItemNode
import com.example.media.AudioPlayerController
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OutlinerItemRow(
    node: TreeItemNode,
    isCurrentEditing: Boolean,
    isFirstItem: Boolean = false,
    audioController: AudioPlayerController,
    onTextChange: (String) -> Unit,
    onSplitItem: (String, String) -> Unit,
    onDeleteEmptyItem: () -> Unit,
    onAddSiblingAfter: () -> Unit,
    onAddChild: () -> Unit,
    onIndent: () -> Unit,
    onUnindent: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onToggleCheckbox: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onFocusGain: () -> Unit,
    onZoomFocus: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenLinkDialog: () -> Unit,
    onSetHeading: (Int) -> Unit = {},
    onNavigateBackToTitle: () -> Unit = {},
    onLookupWord: (String) -> Unit = {},
    verticalPadding: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    val item = node.item
    var showContextMenu by remember { mutableStateOf(false) }
    var isRowFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Maintain local TextFieldValue state with stable cursor position
    var textFieldValue by remember(item.id) {
        mutableStateOf(TextFieldValue(text = item.text, selection = TextRange(item.text.length)))
    }

    // Only update local TextFieldValue if text was changed externally (e.g. undo/redo) and doesn't match current local text
    LaunchedEffect(item.text) {
        if (item.text != textFieldValue.text) {
            val oldSelection = textFieldValue.selection
            val newStart = oldSelection.start.coerceIn(0, item.text.length)
            val newEnd = oldSelection.end.coerceIn(0, item.text.length)
            textFieldValue = textFieldValue.copy(
                text = item.text,
                selection = TextRange(newStart, newEnd)
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

    // Dynamic Markdown & code styling
    val activeFontFamily = if (item.isCode) FontFamily.Monospace else MaterialTheme.typography.bodyLarge.fontFamily
    val baseBody = MaterialTheme.typography.bodyLarge

    val textStyle = when (item.headingLevel) {
        1 -> MaterialTheme.typography.titleLarge.copy(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Bold,
            color = parseColor(item.textColor, MaterialTheme.colorScheme.primary)
        )
        2 -> MaterialTheme.typography.titleMedium.copy(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.SemiBold,
            color = parseColor(item.textColor, MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
        )
        3 -> MaterialTheme.typography.titleSmall.copy(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Medium,
            color = parseColor(item.textColor, MaterialTheme.colorScheme.onSurface)
        )
        else -> baseBody.copy(
            fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (item.isItalic) FontStyle.Italic else FontStyle.Normal,
            fontFamily = activeFontFamily,
            textDecoration = if (item.isStrikethrough || (item.hasCheckbox && item.isChecked)) TextDecoration.LineThrough else TextDecoration.None,
            color = parseColor(
                item.textColor,
                if (item.hasCheckbox && item.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface
            )
        )
    }

    val backgroundColor = parseColor(item.backgroundColor, Color.Transparent)
    val visualTransformation = remember(isRowFocused) {
        MarkdownVisualTransformation(isFocused = isRowFocused)
    }

    // Helper for auto-numbering & markdown prefix detection
    fun handleEnterOrSplit(beforeRaw: String, afterRaw: String) {
        val numberRegex = Regex("^(\\d+)\\.\\s*(.*)$")
        val match = numberRegex.find(beforeRaw)
        if (match != null) {
            val num = match.groupValues[1].toIntOrNull() ?: 1
            val nextPrefix = "${num + 1}. "
            val newAfter = if (afterRaw.startsWith(nextPrefix)) afterRaw else nextPrefix + afterRaw
            onSplitItem(beforeRaw, newAfter)
        } else {
            onSplitItem(beforeRaw, afterRaw)
        }
    }

    // Helper to process markdown shortcuts on typing (#, ##, ###, -, [ ])
    fun processMarkdownInput(input: TextFieldValue) {
        val raw = input.text

        // Heading 3: ### 
        if (raw.startsWith("### ")) {
            val rest = raw.removePrefix("### ")
            onSetHeading(3)
            textFieldValue = TextFieldValue(text = rest, selection = TextRange(rest.length))
            onTextChange(rest)
            return
        }
        // Heading 2: ## 
        if (raw.startsWith("## ")) {
            val rest = raw.removePrefix("## ")
            onSetHeading(2)
            textFieldValue = TextFieldValue(text = rest, selection = TextRange(rest.length))
            onTextChange(rest)
            return
        }
        // Heading 1: # 
        if (raw.startsWith("# ")) {
            val rest = raw.removePrefix("# ")
            onSetHeading(1)
            textFieldValue = TextFieldValue(text = rest, selection = TextRange(rest.length))
            onTextChange(rest)
            return
        }

        // Bullet list shortcut: "- " or "* "
        if (raw.startsWith("- ") || raw.startsWith("* ")) {
            val rest = raw.substring(2)
            textFieldValue = TextFieldValue(text = rest, selection = TextRange(rest.length))
            onTextChange(rest)
            onIndent()
            return
        }

        // Checkbox shortcut: "[ ] " or "[x] "
        if (raw.startsWith("[ ] ") || raw.startsWith("[x] ")) {
            val rest = raw.substring(4)
            textFieldValue = TextFieldValue(text = rest, selection = TextRange(rest.length))
            if (!item.hasCheckbox) onToggleCheckbox()
            onTextChange(rest)
            return
        }

        textFieldValue = input
        onTextChange(input.text)
    }

    // SwipeToDismissBox for Right-to-Left deletion with red reveal & trash icon
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val bgColor by animateColorAsState(
                targetValue = if (isSwiping) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                label = "swipeBg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Line",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = { onFocusGain() },
                    onLongClick = { showContextMenu = true }
                )
                .padding(vertical = verticalPadding, horizontal = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Tree Indentation spacer with vertical branch guideline
            val indentWidth = (node.level * 18).dp
            if (node.level > 0) {
                Box(
                    modifier = Modifier
                        .width(indentWidth)
                        .height(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = (node.level * 18 - 9).dp)
                            .width(1.5.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    )
                }
            }

            // Bullet / Expand Collapse Icon / Drag & Drop Target
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
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Checkbox if present
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
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Text & Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                Surface(
                    color = backgroundColor,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
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
                                handleEnterOrSplit(before, after)
                            } else if (newTfv.text.isEmpty() && textFieldValue.text.isEmpty()) {
                                if (isFirstItem) {
                                    onNavigateBackToTitle()
                                } else {
                                    onDeleteEmptyItem()
                                }
                            } else {
                                processMarkdownInput(newTfv)
                            }
                        },
                        textStyle = textStyle,
                        visualTransformation = visualTransformation,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        keyboardActions = KeyboardActions(
                            onDone = { onAddSiblingAfter() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                isRowFocused = focusState.isFocused
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
                                    handleEnterOrSplit(before, after)
                                    true
                                } else if ((event.key == Key.Backspace || event.key == Key.Delete) && event.type == KeyEventType.KeyDown) {
                                    val text = textFieldValue.text
                                    val cursor = textFieldValue.selection.start
                                    val numberOnlyRegex = Regex("^(\\d+)\\.\\s*$")

                                    if (cursor == 0 && isFirstItem) {
                                        onNavigateBackToTitle()
                                        true
                                    } else if (numberOnlyRegex.matches(text)) {
                                        textFieldValue = TextFieldValue(text = "", selection = TextRange(0))
                                        onTextChange("")
                                        true
                                    } else if (text.isEmpty()) {
                                        if (isFirstItem) {
                                            onNavigateBackToTitle()
                                        } else {
                                            onDeleteEmptyItem()
                                        }
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
                                    text = if (isFirstItem) "Start typing outline..." else "Empty item",
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

            // Context Menu Dropdown on Long Click
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                Text(
                    text = "LINE ACTIONS & REORDER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )

                // Move Up
                DropdownMenuItem(
                    text = { Text("Move Up ⬆") },
                    leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showContextMenu = false
                        onMoveUp()
                    }
                )

                // Move Down
                DropdownMenuItem(
                    text = { Text("Move Down ⬇") },
                    leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showContextMenu = false
                        onMoveDown()
                    }
                )

                // Indent / Move Inside
                DropdownMenuItem(
                    text = { Text("Indent (Move Inside) ➡") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onIndent()
                    }
                )

                // Unindent / Move Out
                DropdownMenuItem(
                    text = { Text("Unindent (Move Out) ⬅") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onUnindent()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Find Meaning action
                val selectedWord = if (textFieldValue.selection.collapsed) {
                    item.text.trim()
                } else {
                    try {
                        textFieldValue.text.substring(textFieldValue.selection.min, textFieldValue.selection.max).trim()
                    } catch (e: Exception) {
                        item.text.trim()
                    }
                }
                if (selectedWord.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("Find Meaning") },
                        leadingIcon = { Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showContextMenu = false
                            onLookupWord(selectedWord)
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Zoom / Focus branch") },
                    leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onZoomFocus()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Add Sub-item") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
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
                    text = { Text("Colors & Highlighting") },
                    leadingIcon = { Icon(Icons.Default.FormatColorFill, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onOpenColorPicker()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Add / Edit Link") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onOpenLinkDialog()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Duplicate Subtree") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onDuplicate()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Delete Line", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showContextMenu = false
                        onDelete()
                    }
                )
            }
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
