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
}
