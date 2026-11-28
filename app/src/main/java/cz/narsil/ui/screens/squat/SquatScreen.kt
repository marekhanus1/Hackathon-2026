package cz.narsil.ui.screens.squat

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.components.NeuPanel
import cz.narsil.ui.components.NeuPrimaryButton
import cz.narsil.ui.components.NeuProgress
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.components.OnGlass
import cz.narsil.ui.components.OnSky
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.aeroSurface

/**
 * Live squat counting screen (Frutiger Aero): průhledná obrazovka na nebi (pozadí kreslí AeroBackdrop
 * aplikace / okna), náhled kamery v zaobleném rámu s bílým okrajem, velké počítadlo v lesklé kouli.
 * CameraX + ML Kit Pose preview is provided via [cameraPreviewSlot] (task 210).
 */
@Composable
fun SquatScreen(
    state: SquatUiState,
    onCancel: () -> Unit,
    onFinishContinue: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreviewSlot: @Composable () -> Unit = { DefaultCameraPlaceholder() }
) {
    val spacing = NarsilTheme.spacing
    val safeTarget = state.targetSquats.coerceAtLeast(1)
    val progress by animateFloatAsState(
        targetValue = (state.completedSquats.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f),
        label = "squatProgress"
    )

    OnSky {
        Column(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AeroGlassButton(text = "Zavřít místo dřepů", onClick = onCancel)
                AeroRewardPill("+${state.earnedXp} XP · ${state.unlockedMinutes} min")
            }

            // Náhled kamery v rámu se zaoblením a bílým okrajem; nahoře instrukce, dole počítadlo.
            val frame = RoundedCornerShape(26.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .aeroSurface(AeroStyle.Glass, frame)
                    .clip(frame)
                    .background(Brush.verticalGradient(listOf(Color(0xFF0B5C8E), Color(0xFF06324F))))
                    .border(2.dp, if (state.isCompleted) Color(0xFF8CF55F) else Color(0xEBFFFFFF), frame)
            ) {
                cameraPreviewSlot()

                AeroToast(
                    text = state.phase.instruction,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(spacing.md)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(spacing.md)
                        .aeroSurface(if (state.isCompleted) AeroStyle.GreenOrb else AeroStyle.BlueOrb, RoundedCornerShape(30.dp))
                        .padding(horizontal = spacing.lg, vertical = spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NeuText(
                        "${state.completedSquats} / ${state.targetSquats}",
                        size = 52.sp,
                        weight = FontWeight.ExtraBold,
                        lineHeight = 58.sp,
                        maxLines = 1
                    )
                    NeuText("dřepů hotovo", size = 14.sp, weight = FontWeight.Bold, color = NeuColors.Muted)
                }
            }

            NeuProgress(progress, height = 14.dp)

            if (state.isCompleted) {
                NeuPanel(Modifier.fillMaxWidth(), gap = spacing.sm) {
                    NeuText(
                        "Hotovo! +${state.earnedXp} XP · ${state.unlockedMinutes} min odemčeno",
                        size = 16.sp,
                        weight = FontWeight.Bold,
                        color = NeuColors.Success
                    )
                    NeuText(state.feedbackMessage, size = 14.sp, weight = FontWeight.Medium, color = NeuColors.Muted, lineHeight = 19.sp)
                    NeuPrimaryButton("Pokračovat", onClick = onFinishContinue)
                }
            } else {
                NeuPanel(Modifier.fillMaxWidth()) {
                    NeuText(
                        state.feedbackMessage,
                        Modifier.fillMaxWidth(),
                        size = 16.sp,
                        weight = FontWeight.Bold,
                        align = TextAlign.Center,
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

/** Modré skleněné tlačítko s bílým textem (`.aero-icon-button`). */
@Composable
private fun AeroGlassButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        Modifier
            .height(44.dp)
            .aeroSurface(AeroStyle.BlueGlass, shape)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        OnSky { NeuText(text, size = 14.sp, weight = FontWeight.Bold, maxLines = 1) }
    }
}

/** Bílo-zelená bublina s odměnou. */
@Composable
private fun AeroRewardPill(text: String) {
    Box(
        Modifier
            .aeroSurface(AeroStyle.GreenBubble, CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        OnGlass { NeuText(text, size = 13.sp, weight = FontWeight.Bold, color = NeuColors.OnAccent, maxLines = 1) }
    }
}

/** Toast: tmavě modrá průsvitná pilulka s bílým textem. */
@Composable
private fun AeroToast(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Color(0xCC063B5C))
            .border(1.dp, Color(0x66FFFFFF), CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
    }
}

@Composable
private fun DefaultCameraPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        OnSky {
            NeuText(
                "Náhled kamery\n(ML Kit Pose Detection)",
                size = 16.sp,
                weight = FontWeight.Bold,
                color = NeuColors.Muted,
                align = TextAlign.Center
            )
        }
    }
}

@Preview(name = "Dřepy – Průběh (Světlý)", showBackground = true)
@Composable
private fun SquatInProgressLightPreview() {
    NarsilTheme(darkTheme = false) { AeroBackdrop {
        SquatScreen(
            state = SquatUiState(),
            onCancel = {},
            onFinishContinue = {}
        )
    } }
}

@Preview(
    name = "Dřepy – Hotovo (Tmavý)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SquatCompletedDarkPreview() {
    NarsilTheme(darkTheme = true) { AeroBackdrop {
        SquatScreen(
            state = SquatUiState(
                completedSquats = 20,
                phase = SquatPhase.COMPLETED,
                feedbackMessage = "Skvělá práce! Přesunul ses z hlavy do těla."
            ),
            onCancel = {},
            onFinishContinue = {}
        )
    } }
}
