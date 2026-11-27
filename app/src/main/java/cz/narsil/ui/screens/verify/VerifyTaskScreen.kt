package cz.narsil.ui.screens.verify

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme

/**
 * Task verification screen supporting photo or short text (<= 300 chars) proof,
 * plus PENDING / VERIFYING / VERIFIED / REJECTED / VERIFY_LATER states.
 * Ready for TaskVerifier wiring in task 520.
 */
@Composable
fun VerifyTaskScreen(
    state: VerifyTaskUiState,
    onSelectProofMode: (ProofMode) -> Unit,
    onTextProofChange: (String) -> Unit,
    onCapturePhoto: () -> Unit,
    onRetakePhoto: () -> Unit,
    onSubmitProof: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreviewSlot: @Composable () -> Unit = { DefaultVerifyCameraPlaceholder(state) },
    animateRewardOnVerified: Boolean = true
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
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.echoContainer,
                    contentColor = colors.onEchoContainer
                ) {
                    Text(
                        text = state.taskType.label,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = colors.positiveContainer,
                    contentColor = colors.onPositiveContainer
                ) {
                    Text(
                        text = "+${state.rewardMinutes} min",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = state.taskTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.taskGoal,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (state.status) {
                VerificationStatus.VERIFIED -> VerifiedRewardCard(
                    state = state,
                    animateOnStart = animateRewardOnVerified,
                    onBack = onBack
                )

                VerificationStatus.VERIFYING -> VerifyingCard()

                VerificationStatus.REJECTED -> RejectedCard(
                    state = state,
                    onRetry = onRetry,
                    onBack = onBack
                )

                VerificationStatus.VERIFY_LATER -> VerifyLaterCard(
                    state = state,
                    onBack = onBack
                )

                VerificationStatus.PENDING -> PendingProofSection(
                    state = state,
                    onSelectProofMode = onSelectProofMode,
                    onTextProofChange = onTextProofChange,
                    onCapturePhoto = onCapturePhoto,
                    onRetakePhoto = onRetakePhoto,
                    onSubmitProof = onSubmitProof,
                    onBack = onBack,
                    cameraPreviewSlot = cameraPreviewSlot
                )
            }
        }
    }
}

@Composable
private fun PendingProofSection(
    state: VerifyTaskUiState,
    onSelectProofMode: (ProofMode) -> Unit,
    onTextProofChange: (String) -> Unit,
    onCapturePhoto: () -> Unit,
    onRetakePhoto: () -> Unit,
    onSubmitProof: () -> Unit,
    onBack: () -> Unit,
    cameraPreviewSlot: @Composable () -> Unit
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            ProofMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.proofMode == mode,
                    onClick = { onSelectProofMode(mode) },
                    label = { Text(mode.label) }
                )
            }
        }

        if (state.proofMode == ProofMode.PHOTO) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (state.photoCaptured) colors.positive else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    cameraPreviewSlot()
                    if (state.photoCaptured) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(spacing.sm),
                            shape = CircleShape,
                            color = colors.positive,
                            contentColor = colors.onPositive
                        ) {
                            Text(
                                text = "Fotka připravena",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = if (state.photoCaptured) onRetakePhoto else onCapturePhoto,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.photoCaptured) "Vyfotit znovu" else "Vyfotit důkaz")
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                OutlinedTextField(
                    value = state.textProof,
                    onValueChange = onTextProofChange,
                    label = { Text("Popiš, co se povedlo dokončit") },
                    placeholder = { Text("Např. Odevzdal jsem úlohu #3 v IS a prošly všechny dotazy.") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${state.textProof.length} / ${state.maxTextLength} znaků",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Zpět")
            }
            Button(
                onClick = onSubmitProof,
                enabled = state.canSubmit,
                modifier = Modifier.weight(1.6f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.positive,
                    contentColor = colors.onPositive
                )
            ) {
                Text("Odeslat k ověření")
            }
        }
    }
}

