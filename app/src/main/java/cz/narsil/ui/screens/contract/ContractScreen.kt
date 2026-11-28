package cz.narsil.ui.screens.contract

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cz.narsil.core.PlanRules
import cz.narsil.core.TaskType
import cz.narsil.ui.components.NeuBadgeIcon
import cz.narsil.ui.components.NeuDivider
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
import java.text.NumberFormat
import java.util.Locale

/**
 * Večerní smlouva ve stylu E (334): jedna obrazovka místo průvodce. Pohybový cíl, úkoly (posouvá se jen
 * seznam ve vaně, přidání v dialogu), plánovaný obsah, 15s video Ozvěny (nahrávání v překryvu
 * s náhledem kamery [cameraPreviewSlot]) a „Podepsat smlouvu“.
 *
 * Parametry kroků ([onNextStep], [onPreviousStep]) zůstávají kvůli kompatibilitě route, obrazovka je nepoužívá.
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
    @Suppress("UNUSED_PARAMETER") onNextStep: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onPreviousStep: () -> Unit,
    onSubmitContract: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreviewSlot: @Composable () -> Unit = { DefaultContractCameraPlaceholder(state) },
    onSelectTaskType: (TaskType) -> Unit = {},
    onTaskDraftChange: (String) -> Unit = {},
    onAddTask: (TaskType, String) -> Unit = { _, _ -> },
    onRemoveTask: (TaskDraft) -> Unit = {},
) {
    var recorderOpen by rememberSaveable { mutableStateOf(false) }
    var taskDialog by rememberSaveable { mutableStateOf(false) }
    var contentDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        NeuScreen(gap = 14.dp) {
            NeuHeader(
                title = "Večerní smlouva",
                subtitle = "Rozhodni teď v klidu.",
                onBack = onBackToHome,
                backDescription = "Zpět domů",
                trailing = {
                    Box(Modifier.neuInset(NeuDepth.Deep, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        NeuText(if (state.targetIsToday) "Na dnes" else "Na zítra", size = 12.sp, weight = FontWeight.ExtraBold, color = NeuColors.Accent)
                    }
                },
            )

            if (state.isSubmitted) {
                SubmittedPanel(state, onBackToHome)
            } else {
                GoalPanel(state, onAdjustGoalAmount)
                TasksPanel(state, onRemoveTask, onAdd = { taskDialog = true }, modifier = Modifier.weight(1f))
                PlannedContentPanel(state, onRemovePlannedItem, onAdd = { contentDialog = true })
                VideoRow(state, onOpen = { recorderOpen = true })
                NeuPrimaryButton("Podepsat smlouvu", onClick = onSubmitContract, enabled = !state.isRecordingVideo)
            }
        }

        if (recorderOpen) {
            RecorderOverlay(
                state = state,
                onToggleRecording = onToggleRecording,
                onClose = {
                    if (state.isRecordingVideo) onToggleRecording()
                    recorderOpen = false
                },
                cameraPreviewSlot = cameraPreviewSlot,
            )
        }
    }

    if (taskDialog) {
        AddTaskDialog(
            state = state,
            onSelectType = onSelectTaskType,
            onDraftChange = onTaskDraftChange,
            onAdd = { type, title ->
                onAddTask(type, title)
                taskDialog = false
            },
            onDismiss = { taskDialog = false },
        )
    }
    if (contentDialog) {
        AddContentDialog(
            state = state,
            onDraftChange = onDraftChange,
            onAdd = { item ->
                onAddPlannedItem(item)
                contentDialog = false
            },
            onDismiss = { contentDialog = false },
        )
    }
}

@Composable
private fun GoalPanel(state: ContractUiState, onAdjust: (Int) -> Unit) {
    val delta = if (state.goalType == MovementGoalType.STEPS) 500 else state.goalType.stepDelta
    NeuPanel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                NeuLabel("Pohybový cíl")
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NeuText(formatNumber(state.goalAmount), size = 20.sp, weight = FontWeight.ExtraBold)
                    NeuText(state.goalType.unit, Modifier.padding(bottom = 2.dp), size = 13.sp, weight = FontWeight.Bold, color = NeuColors.Muted)
                }
            }
            StepButton("−", "Snížit cíl o $delta", NeuColors.Text) { onAdjust(-delta) }
            StepButton("+", "Zvýšit cíl o $delta", NeuColors.Accent) { onAdjust(delta) }
        }
    }
}

@Composable
private fun StepButton(text: String, description: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(44.dp)
            .neuInset(NeuDepth.Deep, shape)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        NeuText(text, size = 20.sp, weight = FontWeight.ExtraBold, color = color)
    }
}

private val TaskType.short: String
    get() = when (this) {
        TaskType.MOVEMENT -> "PO"
        TaskType.SCHOOL -> "ŠK"
        TaskType.WORK -> "PR"
        TaskType.OTHER -> "OS"
    }

@Composable
private fun TasksPanel(state: ContractUiState, onRemove: (TaskDraft) -> Unit, onAdd: () -> Unit, modifier: Modifier) {
    NeuPanel(modifier.fillMaxWidth(), radius = 28.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), gap = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            NeuText(if (state.targetIsToday) "Úkoly na dnešek" else "Úkoly na zítřek", size = 15.sp, weight = FontWeight.ExtraBold)
            NeuText("${state.tasks.size} / ${PlanRules.MAX_TASKS}", size = 12.sp, weight = FontWeight.Bold, color = NeuColors.Muted)
        }
        if (state.tasks.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                NeuText(
                    "Přidej, co chceš ${state.dayWord} stihnout. Za každý ověřený úkol dostaneš minuty navíc.",
                    size = 13.sp,
                    weight = FontWeight.Medium,
                    color = NeuColors.Muted,
                    align = TextAlign.Center,
                    lineHeight = 19.sp,
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(state.tasks, key = { "${it.type}-${it.title}" }) { task ->
                    NeuDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        NeuBadgeIcon(task.type.short, size = 34.dp, radius = 11.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            NeuText(task.title, size = 14.sp, weight = FontWeight.Bold, maxLines = 1)
                            NeuText(
                                "${task.type.label} · +${task.rewardMinutes} min · fotka nebo popis",
                                size = 11.sp,
                                weight = FontWeight.SemiBold,
                                color = NeuColors.Muted,
                                maxLines = 1,
                            )
                        }
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button) { onRemove(task) }
                                .semantics { contentDescription = "Odebrat úkol ${task.title}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(NeuIcons.Close, contentDescription = null, tint = NeuColors.Muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        if (state.canAddTask) {
            Box(Modifier.padding(top = 6.dp)) {
                NeuSmallButton("+ Přidat úkol", onAdd, Modifier.fillMaxWidth(), height = 42.dp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlannedContentPanel(state: ContractUiState, onRemove: (String) -> Unit, onAdd: () -> Unit) {
    NeuPanel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        NeuLabel("Plánovaný obsah zdarma")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.plannedItems.forEach { item ->
                Row(
                    Modifier
                        .neuInset(NeuDepth.Deep, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClickLabel = "Odebrat", role = Role.Button) { onRemove(item) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NeuText(item, size = 12.sp, weight = FontWeight.Bold, maxLines = 1)
                    Icon(NeuIcons.Close, contentDescription = "Odebrat $item", tint = NeuColors.Muted, modifier = Modifier.size(12.dp))
                }
            }
            if (state.plannedItems.size < PlanRules.MAX_PLANNED_CONTENT) {
                Box(
                    Modifier
                        .border(1.dp, NeuColors.Dashed, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClick = onAdd)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    NeuText("+ Přidat", size = 12.sp, weight = FontWeight.Bold, color = NeuColors.Accent)
                }
            }
        }
    }
}

@Composable
private fun VideoRow(state: ContractUiState, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .neuInset(NeuDepth.Normal, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).neuInset(NeuDepth.Deep, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                NeuIcons.Video,
                contentDescription = null,
                tint = if (state.hasRecordedVideo) NeuColors.Success else NeuColors.Accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            NeuText("Tvoje 15s Ozvěna", size = 14.sp, weight = FontWeight.Bold)
            NeuText(
                when {
                    state.isRecordingVideo -> "Nahrávám ${state.recordedSeconds}/${state.maxVideoSeconds} s"
                    state.hasRecordedVideo -> (if (state.recordedSeconds > 0) "Uloženo (${state.recordedSeconds} s)" else "Uloženo") + " · nahrát znovu"
                    else -> "Zatím nenahráno · klepni a nahraj"
                },
                size = 11.sp,
                weight = FontWeight.Bold,
                color = if (state.hasRecordedVideo) NeuColors.Success else NeuColors.Muted,
            )
        }
        Icon(NeuIcons.Chevron, contentDescription = null, tint = NeuColors.Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun RecorderOverlay(
    state: ContractUiState,
    onToggleRecording: () -> Unit,
    onClose: () -> Unit,
    cameraPreviewSlot: @Composable () -> Unit,
) {
    BackHandler(onBack = onClose)
    NeuScreen(Modifier.clickable(enabled = false) {}, gap = 14.dp) {
        NeuHeader(title = "Tvoje 15s Ozvěna", onBack = onClose, backDescription = "Zavřít nahrávání")
        NeuText(
            if (state.targetIsToday) "Nahraj krátké 15s video pro sebe v pokušení: jak se teď cítíš a proč chceš mít dnes klid."
            else "Nahraj krátké 15s video pro své zítřejší já: jak se dnes cítíš a proč chceš mít zítra klid.",
            Modifier.padding(horizontal = 4.dp),
            size = 13.sp,
            weight = FontWeight.Medium,
            color = NeuColors.Muted,
            lineHeight = 19.sp,
        )
        Box(Modifier.weight(1f).fillMaxWidth().neuInset(NeuDepth.Normal, RoundedCornerShape(28.dp)).padding(10.dp)) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(NeuColors.Deep)) {
                cameraPreviewSlot()
                if (state.isRecordingVideo || state.hasRecordedVideo) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (state.isRecordingVideo) NeuColors.Accent else NeuColors.Success)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        NeuText(
                            if (state.isRecordingVideo) "Nahrávám ${state.recordedSeconds}/${state.maxVideoSeconds} s"
                            else if (state.recordedSeconds > 0) "Uloženo (${state.recordedSeconds} s)" else "Uloženo",
                            size = 12.sp,
                            weight = FontWeight.ExtraBold,
                            color = if (state.isRecordingVideo) NeuColors.OnAccent else NeuColors.OnSuccess,
                        )
                    }
                }
            }
        }
        state.videoError?.let { NeuText(it, size = 12.sp, weight = FontWeight.Bold, color = NeuColors.Error) }
        NeuPrimaryButton(
            when {
                state.isRecordingVideo -> "Ukončit nahrávání"
                state.hasRecordedVideo -> "Nahrát zprávu znovu"
                else -> "Nahrát 15s video Ozvěny"
            },
            onClick = onToggleRecording,
        )
        if (state.hasRecordedVideo && !state.isRecordingVideo) {
            NeuSmallButton("Hotovo", onClose, Modifier.fillMaxWidth(), color = NeuColors.Success, height = 44.dp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTaskDialog(
    state: ContractUiState,
    onSelectType: (TaskType) -> Unit,
    onDraftChange: (String) -> Unit,
    onAdd: (TaskType, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val suggestions = listOf(
        TaskType.MOVEMENT to "Běh nebo procházka 20 min",
        TaskType.SCHOOL to "Úkol do školy",
        TaskType.WORK to "Hodina soustředěné práce",
        TaskType.OTHER to "Uklidit pokoj",
    )
    NeuDialog(title = "Nový úkol", onDismiss = onDismiss) {
        NeuSegmented(
            options = TaskType.entries.toList(),
            selected = state.taskDraftType,
            label = { it.label },
            onSelect = onSelectType,
        )
        NeuText(
            "${state.taskDraftType.label} · +${state.taskDraftType.defaultRewardMinutes} min za ověření",
            Modifier.padding(horizontal = 4.dp),
            size = 12.sp,
            weight = FontWeight.Bold,
            color = NeuColors.Success,
        )
        NeuTextField(state.taskDraftText, onDraftChange, Modifier.fillMaxWidth(), placeholder = "Např. kapitola 3 z fyziky")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.filter { (_, t) -> state.tasks.none { it.title.equals(t, ignoreCase = true) } }.forEach { (type, title) ->
                Box(
                    Modifier
                        .border(1.dp, NeuColors.Dashed, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { onAdd(type, title) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) { NeuText("+ $title", size = 12.sp, weight = FontWeight.Bold, color = NeuColors.Muted) }
            }
        }
        NeuPrimaryButton(
            "Přidat úkol",
            onClick = { onAdd(state.taskDraftType, state.taskDraftText) },
            enabled = state.taskDraftText.isNotBlank(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddContentDialog(
    state: ContractUiState,
    onDraftChange: (String) -> Unit,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val suggestions = listOf("Přednáška o Rustu", "Jeden díl seriálu", "Dokument o přírodě").filter { it !in state.plannedItems }
    NeuDialog(title = "Plánovaný obsah", onDismiss = onDismiss) {
        NeuText(
            "Co si ${state.dayWord} pustíš zdarma – Ozvěna u toho nezasáhne.",
            Modifier.padding(horizontal = 4.dp),
            size = 12.sp,
            weight = FontWeight.Medium,
            color = NeuColors.Muted,
        )
        NeuTextField(state.draftItemText, onDraftChange, Modifier.fillMaxWidth(), placeholder = "Např. jeden díl seriálu")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { s ->
                Box(
                    Modifier
                        .border(1.dp, NeuColors.Dashed, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { onAdd(s) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) { NeuText("+ $s", size = 12.sp, weight = FontWeight.Bold, color = NeuColors.Muted) }
            }
        }
        NeuPrimaryButton("Přidat", onClick = { onAdd(state.draftItemText) }, enabled = state.draftItemText.isNotBlank())
    }
}

/** Dialog ve stylu E: podklad, nadpis s křížkem, obsah. */
@Composable
private fun NeuDialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(NeuColors.Base).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeuText(title, Modifier.weight(1f), size = 18.sp, weight = FontWeight.ExtraBold)
                Box(
                    Modifier.size(36.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onDismiss).semantics { contentDescription = "Zavřít" },
                    contentAlignment = Alignment.Center,
                ) { Icon(NeuIcons.Close, contentDescription = null, tint = NeuColors.Muted, modifier = Modifier.size(18.dp)) }
            }
            content()
        }
    }
}

