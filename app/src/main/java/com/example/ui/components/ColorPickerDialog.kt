package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    // Initial HSV calculation
    var hexInput by remember { mutableStateOf(initialHex ?: "#4F46E5") }

    val initialHsv = remember(initialHex) {
        val hsv = FloatArray(3)
        try {
            val parsed = android.graphics.Color.parseColor(initialHex ?: "#4F46E5")
            android.graphics.Color.colorToHSV(parsed, hsv)
        } catch (e: Exception) {
            hsv[0] = 240f
            hsv[1] = 0.7f
            hsv[2] = 0.9f
        }
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    fun updateFromHsv(h: Float, s: Float, v: Float) {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        val r = android.graphics.Color.red(rgb)
        val g = android.graphics.Color.green(rgb)
        val b = android.graphics.Color.blue(rgb)
        hexInput = String.format("#%02X%02X%02X", r, g, b)
    }

    fun updateFromHex(hex: String) {
        try {
            val parsed = android.graphics.Color.parseColor(hex)
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(parsed, hsv)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
        } catch (e: Exception) {
            // Invalid hex
        }
    }

    val currentColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Color Preview Box & Current Hex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = hexInput.uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hue: ${hue.toInt()}° | Sat: ${(saturation * 100).toInt()}% | Val: ${(value * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Square 2D Color Picker (Saturation & Value)
                Text(
                    text = "Square Color Palette (Tap or Drag)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .pointerInput(hue) {
                            detectTapGestures { offset ->
                                saturation = (offset.x / size.width).coerceIn(0f, 1f)
                                value = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                                updateFromHsv(hue, saturation, value)
                            }
                        }
                        .pointerInput(hue) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                value = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                                updateFromHsv(hue, saturation, value)
                            }
                        }
                ) {
                    val baseHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 1. Base Hue Color fill
                        drawRect(color = baseHueColor)

                        // 2. Horizontal White to Transparent gradient (Saturation)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.White, Color.Transparent)
                            )
                        )

                        // 3. Vertical Transparent to Black gradient (Value / Brightness)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black)
                            )
                        )

                        // 4. Selector Target Circle
                        val selectorX = saturation * size.width
                        val selectorY = (1f - value) * size.height

                        drawCircle(
                            color = Color.White,
                            radius = 9.dp.toPx(),
                            center = Offset(selectorX, selectorY)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 7.dp.toPx(),
                            center = Offset(selectorX, selectorY)
                        )
                        drawCircle(
                            color = currentColor,
                            radius = 5.dp.toPx(),
                            center = Offset(selectorX, selectorY)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rainbow Hue Bar
                Text(
                    text = "Hue Slider",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Visual rainbow track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Red,
                                    Color.Yellow,
                                    Color.Green,
                                    Color.Cyan,
                                    Color.Blue,
                                    Color.Magenta,
                                    Color.Red
                                )
                            )
                        )
                )

                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        updateFromHsv(hue, saturation, value)
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = currentColor,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                                    width = if (hexInput.equals(hex, ignoreCase = true)) 2.5.dp else 1.dp,
                                    color = if (hexInput.equals(hex, ignoreCase = true)) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    hexInput = hex
                                    updateFromHex(hex)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hex Input Field
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        if (input.startsWith("#") && (input.length == 7 || input.length == 9)) {
                            updateFromHex(input)
                        }
                    },
                    label = { Text("Hex Code (#RRGGBB)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(hexInput.uppercase())
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
