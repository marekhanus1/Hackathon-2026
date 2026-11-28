package cz.narsil.verify.demo

import cz.narsil.core.DailyContract
import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import cz.narsil.core.VerificationResult
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.core.fake.FakeSettingsRepository
import cz.narsil.core.fake.FakeTaskVerifier
import cz.narsil.verify.TaskVerificationService
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoAwareTaskVerifierTest {
    private val task = PlanTask(id = 1, type = TaskType.OTHER, title = "Uklidit pokoj")

    @Test
    fun alwaysYesSwitch() = runTest {
        val settings = FakeSettingsRepository()
        val real = FakeTaskVerifier(nextResult = VerificationResult.LATER) // jako bez sítě
        val demo = DemoAwareTaskVerifier(real, settings)
        assertEquals(VerificationResult.LATER, demo.verify(task, Proof.Photo("content://x")))
        settings.update { it.copy(demoAlwaysYes = true) }
        val out = demo.verifyDetailed(task, Proof.Photo("content://x"))
        assertEquals(VerificationResult.YES, out.result)
        assertEquals(DemoAwareTaskVerifier.DEMO_REASON, out.reason)
    }

    @Test
    fun weakWifiThenAlwaysYesInService() = runTest {
        val day = LocalDate.of(2026, 11, 28)
        val contracts = FakeContractRepository(clock = { day }, withDemoContract = false)
        contracts.save(DailyContract(day, 6_000, tasks = listOf(task.copy(id = 0))))
        val settings = FakeSettingsRepository()
        val service = TaskVerificationService(contracts, DemoAwareTaskVerifier(FakeTaskVerifier(nextResult = VerificationResult.LATER), settings))
        val t = contracts.today.value!!.tasks.single()
        assertEquals(TaskStatus.VERIFY_LATER, service.submit(t, Proof.Photo("content://pokoj")).status)
        settings.update { it.copy(demoAlwaysYes = true) }
        assertEquals(TaskStatus.VERIFIED, service.retryPending().single().status)
    }

    @Test
    fun demoTextsAreConsistent() {
        assertEquals(2, DemoProofs.TEXT_EXAMPLES.count { it.expected })
        assertEquals(2, DemoProofs.TEXT_EXAMPLES.count { !it.expected })
    }
}
