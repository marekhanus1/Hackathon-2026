package cz.narsil.core.fake

import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.StravaRepository
import cz.narsil.core.TaskVerifier
import cz.narsil.core.VerificationResult
import kotlinx.coroutines.delay

/**
 * Fake bez sítě. Výsledek se dá vnutit přes [nextResult] (null = pravidla níže):
 * text ≥ 10 znaků → YES, kratší → NO; fotka → YES, stejná URI podruhé → NO (opakovaná fotka);
 * Strava → YES, když [strava] má dnes pohybovou aktivitu ≥ 15 min, jinak NO.
 */
class FakeTaskVerifier(
    private val strava: StravaRepository? = null,
    private val delayMillis: Long = 0,
    var nextResult: VerificationResult? = null,
) : TaskVerifier {
    private val usedPhotos = mutableSetOf<String>()

    override suspend fun verify(task: PlanTask, proof: Proof): VerificationResult {
        if (delayMillis > 0) delay(delayMillis)
        nextResult?.let { return it }
        return when (proof) {
            is Proof.Text -> if (proof.text.trim().length >= 10) VerificationResult.YES else VerificationResult.NO
            is Proof.Photo -> if (usedPhotos.add(proof.uri)) VerificationResult.YES else VerificationResult.NO
            Proof.Strava -> {
                val ok = strava?.todayActivities().orEmpty().any {
                    it.type in StravaRepository.MOVEMENT_TYPES && it.movingTimeSec >= StravaRepository.MIN_MOVING_TIME_SEC
                }
                if (ok) VerificationResult.YES else VerificationResult.NO
            }
        }
    }
}
