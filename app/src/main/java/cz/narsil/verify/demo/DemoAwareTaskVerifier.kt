package cz.narsil.verify.demo

import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.SettingsRepository
import cz.narsil.core.TaskVerifier
import cz.narsil.core.VerificationResult
import cz.narsil.verify.DetailedVerifier
import cz.narsil.verify.VerificationOutcome
import kotlinx.coroutines.delay

/**
 * Demo obal ověření (522). Když je v demo panelu zapnuté „ověření vždy ANO“ (`Settings.demoAlwaysYes`,
 * SPEC 10), úkol se po krátké pauze ([fakeDelayMillis], ať je vidět „ověřuji…“) schválí bez sítě a bez LLM.
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
            return VerificationOutcome(VerificationResult.YES, reason = DEMO_REASON)
        }
        return detailed.verifyDetailed(task, proof)
    }

    companion object {
        const val DEMO_REASON = "demo: ověření vždy ANO"
    }
}