@Composable
private fun VerifyingCard() {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = colors.echoContainer,
            contentColor = colors.onEchoContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            CircularProgressIndicator(color = colors.echo)
            Text(
                text = "Ověřuji důkaz…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Chvilku strpení, kontrolujeme splnění úkolu.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VerifiedRewardCard(
    state: VerifyTaskUiState,
    animateOnStart: Boolean,
    onBack: () -> Unit
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    var appeared by remember { mutableStateOf(!animateOnStart) }
    LaunchedEffect(Unit) {
        appeared = true
    }

    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "verifyRewardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = colors.positiveContainer,
            contentColor = colors.onPositiveContainer
        ),
        border = BorderStroke(2.dp, colors.positive)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.positive,
                contentColor = colors.onPositive
            ) {
                Text(
                    text = "+${state.rewardMinutes} min",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)
                )
            }
            Text(
                text = "Úkol splněn a ověřen!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = state.statusMessage.ifBlank {
                    "Odměna se připsala do tvého dnešního rozpočtu. Dobrá práce!"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.positive,
                    contentColor = colors.onPositive
                )
            ) {
                Text("Pokračovat")
            }
        }
    }
}

@Composable
private fun RejectedCard(
    state: VerifyTaskUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = colors.debtContainer,
            contentColor = colors.onDebtContainer
        ),
        border = BorderStroke(1.dp, colors.debt)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = "Důkaz zatím nestačil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = state.statusMessage.ifBlank {
                    "Zkus vyfotit jasnější detail nebo napsat krátký popis, co přesně je hotové."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (state.remainingAttempts > 0) {
                    "Zbývající pokusy: ${state.remainingAttempts} z ${state.maxAttempts}"
                } else {
                    "Vyčerpány všechny ${state.maxAttempts} pokusy pro tento úkol."
                },
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Zpět")
                }
                if (state.remainingAttempts > 0) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.ember,
                            contentColor = colors.onEmber
                        )
                    ) {
                        Text("Zkusit znovu")
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifyLaterCard(
    state: VerifyTaskUiState,
    onBack: () -> Unit
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = colors.echoContainer,
            contentColor = colors.onEchoContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = "Důkaz uložen – ověříme později",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = state.statusMessage.ifBlank {
                    "Momentálně není dostupné spojení. Jakmile se telefon připojí, úkol automaticky zkontrolujeme a odměnu (+${state.rewardMinutes} min) připíšeme."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zpět na přehled")
            }
        }
    }
}

@Composable
private fun DefaultVerifyCameraPlaceholder(state: VerifyTaskUiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (state.photoCaptured) {
                "Snímek důkazu zachycen\n(zmenšeno na 512 px JPEG)"
            } else {
                "Náhled kamery pro vyfocení důkazu"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(name = "Ověření – Foto (Světlý)", showBackground = true)
@Composable
private fun VerifyPendingLightPreview() {
    NarsilTheme(darkTheme = false) {
        VerifyTaskScreen(
            state = VerifyTaskUiState(),
            onSelectProofMode = {},
            onTextProofChange = {},
            onCapturePhoto = {},
            onRetakePhoto = {},
            onSubmitProof = {},
            onRetry = {},
            onBack = {}
        )
    }
}

@Preview(
    name = "Ověření – Splněno (Tmavý)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun VerifyVerifiedDarkPreview() {
    NarsilTheme(darkTheme = true) {
        VerifyTaskScreen(
            state = VerifyTaskUiState(
                status = VerificationStatus.VERIFIED,
                attemptsUsed = 1
            ),
            onSelectProofMode = {},
            onTextProofChange = {},
            onCapturePhoto = {},
            onRetakePhoto = {},
            onSubmitProof = {},
            onRetry = {},
            onBack = {},
            animateRewardOnVerified = false
        )
    }
}

@Preview(name = "Ověření – Zamítnuto (Světlý)", showBackground = true)
@Composable
private fun VerifyRejectedLightPreview() {
    NarsilTheme(darkTheme = false) {
        VerifyTaskScreen(
            state = VerifyTaskUiState(
                proofMode = ProofMode.TEXT,
                status = VerificationStatus.REJECTED,
                attemptsUsed = 1
            ),
            onSelectProofMode = {},
            onTextProofChange = {},
            onCapturePhoto = {},
            onRetakePhoto = {},
            onSubmitProof = {},
            onRetry = {},
            onBack = {}
        )
    }
}

@Preview(
    name = "Ověření – Později (Tmavý)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun VerifyLaterDarkPreview() {
    NarsilTheme(darkTheme = true) {
        VerifyTaskScreen(
            state = VerifyTaskUiState(
                status = VerificationStatus.VERIFY_LATER
            ),
            onSelectProofMode = {},
            onTextProofChange = {},
            onCapturePhoto = {},
            onRetakePhoto = {},
            onSubmitProof = {},
            onRetry = {},
            onBack = {}
        )
    }
}
