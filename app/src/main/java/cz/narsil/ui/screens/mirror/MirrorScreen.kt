package cz.narsil.ui.screens.mirror

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
 * Mirror ("Zrcadlo") & Content Value Map ("Mapa hodnoty") over 21-day data (task 430).
 */
@Composable
fun MirrorScreen(
    state: MirrorUiState,
    onSelectTab: (MirrorTab) -> Unit,
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
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = "Zrcadlo a hodnota obsahu",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                MirrorTab.entries.forEach { tab ->
                    FilterChip(
                        selected = state.activeTab == tab,
                        onClick = { onSelectTab(tab) },
                        label = { Text(tab.label) }
                    )
                }
            }

            when (state.activeTab) {
                MirrorTab.MIRROR -> MirrorOverviewContent(state = state)
                MirrorTab.VALUE_MAP -> ValueMapContent(state = state)
            }
        }
    }
}

@Composable
private fun MirrorOverviewContent(state: MirrorUiState) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = colors.positiveContainer,
                contentColor = colors.onPositiveContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = "Za 21 dní sis vzal zpět ${state.totalReclaimedMinutes / 60} hodin",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pasivní scrollování kleslo o ${state.feedDropPercent} % · v průměru ${state.averageDailySquats} dřepů denně.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Boss of critical hour
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = colors.echoContainer,
                contentColor = colors.onEchoContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                Text(
                    text = "Boss kritické hodiny · ${state.criticalBossHour}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Největší pokušení přichází večer. Bez feedu přežito ${state.bossSurvivedDays} dní (+150 XP za den).",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Weekly improvement trend
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
                Text(
                    text = "Vývoj po týdnech (získaný čas vs. feed)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                state.weeklyTrend.forEach { point ->
                    val total = (point.reclaimedMinutes + point.feedMinutes).coerceAtLeast(1)
                    val reclaimedShare = (point.reclaimedMinutes.toFloat() / total.toFloat()).coerceIn(0.1f, 0.9f)

                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = point.dayLabel,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "${point.reclaimedMinutes} min pro tebe · ${point.feedMinutes} min feed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
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
                    }
                }
            }
        }
    }
}

@Composable
private fun ValueMapContent(state: MirrorUiState) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = "Který obsah ti dává energii a který tě jen vysává (podle tvých hodnocení 1–5).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state.valueEntries.isEmpty()) {
            Text(
                text = "Zatím žádná hodnocení. Po videu na YouTube ohodnoť, co ti dalo – tady se pak ukáže, co stojí za to.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        state.valueEntries.forEach { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (entry.isLowValueTrap) colors.debtContainer else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (entry.isLowValueTrap) colors.debt.copy(alpha = 0.45f) else colors.positive.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                    ) {
                        Text(
                            text = entry.channelOrFormat,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (entry.isLowValueTrap) {
                                "${entry.videoCount}× · díváš se, ale hodnotíš nízko"
                            } else {
                                if (entry.averageRating >= 4f) "${entry.videoCount}× · vysoká hodnota pro tebe" else "${entry.videoCount}× · střední hodnota"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (entry.isLowValueTrap) colors.debt else colors.positive,
                        contentColor = if (entry.isLowValueTrap) colors.onDebt else colors.onPositive
                    ) {
                        Text(
                            text = "${entry.averageRating} / 5",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Zrcadlo – Světlý", showBackground = true)
@Composable
private fun MirrorLightPreview() {
    NarsilTheme(darkTheme = false) {
        MirrorScreen(
            state = MirrorUiState(activeTab = MirrorTab.MIRROR),
            onSelectTab = {}
        )
    }
}

@Preview(
    name = "Mapa hodnoty – Tmavý",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ValueMapDarkPreview() {
    NarsilTheme(darkTheme = true) {
        MirrorScreen(
            state = MirrorUiState(activeTab = MirrorTab.VALUE_MAP),
            onSelectTab = {}
        )
    }
}
