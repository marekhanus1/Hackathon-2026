package cz.narsil.verify

import cz.narsil.core.DailyContract
import cz.narsil.core.LlmProvider
import cz.narsil.core.PhotoKind
import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import cz.narsil.core.VerificationResult
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.llm.client.LlmClient
import cz.narsil.llm.client.LlmRequest
import cz.narsil.llm.client.LlmResponse
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Režimy ověření 541: fotka úkolu, fotka prostředí, vlastní popis – prompty, parsování, chyby, demo. */
class VerifyModesTest {
    private val task = PlanTask(id = 7, type = TaskType.SCHOOL, title = "Lineární algebra", target = "kapitola 3, příklady 1–10")
    private val cleanup = PlanTask(id = 8, type = TaskType.OTHER, title = "Uklidit pokoj")
    private val good =
        "Spočítal jsem příklady 1–10 z kapitoly 3. U sedmičky vyšel determinant −12 a u devítky jsem použil Gaussovu eliminaci."

    private class Llm(var answer: String?, var error: String? = null) : LlmClient {
        val requests = mutableListOf<LlmRequest>()
        override suspend fun complete(request: LlmRequest): LlmResponse {
            requests += request
            return LlmResponse(LlmProvider.GEMINI, answer, error)
        }
    }

    private fun verifier(llm: LlmClient, used: Set<String> = emptySet(), online: Boolean = true) = LlmTaskVerifier(
        llm = llm,
        photos = PhotoLoader { PreparedPhoto(byteArrayOf(1, 2), "hash-$it") },
        hashes = ProofHashCheck { h, _ -> h in used },
        network = NetworkStatus { online },
    )

    // --- prompty ---

    @Test
    fun modeFromProof() {
        assertEquals(VerifyMode.TASK_PHOTO, VerifyRules.mode(Proof.Photo("x")))
        assertEquals(VerifyMode.ROOM_PHOTO, VerifyRules.mode(Proof.Photo("x", PhotoKind.ROOM)))
        assertEquals(VerifyMode.TEXT, VerifyRules.mode(Proof.Text("a")))
        assertNull(VerifyRules.mode(Proof.Strava))
    }

    @Test
    fun taskPhotoPrompt() {
        val p = VerifyRules.prompt(task, Proof.Photo("x"))
        assertTrue(p.contains("Úkol: \"Lineární algebra\" (kapitola 3, příklady 1–10)."))
        assertTrue(p.contains("Přiložená fotka je důkaz"))
        assertTrue(p.contains("displej se seriálem"))
        assertTrue(p.contains("ANO nebo NE, pak pomlčka a jedna krátká věta"))
        assertFalse(p.contains("<dukaz>"))
    }

    @Test
    fun roomPhotoPrompt() {
        val p = VerifyRules.prompt(cleanup, Proof.Photo("x", PhotoKind.ROOM))
        assertTrue(p.contains("Úkol: \"Uklidit pokoj\".\n"))
        assertTrue(p.contains("prostředí uživatele"))
        assertTrue(p.contains("uklizený pokoj u úklidu"))
        assertTrue(p.contains("rozložená podložka"))
        assertFalse(p.contains("Přiložená fotka je důkaz"))
    }

    @Test
    fun textPrompt() {
        val p = VerifyRules.prompt(task, Proof.Text(good))
        assertTrue(p.contains("konkrétně a věrohodně"))
        assertTrue(p.contains("co by měl popsat konkrétněji"))
        assertTrue(p.contains("zkopírovaný"))
        assertTrue(p.endsWith("<dukaz>\n$good\n</dukaz>"))
    }

    // --- parsování ---

    @Test
    fun parseVerdictWithFeedback() {
        val yes = VerifyRules.parseVerdict("ANO – Skvěle jsi zmínil konkrétní výsledky a postupy u příkladů 2, 7 a 9.")
        assertEquals(VerificationResult.YES, yes.result)
        assertEquals("Skvěle jsi zmínil konkrétní výsledky a postupy u příkladů 2, 7 a 9.", yes.feedback)
        val no = VerifyRules.parseVerdict("NE - napiš, které příklady jsi vyřešil.\nDalší řádek se ignoruje.")
        assertEquals(VerificationResult.NO, no.result)
        assertEquals("Napiš, které příklady jsi vyřešil.", no.feedback)
        assertEquals("Na fotce je televize.", VerifyRules.parseVerdict("**NE**: \"Na fotce je televize.\"").feedback)
        assertEquals(VerificationResult.NO, VerifyRules.parseVerdict("**NE**: \"Na fotce je televize.\"").result)
        assertEquals("", VerifyRules.parseVerdict("ANO").feedback)
        assertEquals(VerificationResult.YES, VerifyRules.parseVerdict("ano.").result)
    }

