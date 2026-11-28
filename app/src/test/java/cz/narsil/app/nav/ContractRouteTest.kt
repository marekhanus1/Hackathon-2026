package cz.narsil.app.nav

import cz.narsil.core.PlanTask
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import cz.narsil.ui.screens.onboarding.OnboardingVideoState
import org.junit.Assert.assertEquals
import org.junit.Test

class ContractRouteTest {
    /** Úprava smlouvy nesmaže stav už ověřeného úkolu. */
    @Test
    fun mergeKeepsProgressOfExistingTasks() {
        val verified = PlanTask(id = 7, type = TaskType.SCHOOL, title = "Kapitola 3", status = TaskStatus.VERIFIED, attempts = 1)
        val merged = mergeTasks(
            previous = listOf(verified, PlanTask(id = 8, type = TaskType.OTHER, title = "Smazaný")),
            drafted = listOf(PlanTask(type = TaskType.SCHOOL, title = "kapitola 3"), PlanTask(type = TaskType.WORK, title = "Nový")),
        )
        assertEquals(listOf(verified, PlanTask(type = TaskType.WORK, title = "Nový")), merged)
    }

    @Test
    fun onboardingVideoButtonLabels() {
        assertEquals("Natočit video", OnboardingVideoState().buttonLabel)
        assertEquals("Začít nahrávat", OnboardingVideoState(cameraOpen = true).buttonLabel)
        assertEquals("Ukončit nahrávání (4/15 s)", OnboardingVideoState(cameraOpen = true, isRecording = true, seconds = 4).buttonLabel)
        assertEquals("Natočit znovu", OnboardingVideoState(hasVideo = true).buttonLabel)
    }
}
