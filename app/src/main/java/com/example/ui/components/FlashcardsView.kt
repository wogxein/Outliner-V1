package com.example.ui.components

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Note
import com.example.domain.model.TreeItemNode
import com.example.util.PdfExporter

data class FlashcardItem(
    val id: String,
    val question: String,
    val parentContext: String? = null,
    val answers: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsView(
    note: Note,
    allNodes: List<TreeItemNode>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var showFilterDialog by remember { mutableStateOf(false) }
    var isShuffled by remember { mutableStateOf(false) }

    // Memory-only selection set for this transform view
    val excludedNodeIds = remember { mutableStateMapOf<String, Boolean>() }

    // Filtered tree
    val activeNodes = remember(allNodes, excludedNodeIds.toMap()) {
        allNodes.filter { node ->
            node.item.text.isNotBlank() && excludedNodeIds[node.item.id] != true
        }
    }

    // Generate Flashcards list
    val flashcards = remember(note.title, activeNodes, isShuffled) {
        val list = generateFlashcardsFromTree(note.title, activeNodes)
        if (isShuffled) list.shuffled() else list
    }

    var currentIndex by remember(flashcards) { mutableIntStateOf(0) }
    var isFlipped by remember(currentIndex) { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "CardFlip"
    )

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
                        text = "Flashcards: ${note.title.ifBlank { "Untitled" }}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (flashcards.isNotEmpty()) "Card ${currentIndex + 1} of ${flashcards.size}" else "0 cards",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Flashcards")
                }
            },
            actions = {
                // Shuffle Toggle
                IconButton(onClick = {
                    isShuffled = !isShuffled
                    currentIndex = 0
                    isFlipped = false
                }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Filter Nodes
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter Nodes")
                }
                // Export to PDF
                IconButton(onClick = {
                    val pdfCards = flashcards.map { card ->
                        Pair(card.question, card.answers)
                    }
                    val pdf = PdfExporter.exportFlashcardsToPdf(
                        context = context,
                        noteTitle = note.title.ifBlank { "Untitled" },
                        cards = pdfCards
                    )
                    if (pdf != null) {
                        PdfExporter.sharePdf(context, pdf, "Flashcards - ${note.title}")
                    }
                }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Progress bar
        if (flashcards.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / flashcards.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        if (flashcards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No non-empty outline items found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Add bullet items to your note to automatically generate flashcards.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        } else {
            val currentCard = flashcards[currentIndex.coerceIn(0, flashcards.size - 1)]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Flippable Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable { isFlipped = !isFlipped }
                ) {
                    if (rotation <= 90f) {
                        // FRONT SIDE (Question / Topic)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "TOPIC / QUESTION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Tap to flip",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                if (!currentCard.parentContext.isNullOrBlank()) {
                                    Text(
                                        text = currentCard.parentContext,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                Text(
                                    text = currentCard.question,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 30.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${currentCard.answers.size} supporting details",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // BACK SIDE (Answer / Supporting Details)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f }
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "SUPPORTING DETAILS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Tap to flip back",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                item {
                                    Text(
                                        text = currentCard.question,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }

                                if (currentCard.answers.isEmpty()) {
                                    item {
                                        Text(
                                            text = "Leaf node: Review topic context and key terms.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    items(currentCard.answers) { ans ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "•",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                text = ans,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "Card ${currentIndex + 1} of ${flashcards.size}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Navigation Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                if (currentIndex > 0) {
                                    currentIndex--
                                    isFlipped = false
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous",
                                modifier = Modifier.size(18.dp),
                                tint = if (currentIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Previous",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    // Flip indicator icon
                    IconButton(onClick = { isFlipped = !isFlipped }) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Flip Card",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Next Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentIndex < flashcards.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = currentIndex < flashcards.size - 1) {
                                if (currentIndex < flashcards.size - 1) {
                                    currentIndex++
                                    isFlipped = false
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Next",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentIndex < flashcards.size - 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                modifier = Modifier.size(18.dp),
                                tint = if (currentIndex < flashcards.size - 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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
                    Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Flashcard Nodes", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Toggle outline items to include in flashcards (saved in memory, does not alter outline note):",
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
 * Builds flashcard items from hierarchical outline tree.
 * Card rule:
 * - Only nodes that have child items create a flashcard (Front: Topic, Back: list of direct children).
 * - Empty leaf category items without sub-items do NOT create their own empty cards.
 * - If a child node has its own sub-items, it creates its own separate card.
 * - If entire tree is flat with no children, exactly 1 card is created with Note Title as front and items as back.
 */
private fun generateFlashcardsFromTree(noteTitle: String, nodes: List<TreeItemNode>): List<FlashcardItem> {
    val cards = mutableListOf<FlashcardItem>()
    val cleanTitle = noteTitle.ifBlank { "Note Overview" }

    val validNodes = nodes.filter { it.item.text.isNotBlank() }
    if (validNodes.isEmpty()) return emptyList()

    val itemMap = validNodes.associateBy { it.item.id }
    val childrenMap = validNodes.groupBy { it.item.parentId }

    val nodesWithChildren = validNodes.filter { childrenMap[it.item.id]?.isNotEmpty() == true }

    if (nodesWithChildren.isEmpty()) {
        // Flat outline: create single card with Note Title as question and all items as answers
        cards.add(
            FlashcardItem(
                id = "__root_title__",
                question = cleanTitle,
                parentContext = "Overview",
                answers = validNodes.map { it.item.text.trim() }
            )
        )
        return cards
    }

    // Root-level items without parent: check if we should create a root overview card if level 0 items exist
    val topLevelNodes = validNodes.filter { it.level == 0 || it.item.parentId == null }
    if (topLevelNodes.isNotEmpty() && !nodesWithChildren.any { it.level == 0 }) {
        cards.add(
            FlashcardItem(
                id = "__root_title__",
                question = cleanTitle,
                parentContext = "Overview",
                answers = topLevelNodes.map { it.item.text.trim() }
            )
        )
    }

    // For every node with children, create a flashcard
    for (node in nodesWithChildren) {
        val item = node.item
        val directChildren = childrenMap[item.id] ?: emptyList()
        val childAnswers = directChildren.map { it.item.text.trim() }.filter { it.isNotBlank() }

        if (childAnswers.isEmpty()) continue

        val parentItem = item.parentId?.let { itemMap[it]?.item }
        val contextStr = parentItem?.text?.take(30) ?: cleanTitle

        cards.add(
            FlashcardItem(
                id = item.id,
                question = item.text.trim(),
                parentContext = "Under: $contextStr",
                answers = childAnswers
            )
        )
    }

    return cards
}