    @Test
    fun parseVerdictLaterAndGarbage() {
        assertEquals(VerificationResult.LATER, VerifyRules.parseVerdict("POZDĚJI – fotka je úplně tmavá").result)
        assertEquals("", VerifyRules.parseVerdict("POZDĚJI – fotka je úplně tmavá").feedback)
        assertEquals(VerificationResult.LATER, VerifyRules.parseVerdict("Nevím – nejde to poznat").result)
        assertEquals(VerificationResult.LATER, VerifyRules.parseVerdict("NEVÍM").result)
        assertEquals(VerificationResult.LATER, VerifyRules.parseVerdict("🤷").result)
        assertEquals(VerificationResult.LATER, VerifyRules.parseVerdict(null).result)
        assertEquals(VerificationResult.LATER, VerifyRules.parseVerdict("").result)
    }

    @Test
    fun longFeedbackIsTrimmed() {
        val v = VerifyRules.parseVerdict("NE – " + "a".repeat(500))
        assertEquals(VerifyRules.MAX_FEEDBACK_CHARS, v.feedback.length)
        assertTrue(v.feedback.endsWith("…"))
    }

    // --- místní kontrola popisu ---

    @Test
    fun textIssueCatchesObviousCheats() {
        assertNull(VerifyRules.textIssue(task, good))
        assertTrue(VerifyRules.textIssue(task, "   ")!!.contains("Napiš"))
        assertTrue(VerifyRules.textIssue(task, "jo udělal")!!.contains("2–3 věty"))
        // dlouhé, ale jen jedna věta
        assertTrue(VerifyRules.textIssue(task, "Spočítal jsem všechny příklady z kapitoly tři a všechno vyšlo správně podle výsledků")!!.contains("2–3 věty"))
        // opakovaný text
        assertTrue(VerifyRules.textIssue(task, "Hotovo hotovo hotovo hotovo. Hotovo hotovo hotovo hotovo. Hotovo hotovo hotovo.")!!.contains("opakuje"))
        assertTrue(VerifyRules.textIssue(task, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa. aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa aaa. aaaa aaa.")!!.contains("opakuje"))
        // opsané zadání
        assertTrue(
            VerifyRules.textIssue(task, "Lineární algebra, kapitola 3, příklady 1–10. Lineární algebra hotovo. Kapitola 3 splnil jsem.")!!
                .contains("Nepřepisuj zadání"),
        )
    }

    @Test
    fun sentenceCountAndTextHash() {
        assertEquals(2, VerifyRules.sentenceCount("Udělal jsem to. A ještě tohle!"))
        assertEquals(2, VerifyRules.sentenceCount("První řádek popisu\nDruhý řádek popisu"))
        assertEquals(1, VerifyRules.sentenceCount("Determinant 1.5 vyšel dobře"))
        assertEquals(VerifyRules.textHash("Ahoj, světe!  Tady"), VerifyRules.textHash("ahoj svete tady"))
        assertFalse(VerifyRules.textHash("ahoj svete") == VerifyRules.textHash("ahoj svete tady"))
    }

    // --- ověřovač ---

    @Test
    fun textModeReturnsFeedback() = runTest {
        val llm = Llm("NE – Napiš konkrétně, které příklady jsi vyřešil.")
        val out = verifier(llm).verifyDetailed(task, Proof.Text(good))
        assertEquals(VerificationResult.NO, out.result)
        assertEquals("Napiš konkrétně, které příklady jsi vyřešil.", out.feedback)
        assertEquals(VerifyRules.textHash(good), out.proofHash)
        assertEquals(VerifyRules.MAX_ANSWER_TOKENS, llm.requests.single().maxTokens)
        assertNull(llm.requests.single().imageJpeg)
    }

    @Test
    fun shortOrReusedTextRejectedWithoutLlm() = runTest {
        val llm = Llm("ANO")
        val short = verifier(llm).verifyDetailed(task, Proof.Text("jo udělal"))
        assertEquals(VerificationResult.NO, short.result)
        assertTrue(short.feedback.contains("2–3 věty"))
        val reused = verifier(llm, used = setOf(VerifyRules.textHash(good))).verifyDetailed(task, Proof.Text(good))
        assertEquals(VerificationResult.NO, reused.result)
        assertTrue(reused.feedback.contains("už jsi použil"))
        assertTrue(llm.requests.isEmpty())
    }

    @Test
    fun roomPhotoGoesToLlmWithRoomPrompt() = runTest {
        val llm = Llm("ANO – Na fotce je rozložená podložka.")
        val out = verifier(llm).verifyDetailed(cleanup, Proof.Photo("content://pokoj", PhotoKind.ROOM))
        assertEquals(VerificationResult.YES, out.result)
        assertEquals("hash-content://pokoj", out.proofHash)
        assertTrue(llm.requests.single().prompt.contains("prostředí uživatele"))
        assertTrue(llm.requests.single().imageJpeg != null)
    }

    @Test
    fun errorsAreFriendlyCzech() = runTest {
        val notFound = verifier(Llm(null, "HTTP 404: {\"error\": model not found}")).verifyDetailed(task, Proof.Photo("p"))
        assertEquals(VerificationResult.LATER, notFound.result)
        assertFalse(notFound.reason.contains("HTTP"))
        assertTrue(notFound.detail.contains("HTTP 404"))
        assertTrue(verifier(Llm(null, "HTTP 429: quota")).verifyDetailed(task, Proof.Photo("p")).reason.contains("přetížená"))
        assertTrue(verifier(Llm(null, "chyba sítě nebo timeout")).verifyDetailed(task, Proof.Photo("p")).reason.contains("zkusíme"))
        assertTrue(verifier(Llm(null, "chybí API klíč pro GEMINI (local.properties)")).verifyDetailed(task, Proof.Photo("p")).reason.contains("chybí klíč"))
        assertTrue(verifier(Llm("Možná")).verifyDetailed(task, Proof.Photo("p")).reason.contains("neodpověděl jasně"))
        val offline = verifier(Llm("ANO"), online = false).verifyDetailed(task, Proof.Photo("p", PhotoKind.ROOM))
        assertEquals(VerificationResult.LATER, offline.result)
        assertEquals(VerifyRules.OFFLINE_REASON, offline.reason)
    }

    // --- uložení důkazu a služba ---

    @Test
    fun codecKeepsRoomKind() {
        val room = Proof.Photo("content://a", PhotoKind.ROOM)
        assertEquals("room:content://a", ProofCodec.encode(room))
        assertEquals(room, ProofCodec.decode(ProofCodec.encode(room)))
        assertEquals(Proof.Photo("content://a"), ProofCodec.decode("content://a"))
        assertEquals(Proof.Text(good), ProofCodec.decode(ProofCodec.encode(Proof.Text(good))))
    }

    @Test
    fun serviceShowsFeedbackAndRetriesRoomLater() = runTest {
        val day = LocalDate.of(2026, 11, 28)
        val contracts = FakeContractRepository(clock = { day }, withDemoContract = false)
        contracts.save(DailyContract(day, 6_000, tasks = listOf(task.copy(id = 0), cleanup.copy(id = 0))))
        val llm = Llm("NE – Napiš konkrétně, které příklady jsi vyřešil.")
        val service = TaskVerificationService(contracts, verifier(llm))
        val (t1, t2) = contracts.today.value!!.tasks
        val rejected = service.submit(t1, Proof.Text(good))
        assertEquals(TaskStatus.REJECTED, rejected.status)
        val ev = service.events.value[t1.id]!!
        assertEquals("Napiš konkrétně, které příklady jsi vyřešil.", ev.feedback)
        assertTrue(ev.reason.startsWith("Zamítnuto – Napiš konkrétně"))

        llm.answer = null
        llm.error = "chyba sítě nebo timeout"
        val later = service.submit(t2, Proof.Photo("content://pokoj", PhotoKind.ROOM))
        assertEquals(TaskStatus.VERIFY_LATER, later.status)
        assertEquals("room:content://pokoj", later.proofUri)
        assertFalse(service.events.value[t2.id]!!.reason.contains("timeout"))

        llm.answer = "ANO – Pokoj je uklizený."
        llm.error = null
        val retried = service.retryPending().single()
        assertEquals(TaskStatus.VERIFIED, retried.status)
        assertTrue(llm.requests.last().prompt.contains("prostředí uživatele"))
        assertEquals("Ověřeno – Pokoj je uklizený.", service.events.value[t2.id]!!.reason)
    }
}
