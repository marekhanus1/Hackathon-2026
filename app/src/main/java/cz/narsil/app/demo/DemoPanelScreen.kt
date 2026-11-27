package cz.narsil.app.demo

import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.narsil.app.EchoOverlayStarter
import cz.narsil.app.ServiceLocator
import cz.narsil.core.AvatarState
import cz.narsil.core.ComfortContext
import cz.narsil.detection.accessibility.AccessibilityEvents
import cz.narsil.detection.accessibility.AccessibilityTriggerSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Demo panel (123): příprava dema za 30 s. Texty česky, jen pro tým, ne pro porotu. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DemoPanelScreen(onBack: () -> Unit) {
    val sl = ServiceLocator
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by sl.settingsRepository.settings.collectAsState()
    val comfort by sl.contextRepository.context.collectAsState()
    val manual by sl.contextRepository.manualOverride.collectAsState()
    val serviceOn by AccessibilityEvents.serviceConnected.collectAsState()
    val movement by sl.movementRepository.state.collectAsState()
    val game by sl.gamificationEngine.state.collectAsState()
    val unlockedUntil by sl.unlockTimer.unlockedUntil.collectAsState()
    val session = (sl.interventionTriggerSource as? AccessibilityTriggerSource)?.current?.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = System.currentTimeMillis() } }

    fun act(label: String, block: suspend () -> Boolean) {
        scope.launch { message = if (block()) "$label: hotovo" else "$label: nejde (modul není fake / služba neběží)" }
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Demo panel", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Zpět") }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Stav", fontWeight = FontWeight.SemiBold)
                Text("Služba přístupnosti: ${if (serviceOn) "běží" else "VYPNUTÁ"} · Ozvěna ${if (EchoOverlayStarter.isReady) "připravená" else "nepřipravená"}")
                Text("Překrytí oken: ${if (AndroidSettings.canDrawOverlays(context)) "povoleno" else "nepovoleno"}")
                Text("Kontext: $comfort${manual?.let { " (ručně)" } ?: ""} · domov: ${settings.homeSsid ?: "neuložen"}")
                Text("Práh zásahu: ${settings.demoThresholdSec?.let { "$it s" } ?: "5 min"}")
                Text("Relace: ${session?.value?.let { "${it.surface} ${(now - it.startedAtMillis) / 1000} s" } ?: "žádná"}")
                Text("Odemčeno: ${((unlockedUntil - now) / 1000).takeIf { it > 0 }?.let { "ještě $it s" } ?: "ne"}")
                Text("Kroky ${movement.stepsToday} · zůstatek ${movement.balanceMinutes} min · dluh ${movement.debtMinutes} min")
                Text("XP ${game.xp} · úroveň ${game.level} · série ${game.streakDays} · postava ${game.avatar}")
                Text("Moduly: ${if (sl.useFakes) "vše fake" else "skutečná detekce a kontext, pohyb a gamifikace fake"}")
            }
        }

        Button(onClick = { act("Připravit demo") { DemoActions.prepareDemo(); true } }, modifier = Modifier.fillMaxWidth()) {
            Text("Připravit demo (doma, 20 s, smlouva dnes, čistý den)")
        }
        Button(onClick = { act("Ozvěna teď") { DemoActions.showEchoNow() } }, modifier = Modifier.fillMaxWidth()) {
            Text("Ukázat Ozvěnu teď")
        }

        Text("Kontext", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ComfortContext.HOME to "Doma", ComfortContext.AWAY to "Venku", null to "Automaticky").forEach { (c, label) ->
                FilterChip(selected = manual == c, onClick = { act(label) { DemoActions.setContext(c); true } }, label = { Text(label) })
            }
        }

        ToggleRow("Práh 20 s místo 5 min", settings.demoThresholdSec != null) { on -> act("Práh") { DemoActions.setShortThreshold(on); true } }
        ToggleRow("Smlouva platí hned (dnes)", settings.demoContractToday) { on -> act("Smlouva") { DemoActions.setContractToday(on); true } }

        Text("Pohyb a gamifikace", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { act("+1 000 kroků") { DemoActions.addSteps() } }) { Text("+1 000 kroků") }
            OutlinedButton(onClick = { act("+500 XP") { DemoActions.addXp() } }) { Text("+500 XP") }
        }
        Text("Postava")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AvatarState.entries.forEach { a ->
                FilterChip(selected = game.avatar == a, onClick = { act("Postava") { DemoActions.setAvatar(a) } }, label = { Text(a.name) })
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
            ToggleRow(
                "⚠ Ověření úkolů vždy ANO (jen demo)",
                settings.demoAlwaysYes,
                modifier = Modifier.padding(12.dp),
            ) { on -> act("Vždy ANO") { DemoActions.setAlwaysYes(on); true } }
        }

        OutlinedButton(onClick = { act("Reset dne") { DemoActions.resetDay(); true } }, modifier = Modifier.fillMaxWidth()) {
            Text("Reset dne")
        }
        Text("Mock data (430) zatím nejsou k dispozici.", style = MaterialTheme.typography.bodySmall)
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
