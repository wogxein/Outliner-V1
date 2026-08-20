package com.example.ui.screens.transform

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

data class FlashcardItem(
    val id: String,
    val question: String,
    val answers: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
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

    val excludedNodeIdsMap by viewModel.flashcardsExcludedNodeIds.collectAsState()
    val excludedIds = excludedNodeIdsMap[noteId] ?: emptySet()

    var showNodeSelector by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val flashcards = remember(treeItems, focusedItemId, excludedIds) {
        buildFlashcardDeck(treeItems, focusedItemId, excludedIds)
    }

    LaunchedEffect(flashcards.size) {
        if (currentIndex >= flashcards.size) {
            currentIndex = (flashcards.size - 1).coerceAtLeast(0)
        }
        isFlipped = false
    }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "cardFlip"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Flashcards: ${activeNote?.title ?: "Note"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (flashcards.isNotEmpty()) "Card ${currentIndex + 1} of ${flashcards.size}" else "0 cards",
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
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Cards")
                    }
                    IconButton(onClick = {
                        if (flashcards.isNotEmpty()) {
                            val pdfData = flashcards.map { it.question to it.answers }
                            val pdfFile = PdfExportHelper.exportFlashcardsToPdf(
                                context = context,
                                noteTitle = activeNote?.title ?: "Study Flashcards",
                                flashcards = pdfData
                            )
                            if (pdfFile != null) {
                                PdfExportHelper.sharePdf(context, pdfFile)
                            } else {
                                Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "No flashcards to export", Toast.LENGTH_SHORT).show()
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (flashcards.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / flashcards.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (flashcards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No flashcards available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add hierarchical outline points in your note to generate study cards.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val currentCard = flashcards[currentIndex]

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density.density
                        }
                        .clickable { isFlipped = !isFlipped },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (rotation <= 90f) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Text(
                                        text = "QUESTION / PROMPT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                Text(
                                    text = currentCard.question,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tap card to reveal answer",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationY = 180f },
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Text(
                                            text = "ANSWER & KEY DETAILS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }

                                    Text(
                                        text = currentCard.question,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    LazyColumn(
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        if (currentCard.answers.isEmpty()) {
                                            item {
                                                Text(
                                                    text = "No sub-points attached.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                        } else {
                                            items(currentCard.answers) { ans ->
                                                Row(
                                                    modifier = Modifier.padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = "• ",
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = ans,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "Tap to flip back",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        if (currentIndex > 0) {
                            currentIndex--
                            isFlipped = false
                        }
                    },
                    enabled = currentIndex > 0,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Card")
                }

                Button(
                    onClick = { isFlipped = !isFlipped },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFlipped) "Show Question" else "Reveal Answer")
                }

                FilledTonalIconButton(
                    onClick = {
                        if (currentIndex < flashcards.size - 1) {
                            currentIndex++
                            isFlipped = false
                        }
                    },
                    enabled = currentIndex < flashcards.size - 1,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Card")
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
                        text = "Select Cards for Flashcards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        excludedIds.forEach { viewModel.toggleFlashcardNode(noteId, it) }
                    }) {
                        Text("Include All")
                    }
                }

                Text(
                    text = "Include or exclude specific outline items from your study deck. (Original note is unchanged)",
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
                                    viewModel.toggleFlashcardNode(noteId, item.id)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = !isExcluded,
                                onCheckedChange = { viewModel.toggleFlashcardNode(noteId, item.id) }
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

private fun buildFlashcardDeck(
    treeItems: List<TreeItemNode>,
    focusedItemId: String?,
    excludedIds: Set<String>
): List<FlashcardItem> {
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

    val validItems = items.filter { !excludedIds.contains(it.item.id) && it.item.text.isNotBlank() }
    if (validItems.isEmpty()) return emptyList()

    val childrenMap = validItems.groupBy { it.item.parentId }
    val itemsWithChildren = validItems.filter { childrenMap[it.item.id]?.isNotEmpty() == true }

    val cards = mutableListOf<FlashcardItem>()

    if (itemsWithChildren.isEmpty()) {
        // Flat list: 1 single flashcard for overview
        cards.add(
            FlashcardItem(
                id = "__root_deck__",
                question = "Study Topic",
                answers = validItems.map { it.item.text.trim() }
            )
        )
        return cards
    }

    for (fi in itemsWithChildren) {
        val children = childrenMap[fi.item.id] ?: emptyList()
        val answers = children.map { it.item.text.trim() }.filter { it.isNotBlank() }
        if (answers.isNotEmpty()) {
            cards.add(
                FlashcardItem(
                    id = fi.item.id,
                    question = fi.item.text.trim(),
                    answers = answers
                )
            )
        }
    }

    return cards
}
