package cz.narsil.ui.screens.pushup

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.core.Exercise
import cz.narsil.core.MovementRates
import cz.narsil.ui.components.NarsilProgressBar
import cz.narsil.ui.components.StatusPill
import cz.narsil.ui.theme.NarsilTheme

/**
 * High-contrast floor-readable pushup screen with live anti-cheat placement status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushupScreen(
    state: PushupUiState,
    onCancel: () -> Unit,
    onFinishContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectExercise: (Exercise) -> Unit = {},
    cameraPreviewSlot: @Composable () -> Unit = { DefaultPushupCameraPlaceholder() }
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    val safeTarget = state.targetPushups.coerceAtLeast(1)
    val progress by animateFloatAsState(
        targetValue = (state.completedPushups.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f),
        label = "pushupProgress"
    )
    val placementReady = state.placementStatus == PushupPlacementStatus.READY

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text(text = "Zavřít místo kliků")
                }

                StatusPill(
                    text = "+${state.earnedXp} XP · ${state.unlockedMinutes} min",
                    containerColor = colors.emberContainer,
                    contentColor = colors.onEmberContainer
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = false,
                    onClick = { onSelectExercise(Exercise.SQUATS) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) {
                    Text(text = "Dřepy (${MovementRates.SQUATS_PER_UNLOCK})")
                }
                SegmentedButton(
                    selected = true,
                    onClick = { onSelectExercise(Exercise.PUSHUPS) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) {
                    Text(text = "Kliky (${state.targetPushups})")
                }
            }

            // Anti-cheat placement banner clearly readable from plank height
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = if (placementReady) colors.positiveContainer else colors.debtContainer,
                contentColor = if (placementReady) colors.onPositiveContainer else colors.onDebtContainer
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    Text(
                        text = state.placementStatus.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.placementStatus.hint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Camera preview + huge floor-legible counter
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = 2.dp,
                    color = when {
                        state.isCompleted -> colors.positive
                        state.proximityNear -> colors.goldAccent
                        placementReady -> colors.ember
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    cameraPreviewSlot()

                    if (!state.personVisible && placementReady) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(spacing.md),
                            shape = CircleShape,
                            color = colors.debtContainer,
                            contentColor = colors.onDebtContainer
                        ) {
                            Text(
                                text = "Nastav hrudník nad přední kameru",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(spacing.md),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.md),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${state.completedPushups} / ${state.targetPushups}",
                                style = MaterialTheme.typography.displayLarge,
                                color = if (state.isCompleted) colors.positive else colors.ember
                            )
                            Text(
                                text = "kliků hotovo",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            NarsilProgressBar(
                progress = progress,
                fillColor = if (state.isCompleted) colors.positive else colors.ember,
                height = 12.dp
            )

            if (state.isCompleted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = colors.positiveContainer,
                        contentColor = colors.onPositiveContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        Text(
                            text = "Hotovo! +${state.earnedXp} XP · ${state.unlockedMinutes} min odemčeno",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.feedbackMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onFinishContinue,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.positive,
                                contentColor = colors.onPositive
                            )
                        ) {
                            Text(text = "Pokračovat")
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = colors.emberContainer,
                    contentColor = colors.onEmberContainer
                ) {
                    Text(
                        text = state.feedbackMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(spacing.md)
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultPushupCameraPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Přední kamera + senzory\n(polož telefon na zem)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(name = "Kliky – Na zemi (Světlý)", showBackground = true)
@Composable
private fun PushupReadyLightPreview() {
    NarsilTheme(darkTheme = false) {
        PushupScreen(
            state = PushupUiState(),
            onCancel = {},
            onFinishContinue = {}
        )
    }
}

@Preview(
    name = "Kliky – Anti-cheat varování (Tmavý)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PushupMovingDarkPreview() {
    NarsilTheme(darkTheme = true) {
        PushupScreen(
            state = PushupUiState(
                completedPushups = 8,
                placementStatus = PushupPlacementStatus.MOVING,
                feedbackMessage = "Zastav pohyb telefonem, ať můžeme pokračovat."
            ),
            onCancel = {},
            onFinishContinue = {}
        )
    }
}
