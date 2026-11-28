package cz.narsil.verify

import cz.narsil.core.DailyContract
import cz.narsil.core.GamificationState
import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.TaskType
import cz.narsil.core.VerificationResult
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.core.fake.FakeGamificationEngine
import cz.narsil.core.fake.FakeMovementRepository
import cz.narsil.core.fake.FakeRewardService
import cz.narsil.core.fake.FakeTaskVerifier
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RewardHandoffTest {
    @Test
    fun verifiedTaskRaisesBalanceAndXpOnce() = runTest {
        val day = LocalDate.of(2026, 11, 28)
        val contracts = FakeContractRepository(clock = { day }, withDemoContract = false)
        contracts.save(DailyContract(day, 6_000, tasks = listOf(PlanTask(type = TaskType.SCHOOL, title = "Zápisky"))))
        val movement = FakeMovementRepository(stepsToday = 0)
        val gamification = FakeGamificationEngine(GamificationState())
        val verifier = FakeTaskVerifier(nextResult = VerificationResult.NO)
        val service = VerificationModule.service(contracts, verifier, FakeRewardService(movement, gamification))

        val task = contracts.today.value!!.tasks.single()
        val rejected = service.submit(task, Proof.Photo("content://rozmazane"))
        assertEquals(0, movement.state.value.balanceMinutes) // zamítnutý úkol bez odměny

        verifier.nextResult = VerificationResult.YES
        val verified = service.submit(rejected, Proof.Photo("content://zapisky"))
        service.submit(verified, Proof.Photo("content://zapisky2"))
        assertEquals(20, movement.state.value.balanceMinutes) // SCHOOL = 20 min
        assertEquals(30, gamification.state.value.xp)
    }
}
