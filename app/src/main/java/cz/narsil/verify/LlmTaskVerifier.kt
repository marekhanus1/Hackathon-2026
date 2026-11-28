package cz.narsil.verify

import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.StravaRepository
import cz.narsil.core.TaskVerifier
import cz.narsil.core.VerificationResult
import cz.narsil.llm.client.LlmClient
import cz.narsil.llm.client.LlmDefaults
import cz.narsil.llm.client.LlmRequest
import cz.narsil.llm.client.runCatchingCancellable

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

/**
 * Výsledek ověření i s otiskem fotky (nebo textu), který volající (520) uloží k úkolu.
 * [reason] = česká věta pro UI (u LATER srozumitelný důvod, ne surová chyba), [feedback] = věta
 * zpětné vazby od LLM nebo místní kontroly (541; prázdná, když žádná není), [detail] = technický
 * detail pro log (surová chyba / odpověď modelu).
 */
data class VerificationOutcome(
    val result: VerificationResult,
    val proofHash: String? = null,
    val reason: String = "",
    val feedback: String = "",
    val detail: String = "",
)

/**
 * Skutečný [TaskVerifier] (501, SPEC 7, režimy 541). Nikdy nehází: chyba, timeout nebo nejasná odpověď = LATER.
 * Bez sítě nikdy YES (LATER). Opakovaná fotka (stejný SHA-256) = NO bez volání LLM.
 *
 * Režimy důkazu ([VerifyMode]): fotka úkolu, fotka prostředí ([cz.narsil.core.PhotoKind.ROOM]) a vlastní
 * popis. Popis se nejdřív zkontroluje místně ([VerifyRules.textIssue] – prázdný, krátký, opakovaný,
 * opsané zadání) a otiskem ([VerifyRules.textHash] – stejný text u jiného úkolu); obojí = NO bez LLM
 * se zpětnou vazbou. LLM odpovídá „ANO – věta“ / „NE – věta“ ([VerifyRules.parseVerdict]).
 */
class LlmTaskVerifier(
    private val llm: LlmClient,
    private val photos: PhotoLoader,
    private val hashes: ProofHashCheck,
    private val network: NetworkStatus = NetworkStatus { true },
    private val strava: StravaRepository? = null,
    private val timeoutMillis: Long = LlmDefaults.TIMEOUT_MILLIS,
) : TaskVerifier, DetailedVerifier {

    override suspend fun verify(task: PlanTask, proof: Proof): VerificationResult = verifyDetailed(task, proof).result

    override suspend fun verifyDetailed(task: PlanTask, proof: Proof): VerificationOutcome = runCatchingCancellable {
        when (proof) {
            is Proof.Text -> verifyText(task, proof)
            is Proof.Photo -> verifyPhoto(task, proof)
            Proof.Strava -> verifyStrava()
        }
    }.getOrElse {
        VerificationOutcome(VerificationResult.LATER, reason = VerifyRules.friendlyLaterReason("chyba"), detail = "chyba: ${it.message}")
    }

    private suspend fun verifyText(task: PlanTask, proof: Proof.Text): VerificationOutcome {
        VerifyRules.textIssue(task, proof.text)?.let { issue ->
            return VerificationOutcome(VerificationResult.NO, reason = issue, feedback = issue, detail = "místní kontrola textu")
        }
        val hash = VerifyRules.textHash(proof.text)
        if (hashes.isUsed(hash, task.id)) {
            val msg = "Tenhle popis už jsi použil u jiného úkolu – napiš, co jsi udělal tentokrát."
            return VerificationOutcome(VerificationResult.NO, hash, msg, msg, "opakovaný text")
        }
        return ask(task, proof, image = null, hash = hash)
    }

    private suspend fun verifyPhoto(task: PlanTask, proof: Proof.Photo): VerificationOutcome {
        val photo = photos.load(proof.uri)
            ?: return VerificationOutcome(
                VerificationResult.LATER,
                reason = "Fotku se nepodařilo načíst – zkus ji vyfotit znovu.",
                detail = "fotku nejde přečíst",
            )
        if (hashes.isUsed(photo.sha256, task.id)) {
            val msg = "Tuhle fotku už jsi použil – vyfoť nový důkaz."
            return VerificationOutcome(VerificationResult.NO, photo.sha256, msg, msg, "fotka už byla použita")
        }
        return ask(task, proof, photo.jpeg512, photo.sha256)
    }

    private suspend fun verifyStrava(): VerificationOutcome {
        val repo = strava ?: return VerificationOutcome(VerificationResult.LATER, reason = "Strava není k dispozici – pošli fotku.")
        if (!repo.isConnected.value) return VerificationOutcome(VerificationResult.LATER, reason = "Strava není připojená – pošli fotku.")
        if (!network.isOnline()) return VerificationOutcome(VerificationResult.LATER, reason = VerifyRules.OFFLINE_REASON)
        val activities = repo.todayActivities()
        // Prázdný seznam = žádná aktivita, nebo chyba API (výpadek, vypršený token) – nezamítat, zkusit znovu.
        if (activities.isEmpty()) {
            return VerificationOutcome(VerificationResult.LATER, reason = "Na Stravě zatím není dnešní aktivita – zkus později nebo pošli fotku.")
        }
        return if (VerifyRules.stravaSatisfies(activities)) {
            VerificationOutcome(VerificationResult.YES, reason = "Aktivita na Stravě")
        } else {
            val msg = "Dnes na Stravě chybí aktivita aspoň 15 minut."
            VerificationOutcome(VerificationResult.NO, reason = msg, feedback = msg)
        }
    }

    private suspend fun ask(task: PlanTask, proof: Proof, image: ByteArray?, hash: String?): VerificationOutcome {
        if (!network.isOnline()) return VerificationOutcome(VerificationResult.LATER, hash, VerifyRules.OFFLINE_REASON, detail = "bez sítě")
        val response = llm.complete(
            LlmRequest(
                prompt = VerifyRules.prompt(task, proof),
                imageJpeg = image,
                maxTokens = VerifyRules.MAX_ANSWER_TOKENS,
                timeoutMillis = timeoutMillis,
            ),
        )
        val verdict = VerifyRules.parseVerdict(response.text)
        val detail = response.error ?: "odpověď: ${response.text}"
        return when (verdict.result) {
            VerificationResult.LATER -> VerificationOutcome(
                VerificationResult.LATER, hash, VerifyRules.friendlyLaterReason(response.error), detail = detail,
            )
            else -> VerificationOutcome(verdict.result, hash, verdict.feedback, verdict.feedback, detail)
        }
    }
}
