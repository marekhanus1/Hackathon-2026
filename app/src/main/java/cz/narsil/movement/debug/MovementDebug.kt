package cz.narsil.movement.debug

import cz.narsil.core.MovementRepository
import cz.narsil.core.fake.FakeMovementRepository
import cz.narsil.movement.StepsMovementRepository

/**
 * Ladící akce pohybu pro demo panel (222 → 123). Fungují nad fake i skutečnou implementací;
 * vrací false, když daná implementace akci neumí.
 */
object MovementDebug {
    /** Demo „+1 000 kroků“ (jen pro dnešek, do Health Connect se nic nepíše). */
    suspend fun addSteps(repo: MovementRepository, steps: Int = 1_000): Boolean = when (repo) {
        is StepsMovementRepository -> {
            repo.addSteps(steps)
            true
        }
        is FakeMovementRepository -> {
            repo.addSteps(steps)
            true
        }
        else -> false
    }

    /**
     * Demo „reset dne“: minuty dneška od nuly (platby, půjčky, odměny, přenesený dluh), dluh 0,
     * kroky dneška na [steps] (výchozí 4 200 jako ve fake). Do Health Connect se nic nepíše.
     */
    suspend fun resetDay(repo: MovementRepository, steps: Int = DEMO_STEPS): Boolean = when (repo) {
        is StepsMovementRepository -> {
            repo.resetDay(steps)
            true
        }
        is FakeMovementRepository -> {
            repo.setSteps(steps)
            repo.setDebt(0)
            true
        }
        else -> false
    }

    const val DEMO_STEPS = 4_200
}
