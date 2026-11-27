package cz.narsil.contract

import cz.narsil.core.DailyContract
import cz.narsil.core.EchoVideo
import cz.narsil.core.PlanTask
import cz.narsil.core.TaskType
import cz.narsil.core.data.ContractEntity
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.core.fake.FakeSettingsRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContractRulesTest {
    private val day = LocalDate.of(2026, 11, 28)

    private fun clockAt(time: LocalDateTime) = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = time.toInstant(ZoneOffset.UTC)
    }

    @Test
    fun sanitizeLimits() {
        val contract = DailyContract(
            date = day,
            stepGoal = -5,
            plannedContent = listOf(" Rust ", "rust", "", "a", "b", "c", "d", "e"),
            tasks = (1..8).map { PlanTask(type = TaskType.OTHER, title = "Úkol $it", rewardMinutes = if (it == 1) 1 else 90) } +
                PlanTask(type = TaskType.OTHER, title = "  "),
            videoUri = " ",
        )
        val clean = ContractRules.sanitize(contract)
        assertEquals(0, clean.stepGoal)
        assertEquals(listOf("Rust", "a", "b", "c", "d"), clean.plannedContent)
        assertEquals(6, clean.tasks.size)
        assertEquals(5, clean.tasks.first().rewardMinutes)
        assertEquals(60, clean.tasks.last().rewardMinutes)
        assertNull(clean.videoUri)
    }

    @Test
    fun eveningContractIsForTomorrow() = runTest {
        val repo = FakeContractRepository(clock = { day }, withDemoContract = false)
        val signer = ContractSigner(repo, clock = clockAt(LocalDateTime.of(2026, 11, 28, 21, 0)))
        val saved = signer.sign(ContractDraft(stepGoal = 7_000, plannedContent = listOf("přednáška o Rustu")))
        assertEquals(day.plusDays(1), saved.date)
        assertEquals(7_000, repo.forDate(day.plusDays(1))?.stepGoal)
    }

    @Test
    fun morningAndDemoContractIsForToday() = runTest {
        val repo = FakeContractRepository(clock = { day }, withDemoContract = false)
        assertEquals(day, ContractSigner(repo, clock = clockAt(LocalDateTime.of(2026, 11, 28, 9, 0))).targetDate())
        val settings = FakeSettingsRepository()
        settings.update { it.copy(demoContractToday = true) }
        val demo = ContractSigner(repo, settings, clockAt(LocalDateTime.of(2026, 11, 28, 21, 0)))
        assertEquals(day, demo.sign(ContractDraft()).date)
        assertEquals(day, repo.today.value?.date)
    }

    @Test
    fun echoVideoPreference() {
        val zone = ZoneOffset.UTC
        val yesterdayEvening = LocalDateTime.of(2026, 11, 27, 21, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val today = ContractEntity(date = day.toString(), stepGoal = 6_000, videoUri = "file://dnes.mp4", createdAtMillis = yesterdayEvening)
        val older = ContractEntity(date = day.minusDays(3).toString(), stepGoal = 6_000, videoUri = "file://stare.mp4", createdAtMillis = 0)
        val onboarding = EchoVideo("file://onboarding.mp4", day.minusDays(10))

        assertEquals(EchoVideo("file://dnes.mp4", day.minusDays(1)), EchoVideoPicker.pick(today, older, onboarding, zone))
        assertEquals("file://stare.mp4", EchoVideoPicker.pick(today.copy(videoUri = null), older, onboarding, zone)?.uri)
        assertEquals(onboarding, EchoVideoPicker.pick(null, null, onboarding, zone))
        assertNull(EchoVideoPicker.pick(null, null, null, zone))
    }
}
