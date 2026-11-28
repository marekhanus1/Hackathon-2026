package cz.narsil.ui.screens.rating

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme

/**
 * Spodní panel „Co ti to video dalo?“ (156): pět odpovědí, jedno ťuknutí. Po odpovědi jen poděkuje.
 * Neblokuje: okno nad YouTube je malé a samo zmizí (overlay), „Teď ne“ ho zavře hned.
 */
@Composable
fun VideoRatingSheet(
    state: VideoRatingUiState,
    onAnswer: (VideoValue) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 4.dp,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.question,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (state.earnedXp > 0) {
                    Surface(shape = CircleShape, color = colors.positiveContainer, contentColor = colors.onPositiveContainer) {
                        Text(
                            text = "+${state.earnedXp} XP",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs),
                        )
                    }
                }
            }
            Text(
                text = state.videoTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val answered = state.answered
            if (answered != null) {
                Text(
                    text = "${answered.emoji} Díky. Zapsáno do tvé mapy hodnoty.",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = spacing.sm),
                )
            } else {
                VideoValue.entries.forEach { value ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onAnswer(value) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = value.emoji, style = MaterialTheme.typography.titleMedium)
                            Text(text = value.label, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Teď ne")
                }
            }
        }
    }
}

@Preview(name = "Co ti video dalo – Světlý", showBackground = true)
@Composable
private fun VideoRatingLightPreview() {
    NarsilTheme(darkTheme = false) {
        VideoRatingSheet(state = VideoRatingUiState(), onAnswer = {}, onDismiss = {})
    }
}

@Preview(name = "Co ti video dalo – Tmavý, odpovězeno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VideoRatingDarkPreview() {
    NarsilTheme(darkTheme = true) {
        VideoRatingSheet(
            state = VideoRatingUiState(videoTitle = "10 minut feedu na TikToku", question = "Co ti daly ty minuty?", answered = VideoValue.NOTHING),
            onAnswer = {},
            onDismiss = {},
        )
    }
}
