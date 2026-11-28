package cz.narsil.ui.screens.verify

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.components.NeuHeader
import cz.narsil.ui.components.NeuIcons
import cz.narsil.ui.components.NeuLabel
import cz.narsil.ui.components.NeuPanel
import cz.narsil.ui.components.NeuPrimaryButton
import cz.narsil.ui.components.NeuScreen
import cz.narsil.ui.components.NeuSegmented
import cz.narsil.ui.components.NeuSmallButton
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.components.NeuTextField
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.neuInset

/**
 * Ověření úkolu ve stylu E (334, dříve 324/541). Nahoře záložky úkolů dnešní smlouvy – jde přepnout,
 * na co odpovídáš. Důkaz: fotka úkolu, fotka prostředí nebo vlastní popis; stavy
 * PENDING / VERIFYING / VERIFIED / REJECTED / VERIFY_LATER. Obrazovka se neposouvá.
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
    animateRewardOnVerified: Boolean = true,
    /** „Pokračovat“ / „Další úkol“ na kartě ověřeného úkolu; výchozí = zpět. */
    onContinue: () -> Unit = onBack,
    /** Přepnutí úkolu v záložkách nahoře. */
    onSelectTask: (Long) -> Unit = {},
) {
    NeuScreen(modifier, gap = 14.dp) {
        NeuHeader(title = "Ověření úkolu", onBack = onBack, backDescription = "Zpět")

        val tabs = state.tasks.filter { it.canVerify || it.id == state.selectedTaskId }
        if (tabs.size > 1) {
            NeuSegmented(
                options = tabs.map { it.id },
                selected = state.selectedTaskId,
                label = { id -> tabs.first { it.id == id }.tabLabel },
                onSelect = onSelectTask,
            )
        }

        TaskCard(state)

        when (state.status) {
            VerificationStatus.PENDING -> PendingSection(
                state, onSelectProofMode, onTextProofChange, onCapturePhoto, onRetakePhoto, onSubmitProof, cameraPreviewSlot,
            )
            VerificationStatus.VERIFYING -> {
                StatusPanel {
                    CircularProgressIndicator(color = NeuColors.Accent, modifier = Modifier.size(44.dp))
                    NeuText("Ověřuji důkaz…", size = 16.sp, weight = FontWeight.ExtraBold)
                    NeuText(
                        "Chvilku strpení, kontrolujeme splnění úkolu.",
                        size = 13.sp,
                        weight = FontWeight.Medium,
                        color = NeuColors.Muted,
                        align = TextAlign.Center,
                    )
                }
                NeuPrimaryButton("Ověřuji…", onClick = {}, enabled = false)
            }
            VerificationStatus.VERIFIED -> {
                VerifiedPanel(state, animateRewardOnVerified)
                NeuPrimaryButton(
                    if (state.nextTaskTitle != null) "Další úkol: ${state.nextTaskTitle}" else "Pokračovat",
                    onClick = onContinue,
                    color = NeuColors.Success,
                    contentColor = NeuColors.OnSuccess,
                )
            }
            VerificationStatus.REJECTED -> {
                StatusPanel {
                    StatusIcon { Icon(NeuIcons.Close, null, tint = NeuColors.Error, modifier = Modifier.size(30.dp)) }
                    NeuText("Důkaz zatím nestačil", size = 16.sp, weight = FontWeight.ExtraBold)
                    NeuText(
                        state.statusMessage.ifBlank {
                            "Zkus vyfotit jasnější detail, prostředí, nebo popsat ve 2–3 větách, co přesně je hotové."
                        },
                        size = 13.sp,
                        weight = FontWeight.Medium,
                        color = NeuColors.Muted,
                        align = TextAlign.Center,
                        lineHeight = 19.sp,
                    )
                    NeuText(
                        if (state.remainingAttempts > 0) "Zbývající pokusy: ${state.remainingAttempts} z ${state.maxAttempts}"
                        else "Vyčerpány všechny ${state.maxAttempts} pokusy pro tento úkol.",
                        size = 12.sp,
                        weight = FontWeight.Bold,
                        color = NeuColors.Error,
                    )
                }
                if (state.remainingAttempts > 0) NeuPrimaryButton("Zkusit znovu", onClick = onRetry)
                else NeuPrimaryButton("Zpět", onClick = onBack)
            }
            VerificationStatus.VERIFY_LATER -> {
                StatusPanel {
                    StatusIcon { Icon(NeuIcons.Pause, null, tint = NeuColors.Muted, modifier = Modifier.size(28.dp)) }
                    NeuText("Důkaz uložen – ověříme později", size = 16.sp, weight = FontWeight.ExtraBold, align = TextAlign.Center)
                    NeuText(
                        state.statusMessage.ifBlank {
                            "Momentálně není dostupné spojení. Jakmile se telefon připojí, úkol automaticky zkontrolujeme " +
                                "a odměnu (+${state.rewardMinutes} min) připíšeme."
                        },
                        size = 13.sp,
                        weight = FontWeight.Medium,
                        color = NeuColors.Muted,
                        align = TextAlign.Center,
                        lineHeight = 19.sp,
                    )
                }
                NeuPrimaryButton("Zpět na přehled", onClick = onBack)
            }
        }
    }
}

