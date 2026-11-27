package cz.narsil.core.fake

import cz.narsil.core.GamificationEngine
import cz.narsil.core.MovementRepository
import cz.narsil.core.PlanTask
import cz.narsil.core.RewardService

/** Fake: minuty do [movement], XP do [gamification]; každý úkol (podle id) jen jednou. */
class FakeRewardService(
    private val movement: MovementRepository,
    private val gamification: GamificationEngine,
) : RewardService {
    private val rewarded = mutableSetOf<Long>()

    override suspend fun reward(task: PlanTask) {
        if (!rewarded.add(task.id)) return
        movement.addEarnedMinutes(task.rewardMinutes)
        gamification.onTaskVerified(task)
    }
}
