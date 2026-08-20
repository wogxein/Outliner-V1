package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Note
import com.example.domain.model.TreeItemNode
import com.example.util.PdfExporter
import kotlin.math.roundToInt

enum class MindMapDirection(val label: String) {
    LEFT_TO_RIGHT("Left to Right"),
    TOP_TO_BOTTOM("Top to Bottom")
}

data class MindMapNodeLayout(
    val id: String,
    val text: String,
    val level: Int,
    val parentId: String?,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val isRoot: Boolean = false,
    val colorHex: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapView(
    note: Note,
    allNodes: List<TreeItemNode>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var direction by remember { mutableStateOf(MindMapDirection.LEFT_TO_RIGHT) }
    var useNoteTitleAsRoot by remember { mutableStateOf(true) }
    var showNodeSelectDialog by remember { mutableStateOf(false) }

    // Memory-only selection set for this transform view
    val excludedNodeIds = remember { mutableStateMapOf<String, Boolean>() }

    // Filtered tree
    val activeNodes = remember(allNodes, excludedNodeIds.toMap()) {
        allNodes.filter { node ->
            node.item.text.isNotBlank() && excludedNodeIds[node.item.id] != true
        }
    }

    // Pan & Zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Calculate Mind Map Tree layout based on selected direction
    val layoutNodes = remember(note.title, activeNodes, useNoteTitleAsRoot, direction) {
        if (direction == MindMapDirection.LEFT_TO_RIGHT) {
            calculateMindMapLayoutHorizontal(
                noteTitle = note.title.ifBlank { "Untitled Note" },
                nodes = activeNodes,
                useTitleAsRoot = useNoteTitleAsRoot
            )
        } else {
            calculateMindMapLayoutVertical(
                noteTitle = note.title.ifBlank { "Untitled Note" },
                nodes = activeNodes,
                useTitleAsRoot = useNoteTitleAsRoot
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Mind Map: ${note.title.ifBlank { "Untitled" }}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${layoutNodes.size} nodes mapped • ${direction.label}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Mind Map")
                }
            },
            actions = {
                // Filter / Select Nodes
                IconButton(onClick = { showNodeSelectDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter Nodes")
                }
                // Export to PDF
                IconButton(onClick = {
                    val rootLabel = if (useNoteTitleAsRoot) note.title.ifBlank { "Note" } else "Overview"
                    val pdf = PdfExporter.exportMindMapToPdf(
                        context = context,
                        noteTitle = note.title.ifBlank { "Untitled" },
                        rootTitle = rootLabel,
                        nodes = activeNodes
                    )
                    if (pdf != null) {
                        PdfExporter.sharePdf(context, pdf, "Mind Map - ${note.title}")
                    }
                }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Sub-toolbar with Direction Toggle, Title as Root toggle and Zoom controls
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Direction Toggle (Left to Right / Top to Bottom)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = direction == MindMapDirection.LEFT_TO_RIGHT,
                        onClick = { direction = MindMapDirection.LEFT_TO_RIGHT },
                        label = { Text("L → R", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    FilterChip(
                        selected = direction == MindMapDirection.TOP_TO_BOTTOM,
                        onClick = { direction = MindMapDirection.TOP_TO_BOTTOM },
                        label = { Text("T → B", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Zoom controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { scale = (scale - 0.15f).coerceAtLeast(0.35f) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${(scale * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { scale = (scale + 0.15f).coerceAtMost(2.5f) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Interactive Canvas Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(0.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.3f, 3.0f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

            // Canvas for Connector Lines (Drawn BEHIND nodes so text is never covered)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            ) {
                val nodeMap = layoutNodes.associateBy { it.id }

                for (node in layoutNodes) {
                    val parent = node.parentId?.let { nodeMap[it] }
                    if (parent != null) {
                        val path = Path()

                        if (direction == MindMapDirection.LEFT_TO_RIGHT) {
                            val startX = parent.x + parent.width
                            val startY = parent.y + (parent.height / 2f)
                            val endX = node.x
                            val endY = node.y + (node.height / 2f)

                            val ctrlX1 = startX + (endX - startX) * 0.5f
                            val ctrlX2 = startX + (endX - startX) * 0.5f

                            path.moveTo(startX, startY)
                            path.cubicTo(ctrlX1, startY, ctrlX2, endY, endX, endY)
                        } else {
                            val startX = parent.x + (parent.width / 2f)
                            val startY = parent.y + parent.height
                            val endX = node.x + (node.width / 2f)
                            val endY = node.y

                            val ctrlY1 = startY + (endY - startY) * 0.5f
                            val ctrlY2 = startY + (endY - startY) * 0.5f

                            path.moveTo(startX, startY)
                            path.cubicTo(startX, ctrlY1, endX, ctrlY2, endX, endY)
                        }

                        drawPath(
                            path = path,
                            color = if (parent.isRoot) primaryColor.copy(alpha = 0.6f) else lineColor,
                            style = Stroke(width = if (parent.isRoot) 2.0f else 1.5f, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Render Node UI elements (Opaque surface cards in front of connector lines)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            ) {
                layoutNodes.forEach { node ->
                    MindMapNodeCard(node = node)
                }
            }
        }
    }

    // Node Selector / Filter Dialog
    if (showNodeSelectDialog) {
        AlertDialog(
            onDismissRequest = { showNodeSelectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Nodes to Map", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Toggle outline items to include in the mind map (saved in memory, does not alter outline note):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { excludedNodeIds.clear() }) {
                            Text("Select All", fontSize = 12.sp)
                        }
                        TextButton(onClick = {
                            allNodes.forEach { excludedNodeIds[it.item.id] = true }
                        }) {
                            Text("Clear All", fontSize = 12.sp)
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        items(allNodes) { node ->
                            if (node.item.text.isNotBlank()) {
                                val isExcluded = excludedNodeIds[node.item.id] == true
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isExcluded) excludedNodeIds.remove(node.item.id)
                                            else excludedNodeIds[node.item.id] = true
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(modifier = Modifier.width((node.level * 14).dp))
                                    Checkbox(
                                        checked = !isExcluded,
                                        onCheckedChange = { checked ->
                                            if (checked) excludedNodeIds.remove(node.item.id)
                                            else excludedNodeIds[node.item.id] = true
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Text(
                                        text = node.item.text,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNodeSelectDialog = false }) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun MindMapNodeCard(node: MindMapNodeLayout) {
    val cardColor = if (node.isRoot) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (node.isRoot) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (node.isRoot) {
        MaterialTheme.colorScheme.primary
    } else when (node.level) {
        0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        1 -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Surface(
        color = cardColor,
        shape = RoundedCornerShape(if (node.isRoot) 12.dp else 8.dp),
        border = BorderStroke(if (node.isRoot) 1.8.dp else 1.0.dp, borderColor),
        shadowElevation = if (node.isRoot) 3.dp else 1.dp,
        modifier = Modifier
            .offset { IntOffset(node.x.roundToInt(), node.y.roundToInt()) }
            .width(node.width.dp)
            .height(node.height.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = node.text,
                fontSize = if (node.isRoot) 13.sp else if (node.level == 0) 12.sp else 11.sp,
                fontWeight = if (node.isRoot || node.level == 0) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Computes horizontal (Left to Right) tree coordinates.
 */
private fun calculateMindMapLayoutHorizontal(
    noteTitle: String,
    nodes: List<TreeItemNode>,
    useTitleAsRoot: Boolean
): List<MindMapNodeLayout> {
    val result = mutableListOf<MindMapNodeLayout>()
    if (nodes.isEmpty() && !useTitleAsRoot) return result

    val rootId = "__root__"
    val colWidth = 180f
    val nodeWidth = 150f
    val nodeHeight = 40f
    val verticalGap = 14f

    val topLevelNodes = nodes.filter { it.level == 0 }

    if (useTitleAsRoot) {
        val rootNode = MindMapNodeLayout(
            id = rootId,
            text = noteTitle,
            level = -1,
            parentId = null,
            x = 40f,
            y = 100f,
            width = 160f,
            height = 46f,
            isRoot = true
        )
        result.add(rootNode)

        var branchY = 40f
        for (topNode in topLevelNodes) {
            val children = nodes.filter { it.item.parentId == topNode.item.id }
            val bNode = MindMapNodeLayout(
                id = topNode.item.id,
                text = topNode.item.text,
                level = 0,
                parentId = rootId,
                x = 40f + colWidth,
                y = branchY,
                width = nodeWidth,
                height = nodeHeight,
                colorHex = topNode.item.textColor
            )
            result.add(bNode)

            var childY = branchY
            for (childNode in children) {
                val subChildren = nodes.filter { it.item.parentId == childNode.item.id }
                val cNode = MindMapNodeLayout(
                    id = childNode.item.id,
                    text = childNode.item.text,
                    level = 1,
                    parentId = topNode.item.id,
                    x = 40f + (colWidth * 2),
                    y = childY,
                    width = nodeWidth,
                    height = nodeHeight,
                    colorHex = childNode.item.textColor
                )
                result.add(cNode)

                var subY = childY
                for (subNode in subChildren) {
                    val sNode = MindMapNodeLayout(
                        id = subNode.item.id,
                        text = subNode.item.text,
                        level = 2,
                        parentId = childNode.item.id,
                        x = 40f + (colWidth * 3),
                        y = subY,
                        width = nodeWidth,
                        height = nodeHeight,
                        colorHex = subNode.item.textColor
                    )
                    result.add(sNode)
                    subY += nodeHeight + verticalGap
                }

                val childSpan = if (subChildren.isNotEmpty()) subChildren.size * (nodeHeight + verticalGap) else (nodeHeight + verticalGap)
                childY += childSpan
            }

            val branchHeight = if (children.isNotEmpty()) {
                children.sumOf { child ->
                    val subCount = nodes.count { it.item.parentId == child.item.id }
                    if (subCount > 0) subCount else 1
                } * (nodeHeight + verticalGap)
            } else {
                nodeHeight + verticalGap
            }
            branchY += branchHeight
        }

        // Center root Y with the middle of branches
        if (result.size > 1) {
            val avgY = (result.drop(1).map { it.y }.average()).toFloat()
            val updatedRoot = rootNode.copy(y = (avgY - 23f).coerceAtLeast(40f))
            result[0] = updatedRoot
        }
    } else {
        var startY = 40f
        for (topNode in topLevelNodes) {
            val children = nodes.filter { it.item.parentId == topNode.item.id }
            val bNode = MindMapNodeLayout(
                id = topNode.item.id,
                text = topNode.item.text,
                level = 0,
                parentId = null,
                x = 40f,
                y = startY,
                width = 160f,
                height = 44f,
                isRoot = true,
                colorHex = topNode.item.textColor
            )
            result.add(bNode)

            var childY = startY
            for (childNode in children) {
                val subChildren = nodes.filter { it.item.parentId == childNode.item.id }
                val cNode = MindMapNodeLayout(
                    id = childNode.item.id,
                    text = childNode.item.text,
                    level = 1,
                    parentId = topNode.item.id,
                    x = 40f + colWidth,
                    y = childY,
                    width = nodeWidth,
                    height = nodeHeight,
                    colorHex = childNode.item.textColor
                )
                result.add(cNode)

                var subY = childY
                for (subNode in subChildren) {
                    val sNode = MindMapNodeLayout(
                        id = subNode.item.id,
                        text = subNode.item.text,
                        level = 2,
                        parentId = childNode.item.id,
                        x = 40f + (colWidth * 2),
                        y = subY,
                        width = nodeWidth,
                        height = nodeHeight,
                        colorHex = subNode.item.textColor
                    )
                    result.add(sNode)
                    subY += nodeHeight + verticalGap
                }

                val childSpan = if (subChildren.isNotEmpty()) subChildren.size * (nodeHeight + verticalGap) else (nodeHeight + verticalGap)
                childY += childSpan
            }

            startY += (children.size.coerceAtLeast(1) * (nodeHeight + verticalGap)) + verticalGap
        }
    }

    return result
}

/**
 * Computes vertical (Top to Bottom) tree coordinates.
 */
private fun calculateMindMapLayoutVertical(
    noteTitle: String,
    nodes: List<TreeItemNode>,
    useTitleAsRoot: Boolean
): List<MindMapNodeLayout> {
    val result = mutableListOf<MindMapNodeLayout>()
    if (nodes.isEmpty() && !useTitleAsRoot) return result

    val rootId = "__root__"
    val rowHeight = 70f
    val nodeWidth = 140f
    val nodeHeight = 38f
    val horizontalGap = 16f

    val topLevelNodes = nodes.filter { it.level == 0 }

    if (useTitleAsRoot) {
        val rootNode = MindMapNodeLayout(
            id = rootId,
            text = noteTitle,
            level = -1,
            parentId = null,
            x = 100f,
            y = 40f,
            width = 160f,
            height = 46f,
            isRoot = true
        )
        result.add(rootNode)

        var currentX = 40f
        for (topNode in topLevelNodes) {
            val children = nodes.filter { it.item.parentId == topNode.item.id }
            val subNodeCount = children.sumOf { child ->
                val subs = nodes.count { it.item.parentId == child.item.id }
                if (subs > 0) subs else 1
            }.coerceAtLeast(1)

            val branchWidth = subNodeCount * (nodeWidth + horizontalGap)
            val topNodeX = currentX + (branchWidth - nodeWidth) / 2f

            val bNode = MindMapNodeLayout(
                id = topNode.item.id,
                text = topNode.item.text,
                level = 0,
                parentId = rootId,
                x = topNodeX,
                y = 40f + rowHeight,
                width = nodeWidth,
                height = nodeHeight,
                colorHex = topNode.item.textColor
            )
            result.add(bNode)

            var childX = currentX
            for (childNode in children) {
                val subChildren = nodes.filter { it.item.parentId == childNode.item.id }
                val childSpan = (subChildren.size.coerceAtLeast(1)) * (nodeWidth + horizontalGap)
                val cNodeX = childX + (childSpan - nodeWidth) / 2f

                val cNode = MindMapNodeLayout(
                    id = childNode.item.id,
                    text = childNode.item.text,
                    level = 1,
                    parentId = topNode.item.id,
                    x = cNodeX,
                    y = 40f + (rowHeight * 2),
                    width = nodeWidth,
                    height = nodeHeight,
                    colorHex = childNode.item.textColor
                )
                result.add(cNode)

                var subX = childX
                for (subNode in subChildren) {
                    val sNode = MindMapNodeLayout(
                        id = subNode.item.id,
                        text = subNode.item.text,
                        level = 2,
                        parentId = childNode.item.id,
                        x = subX,
                        y = 40f + (rowHeight * 3),
                        width = nodeWidth,
                        height = nodeHeight,
                        colorHex = subNode.item.textColor
                    )
                    result.add(sNode)
                    subX += nodeWidth + horizontalGap
                }

                childX += childSpan
            }

            currentX += branchWidth
        }

        // Center root X with the middle of branches
        if (result.size > 1) {
            val avgX = (result.drop(1).map { it.x }.average()).toFloat()
            val updatedRoot = rootNode.copy(x = (avgX - 10f).coerceAtLeast(40f))
            result[0] = updatedRoot
        }
    } else {
        var currentX = 40f
        for (topNode in topLevelNodes) {
            val children = nodes.filter { it.item.parentId == topNode.item.id }
            val subNodeCount = children.sumOf { child ->
                val subs = nodes.count { it.item.parentId == child.item.id }
                if (subs > 0) subs else 1
            }.coerceAtLeast(1)

            val branchWidth = subNodeCount * (nodeWidth + horizontalGap)
            val topNodeX = currentX + (branchWidth - nodeWidth) / 2f

            val bNode = MindMapNodeLayout(
                id = topNode.item.id,
                text = topNode.item.text,
                level = 0,
                parentId = null,
                x = topNodeX,
                y = 40f,
                width = 150f,
                height = 44f,
                isRoot = true,
                colorHex = topNode.item.textColor
            )
            result.add(bNode)

            var childX = currentX
            for (childNode in children) {
                val subChildren = nodes.filter { it.item.parentId == childNode.item.id }
                val childSpan = (subChildren.size.coerceAtLeast(1)) * (nodeWidth + horizontalGap)
                val cNodeX = childX + (childSpan - nodeWidth) / 2f

                val cNode = MindMapNodeLayout(
                    id = childNode.item.id,
                    text = childNode.item.text,
                    level = 1,
                    parentId = topNode.item.id,
                    x = cNodeX,
                    y = 40f + rowHeight,
                    width = nodeWidth,
                    height = nodeHeight,
                    colorHex = childNode.item.textColor
                )
                result.add(cNode)

                var subX = childX
                for (subNode in subChildren) {
                    val sNode = MindMapNodeLayout(
                        id = subNode.item.id,
                        text = subNode.item.text,
                        level = 2,
                        parentId = childNode.item.id,
                        x = subX,
                        y = 40f + (rowHeight * 2),
                        width = nodeWidth,
                        height = nodeHeight,
                        colorHex = subNode.item.textColor
                    )
                    result.add(sNode)
                    subX += nodeWidth + horizontalGap
                }

                childX += childSpan
            }

            currentX += branchWidth
        }
    }

    return result
}
