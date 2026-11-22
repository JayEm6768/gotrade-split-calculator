package com.splitcalc.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Colors that Material 3 has no slot for. */
@Immutable
data class SplitColors(
    val amount: Color,
    val tint: Color,
    val border: Color,
    val container: Color,
)

val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Mint,
    onPrimary = Ink,
    primaryContainer = MintTint,
    onPrimaryContainer = Ink,
    secondary = ForestGreen,
    onSecondary = Color.White,
    background = Cloud,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Cloud,
    onSurfaceVariant = Slate,
    outline = Border,
    outlineVariant = Border,
    error = ErrorRed,
    onError = Color.White,
)

val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Mint,
    onPrimary = NightBackground,
    primaryContainer = NightSurface,
    onPrimaryContainer = NightText,
    secondary = DeepGreen,
    onSecondary = NightBackground,
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightContainer,
    onSurfaceVariant = Slate,
    outline = NightBorder,
    outlineVariant = NightBorder,
    error = ErrorRed,
    onError = Color.White,
)

private val LightSplitColors = SplitColors(amount = ForestGreen, tint = MintTint, border = Border, container = Cloud)
private val DarkSplitColors = SplitColors(amount = Mint, tint = NightBackground, border = NightBorder, container = NightContainer)

private val LocalSplitColors = staticCompositionLocalOf { LightSplitColors }

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

object SplitTheme {
    val colors: SplitColors
        @Composable @ReadOnlyComposable get() = LocalSplitColors.current
}

@Composable
fun SplitCalculatorTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSplitColors provides if (darkTheme) DarkSplitColors else LightSplitColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
