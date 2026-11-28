package cz.narsil.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme

/**
 * Unified rounded progress bar shared across XP, steps, squats, badges, and party goals.
 */
@Composable
fun NarsilProgressBar(
    progress: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = NarsilTheme.colors.xpTrack,
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
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(fillColor)
        )
    }
}

@Preview(name = "NarsilProgressBar – Světlý", showBackground = true)
@Composable
private fun NarsilProgressBarLightPreview() {
    NarsilTheme(darkTheme = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            NarsilProgressBar(
                progress = 0.68f,
                fillColor = NarsilTheme.colors.ember
            )
        }
    }
}

@Preview(
    name = "NarsilProgressBar – Tmavý",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun NarsilProgressBarDarkPreview() {
    NarsilTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            NarsilProgressBar(
                progress = 0.68f,
                fillColor = NarsilTheme.colors.positive
            )
        }
    }
}
