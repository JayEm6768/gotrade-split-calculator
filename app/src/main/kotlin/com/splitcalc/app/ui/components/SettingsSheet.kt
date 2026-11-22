package com.splitcalc.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.splitcalc.app.parseFees
import com.splitcalc.app.plain
import com.splitcalc.app.sanitizeDecimalInput
import com.splitcalc.core.FeeSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(fees: FeeSettings, onSave: (FeeSettings) -> Unit, onDismiss: () -> Unit) {
    var rate by remember { mutableStateOf(fees.tradingRatePercent.plain()) }
    var minimum by remember { mutableStateOf(fees.minTradingFee.plain()) }
    var regulatory by remember { mutableStateOf(fees.regulatoryFee.plain()) }
    val parsed = parseFees(rate, minimum, regulatory)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Fee settings", style = MaterialTheme.typography.titleLarge)
            FeeField("Trading fee", rate, "%") { rate = it }
            FeeField("Minimum fee per order", minimum, "$") { minimum = it }
            FeeField("Regulatory fee per order", regulatory, "$") { regulatory = it }
            if (parsed == null) {
                Text("Check the values: rate must be below 100.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val defaults = FeeSettings()
                        rate = defaults.tradingRatePercent.plain()
                        minimum = defaults.minTradingFee.plain()
                        regulatory = defaults.regulatoryFee.plain()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Reset") }
                Button(
                    onClick = { parsed?.let(onSave); onDismiss() },
                    enabled = parsed != null,
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun FeeField(label: String, value: String, unit: String, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { onChange(sanitizeDecimalInput(it) ?: value) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        suffix = if (unit == "%") ({ Text(unit) }) else null,
        prefix = if (unit == "$") ({ Text(unit) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.medium,
        colors = fieldColors(),
    )
}
