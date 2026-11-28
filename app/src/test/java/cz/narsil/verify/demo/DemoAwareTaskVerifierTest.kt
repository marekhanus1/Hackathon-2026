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
import org.junit.Assert.assertTrue
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
        assertEquals(3, DemoProofs.TEXT_EXAMPLES.count { !it.expected })
        // Kladné ukázky projdou místní kontrolou popisu (jinak by je demo zamítlo).
        DemoProofs.TEXT_EXAMPLES.filter { it.expected }.forEach { ex ->
            val t = PlanTask(type = TaskType.OTHER, title = ex.taskTitle, target = ex.target)
            assertEquals(ex.note, null, cz.narsil.verify.VerifyRules.textIssue(t, (ex.proof as Proof.Text).text))
        }
        assertEquals(1, DemoProofs.ROOM_EXAMPLES.count { it.expected })
        DemoProofs.ROOM_EXAMPLES.forEach { assertEquals(cz.narsil.core.PhotoKind.ROOM, (it.proof as Proof.Photo).kind) }
    }

    @Test
    fun demoModesWithoutNetwork() = runTest {
        val settings = FakeSettingsRepository().apply { update { it.copy(demoAlwaysYes = true) } }
        val demo = DemoAwareTaskVerifier(FakeTaskVerifier(nextResult = VerificationResult.LATER), settings, fakeDelayMillis = 0)
        val room = demo.verifyDetailed(task, Proof.Photo("content://pokoj", cz.narsil.core.PhotoKind.ROOM))
        assertEquals(VerificationResult.YES, room.result)
        assertEquals("Prostředí na fotce odpovídá splněnému úkolu.", room.feedback)
        val short = demo.verifyDetailed(task, Proof.Text("jo uklizeno"))
        assertEquals(VerificationResult.NO, short.result)
        assertTrue(short.feedback.contains("2–3 věty"))
        val good = demo.verifyDetailed(
            task,
            Proof.Text("Vysál jsem koberec a složil oblečení do skříně. Vynesl jsem dva pytle odpadků a utřel prach na stole."),
        )
        assertEquals(VerificationResult.YES, good.result)
        assertTrue(good.feedback.startsWith("Popis je konkrétní"))
    }
}
