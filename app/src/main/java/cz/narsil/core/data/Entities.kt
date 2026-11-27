package cz.narsil.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Schéma: docs/SPEC.md 11.5. Enumy jako text (name), datum jako LocalDate.toString() ("2026-11-28").
// Nová entita nebo sloupec → Poznámky v tasku, přidá Marek.

/** Smlouva na den. [plannedContent] = JSON pole řetězců (viz [plannedContentToJson]). */
@Entity(tableName = "contracts")
data class ContractEntity(
    @PrimaryKey val date: String,
    val stepGoal: Int,
    val plannedContent: String = "[]",
    val videoUri: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

/** Úkol plánu dne. [type]/[method]/[status] = názvy enumů z cz.narsil.core. */
@Entity(
    tableName = "plan_tasks",
    indices = [Index("date"), Index("proofHash")],
)
data class PlanTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val type: String,
    val title: String,
    val target: String = "",
    val rewardMinutes: Int,
    val method: String,
    val status: String,
    val attempts: Int = 0,
    val proofUri: String? = null,
    val proofHash: String? = null,
    val verifiedAtMillis: Long? = null,
)

/** Úsek spotřeby z detekce (121). [surface]/[context] = názvy enumů Surface/ComfortContext. */
@Entity(tableName = "consumption_events", indices = [Index("startedAtMillis")])
data class ConsumptionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val surface: String,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val videoTitle: String? = null,
    val url: String? = null,
    val planned: Boolean = false,
    val context: String,
)

/** Zásah Ozvěny (114, 121). [reason] = TriggerReason, [outcome] = InterventionOutcome, [feeling] = nuda/únava/úkol/samota/jiné. */
@Entity(tableName = "interventions", indices = [Index("atMillis")])
data class InterventionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atMillis: Long,
    val packageName: String,
    val reason: String,
    val feeling: String? = null,
    val outcome: String,
    val xpGained: Int = 0,
)

/** Druhy pohybů minut v [LedgerEntryEntity.kind]. */
object LedgerKind {
    const val TASK_REWARD = "TASK_REWARD"
    const val PAID = "PAID"
    const val BORROW = "BORROW"
    const val DEBT_CARRY = "DEBT_CARRY"
}

/** Pohyb minut (203, 211). [minutes] vždy kladné, směr určuje [kind] ([LedgerKind]). */
@Entity(tableName = "ledger", indices = [Index("date")])
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val atMillis: Long,
    val kind: String,
    val minutes: Int,
)

/** Přidané XP (202, 220). [source] = InterventionOutcome, "TASK", "CONTRACT", "VIDEO_RATED", "BOSS", "DEMO"… */
@Entity(tableName = "xp_events", indices = [Index("date")])
data class XpEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val atMillis: Long,
    val source: String,
    val xp: Int,
)

/** Získaný odznak (220); [id] např. "FIRST_CLOSE". */
@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val unlockedAtMillis: Long,
)

/** Záměr a hodnocení videa (120, 321); [rating] 1–5. */
@Entity(tableName = "video_ratings", indices = [Index("atMillis")])
data class VideoRatingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atMillis: Long,
    val title: String,
    val intent: String? = null,
    val rating: Int,
)
