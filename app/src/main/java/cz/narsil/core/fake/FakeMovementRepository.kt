package cz.narsil.core.fake

import cz.narsil.core.MovementRates
import cz.narsil.core.MovementRepository
import cz.narsil.core.MovementState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Fake: 4 200 kroků (42 min), bez dluhu. Kroky jde měnit přes [setSteps]/[addSteps]. */
class FakeMovementRepository(
    stepsToday: Int = 4_200,
    debtMinutes: Int = 0,
) : MovementRepository {
    private var spentMinutes = 0
    var squatsTotal = 0
        private set

    private val _state = MutableStateFlow(MovementState(debtMinutes = debtMinutes))
    override val state: StateFlow<MovementState> = _state.asStateFlow()

    init {
        setSteps(stepsToday)
    }

    override suspend fun pay(minutes: Int): Boolean {
        if (_state.value.balanceMinutes < minutes) return false
        spentMinutes += minutes
        recompute()
        return true
    }

    override suspend fun borrow(minutes: Int) {
        _state.update { it.copy(debtMinutes = it.debtMinutes + minutes) }
    }

    override suspend fun recordSquats(count: Int): Int {
        squatsTotal += count
        return count * MovementRates.MINUTES_PER_UNLOCK / MovementRates.SQUATS_PER_UNLOCK
    }

    override suspend fun refresh() = Unit

    fun setSteps(steps: Int) {
        _state.update { it.copy(stepsToday = steps) }
        recompute()
    }

    /** Demo „+1 000 kroků“. */
    fun addSteps(steps: Int = 1_000) = setSteps(_state.value.stepsToday + steps)

    fun setDebt(minutes: Int) = _state.update { it.copy(debtMinutes = minutes) }

    private fun recompute() = _state.update {
        it.copy(balanceMinutes = (it.stepsToday / MovementRates.STEPS_PER_MINUTE - spentMinutes).coerceAtLeast(0))
    }
}
