package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.OutlineItem

@Composable
fun OutlinerToolbar(
    activeItem: OutlineItem?,
    isMultiSelect: Boolean,
    selectedCount: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onAddSibling: () -> Unit,
    onAddChild: () -> Unit,
    onIndent: () -> Unit,
    onUnindent: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleCheckbox: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onToggleCode: () -> Unit,
    onSetHeading: (Int) -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenLinkDialog: () -> Unit,
    onFocusItem: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Multi-select indicator
                if (isMultiSelect) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "$selectedCount selected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Add Item Buttons
                ToolbarIconButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Add Sibling Item",
                    onClick = onAddSibling
                )

                if (activeItem != null && !isMultiSelect) {
                    ToolbarIconButton(
                        icon = Icons.Default.SubdirectoryArrowRight,
                        contentDescription = "Add Sub-item",
                        onClick = onAddChild
                    )
                }

                ToolbarDivider()

                // Indent / Unindent
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                    contentDescription = "Indent (Tab)",
                    onClick = onIndent
                )
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                    contentDescription = "Unindent (Shift+Tab)",
                    onClick = onUnindent
                )

                // Move Up / Down
                ToolbarIconButton(
                    icon = Icons.Default.ArrowUpward,
                    contentDescription = "Move Up",
                    onClick = onMoveUp
                )
                ToolbarIconButton(
                    icon = Icons.Default.ArrowDownward,
                    contentDescription = "Move Down",
                    onClick = onMoveDown
                )

                ToolbarDivider()

                // Checkbox
                ToolbarIconButton(
                    icon = Icons.Default.CheckBox,
                    contentDescription = "Toggle Checkbox",
                    isActive = activeItem?.hasCheckbox == true,
                    onClick = onToggleCheckbox
                )

                // Headings
                HeadingButton(label = "H1", isActive = activeItem?.headingLevel == 1) { onSetHeading(1) }
                HeadingButton(label = "H2", isActive = activeItem?.headingLevel == 2) { onSetHeading(2) }
                HeadingButton(label = "H3", isActive = activeItem?.headingLevel == 3) { onSetHeading(3) }

                ToolbarDivider()

                // Formatting: Bold, Italic, Strikethrough, Code
                ToolbarIconButton(
                    icon = Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    isActive = activeItem?.isBold == true,
                    onClick = onToggleBold
                )
                ToolbarIconButton(
                    icon = Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    isActive = activeItem?.isItalic == true,
                    onClick = onToggleItalic
                )
                ToolbarIconButton(
                    icon = Icons.Default.FormatStrikethrough,
                    contentDescription = "Strikethrough",
                    isActive = activeItem?.isStrikethrough == true,
                    onClick = onToggleStrikethrough
                )
                ToolbarIconButton(
                    icon = Icons.Default.Code,
                    contentDescription = "Inline Code",
                    isActive = activeItem?.isCode == true,
                    onClick = onToggleCode
                )

                ToolbarDivider()

                // Color picker
                ToolbarIconButton(
                    icon = Icons.Default.FormatColorFill,
                    contentDescription = "Text & Highlight Color",
                    isActive = activeItem?.textColor != null || activeItem?.backgroundColor != null,
                    onClick = onOpenColorPicker
                )

                // Link / Media
                ToolbarIconButton(
                    icon = Icons.Default.Link,
                    contentDescription = "Add Link / Media",
                    isActive = !activeItem?.url.isNullOrEmpty(),
                    onClick = onOpenLinkDialog
                )

                // Focus Zoom
                if (activeItem != null && !isMultiSelect) {
                    ToolbarIconButton(
                        icon = Icons.Default.ZoomIn,
                        contentDescription = "Zoom / Focus into Subtree",
                        onClick = onFocusItem
                    )
                }

                // Duplicate & Delete
                ToolbarIconButton(
                    icon = Icons.Default.ContentCopy,
                    contentDescription = "Duplicate Subtree",
                    onClick = onDuplicate
                )
                ToolbarIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete Subtree",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete
                )

                ToolbarDivider()

                // Undo / Redo
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    enabled = canUndo,
                    onClick = onUndo
                )
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    enabled = canRedo,
                    onClick = onRedo
                )
            }
        }
    }
}

@Composable
private fun Column(modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        content()
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val activeBg = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val activeTint = if (isActive) MaterialTheme.colorScheme.primary else if (enabled) tint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(activeBg),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = activeTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HeadingButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val textColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(38.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(width = 1.dp, height = 22.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
