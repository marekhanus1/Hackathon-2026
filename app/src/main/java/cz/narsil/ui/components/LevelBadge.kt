package cz.narsil.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 * Compact medallion pill showing the user's current level and optional Czech rank title.
 */
@Composable
fun LevelBadge(
    level: Int,
    title: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = colors.emberContainer,
        contentColor = colors.onEmberContainer,
        border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(
                start = spacing.xs,
                end = spacing.sm,
                top = spacing.xxs,
                bottom = spacing.xxs
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colors.goldAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onEmber
                )
            }

            val label = if (title.isNullOrBlank()) {
                "Úroveň $level"
            } else {
                "Úroveň $level · $title"
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onEmberContainer
            )
        }
    }
}

@Preview(name = "LevelBadge – Světlý", showBackground = true)
@Composable
private fun LevelBadgeLightPreview() {
    NarsilTheme(darkTheme = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            LevelBadge(level = 4, title = "Kovář klidu")
        }
    }
}

@Preview(
    name = "LevelBadge – Tmavý",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun LevelBadgeDarkPreview() {
    NarsilTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            LevelBadge(level = 4, title = "Kovář klidu")
        }
    }
}
