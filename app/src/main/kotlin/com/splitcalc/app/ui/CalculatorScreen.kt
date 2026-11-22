package com.splitcalc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitcalc.app.CalculatorViewModel
import com.splitcalc.app.Limits
import com.splitcalc.app.Outcome
import com.splitcalc.app.ui.components.AddTickerButton
import com.splitcalc.app.ui.components.AmountField
import com.splitcalc.app.ui.components.ComboBar
import com.splitcalc.app.ui.components.CustomPercentDialog
import com.splitcalc.app.ui.components.PresetChipBar
import com.splitcalc.app.ui.components.ResultCard
import com.splitcalc.app.ui.components.SettingsSheet
import com.splitcalc.app.ui.components.TickerRowItem
import com.splitcalc.app.ui.components.TotalBar
import com.splitcalc.app.ui.components.TotalsCard
import kotlinx.coroutines.delay

private const val NOTICE_MILLIS = 3_000L
private const val FIRST_ROW_ITEM = 2

/**
 * The calculator flow: amount, ticker rows, total bar, live results and the preset chip bar.
 * [onClose], when set, adds a close button (used by the quick-calc popup).
 */
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    insetBottomBar: Boolean = true,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.focusRequestId) {
        val id = state.focusRequestId ?: return@LaunchedEffect
        val index = state.rows.indexOfFirst { it.id == id }
        if (index >= 0) listState.animateScrollToItem(index + FIRST_ROW_ITEM)
    }

    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(NOTICE_MILLIS)
            viewModel.onDismissNotice()
        }
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Split Calculator", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Fee settings")
            }
            if (onClose != null) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
            }
        }

        if (state.loaded) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { AmountField(value = state.amountText, onValueChange = viewModel::onAmountChange) }
                item {
                    ComboBar(
                        combos = state.combos,
                        canSave = state.canSaveCombo,
                        defaultName = state.defaultComboName,
                        onApply = {
                            viewModel.onApplyCombo(it)
                            focusManager.clearFocus()
                        },
                        onSave = viewModel::onSaveCombo,
                        onDelete = viewModel::onDeleteCombo,
                    )
                }
                items(state.rows, key = { it.id }) { row ->
                    TickerRowItem(
                        row = row,
                        selected = row.id == state.selectedId,
                        onSelect = { viewModel.onSelect(row.id) },
                        onTickerChange = { viewModel.onTickerChange(row.id, it) },
                        onRemove = { viewModel.onRemoveRow(row.id) },
                        requestFocus = row.id == state.focusRequestId,
                        onFocusHandled = viewModel::onFocusHandled,
                    )
                }
                item { AddTickerButton(onClick = viewModel::onAddRow) }
                item { TotalBar(allocated = state.allocated) }
                when (val outcome = state.outcome) {
                    is Outcome.Blocked -> item {
                        val waitingForAmount = state.amount == null && state.allocated.compareTo(Limits.HUNDRED) == 0
                        Text(
                            text = outcome.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (waitingForAmount) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        )
                    }
                    is Outcome.Ready -> {
                        items(outcome.result.rows, key = { it.ticker }) { ResultCard(it) }
                        item { TotalsCard(outcome.result.totals, outcome.result.warnings) }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {}
        }

        PresetChipBar(
            selectedPercent = state.selectedRow?.percent,
            canFillRest = state.canFillRest,
            notice = state.notice,
            insetForNavigationBar = insetBottomBar,
            onPreset = {
                viewModel.onPreset(it)
                focusManager.clearFocus()
            },
            onRest = {
                viewModel.onRest()
                focusManager.clearFocus()
            },
            onCustom = { showCustom = true },
        )
    }

    if (showCustom) {
        CustomPercentDialog(
            onConfirm = {
                viewModel.onCustom(it)
                showCustom = false
                focusManager.clearFocus()
            },
            onDismiss = { showCustom = false },
        )
    }
    if (showSettings) {
        SettingsSheet(fees = state.fees, onSave = viewModel::onFeesChange, onDismiss = { showSettings = false })
    }
}
