package cz.narsil.verify

import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.StravaRepository
import cz.narsil.core.TaskVerifier
import cz.narsil.core.VerificationResult
import cz.narsil.llm.client.LlmClient
import cz.narsil.llm.client.LlmDefaults
import cz.narsil.llm.client.LlmRequest

/** Fotka připravená pro LLM: zmenšený JPEG a otisk původního souboru. */
class PreparedPhoto(val jpeg512: ByteArray, val sha256: String)

/** Načtení a zmenšení fotky (Android: [AndroidPhotoLoader]). null = fotku nejde přečíst. */
fun interface PhotoLoader {
    suspend fun load(uri: String): PreparedPhoto?
}

/** Byl otisk fotky už použit u jiného úkolu? (Room: `ContractDao.isProofHashUsed`.) */
fun interface ProofHashCheck {
    suspend fun isUsed(hash: String, exceptTaskId: Long): Boolean
}

/** Je síť? Bez sítě se nikdy automaticky neschvaluje (SPEC 7). */
fun interface NetworkStatus {
    fun isOnline(): Boolean
}

/** Výsledek ověření i s otiskem fotky, který volající (520) uloží k úkolu. */
data class VerificationOutcome(
    val result: VerificationResult,
    val proofHash: String? = null,
    val reason: String = "",
)

/**
 * Skutečný [TaskVerifier] (501, SPEC 7). Nikdy nehází: chyba, timeout nebo nejasná odpověď = LATER.
 * Bez sítě nikdy YES (LATER). Opakovaná fotka (stejný SHA-256) = NO bez volání LLM.
 */
class LlmTaskVerifier(
    private val llm: LlmClient,
    private val photos: PhotoLoader,
    private val hashes: ProofHashCheck,
    private val network: NetworkStatus = NetworkStatus { true },
    private val strava: StravaRepository? = null,
    private val timeoutMillis: Long = LlmDefaults.TIMEOUT_MILLIS,
) : TaskVerifier {

    override suspend fun verify(task: PlanTask, proof: Proof): VerificationResult = verifyDetailed(task, proof).result

    suspend fun verifyDetailed(task: PlanTask, proof: Proof): VerificationOutcome = runCatching {
        when (proof) {
            is Proof.Text -> verifyText(task, proof)
            is Proof.Photo -> verifyPhoto(task, proof)
            Proof.Strava -> verifyStrava()
        }
    }.getOrElse { VerificationOutcome(VerificationResult.LATER, reason = "chyba: ${it.message}") }

    private suspend fun verifyText(task: PlanTask, proof: Proof.Text): VerificationOutcome {
        if (VerifyRules.clampText(proof.text).isEmpty()) return VerificationOutcome(VerificationResult.NO, reason = "prázdný text")
        return ask(task, proof, image = null, hash = null)
    }

    private suspend fun verifyPhoto(task: PlanTask, proof: Proof.Photo): VerificationOutcome {
        val photo = photos.load(proof.uri)
            ?: return VerificationOutcome(VerificationResult.LATER, reason = "fotku nejde přečíst")
        if (hashes.isUsed(photo.sha256, task.id)) {
            return VerificationOutcome(VerificationResult.NO, photo.sha256, "fotka už byla použita")
        }
        return ask(task, proof, photo.jpeg512, photo.sha256)
    }

    private suspend fun verifyStrava(): VerificationOutcome {
        val repo = strava ?: return VerificationOutcome(VerificationResult.LATER, reason = "Strava není k dispozici")
        if (!repo.isConnected.value) return VerificationOutcome(VerificationResult.LATER, reason = "Strava není připojená – pošli fotku")
        if (!network.isOnline()) return VerificationOutcome(VerificationResult.LATER, reason = "bez sítě")
        return if (VerifyRules.stravaSatisfies(repo.todayActivities())) {
            VerificationOutcome(VerificationResult.YES, reason = "aktivita na Stravě")
        } else {
            VerificationOutcome(VerificationResult.NO, reason = "dnes chybí aktivita ≥ 15 min")
        }
    }

    private suspend fun ask(task: PlanTask, proof: Proof, image: ByteArray?, hash: String?): VerificationOutcome {
        if (!network.isOnline()) return VerificationOutcome(VerificationResult.LATER, hash, "bez sítě")
        val response = llm.complete(
            LlmRequest(
                prompt = VerifyRules.prompt(task, proof),
                imageJpeg = image,
                maxTokens = LlmDefaults.MAX_TOKENS,
                timeoutMillis = timeoutMillis,
            ),
        )
        val result = VerifyRules.parse(response.text)
        return VerificationOutcome(result, hash, response.error ?: "odpověď: ${response.text}")
    }
}
