package cz.narsil.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.components.AlgorithmDuelCard
import cz.narsil.ui.components.AvatarCard
import cz.narsil.ui.components.LevelBadge
import cz.narsil.ui.components.NarsilProgressBar
import cz.narsil.ui.components.StatusPill
import cz.narsil.ui.components.StreakJokerRow
import cz.narsil.ui.components.XpProgressBar
import cz.narsil.ui.theme.NarsilTheme

/**
 * Main dashboard screen for Narsil using ViewModel StateFlow<HomeUiState> / fake data.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenContract: () -> Unit,
    onOpenMirror: () -> Unit = {},
    onOpenBadges: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Narsil",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                ZoneStatusPill(status = state.zoneStatus)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LevelBadge(level = state.level, title = state.levelTitle)
                StatusPill(
                    text = "Odznaky",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onOpenBadges
                )
            }

            XpProgressBar(currentXp = state.currentXp)

            AvatarCard(
                state = state.avatarState,
                summary7Days = state.avatarSummary7Days
            )

            AlgorithmDuelCard(
                reclaimedMinutes = state.reclaimedMinutesToday,
                feedMinutes = state.feedMinutesToday,
                wonDaysCount = state.wonDaysTotal
            )

            StreakJokerRow(
                streakDays = state.streakDays,
                availableJokers = state.availableJokers
            )

            // Movement & gentle debt summary card
            MovementBalanceCard(state = state)

            // Evening contract card
            ContractStatusCard(
                state = state,
                onOpenContract = onOpenContract,
                onOpenMirror = onOpenMirror
            )
        }
    }
}

@Composable
private fun ZoneStatusPill(status: ZoneStatus) {
    val colors = NarsilTheme.colors

    val containerColor = when (status) {
        ZoneStatus.HOME -> colors.echoContainer
        ZoneStatus.AWAY -> colors.positiveContainer
        ZoneStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (status) {
        ZoneStatus.HOME -> colors.onEchoContainer
        ZoneStatus.AWAY -> colors.onPositiveContainer
        ZoneStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    StatusPill(
        text = status.label,
        containerColor = containerColor,
        contentColor = contentColor
    )
}

@Composable
private fun MovementBalanceCard(state: HomeUiState) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    val safeGoal = state.stepGoal.coerceAtLeast(1)
    val fraction = (state.stepsToday.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "Pohyb a rozpočet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "K dispozici ${state.availableStepMinutes} min",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.ember
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${state.stepsToday} / ${state.stepGoal} kroků",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (state.debtMinutes > 0) {
                    Text(
                        text = "Dluh do zítřka: ${state.debtMinutes} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.debt
                    )
                } else {
                    Text(
                        text = "Bez dluhu",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.positive
                    )
                }
            }

            NarsilProgressBar(
                progress = fraction,
                fillColor = colors.ember,
                height = 10.dp
            )
        }
    }
}

@Composable
private fun ContractStatusCard(
    state: HomeUiState,
    onOpenContract: () -> Unit,
    onOpenMirror: () -> Unit
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (state.contractCompletedToday) {
                colors.positiveContainer
            } else {
                colors.echoContainer
            },
            contentColor = if (state.contractCompletedToday) {
                colors.onPositiveContainer
            } else {
                colors.onEchoContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Text(
                text = if (state.contractCompletedToday) {
                    "Večerní smlouva je podepsaná (+100 XP)"
                } else {
                    "Večerní smlouva čeká (2 minuty)"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Plánovaný obsah: ${state.plannedContentSummary}",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Button(
                    onClick = onOpenContract,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.contractCompletedToday) colors.positive else colors.echo,
                        contentColor = if (state.contractCompletedToday) colors.onPositive else colors.onEcho
                    )
                ) {
                    Text(
                        text = if (state.contractCompletedToday) {
                            "Upravit dnešní plán"
                        } else {
                            "Uzavřít večerní smlouvu"
                        }
                    )
                }

                Surface(
                    onClick = onOpenMirror,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "Zrcadlo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
                    )
                }
            }
        }
    }
}

@Preview(name = "Domů – Světlý režim", showBackground = true)
@Composable
private fun HomeScreenLightPreview() {
    NarsilTheme(darkTheme = false) {
        HomeScreen(
            state = HomeUiState(),
            onOpenContract = {}
        )
    }
}

@Preview(
    name = "Domů – Tmavý režim",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HomeScreenDarkPreview() {
    NarsilTheme(darkTheme = true) {
        HomeScreen(
            state = HomeUiState(
                contractCompletedToday = true,
                debtMinutes = 10,
                zoneStatus = ZoneStatus.AWAY
            ),
            onOpenContract = {}
        )
    }
}
