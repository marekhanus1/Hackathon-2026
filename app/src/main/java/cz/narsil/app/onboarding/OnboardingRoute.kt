package cz.narsil.app.onboarding

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import cz.narsil.app.ServiceLocator
import cz.narsil.movement.steps.HealthConnectStepsSource
import cz.narsil.ui.theme.NarsilTheme
import kotlinx.coroutines.launch

/**
 * Průvodce oprávněními a domácí Wi-Fi (122). Funkční náhrada, dokud se nesloučí Janova obrazovka 320;
 * logika ([PermissionChecker], [OnboardingPrefs]) zůstane i pro ni.
 */
@Composable
fun OnboardingRoute(onDone: () -> Unit) {
    val context = LocalContext.current
    val sl = ServiceLocator
    val scope = rememberCoroutineScope()
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    // Přepočet stavu po návratu z nastavení / dialogu.
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { refresh++ } }

    var healthGranted by remember { mutableStateOf<Boolean?>(null) }
    val healthAvailable = remember { HealthConnectStepsSource.isAvailable(context) }
    val runtime = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh++
        sl.contextRepository.let { repo -> (repo as? cz.narsil.detection.context.WifiContextRepository)?.refresh() }
    }
    val health = rememberLauncherForActivityResult(HealthConnectStepsSource.permissionContract()) { granted ->
        healthGranted = granted.containsAll(HealthConnectStepsSource.PERMISSIONS)
    }

    val wifi by sl.contextRepository.currentWifiSsid.collectAsState()
    val settings by sl.settingsRepository.settings.collectAsState()
    var homeMessage by remember { mutableStateOf<String?>(null) }

    val steps = PermissionChecker.steps()
    val status = remember(refresh, healthGranted) {
        steps.associateWith { step ->
            if (step == PermissionStep.HEALTH_CONNECT) healthGranted else PermissionChecker.isGranted(context, step)
        }
    }
    val requiredDone = steps.filter { it.required }.all { status[it] == true }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text("Vítej v Narsilu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "Pomocník, ne dozorce: zasahuje jen doma a nic neblokuje. Aby to šlo, potřebuje pár oprávnění.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Domácí Wi-Fi
        Card {
            Text("Tohle je můj domov", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Ozvěna zasahuje jen na domácí Wi-Fi. Teď: ${wifi ?: "žádná Wi-Fi (nebo chybí poloha)"} · uložený domov: ${settings.homeSsid ?: "žádný"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                enabled = wifi != null,
                onClick = {
                    scope.launch {
                        homeMessage = if (sl.contextRepository.saveCurrentWifiAsHome()) "Uloženo: $wifi je domov." else "Nejdřív se připoj k domácí Wi-Fi a povol polohu."
                    }
                },
            ) { Text("Uložit tuhle Wi-Fi jako domov") }
            homeMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }

        steps.forEach { step ->
            val granted = status[step]
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(
                                when (granted) {
                                    true -> colors.positive
                                    false -> if (step.required) colors.debt else colors.ember
                                    null -> MaterialTheme.colorScheme.outline
                                },
                                CircleShape,
                            ),
                    )
                    Spacer(Modifier.width(spacing.xs))
                    Text(
                        step.title + if (step.required) "" else " (nepovinné)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (granted == true) Text("hotovo", color = colors.positive, style = MaterialTheme.typography.labelLarge)
                }
                Text(step.why, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (step == PermissionStep.ACCESSIBILITY && granted != true && Build.VERSION.SDK_INT >= 33) {
                    Text(
                        "Když je Narsil v Usnadnění šedý: Informace o aplikaci → ⋮ → Povolit omezená nastavení.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (granted != true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        when {
                            step == PermissionStep.HEALTH_CONNECT && !healthAvailable ->
                                Text("Health Connect není nainstalovaný, kroky půjdou ze senzoru.", style = MaterialTheme.typography.bodySmall)
                            step == PermissionStep.HEALTH_CONNECT ->
                                OutlinedButton(onClick = { runCatching { health.launch(HealthConnectStepsSource.PERMISSIONS) } }) { Text("Povolit") }
                            step.runtime.isNotEmpty() ->
                                OutlinedButton(onClick = { runtime.launch(step.runtime.toTypedArray()) }) { Text("Povolit") }
                            else ->
                                OutlinedButton(onClick = { PermissionChecker.open(context, step) }) { Text("Otevřít nastavení") }
                        }
                        if (step == PermissionStep.ACCESSIBILITY && Build.VERSION.SDK_INT >= 33) {
                            TextButton(onClick = { runCatching { context.startActivity(PermissionChecker.appDetails(context)) } }) {
                                Text("Informace o aplikaci")
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                OnboardingPrefs.setDone(context)
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (requiredDone) "Hotovo" else "Pokračovat i tak") }
        if (!requiredDone) {
            Text(
                "Bez povinných oprávnění Ozvěna nenaskočí. Průvodce najdeš znovu v Nastavení → Onboarding.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val spacing = NarsilTheme.spacing
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) { content() }
    }
}
