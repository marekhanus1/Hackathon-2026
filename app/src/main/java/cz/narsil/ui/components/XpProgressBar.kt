package cz.narsil.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors

/**
 * XP v rámci aktuální úrovně (výchozí 500 XP na úroveň): modré číslo, tlumený zbytek a zelený lesk.
 * Barvy textu se řídí podkladem ([OnSky] / [OnGlass]).
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuText("$xpInLevel / $safeThreshold XP", size = 13.sp, weight = FontWeight.Bold, color = NeuColors.Accent, maxLines = 1)
            NeuText("Zbývá $remainingXp XP do další úrovně", size = 11.sp, weight = FontWeight.Medium, color = NeuColors.Muted, maxLines = 1)
        }

        NarsilProgressBar(
            progress = animatedFraction,
            fillColor = Color(0xFF22AF42),
            height = 12.dp
        )
    }
}

@Preview(name = "XpProgressBar – Aero", widthDp = 360, heightDp = 100)
@Composable
private fun XpProgressBarPreview() {
    NarsilTheme {
        AeroBackdrop {
            NeuPanel(Modifier.padding(16.dp)) { XpProgressBar(currentXp = 1360) }
        }
    }
}
