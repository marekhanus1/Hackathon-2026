package cz.narsil.core

/**
 * Odměna za ověřený úkol (221, Šimon): [PlanTask.rewardMinutes] do zůstatku
 * ([MovementRepository.addEarnedMinutes]) a +30 XP ([GamificationEngine.onTaskVerified]).
 * Volá ověření (520) po výsledku VERIFIED. Každý úkol se odmění jen jednou.
 */
interface RewardService {
    suspend fun reward(task: PlanTask)
}
