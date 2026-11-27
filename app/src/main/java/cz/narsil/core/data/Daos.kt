package cz.narsil.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Parametr `date` = LocalDate.toString(), viz DayKey.

@Dao
interface ContractDao {
    @Upsert
    suspend fun upsertContract(contract: ContractEntity)

    @Query("SELECT * FROM contracts WHERE date = :date")
    suspend fun contract(date: String): ContractEntity?

    @Query("SELECT * FROM contracts WHERE date = :date")
    fun observeContract(date: String): Flow<ContractEntity?>

    /** Nejnovější smlouva s videem před [date] (Ozvěna, SPEC 9). */
    @Query("SELECT * FROM contracts WHERE date < :date AND videoUri IS NOT NULL ORDER BY date DESC LIMIT 1")
    suspend fun latestWithVideoBefore(date: String): ContractEntity?

    @Insert
    suspend fun insertTask(task: PlanTaskEntity): Long

    @Insert
    suspend fun insertTasks(tasks: List<PlanTaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: PlanTaskEntity)

    @Query("SELECT * FROM plan_tasks WHERE id = :id")
    suspend fun task(id: Long): PlanTaskEntity?

    @Query("SELECT * FROM plan_tasks WHERE date = :date ORDER BY id")
    suspend fun tasksFor(date: String): List<PlanTaskEntity>

    @Query("SELECT * FROM plan_tasks WHERE date = :date ORDER BY id")
    fun observeTasks(date: String): Flow<List<PlanTaskEntity>>

    @Query("DELETE FROM plan_tasks WHERE date = :date")
    suspend fun deleteTasksFor(date: String)

    /** Úkoly čekající na nové ověření (VERIFY_LATER) – 520 je zkusí při další síti. */
    @Query("SELECT * FROM plan_tasks WHERE status = 'VERIFY_LATER' ORDER BY id")
    suspend fun tasksToVerifyLater(): List<PlanTaskEntity>

    /** Byl otisk fotky už použit (u jakéhokoli úkolu kromě [exceptTaskId])? */
    @Query("SELECT COUNT(*) > 0 FROM plan_tasks WHERE proofHash = :hash AND id != :exceptTaskId")
    suspend fun isProofHashUsed(hash: String, exceptTaskId: Long = -1): Boolean

    /** Přepíše smlouvu i s úkoly (úkoly dne se smažou a vloží znovu). */
    @Transaction
    suspend fun replaceContract(contract: ContractEntity, tasks: List<PlanTaskEntity>) {
        upsertContract(contract)
        deleteTasksFor(contract.date)
        insertTasks(tasks)
    }
}

@Dao
interface ConsumptionDao {
    @Insert
    suspend fun insert(event: ConsumptionEventEntity): Long

    @Query("SELECT * FROM consumption_events WHERE startedAtMillis >= :fromMillis AND startedAtMillis < :toMillis ORDER BY startedAtMillis")
    suspend fun between(fromMillis: Long, toMillis: Long): List<ConsumptionEventEntity>

    @Query("SELECT * FROM consumption_events WHERE startedAtMillis >= :fromMillis ORDER BY startedAtMillis")
    fun observeSince(fromMillis: Long): Flow<List<ConsumptionEventEntity>>

    /** Minuty neplánovaného nekonečného obsahu doma od [fromMillis] (souboj s algoritmem, SPEC 8). */
    @Query(
        "SELECT COALESCE(SUM(durationMillis), 0) / 60000 FROM consumption_events " +
            "WHERE startedAtMillis >= :fromMillis AND planned = 0 AND context = 'HOME' " +
            "AND surface IN ('FEED', 'REELS', 'SHORTS')",
    )
    fun observeFeedMinutesSince(fromMillis: Long): Flow<Int>
}

@Dao
interface InterventionDao {
    @Insert
    suspend fun insert(intervention: InterventionEntity): Long

    @Query("SELECT * FROM interventions WHERE atMillis >= :fromMillis AND atMillis < :toMillis ORDER BY atMillis")
    suspend fun between(fromMillis: Long, toMillis: Long): List<InterventionEntity>

    @Query("SELECT * FROM interventions WHERE atMillis >= :fromMillis ORDER BY atMillis")
    fun observeSince(fromMillis: Long): Flow<List<InterventionEntity>>

    /** Poslední zásah s výsledkem [outcome] (strop a odstup u CLOSED, SPEC 4). */
    @Query("SELECT * FROM interventions WHERE outcome = :outcome ORDER BY atMillis DESC LIMIT 1")
    suspend fun lastWithOutcome(outcome: String): InterventionEntity?

    @Query("SELECT COUNT(*) FROM interventions WHERE outcome = :outcome AND atMillis >= :fromMillis")
    suspend fun countWithOutcomeSince(outcome: String, fromMillis: Long): Int

    @Query("SELECT COUNT(*) FROM interventions WHERE outcome = :outcome")
    suspend fun countWithOutcome(outcome: String): Int
}

@Dao
interface LedgerDao {
    @Insert
    suspend fun insert(entry: LedgerEntryEntity): Long

    @Query("SELECT * FROM ledger WHERE date = :date ORDER BY atMillis")
    suspend fun forDate(date: String): List<LedgerEntryEntity>

    @Query("SELECT * FROM ledger WHERE date = :date ORDER BY atMillis")
    fun observeForDate(date: String): Flow<List<LedgerEntryEntity>>

    @Query("SELECT COALESCE(SUM(minutes), 0) FROM ledger WHERE date = :date AND kind = :kind")
    suspend fun sumForDate(date: String, kind: String): Int
}

@Dao
interface XpDao {
    @Insert
    suspend fun insert(event: XpEventEntity): Long

    @Query("SELECT COALESCE(SUM(xp), 0) FROM xp_events")
    fun observeTotal(): Flow<Int>

    @Query("SELECT COALESCE(SUM(xp), 0) FROM xp_events")
    suspend fun total(): Int

    @Query("SELECT * FROM xp_events WHERE date = :date ORDER BY atMillis")
    suspend fun forDate(date: String): List<XpEventEntity>

    @Query("SELECT * FROM xp_events WHERE date = :date ORDER BY atMillis")
    fun observeForDate(date: String): Flow<List<XpEventEntity>>

    @Query("SELECT COUNT(*) FROM xp_events WHERE date = :date AND source = :source")
    suspend fun countForDate(date: String, source: String): Int
}

@Dao
interface BadgeDao {
    /** Vrací -1, když odznak už je. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(badge: BadgeEntity): Long

    @Query("SELECT * FROM badges ORDER BY unlockedAtMillis")
    suspend fun all(): List<BadgeEntity>

    @Query("SELECT * FROM badges ORDER BY unlockedAtMillis")
    fun observeAll(): Flow<List<BadgeEntity>>
}

@Dao
interface VideoRatingDao {
    @Insert
    suspend fun insert(rating: VideoRatingEntity): Long

    @Query("SELECT * FROM video_ratings ORDER BY atMillis DESC")
    fun observeAll(): Flow<List<VideoRatingEntity>>

    @Query("SELECT COUNT(*) FROM video_ratings WHERE atMillis >= :fromMillis")
    suspend fun countSince(fromMillis: Long): Int
}
