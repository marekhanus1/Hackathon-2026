package cz.narsil.ui.screens.contract

import cz.narsil.core.DailyContract
import cz.narsil.core.PlanRules
import cz.narsil.core.PlanTask
import cz.narsil.core.TaskType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 139: úkoly ve smlouvě, kroky a stav nahrávání. */
class ContractViewModelTest {
    @Test
    fun stepsGoThroughTasks() {
        val vm = ContractViewModel()
        assertEquals(ContractStep.MOVEMENT_GOAL, vm.uiState.value.currentStep)
        vm.nextStep(); assertEquals(ContractStep.PLANNED_CONTENT, vm.uiState.value.currentStep)
        vm.nextStep(); assertEquals(ContractStep.TASKS, vm.uiState.value.currentStep)
        vm.nextStep(); assertEquals(ContractStep.ECHO_VIDEO, vm.uiState.value.currentStep)
        vm.previousStep(); assertEquals(ContractStep.TASKS, vm.uiState.value.currentStep)
        assertEquals(4, ContractStep.entries.size)
    }

    @Test
    fun addTasksWithLimitAndNoDuplicates() {
        val vm = ContractViewModel()
        vm.selectTaskType(TaskType.SCHOOL)
        vm.updateTaskDraftText("  Kapitola 3  ")
        vm.addTask()
        val first = vm.uiState.value.tasks.single()
        assertEquals(TaskDraft(TaskType.SCHOOL, "Kapitola 3"), first)
        assertEquals("", vm.uiState.value.taskDraftText)
        assertEquals(20, first.rewardMinutes)
        vm.addTask(TaskType.OTHER, "kapitola 3") // duplicita
        vm.addTask(TaskType.OTHER, "   ") // prázdné
        assertEquals(1, vm.uiState.value.tasks.size)
        (1..10).forEach { vm.addTask(TaskType.OTHER, "Úkol $it") }
        assertEquals(PlanRules.MAX_TASKS, vm.uiState.value.tasks.size)
        assertFalse(vm.uiState.value.canAddTask)
        vm.removeTask(first)
        assertEquals(PlanRules.MAX_TASKS - 1, vm.uiState.value.tasks.size)
        assertTrue(vm.uiState.value.canAddTask)
        val plan = TaskDraft(TaskType.MOVEMENT, "Běh").toPlanTask()
        assertEquals(30, plan.rewardMinutes)
        assertEquals(TaskType.MOVEMENT.defaultMethod, plan.method)
    }

    @Test
    fun prefillFromExistingContract() {
        val vm = ContractViewModel()
        vm.prefill(
            DailyContract(
                date = LocalDate.of(2026, 11, 29), stepGoal = 8_000, plannedContent = listOf("Dokument"),
                videoUri = "file:///v.mp4", tasks = listOf(PlanTask(type = TaskType.WORK, title = "Report")),
            ),
        )
        val s = vm.uiState.value
        assertEquals(MovementGoalType.STEPS, s.goalType)
        assertEquals(8_000, s.goalAmount)
        assertEquals(listOf("Dokument"), s.plannedItems)
        assertEquals(listOf(TaskDraft(TaskType.WORK, "Report")), s.tasks)
        assertTrue(s.hasRecordedVideo)
    }

    @Test
    fun recordingStateFromRecorder() {
        val vm = ContractViewModel()
        vm.onRecordingState(isRecording = true, seconds = 3, hasVideo = false, error = null)
        assertTrue(vm.uiState.value.isRecordingVideo)
        assertFalse(vm.uiState.value.hasRecordedVideo)
        vm.onRecordingState(isRecording = false, seconds = 12, hasVideo = true, error = null)
        assertTrue(vm.uiState.value.hasRecordedVideo)
        assertEquals(12, vm.uiState.value.recordedSeconds)
        vm.onRecordingState(isRecording = false, seconds = 0, hasVideo = false, error = "Kamera není k dispozici")
        assertEquals("Kamera není k dispozici", vm.uiState.value.videoError)
        // submit už nepředstírá video
        val fresh = ContractViewModel()
        fresh.submitContract()
        assertTrue(fresh.uiState.value.isSubmitted)
        assertFalse(fresh.uiState.value.hasRecordedVideo)
    }

    @Test
    fun textsFollowTargetDay() {
        assertEquals("Úkoly na zítřek", ContractStep.TASKS.title(targetIsToday = false))
        assertEquals("Úkoly na dnešek", ContractStep.TASKS.title(targetIsToday = true))
        assertEquals("Pohybový cíl na dnešek", ContractStep.MOVEMENT_GOAL.title(targetIsToday = true))
        assertEquals("Tvoje 15s Ozvěna", ContractStep.ECHO_VIDEO.title(targetIsToday = true))
        assertEquals("dnes", ContractUiState(targetIsToday = true).dayWord)
        assertEquals("zítra", ContractUiState().dayWord)
    }

    @Test
    fun savedVideoKeepsItsLength() {
        val vm = ContractViewModel()
        vm.onRecordingState(isRecording = false, seconds = 0, hasVideo = false, error = null)
        vm.prefill(DailyContract(LocalDate.of(2026, 11, 28), 6_000, videoUri = "file:///v.mp4"), videoSeconds = 12)
        vm.onRecordingState(isRecording = false, seconds = 0, hasVideo = false, error = null)
        assertEquals(12, vm.uiState.value.recordedSeconds)
        assertTrue(vm.uiState.value.hasRecordedVideo)
        vm.onRecordingState(isRecording = true, seconds = 3, hasVideo = false, error = null)
        assertEquals(3, vm.uiState.value.recordedSeconds)
    }
}
