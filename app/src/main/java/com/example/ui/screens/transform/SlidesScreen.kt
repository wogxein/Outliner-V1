package com.example.ui.screens.transform

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

data class SlideItem(
    val id: String,
    val title: String,
    val bullets: List<String>,
    val slideNumber: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlidesScreen(
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

    val excludedNodeIdsMap by viewModel.slidesExcludedNodeIds.collectAsState()
    val excludedIds = excludedNodeIdsMap[noteId] ?: emptySet()

    var showNodeSelector by remember { mutableStateOf(false) }
    var currentSlideIndex by remember { mutableIntStateOf(0) }

    val slides = remember(activeNote, treeItems, focusedItemId, excludedIds) {
        buildSlidesDeck(activeNote?.title ?: "Presentation", treeItems, focusedItemId, excludedIds)
    }

    LaunchedEffect(slides.size) {
        if (currentSlideIndex >= slides.size) {
            currentSlideIndex = (slides.size - 1).coerceAtLeast(0)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Slides: ${activeNote?.title ?: "Presentation"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (slides.isNotEmpty()) "Slide ${currentSlideIndex + 1} of ${slides.size}" else "0 slides",
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
                    IconButton(onClick = { showNodeSelector = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Slides")
                    }
                    IconButton(onClick = {
                        if (slides.isNotEmpty()) {
                            val pdfData = slides.map { it.title to it.bullets }
                            val pdfFile = PdfExportHelper.exportSlidesToPdf(
                                context = context,
                                noteTitle = activeNote?.title ?: "Presentation",
                                slides = pdfData
                            )
                            if (pdfFile != null) {
                                PdfExportHelper.sharePdf(context, pdfFile)
                            } else {
                                Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "No slides to export", Toast.LENGTH_SHORT).show()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (slides.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Slideshow,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No slides to display",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add outline points to your note to automatically build presentation slides.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val activeSlide = slides[currentSlideIndex]

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = activeSlide.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                HorizontalDivider(
                                    thickness = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(bottom = 20.dp)
                                )

                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    if (activeSlide.bullets.isEmpty()) {
                                        item {
                                            Text(
                                                text = "Key concept overview.",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        items(activeSlide.bullets) { bullet ->
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 8.dp, end = 12.dp)
                                                        .size(8.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.primary,
                                                            shape = RoundedCornerShape(2.dp)
                                                        )
                                                )
                                                Text(
                                                    text = bullet,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    lineHeight = 26.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeNote?.title ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${currentSlideIndex + 1} / ${slides.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Left & Right tap touch areas for slide navigation
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (currentSlideIndex > 0) currentSlideIndex--
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
                                        if (currentSlideIndex < slides.size - 1) currentSlideIndex++
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (slides.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(slides) { idx, slide ->
                        val isSelected = idx == currentSlideIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .width(90.dp)
                                .height(56.dp)
                                .clickable { currentSlideIndex = idx }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = slide.title,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "#${idx + 1}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        if (currentSlideIndex > 0) currentSlideIndex--
                    },
                    enabled = currentSlideIndex > 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Previous")
                }

                Button(
                    onClick = {
                        if (currentSlideIndex < slides.size - 1) currentSlideIndex++
                    },
                    enabled = currentSlideIndex < slides.size - 1,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }

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
                        text = "Select Content for Slides",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        excludedIds.forEach { viewModel.toggleSlideNode(noteId, it) }
                    }) {
                        Text("Include All")
                    }
                }

                Text(
                    text = "Select nodes to include as slides in your presentation. (Original note is unchanged)",
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
                                    viewModel.toggleSlideNode(noteId, item.id)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = !isExcluded,
                                onCheckedChange = { viewModel.toggleSlideNode(noteId, item.id) }
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

private fun buildSlidesDeck(
    noteTitle: String,
    treeItems: List<TreeItemNode>,
    focusedItemId: String?,
    excludedIds: Set<String>
): List<SlideItem> {
    val items = if (focusedItemId != null) {
        val target = treeItems.find { it.item.id == focusedItemId }
        if (target != null) {
            val focusedChildren = mutableListOf(target)
            fun collect(parentId: String) {
                val children = treeItems.filter { it.item.parentId == parentId }
                for (c in children) {
                    focusedChildren.add(c)
                    collect(c.item.id)
                }
            }
            collect(target.item.id)
            focusedChildren
        } else {
            treeItems
        }
    } else {
        treeItems
    }

    val slides = mutableListOf<SlideItem>()

    val topLevelItems = items.filter { it.item.parentId == null && !excludedIds.contains(it.item.id) && it.item.text.isNotBlank() }

    slides.add(
        SlideItem(
            id = "title-slide",
            title = noteTitle,
            bullets = topLevelItems.map { it.item.text },
            slideNumber = 1
        )
    )

    var slideNum = 2
    for (fi in items) {
        if (excludedIds.contains(fi.item.id) || fi.item.text.isBlank()) continue

        val subPoints = items.filter { it.item.parentId == fi.item.id && !excludedIds.contains(it.item.id) && it.item.text.isNotBlank() }
        if (subPoints.isNotEmpty()) {
            slides.add(
                SlideItem(
                    id = fi.item.id,
                    title = fi.item.text,
                    bullets = subPoints.map { it.item.text },
                    slideNumber = slideNum++
                )
            )
        }
    }

    return slides
}
