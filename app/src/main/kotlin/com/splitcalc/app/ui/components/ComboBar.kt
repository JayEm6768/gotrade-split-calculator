package com.splitcalc.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.splitcalc.app.Limits
import com.splitcalc.app.SavedCombo
import com.splitcalc.app.ui.theme.SplitTheme

/** Saved ticker combos: tap to fill the rows, long-press to delete, or save the current rows. */
@Composable
fun ComboBar(
    combos: List<SavedCombo>,
    canSave: Boolean,
    defaultName: String,
    onApply: (String) -> Unit,
    onSave: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSave by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Saved combos",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(combos, key = { it.name }) { combo ->
                ComboChip(label = combo.name, onClick = { onApply(combo.name) }, onLongClick = { pendingDelete = combo.name })
            }
            item { ComboChip(label = "+ Save current", emphasized = true, enabled = canSave, onClick = { showSave = true }) }
        }
    }

    if (showSave) {
        SaveComboDialog(
            defaultName = defaultName,
            onConfirm = {
                onSave(it)
                showSave = false
            },
            onDismiss = { showSave = false },
        )
    }
    pendingDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete combo $name?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(name)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            shape = MaterialTheme.shapes.large,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComboChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    val colors = SplitTheme.colors
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (emphasized) colors.tint else colors.container,
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, if (emphasized) scheme.primary else colors.border),
        modifier = modifier.alpha(if (enabled) 1f else 0.4f),
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                .combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SaveComboDialog(defaultName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save combo") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it.take(Limits.MAX_COMBO_NAME_CHARS).filter { c -> c.isLetterOrDigit() || c in " -_/" } },
                singleLine = true,
                placeholder = { Text("Name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = MaterialTheme.shapes.large,
    )
}
