package cz.narsil.ui.screens.onboarding

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.components.NeuPanel
import cz.narsil.ui.components.NeuPrimaryButton
import cz.narsil.ui.components.NeuSmallButton
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.components.OnSky
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.aeroSurface
import cz.narsil.ui.theme.neuInset

/**
 * Onboarding flow for Home Wi-Fi setup, one-sentence permissions, and MIUI/Redmi steps.
 * Logic wiring is handled in task 122. Vzhled Frutiger Aero: nadpisy na nebi, karty ve skle.
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
    OnSky {
        Column(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NeuText("Nastavení Narsilu", size = 30.sp, weight = FontWeight.Bold, letterSpacing = (-0.8).sp, lineHeight = 34.sp)
            NeuText(
                "Narsil tě chrání jen doma, kdy chceš mít klid. Venku a ve zprávách mlčí.",
                size = 14.sp,
                weight = FontWeight.Medium,
                color = NeuColors.Muted,
                lineHeight = 19.sp,
            )

            // Privacy reassurance banner
            NeuPanel(Modifier.fillMaxWidth(), radius = 18.dp, depth = NeuDepth.Deep, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                NeuText(
                    "Soukromí: Zprávy a chaty nikdy nečteme. Všechna videa zůstávají jen v telefonu.",
                    size = 13.sp,
                    weight = FontWeight.SemiBold,
                    color = NeuColors.OnAccent,
                    lineHeight = 18.sp,
                )
            }

            // Home Wi-Fi card
            NeuPanel(Modifier.fillMaxWidth(), gap = 10.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeuText("Domácí Wi-Fi", size = 16.sp, weight = FontWeight.Bold)
                    if (state.homeWifiSsid != null) StatusPill("Nastaveno")
                }
                NeuText(state.homeWifiStatusText, size = 13.sp, weight = FontWeight.Medium, color = NeuColors.Muted, lineHeight = 18.sp)
                NeuPrimaryButton(state.homeWifiButtonLabel, onClick = onConfirmHomeWifi, enabled = !state.isOnHomeWifi)
            }

            // Permissions list (1 sentence each)
            NeuText("Potřebná oprávnění", size = 18.sp, weight = FontWeight.Bold)

            NeuPanel(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp), gap = 0.dp) {
                state.permissions.forEachIndexed { index, permission ->
                    if (index > 0) cz.narsil.ui.components.NeuDivider()
                    PermissionRow(
                        item = permission,
                        onRequest = { onRequestPermission(permission.type) }
                    )
                }
            }

            // MIUI / Redmi specific steps
            if (state.isMiuiDevice) {
                NeuPanel(Modifier.fillMaxWidth(), gap = 6.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeuText("Kroky pro Xiaomi / MIUI (Redmi)", Modifier.weight(1f), size = 16.sp, weight = FontWeight.Bold)
                        NeuSmallButton(if (state.miuiStepsExpanded) "Skrýt" else "Zobrazit", onToggleMiuiExpanded)
                    }

                    AnimatedVisibility(visible = state.miuiStepsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            NeuText(
                                "Xiaomi jinak Narsil po chvíli vypne a hlídání přestane fungovat. Klepnutím otevřeš správné nastavení:",
                                size = 12.sp,
                                weight = FontWeight.Medium,
                                color = NeuColors.Muted,
                                lineHeight = 17.sp,
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
                                NeuText(
                                    "Otevři přehled aplikací (přejeď zdola nahoru a podrž), podrž kartu Narsil a klepni na zámek.",
                                    size = 12.sp,
                                    weight = FontWeight.Medium,
                                    color = NeuColors.Muted,
                                    lineHeight = 17.sp,
                                )
                            }
                        }
                    }
                }
            }

            OnboardingVideoCard(state = state.video, onVideoButton = onVideoButton, cameraSlot = videoCameraSlot)

            NeuPrimaryButton(
                if (state.allRequiredGranted) "Dokončit nastavení" else "Pokračovat na hlavní obrazovku",
                onClick = onFinishOnboarding,
            )
        }
    }
}

/** Zelená bublina se stavem („Nastaveno“, „Uloženo“, „Povoleno“). */
@Composable
private fun StatusPill(text: String) {
    Box(Modifier.neuInset(NeuDepth.Deep, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        NeuText(text, size = 12.sp, weight = FontWeight.Bold, color = NeuColors.OnAccent, maxLines = 1)
    }
}

/** Nepovinné video pro Ozvěnu (139): vzkaz sobě, který Ozvěna přehraje, než natočíš video ve smlouvě. */
@Composable
private fun OnboardingVideoCard(
    state: OnboardingVideoState,
    onVideoButton: () -> Unit,
    cameraSlot: @Composable () -> Unit
) {
    NeuPanel(Modifier.fillMaxWidth(), gap = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuText("Video pro Ozvěnu", size = 16.sp, weight = FontWeight.Bold)
            if (state.hasVideo) StatusPill("Uloženo")
        }
        NeuText(
            "Nepovinné. Řekni do ${state.maxSeconds} s, proč chceš mít večer klid od feedu. " +
                "Ozvěna ti to přehraje, než natočíš video ve večerní smlouvě.",
            size = 13.sp,
            weight = FontWeight.Medium,
            color = NeuColors.Muted,
            lineHeight = 18.sp,
        )
        if (state.cameraOpen) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .aeroSurface(AeroStyle.Frost, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
            ) {
                cameraSlot()
            }
        }
        state.error?.let {
            NeuText(it, size = 12.sp, weight = FontWeight.Bold, color = NeuColors.Error)
        }
        NeuSmallButton(state.buttonLabel, onVideoButton, Modifier.fillMaxWidth(), height = 44.dp)
    }
}

@Composable
private fun PermissionRow(
    item: PermissionItem,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            NeuText(item.title, size = 14.sp, weight = FontWeight.Bold)
            NeuText(item.oneSentenceReason, size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted, lineHeight = 16.sp)
        }

        if (item.isGranted) {
            StatusPill("Povoleno")
        } else {
            NeuSmallButton("Povolit", onRequest)
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
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = NeuColors.Success,
                uncheckedColor = NeuColors.Label,
                checkmarkColor = Color.White,
            ),
        )
        NeuText(label, Modifier.weight(1f), size = 14.sp, weight = FontWeight.SemiBold)
    }
}

@Preview(name = "Onboarding – Světlý režim", showBackground = true)
@Composable
private fun OnboardingLightPreview() {
    NarsilTheme(darkTheme = false) { AeroBackdrop {
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
    } }
}

@Preview(
    name = "Onboarding – Tmavý režim",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun OnboardingDarkPreview() {
    NarsilTheme(darkTheme = true) { AeroBackdrop {
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
    } }
}
