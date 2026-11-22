package com.splitcalc.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.splitcalc.app.Limits
import com.splitcalc.app.plain
import com.splitcalc.app.ui.theme.SplitTheme
import java.math.BigDecimal

/** Shows the allocated share; red until it reaches exactly 100%. */
@Composable
fun TotalBar(allocated: BigDecimal, modifier: Modifier = Modifier) {
    val complete = allocated.compareTo(Limits.HUNDRED) == 0
    val accent = if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val fraction = (allocated.toFloat() / 100f).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Allocated", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${allocated.plain()} / 100%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (complete) MaterialTheme.colorScheme.onSurface else accent,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(SplitTheme.colors.border)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(CircleShape).background(accent))
        }
    }
}
