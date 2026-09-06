package com.geotagcamera.geotagginglocationonphoto.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.geotagcamera.geotagginglocationonphoto.ads.RewardedAdManager
import com.geotagcamera.geotagginglocationonphoto.ads.RewardedUnlockDialog
import com.geotagcamera.geotagginglocationonphoto.stamp.StampFields
import com.geotagcamera.geotagginglocationonphoto.stamp.StampFont

private val SwatchColors = listOf(
    Color.White, Color(0xFFFFD54F), Color(0xFF56CB98), Color(0xFF6DB6FF), Color(0xFFE24947), Color.Black
)

/**
 * Tapping the geolocation stamp box (Capture live preview, or Settings)
 * opens this. Project name / text size / box size / font / colour are free.
 * "Modify date, time, day & location" stays behind a lock icon that opens
 * [RewardedUnlockDialog] — editing there is only possible once an ad has
 * just been watched (per dialog session; re-locks next time this opens).
 */
@Composable
fun PreviewEditDialog(
    fields: StampFields,
    adManager: RewardedAdManager,
    onDismiss: () -> Unit,
    onSave: (StampFields) -> Unit
) {
    var projectName by remember { mutableStateOf(fields.orgLabel) }
    var textScale by remember { mutableStateOf(fields.textScale) }
    var boxScale by remember { mutableStateOf(fields.boxScale) }
    var font by remember { mutableStateOf(fields.font) }
    var fontMenuOpen by remember { mutableStateOf(false) }
    var textColor by remember { mutableStateOf(fields.textColorArgb?.let { Color(it) }) }

    var unlocked by remember { mutableStateOf(false) }
    var showAdDialog by remember { mutableStateOf(false) }
    var customDateTime by remember { mutableStateOf(fields.customDateTimeText.orEmpty()) }
    var customLocation by remember { mutableStateOf(fields.customLocationText.orEmpty()) }

    if (showAdDialog) {
        RewardedUnlockDialog(
            adManager = adManager,
            onDismiss = { showAdDialog = false },
            onUnlocked = { showAdDialog = false; unlocked = true }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preview / Edit") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Project name", modifier = Modifier.padding(bottom = 4.dp))
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))
                Text("Text size — ${"%.1f".format(textScale)}x")
                Slider(value = textScale, onValueChange = { textScale = it }, valueRange = 0.6f..1.8f)

                Text("Box size — ${"%.1f".format(boxScale)}x")
                Slider(value = boxScale, onValueChange = { boxScale = it }, valueRange = 0.5f..1.6f)

                Spacer(Modifier.height(8.dp))
                Text("Font")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { fontMenuOpen = true }) { Text(font.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    DropdownMenu(expanded = fontMenuOpen, onDismissRequest = { fontMenuOpen = false }) {
                        StampFont.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { font = option; fontMenuOpen = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Text colour")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                    SwatchColors.forEach { swatch ->
                        ColorSwatch(color = swatch, selected = textColor == swatch, onClick = { textColor = swatch })
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!unlocked) Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Modify date, time, day & location", style = MaterialTheme.typography.titleSmall)
                }
                if (!unlocked) {
                    TextButton(onClick = { showAdDialog = true }) { Text("Watch ad to unlock") }
                } else {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customDateTime,
                        onValueChange = { customDateTime = it },
                        label = { Text("Date, time & day (blank = automatic)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customLocation,
                        onValueChange = { customLocation = it },
                        label = { Text("Location (blank = automatic)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    fields.copy(
                        orgLabel = projectName,
                        textScale = textScale,
                        boxScale = boxScale,
                        font = font,
                        textColorArgb = textColor?.toArgb()?.toLong()?.and(0xFFFFFFFFL),
                        customDateTimeText = if (unlocked) customDateTime.ifBlank { null } else fields.customDateTimeText,
                        customLocationText = if (unlocked) customLocation.ifBlank { null } else fields.customLocationText
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) Modifier.border(2.dp, Color(0xFF6DB6FF), CircleShape)
                else Modifier.border(1.dp, Color(0x33000000), CircleShape)
            )
            .clickable(onClick = onClick)
    )
}
