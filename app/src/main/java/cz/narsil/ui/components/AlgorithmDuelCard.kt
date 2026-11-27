package cz.narsil.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme

/**
 * Daily score card comparing minutes reclaimed by the user vs. minutes spent in passive feeds.
 */
@Composable
fun AlgorithmDuelCard(
    reclaimedMinutes: Int,
    feedMinutes: Int,
    wonDaysCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    val safeReclaimed = reclaimedMinutes.coerceAtLeast(0)
    val safeFeed = feedMinutes.coerceAtLeast(0)
    val total = (safeReclaimed + safeFeed).coerceAtLeast(1)
    val reclaimedShare = if (safeReclaimed + safeFeed == 0) {
        0.5f
    } else {
        (safeReclaimed.toFloat() / total.toFloat()).coerceIn(0.08f, 0.92f)
    }

    val statusText = when {
        safeReclaimed > safeFeed -> "Dnes vedeš"
        safeReclaimed == safeFeed -> "Vyrovnaný souboj"
        else -> "Algoritmus má náskok"
    }
    val statusContainer = if (safeReclaimed >= safeFeed) {
        colors.positiveContainer
    } else {
        colors.echoContainer
    }
    val statusContent = if (safeReclaimed >= safeFeed) {
        colors.onPositiveContainer
    } else {
        colors.onEchoContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Souboj s algoritmem",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = statusContainer,
                    contentColor = statusContent
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$safeReclaimed min",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.positive
                    )
                    Text(
                        text = "Získaný čas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$safeFeed min",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.algorithm
                    )
                    Text(
                        text = "Čas ve feedu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(colors.algorithmContainer)
            ) {
                Box(
                    modifier = Modifier
                        .weight(reclaimedShare)
                        .fillMaxHeight()
                        .background(colors.positive)
                )
                Box(
                    modifier = Modifier
                        .weight(1f - reclaimedShare)
                        .fillMaxHeight()
                        .background(colors.algorithm)
                )
            }

            Text(
                text = "Vyhrané dny celkem: $wonDaysCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "AlgorithmDuelCard – Světlý", showBackground = true)
@Composable
private fun AlgorithmDuelCardLightPreview() {
    NarsilTheme(darkTheme = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            AlgorithmDuelCard(
                reclaimedMinutes = 45,
                feedMinutes = 18,
                wonDaysCount = 12
            )
        }
    }
}

@Preview(
    name = "AlgorithmDuelCard – Tmavý",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AlgorithmDuelCardDarkPreview() {
    NarsilTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            AlgorithmDuelCard(
                reclaimedMinutes = 15,
                feedMinutes = 28,
                wonDaysCount = 12
            )
        }
    }
}
