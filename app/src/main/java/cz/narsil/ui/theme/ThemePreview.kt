package cz.narsil.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
private fun NarsilThemeShowcase() {
    val spacing = NarsilTheme.spacing
    val extended = NarsilTheme.colors

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = "Narsil – Design systém",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Klidný, osobní a povzbuzující tón. Pomocník, ne dozorce.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Calm Ozvěna tone preview card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = extended.echoContainer,
                    contentColor = extended.onEchoContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = "Ozvěna",
                        style = MaterialTheme.typography.labelLarge,
                        color = extended.echo
                    )
                    Text(
                        text = "Čemu se teď vyhýbáš?",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Včera večer sis přál mít na zítřek víc klidu. Stačí zavřít feed a získáš +50 XP.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(extended.positive)
                            .padding(vertical = spacing.sm, horizontal = spacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Zavřít (+50 XP)",
                            style = MaterialTheme.typography.labelLarge,
                            color = extended.onPositive
                        )
                    }
                }
            }

            // Semantic color swatches
            Text(
                text = "Sémantické barvy",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                SwatchTile("Pozitivní", extended.positive, extended.onPositive, Modifier.weight(1f))
                SwatchTile("Jiskra", extended.ember, extended.onEmber, Modifier.weight(1f))
                SwatchTile("Ozvěna", extended.echo, extended.onEcho, Modifier.weight(1f))
                SwatchTile("Dluh", extended.debt, extended.onDebt, Modifier.weight(1f))
            }

            // Typography & shape showcase
            Text(
                text = "Typografie a tvary",
                style = MaterialTheme.typography.titleMedium
            )
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(text = "20 dřepů", style = MaterialTheme.typography.displayLarge)
                    Text(text = "Nadpis obrazovky", style = MaterialTheme.typography.headlineMedium)
                    Text(text = "Podnadpis karty", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Český text s diakritikou: Příliš žluťoučký kůň úpěl ďábelské ódy.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        ShapeChip("XS", MaterialTheme.shapes.extraSmall)
                        ShapeChip("S", MaterialTheme.shapes.small)
                        ShapeChip("M", MaterialTheme.shapes.medium)
                        ShapeChip("L", MaterialTheme.shapes.large)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwatchTile(
    label: String,
    color: Color,
    onColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(MaterialTheme.shapes.small)
            .background(color)
            .padding(NarsilTheme.spacing.xs),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = onColor
        )
    }
}

@Composable
private fun ShapeChip(
    label: String,
    shape: androidx.compose.ui.graphics.Shape
) {
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 32.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Preview(name = "Narsil – Světlý režim", showBackground = true)
@Composable
private fun NarsilLightPreview() {
    NarsilTheme(darkTheme = false) {
        NarsilThemeShowcase()
    }
}

@Preview(
    name = "Narsil – Tmavý režim",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun NarsilDarkPreview() {
    NarsilTheme(darkTheme = true) {
        NarsilThemeShowcase()
    }
}
