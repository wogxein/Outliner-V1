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
import com.example.ui.components.CustomColorPickerDialog
import com.example.ui.theme.AppDensity
import com.example.ui.theme.AppFontFamily
import com.example.ui.viewmodel.OutlinerViewModel
import kotlinx.coroutines.launch

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
    val showWordCountInNote by viewModel.showWordCountInNote.collectAsState()

    val notes by viewModel.notes.collectAsState()
    val folders by viewModel.folders.collectAsState()

    var showPrimaryColorPicker by remember { mutableStateOf(false) }
    var showBgColorPicker by remember { mutableStateOf(false) }

    var isExportingMdZip by remember { mutableStateOf(false) }
    var isExportingOpmlZip by remember { mutableStateOf(false) }
    var showImportOpmlDialog by remember { mutableStateOf(false) }
    var showImportMdDialog by remember { mutableStateOf(false) }

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
        // Section 1: Appearance & Colors
        Text(
            text = "APPEARANCE & THEMES",
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
                    text = "Base Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Choose light or dark base tone.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Light Option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isDarkTheme) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (!isDarkTheme) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.setDarkTheme(false) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (!isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Light",
                                fontWeight = if (!isDarkTheme) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Dark Option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isDarkTheme) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.setDarkTheme(true) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dark",
                                fontWeight = if (isDarkTheme) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                // Custom Primary / Accent Color
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPrimaryColorPicker = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val primaryPreviewColor = if (!customPrimaryHex.isNullOrBlank()) {
                        try { Color(android.graphics.Color.parseColor(customPrimaryHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                    } else MaterialTheme.colorScheme.primary

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(primaryPreviewColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accent / Primary Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = customPrimaryHex ?: "Default Palette",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Background Color
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showBgColorPicker = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bgPreviewColor = if (!customBgHex.isNullOrBlank()) {
                        try { Color(android.graphics.Color.parseColor(customBgHex)) } catch (e: Exception) { MaterialTheme.colorScheme.background }
                    } else MaterialTheme.colorScheme.background

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(bgPreviewColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Custom Background Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = customBgHex ?: "Default Surface",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
                Text(
                    text = "Font Family",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Inter (default), clean sans-serifs, elegant serifs, or import custom TTF/OTF.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Font Family list
                val fonts = listOf(
                    AppFontFamily.INTER,
                    AppFontFamily.ROBOTO,
                    AppFontFamily.LORA,
                    AppFontFamily.SOURCE_SERIF,
                    AppFontFamily.JETBRAINS_MONO,
                    AppFontFamily.MONO,
                    AppFontFamily.SYSTEM
                )

                fonts.forEach { fontOption ->
                    val isSelected = appFontFamilyKey.equals(fontOption.key, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                            .clickable { viewModel.setAppFontFamily(fontOption.key) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fontOption.displayName,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Text("Active", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Font Upload Button
                OutlinedButton(
                    onClick = { fontFilePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (appFontFamilyKey == "custom" && customFontPath != null) "Custom Font Active (Change .ttf/.otf)"
                        else "Import Custom Font (.ttf / .otf)",
                        fontSize = 13.sp
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                // Font Size Slider with Notches
                Text(
                    text = "Font Size Scale",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                val sizeLabels = listOf("Extra Small", "Small", "Normal", "Large", "Extra Large")
                val currentSizeLabel = sizeLabels.getOrElse(appFontSizeIdx) { "Normal" }
                Text(
                    text = "$currentSizeLabel (${appFontSizeIdx + 1}/5)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )

                Slider(
                    value = appFontSizeIdx.toFloat(),
                    onValueChange = { viewModel.setAppFontSizeIndex(it.toInt()) },
                    valueRange = 0f..4f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                // Layout Density
                Text(
                    text = "Layout Density",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Adjust padding, margins, and density between outline nodes.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppDensity.values().forEach { density ->
                        val isSelected = appDensityName == density.name
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setAppDensity(density.name) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = density.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Sidebar & Note Layout Customization
        Text(
            text = "SIDEBAR & INTERFACE",
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
                // Toggle Favorites in Sidebar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Favorites Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Show favorites in the left sidebar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showFavoritesInSidebar,
                        onCheckedChange = { viewModel.setShowFavoritesInSidebar(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Toggle Recent in Sidebar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recent Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Show recently edited notes in sidebar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showRecentInSidebar,
                        onCheckedChange = { viewModel.setShowRecentInSidebar(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Toggle Trash in Sidebar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Trash Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Show trash & recycle bin in sidebar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showTrashInSidebar,
                        onCheckedChange = { viewModel.setShowTrashInSidebar(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Toggle Item Counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Number Counts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Show note count badges in All Notes and Folders", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showItemCounts,
                        onCheckedChange = { viewModel.setShowItemCounts(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Toggle Word Count in Note Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Word Count & Statistics", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Show word count summary panel in note sidebar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showWordCountInNote,
                        onCheckedChange = { viewModel.setShowWordCountInNote(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 4: Export All Notes (Markdown ZIP & OPML XML ZIP)
        Text(
            text = "EXPORT ALL NOTES",
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
                    text = "Full Archive Export",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Export all notes packaged in a .zip file, with your complete folder and subfolder hierarchy preserved intact.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Markdown ZIP
                Button(
                    onClick = {
                        if (!isExportingMdZip) {
                            isExportingMdZip = true
                            scope.launch {
                                val zipFile = viewModel.exportAllNotesAsZip(context)
                                isExportingMdZip = false
                                if (zipFile != null && zipFile.exists()) {
                                    shareFile(context, zipFile, "Outliner Markdown Notes (.zip)")
                                } else {
                                    Toast.makeText(context, "Failed to create Markdown ZIP", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isExportingMdZip) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Markdown ZIP (.zip)", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // OPML XML ZIP
                OutlinedButton(
                    onClick = {
                        if (!isExportingOpmlZip) {
                            isExportingOpmlZip = true
                            scope.launch {
                                val zipFile = viewModel.exportAllNotesAsOpmlZip(context)
                                isExportingOpmlZip = false
                                if (zipFile != null && zipFile.exists()) {
                                    shareFile(context, zipFile, "Outliner OPML Outlines (.zip)")
                                } else {
                                    Toast.makeText(context, "Failed to create OPML ZIP", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isExportingOpmlZip) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting OPML...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export OPML XML ZIP (.zip)", fontSize = 13.sp)
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

        // Section: AI & Model Configuration
        val aiApiKey by viewModel.aiApiKey.collectAsState()
        val aiBaseUrl by viewModel.aiBaseUrl.collectAsState()
        val aiModelId by viewModel.aiModelId.collectAsState()
        val aiCustomTitle by viewModel.aiCustomTitle.collectAsState()
        val aiCustomSystemPrompt by viewModel.aiCustomSystemPrompt.collectAsState()

        var editAiApiKey by remember(aiApiKey) { mutableStateOf(aiApiKey) }
        var editAiBaseUrl by remember(aiBaseUrl) { mutableStateOf(aiBaseUrl) }
        var editAiModelId by remember(aiModelId) { mutableStateOf(aiModelId) }
        var editAiTitle by remember(aiCustomTitle) { mutableStateOf(aiCustomTitle) }
        var editAiSystemPrompt by remember(aiCustomSystemPrompt) { mutableStateOf(aiCustomSystemPrompt) }
        var showApiKeyText by remember { mutableStateOf(false) }

        Text(
            text = "AI & MODEL CONFIGURATION",
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
                    text = "Custom AI Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Configure your own AI endpoint, API key, model ID, and assistant persona so you don't rely on default keys.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // API Key Field
                Text("API Key", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                OutlinedTextField(
                    value = editAiApiKey,
                    onValueChange = {
                        editAiApiKey = it
                        viewModel.setAiApiKey(it)
                    },
                    placeholder = { Text("Enter your AI / Gemini API Key...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)
                )

                // Base URL / Endpoint Field
                Text("Base URL / Endpoint", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                OutlinedTextField(
                    value = editAiBaseUrl,
                    onValueChange = {
                        editAiBaseUrl = it
                        viewModel.setAiBaseUrl(it)
                    },
                    placeholder = { Text("https://generativelanguage.googleapis.com/v1beta/models", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)
                )

                // Model ID Field
                Text("Model ID", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                OutlinedTextField(
                    value = editAiModelId,
                    onValueChange = {
                        editAiModelId = it
                        viewModel.setAiModelId(it)
                    },
                    placeholder = { Text("gemini-2.5-flash", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)
                )

                // Assistant Title Field
                Text("Assistant Title", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                OutlinedTextField(
                    value = editAiTitle,
                    onValueChange = {
                        editAiTitle = it
                        viewModel.setAiCustomTitle(it)
                    },
                    placeholder = { Text("Research Assistant", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)
                )

                // Custom System Instruction / Prompt
                Text("Custom System Prompt / Role (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                OutlinedTextField(
                    value = editAiSystemPrompt,
                    onValueChange = {
                        editAiSystemPrompt = it
                        viewModel.setAiCustomSystemPrompt(it)
                    },
                    placeholder = { Text("e.g. Always summarize in bullet points for outliner notes...", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
                )
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
