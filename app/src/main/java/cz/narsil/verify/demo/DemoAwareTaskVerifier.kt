package cz.narsil.verify.demo

import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.SettingsRepository
import cz.narsil.core.TaskVerifier
import cz.narsil.core.VerificationResult
import cz.narsil.verify.DetailedVerifier
import cz.narsil.verify.VerificationOutcome
import cz.narsil.verify.VerifyRules
import kotlinx.coroutines.delay

/**
 * Demo obal ověření (522). Když je v demo panelu zapnuté „ověření vždy ANO“ (`Settings.demoAlwaysYes`,
 * SPEC 10), úkol se po krátké pauze ([fakeDelayMillis], ať je vidět „ověřuji…“) schválí bez sítě a bez LLM,
 * se zpětnou vazbou podle režimu ([DemoProofs.feedbackFor]). Popis, který neprojde místní kontrolou
 * ([VerifyRules.textIssue] – krátký, opakovaný, opsané zadání), se i v demu zamítne s radou, co dopsat,
 * aby šlo předvést i zamítnutí bez sítě (541).
 * Jinak rozhoduje skutečný ověřovač – včetně „ověří se později“ při slabé Wi-Fi v sále.
 */
class DemoAwareTaskVerifier(
    private val delegate: TaskVerifier,
    private val settings: SettingsRepository,
    private val fakeDelayMillis: Long = 1_200,
) : TaskVerifier, DetailedVerifier {
    private val detailed = DetailedVerifier.of(delegate)

    val alwaysYes: Boolean get() = settings.settings.value.demoAlwaysYes

    override suspend fun verify(task: PlanTask, proof: Proof): VerificationResult = verifyDetailed(task, proof).result

    override suspend fun verifyDetailed(task: PlanTask, proof: Proof): VerificationOutcome {
        if (alwaysYes) {
            if (fakeDelayMillis > 0) delay(fakeDelayMillis)
            if (proof is Proof.Text) {
                VerifyRules.textIssue(task, proof.text)?.let { issue ->
                    return VerificationOutcome(VerificationResult.NO, reason = issue, feedback = issue, detail = DEMO_REASON)
                }
            }
            return VerificationOutcome(VerificationResult.YES, reason = DEMO_REASON, feedback = DemoProofs.feedbackFor(proof))
        }
        return detailed.verifyDetailed(task, proof)
    }

    companion object {
        const val DEMO_REASON = "demo: ověření vždy ANO"
    }
}
