package cz.narsil.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme

/**
 * Displays XP progress within the current level (default 500 XP per level).
 */
@Composable
fun XpProgressBar(
    currentXp: Int,
    levelThresholdXp: Int = 500,
    modifier: Modifier = Modifier
) {
    val safeThreshold = levelThresholdXp.coerceAtLeast(1)
    val xpInLevel = (currentXp.coerceAtLeast(0)) % safeThreshold
    val remainingXp = safeThreshold - xpInLevel
    val targetFraction = (xpInLevel.toFloat() / safeThreshold.toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        label = "xpProgress"
    )

    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$xpInLevel / $safeThreshold XP",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Zbývá $remainingXp XP do další úrovně",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        NarsilProgressBar(
            progress = animatedFraction,
            fillColor = colors.xpFill,
            height = 12.dp
        )
    }
}

@Preview(name = "XpProgressBar – Světlý", showBackground = true)
@Composable
private fun XpProgressBarLightPreview() {
    NarsilTheme(darkTheme = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            XpProgressBar(currentXp = 1360)
        }
    }
}

@Preview(
    name = "XpProgressBar – Tmavý",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun XpProgressBarDarkPreview() {
    NarsilTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            XpProgressBar(currentXp = 1360)
        }
    }
}