@Composable
private fun SubmittedPanel(state: ContractUiState, onBackToHome: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().neuInset(NeuDepth.Normal, RoundedCornerShape(28.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(NeuColors.Success), contentAlignment = Alignment.Center) {
            Icon(NeuIcons.Check, null, tint = NeuColors.OnSuccess, modifier = Modifier.size(34.dp))
        }
        NeuText("Smlouva podepsaná!", size = 18.sp, weight = FontWeight.ExtraBold)
        NeuText(
            "Až ji ${state.dayWord} splníš (krokový cíl a všechny úkoly ověřené), dostaneš +100 XP. " +
                "Ozvěna tě ${state.dayWord} jemně podpoří tvým vlastním hlasem.",
            size = 13.sp,
            weight = FontWeight.Medium,
            color = NeuColors.Muted,
            align = TextAlign.Center,
            lineHeight = 19.sp,
        )
    }
    NeuPrimaryButton("Zpět na hlavní obrazovku", onClick = onBackToHome, color = NeuColors.Success, contentColor = NeuColors.OnSuccess)
}

@Composable
private fun DefaultContractCameraPlaceholder(state: ContractUiState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        NeuText(
            if (state.hasRecordedVideo) "Video Ozvěny připraveno\n(uloženo lokálně v telefonu)" else "Náhled přední kamery",
            size = 14.sp,
            weight = FontWeight.Medium,
            color = NeuColors.Muted,
            align = TextAlign.Center,
        )
    }
}

private fun formatNumber(n: Int): String = NumberFormat.getIntegerInstance(Locale("cs", "CZ")).format(n)

@Preview(name = "Smlouva – koncept E", widthDp = 390, heightDp = 844)
@Composable
private fun ContractPreview() {
    NarsilTheme {
        ContractScreen(
            state = ContractUiState(
                plannedItems = listOf("Přednáška o Rustu", "Jeden díl seriálu"),
                hasRecordedVideo = true,
                recordedSeconds = 12,
                tasks = listOf(
                    TaskDraft(TaskType.SCHOOL, "Laboratorní úloha z databází"),
                    TaskDraft(TaskType.MOVEMENT, "Běh 5 km"),
                    TaskDraft(TaskType.WORK, "Dopsat report do práce"),
                    TaskDraft(TaskType.OTHER, "Uklidit pokoj"),
                ),
            ),
            onSelectGoalType = {}, onSetGoalAmount = {}, onAdjustGoalAmount = {}, onDraftChange = {},
            onAddPlannedItem = {}, onRemovePlannedItem = {}, onToggleRecording = {}, onNextStep = {},
            onPreviousStep = {}, onSubmitContract = {}, onBackToHome = {},
        )
    }
}
