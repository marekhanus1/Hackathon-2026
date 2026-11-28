package cz.narsil.ui.screens.settings

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.core.ComfortContext
import cz.narsil.core.Exercise
import cz.narsil.core.LlmProvider
import cz.narsil.core.MovementRates
import cz.narsil.core.Settings
import cz.narsil.core.SettingsDefaults
import cz.narsil.core.TrackedApp
import cz.narsil.ui.components.NeuBadgeIcon
import cz.narsil.ui.components.NeuDivider
import cz.narsil.ui.components.NeuHeader
import cz.narsil.ui.components.NeuIcons
import cz.narsil.ui.components.NeuLabel
import cz.narsil.ui.components.NeuListRow
import cz.narsil.ui.components.NeuPanel
import cz.narsil.ui.components.NeuScreen
import cz.narsil.ui.components.NeuSegmented
import cz.narsil.ui.components.NeuSmallButton
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.components.StatusPill
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.neuInset

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

/** Stránka detailu nastavení za řádkem přehledu (334). */
enum class SettingsPage(val title: String, val short: String) {
    ECHO("Ozvěna", "OZ"),
    APPS("Sledované aplikace", "AP"),
    HOME("Domov", "DO"),
    MOVEMENT("Pohyb a odemykání", "PO"),
    AI("Ověření úkolů", "OV"),
    NOTIFICATIONS("Upozornění", "UP"),
    PERMISSIONS("Oprávnění", "OP"),
    RULES("Pevná pravidla", "PR"),
}

