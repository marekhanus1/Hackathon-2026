package cz.narsil.ui.screens.pushup

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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
 * High-contrast floor-readable pushup screen with live anti-cheat placement status (Frutiger Aero):
 * průhledná obrazovka na nebi, stav položení v lesklé bublině (zelená = připraveno, jantarová = varování),
 * náhled kamery v rámu s bílým okrajem a obří počítadlo v lesklé kouli.
 */
@Composable
fun PushupScreen(
    state: PushupUiState,
    onCancel: () -> Unit,
    onFinishContinue: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreviewSlot: @Composable () -> Unit = { DefaultPushupCameraPlaceholder() }
) {
    val spacing = NarsilTheme.spacing
    val safeTarget = state.targetPushups.coerceAtLeast(1)
    val progress by animateFloatAsState(
        targetValue = (state.completedPushups.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f),
        label = "pushupProgress"
    )
    val placementReady = state.placementStatus == PushupPlacementStatus.READY

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
                AeroGlassButton(text = "Zavřít místo kliků", onClick = onCancel)
                AeroRewardPill("+${state.earnedXp} XP · ${state.unlockedMinutes} min")
            }

            // Anti-cheat placement banner clearly readable from plank height
            OnGlass {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aeroSurface(if (placementReady) AeroStyle.GreenBubble else AeroStyle.AmberGloss, RoundedCornerShape(22.dp))
                        .padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    val textColor = if (placementReady) NeuColors.OnAccent else WarningText
                    NeuText(state.placementStatus.label, size = 17.sp, weight = FontWeight.Bold, color = textColor)
                    NeuText(state.placementStatus.hint, size = 14.sp, weight = FontWeight.Medium, color = textColor, lineHeight = 19.sp)
                }
            }

            // Camera preview + huge floor-legible counter
            val frame = RoundedCornerShape(26.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .aeroSurface(AeroStyle.Glass, frame)
                    .clip(frame)
                    .background(Brush.verticalGradient(listOf(Color(0xFF0B5C8E), Color(0xFF06324F))))
                    .border(
                        width = 2.5.dp,
                        color = when {
                            state.isCompleted -> Color(0xFF8CF55F)
                            state.proximityNear -> NeuColors.Warning
                            placementReady -> Color.White
                            else -> Color(0x80FFFFFF)
                        },
                        shape = frame
                    )
            ) {
                cameraPreviewSlot()

                if (!state.personVisible && placementReady) {
                    OnGlass {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(spacing.md)
                                .aeroSurface(AeroStyle.AmberGloss, CircleShape)
                                .padding(horizontal = spacing.md, vertical = spacing.xs)
                        ) {
                            NeuText("Nastav hrudník nad přední kameru", size = 14.sp, weight = FontWeight.Bold, color = WarningText)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(spacing.md)
                        .aeroSurface(if (state.isCompleted) AeroStyle.GreenOrb else AeroStyle.BlueOrb, RoundedCornerShape(36.dp))
                        .padding(horizontal = spacing.xl, vertical = spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NeuText(
                        "${state.completedPushups} / ${state.targetPushups}",
                        size = 60.sp,
                        weight = FontWeight.ExtraBold,
                        lineHeight = 66.sp,
                        maxLines = 1
                    )
                    NeuText("kliků hotovo", size = 16.sp, weight = FontWeight.Bold, color = NeuColors.Muted)
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

/** Tmavě hnědý text na jantarovém lesku (varování). */
private val WarningText = Color(0xFF5A3300)

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

@Composable
private fun DefaultPushupCameraPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        OnSky {
            NeuText(
                "Přední kamera + senzory\n(polož telefon na zem)",
                size = 16.sp,
                weight = FontWeight.Bold,
                color = NeuColors.Muted,
                align = TextAlign.Center
            )
        }
    }
}

@Preview(name = "Kliky – Na zemi (Světlý)", showBackground = true)
@Composable
private fun PushupReadyLightPreview() {
    NarsilTheme(darkTheme = false) { AeroBackdrop {
        PushupScreen(
            state = PushupUiState(),
            onCancel = {},
            onFinishContinue = {}
        )
    } }
}

@Preview(
    name = "Kliky – Anti-cheat varování (Tmavý)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PushupMovingDarkPreview() {
    NarsilTheme(darkTheme = true) { AeroBackdrop {
        PushupScreen(
            state = PushupUiState(
                completedPushups = 8,
                placementStatus = PushupPlacementStatus.MOVING,
                feedbackMessage = "Zastav pohyb telefonem, ať můžeme pokračovat."
            ),
            onCancel = {},
            onFinishContinue = {}
        )
    } }
}
