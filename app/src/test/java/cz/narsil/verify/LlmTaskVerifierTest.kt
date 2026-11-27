package cz.narsil.verify

import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.TaskType
import cz.narsil.core.VerificationResult
import cz.narsil.core.fake.FakeStravaRepository
import cz.narsil.llm.client.LlmClient
import cz.narsil.llm.client.LlmRequest
import cz.narsil.llm.client.LlmResponse
import cz.narsil.core.LlmProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Falešné HTTP/LLM: vrací [answer], počítá volání, volitelně čeká. */
private class FakeLlm(var answer: String?, private val delayMs: Long = 0) : LlmClient {
    val requests = mutableListOf<LlmRequest>()
    override suspend fun complete(request: LlmRequest): LlmResponse {
        requests += request
        if (delayMs > 0) {
            delay(delayMs)
        }
        return LlmResponse(LlmProvider.CLAUDE, answer)
    }
}

class LlmTaskVerifierTest {
    private val task = PlanTask(id = 5, type = TaskType.SCHOOL, title = "Lineární algebra", target = "kapitola 3")
    private val photo = PreparedPhoto(byteArrayOf(9, 9), "abc123")

    private fun verifier(
        llm: LlmClient,
        used: Set<String> = emptySet(),
        online: Boolean = true,
        strava: FakeStravaRepository? = null,
    ) = LlmTaskVerifier(
        llm = llm,
        photos = PhotoLoader { if (it == "content://chyba") null else photo },
        hashes = ProofHashCheck { h, _ -> h in used },
        network = NetworkStatus { online },
        strava = strava,
    )

    @Test
    fun promptFollowsSpec() {
        val p = VerifyRules.prompt(task, Proof.Photo("x"))
        assertEquals(
            "Jsi kontrolor splněných úkolů. Úkol: \"Lineární algebra\" (kapitola 3).\n" +
                "Rozhodni, zda přiložený důkaz věrohodně ukazuje, že úkol byl splněn.\n" +
                "Odpověz jediným slovem: ANO nebo NE.",
            p,
        )
        assertTrue(VerifyRules.prompt(task, Proof.Text("Spočítal jsem 1–10")).endsWith("Spočítal jsem 1–10"))
        assertEquals(300, VerifyRules.clampText("a".repeat(500)).length)
    }

    @Test
    fun parseAnswers() {
        assertEquals(VerificationResult.YES, VerifyRules.parse(" ano."))
        assertEquals(VerificationResult.YES, VerifyRules.parse("ANO, splněno"))
        assertEquals(VerificationResult.NO, VerifyRules.parse("Ne"))
        assertEquals(VerificationResult.NO, VerifyRules.parse("NÉ"))
        assertEquals(VerificationResult.LATER, VerifyRules.parse("Nevím"))
        assertEquals(VerificationResult.LATER, VerifyRules.parse("Možná"))
        assertEquals(VerificationResult.LATER, VerifyRules.parse(""))
        assertEquals(VerificationResult.LATER, VerifyRules.parse(null))
    }

    @Test
    fun yesNoGarbageAndTimeout() = runTest {
        assertEquals(VerificationResult.YES, verifier(FakeLlm("ANO")).verify(task, Proof.Text("Hotové příklady 1 až 10")))
        assertEquals(VerificationResult.NO, verifier(FakeLlm("NE")).verify(task, Proof.Photo("content://1")))
        assertEquals(VerificationResult.LATER, verifier(FakeLlm("🤷 asi")).verify(task, Proof.Photo("content://1")))
        assertEquals(VerificationResult.LATER, verifier(FakeLlm(null)).verify(task, Proof.Photo("content://1"))) // timeout/chyba = null
    }

    @Test
    fun photoIsResizedAndHashed() = runTest {
        val llm = FakeLlm("ANO")
        val outcome = verifier(llm).verifyDetailed(task, Proof.Photo("content://1"))
        assertEquals(VerificationResult.YES, outcome.result)
        assertEquals("abc123", outcome.proofHash)
        assertTrue(llm.requests.single().imageJpeg!!.contentEquals(photo.jpeg512))
    }

    @Test
    fun reusedPhotoIsRejectedWithoutLlm() = runTest {
        val llm = FakeLlm("ANO")
        val outcome = verifier(llm, used = setOf("abc123")).verifyDetailed(task, Proof.Photo("content://1"))
        assertEquals(VerificationResult.NO, outcome.result)
        assertTrue(llm.requests.isEmpty())
    }

    @Test
    fun offlineNeverYes() = runTest {
        val llm = FakeLlm("ANO")
        assertEquals(VerificationResult.LATER, verifier(llm, online = false).verify(task, Proof.Photo("content://1")))
        assertEquals(VerificationResult.LATER, verifier(llm, online = false).verify(task, Proof.Text("dlouhý důkaz textem")))
        assertTrue(llm.requests.isEmpty())
    }

    @Test
    fun unreadablePhotoAndEmptyText() = runTest {
        assertEquals(VerificationResult.LATER, verifier(FakeLlm("ANO")).verify(task, Proof.Photo("content://chyba")))
        assertEquals(VerificationResult.NO, verifier(FakeLlm("ANO")).verify(task, Proof.Text("   ")))
    }

    @Test
    fun stravaRules() = runTest {
        val run = FakeStravaRepository()
        assertEquals(VerificationResult.YES, verifier(FakeLlm(null), strava = run).verify(task, Proof.Strava))
        val short = FakeStravaRepository(activities = listOf(FakeStravaRepository.demoRun().copy(movingTimeSec = 600)))
        assertEquals(VerificationResult.NO, verifier(FakeLlm(null), strava = short).verify(task, Proof.Strava))
        val swim = FakeStravaRepository(activities = listOf(FakeStravaRepository.demoRun().copy(type = "Swim")))
        assertEquals(VerificationResult.NO, verifier(FakeLlm(null), strava = swim).verify(task, Proof.Strava))
        val disconnected = FakeStravaRepository(connected = false)
        assertEquals(VerificationResult.LATER, verifier(FakeLlm(null), strava = disconnected).verify(task, Proof.Strava))
    }

    @Test
    fun helpers() {
        assertEquals(512 to 384, VerifyRules.scaledSize(4000, 3000))
        assertEquals(300 to 200, VerifyRules.scaledSize(300, 200))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", VerifyRules.sha256Hex("abc".toByteArray()))
        assertNull(null)
    }
}
