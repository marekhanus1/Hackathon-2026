package cz.narsil.ui.screens.echo

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.components.ChoiceCard
import cz.narsil.ui.components.ChoiceCardTone
import cz.narsil.ui.theme.NarsilTheme

/**
 * Ozvěna intervention overlay UI.
 * Video playback is injected via [videoSlot] from the overlay service (task 113).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EchoScreen(
    state: EchoUiState,
    onSelectFeeling: (EchoFeeling) -> Unit,
    onCloseFeed: () -> Unit,
    onStartSquats: () -> Unit,
    onPayWithSteps: () -> Unit,
    onBorrow: () -> Unit,
    modifier: Modifier = Modifier,
    videoSlot: @Composable () -> Unit = { DefaultEchoVideoPlaceholder(state) }
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    val canPayWithSteps = state.availableSteps >= state.stepCostForTenMin

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.echoContainer,
                    contentColor = colors.onEchoContainer
                ) {
                    Text(
                        text = "Ozvěna · ${state.sourceAppName}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                    )
                }

                if (state.currentDebtMinutes > 0) {
                    Surface(
                        shape = CircleShape,
                        color = colors.debtContainer,
                        contentColor = colors.onDebtContainer
                    ) {
                        Text(
                            text = "Dluh: ${state.currentDebtMinutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                        )
                    }
                }
            }

            // Dedicated slot for ExoPlayer evening reflection video
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                shape = MaterialTheme.shapes.large,
                color = colors.echoContainer,
                border = BorderStroke(1.dp, colors.echo.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    videoSlot()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = "Čemu se teď vyhýbáš?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Pojmenovat pocit často stačí, aby tlak povolil.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    EchoFeeling.entries.forEach { feeling ->
                        val selected = state.selectedFeeling == feeling
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectFeeling(feeling) },
                            label = {
                                Text(
                                    text = feeling.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.echoContainer,
                                selectedLabelColor = colors.onEchoContainer
                            )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                ChoiceCard(
                    title = "Zavřít feed",
                    subtitle = "Chuť opadla, vracím se k tomu, co má smysl",
                    badgeText = "+50 XP",
                    tone = ChoiceCardTone.POSITIVE,
                    onClick = onCloseFeed
                )

                ChoiceCard(
                    title = "Udělat 20 dřepů hned",
                    subtitle = "Kamera spočítá opakování a odemkne 10 min",
                    badgeText = "+20 XP",
                    tone = ChoiceCardTone.EMBER,
                    onClick = onStartSquats
                )

                ChoiceCard(
                    title = "Zaplatit kroky",
                    subtitle = if (canPayWithSteps) {
                        "${state.stepCostForTenMin} kroků za 10 min · máš ${state.availableSteps}"
                    } else {
                        "Potřebuješ ${state.stepCostForTenMin} kroků · zatím máš ${state.availableSteps}"
                    },
                    badgeText = "+10 XP",
                    tone = ChoiceCardTone.ECHO,
                    enabled = canPayWithSteps,
                    onClick = onPayWithSteps
                )

                ChoiceCard(
                    title = "Půjčit si 10 min",
                    subtitle = "Přičte se k zítřejšímu pohybovému cíli",
                    badgeText = "0 XP",
                    tone = ChoiceCardTone.DEBT,
                    onClick = onBorrow
                )
            }
        }
    }
}

@Composable
private fun DefaultEchoVideoPlaceholder(state: EchoUiState) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.echo),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (state.isVideoPlaying) "▶" else "❚❚",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onEcho
            )
        }
        Text(
            text = "Tvoje včerejší Ozvěna",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onEchoContainer,
            modifier = Modifier.padding(top = spacing.sm)
        )
        Text(
            text = state.videoCaption,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onEchoContainer.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xxs)
        )
    }
}

@Preview(name = "Ozvěna – Světlý režim", showBackground = true)
@Composable
private fun EchoScreenLightPreview() {
    NarsilTheme(darkTheme = false) {
        EchoScreen(
            state = EchoUiState(selectedFeeling = EchoFeeling.UNAVA),
            onSelectFeeling = {},
            onCloseFeed = {},
            onStartSquats = {},
            onPayWithSteps = {},
            onBorrow = {}
        )
    }
}

@Preview(
    name = "Ozvěna – Tmavý režim",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun EchoScreenDarkPreview() {
    NarsilTheme(darkTheme = true) {
        EchoScreen(
            state = EchoUiState(
                sourceAppName = "Instagram Reels",
                availableSteps = 620,
                currentDebtMinutes = 10
            ),
            onSelectFeeling = {},
            onCloseFeed = {},
            onStartSquats = {},
            onPayWithSteps = {},
            onBorrow = {}
        )
    }
}
