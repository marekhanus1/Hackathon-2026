package cz.narsil.ui.screens.squat

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import cz.narsil.ui.components.NarsilProgressBar
import cz.narsil.ui.components.StatusPill
import cz.narsil.ui.theme.NarsilTheme

/**
 * Live squat counting screen.
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
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    val safeTarget = state.targetSquats.coerceAtLeast(1)
    val progress by animateFloatAsState(
        targetValue = (state.completedSquats.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f),
        label = "squatProgress"
    )

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
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text(text = "Zavřít místo dřepů")
                }

                StatusPill(
                    text = "+${state.earnedXp} XP · ${state.unlockedMinutes} min",
                    containerColor = colors.emberContainer,
                    contentColor = colors.onEmberContainer
                )
            }

            // Camera preview container with overlay counter
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (state.isCompleted) colors.positive else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    cameraPreviewSlot()

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(spacing.md),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    ) {
                        Text(
                            text = state.phase.instruction,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(spacing.md),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${state.completedSquats} / ${state.targetSquats}",
                                style = MaterialTheme.typography.displayLarge,
                                color = if (state.isCompleted) colors.positive else colors.ember
                            )
                            Text(
                                text = "dřepů hotovo",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Progress bar
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
private fun DefaultCameraPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Náhled kamery\n(ML Kit Pose Detection)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(name = "Dřepy – Průběh (Světlý)", showBackground = true)
@Composable
private fun SquatInProgressLightPreview() {
    NarsilTheme(darkTheme = false) {
        SquatScreen(
            state = SquatUiState(),
            onCancel = {},
            onFinishContinue = {}
        )
    }
}

@Preview(
    name = "Dřepy – Hotovo (Tmavý)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SquatCompletedDarkPreview() {
    NarsilTheme(darkTheme = true) {
        SquatScreen(
            state = SquatUiState(
                completedSquats = 20,
                phase = SquatPhase.COMPLETED,
                feedbackMessage = "Skvělá práce! Přesunul ses z hlavy do těla."
            ),
            onCancel = {},
            onFinishContinue = {}
        )
    }
}
