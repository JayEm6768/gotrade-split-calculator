package com.splitcalc.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider as dayNightColor
import androidx.glance.unit.ColorProvider
import com.splitcalc.app.CalculatorState
import com.splitcalc.app.Outcome
import com.splitcalc.app.QuickCalcActivity
import com.splitcalc.app.data.SplitRepository
import com.splitcalc.app.ui.money
import com.splitcalc.app.ui.percentLabel
import com.splitcalc.app.ui.theme.DarkColorScheme
import com.splitcalc.app.ui.theme.ForestGreen
import com.splitcalc.app.ui.theme.LightColorScheme
import com.splitcalc.app.ui.theme.Mint

private val WidgetColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)
private const val MAX_ORDER_LINES = 6

/** Home-screen widget showing the saved split and, when complete, the order amounts. */
class SplitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = CalculatorState.from(SplitRepository.get(context).load())
        provideContent {
            GlanceTheme(colors = WidgetColors) { WidgetContent(state) }
        }
    }
}

@Composable
private fun WidgetContent(state: CalculatorState) {
    val rows = state.rows.filter { it.ticker.isNotEmpty() && it.percent != null }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<QuickCalcActivity>()),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Split",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            state.amount?.let { amount ->
                Text(text = amount.money(), style = TextStyle(color = AmountColor, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        if (rows.isEmpty()) {
            Text(
                text = "Set up your split",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
            )
            return@Column
        }
        Text(
            text = rows.joinToString("  ") { "${it.ticker} ${it.percent!!.percentLabel()}" },
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            maxLines = 2,
        )
        val outcome = state.outcome
        if (outcome is Outcome.Ready) {
            Spacer(GlanceModifier.height(8.dp))
            outcome.result.rows.take(MAX_ORDER_LINES).forEach { row ->
                Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text(
                        text = row.ticker,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(text = row.orderAmount.money(), style = TextStyle(color = AmountColor, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
            }
        } else if (outcome is Outcome.Blocked && state.amount == null) {
            Spacer(GlanceModifier.height(6.dp))
            Text(text = "Tap to enter an amount", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
        }
    }
}

private val AmountColor: ColorProvider = dayNightColor(day = ForestGreen, night = Mint)
