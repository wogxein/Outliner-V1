package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomColorPickerDialog(
    title: String,
    initialHex: String?,
    presetColors: List<String>,
    onColorSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var hexInput by remember { mutableStateOf(initialHex ?: "#4F46E5") }
    
    // Parse initial RGB
    val initialColor = try {
        Color(android.graphics.Color.parseColor(hexInput))
    } catch (e: Exception) {
        Color(0xFF4F46E5)
    }

    var red by remember { mutableFloatStateOf(initialColor.red * 255f) }
    var green by remember { mutableFloatStateOf(initialColor.green * 255f) }
    var blue by remember { mutableFloatStateOf(initialColor.blue * 255f) }

    val currentColor = Color(red.toInt(), green.toInt(), blue.toInt())
    val currentHex = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Color Preview Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = currentHex,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "RGB(${red.toInt()}, ${green.toInt()}, ${blue.toInt()})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Presets
                Text(
                    text = "Presets",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { hex ->
                        val presetColor = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(presetColor)
                                .border(
                                    width = if (currentHex.equals(hex, ignoreCase = true)) 2.5.dp else 1.dp,
                                    color = if (currentHex.equals(hex, ignoreCase = true)) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    red = presetColor.red * 255f
                                    green = presetColor.green * 255f
                                    blue = presetColor.blue * 255f
                                    hexInput = hex
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sliders: Red, Green, Blue
                Text("Red: ${red.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Slider(
                    value = red,
                    onValueChange = {
                        red = it
                        hexInput = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
                    },
                    valueRange = 0f..255f
                )

                Text("Green: ${green.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Slider(
                    value = green,
                    onValueChange = {
                        green = it
                        hexInput = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
                    },
                    valueRange = 0f..255f
                )

                Text("Blue: ${blue.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Slider(
                    value = blue,
                    onValueChange = {
                        blue = it
                        hexInput = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
                    },
                    valueRange = 0f..255f
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Hex Input
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        if (input.startsWith("#") && (input.length == 7 || input.length == 9)) {
                            try {
                                val c = Color(android.graphics.Color.parseColor(input))
                                red = c.red * 255f
                                green = c.green * 255f
                                blue = c.blue * 255f
                            } catch (e: Exception) {
                                // Invalid hex
                            }
                        }
                    },
                    label = { Text("Hex Code (#RRGGBB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(currentHex)
                }
            ) {
                Text("Apply Color")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onColorSelected(null) }) {
                    Text("Reset Default")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
