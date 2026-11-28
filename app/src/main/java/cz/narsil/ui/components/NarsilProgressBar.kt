package cz.narsil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.NarsilTheme

/** Průsvitná drážka (`.progress`), když volající nedá vlastní. */
private val AeroTrack = Color(0x24005B96)

/**
 * Společný ukazatel postupu (XP, kroky, dřepy, odznaky, cíl party) ve stylu Frutiger Aero:
 * průsvitná drážka a lesklá výplň (světlejší nahoře, bílý odlesk).
 */
@Composable
fun NarsilProgressBar(
    progress: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = AeroTrack,
    height: Dp = 10.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(lerp(fillColor, Color.White, 0.45f), fillColor)))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(height * 0.35f)
                        .background(Color(0x99FFFFFF), CircleShape)
                )
            }
        }
    }
}

@Preview(name = "NarsilProgressBar – Aero", widthDp = 360, heightDp = 80)
@Composable
private fun NarsilProgressBarPreview() {
    NarsilTheme {
        AeroBackdrop {
            NeuPanel(Modifier.padding(16.dp)) {
                NarsilProgressBar(progress = 0.68f, fillColor = Color(0xFF22AF42))
            }
        }
    }
}
