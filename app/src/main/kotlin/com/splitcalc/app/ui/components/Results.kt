package com.splitcalc.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitcalc.app.ui.money
import com.splitcalc.app.ui.percentLabel
import com.splitcalc.app.ui.theme.SplitTheme
import com.splitcalc.core.RowResult
import com.splitcalc.core.Totals

@Composable
private fun ResultSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SplitTheme.colors.border),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
fun ResultCard(row: RowResult, modifier: Modifier = Modifier) {
    ResultSurface(modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.ticker, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "${row.percent.percentLabel()} of budget",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("Slice", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = row.slice.money(),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = SplitTheme.colors.amount,
        )
        Text(
            text = "Order ${row.orderAmount.money()}  |  fees ${row.totalFee.money()} " +
                "(${row.tradingFee.money()} + ${row.regulatoryFee.money()} reg)  |  left ${row.leftover.money()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun TotalsCard(totals: Totals, warnings: List<String>, modifier: Modifier = Modifier) {
    ResultSurface(modifier) {
        Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SummaryLine("Invested", totals.totalInvested.money())
        SummaryLine("Fees", "${totals.totalFees.money()} (${totals.feePercentOfBudget.toPlainString()}% of budget)")
        SummaryLine("Left over", totals.totalLeftover.money())
        warnings.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
