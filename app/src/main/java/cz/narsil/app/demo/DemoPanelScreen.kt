package cz.narsil.app.demo

import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.narsil.app.EchoOverlayStarter
import cz.narsil.app.ServiceLocator
import cz.narsil.core.AvatarState
import cz.narsil.core.ComfortContext
import cz.narsil.core.Exercise
import cz.narsil.overlay.ExerciseText
import cz.narsil.core.LlmProvider
import cz.narsil.detection.accessibility.AccessibilityEvents
import cz.narsil.detection.accessibility.AccessibilityTriggerSource
import cz.narsil.ui.components.ChoiceCard
import cz.narsil.ui.components.ChoiceCardTone
import cz.narsil.ui.theme.NarsilTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Demo panel (123): příprava dema za 30 s. Pro tým, ne pro porotu.
 * Vzhled z Janova tématu (301) a komponent (302), aby panel nevyčníval.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DemoPanelScreen(onBack: () -> Unit) {
    NarsilTheme {
        val sl = ServiceLocator
        val colors = NarsilTheme.colors
        val spacing = NarsilTheme.spacing
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        val context = LocalContext.current

        val settings by sl.settingsRepository.settings.collectAsState()
        val comfort by sl.contextRepository.context.collectAsState()
        val manual by sl.contextRepository.manualOverride.collectAsState()
        val wifi by sl.contextRepository.currentWifiSsid.collectAsState()
        val serviceOn by AccessibilityEvents.serviceConnected.collectAsState()
        val echoReady by EchoOverlayStarter.isReady.collectAsState()
        val movement by sl.movementRepository.state.collectAsState()
        val game by sl.gamificationEngine.state.collectAsState()
        val unlockedUntil by sl.unlockTimer.unlockedUntil.collectAsState()
        val session = (sl.interventionTriggerSource as? AccessibilityTriggerSource)?.current?.collectAsState()
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1_000)
                now = System.currentTimeMillis()
            }
        }

        fun act(done: String, block: suspend () -> Boolean) {
            scope.launch {
                val ok = block()
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(if (ok) done else "Nejde: služba neběží nebo modul akci nepodporuje")
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                // Hlavička
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Demo panel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Příprava dema. Porota ho nevidí.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onBack) { Text("Zpět") }
                }

                // Stav
                Section("Stav") {
                    StatusRow("Detekce (přístupnost)", if (serviceOn) "běží" else "vypnutá", ok = serviceOn)
                    StatusRow("Ozvěna", if (echoReady) "připravená" else "nepřipravená", ok = echoReady)
                    val overlayOk = AndroidSettings.canDrawOverlays(context)
                    StatusRow("Překrytí oken", if (overlayOk) "povoleno" else "nepovoleno", ok = overlayOk)
                    StatusRow(
                        "Kontext",
                        when (comfort) {
                            ComfortContext.HOME -> "doma"
                            ComfortContext.AWAY -> "venku"
                            ComfortContext.UNKNOWN -> "nevím"
                        } + if (manual != null) " (ručně)" else "",
                        ok = comfort == ComfortContext.HOME,
                    )
                    StatusRow("Wi-Fi / domov", "${wifi ?: "žádná"} / ${settings.homeSsid ?: "neuložen"}")
                    StatusRow("Práh zásahu", settings.demoThresholdSec?.let { "$it s" } ?: "5 min")
                    val keyOk = DemoActions.hasKey(settings.llmProvider)
                    StatusRow(
                        "AI ověření",
                        "${settings.llmProvider.label()} · klíč ${if (keyOk) "ano" else "ne"}",
                        ok = keyOk || settings.demoAlwaysYes,
                    )
                    StatusRow(
                        "Scrollování",
                        session?.value?.let { "${it.surface.name.lowercase()} ${(now - it.startedAtMillis) / 1000} s" } ?: "nic",
                    )
                    StatusRow(
                        "Odemčeno",
                        ((unlockedUntil - now) / 1000).takeIf { it > 0 }?.let { "ještě ${it / 60}:${"%02d".format(it % 60)}" } ?: "ne",
                    )
                    StatusRow("Kroky / zůstatek / dluh", "${movement.stepsToday} / ${movement.balanceMinutes} min / ${movement.debtMinutes} min")
                    StatusRow("XP / úroveň / série", "${game.xp} / ${game.level} / ${game.streakDays} dní")
                }

                // Hlavní akce
                ChoiceCard(
                    title = "Připravit demo",
                    subtitle = "Doma, práh 20 s, dnešní smlouva s úkoly, čistý den",
                    onClick = { act("Demo připraveno") { DemoActions.prepareDemo(); true } },
                    tone = ChoiceCardTone.POSITIVE,
                )
                ChoiceCard(
                    title = "Ukázat Ozvěnu teď",
                    subtitle = if (echoReady) "Bez čekání na scrollování" else "Nejdřív zapni službu přístupnosti",
                    onClick = { act("Ozvěna ukázána") { DemoActions.showEchoNow() } },
                    tone = ChoiceCardTone.ECHO,
                    enabled = echoReady,
                )

                // Kontext
                Section("Kde jsem") {
                    val options = listOf(ComfortContext.HOME to "Doma", ComfortContext.AWAY to "Venku", null to "Automaticky")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { i, (c, label) ->
                            SegmentedButton(
                                selected = manual == c,
                                onClick = { act("Kontext: $label") { DemoActions.setContext(c); true } },
                                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            ) { Text(label) }
                        }
                    }
                    Hint("Ruční volba platí do 4:00. Automaticky = podle domácí Wi-Fi.")
                }

                // Přepínače
                Section("Demo nastavení") {
                    ToggleRow("Krátký práh", "Ozvěna po 20 s místo 5 min", settings.demoThresholdSec != null) { on ->
                        act(if (on) "Práh 20 s" else "Práh 5 min") { DemoActions.setShortThreshold(on); true }
                    }
                    HorizontalDivider()
                    ToggleRow("Smlouva platí hned", "Večerní smlouva se uloží na dnešek", settings.demoContractToday) { on ->
                        act("Smlouva ${if (on) "na dnešek" else "na zítřek"}") { DemoActions.setContractToday(on); true }
                    }
                    HorizontalDivider()
                    Text("Cvik v Ozvěně", style = MaterialTheme.typography.bodyLarge)
                    val exercises = Exercise.entries
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        exercises.forEachIndexed { i, e ->
                            SegmentedButton(
                                selected = settings.exercise == e,
                                onClick = { act("Cvik: ${ExerciseText.label(e)}") { DemoActions.setExercise(e); true } },
                                shape = SegmentedButtonDefaults.itemShape(i, exercises.size),
                            ) { Text(ExerciseText.label(e)) }
                        }
                    }
                    Hint("Dřepy: 20 opakování, telefon opřený naproti. Kliky: 10 opakování, telefon leží na zemi pod hrudníkem displejem nahoru.")
                    HorizontalDivider()
                    Text("AI pro ověření úkolů", style = MaterialTheme.typography.bodyLarge)
                    val providers = LlmProvider.entries
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        providers.forEachIndexed { i, p ->
                            SegmentedButton(
                                selected = settings.llmProvider == p,
                                onClick = { act("AI: ${p.label()}") { DemoActions.setLlmProvider(p); true } },
                                shape = SegmentedButtonDefaults.itemShape(i, providers.size),
                            ) { Text(p.label()) }
                        }
                    }
                    Hint("Bez klíče v local.properties se ověření odloží na později. Pro demo bez klíče zapni „vždy ANO“ níže.")
                }

                // Pohyb a gamifikace
                Section("Pohyb a XP") {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        OutlinedButton(onClick = { act("+1 000 kroků") { DemoActions.addSteps() } }, modifier = Modifier.weight(1f)) {
                            Text("+1 000 kroků")
                        }
                        OutlinedButton(onClick = { act("+500 XP") { DemoActions.addXp() } }, modifier = Modifier.weight(1f)) {
                            Text("+500 XP")
                        }
                    }
                    Text("Postava", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        AvatarState.entries.forEach { a ->
                            FilterChip(
                                selected = game.avatar == a,
                                onClick = { act("Postava: ${a.label()}") { DemoActions.setAvatar(a) } },
                                label = { Text(a.label()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.positiveContainer,
                                    selectedLabelColor = colors.onPositiveContainer,
                                ),
                            )
                        }
                    }
                }

                // Nebezpečné
                Surface(
                    color = colors.debtContainer,
                    contentColor = colors.onDebtContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(spacing.md)) {
                        ToggleRow(
                            "Ověření úkolů vždy ANO",
                            "Jen pro demo. Nikdy nenechat zapnuté.",
                            settings.demoAlwaysYes,
                        ) { on -> act("Vždy ANO ${if (on) "zapnuto" else "vypnuto"}") { DemoActions.setAlwaysYes(on); true } }
                    }
                }

                OutlinedButton(
                    onClick = { act("Den resetován") { DemoActions.resetDay(); true } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Reset dne") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val msg = DemoActions.loadMockData()
                            snackbar.currentSnackbarData?.dismiss()
                            snackbar.showSnackbar(msg)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Nahrát mock data (21 dní historie)") }
                Hint("Jednou: zásahy, minuty feedu, XP a série za 21 dní + hodnocení videí z mockdata/.")
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val spacing = NarsilTheme.spacing
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, ok: Boolean? = null) {
    val colors = NarsilTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (ok != null) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (ok) colors.positive else colors.debt, CircleShape),
            )
            Box(Modifier.size(8.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (ok == false) colors.debt else Color.Unspecified,
        )
    }
}

@Composable
private fun ToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun AvatarState.label() = when (this) {
    AvatarState.EXHAUSTED -> "Vyčerpaný"
    AvatarState.TIRED -> "Unavený"
    AvatarState.OK -> "V pohodě"
    AvatarState.FIT -> "Fit"
    AvatarState.THRIVING -> "Skvělý"
}

private fun LlmProvider.label() = when (this) {
    LlmProvider.CLAUDE -> "Claude"
    LlmProvider.GEMINI -> "Gemini"
    LlmProvider.OPENAI -> "ChatGPT"
}
