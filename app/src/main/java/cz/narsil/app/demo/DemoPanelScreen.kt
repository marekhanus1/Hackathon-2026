package cz.narsil.app.demo

import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import cz.narsil.ui.components.NeuDivider
import cz.narsil.ui.components.NeuHeader
import cz.narsil.ui.components.NeuLabel
import cz.narsil.ui.components.NeuPanel
import cz.narsil.ui.components.NeuSegmented
import cz.narsil.ui.components.NeuSmallButton
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.components.OnGlass
import cz.narsil.ui.components.OnSky
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.aeroSurface
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
            containerColor = Color.Transparent,
        ) { padding ->
          OnSky {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Hlavička
                NeuHeader(
                    title = "Demo panel",
                    subtitle = "Příprava dema. Porota ho nevidí.",
                    onBack = onBack,
                )

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
                    subtitle = "Doma, 0 minut (Ozvěna po 5 s), úkoly neodevzdané, dřepy",
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
                    NeuSegmented(
                        options = options,
                        selected = options.firstOrNull { it.first == manual },
                        label = { it.second },
                        onSelect = { (c, label) -> act("Kontext: $label") { DemoActions.setContext(c); true } },
                    )
                    Hint("Ruční volba platí do 4:00. Automaticky = podle domácí Wi-Fi.")
                }

                // Přepínače
                Section("Demo nastavení") {
                    ToggleRow("Krátký práh", "Ozvěna po 20 s místo 5 min", settings.demoThresholdSec != null) { on ->
                        act(if (on) "Práh 20 s" else "Práh 5 min") { DemoActions.setShortThreshold(on); true }
                    }
                    NeuDivider()
                    ToggleRow("Smlouva platí hned", "Večerní smlouva se uloží na dnešek", settings.demoContractToday) { on ->
                        act("Smlouva ${if (on) "na dnešek" else "na zítřek"}") { DemoActions.setContractToday(on); true }
                    }
                    NeuDivider()
                    NeuText("Cvik v Ozvěně", size = 14.sp, weight = FontWeight.Bold)
                    NeuSegmented(
                        options = Exercise.entries,
                        selected = settings.exercise,
                        label = { ExerciseText.label(it) },
                        onSelect = { e -> act("Cvik: ${ExerciseText.label(e)}") { DemoActions.setExercise(e); true } },
                    )
                    Hint("Dřepy: 20 opakování, telefon opřený naproti. Kliky: 10 opakování, telefon leží na zemi pod hrudníkem displejem nahoru.")
                    NeuDivider()
                    NeuText("AI pro ověření úkolů", size = 14.sp, weight = FontWeight.Bold)
                    NeuSegmented(
                        options = LlmProvider.entries,
                        selected = settings.llmProvider,
                        label = { it.label() },
                        onSelect = { p -> act("AI: ${p.label()}") { DemoActions.setLlmProvider(p); true } },
                    )
                    Hint("Bez klíče v local.properties se ověření odloží na později. Pro demo bez klíče zapni „vždy ANO“ níže.")
                }

                // Pohyb a gamifikace
                Section("Pohyb a XP") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeuSmallButton("+1 000 kroků", { act("+1 000 kroků") { DemoActions.addSteps() } }, Modifier.weight(1f), height = 42.dp)
                        NeuSmallButton("+500 XP", { act("+500 XP") { DemoActions.addXp() } }, Modifier.weight(1f), height = 42.dp)
                    }
                    NeuLabel("Postava")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AvatarState.entries.forEach { a ->
                            DemoChip(a.label(), selected = game.avatar == a) {
                                act("Postava: ${a.label()}") { DemoActions.setAvatar(a) }
                            }
                        }
                    }
                }

                // Nebezpečné
                OnGlass {
                    val shape = RoundedCornerShape(22.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .aeroSurface(AeroStyle.Glass, shape)
                            .border(2.dp, Color(0x99C2412F), shape)
                            .padding(16.dp),
                    ) {
                        ToggleRow(
                            "Ověření úkolů vždy ANO",
                            "Jen pro demo. Nikdy nenechat zapnuté.",
                            settings.demoAlwaysYes,
                            danger = true,
                        ) { on -> act("Vždy ANO ${if (on) "zapnuto" else "vypnuto"}") { DemoActions.setAlwaysYes(on); true } }
                    }
                }

                NeuSmallButton(
                    "Reset dne",
                    onClick = { act("Den resetován") { DemoActions.resetDay(); true } },
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp,
                )
                NeuSmallButton(
                    "Nahrát mock data (21 dní historie)",
                    onClick = {
                        scope.launch {
                            val msg = DemoActions.loadMockData()
                            snackbar.currentSnackbarData?.dismiss()
                            snackbar.showSnackbar(msg)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp,
                )
                Hint("Smaže historii a nahraje znovu: zásahy, minuty feedu, XP a série za 21 dní + hodnocení videí. Smlouva na dnes zůstane.")
            }
          }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    NeuPanel(Modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(16.dp), gap = 10.dp) {
        NeuText(title, size = 16.sp, weight = FontWeight.ExtraBold)
        content()
    }
}

@Composable
private fun StatusRow(label: String, value: String, ok: Boolean? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (ok != null) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (ok) Color(0xFF36C246) else NeuColors.Error, CircleShape),
            )
            Box(Modifier.size(8.dp))
        }
        NeuText(label, Modifier.weight(1f), size = 13.sp, weight = FontWeight.Medium, color = NeuColors.Muted)
        NeuText(value, size = 13.sp, weight = FontWeight.Bold, color = if (ok == false) NeuColors.Error else NeuColors.Text)
    }
}

@Composable
private fun ToggleRow(title: String, description: String, checked: Boolean, danger: Boolean = false, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            NeuText(title, size = 14.sp, weight = FontWeight.Bold, color = if (danger) NeuColors.Error else NeuColors.Text)
            NeuText(description, size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted, lineHeight = 17.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = if (danger) NeuColors.Error else Color(0xFF36C246),
                checkedBorderColor = if (danger) NeuColors.Error else Color(0xFF1E9E3A),
                uncheckedThumbColor = Color(0xFF7FA3B0),
                uncheckedTrackColor = Color(0xFFD6E6EE),
                uncheckedBorderColor = Color(0x520078B5),
            ),
        )
    }
}

/** Volba postavy: vybraná bílo-zelená bublina, ostatní matné sklo. */
@Composable
private fun DemoChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .aeroSurface(if (selected) AeroStyle.GreenBubble else AeroStyle.Frost, shape)
            .clip(shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        NeuText(text, size = 12.sp, weight = FontWeight.ExtraBold, color = if (selected) NeuColors.OnAccent else NeuColors.Label, maxLines = 1)
    }
}

@Composable
private fun Hint(text: String) {
    NeuText(text, size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted, lineHeight = 17.sp)
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
