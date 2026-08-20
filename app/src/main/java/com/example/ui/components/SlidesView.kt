package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Note
import com.example.domain.model.TreeItemNode
import com.example.util.PdfExporter

data class SlideItem(
    val title: String,
    val isCover: Boolean = false,
    val bullets: List<String> = emptyList(),
    val subtitle: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlidesView(
    note: Note,
    allNodes: List<TreeItemNode>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var showFilterDialog by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Memory-only selection set for this transform view
    val excludedNodeIds = remember { mutableStateMapOf<String, Boolean>() }

    // Filtered tree
    val activeNodes = remember(allNodes, excludedNodeIds.toMap()) {
        allNodes.filter { node ->
            node.item.text.isNotBlank() && excludedNodeIds[node.item.id] != true
        }
    }

    // Generate Slides preserving hierarchy
    val slides = remember(note.title, activeNodes) {
        generateSlidesFromTree(note.title, activeNodes)
    }

    var currentIndex by remember(slides) { mutableIntStateOf(0) }
    var slideMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar (hidden in fullscreen)
        if (!isFullscreen) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Slides: ${note.title.ifBlank { "Untitled" }}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (slides.isNotEmpty()) "Slide ${currentIndex + 1} of ${slides.size}" else "0 slides",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Slides")
                    }
                },
                actions = {
                    // Filter Nodes
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Nodes")
                    }
                    // Fullscreen Toggle
                    IconButton(onClick = { isFullscreen = true }) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
                    }
                    // Export to PDF
                    IconButton(onClick = {
                        val pdfSlides = slides.map { Pair(it.title, it.bullets) }
                        val pdf = PdfExporter.exportSlidesToPdf(
                            context = context,
                            noteTitle = note.title.ifBlank { "Untitled" },
                            slides = pdfSlides
                        )
                        if (pdf != null) {
                            PdfExporter.sharePdf(context, pdf, "Presentation Slides - ${note.title}")
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }

        if (slides.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Slideshow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No outline content to present",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Add topics and sub-items in the outline note to generate presentation slides.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        } else {
            val currentSlide = slides[currentIndex.coerceIn(0, slides.size - 1)]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullscreen) 12.dp else 18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Presentation Slide Card Canvas
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (currentSlide.isCover) {
                            // Title / Cover Slide
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape,
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PresentToAll,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = currentSlide.title,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 36.sp
                                    )

                                    if (!currentSlide.subtitle.isNullOrBlank()) {
                                        Text(
                                            text = currentSlide.subtitle,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 10.dp)
                                        )
                                    }

                                    Text(
                                        text = "${slides.size - 1} topic slides included",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 14.dp)
                                    )
                                }
                            }
                        } else {
                            // Topic Content Slide
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                // Slide Title Header
                                Text(
                                    text = currentSlide.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Blue Accent Bar
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp, bottom = 18.dp)
                                        .width(60.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )

                                // Bullet Points
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(currentSlide.bullets) { bullet ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 6.dp, end = 12.dp)
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                            Text(
                                                text = bullet,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 24.sp
                                            )
                                        }
                                    }
                                }

                                // Slide Footer
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.title.ifBlank { "Notes" },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Slide ${currentIndex + 1} / ${slides.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Left and Right tap gesture areas for previous/next slide navigation
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (currentIndex > 0) currentIndex--
                                    }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (currentIndex < slides.size - 1) currentIndex++
                                    }
                            )
                        }
                    }
                }

                // Slide Navigation Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = currentIndex > 0) {
                                if (currentIndex > 0) currentIndex--
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Slide",
                                modifier = Modifier.size(18.dp),
                                tint = if (currentIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Prev",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    // Slide Selector Dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { slideMenuExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Slide ${currentIndex + 1} of ${slides.size} ▾",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = slideMenuExpanded,
                            onDismissRequest = { slideMenuExpanded = false }
                        ) {
                            slides.forEachIndexed { sIdx, slide ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${sIdx + 1}. ${slide.title}",
                                            fontWeight = if (sIdx == currentIndex) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sIdx == currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        currentIndex = sIdx
                                        slideMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (isFullscreen) {
                        IconButton(onClick = { isFullscreen = false }) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen")
                        }
                    }

                    // Next Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentIndex < slides.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = currentIndex < slides.size - 1) {
                                if (currentIndex < slides.size - 1) currentIndex++
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Next",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentIndex < slides.size - 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Slide",
                                modifier = Modifier.size(18.dp),
                                tint = if (currentIndex < slides.size - 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Slideshow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Slide Nodes", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Toggle outline items to include in presentation slides (saved in memory, does not alter outline note):",
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
                TextButton(onClick = {
                    showFilterDialog = false
                    currentIndex = 0
                }) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Builds presentation slides preserving the outline hierarchy.
 * Slide 1: Cover slide with note title
 * Slide 2..N: Top-level section with children as bullets
 */
private fun generateSlidesFromTree(noteTitle: String, nodes: List<TreeItemNode>): List<SlideItem> {
    val slides = mutableListOf<SlideItem>()
    val cleanTitle = noteTitle.ifBlank { "Untitled Note" }

    // Cover Slide
    slides.add(
        SlideItem(
            title = cleanTitle,
            isCover = true,
            subtitle = "Overview & Key Branches"
        )
    )

    val topLevelNodes = nodes.filter { it.level == 0 }

    for (topNode in topLevelNodes) {
        val topItem = topNode.item
        if (topItem.text.isBlank()) continue

        val children = nodes.filter { it.item.parentId == topItem.id }
        val bullets = mutableListOf<String>()

        for (child in children) {
            if (child.item.text.isNotBlank()) {
                bullets.add(child.item.text.trim())

                // Include grandchild sub-items if present
                val subChildren = nodes.filter { it.item.parentId == child.item.id }
                for (subChild in subChildren) {
                    if (subChild.item.text.isNotBlank()) {
                        bullets.add("  - ${subChild.item.text.trim()}")
                    }
                }
            }
        }

        slides.add(
            SlideItem(
                title = topItem.text,
                isCover = false,
                bullets = bullets
            )
        )
    }

    return slides
}
