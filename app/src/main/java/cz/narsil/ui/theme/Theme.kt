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
    // Koncept E (334) je jen tmavý; světlé schéma zůstává pro náhledy.
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NarsilDarkColorScheme else NarsilLightColorScheme
    val extendedColors = if (darkTheme) DarkNarsilExtendedColors else LightNarsilExtendedColors

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
