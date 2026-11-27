package cz.narsil.gamification.reward

import android.content.Context
import cz.narsil.core.GamificationEngine
import cz.narsil.core.MovementRepository
import cz.narsil.core.PlanTask
import cz.narsil.core.RewardService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Které úkoly už dostaly odměnu (přežije restart, ať se úkol neodmění dvakrát). */
interface RewardLog {
    suspend fun isRewarded(taskId: Long): Boolean
    suspend fun markRewarded(taskId: Long)
}

class InMemoryRewardLog : RewardLog {
    private val ids = mutableSetOf<Long>()
    override suspend fun isRewarded(taskId: Long): Boolean = taskId in ids
    override suspend fun markRewarded(taskId: Long) {
        ids += taskId
    }
}

/** Trvalý záznam v SharedPreferences (id úkolů jsou v Room jedinečná napříč dny). */
class PrefsRewardLog(context: Context) : RewardLog {
    private val prefs = context.applicationContext.getSharedPreferences("narsil_rewarded_tasks", Context.MODE_PRIVATE)

    override suspend fun isRewarded(taskId: Long): Boolean = prefs.getBoolean(taskId.toString(), false)

    override suspend fun markRewarded(taskId: Long) {
        prefs.edit().putBoolean(taskId.toString(), true).apply()
    }
}

/**
 * Odměna za ověřený úkol (221, SPEC 7 a 11.2): [PlanTask.rewardMinutes] do zůstatku
 * ([MovementRepository.addEarnedMinutes]) a +30 XP ([GamificationEngine.onTaskVerified]).
 * Volá ověření (520) po výsledku VERIFIED. Každý úkol (podle id) se odmění jen jednou,
 * i při souběžném nebo opakovaném volání a po restartu ([log]). Nic nehází.
 */
class TaskRewardService(
    private val movement: MovementRepository,
    private val gamification: GamificationEngine,
    private val log: RewardLog = InMemoryRewardLog(),
) : RewardService {
    private val mutex = Mutex()

    override suspend fun reward(task: PlanTask) {
        mutex.withLock {
            if (runCatching { log.isRewarded(task.id) }.getOrDefault(false)) return
            runCatching { log.markRewarded(task.id) }
            runCatching { movement.addEarnedMinutes(task.rewardMinutes) }
            runCatching { gamification.onTaskVerified(task) }
        }
    }

    companion object {
        /** Sestavení pro `ServiceLocator` (124). */
        fun create(context: Context, movement: MovementRepository, gamification: GamificationEngine) =
            TaskRewardService(movement, gamification, PrefsRewardLog(context))
    }
}
