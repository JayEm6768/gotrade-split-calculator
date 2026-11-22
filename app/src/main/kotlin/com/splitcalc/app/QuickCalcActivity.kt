package com.splitcalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.splitcalc.app.ui.CalculatorScreen
import com.splitcalc.app.ui.theme.SplitCalculatorTheme

/** Popup version of the calculator opened from the home-screen widget. */
class QuickCalcActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels { CalculatorViewModel.factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitCalculatorTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SCRIM)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() }
                        .safeDrawingPadding()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(CARD_HEIGHT_FRACTION)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        CalculatorScreen(viewModel, onClose = ::finish, insetBottomBar = false)
                    }
                }
            }
        }
    }

    private companion object {
        val SCRIM = Color(0x99000000)
        const val CARD_HEIGHT_FRACTION = 0.9f
    }
}
