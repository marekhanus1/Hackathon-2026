package cz.narsil.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Jediná Room databáze aplikace (SPEC 11.5). Verze 1, při změně schématu se smaže (hackathon).
 * Instance: `NarsilDatabase.get(context)` (ServiceLocator, 124). Entity a DAO mění jen Marek.
 */
@Database(
    entities = [
        ContractEntity::class,
        PlanTaskEntity::class,
        ConsumptionEventEntity::class,
        InterventionEntity::class,
        LedgerEntryEntity::class,
        XpEventEntity::class,
        BadgeEntity::class,
        VideoRatingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NarsilDatabase : RoomDatabase() {
    abstract fun contractDao(): ContractDao
    abstract fun consumptionDao(): ConsumptionDao
    abstract fun interventionDao(): InterventionDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun xpDao(): XpDao
    abstract fun badgeDao(): BadgeDao
    abstract fun videoRatingDao(): VideoRatingDao

    companion object {
        const val NAME = "narsil.db"

        @Volatile
        private var instance: NarsilDatabase? = null

        fun get(context: Context): NarsilDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, NarsilDatabase::class.java, NAME)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
