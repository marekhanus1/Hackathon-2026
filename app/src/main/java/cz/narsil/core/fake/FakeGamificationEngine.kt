package cz.narsil.core.fake

import cz.narsil.core.AvatarState
import cz.narsil.core.GamificationEngine
import cz.narsil.core.GamificationState
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.MovementRates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fake s výchozími čísly z briefu (zavřít 50, dřepy 20, kroky 10, půjčka 0, smlouva 100, hodnocení 5).
 * Skutečná pravidla: docs/GAMIFICATION.md (201) a 202/220.
 */
class FakeGamificationEngine(
    initial: GamificationState = DEMO_STATE,
) : GamificationEngine {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<GamificationState> = _state.asStateFlow()

    override suspend fun onOutcome(outcome: InterventionOutcome): Int {
        val xp = when (outcome) {
            InterventionOutcome.CLOSED -> 50
            InterventionOutcome.SQUATS -> 20
            InterventionOutcome.PAID_STEPS -> 10
            InterventionOutcome.BORROWED, InterventionOutcome.PLANNED_FREE -> 0
        }
        val minutes = MovementRates.MINUTES_PER_UNLOCK
        _state.update {
            val score = when (outcome) {
                InterventionOutcome.CLOSED -> it.today.copy(reclaimedMinutes = it.today.reclaimedMinutes + minutes)
                InterventionOutcome.PLANNED_FREE -> it.today
                else -> it.today.copy(feedMinutes = it.today.feedMinutes + minutes)
            }
            it.copy(today = score)
        }
        addXp(xp)
        return xp
    }

    override suspend fun onContractFulfilled(): Int {
        _state.update { it.copy(streakDays = it.streakDays + 1) }
        addXp(100)
        return 100
    }

    override suspend fun onVideoRated(rating: Int): Int {
        addXp(5)
        return 5
    }

    /** Demo „+500 XP“. */
    fun addXp(xp: Int) = _state.update {
        val total = it.xp + xp
        it.copy(xp = total, level = 1 + total / it.xpPerLevel, xpInLevel = total % it.xpPerLevel)
    }

    fun setAvatar(avatar: AvatarState) = _state.update { it.copy(avatar = avatar) }

    fun setState(state: GamificationState) {
        _state.value = state
    }

    companion object {
        val DEMO_STATE = GamificationState(
            xp = 1_340,
            level = 3,
            xpInLevel = 340,
            streakDays = 4,
            jokers = 1,
            avatar = AvatarState.FIT,
            wonDays = 9,
            badges = listOf("left_alone_10"),
        )
    }
}
