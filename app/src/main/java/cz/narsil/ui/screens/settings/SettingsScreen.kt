package cz.narsil.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import cz.narsil.core.Exercise
import cz.narsil.core.LlmProvider
import cz.narsil.core.MovementRates
import cz.narsil.core.Settings
import cz.narsil.core.SettingsDefaults
import cz.narsil.core.TrackedApp
import cz.narsil.ui.components.StatusPill
import cz.narsil.ui.theme.NarsilTheme

/** Akce obrazovky nastavení (133). Výchozí = nic, ať náhledy a testy nemusí vyplňovat vše. */
data class SettingsActions(
    val onBack: () -> Unit = {},
    val onSaveHomeWifi: () -> Unit = {},
    val onSelectManualZone: (ManualZoneChoice) -> Unit = {},
    val onSelectLlmProvider: (LlmProvider) -> Unit = {},
    val onSelectExercise: (Exercise) -> Unit = {},
    val onToleranceMinutes: (Int) -> Unit = {},
    val onVideoChainLimit: (Int) -> Unit = {},
    val onSquatsPerUnlock: (Int) -> Unit = {},
    val onPushupsPerUnlock: (Int) -> Unit = {},
    val onDebtLimitMinutes: (Int) -> Unit = {},
    val onAppTracked: (TrackedApp, Boolean) -> Unit = { _, _ -> },
    val onTrackOtherFeedApps: (Boolean) -> Unit = {},
    val onPartnerAlerts: (Boolean) -> Unit = {},
    val onShowScrollProgress: (Boolean) -> Unit = {},
    val onPauseProtection: () -> Unit = {},
    val onResumeProtection: () -> Unit = {},
    val onResetSection: (SettingsSection) -> Unit = {},
    val onConfirmPending: () -> Unit = {},
    val onCancelPending: () -> Unit = {},
    val onConnectStrava: () -> Unit = {},
    val onStartStravaDemo: () -> Unit = {},
    val onDisconnectStrava: () -> Unit = {},
    val onOpenSystemSettings: (SettingsPermissionType) -> Unit = {},
    val onOpenOnboarding: () -> Unit = {},
    val onOpenDemoPanel: () -> Unit = {},
)

