package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ColorOption(val hex: String?, val label: String, val color: Color)

val TEXT_COLORS = listOf(
    ColorOption(null, "Default", Color(0xFF0F172A)),
    ColorOption("#DC2626", "Red", Color(0xFFDC2626)),
    ColorOption("#D97706", "Amber", Color(0xFFD97706)),
    ColorOption("#059669", "Green", Color(0xFF059669)),
    ColorOption("#2563EB", "Blue", Color(0xFF2563EB)),
    ColorOption("#7C3AED", "Purple", Color(0xFF7C3AED)),
    ColorOption("#DB2777", "Pink", Color(0xFFDB2777))
)

val HIGHLIGHT_COLORS = listOf(
    ColorOption(null, "None", Color.Transparent),
    ColorOption("#FEF08A", "Yellow", Color(0xFFFEF08A)),
    ColorOption("#BBF7D0", "Green", Color(0xFFBBF7D0)),
    ColorOption("#BAE6FD", "Cyan", Color(0xFFBAE6FD)),
    ColorOption("#E9D5FF", "Lavender", Color(0xFFE9D5FF)),
    ColorOption("#FBCFE8", "Pink", Color(0xFFFBCFE8)),
    ColorOption("#FED7AA", "Orange", Color(0xFFFED7AA))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    currentTextColor: String?,
    currentBgColor: String?,
    onApplyColors: (textColor: String?, bgColor: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Item Styling",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Text Color Section
            Text(
                text = "Text Color",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TEXT_COLORS.forEach { option ->
                    val isSelected = currentTextColor == option.hex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                                shape = CircleShape
                            )
                            .clickable {
                                onApplyColors(option.hex, currentBgColor)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (option.hex == null || option.hex == "#0F172A") Color.White else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Highlight Background Section
            Text(
                text = "Highlight Color",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HIGHLIGHT_COLORS.forEach { option ->
                    val isSelected = currentBgColor == option.hex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (option.hex == null) MaterialTheme.colorScheme.surfaceVariant else option.color)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onApplyColors(currentTextColor, option.hex)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (option.hex == null) {
                            Icon(
                                imageVector = Icons.Default.FormatColorReset,
                                contentDescription = "No highlight",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    onApplyColors(null, null)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.FormatColorReset,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Reset All Colors")
            }
        }
    }
}
