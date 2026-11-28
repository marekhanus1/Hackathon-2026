package cz.narsil.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.components.NarsilProgressBar
import cz.narsil.ui.components.StatusPill
import cz.narsil.ui.components.XpProgressBar
import cz.narsil.ui.text.CzechPlural
import cz.narsil.ui.theme.NarsilTheme

/**
 * Flat home dashboard with one primary action and a short, scan-friendly daily summary.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenContract: () -> Unit,
    onOpenMirror: () -> Unit = {},
    onOpenBadges: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            HomeHeader(zoneStatus = state.zoneStatus)

            ContractAction(
                state = state,
                onOpenContract = onOpenContract
            )

            DailyOverview(state = state)
            MovementOverview(state = state)
            ProgressOverview(state = state)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                FlatNavigationAction(
                    label = "Zrcadlo",
                    supportingText = "Co ti bere čas",
                    onClick = onOpenMirror,
                    modifier = Modifier.weight(1f)
                )
                FlatNavigationAction(
                    label = "Odznaky",
                    supportingText = "Pokrok a výzvy",
                    onClick = onOpenBadges,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(zoneStatus: ZoneStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NarsilTheme.spacing.xxs)) {
            Text(
                text = "Narsil",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Dnešní přehled",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ZoneStatusPill(status = zoneStatus)
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
        text = when (status) {
            ZoneStatus.HOME -> "Ochrana aktivní"
            ZoneStatus.AWAY -> "Narsil mlčí"
            ZoneStatus.UNKNOWN -> "Zóna neznámá"
        },
        containerColor = containerColor,
        contentColor = contentColor,
        bold = status == ZoneStatus.HOME
    )
}

@Composable
private fun ContractAction(
    state: HomeUiState,
    onOpenContract: () -> Unit
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    val completed = state.contractDone
    val container = if (completed) colors.positiveContainer else colors.echoContainer
    val content = if (completed) colors.onPositiveContainer else colors.onEchoContainer
    val action = if (completed) colors.positive else colors.echo
    val actionContent = if (completed) colors.onPositive else colors.onEcho

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Text(
                text = if (completed) "Plán je připravený" else "Další krok",
                style = MaterialTheme.typography.labelLarge,
                color = content.copy(alpha = 0.78f)
            )
            Text(
                text = state.contractTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Plánovaný obsah: ${state.plannedContentSummary}",
                style = MaterialTheme.typography.bodyMedium,
                color = content.copy(alpha = 0.82f)
            )
            Button(
                onClick = onOpenContract,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xxs),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = action,
                    contentColor = actionContent
                )
            ) {
                Text(text = state.contractButton)
            }
        }
    }
}

@Composable
private fun DailyOverview(state: HomeUiState) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    FlatSection {
        Text(
            text = "Dnes máš zpátky ${CzechPlural.minutesAccusative(state.reclaimedMinutesToday)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.positive
        )
        Text(
            text = if (state.reclaimedMinutesToday >= state.feedMinutesToday) {
                "Ty vedeš nad algoritmem."
            } else {
                "Stačí jeden vědomý krok a rozdíl se zmenší."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            FlatMetric(
                value = "${state.feedMinutesToday} min",
                label = "ve feedu",
                modifier = Modifier.weight(1f)
            )
            FlatMetric(
                value = CzechPlural.days(state.streakDays),
                label = "aktuální série",
                modifier = Modifier.weight(1f)
            )
            FlatMetric(
                value = "${state.wonDaysTotal}",
                label = "vyhraných dní",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MovementOverview(state: HomeUiState) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    val safeGoal = state.stepGoal.coerceAtLeast(1)
    val progress = (state.stepsToday.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)

    FlatSection {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = "Pohyb",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${state.stepsToday} z ${CzechPlural.steps(state.stepGoal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${state.availableStepMinutes} min",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.ember
                )
                Text(
                    text = "k dispozici",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        NarsilProgressBar(
            progress = progress,
            fillColor = colors.ember,
            modifier = Modifier.padding(top = spacing.xs),
            height = 8.dp
        )
        Text(
            text = state.debtLabel ?: "Bez dluhu",
            style = MaterialTheme.typography.labelMedium,
            color = if (state.debtLabel == null) colors.positive else colors.debt
        )
    }
}

@Composable
private fun ProgressOverview(state: HomeUiState) {
    val spacing = NarsilTheme.spacing

    FlatSection {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Úroveň ${state.level}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.levelTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CzechPlural.count(state.availableJokers, "žolík", "žolíky", "žolíků"),
                style = MaterialTheme.typography.labelLarge,
                color = NarsilTheme.colors.goldAccent
            )
        }
        XpProgressBar(
            currentXp = state.currentXp,
            modifier = Modifier.padding(top = spacing.xs)
        )
    }
}

@Composable
private fun FlatMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NarsilTheme.spacing.xxs)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FlatSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(NarsilTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(NarsilTheme.spacing.xs),
            content = content
        )
    }
}

@Composable
private fun FlatNavigationAction(
    label: String,
    supportingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(NarsilTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(NarsilTheme.spacing.xxs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Domů – Flat světlý", showBackground = true)
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
    name = "Domů – Flat tmavý",
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