/**
 * Nastavení Narsilu (325, 133): všechny parametry chování v sekcích s krátkým vysvětlením.
 * Výchozí hodnoty jsou doporučené; uvolnění potvrzuje dialog s odpočtem (viz [SettingsGuard]).
 * Dlouhé podržení verze dole otevře skrytý demo panel.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    val spacing = NarsilTheme.spacing
    val s = state.settings

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
                TextButton(onClick = actions.onBack) {
                    Text("Zpět")
                }
            }
            Text(
                text = "Výchozí hodnoty jsou doporučené a měnit je nemusíš. Zpřísnění platí hned, " +
                    "uvolnění chce chvilku na rozmyšlenou.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ProtectionStatusCard(state, actions)

            // Ozvěna: kdy zasáhne
            SettingsCard(
                title = "Ozvěna",
                description = "Kdy Narsil přeruší nekonečný feed. Zasahuje jen doma a nikdy ve zprávách.",
                section = SettingsSection.PROTECTION,
                settings = s,
                onReset = actions.onResetSection
            ) {
                OptionRow(
                    label = "Po kolika minutách scrollování",
                    hint = "Doporučeno ${SettingsDefaults.TOLERANCE_MINUTES} min. Delší čas = Ozvěna přijde později.",
                    options = SettingsDefaults.TOLERANCE_OPTIONS,
                    selected = s.toleranceMinutes,
                    recommended = SettingsDefaults.TOLERANCE_MINUTES,
                    format = { "$it min" },
                    onSelect = actions.onToleranceMinutes
                )
                if (s.demoThresholdSec != null) {
                    Text(
                        text = "Demo panel teď drží práh ${s.demoThresholdSec} s – má přednost.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NarsilTheme.colors.ember
                    )
                }
                OptionRow(
                    label = "U kolikátého neplánovaného videa po sobě",
                    hint = "Plánovaná videa ze smlouvy se nepočítají. Pauza nad 10 min řadu vynuluje.",
                    options = SettingsDefaults.VIDEO_CHAIN_OPTIONS,
                    selected = s.videoChainLimit,
                    recommended = SettingsDefaults.VIDEO_CHAIN_LIMIT,
                    format = { "$it." },
                    onSelect = actions.onVideoChainLimit
                )
                SwitchRow(
                    title = "Ukazatel limitu při scrollování",
                    subtitle = "Tenký proužek nahoře ukáže, jak blízko je Ozvěna. Nic neblokuje, jen připomíná čas.",
                    checked = s.showScrollProgress,
                    onCheckedChange = actions.onShowScrollProgress
                )
            }

            // Sledované aplikace
            SettingsCard(
                title = "Sledované aplikace",
                description = "Kde Narsil hlídá nekonečný obsah. Chaty (WhatsApp, Messenger, Telegram…) nikdy nečte.",
                section = SettingsSection.APPS,
                settings = s,
                onReset = actions.onResetSection
            ) {
                // Vlastní pravidla vždy, ostatní sítě (149) jen nainstalované.
                TrackedApp.entries
                    .filter { it in SettingsGuard.CORE_APPS || state.installedApps?.contains(it) != false }
                    .forEach { app ->
                        SwitchRow(
                            title = SettingsGuard.appLabel(app),
                            subtitle = appHint(app),
                            checked = app in s.trackedApps,
                            onCheckedChange = { actions.onAppTracked(app, it) }
                        )
                    }
                SwitchRow(
                    title = "Další aplikace s nekonečným feedem",
                    subtitle = "Pozná feed podle opakovaného scrollování v jakékoli jiné aplikaci. Chaty, systém a klávesnice ne.",
                    checked = s.trackOtherFeedApps,
                    onCheckedChange = actions.onTrackOtherFeedApps
                )
            }

            HomeCard(state, actions)

            // Pohyb a odemykání
            SettingsCard(
                title = "Pohyb a odemykání",
                description = "Čím si v Ozvěně odemkneš ${MovementRates.MINUTES_PER_UNLOCK} minut. " +
                    "Kroky platí vždy: ${MovementRates.STEPS_PER_MINUTE * MovementRates.MINUTES_PER_UNLOCK} kroků = " +
                    "${MovementRates.MINUTES_PER_UNLOCK} min.",
                section = SettingsSection.MOVEMENT,
                settings = s,
                onReset = actions.onResetSection
            ) {
                OptionRow(
                    label = "Cvik",
                    hint = if (s.exercise == Exercise.SQUATS) {
                        "Dřepy počítá kamera, telefon opři naproti sobě tak, aby tě viděl celého."
                    } else {
                        "Kliky počítá telefon položený na zemi pod obličejem."
                    },
                    options = Exercise.entries,
                    selected = s.exercise,
                    recommended = Settings().exercise,
                    format = { if (it == Exercise.SQUATS) "Dřepy" else "Kliky" },
                    onSelect = actions.onSelectExercise
                )
                OptionRow(
                    label = "Dřepů na odemčení",
                    hint = null,
                    options = SettingsDefaults.SQUATS_OPTIONS,
                    selected = s.squatsPerUnlock,
                    recommended = MovementRates.SQUATS_PER_UNLOCK,
                    format = { "$it" },
                    onSelect = actions.onSquatsPerUnlock
                )
                OptionRow(
                    label = "Kliků na odemčení",
                    hint = null,
                    options = SettingsDefaults.PUSHUPS_OPTIONS,
                    selected = s.pushupsPerUnlock,
                    recommended = MovementRates.PUSHUPS_PER_UNLOCK,
                    format = { "$it" },
                    onSelect = actions.onPushupsPerUnlock
                )
                OptionRow(
                    label = "Limit dluhu",
                    hint = "Kolik minut si můžeš půjčit. Dluh se odečte zítra, nad limit dostane zprávu Parťák.",
                    options = SettingsDefaults.DEBT_LIMIT_OPTIONS,
                    selected = s.debtLimitMinutes,
                    recommended = MovementRates.DEFAULT_DEBT_LIMIT_MINUTES,
                    format = { if (it == 0) "Nepůjčovat" else "$it min" },
                    onSelect = actions.onDebtLimitMinutes
                )
            }

            // Ověření úkolů (AI)
            SettingsCard(
                title = "Ověření úkolů",
                description = "Kdo kontroluje fotky a popisy splněných úkolů. Bez sítě se nic neschválí samo.",
                section = SettingsSection.AI,
                settings = s,
                onReset = actions.onResetSection
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    LlmProvider.entries.forEach { provider ->
                        val hasKey = state.llmKeys[provider] != false
                        FilterChip(
                            selected = s.llmProvider == provider,
                            onClick = { actions.onSelectLlmProvider(provider) },
                            label = {
                                Text(
                                    providerLabel(provider) +
                                        (if (provider == Settings().llmProvider) " · doporučeno" else "") +
                                        (if (!hasKey) " · bez klíče" else "")
                                )
                            }
                        )
                    }
                }
                val selectedHasKey = state.llmKeys[s.llmProvider] != false
                Text(
                    text = if (selectedHasKey) {
                        "Klíč ${providerLabel(s.llmProvider)} je nastavený."
                    } else {
                        "Klíč ${providerLabel(s.llmProvider)} chybí v local.properties – ověření úkolů počká, až bude klíč."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedHasKey) NarsilTheme.colors.positive else MaterialTheme.colorScheme.error
                )
            }

            // Strava se nepoužije (146) – karta se neukazuje, kód zůstává.

            // Upozornění
            SettingsCard(
                title = "Upozornění",
                description = "Parťák nikdy nevidí tvoje videa ani historii. V téhle verzi jde o ukázkovou notifikaci.",
                section = SettingsSection.NOTIFICATIONS,
                settings = s,
                onReset = actions.onResetSection
            ) {
                SwitchRow(
                    title = "Zprávy Parťákovi",
                    subtitle = "Při dluhu nad limit a při pozastavení ochrany.",
                    checked = s.partnerAlerts,
                    onCheckedChange = actions.onPartnerAlerts
                )
            }

            PermissionsCard(state, actions)

            FixedRulesCard()

            // Skrytý vstup do demo panelu (dlouhé podržení verze)
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
                        onLongClick = actions.onOpenDemoPanel
                    )
                    .padding(vertical = spacing.md)
            )
        }
    }

    state.pendingChange?.let { pending ->
        PendingChangeDialog(pending, onConfirm = actions.onConfirmPending, onCancel = actions.onCancelPending)
    }
}

@Composable
private fun ProtectionStatusCard(state: SettingsUiState, actions: SettingsActions) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    val paused = state.isProtectionPaused
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (paused) colors.emberContainer else colors.positiveContainer,
            contentColor = if (paused) colors.onEmberContainer else colors.onPositiveContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Text(
                text = if (paused) "Ochrana pozastavená ${state.protectionPausedText}" else "Ochrana běží",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (paused) {
                    "Ozvěna do té doby nepřijde. Ráno se ochrana zapne sama."
                } else {
                    "Ozvěna přijde po ${state.settings.toleranceMinutes} min scrollování doma. " +
                        "Potřebuješ výjimku? Pozastav ji nejdéle do 4:00."
                },
                style = MaterialTheme.typography.bodySmall
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (paused) {
                    Button(
                        onClick = actions.onResumeProtection,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.ember, contentColor = colors.onEmber)
                    ) { Text("Zapnout hned") }
                } else {
                    TextButton(onClick = actions.onPauseProtection) { Text("Pozastavit do rána") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeCard(state: SettingsUiState, actions: SettingsActions) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    PlainCard {
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
            text = "Ozvěna zasahuje jen doma. Domov pozná Narsil podle Wi-Fi.",
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
            onClick = actions.onSaveHomeWifi,
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
                    onClick = { actions.onSelectManualZone(choice) },
                    label = { Text(if (choice == ManualZoneChoice.AUTO) "${choice.label} · doporučeno" else choice.label) }
                )
            }
        }
    }
}

@Composable
private fun PermissionsCard(state: SettingsUiState, actions: SettingsActions) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    PlainCard {
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
                    StatusPill(text = "Povoleno", containerColor = colors.positiveContainer, contentColor = colors.onPositiveContainer)
                } else {
                    OutlinedButton(onClick = { actions.onOpenSystemSettings(item.type) }) {
                        Text("Otevřít")
                    }
                }
            }
        }
        TextButton(
            onClick = actions.onOpenOnboarding,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Spustit průvodce oprávněními")
        }
    }
}

/** Co se nastavit nedá – pro přehled, ať uživatel ví, jak Narsil počítá. */
@Composable
private fun FixedRulesCard() {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.echoContainer, contentColor = colors.onEchoContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            Text(text = "Pevná pravidla", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            listOf(
                "Den začíná ve 4:00, tehdy se nuluje i ruční přepínač a pozastavení.",
                "Odemčení dává ${MovementRates.MINUTES_PER_UNLOCK} minut, kroky se počítají za celý den.",
                "Narsil nic tvrdě neblokuje, jen přeruší a nabídne volbu.",
                "Zprávy a chaty se nikdy nesledují ani nepočítají.",
                "Úkoly se dokládají fotkou, fotkou prostředí nebo popisem, nejvýš 3 pokusy.",
            ).forEach { rule ->
                Text(text = "· $rule", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PendingChangeDialog(pending: PendingChange, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(pending.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NarsilTheme.spacing.xs)) {
                pending.reasons.forEach { Text("· $it", style = MaterialTheme.typography.bodyMedium) }
                Text(
                    text = "Je to rozhodnutí, nebo jen chuť pokračovat ve scrollování?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = pending.canConfirm) {
                Text(if (pending.canConfirm) "Ano, změnit" else "Ano, změnit (${pending.secondsLeft} s)")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) { Text("Nechat jak je") }
        }
    )
}

/** Karta sekce s nadpisem, vysvětlením a „Obnovit výchozí“ (aktivní jen při změně). */
@Composable
private fun SettingsCard(
    title: String,
    description: String,
    section: SettingsSection,
    settings: Settings,
    onReset: (SettingsSection) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val modified = SettingsGuard.isModified(settings, section)
    PlainCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (modified) {
                StatusPill(
                    text = "Upraveno",
                    containerColor = NarsilTheme.colors.emberContainer,
                    contentColor = NarsilTheme.colors.onEmberContainer
                )
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
        if (modified) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TextButton(onClick = { onReset(section) }, modifier = Modifier.align(Alignment.End)) {
                Text("Obnovit výchozí")
            }
        }
    }
}

@Composable
private fun PlainCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NarsilTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(NarsilTheme.spacing.sm),
            content = content
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionRow(
    label: String,
    hint: String?,
    options: List<T>,
    selected: T,
    recommended: T,
    format: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(NarsilTheme.spacing.xxs)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(NarsilTheme.spacing.xs)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(if (option == recommended) "${format(option)} ★" else format(option)) }
                )
            }
        }
        hint?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = NarsilTheme.spacing.sm)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun appHint(app: TrackedApp): String = when (app) {
    TrackedApp.INSTAGRAM -> "Feed a Reels. Zprávy ne."
    TrackedApp.TIKTOK -> "Pro tebe a Sledované."
    TrackedApp.YOUTUBE -> "Shorts a řetězení videí. Plánovaná videa projdou."
    TrackedApp.FIREFOX -> "YouTube, Instagram, TikTok, X a Reddit na webu."
    else -> "Feed podle scrollování. Zprávy a hledání ne."
}

