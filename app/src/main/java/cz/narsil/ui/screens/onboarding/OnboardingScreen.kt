package cz.narsil.ui.screens.onboarding

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme

/**
 * Onboarding flow for Home Wi-Fi setup, one-sentence permissions, and MIUI/Redmi steps.
 * Logic wiring is handled in task 122.
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onConfirmHomeWifi: () -> Unit,
    onRequestPermission: (PermissionType) -> Unit,
    onToggleMiuiExpanded: () -> Unit,
    onToggleMiuiAutoStart: () -> Unit,
    onToggleMiuiBattery: () -> Unit,
    onToggleMiuiBackgroundPopup: () -> Unit,
    onToggleMiuiLock: () -> Unit = {},
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    onVideoButton: () -> Unit = {},
    videoCameraSlot: @Composable () -> Unit = {},
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
            Text(
                text = "Nastavení Narsilu",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Narsil tě chrání jen doma, kdy chceš mít klid. Venku a ve zprávách mlčí.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Privacy reassurance banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = colors.echoContainer,
                contentColor = colors.onEchoContainer
            ) {
                Text(
                    text = "Soukromí: Zprávy a chaty nikdy nečteme. Všechna videa zůstávají jen v telefonu.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(spacing.md)
                )
            }

            // Home Wi-Fi card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (state.homeWifiSsid != null) colors.positive else MaterialTheme.colorScheme.outlineVariant
                )
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
                            text = "Domácí Wi-Fi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (state.homeWifiSsid != null) {
                            Surface(
                                shape = CircleShape,
                                color = colors.positiveContainer,
                                contentColor = colors.onPositiveContainer
                            ) {
                                Text(
                                    text = "Nastaveno",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                                )
                            }
                        }
                    }

                    Text(
                        text = state.homeWifiStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onConfirmHomeWifi,
                        enabled = !state.isOnHomeWifi,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.homeWifiSsid != null) colors.positive else colors.ember,
                            contentColor = if (state.homeWifiSsid != null) colors.onPositive else colors.onEmber
                        )
                    ) {
                        Text(text = state.homeWifiButtonLabel)
                    }
                }
            }

            // Permissions list (1 sentence each)
            Text(
                text = "Potřebná oprávnění",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                state.permissions.forEach { permission ->
                    PermissionRowCard(
                        item = permission,
                        onRequest = { onRequestPermission(permission.type) }
                    )
                }
            }

            // MIUI / Redmi specific steps
            if (state.isMiuiDevice) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = colors.emberContainer,
                        contentColor = colors.onEmberContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kroky pro Xiaomi / MIUI (Redmi)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onToggleMiuiExpanded) {
                                Text(if (state.miuiStepsExpanded) "Skrýt" else "Zobrazit")
                            }
                        }

                        AnimatedVisibility(visible = state.miuiStepsExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                                Text(
                                    text = "Xiaomi jinak Narsil po chvíli vypne a hlídání přestane fungovat. Klepnutím otevřeš správné nastavení:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                MiuiCheckRow(
                                    checked = state.miuiAutoStartDone,
                                    label = "Povolit automatické spouštění",
                                    onToggle = onToggleMiuiAutoStart
                                )
                                MiuiCheckRow(
                                    checked = state.miuiBatteryDone,
                                    label = "Vypnout spořič baterie (bez omezení)",
                                    onToggle = onToggleMiuiBattery
                                )
                                MiuiCheckRow(
                                    checked = state.miuiBackgroundPopupDone,
                                    label = "Povolit vyskakovací okna na pozadí",
                                    onToggle = onToggleMiuiBackgroundPopup
                                )
                                MiuiCheckRow(
                                    checked = state.miuiLockDone,
                                    label = "Zamknout Narsil v posledních aplikacích",
                                    onToggle = onToggleMiuiLock
                                )
                                if (!state.miuiLockDone) {
                                    Text(
                                        text = "Otevři přehled aplikací (přejeď zdola nahoru a podrž), podrž kartu Narsil a klepni na zámek.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OnboardingVideoCard(state = state.video, onVideoButton = onVideoButton, cameraSlot = videoCameraSlot)

            Button(
                onClick = onFinishOnboarding,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.positive,
                    contentColor = colors.onPositive
                )
            ) {
                Text(
                    text = if (state.allRequiredGranted) {
                        "Dokončit nastavení"
                    } else {
                        "Pokračovat na hlavní obrazovku"
                    }
                )
            }
        }
    }
}

/** Nepovinné video pro Ozvěnu (139): vzkaz sobě, který Ozvěna přehraje, než natočíš video ve smlouvě. */
@Composable
private fun OnboardingVideoCard(
    state: OnboardingVideoState,
    onVideoButton: () -> Unit,
    cameraSlot: @Composable () -> Unit
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (state.hasVideo) colors.positive else MaterialTheme.colorScheme.outlineVariant)
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
                Text(text = "Video pro Ozvěnu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (state.hasVideo) {
                    Surface(shape = CircleShape, color = colors.positiveContainer, contentColor = colors.onPositiveContainer) {
                        Text(
                            text = "Uloženo",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                        )
                    }
                }
            }
            Text(
                text = "Nepovinné. Řekni do ${state.maxSeconds} s, proč chceš mít večer klid od feedu. " +
                    "Ozvěna ti to přehraje, než natočíš video ve večerní smlouvě.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.cameraOpen) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    cameraSlot()
                }
            }
            state.error?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(onClick = onVideoButton, modifier = Modifier.fillMaxWidth()) {
                Text(state.buttonLabel)
            }
        }
    }
}

@Composable
private fun PermissionRowCard(
    item: PermissionItem,
    onRequest: () -> Unit
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isGranted) colors.positive.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.oneSentenceReason,
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
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs)
                    )
                }
            } else {
                OutlinedButton(onClick = onRequest) {
                    Text("Povolit")
                }
            }
        }
    }
}

@Composable
private fun MiuiCheckRow(
    checked: Boolean,
    label: String,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(name = "Onboarding – Světlý režim", showBackground = true)
@Composable
private fun OnboardingLightPreview() {
    NarsilTheme(darkTheme = false) {
        OnboardingScreen(
            state = OnboardingUiState(),
            onConfirmHomeWifi = {},
            onRequestPermission = {},
            onToggleMiuiExpanded = {},
            onToggleMiuiAutoStart = {},
            onToggleMiuiBattery = {},
            onToggleMiuiBackgroundPopup = {},
            onFinishOnboarding = {}
        )
    }
}

@Preview(
    name = "Onboarding – Tmavý režim",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun OnboardingDarkPreview() {
    NarsilTheme(darkTheme = true) {
        OnboardingScreen(
            state = OnboardingUiState(
                homeWifiSsid = "Domov_5G",
                miuiAutoStartDone = true,
                miuiBatteryDone = true
            ),
            onConfirmHomeWifi = {},
            onRequestPermission = {},
            onToggleMiuiExpanded = {},
            onToggleMiuiAutoStart = {},
            onToggleMiuiBattery = {},
            onToggleMiuiBackgroundPopup = {},
            onFinishOnboarding = {}
        )
    }
}
