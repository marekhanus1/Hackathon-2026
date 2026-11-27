package cz.narsil.core.data

import cz.narsil.core.DailyContract
import cz.narsil.core.PlanTask
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DataHelpersTest {
    private val zone = ZoneId.of("Europe/Prague")
    private fun at(day: Int, hour: Int) = ZonedDateTime.of(2026, 11, day, hour, 30, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun dayStartsAtFour() {
        assertEquals(LocalDate.of(2026, 11, 27), DayKey.of(at(28, 3), zone))
        assertEquals(LocalDate.of(2026, 11, 28), DayKey.of(at(28, 4), zone))
        assertEquals(at(28, 4) - 30 * 60_000, DayKey.startMillis(LocalDate.of(2026, 11, 28), zone))
    }

    @Test
    fun eveningContractIsForTomorrow() {
        assertEquals(LocalDate.of(2026, 11, 29), DayKey.contractDateFor(at(28, 21), zone = zone))
        assertEquals(LocalDate.of(2026, 11, 29), DayKey.contractDateFor(at(29, 2), zone = zone))
        assertEquals(LocalDate.of(2026, 11, 28), DayKey.contractDateFor(at(28, 9), zone = zone))
        assertEquals(LocalDate.of(2026, 11, 28), DayKey.contractDateFor(at(28, 21), contractToday = true, zone = zone))
    }

    @Test
    fun contractRoundTrip() {
        val day = LocalDate.of(2026, 11, 28)
        val task = PlanTask(id = 3, type = TaskType.WORK, title = "Report", status = TaskStatus.VERIFIED, proofHash = "ab")
        val c = DailyContract(day, 6_000, listOf("přednáška o Rustu", "seriál \"X\""), "file://v.mp4", listOf(task))
        val taskEntity = task.toEntity(day)
        assertNotNull(taskEntity.verifiedAtMillis)
        assertNull(task.copy(status = TaskStatus.PENDING).toEntity(day).verifiedAtMillis)
        assertEquals(c, c.toEntity().toDomain(listOf(taskEntity)))
    }

    @Test
    fun unknownEnumFallsBack() {
        assertEquals(TaskType.OTHER, enumOr<TaskType>("NONSENSE", TaskType.OTHER))
        assertEquals(emptyList<String>(), plannedContentFromJson("not json"))
    }
}
