package cz.narsil.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.core.ComfortContext
import cz.narsil.core.LlmProvider
import cz.narsil.ui.theme.NarsilTheme

/**
 * Calm, plain-Czech settings screen for Narsil (task 325).
 * Long-pressing the version footer opens the hidden Demo panel.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSaveHomeWifi: () -> Unit,
    onSelectManualZone: (ManualZoneChoice) -> Unit,
    onSelectLlmProvider: (LlmProvider) -> Unit,
    onConnectStrava: () -> Unit,
    onOpenSystemSettings: (SettingsPermissionType) -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenDemoPanel: () -> Unit,
    onBack: () -> Unit,
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
                    text = "Nastavení",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onBack) {
                    Text("Zpět")
                }
            }

            // 1. Domov
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
                            text = "Domov",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        ContextStatusBadge(context = state.comfortContext)
                    }

                    Text(
                        text = "Ozvěna zasahuje jen doma.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Aktuální Wi-Fi: ${state.currentWifiSsid ?: "nepřipojeno"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Uložený domov: ${state.homeWifiSsid ?: "zatím nenastaveno"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onSaveHomeWifi,
                        enabled = !state.currentWifiSsid.isNullOrBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.ember,
                            contentColor = colors.onEmber
                        )
                    ) {
                        Text("Tohle je můj domov")
                    }

                    state.homeSaveFeedback?.let { feedback ->
                        Text(
                            text = feedback,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.positive
                        )
                    }

                    Text(
                        text = "Ruční přepínač (platí do 4:00):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        ManualZoneChoice.entries.forEach { choice ->
                            FilterChip(
                                selected = state.manualZone == choice,
                                onClick = { onSelectManualZone(choice) },
                                label = { Text(choice.label) }
                            )
                        }
                    }
                }
            }

            // 2. Ověření úkolů (AI)
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
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = "Ověření úkolů",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Vyber, kdo kontroluje fotky a popisy splněných úkolů.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        LlmOption(
                            label = "Claude",
                            selected = state.llmProvider == LlmProvider.CLAUDE,
                            onClick = { onSelectLlmProvider(LlmProvider.CLAUDE) }
                        )
                        LlmOption(
                            label = "Gemini",
                            selected = state.llmProvider == LlmProvider.GEMINI,
                            onClick = { onSelectLlmProvider(LlmProvider.GEMINI) }
                        )
                        LlmOption(
                            label = "ChatGPT",
                            selected = state.llmProvider == LlmProvider.OPENAI,
                            onClick = { onSelectLlmProvider(LlmProvider.OPENAI) }
                        )
                    }
                }
            }

            // 3. Strava
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                    ) {
                        Text(
                            text = "Strava (nepovinné)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.stravaConnected) {
                                "Připojeno · sportovní aktivity ověří pohybové úkoly automaticky."
                            } else {
                                "Nepřipojeno · pohybový úkol můžeš vždy doložit i fotkou."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (state.stravaConnected) {
                        Surface(
                            shape = CircleShape,
                            color = colors.positiveContainer,
                            contentColor = colors.onPositiveContainer
                        ) {
                            Text(
                                text = "Připojeno",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs)
                            )
                        }
                    } else {
                        OutlinedButton(onClick = onConnectStrava) {
                            Text("Připojit")
                        }
                    }
                }
            }

            // 4. Oprávnění
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
                        text = "Oprávnění",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    state.permissions.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = spacing.sm)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = item.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (item.isGranted) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.positiveContainer,
                                    contentColor = colors.onPositiveContainer
                                ) {
                                    Text(
                                        text = "Povoleno",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                                    )
                                }
                            } else {
                                OutlinedButton(onClick = { onOpenSystemSettings(item.type) }) {
                                    Text("Otevřít")
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = onOpenOnboarding,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Spustit průvodce oprávněními")
                    }
                }
            }

            // 5. Parťák (MVP simulace)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Parťák",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "MVP ukázka",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                            )
                        }
                    }
                    Text(
                        text = "V téhle verzi jde jen o ukázku notifikace na druhém telefonu. Parťák nikdy nevidí tvoje videa ani historii – dostane zprávu jen při vypnutí ochrany nebo překročení limitu dluhu.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 6. Skrytý vstup do demo panelu (dlouhé podržení verze)
            Text(
                text = state.appVersion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onOpenDemoPanel
                    )
                    .padding(vertical = spacing.md)
            )
        }
    }
}

@Composable
private fun ContextStatusBadge(context: ComfortContext) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    val (label, bg, fg) = when (context) {
        ComfortContext.HOME -> Triple("Doma", colors.echoContainer, colors.onEchoContainer)
        ComfortContext.AWAY -> Triple("Venku", colors.positiveContainer, colors.onPositiveContainer)
        ComfortContext.UNKNOWN -> Triple(
            "Neznámo",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Surface(shape = CircleShape, color = bg, contentColor = fg) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
        )
    }
}

@Composable
private fun LlmOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Preview(name = "Nastavení – Světlý režim", showBackground = true)
@Composable
private fun SettingsLightPreview() {
    NarsilTheme(darkTheme = false) {
        SettingsScreen(
            state = SettingsUiState(
                permissions = defaultSettingsPermissions(
                    accessibilityGranted = true,
                    overlayGranted = false,
                    locationGranted = true
                )
            ),
            onSaveHomeWifi = {},
            onSelectManualZone = {},
            onSelectLlmProvider = {},
            onConnectStrava = {},
            onOpenSystemSettings = {},
            onOpenOnboarding = {},
            onOpenDemoPanel = {},
            onBack = {}
        )
    }
}

@Preview(
    name = "Nastavení – Tmavý režim",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SettingsDarkPreview() {
    NarsilTheme(darkTheme = true) {
        SettingsScreen(
            state = SettingsUiState(
                comfortContext = ComfortContext.AWAY,
                manualZone = ManualZoneChoice.AWAY,
                llmProvider = LlmProvider.GEMINI,
                stravaConnected = true
            ),
            onSaveHomeWifi = {},
            onSelectManualZone = {},
            onSelectLlmProvider = {},
            onConnectStrava = {},
            onOpenSystemSettings = {},
            onOpenOnboarding = {},
            onOpenDemoPanel = {},
            onBack = {}
        )
    }
}