private fun providerLabel(provider: LlmProvider): String = when (provider) {
    LlmProvider.CLAUDE -> "Claude"
    LlmProvider.GEMINI -> "Gemini"
    LlmProvider.OPENAI -> "ChatGPT"
}

/** Strava (132): nenastaveno → hláška + demo; demo vždy viditelně označené; chyby jako hláška. */
@Composable
private fun StravaCard(
    state: StravaCardState,
    onConnect: () -> Unit,
    onStartDemo: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    PlainCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strava (nepovinné)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            when {
                state.connected && state.demo -> StatusPill("Demo", colors.echoContainer, colors.onEchoContainer)
                state.connected -> StatusPill("Připojeno", colors.positiveContainer, colors.onPositiveContainer)
                !state.configured -> StatusPill(
                    "Nenastaveno",
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = when {
                state.connected && state.demo ->
                    "Demo režim · ukázková aktivita (běh 28 min), ne skutečná data ze Stravy."
                state.connected -> "Připojeno · sportovní aktivity ověří pohybové úkoly automaticky."
                !state.configured ->
                    "Strava není nastavená. Připojení zatím nejde, pohybový úkol můžeš doložit fotkou nebo vyzkoušet demo."
                else -> "Nepřipojeno · pohybový úkol můžeš vždy doložit i fotkou."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        state.message?.takeIf { !state.connected }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.End)
        ) {
            when {
                state.connected -> TextButton(onClick = onDisconnect) {
                    Text(if (state.demo) "Ukončit demo" else "Odpojit")
                }
                !state.configured -> OutlinedButton(onClick = onStartDemo) { Text("Vyzkoušet demo") }
                else -> OutlinedButton(onClick = onConnect) { Text("Připojit") }
            }
        }
    }
}