/**
 * Nastavení Narsilu ve stylu E (334; dříve 325, 133). Přehled: stav ochrany se zónou a řádky sekcí;
 * klepnutí otevře detail sekce se všemi parametry. Výchozí hodnoty jsou doporučené; uvolnění potvrzuje
 * dialog s odpočtem (viz [SettingsGuard]). Dlouhé podržení verze dole otevře skrytý demo panel.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    val current = page
    if (current == null) {
        SettingsOverview(state, actions, onOpen = { page = it }, modifier = modifier)
    } else {
        BackHandler { page = null }
        NeuScreen(modifier, gap = 14.dp) {
            NeuHeader(title = current.title, onBack = { page = null }, backDescription = "Zpět na nastavení")
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SettingsPageContent(current, state, actions)
            }
        }
    }

    state.pendingChange?.let { pending ->
        PendingChangeDialog(pending, onConfirm = actions.onConfirmPending, onCancel = actions.onCancelPending)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsOverview(
    state: SettingsUiState,
    actions: SettingsActions,
    onOpen: (SettingsPage) -> Unit,
    modifier: Modifier,
) {
    val s = state.settings
    val paused = state.isProtectionPaused
    NeuScreen(modifier, gap = 14.dp) {
        NeuHeader(title = "Nastavení", subtitle = "Narsil nic tvrdě neblokuje, jen přeruší a nabídne volbu.")

        NeuPanel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), gap = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (paused) NeuColors.Error else NeuColors.Success))
                Column(Modifier.weight(1f)) {
                    NeuText(if (paused) "Ochrana pozastavená" else "Ochrana běží", size = 15.sp, weight = FontWeight.ExtraBold)
                    NeuText(
                        if (paused) "${state.protectionPausedText?.replaceFirstChar { it.uppercase() } ?: "Do rána"} Ozvěna nepřijde"
                        else "Ozvěna po ${s.toleranceMinutes} min scrollování doma",
                        size = 12.sp,
                        weight = FontWeight.Medium,
                        color = NeuColors.Muted,
                        maxLines = 2,
                    )
                }
                if (paused) NeuSmallButton("Obnovit", actions.onResumeProtection, height = 38.dp)
                else NeuSmallButton("Pozastavit", actions.onPauseProtection, height = 38.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NeuText(
                    "Zóna · domácí Wi-Fi ${state.homeWifiSsid ?: "nenastavena"}" +
                        if (state.manualZone != ManualZoneChoice.AUTO) " · ručně platí do 4:00" else "",
                    size = 11.sp,
                    weight = FontWeight.Bold,
                    color = NeuColors.Muted,
                    maxLines = 1,
                )
                NeuSegmented(
                    options = ManualZoneChoice.entries.toList(),
                    selected = state.manualZone,
                    label = { it.label },
                    onSelect = actions.onSelectManualZone,
                    height = 34.dp,
                    outerRadius = 14.dp,
                    innerRadius = 10.dp,
                    outerDepth = NeuDepth.Deep,
                    selectedDepth = NeuDepth.Shallow,
                    ring = true,
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .neuInset(NeuDepth.Normal, RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsPage.entries.forEachIndexed { i, p ->
                if (i > 0) NeuDivider()
                val (sub, warn) = pageSummary(p, state)
                NeuListRow(
                    title = p.title,
                    subtitle = sub,
                    subtitleColor = if (warn) NeuColors.Error else NeuColors.Muted,
                    onClick = { onOpen(p) },
                    leading = { NeuBadgeIcon(p.short) },
                    trailing = { Icon(NeuIcons.Chevron, contentDescription = null, tint = NeuColors.Muted, modifier = Modifier.size(16.dp)) },
                    verticalPadding = 11.dp,
                )
            }
            // Skrytý vstup do demo panelu (dlouhé podržení verze)
            NeuText(
                text = state.appVersion,
                size = 11.sp,
                weight = FontWeight.Medium,
                color = NeuColors.Muted,
                align = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(onClick = {}, onLongClick = actions.onOpenDemoPanel)
                    .padding(vertical = 14.dp),
            )
        }
    }
}

/** Podnadpis řádku přehledu; druhá hodnota = varování (červeně). */
private fun pageSummary(page: SettingsPage, state: SettingsUiState): Pair<String, Boolean> {
    val s = state.settings
    val modified = when (page) {
        SettingsPage.ECHO -> SettingsGuard.isModified(s, SettingsSection.PROTECTION)
        SettingsPage.APPS -> SettingsGuard.isModified(s, SettingsSection.APPS)
        SettingsPage.MOVEMENT -> SettingsGuard.isModified(s, SettingsSection.MOVEMENT)
        SettingsPage.AI -> SettingsGuard.isModified(s, SettingsSection.AI)
        SettingsPage.NOTIFICATIONS -> SettingsGuard.isModified(s, SettingsSection.NOTIFICATIONS)
        else -> false
    }
    val mark = if (modified) " · upraveno" else ""
    return when (page) {
        SettingsPage.ECHO -> "Po ${s.toleranceMinutes} min scrollování doma · ukazatel limitu ${if (s.showScrollProgress) "zapnutý" else "vypnutý"}$mark" to false
        SettingsPage.APPS -> {
            val apps = s.trackedApps.map { SettingsGuard.appLabel(it) }
            (apps.joinToString(", ").ifBlank { "Žádná aplikace" } + (if (s.trackOtherFeedApps) " + další feedy" else "") + mark) to apps.isEmpty()
        }
        SettingsPage.HOME -> (
            "Domácí Wi-Fi ${state.homeWifiSsid ?: "zatím nenastavena"}" +
                " · teď ${when (state.comfortContext) { ComfortContext.HOME -> "doma"; ComfortContext.AWAY -> "venku"; ComfortContext.UNKNOWN -> "neznámo" }}"
            ) to (state.homeWifiSsid == null)
        SettingsPage.MOVEMENT -> (
            "${if (s.exercise == Exercise.SQUATS) "Dřepy" else "Kliky"} · " +
                "${MovementRates.STEPS_PER_MINUTE * MovementRates.MINUTES_PER_UNLOCK} kroků = ${MovementRates.MINUTES_PER_UNLOCK} min · " +
                (if (s.debtLimitMinutes == 0) "bez půjček" else "dluh max ${s.debtLimitMinutes} min") + mark
            ) to false
        SettingsPage.AI -> {
            val hasKey = state.llmKeys[s.llmProvider] != false
            "${providerLabel(s.llmProvider)} · ${if (hasKey) "klíč nastavený" else "chybí klíč"}$mark" to !hasKey
        }
        SettingsPage.NOTIFICATIONS -> "Zprávy Parťákovi ${if (s.partnerAlerts) "zapnuté" else "vypnuté"}$mark" to false
        SettingsPage.PERMISSIONS -> {
            val missing = state.permissions.filter { !it.isGranted }
            (if (missing.isEmpty()) "Vše povoleno" else "Chybí: " + missing.joinToString(", ") { it.title.lowercase() }) to missing.isNotEmpty()
        }
        SettingsPage.RULES -> "Co se nastavit nedá a jak Narsil počítá" to false
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsPageContent(page: SettingsPage, state: SettingsUiState, actions: SettingsActions) {
    val spacing = NarsilTheme.spacing
    val s = state.settings
    when (page) {
        SettingsPage.ECHO -> SettingsCard(
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
                NeuText(
                    "Demo panel teď drží práh ${s.demoThresholdSec} s – má přednost.",
                    size = 12.sp,
                    weight = FontWeight.SemiBold,
                    color = NeuColors.Accent
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

        SettingsPage.APPS -> SettingsCard(
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

        SettingsPage.HOME -> HomeCard(state, actions)

        SettingsPage.MOVEMENT -> SettingsCard(
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

        SettingsPage.AI -> SettingsCard(
            title = "Ověření úkolů",
            description = "Kdo kontroluje fotky a popisy splněných úkolů. Bez sítě se nic neschválí samo.",
            section = SettingsSection.AI,
            settings = s,
            onReset = actions.onResetSection
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                LlmProvider.entries.forEach { provider ->
                    val hasKey = state.llmKeys[provider] != false
                    NeuChip(
                        text = providerLabel(provider) +
                            (if (provider == Settings().llmProvider) " · doporučeno" else "") +
                            (if (!hasKey) " · bez klíče" else ""),
                        selected = s.llmProvider == provider,
                        onClick = { actions.onSelectLlmProvider(provider) }
                    )
                }
            }
            val selectedHasKey = state.llmKeys[s.llmProvider] != false
            NeuText(
                if (selectedHasKey) "Klíč ${providerLabel(s.llmProvider)} je nastavený."
                else "Klíč ${providerLabel(s.llmProvider)} chybí v local.properties – ověření úkolů počká, až bude klíč.",
                size = 12.sp,
                weight = FontWeight.SemiBold,
                color = if (selectedHasKey) NeuColors.Success else NeuColors.Error
            )
        }

        // Strava se nepoužije (146) – karta se neukazuje, kód zůstává.

        SettingsPage.NOTIFICATIONS -> SettingsCard(
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

        SettingsPage.PERMISSIONS -> PermissionsCard(state, actions)

        SettingsPage.RULES -> FixedRulesCard()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeCard(state: SettingsUiState, actions: SettingsActions) {
    val spacing = NarsilTheme.spacing
    PlainCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuText("Domov", size = 16.sp, weight = FontWeight.ExtraBold)
            ContextStatusBadge(context = state.comfortContext)
        }
        CardText("Ozvěna zasahuje jen doma. Domov pozná Narsil podle Wi-Fi.")
        NeuText("Aktuální Wi-Fi: ${state.currentWifiSsid ?: "nepřipojeno"}", size = 13.sp, weight = FontWeight.SemiBold)
        CardText("Uložený domov: ${state.homeWifiSsid ?: "zatím nenastaveno"}")
        NeuSmallButton(
            "Tohle je můj domov",
            actions.onSaveHomeWifi,
            Modifier.fillMaxWidth(),
            enabled = !state.currentWifiSsid.isNullOrBlank(),
            height = 44.dp,
        )
        state.homeSaveFeedback?.let { feedback ->
            NeuText(feedback, size = 12.sp, weight = FontWeight.SemiBold, color = NeuColors.Success)
        }
        NeuLabel("Ruční přepínač (platí do 4:00)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            ManualZoneChoice.entries.forEach { choice ->
                NeuChip(
                    text = if (choice == ManualZoneChoice.AUTO) "${choice.label} · doporučeno" else choice.label,
                    selected = state.manualZone == choice,
                    onClick = { actions.onSelectManualZone(choice) }
                )
            }
        }
    }
}

@Composable
private fun PermissionsCard(state: SettingsUiState, actions: SettingsActions) {
    PlainCard {
        NeuText("Oprávnění", size = 16.sp, weight = FontWeight.ExtraBold)
        state.permissions.forEachIndexed { i, item ->
            if (i > 0) NeuDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    NeuText(item.title, size = 14.sp, weight = FontWeight.Bold)
                    CardText(item.reason)
                }
                if (item.isGranted) {
                    NeuText("Povoleno", size = 12.sp, weight = FontWeight.ExtraBold, color = NeuColors.Success)
                } else {
                    NeuSmallButton("Otevřít", { actions.onOpenSystemSettings(item.type) })
                }
            }
        }
        NeuSmallButton("Spustit průvodce oprávněními", actions.onOpenOnboarding, Modifier.fillMaxWidth(), height = 44.dp)
    }
}

/** Co se nastavit nedá – pro přehled, ať uživatel ví, jak Narsil počítá. */
@Composable
private fun FixedRulesCard() {
    PlainCard {
        NeuText("Pevná pravidla", size = 16.sp, weight = FontWeight.ExtraBold)
        listOf(
            "Den začíná ve 4:00, tehdy se nuluje i ruční přepínač a pozastavení.",
            "Odemčení dává ${MovementRates.MINUTES_PER_UNLOCK} minut, kroky se počítají za celý den.",
            "Narsil nic tvrdě neblokuje, jen přeruší a nabídne volbu.",
            "Zprávy a chaty se nikdy nesledují ani nepočítají.",
            "Úkoly se dokládají fotkou, fotkou prostředí nebo popisem, nejvýš 3 pokusy.",
        ).forEach { rule -> CardText("· $rule") }
    }
}

@Composable
private fun PendingChangeDialog(pending: PendingChange, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = NeuColors.Base,
        titleContentColor = NeuColors.Text,
        textContentColor = NeuColors.Text,
        title = { Text(pending.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NarsilTheme.spacing.xs)) {
                pending.reasons.forEach { Text("· $it", style = MaterialTheme.typography.bodyMedium) }
                Text(
                    text = "Je to rozhodnutí, nebo jen chuť pokračovat ve scrollování?",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuColors.Muted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = pending.canConfirm) {
                Text(
                    if (pending.canConfirm) "Ano, změnit" else "Ano, změnit (${pending.secondsLeft} s)",
                    color = if (pending.canConfirm) NeuColors.Muted else NeuColors.Muted.copy(alpha = 0.5f)
                )
            }
        },
        dismissButton = {
            NeuSmallButton("Nechat jak je", onCancel, height = 40.dp)
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
            NeuText(title, Modifier.weight(1f), size = 16.sp, weight = FontWeight.ExtraBold)
            if (modified) NeuText("Upraveno", size = 12.sp, weight = FontWeight.ExtraBold, color = NeuColors.Accent)
        }
        CardText(description)
        content()
        if (modified) {
            NeuDivider()
            NeuSmallButton("Obnovit výchozí", { onReset(section) }, Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun PlainCard(content: @Composable ColumnScope.() -> Unit) {
    NeuPanel(Modifier.fillMaxWidth(), radius = 24.dp, padding = PaddingValues(16.dp), gap = 12.dp, content = content)
}

@Composable
private fun CardText(text: String) {
    NeuText(text, size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted, lineHeight = 17.sp)
}

/** Volba ze seznamu: vybraná zamáčknutá hlouběji s jantarovým rámečkem. */
@Composable
private fun NeuChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .neuInset(if (selected) NeuDepth.Deep else NeuDepth.Shallow, shape)
            .then(if (selected) Modifier.border(1.dp, NeuColors.Accent, shape) else Modifier)
            .clip(shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        NeuText(text, size = 12.sp, weight = FontWeight.ExtraBold, color = if (selected) NeuColors.Accent else NeuColors.Muted, maxLines = 1)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NeuText(label, size = 13.sp, weight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                NeuChip(
                    text = if (option == recommended) "${format(option)} ★" else format(option),
                    selected = option == selected,
                    onClick = { onSelect(option) }
                )
            }
        }
        hint?.let { CardText(it) }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            NeuText(title, size = 14.sp, weight = FontWeight.Bold)
            subtitle?.let { CardText(it) }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeuColors.OnAccent,
                checkedTrackColor = NeuColors.Accent,
                uncheckedThumbColor = NeuColors.Muted,
                uncheckedTrackColor = NeuColors.Deep,
                uncheckedBorderColor = NeuColors.Divider,
            )
        )
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
    val (label, color) = when (context) {
        ComfortContext.HOME -> "Doma" to NeuColors.Success
        ComfortContext.AWAY -> "Venku" to NeuColors.Accent
        ComfortContext.UNKNOWN -> "Neznámo" to NeuColors.Muted
    }
    Box(Modifier.neuInset(NeuDepth.Deep, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        NeuText(label, size = 12.sp, weight = FontWeight.ExtraBold, color = color)
    }
}

@Preview(name = "Nastavení – přehled", showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun SettingsLightPreview() {
    NarsilTheme {
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
    heightDp = 844, widthDp = 390,
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
