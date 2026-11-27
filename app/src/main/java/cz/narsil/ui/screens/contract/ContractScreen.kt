package cz.narsil.ui.screens.contract

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme

/**
 * Three-step Evening Contract ("Večerní smlouva") wizard.
 * Step 3 accepts CameraX preview via [cameraPreviewSlot] (task 212).
 */
@Composable
fun ContractScreen(
    state: ContractUiState,
    onSelectGoalType: (MovementGoalType) -> Unit,
    onSetGoalAmount: (Int) -> Unit,
    onAdjustGoalAmount: (Int) -> Unit,
    onDraftChange: (String) -> Unit,
    onAddPlannedItem: (String) -> Unit,
    onRemovePlannedItem: (String) -> Unit,
    onToggleRecording: () -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onSubmitContract: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreviewSlot: @Composable () -> Unit = { DefaultContractCameraPlaceholder(state) }
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
                Text(
                    text = "Večerní smlouva",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = colors.echoContainer,
                    contentColor = colors.onEchoContainer
                ) {
                    Text(
                        text = "Krok ${state.currentStep.index} ze 3",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                    )
                }
            }

            // Step indicator bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                ContractStep.entries.forEach { step ->
                    val active = step.index <= state.currentStep.index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (active) colors.echo else colors.xpTrack)
                    )
                }
            }

            Text(
                text = state.currentStep.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            when (state.currentStep) {
                ContractStep.MOVEMENT_GOAL -> MovementGoalStep(
                    state = state,
                    onSelectGoalType = onSelectGoalType,
                    onSetGoalAmount = onSetGoalAmount,
                    onAdjustGoalAmount = onAdjustGoalAmount
                )

                ContractStep.PLANNED_CONTENT -> PlannedContentStep(
                    state = state,
                    onDraftChange = onDraftChange,
                    onAddPlannedItem = onAddPlannedItem,
                    onRemovePlannedItem = onRemovePlannedItem
                )

                ContractStep.ECHO_VIDEO -> EchoVideoStep(
                    state = state,
                    onToggleRecording = onToggleRecording,
                    cameraPreviewSlot = cameraPreviewSlot
                )
            }

            if (state.isSubmitted) {
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
                            text = "Smlouva podepsaná! +100 XP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zítra tě Ozvěna jemně podpoří tvým vlastním hlasem.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onBackToHome,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.positive,
                                contentColor = colors.onPositive
                            )
                        ) {
                            Text("Zpět na hlavní obrazovku")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    if (state.currentStep == ContractStep.MOVEMENT_GOAL) {
                        OutlinedButton(
                            onClick = onBackToHome,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Zrušit")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPreviousStep,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Zpět")
                        }
                    }

                    if (state.currentStep == ContractStep.ECHO_VIDEO) {
                        Button(
                            onClick = onSubmitContract,
                            modifier = Modifier.weight(1.4f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.positive,
                                contentColor = colors.onPositive
                            )
                        ) {
                            Text("Podepsat smlouvu (+100 XP)")
                        }
                    } else {
                        Button(
                            onClick = onNextStep,
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Text("Další krok")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovementGoalStep(
    state: ContractUiState,
    onSelectGoalType: (MovementGoalType) -> Unit,
    onSetGoalAmount: (Int) -> Unit,
    onAdjustGoalAmount: (Int) -> Unit
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(
            text = "Rozhoduješ teď v klidu – ne zítra v pokušení.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            MovementGoalType.entries.forEach { type ->
                FilterChip(
                    selected = state.goalType == type,
                    onClick = { onSelectGoalType(type) },
                    label = { Text(type.label) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Text(
                    text = "${state.goalAmount} ${state.goalType.unit}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.ember
                )

                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    OutlinedButton(onClick = { onAdjustGoalAmount(-state.goalType.stepDelta) }) {
                        Text("-${state.goalType.stepDelta}")
                    }
                    OutlinedButton(onClick = { onAdjustGoalAmount(state.goalType.stepDelta) }) {
                        Text("+${state.goalType.stepDelta}")
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    state.goalType.presets.forEach { preset ->
                        FilterChip(
                            selected = state.goalAmount == preset,
                            onClick = { onSetGoalAmount(preset) },
                            label = { Text("$preset") }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlannedContentStep(
    state: ContractUiState,
    onDraftChange: (String) -> Unit,
    onAddPlannedItem: (String) -> Unit,
    onRemovePlannedItem: (String) -> Unit
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    val suggestions = listOf("Přednáška o Rustu", "Jeden díl seriálu", "Dokument o přírodě")

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = "Plánovaný obsah máš zítra na YouTube zdarma. Ostatní videa se platí pohybem.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.draftItemText,
                onValueChange = onDraftChange,
                placeholder = { Text("Např. přednáška o Rustu") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onAddPlannedItem(state.draftItemText) },
                enabled = state.draftItemText.isNotBlank()
            ) {
                Text("Přidat")
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            suggestions.forEach { suggestion ->
                FilterChip(
                    selected = state.plannedItems.contains(suggestion),
                    onClick = { onAddPlannedItem(suggestion) },
                    label = { Text("+ $suggestion") }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            state.plannedItems.forEach { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = colors.positiveContainer,
                    contentColor = colors.onPositiveContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.md, vertical = spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onRemovePlannedItem(item) }) {
                            Text("Odebrat")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EchoVideoStep(
    state: ContractUiState,
    onToggleRecording: () -> Unit,
    cameraPreviewSlot: @Composable () -> Unit
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(
            text = "Nahraj krátké 15s video pro své zítřejší já: jak se dnes cítíš a proč chceš mít zítra klid.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                width = 1.dp,
                color = if (state.hasRecordedVideo) colors.positive else colors.echo.copy(alpha = 0.4f)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                cameraPreviewSlot()

                if (state.isRecordingVideo || state.hasRecordedVideo) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(spacing.sm),
                        shape = CircleShape,
                        color = if (state.isRecordingVideo) colors.ember else colors.positive
                    ) {
                        Text(
                            text = if (state.isRecordingVideo) {
                                "Nahrávám ${state.recordedSeconds}/${state.maxVideoSeconds} s"
                            } else {
                                "Uloženo (${state.recordedSeconds} s)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onPositive,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onToggleRecording,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    state.isRecordingVideo -> "Ukončit nahrávání"
                    state.hasRecordedVideo -> "Nahrát zprávu znovu"
                    else -> "Nahrát 15s video Ozvěny"
                }
            )
        }
    }
}

@Composable
private fun DefaultContractCameraPlaceholder(state: ContractUiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (state.hasRecordedVideo) {
                "Video Ozvěny připraveno\n(uloženo lokálně v telefonu)"
            } else {
                "Náhled přední kamery\n(CameraX)"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(name = "Smlouva – Krok 1 (Světlý)", showBackground = true)
@Composable
private fun ContractStep1LightPreview() {
    NarsilTheme(darkTheme = false) {
        ContractScreen(
            state = ContractUiState(currentStep = ContractStep.MOVEMENT_GOAL),
            onSelectGoalType = {},
            onSetGoalAmount = {},
            onAdjustGoalAmount = {},
            onDraftChange = {},
            onAddPlannedItem = {},
            onRemovePlannedItem = {},
            onToggleRecording = {},
            onNextStep = {},
            onPreviousStep = {},
            onSubmitContract = {},
            onBackToHome = {}
        )
    }
}

@Preview(name = "Smlouva – Krok 2 (Světlý)", showBackground = true)
@Composable
private fun ContractStep2LightPreview() {
    NarsilTheme(darkTheme = false) {
        ContractScreen(
            state = ContractUiState(
                currentStep = ContractStep.PLANNED_CONTENT,
                plannedItems = listOf("Přednáška o Rustu", "Jeden díl seriálu")
            ),
            onSelectGoalType = {},
            onSetGoalAmount = {},
            onAdjustGoalAmount = {},
            onDraftChange = {},
            onAddPlannedItem = {},
            onRemovePlannedItem = {},
            onToggleRecording = {},
            onNextStep = {},
            onPreviousStep = {},
            onSubmitContract = {},
            onBackToHome = {}
        )
    }
}

@Preview(
    name = "Smlouva – Krok 3 (Tmavý)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ContractStep3DarkPreview() {
    NarsilTheme(darkTheme = true) {
        ContractScreen(
            state = ContractUiState(
                currentStep = ContractStep.ECHO_VIDEO,
                recordedSeconds = 14,
                hasRecordedVideo = true
            ),
            onSelectGoalType = {},
            onSetGoalAmount = {},
            onAdjustGoalAmount = {},
            onDraftChange = {},
            onAddPlannedItem = {},
            onRemovePlannedItem = {},
            onToggleRecording = {},
            onNextStep = {},
            onPreviousStep = {},
            onSubmitContract = {},
            onBackToHome = {}
        )
    }
}
