package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.domain.model.AiBackend
import com.example.ui.components.CustomColorPickerDialog
import com.example.ui.theme.AppDensity
import com.example.ui.theme.AppFontFamily
import com.example.ui.viewmodel.OutlinerViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SettingsScreen(
    viewModel: OutlinerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val customPrimaryHex by viewModel.customPrimaryColorHex.collectAsState()
    val customBgHex by viewModel.customBgColorHex.collectAsState()
    val appFontFamilyKey by viewModel.appFontFamily.collectAsState()
    val customFontPath by viewModel.customFontPath.collectAsState()
    val appFontSizeIdx by viewModel.appFontSizeIndex.collectAsState()
    val appDensityName by viewModel.appDensity.collectAsState()

    val showFavoritesInSidebar by viewModel.showFavoritesInSidebar.collectAsState()
    val showRecentInSidebar by viewModel.showRecentInSidebar.collectAsState()
    val showTrashInSidebar by viewModel.showTrashInSidebar.collectAsState()
    val showItemCounts by viewModel.showItemCounts.collectAsState()
    val hideWordCount by viewModel.hideWordCount.collectAsState()

    val notes by viewModel.notes.collectAsState()
    val folders by viewModel.folders.collectAsState()

    var showPrimaryColorPicker by remember { mutableStateOf(false) }
    var showBgColorPicker by remember { mutableStateOf(false) }

    var isExportingMdZip by remember { mutableStateOf(false) }
    var isExportingOpmlZip by remember { mutableStateOf(false) }
    var showImportOpmlDialog by remember { mutableStateOf(false) }
    var showImportMdDialog by remember { mutableStateOf(false) }
    var showBackendDialog by remember { mutableStateOf(false) }
    var editingBackend by remember { mutableStateOf<AiBackend?>(null) }

    // Custom Font File Picker (.ttf / .otf)
    val fontFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importCustomFont(context, it)
        }
    }

    // ZIP file picker launcher
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importZipArchive(context, it)
        }
    }

    // Individual MD / OPML file picker launcher
    val singleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            scope.launch {
                try {
                    val content = context.contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { it.readText() }
                    if (!content.isNullOrBlank()) {
                        val path = fileUri.path ?: ""
                        if (path.endsWith(".opml", ignoreCase = true) || content.contains("<opml")) {
                            viewModel.importOpmlContent(content)
                        } else {
                            viewModel.importMarkdownContent(content)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error reading file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings & Appearance",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Section 1: Appearance & Theme
        Text(
            text = "THEME & COLORS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Dark Theme Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dark Theme",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isDarkTheme) "Dark mode enabled" else "Light mode enabled",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.setDarkTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                // Custom Primary Accent Color
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Primary Accent Color",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = customPrimaryHex ?: "Default Indigo",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Current color preview circle
                        val previewColor = customPrimaryHex?.let {
                            try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        } ?: MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(previewColor)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { showPrimaryColorPicker = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showPrimaryColorPicker = true }) {
                            Text("Change", fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                // Custom Background Color
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "App Background Color",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = customBgHex ?: "Default Theme Background",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val previewBg = customBgHex?.let {
                            try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { MaterialTheme.colorScheme.background }
                        } ?: MaterialTheme.colorScheme.background
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(previewBg)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { showBgColorPicker = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showBgColorPicker = true }) {
                            Text("Change", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Typography & Font Family
        Text(
            text = "TYPOGRAPHY & FONTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Choose Font : [BOX] [+]
                var fontDropdownExpanded by remember { mutableStateOf(false) }

                val activeFontDisplayName = if (customFontPath != null) {
                    "Custom Font (Imported)"
                } else {
                    AppFontFamily.values().find { it.key == appFontFamilyKey }?.displayName ?: "System Default"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FontDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Choose Font :",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // [BOX] Dropdown Box
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { fontDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = activeFontDisplayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Font",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = fontDropdownExpanded,
                                onDismissRequest = { fontDropdownExpanded = false }
                            ) {
                                AppFontFamily.values().forEach { font ->
                                    val isSelected = (customFontPath == null && appFontFamilyKey == font.key)
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = font.displayName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.clearCustomFont()
                                            viewModel.setAppFontFamily(font.key)
                                            fontDropdownExpanded = false
                                        }
                                    )
                                }

                                if (customFontPath != null) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Custom Font (Imported)",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            fontDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // [+] Button to import custom font
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    fontFilePickerLauncher.launch("*/*")
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Import Font",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                // Font Size Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Base Font Size Scale",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        val sizeLabels = listOf("Small", "Compact", "Default", "Large", "Extra Large")
                        Text(
                            text = sizeLabels.getOrElse(appFontSizeIdx) { "Default" },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                var sliderVal by remember(appFontSizeIdx) { mutableFloatStateOf(appFontSizeIdx.toFloat()) }
                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    onValueChangeFinished = { viewModel.setAppFontSizeIndex(sliderVal.toInt()) },
                    valueRange = 0f..4f,
                    steps = 3,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                // Density Selection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SpaceDashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Layout Spacing & Density",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppDensity.values().forEach { density ->
                        val isSelected = (appDensityName == density.name)
                        OutlinedButton(
                            onClick = { viewModel.setAppDensity(density.name) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
                            )
                        ) {
                            Text(
                                text = density.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Sidebar & View Options
        Text(
            text = "SIDEBAR & OUTLINE PREFERENCES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Show Favorites
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Show Favorites section in sidebar", fontSize = 13.sp)
                    Switch(
                        checked = showFavoritesInSidebar,
                        onCheckedChange = { viewModel.setShowFavoritesInSidebar(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // Show Recent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Show Recent notes section in sidebar", fontSize = 13.sp)
                    Switch(
                        checked = showRecentInSidebar,
                        onCheckedChange = { viewModel.setShowRecentInSidebar(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // Show Trash
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Show Trash & Deleted section", fontSize = 13.sp)
                    Switch(
                        checked = showTrashInSidebar,
                        onCheckedChange = { viewModel.setShowTrashInSidebar(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // Show Item Counts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Show outline item count badges", fontSize = 13.sp)
                    Switch(
                        checked = showItemCounts,
                        onCheckedChange = { viewModel.setShowItemCounts(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // Hide Word Count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Hide Word Count", fontSize = 13.sp)
                    Switch(
                        checked = hideWordCount,
                        onCheckedChange = { viewModel.setHideWordCount(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 4: Export All Notes (to ZIP)
        Text(
            text = "EXPORT ALL NOTES & BACKUP",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Export Entire Library",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Export all ${notes.size} notes across ${folders.size} folders packaged neatly in a single .zip archive.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Export Markdown ZIP
                Button(
                    onClick = {
                        isExportingMdZip = true
                        scope.launch {
                            try {
                                val file = viewModel.exportAllNotesToMarkdownZip(context)
                                isExportingMdZip = false
                                if (file != null) {
                                    shareFile(context, file, "Outliner_Markdown_Export.zip")
                                }
                            } catch (e: Exception) {
                                isExportingMdZip = false
                                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExportingMdZip && !isExportingOpmlZip
                ) {
                    if (isExportingMdZip) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating Markdown ZIP...")
                    } else {
                        Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export All Notes to Markdown ZIP (.zip)", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Export OPML ZIP
                OutlinedButton(
                    onClick = {
                        isExportingOpmlZip = true
                        scope.launch {
                            try {
                                val file = viewModel.exportAllNotesToOpmlZip(context)
                                isExportingOpmlZip = false
                                if (file != null) {
                                    shareFile(context, file, "Outliner_OPML_Export.zip")
                                }
                            } catch (e: Exception) {
                                isExportingOpmlZip = false
                                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExportingMdZip && !isExportingOpmlZip
                ) {
                    if (isExportingOpmlZip) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating OPML ZIP...")
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export All Notes to OPML ZIP (.zip)", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 5: Import All Notes (from ZIP)
        Text(
            text = "IMPORT ALL NOTES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Import Notes ZIP Archive",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Restore or import folders and notes directly from a Markdown or OPML .zip archive.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Button(
                    onClick = {
                        zipPickerLauncher.launch("application/zip")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select & Import ZIP File (.zip)", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 6: Import Individual Notes (MD / OPML)
        Text(
            text = "IMPORT INDIVIDUAL NOTE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Import Single Note (MD / OPML)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Import single notes from Markdown files (.md), OPML files (.opml), or paste raw text content.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showImportOpmlDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste OPML", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showImportMdDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste MD", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        singleFilePickerLauncher.launch("*/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Note File (.md / .opml)", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: AI Backends & OpenAI-Compatible Configuration
        val aiBackends by viewModel.aiBackends.collectAsState()
        val activeAiBackendId by viewModel.activeAiBackendId.collectAsState()
        val activeAiBackend by viewModel.activeAiBackend.collectAsState()
        val isAiFeatureEnabled by viewModel.isAiFeatureEnabled.collectAsState()

        Text(
            text = "AI ASSISTANT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Master AI Toggle Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable AI Assistant Functions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Toggle on or off all AI assistant buttons, web research, and note generation features throughout the app.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = isAiFeatureEnabled,
                    onCheckedChange = { viewModel.setAiFeatureEnabled(it) }
                )
            }
        }

        if (isAiFeatureEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configured AI Backends",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Configure and switch between multiple OpenAI-compatible API backends (Gemini OpenAI endpoint, OpenAI, Ollama, OpenRouter, Groq, etc.).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                // Active Backend Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeAiBackend.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                if (activeAiBackend.isDefault) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = "DEFAULT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Models: ${activeAiBackend.modelsList.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "URL: ${activeAiBackend.url}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }

                // List of Saved Backends
                Text(
                    text = "All Configured Backends (${aiBackends.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                aiBackends.forEach { backend ->
                    val isSelected = (backend.id == activeAiBackendId || (activeAiBackendId == null && backend.isDefault))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectAiBackend(backend.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = backend.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (backend.isDefault) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("(Default)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = backend.modelsList.firstOrNull() ?: "Default Model",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Actions
                            if (!backend.isDefault) {
                                TextButton(
                                    onClick = { viewModel.setDefaultAiBackend(backend.id) },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Make Default", fontSize = 11.sp)
                                }
                            }

                            TextButton(
                                onClick = {
                                    editingBackend = backend
                                    showBackendDialog = true
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Edit", fontSize = 11.sp)
                            }

                            if (aiBackends.size > 1) {
                                TextButton(
                                    onClick = { viewModel.deleteAiBackend(backend.id) },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Delete", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add Backend Button
                Button(
                    onClick = {
                        editingBackend = null
                        showBackendDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add AI Backend", fontSize = 13.sp)
                }
            }
        }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 7: Privacy & App Info
        Text(
            text = "ABOUT & PRIVACY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% Offline & Private",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "All your notes and outlines are stored strictly on your local device in Room SQLite database. No external tracking.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "App Version", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "2.00", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // Color Pickers
    if (showPrimaryColorPicker) {
        CustomColorPickerDialog(
            title = "Pick Primary Accent Color",
            initialHex = customPrimaryHex,
            presetColors = listOf("#4F46E5", "#2563EB", "#0D9488", "#16A34A", "#CA8A04", "#EA580C", "#DC2626", "#7C3AED", "#DB2777"),
            onColorSelected = { hex ->
                viewModel.setCustomPrimaryColor(hex)
                showPrimaryColorPicker = false
            },
            onDismiss = { showPrimaryColorPicker = false }
        )
    }

    if (showBgColorPicker) {
        CustomColorPickerDialog(
            title = "Pick Custom Background Color",
            initialHex = customBgHex,
            presetColors = if (isDarkTheme) listOf("#121212", "#1E1E1E", "#18181B", "#0F172A", "#1A1B26", "#2E3440")
            else listOf("#FFFFFF", "#F8FAFC", "#F9FAFB", "#FFFBEB", "#F0FDF4", "#FAF5FF"),
            onColorSelected = { hex ->
                viewModel.setCustomBgColor(hex)
                showBgColorPicker = false
            },
            onDismiss = { showBgColorPicker = false }
        )
    }

    // Dialog: Add / Edit AI Backend
    if (showBackendDialog) {
        val isEditing = editingBackend != null
        var backendName by remember(editingBackend) { mutableStateOf(editingBackend?.name ?: "Gemini") }
        var backendApiKey by remember(editingBackend) { mutableStateOf(editingBackend?.apiKey ?: "") }
        var backendUrl by remember(editingBackend) { mutableStateOf(editingBackend?.url ?: "https://generativelanguage.googleapis.com/v1beta/openai/") }
        var backendModels by remember(editingBackend) { mutableStateOf(editingBackend?.models ?: "gemini-3.5-flash-lite") }
        var backendIsDefault by remember(editingBackend) { mutableStateOf(editingBackend?.isDefault ?: false) }

        AlertDialog(
            onDismissRequest = { showBackendDialog = false },
            title = { Text(if (isEditing) "Edit AI Backend" else "Add AI Backend", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Backend Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = backendName,
                        onValueChange = { backendName = it },
                        placeholder = { Text("e.g. Gemini / Ollama Local") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp)
                    )

                    Text("Base URL", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it },
                        placeholder = { Text("e.g. https://generativelanguage.googleapis.com/v1beta/openai/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp)
                    )

                    Text("API Key (api_key)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = backendApiKey,
                        onValueChange = { backendApiKey = it },
                        placeholder = { Text("api_key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp)
                    )

                    Text("Models (comma separated)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = backendModels,
                        onValueChange = { backendModels = it },
                        placeholder = { Text("e.g. gemini-3.5-flash-lite") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Set as Default Backend", fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = backendIsDefault,
                            onCheckedChange = { backendIsDefault = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        val newBackend = AiBackend(
                            id = editingBackend?.id ?: UUID.randomUUID().toString(),
                            name = backendName.trim().ifBlank { "Gemini" },
                            url = backendUrl.trim().ifBlank { "https://generativelanguage.googleapis.com/v1beta/openai/" },
                            apiKey = backendApiKey.trim(),
                            models = backendModels.trim().ifBlank { "gemini-3.5-flash-lite" },
                            isDefault = backendIsDefault,
                            createdAt = editingBackend?.createdAt ?: now,
                            updatedAt = now
                        )
                        viewModel.saveAiBackend(newBackend)
                        showBackendDialog = false
                    }
                ) {
                    Text("Save Backend")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Paste OPML
    if (showImportOpmlDialog) {
        var opmlInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showImportOpmlDialog = false },
            title = { Text("Import OPML", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Paste OPML XML content to import as a new note:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = opmlInput,
                        onValueChange = { opmlInput = it },
                        placeholder = { Text("<opml version=\"2.0\">...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (opmlInput.isNotBlank()) {
                            viewModel.importOpmlContent(opmlInput.trim())
                            showImportOpmlDialog = false
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportOpmlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Paste Markdown
    if (showImportMdDialog) {
        var mdInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showImportMdDialog = false },
            title = { Text("Import Markdown", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Paste Markdown text to import as a new note outline:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mdInput,
                        onValueChange = { mdInput = it },
                        placeholder = { Text("# My Note\n- Item 1\n  - Subitem 1.1") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mdInput.isNotBlank()) {
                            viewModel.importMarkdownContent(mdInput.trim())
                            showImportMdDialog = false
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportMdDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun shareFile(context: Context, file: java.io.File, title: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share or Save ZIP"))
    } catch (e: Exception) {
        Toast.makeText(context, "Exported: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