@Composable
private fun TaskCard(state: VerifyTaskUiState) {
    val item = state.tasks.firstOrNull { it.id == state.selectedTaskId }
    NeuPanel(Modifier.fillMaxWidth(), radius = 24.dp, padding = PaddingValues(16.dp), gap = 6.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            NeuLabel(state.taskType.label)
            NeuText("+${state.rewardMinutes} min", size = 13.sp, weight = FontWeight.ExtraBold, color = NeuColors.Success)
        }
        NeuText(state.taskTitle, size = 18.sp, weight = FontWeight.ExtraBold, lineHeight = 23.sp, maxLines = 3)
        if (state.taskGoal.isNotBlank()) {
            NeuText(state.taskGoal, size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted, maxLines = 2)
        }
        val attempts = when {
            state.status == VerificationStatus.VERIFIED -> null
            item != null -> item.attemptsLine
            state.attemptsUsed > 0 -> "Pokus ${(state.attemptsUsed + 1).coerceAtMost(state.maxAttempts)} ze ${state.maxAttempts}"
            else -> "Pokus 1 ze ${state.maxAttempts}"
        }
        if (attempts != null) {
            NeuText(
                attempts,
                size = 12.sp,
                weight = FontWeight.Bold,
                color = if (item?.isWarning == true || state.status == VerificationStatus.REJECTED) NeuColors.Error else NeuColors.Muted,
            )
        }
    }
}

private val ProofMode.tabLabel: String
    get() = when (this) {
        ProofMode.PHOTO -> "Fotka úkolu"
        ProofMode.ROOM -> "Prostředí"
        ProofMode.TEXT -> "Popis"
    }

@Composable
private fun ColumnScope.PendingSection(
    state: VerifyTaskUiState,
    onSelectProofMode: (ProofMode) -> Unit,
    onTextProofChange: (String) -> Unit,
    onCapturePhoto: () -> Unit,
    onRetakePhoto: () -> Unit,
    onSubmitProof: () -> Unit,
    cameraPreviewSlot: @Composable () -> Unit,
) {
    NeuSegmented(
        options = ProofMode.entries.toList(),
        selected = state.proofMode,
        label = { it.tabLabel },
        onSelect = onSelectProofMode,
    )

    if (state.proofMode.isPhoto) {
        Box(Modifier.weight(1f).fillMaxWidth().neuInset(NeuDepth.Normal, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))) {
            if (state.photoCaptured) {
                Box(Modifier.fillMaxSize()) { cameraPreviewSlot() }
                Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StatusIcon { Icon(NeuIcons.Check, null, tint = NeuColors.Success, modifier = Modifier.size(30.dp)) }
                    NeuText("Fotka připravena", size = 15.sp, weight = FontWeight.ExtraBold)
                    NeuSmallButton("Vyfotit znovu", onRetakePhoto)
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StatusIcon { Icon(NeuIcons.Camera, null, tint = NeuColors.Accent, modifier = Modifier.size(34.dp)) }
                    NeuText(
                        state.proofMode.hint,
                        Modifier.widthIn(max = 280.dp),
                        size = 13.sp,
                        weight = FontWeight.Medium,
                        color = NeuColors.Muted,
                        align = TextAlign.Center,
                        lineHeight = 19.sp,
                    )
                }
            }
        }
        if (state.photoCaptured) {
            NeuPrimaryButton("Odeslat k ověření", onClick = onSubmitProof, enabled = state.canSubmit)
        } else {
            NeuPrimaryButton("Vyfotit a ověřit", onClick = onCapturePhoto, enabled = state.remainingAttempts > 0)
        }
    } else {
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NeuText(state.proofMode.hint, Modifier.padding(horizontal = 4.dp), size = 12.sp, weight = FontWeight.Bold, color = NeuColors.Muted)
            NeuTextField(
                value = state.textProof,
                onValueChange = onTextProofChange,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                placeholder = "Např. Odevzdal jsem úlohu #3 v IS. Prošly všechny dotazy kromě posledního, ten jsem opravil podle cvičení.",
                singleLine = false,
                radius = 24.dp,
            )
            NeuText(
                "${state.textProof.length} / ${state.maxTextLength} znaků · " +
                    if (state.missingTextChars > 0) "ještě ${state.missingTextChars}" else "délka stačí",
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                size = 11.sp,
                weight = FontWeight.SemiBold,
                color = if (state.missingTextChars > 0) NeuColors.Muted else NeuColors.Success,
                align = TextAlign.End,
            )
        }
        NeuPrimaryButton("Odeslat k ověření", onClick = onSubmitProof, enabled = state.canSubmit)
    }
}

