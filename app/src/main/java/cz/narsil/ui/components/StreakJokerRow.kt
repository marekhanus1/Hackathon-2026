package cz.narsil.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
 * Displays the user's completed contract streak and available joker shields (earned via 50 extra squats).
 */
@Composable
fun StreakJokerRow(
    streakDays: Int,
    availableJokers: Int,
    maxJokers: Int = 3,
    modifier: Modifier = Modifier
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    val safeDays = streakDays.coerceAtLeast(0)
    val safeMax = maxJokers.coerceAtLeast(1)
    val safeJokers = availableJokers.coerceIn(0, safeMax)

    val dayUnit = when {
        safeDays == 1 -> "den"
        safeDays in 2..4 -> "dny"
        else -> "dní"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                Text(
                    text = "Série: $safeDays $dayUnit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Žolík (50 dřepů navíc) chrání sérii při relapsu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(safeMax) { index ->
                    val isActive = index < safeJokers
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) colors.emberContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 1.dp,
                                color = if (isActive) colors.goldAccent else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isActive) "Ž" else "·",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) {
                                colors.onEmberContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "StreakJokerRow – Světlý", showBackground = true)
@Composable
private fun StreakJokerRowLightPreview() {
    NarsilTheme(darkTheme = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StreakJokerRow(streakDays = 6, availableJokers = 2)
        }
    }
}

@Preview(
    name = "StreakJokerRow – Tmavý",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun StreakJokerRowDarkPreview() {
    NarsilTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StreakJokerRow(streakDays = 1, availableJokers = 1)
        }
    }
}
