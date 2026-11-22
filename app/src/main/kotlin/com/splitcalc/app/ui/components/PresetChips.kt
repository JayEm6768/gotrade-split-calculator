package com.splitcalc.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.splitcalc.app.PRESET_PERCENTS
import com.splitcalc.app.sanitizeDecimalInput
import com.splitcalc.app.ui.theme.SplitTheme
import java.math.BigDecimal

/** Preset percent chips applied to the selected ticker row, pinned to the bottom of the screen. */
@Composable
fun PresetChipBar(
    selectedPercent: BigDecimal?,
    canFillRest: Boolean,
    notice: String?,
    insetForNavigationBar: Boolean,
    onPreset: (Int) -> Unit,
    onRest: () -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.then(if (insetForNavigationBar) Modifier.navigationBarsPadding() else Modifier).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = notice ?: "Percent for the selected ticker",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (notice != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(PRESET_PERCENTS) { value ->
                    Chip(
                        label = "$value",
                        selected = selectedPercent?.compareTo(BigDecimal(value)) == 0,
                        onClick = { onPreset(value) },
                    )
                }
                item { Chip(label = "Rest", selected = false, enabled = canFillRest, emphasized = true, onClick = onRest) }
                item { Chip(label = "Custom", selected = false, emphasized = true, onClick = onCustom) }
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    val colors = SplitTheme.colors
    val scheme = MaterialTheme.colorScheme
    val container = when {
        selected -> scheme.primary
        emphasized -> colors.tint
        else -> colors.container
    }
    val content = if (selected) scheme.onPrimary else scheme.onSurface
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, if (emphasized || selected) scheme.primary else colors.border),
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun CustomPercentDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom percent") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = sanitizeDecimalInput(it, maxChars = 6) ?: text },
                singleLine = true,
                suffix = { Text("%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = MaterialTheme.shapes.large,
    )
}