@Composable
private fun ColumnScope.StatusPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.weight(1f).fillMaxWidth().neuInset(NeuDepth.Normal, RoundedCornerShape(28.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun StatusIcon(content: @Composable () -> Unit) {
    Box(Modifier.size(84.dp).neuInset(NeuDepth.Deep, CircleShape), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ColumnScope.VerifiedPanel(state: VerifyTaskUiState, animateOnStart: Boolean) {
    var appeared by remember { mutableStateOf(!animateOnStart) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "verifyRewardScale",
    )
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .neuInset(NeuDepth.Normal, RoundedCornerShape(28.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(NeuColors.Success), contentAlignment = Alignment.Center) {
            Icon(NeuIcons.Check, null, tint = NeuColors.OnSuccess, modifier = Modifier.size(34.dp))
        }
        NeuText(
            if (state.rewardXp > 0) "+${state.rewardMinutes} min · +${state.rewardXp} XP" else "+${state.rewardMinutes} min",
            size = 22.sp,
            weight = FontWeight.ExtraBold,
            color = NeuColors.Success,
        )
        NeuText("Úkol splněn a ověřen!", size = 16.sp, weight = FontWeight.ExtraBold)
        NeuText(
            state.statusMessage.ifBlank { "Odměna se připsala do tvého dnešního rozpočtu. Dobrá práce!" },
            size = 13.sp,
            weight = FontWeight.Medium,
            color = NeuColors.Muted,
            align = TextAlign.Center,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun DefaultVerifyCameraPlaceholder(@Suppress("UNUSED_PARAMETER") state: VerifyTaskUiState) {
    // Fotka z systémového fotoaparátu se nenáhleduje; stav ukazuje „Fotka připravena“.
}

private fun previewTasks() = listOf(
    TaskItem(2, "Laboratorní úloha z databází", TaskType.SCHOOL, goal = "Odevzdaný SQL skript nebo zápisky z cvičení"),
    TaskItem(3, "Dopsat report do práce", TaskType.WORK, status = TaskItemStatus.REJECTED, attemptsUsed = 1),
    TaskItem(4, "Uklidit pokoj", TaskType.OTHER),
)

@Preview(name = "Ověření – fotka", widthDp = 390, heightDp = 844)
@Composable
private fun VerifyPendingPreview() {
    NarsilTheme {
        VerifyTaskScreen(
            state = VerifyTaskUiState(tasks = previewTasks(), selectedTaskId = 2),
            onSelectProofMode = {}, onTextProofChange = {}, onCapturePhoto = {}, onRetakePhoto = {},
            onSubmitProof = {}, onRetry = {}, onBack = {},
        )
    }
}

@Preview(name = "Ověření – popis", widthDp = 390, heightDp = 844)
@Composable
private fun VerifyTextPreview() {
    NarsilTheme {
        VerifyTaskScreen(
            state = VerifyTaskUiState(
                proofMode = ProofMode.TEXT,
                textProof = "Dopsal jsem tři SQL dotazy na spojení tabulek a odevzdal je do Moodlu.",
                tasks = previewTasks(),
                selectedTaskId = 2,
            ),
            onSelectProofMode = {}, onTextProofChange = {}, onCapturePhoto = {}, onRetakePhoto = {},
            onSubmitProof = {}, onRetry = {}, onBack = {},
        )
    }
}

@Preview(name = "Ověření – splněno", widthDp = 390, heightDp = 844)
@Composable
private fun VerifyVerifiedPreview() {
    NarsilTheme {
        VerifyTaskScreen(
            state = VerifyTaskUiState(status = VerificationStatus.VERIFIED, attemptsUsed = 1, rewardXp = 30),
            onSelectProofMode = {}, onTextProofChange = {}, onCapturePhoto = {}, onRetakePhoto = {},
            onSubmitProof = {}, onRetry = {}, onBack = {}, animateRewardOnVerified = false,
        )
    }
}

@Preview(name = "Ověření – zamítnuto", widthDp = 390, heightDp = 844)
@Composable
private fun VerifyRejectedPreview() {
    NarsilTheme {
        VerifyTaskScreen(
            state = VerifyTaskUiState(status = VerificationStatus.REJECTED, attemptsUsed = 1),
            onSelectProofMode = {}, onTextProofChange = {}, onCapturePhoto = {}, onRetakePhoto = {},
            onSubmitProof = {}, onRetry = {}, onBack = {},
        )
    }
}
