package cz.narsil.app.nav

import cz.narsil.core.PlanTask
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import cz.narsil.ui.screens.verify.TaskItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Úkoly smlouvy → položky todo listu na domově, výběru a záložkách ověření (334). */
class TaskItemsTest {
    @Test
    fun pendingTaskCanBeVerified() {
        val item = PlanTask(id = 7, type = TaskType.SCHOOL, title = "Laboratorní úloha z databází").toTaskItem()
        assertEquals(TaskItemStatus.PENDING, item.status)
        assertTrue(item.canVerify)
        assertEquals("ŠK", item.short)
        assertEquals("Škola · fotka nebo popis · +20 min", item.meta)
        assertEquals("Pokus 1 ze 3", item.attemptsLine)
    }

    @Test
    fun rejectedTaskShowsRemainingAttempts() {
        val item = PlanTask(id = 1, type = TaskType.WORK, title = "Report", status = TaskStatus.REJECTED, attempts = 1).toTaskItem()
        assertEquals(TaskItemStatus.REJECTED, item.status)
        assertTrue(item.canVerify)
        assertEquals("Práce · zamítnuto, zbývají 2 pokusy", item.meta)
        assertEquals("Minule zamítnuto · pokus 2 ze 3", item.attemptsLine)
    }

    @Test
    fun rejectedWithoutAttemptsIsExhausted() {
        val item = PlanTask(id = 1, type = TaskType.OTHER, title = "Pokoj", status = TaskStatus.REJECTED, attempts = 3).toTaskItem()
        assertEquals(TaskItemStatus.EXHAUSTED, item.status)
        assertFalse(item.canVerify)
    }

    @Test
    fun verifiedTaskIsDone() {
        val item = PlanTask(id = 2, type = TaskType.MOVEMENT, title = "Běh 5 km", status = TaskStatus.VERIFIED, attempts = 1).toTaskItem()
        assertTrue(item.isDone)
        assertFalse(item.canVerify)
        assertEquals("Pohyb · ověřeno", item.meta)
        assertEquals("Běh", item.tabLabel)
    }

    @Test
    fun tabsOnlyForBottomMenuScreens() {
        assertEquals(cz.narsil.ui.components.NeuTab.HOME, tabFor(NarsilRoute.Home.route))
        assertEquals(null, tabFor(NarsilRoute.Contract.route))
        assertEquals(null, tabFor(NarsilRoute.Verify.PATTERN))
        assertEquals("verify?taskId=5", NarsilRoute.Verify.forTask(5))
    }
}