@Composable
private fun ContextStatusBadge(context: ComfortContext) {
    val colors = NarsilTheme.colors
    val (label, bg, fg) = when (context) {
        ComfortContext.HOME -> Triple("Doma", colors.echoContainer, colors.onEchoContainer)
        ComfortContext.AWAY -> Triple("Venku", colors.positiveContainer, colors.onPositiveContainer)
        ComfortContext.UNKNOWN -> Triple(
            "Neznámo",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    StatusPill(text = label, containerColor = bg, contentColor = fg)
}

@Preview(name = "Nastavení – Světlý režim", showBackground = true, heightDp = 2400)
@Composable
private fun SettingsLightPreview() {
    NarsilTheme(darkTheme = false) {
        SettingsScreen(
            state = SettingsUiState(
                permissions = defaultSettingsPermissions(
                    accessibilityGranted = true,
                    overlayGranted = false,
                    locationGranted = true
                ),
                settings = Settings(toleranceMinutes = 10),
                strava = StravaCardState(configured = false),
                llmKeys = mapOf(LlmProvider.GEMINI to true, LlmProvider.CLAUDE to false, LlmProvider.OPENAI to false)
            ),
            actions = SettingsActions()
        )
    }
}

@Preview(
    name = "Nastavení – Tmavý režim, pozastaveno",
    showBackground = true,
    heightDp = 2400,
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
                strava = StravaCardState(connected = true, demo = true),
                protectionPausedText = "do 4:00 ráno",
                pendingChange = PendingChange("Zvýšit limit dluhu?", listOf("Vyšší limit dluhu: 45 min místo 30 min."), 7)
            ),
            actions = SettingsActions()
        )
    }
}
