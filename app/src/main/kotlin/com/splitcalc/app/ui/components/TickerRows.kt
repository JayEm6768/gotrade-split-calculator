package com.splitcalc.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.splitcalc.app.TickerRow
import com.splitcalc.app.ui.percentLabel
import com.splitcalc.app.ui.theme.SplitTheme

@Composable
fun TickerRowItem(
    row: TickerRow,
    selected: Boolean,
    onSelect: () -> Unit,
    onTickerChange: (String) -> Unit,
    onRemove: () -> Unit,
    requestFocus: Boolean,
    onFocusHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SplitTheme.colors
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            withFrameNanos { }
            focusRequester.requestFocus()
            onFocusHandled()
        }
    }
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else colors.border
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = if (selected) colors.tint else MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = row.ticker,
                onValueChange = onTickerChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester).onFocusChanged { if (it.isFocused) onSelect() },
                singleLine = true,
                placeholder = { Text("Ticker") },
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = fieldColors(container = Color.Transparent),
            )
            PercentPill(text = row.percent?.percentLabel() ?: "--", filled = row.percent != null)
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove ${row.ticker.ifEmpty { "ticker" }}")
            }
        }
    }
}

@Composable
private fun PercentPill(text: String, filled: Boolean) {
    val background = if (filled) MaterialTheme.colorScheme.primary else SplitTheme.colors.container
    val content = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = text,
        modifier = Modifier.background(background, CircleShape).padding(horizontal = 14.dp, vertical = 6.dp),
        color = content,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
fun AddTickerButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("Add ticker", modifier = Modifier.padding(start = 6.dp))
    }
}
