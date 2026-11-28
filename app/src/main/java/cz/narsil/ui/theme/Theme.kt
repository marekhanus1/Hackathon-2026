package cz.narsil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Root Compose theme for Narsil providing Material 3 schemes,
 * extended semantic colors, typography, shapes, and spacing tokens.
 */
@Composable
fun NarsilTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Frutiger Aero (narsil-frontend-v2) je jen světlé; parametr zůstal kvůli volajícím.
    val colorScheme = AeroColorScheme
    val extendedColors = AeroNarsilExtendedColors

    CompositionLocalProvider(
        LocalNarsilColors provides extendedColors,
        LocalNarsilSpacing provides NarsilSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NarsilTypography,
            shapes = NarsilShapes,
            content = content
        )
    }
}

object NarsilTheme {
    val colors: NarsilExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNarsilColors.current

    val spacing: NarsilSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalNarsilSpacing.current
}
