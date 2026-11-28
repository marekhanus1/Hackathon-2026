package cz.narsil.ui.screens.contract

import androidx.lifecycle.ViewModel
import cz.narsil.core.DailyContract
import cz.narsil.core.PlanRules
import cz.narsil.core.PlanTask
import cz.narsil.core.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ContractStep(val index: Int, private val baseTitle: String, private val withDay: Boolean) {
    MOVEMENT_GOAL(1, "Pohybový cíl na", true),
    PLANNED_CONTENT(2, "Plánovaný obsah zdarma", false),
    TASKS(3, "Úkoly na", true),
    ECHO_VIDEO(4, "Tvoje 15s Ozvěna", false);

    /** Nadpis kroku podle cílového dne smlouvy (ráno / demo přepínač = dnešek). */
    fun title(targetIsToday: Boolean): String =
        if (withDay) "$baseTitle ${if (targetIsToday) "dnešek" else "zítřek"}" else baseTitle
}

/** Úkol rozepsaný ve smlouvě (139). Odměna a ověření podle [TaskType] (SPEC 6). */
data class TaskDraft(val type: TaskType, val title: String) {
    val rewardMinutes: Int get() = type.defaultRewardMinutes

    fun toPlanTask(): PlanTask = PlanTask(type = type, title = title)
}

/** Popisky typů úkolů v UI. */
val TaskType.label: String
    get() = when (this) {
        TaskType.MOVEMENT -> "Pohyb"
        TaskType.SCHOOL -> "Škola"
        TaskType.WORK -> "Práce"
        TaskType.OTHER -> "Jiné"
    }

enum class MovementGoalType(
    val label: String,
    val unit: String,
    val defaultAmount: Int,
    val stepDelta: Int,
    val presets: List<Int>
) {
    STEPS("Kroky", "kroků", 6000, 1000, listOf(4000, 6000, 8000)),
    ACTIVITY_MINUTES("Minuty aktivity", "min", 30, 10, listOf(20, 30, 45)),
    SQUATS("Dřepy", "dřepů", 30, 10, listOf(20, 30, 50))
}

data class ContractUiState(
    val currentStep: ContractStep = ContractStep.MOVEMENT_GOAL,
    val goalType: MovementGoalType = MovementGoalType.STEPS,
    val goalAmount: Int = 6000,
    val plannedItems: List<String> = listOf("Přednáška o Rustu"),
    val draftItemText: String = "",
    val isRecordingVideo: Boolean = false,
    val recordedSeconds: Int = 0,
    val maxVideoSeconds: Int = 15,
    val hasRecordedVideo: Boolean = false,
    val isSubmitted: Boolean = false,
    /** Úkoly smlouvy (139), nejvýš [PlanRules.MAX_TASKS]. */
    val tasks: List<TaskDraft> = emptyList(),
    val taskDraftType: TaskType = TaskType.MOVEMENT,
    val taskDraftText: String = "",
    /** Hláška kamery (chybí oprávnění, nahrávání selhalo), null = nic. */
    val videoError: String? = null,
    /** Smlouva se uloží na dnešek (ráno před 17:00 nebo demo „Smlouva platí hned“), jinak na zítřek. */
    val targetIsToday: Boolean = false,
) {
    val canAddTask: Boolean get() = tasks.size < PlanRules.MAX_TASKS

    /** „dnes“ / „zítra“ podle cílového dne smlouvy. */
    val dayWord: String get() = if (targetIsToday) "dnes" else "zítra"
}

class ContractViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ContractUiState())
    val uiState: StateFlow<ContractUiState> = _uiState.asStateFlow()

    fun selectGoalType(type: MovementGoalType) {
        _uiState.update { it.copy(goalType = type, goalAmount = type.defaultAmount) }
    }

    fun setGoalAmount(amount: Int) {
        _uiState.update { it.copy(goalAmount = amount.coerceAtLeast(1)) }
    }

    fun adjustGoalAmount(delta: Int) {
        _uiState.update { current ->
            current.copy(goalAmount = (current.goalAmount + delta).coerceAtLeast(current.goalType.stepDelta))
        }
    }

    fun updateDraftItemText(text: String) {
        _uiState.update { it.copy(draftItemText = text) }
    }

    fun addPlannedItem(item: String = _uiState.value.draftItemText) {
        val clean = item.trim()
        if (clean.isEmpty()) return
        _uiState.update { current ->
            if (current.plannedItems.contains(clean)) {
                current.copy(draftItemText = "")
            } else {
                current.copy(
                    plannedItems = current.plannedItems + clean,
                    draftItemText = ""
                )
            }
        }
    }

    fun removePlannedItem(item: String) {
        _uiState.update { current ->
            current.copy(plannedItems = current.plannedItems - item)
        }
    }

    /** Předvyplní smlouvu, která už na ten den existuje (úprava večer). */
    fun prefill(contract: DailyContract) {
        _uiState.update { current ->
            if (current.isSubmitted) return@update current
            current.copy(
                goalType = MovementGoalType.STEPS,
                goalAmount = contract.stepGoal,
                plannedItems = contract.plannedContent,
                tasks = contract.tasks.map { TaskDraft(it.type, it.title) },
                hasRecordedVideo = contract.videoUri != null,
            )
        }
    }

    fun selectTaskType(type: TaskType) {
        _uiState.update { it.copy(taskDraftType = type) }
    }

    fun updateTaskDraftText(text: String) {
        _uiState.update { it.copy(taskDraftText = text) }
    }

    /** Přidá úkol (bez prázdných a duplicit, nejvýš [PlanRules.MAX_TASKS]). */
    fun addTask(type: TaskType = _uiState.value.taskDraftType, title: String = _uiState.value.taskDraftText) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        _uiState.update { current ->
            val duplicate = current.tasks.any { it.title.equals(clean, ignoreCase = true) }
            if (duplicate || !current.canAddTask) current.copy(taskDraftText = "")
            else current.copy(tasks = current.tasks + TaskDraft(type, clean), taskDraftText = "")
        }
    }

    fun removeTask(task: TaskDraft) {
        _uiState.update { it.copy(tasks = it.tasks - task) }
    }

    /** Stav skutečného nahrávání (ContractVideoRecorder) – dodá route. */
    fun onRecordingState(isRecording: Boolean, seconds: Int, hasVideo: Boolean, error: String?) {
        _uiState.update {
            it.copy(isRecordingVideo = isRecording, recordedSeconds = seconds, hasRecordedVideo = hasVideo || (it.hasRecordedVideo && !isRecording), videoError = error)
        }
    }

    /** Simulace nahrávání pro náhledy (bez kamery). Skutečné nahrávání řídí route. */
    fun toggleRecording() {
        _uiState.update { current ->
            if (current.isRecordingVideo) {
                current.copy(
                    isRecordingVideo = false,
                    recordedSeconds = current.recordedSeconds.coerceAtLeast(12),
                    hasRecordedVideo = true
                )
            } else {
                current.copy(
                    isRecordingVideo = true,
                    recordedSeconds = 8
                )
            }
        }
    }

    fun nextStep() {
        _uiState.update { current ->
            val next = when (current.currentStep) {
                ContractStep.MOVEMENT_GOAL -> ContractStep.PLANNED_CONTENT
                ContractStep.PLANNED_CONTENT -> ContractStep.TASKS
                ContractStep.TASKS -> ContractStep.ECHO_VIDEO
                ContractStep.ECHO_VIDEO -> ContractStep.ECHO_VIDEO
            }
            current.copy(currentStep = next)
        }
    }

    fun previousStep() {
        _uiState.update { current ->
            val prev = when (current.currentStep) {
                ContractStep.MOVEMENT_GOAL -> ContractStep.MOVEMENT_GOAL
                ContractStep.PLANNED_CONTENT -> ContractStep.MOVEMENT_GOAL
                ContractStep.TASKS -> ContractStep.PLANNED_CONTENT
                ContractStep.ECHO_VIDEO -> ContractStep.TASKS
            }
            current.copy(currentStep = prev)
        }
    }

    fun submitContract() {
        _uiState.update { it.copy(isSubmitted = true) }
    }
}
