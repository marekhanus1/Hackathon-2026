package cz.narsil.ui.screens.rating

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme

/**
 * Bottom-sheet panel for rating a watched YouTube video (intention + 1–5 value).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoRatingSheet(
    state: VideoRatingUiState,
    onSelectIntention: (WatchIntention) -> Unit,
    onSelectRating: (Int) -> Unit,
    onSubmitRating: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    val ratingCaption = when (state.selectedRating) {
        1 -> "1 · Spíš ztráta času"
        2 -> "2 · Slabší přínos"
        3 -> "3 · Průměrné video"
        4 -> "4 · Přínosné a hodnotné"
        5 -> "5 · Skvělá hodnota"
        else -> "Vyber hodnocení 1–5"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jakou hodnotu video mělo?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = CircleShape,
                    color = colors.positiveContainer,
                    contentColor = colors.onPositiveContainer
                ) {
                    Text(
                        text = "+${state.earnedXp} XP",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    Text(
                        text = state.videoTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = state.channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = "Záměr před sledováním",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    WatchIntention.entries.forEach { intention ->
                        FilterChip(
                            selected = state.selectedIntention == intention,
                            onClick = { onSelectIntention(intention) },
                            label = { Text(intention.label) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    (1..5).forEach { score ->
                        val selected = state.selectedRating == score
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onSelectRating(score) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) colors.ember else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) colors.onEmber else MaterialTheme.colorScheme.onSurface,
                            border = if (selected) BorderStroke(1.dp, colors.goldAccent) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = score.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Text(
                    text = ratingCaption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Přeskočit")
                }
                Button(
                    onClick = onSubmitRating,
                    enabled = state.canSubmit,
                    modifier = Modifier.weight(1.8f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.positive,
                        contentColor = colors.onPositive
                    )
                ) {
                    Text("Uložit hodnocení (+${state.earnedXp} XP)")
                }
            }
        }
    }
}

@Preview(name = "Hodnocení videa – Světlý", showBackground = true)
@Composable
private fun VideoRatingLightPreview() {
    NarsilTheme(darkTheme = false) {
        VideoRatingSheet(
            state = VideoRatingUiState(),
            onSelectIntention = {},
            onSelectRating = {},
            onSubmitRating = {},
            onDismiss = {}
        )
    }
}

@Preview(
    name = "Hodnocení videa – Tmavý",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun VideoRatingDarkPreview() {
    NarsilTheme(darkTheme = true) {
        VideoRatingSheet(
            state = VideoRatingUiState(
                videoTitle = "Top 10 funny moments kompilace",
                channelName = "Daily Shorts",
                selectedIntention = WatchIntention.AIMLESS,
                selectedRating = 1
            ),
            onSelectIntention = {},
            onSelectRating = {},
            onSubmitRating = {},
            onDismiss = {}
        )
    }
}
