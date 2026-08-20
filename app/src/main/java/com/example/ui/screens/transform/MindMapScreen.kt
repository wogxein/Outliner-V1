package com.example.ui.screens.transform

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Note
import com.example.domain.model.OutlineItem
import com.example.domain.model.TreeItemNode
import com.example.ui.viewmodel.OutlinerViewModel
import com.example.util.PdfExportHelper
import kotlin.math.*

data class MindMapNode(
    val id: String,
    val text: String,
    val depth: Int,
    val parentId: String?,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(
    noteId: String,
    focusedItemId: String? = null,
    viewModel: OutlinerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val activeNote = notes.find { it.id == noteId }
    val outlineItems by viewModel.outlineItems.collectAsState()
    val treeItems by viewModel.flattenedTree.collectAsState()

    val excludedNodeIdsMap by viewModel.mindMapExcludedNodeIds.collectAsState()
    val excludedIds = excludedNodeIdsMap[noteId] ?: emptySet()

    var showNodeSelector by remember { mutableStateOf(false) }

    // Canvas Transformation State (Pan & Zoom)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.3f, 3.5f)
        offset += offsetChange
    }

    val textMeasurer = rememberTextMeasurer()

    // Palette for different branches/depths
    val branchColors = remember {
        listOf(
            Color(0xFF3B82F6), // Blue
            Color(0xFF10B981), // Emerald
            Color(0xFF8B5CF6), // Purple
            Color(0xFFF59E0B), // Amber
            Color(0xFFEC4899), // Pink
            Color(0xFF06B6D4), // Cyan
            Color(0xFF14B8A6), // Teal
            Color(0xFF6366F1)  // Indigo
        )
    }

    val nodes = remember(activeNote, treeItems, focusedItemId, excludedIds, branchColors) {
        layoutMindMap(
            rootTitle = activeNote?.title ?: "Note",
            items = treeItems,
            focusedItemId = focusedItemId,
            excludedIds = excludedIds,
            colors = branchColors
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mind Map: ${activeNote?.title ?: "Note"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${nodes.size} nodes mapped • Pinch to zoom, drag to pan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Reset View
                    IconButton(onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center View")
                    }
                    // Filter Nodes Dialog
                    IconButton(onClick = { showNodeSelector = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Nodes")
                    }
                    // Export to PDF
                    IconButton(onClick = {
                        if (nodes.isNotEmpty()) {
                            val treeData = nodes.filter { it.depth > 0 }.map { node ->
                                val childTexts = nodes.filter { it.parentId == node.id }.map { it.text }
                                Pair(node.text, childTexts)
                            }
                            val pdfFile = PdfExportHelper.exportMindMapToPdf(
                                context = context,
                                noteTitle = activeNote?.title ?: "Mind Map",
                                rootTitle = activeNote?.title ?: "Mind Map",
                                treeNodes = treeData
                            )
                            if (pdfFile != null) {
                                PdfExportHelper.sharePdf(context, pdfFile)
                            } else {
                                Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clipToBounds()
                .transformable(state = transformState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = if (scale > 1.2f) 1f else 1.8f
                            offset = Offset.Zero
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                val centerCanvas = Offset(size.width / 2f, size.height / 2f)

                // 1. Draw Connecting Curved Bezier Lines
                val nodeMap = nodes.associateBy { it.id }
                for (node in nodes) {
                    if (node.parentId != null && nodeMap.containsKey(node.parentId)) {
                        val parent = nodeMap[node.parentId]!!
                        val start = centerCanvas + Offset(parent.x, parent.y)
                        val end = centerCanvas + Offset(node.x, node.y)

                        val path = Path().apply {
                            moveTo(start.x, start.y)
                            val midX = (start.x + end.x) / 2f
                            cubicTo(midX, start.y, midX, end.y, end.x, end.y)
                        }

                        drawPath(
                            path = path,
                            color = node.color.copy(alpha = 0.6f),
                            style = Stroke(width = (2.5f * (3 - node.depth).coerceAtLeast(1)).dp.toPx())
                        )
                    }
                }

                // 2. Draw Nodes
                for (node in nodes) {
                    val nodeCenter = centerCanvas + Offset(node.x, node.y)
                    val isRoot = node.depth == 0

                    val widthPx = node.width.dp.toPx()
                    val heightPx = node.height.dp.toPx()
                    val topLeft = Offset(nodeCenter.x - widthPx / 2f, nodeCenter.y - heightPx / 2f)

                    // Draw Node Background
                    drawRoundRect(
                        color = if (isRoot) node.color else node.color.copy(alpha = 0.15f),
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(widthPx, heightPx),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )

                    // Draw Node Border
                    drawRoundRect(
                        color = node.color,
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(widthPx, heightPx),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(width = if (isRoot) 2.5.dp.toPx() else 1.5.dp.toPx())
                    )

                    // Measure and Draw Text
                    val textColor = if (isRoot) Color.White else Color.Black
                    val measuredText = textMeasurer.measure(
                        text = AnnotatedString(node.text),
                        style = TextStyle(
                            fontSize = if (isRoot) 14.sp else 12.sp,
                            fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        constraints = androidx.compose.ui.unit.Constraints(
                            maxWidth = (widthPx - 16.dp.toPx()).toInt().coerceAtLeast(10)
                        )
                    )

                    drawText(
                        textLayoutResult = measuredText,
                        topLeft = Offset(
                            nodeCenter.x - measuredText.size.width / 2f,
                            nodeCenter.y - measuredText.size.height / 2f
                        )
                    )
                }
            }

            // Bottom Controls Bar (Zoom In, Zoom Out, Reset, Filter)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scale = (scale * 1.25f).coerceAtMost(3.5f) }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                    }
                    IconButton(onClick = { scale = (scale / 1.25f).coerceAtLeast(0.3f) }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                    }
                    IconButton(onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom")
                    }
                    VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                    TextButton(onClick = { showNodeSelector = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Filter (${nodes.size})", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet to include/exclude nodes (in-memory, without modifying the note)
    if (showNodeSelector) {
        ModalBottomSheet(
            onDismissRequest = { showNodeSelector = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Nodes for Mind Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        // Reset all excluded nodes
                        excludedIds.forEach { viewModel.toggleMindMapNode(noteId, it) }
                    }) {
                        Text("Include All")
                    }
                }

                Text(
                    text = "Include or exclude specific branches from the mind map. (The original note is never modified)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    items(outlineItems) { item ->
                        val isExcluded = excludedIds.contains(item.id)
                        val indent = (treeItems.find { it.item.id == item.id }?.level ?: 0) * 16
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = indent.dp)
                                .clickable {
                                    viewModel.toggleMindMapNode(noteId, item.id)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = !isExcluded,
                                onCheckedChange = { viewModel.toggleMindMapNode(noteId, item.id) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.text.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showNodeSelector = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun layoutMindMap(
    rootTitle: String,
    items: List<TreeItemNode>,
    focusedItemId: String?,
    excludedIds: Set<String>,
    colors: List<Color>
): List<MindMapNode> {
    val result = mutableListOf<MindMapNode>()

    // Filter items to focused item subtree if specified
    val filteredItems = if (focusedItemId != null) {
        val target = items.find { it.item.id == focusedItemId }
        if (target != null) {
            val focusedChildren = mutableListOf(target)
            fun collect(parentId: String) {
                val children = items.filter { it.item.parentId == parentId }
                for (c in children) {
                    focusedChildren.add(c)
                    collect(c.item.id)
                }
            }
            collect(target.item.id)
            focusedChildren
        } else {
            items
        }
    } else {
        items
    }

    // Root Node at (0, 0)
    val rootId = "root_node"
    result.add(
        MindMapNode(
            id = rootId,
            text = rootTitle.ifBlank { "Central Theme" },
            depth = 0,
            parentId = null,
            x = 0f,
            y = 0f,
            width = 160f,
            height = 56f,
            color = Color(0xFF4F46E5)
        )
    )

    // Primary branches (Direct children of root or top-level items)
    val primaryItems = filteredItems.filter { it.item.parentId == null && !excludedIds.contains(it.item.id) }
    val totalPrimary = primaryItems.size.coerceAtLeast(1)

    // Radial Layout for primary branches
    val radiusStep = 260f

    primaryItems.forEachIndexed { index, fi ->
        val angle = (2 * PI * index / totalPrimary) - (PI / 2) // Start from top
        val color = colors[index % colors.size]

        val x = (radiusStep * cos(angle)).toFloat()
        val y = (radiusStep * sin(angle)).toFloat()

        result.add(
            MindMapNode(
                id = fi.item.id,
                text = fi.item.text.ifBlank { "Point ${index + 1}" },
                depth = 1,
                parentId = rootId,
                x = x,
                y = y,
                width = 140f,
                height = 46f,
                color = color
            )
        )

        // Sub-branches (Depth 2+)
        fun layoutSubChildren(parentFi: TreeItemNode, parentX: Float, parentY: Float, parentAngle: Double, depth: Int) {
            val children = filteredItems.filter { it.item.parentId == parentFi.item.id && !excludedIds.contains(it.item.id) }
            if (children.isEmpty()) return

            val subRadius = 180f
            val spreadAngle = PI / 3 // 60 degrees spread

            children.forEachIndexed { cIdx, child ->
                val childAngle = if (children.size == 1) {
                    parentAngle
                } else {
                    parentAngle - (spreadAngle / 2) + (spreadAngle * cIdx / (children.size - 1))
                }

                val cx = parentX + (subRadius * cos(childAngle)).toFloat()
                val cy = parentY + (subRadius * sin(childAngle)).toFloat()

                result.add(
                    MindMapNode(
                        id = child.item.id,
                        text = child.item.text.ifBlank { "Sub-item" },
                        depth = depth,
                        parentId = parentFi.item.id,
                        x = cx,
                        y = cy,
                        width = 120f,
                        height = 40f,
                        color = color.copy(alpha = 0.85f)
                    )
                )

                if (depth < 3) {
                    layoutSubChildren(child, cx, cy, childAngle, depth + 1)
                }
            }
        }

        layoutSubChildren(fi, x, y, angle, 2)
    }

    return result
}
