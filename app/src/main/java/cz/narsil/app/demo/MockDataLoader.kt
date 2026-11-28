package cz.narsil.app.demo

import android.content.Context
import cz.narsil.core.data.ConsumptionEventEntity
import cz.narsil.core.data.DayKey
import cz.narsil.core.data.InterventionEntity
import cz.narsil.core.data.NarsilDatabase
import cz.narsil.core.data.VideoRatingEntity
import cz.narsil.core.data.XpEventEntity
import cz.narsil.gamification.GamificationRules
import cz.narsil.gamification.XpSource
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Nahraje připravená data (SPEC 10, demo panel) do sdílené databáze: 21 dní spotřeby, zásahů a XP
 * + hodnocení videí. Data dává [MockDataGenerator] přibalený v APK (138) – žádné soubory nejsou
 * potřeba; JSON v `mockdata/` zůstávají jako podklad pro pitch. Datum se posune tak, aby poslední den
 * byl včera. Před nahráním smaže historii ([clearStatements], 147), takže výsledek je vždy stejný
 * (480 / 500 XP, série 4, 1 žolík, postava FIT) bez ohledu na to, co v telefonu bylo. Smlouvy a úkoly
 * na dnešek a dál zůstanou.
 */
object MockDataLoader {
    private const val PREFS = "demo"
    private const val KEY_LOADED = "mock_loaded"

    val PACKAGES = mapOf(
        "instagram" to "com.instagram.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "youtube" to "com.google.android.youtube",
        "firefox" to "org.mozilla.firefox",
    )

    data class Result(val days: Int, val interventions: Int, val ratings: Int, val alreadyLoaded: Boolean = false)

    /** Řádky pro databázi – čistý Kotlin, testovatelné bez Androidu. */
    data class Rows(
        val consumption: List<ConsumptionEventEntity>,
        val interventions: List<InterventionEntity>,
        val xp: List<XpEventEntity>,
        val ratings: List<VideoRatingEntity>,
    )

    fun isLoaded(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_LOADED, false)

    /** Mazání historie před nahráním: vše kromě smluv a úkolů od [today] dál. */
    fun clearStatements(today: LocalDate): List<String> = listOf(
        "DELETE FROM xp_events",
        "DELETE FROM ledger",
        "DELETE FROM interventions",
        "DELETE FROM consumption_events",
        "DELETE FROM video_ratings",
        "DELETE FROM badges",
        "DELETE FROM plan_tasks WHERE date < '$today'",
        "DELETE FROM contracts WHERE date < '$today'",
    )

    /** Data z [MockDataGenerator] posunutá tak, aby poslední den byl den před [today]. */
    fun rows(today: LocalDate, zone: ZoneId): Rows {
        val days = MockDataGenerator.days()
        val shift = ChronoUnit.DAYS.between(days.last().date, today.minusDays(1))
        fun millis(date: LocalDate, time: String) = date.atTime(LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli()
        val consumption = mutableListOf<ConsumptionEventEntity>()
        val interventions = mutableListOf<InterventionEntity>()
        val xp = mutableListOf<XpEventEntity>()
        for (day in days) {
            val date = day.date.plusDays(shift)
            val key = date.toString()
            // Spotřeba po aplikacích (feed doma, neplánovaná) – souboj s algoritmem a Zrcadlo.
            day.byApp.forEach { (appName, min) ->
                consumption += ConsumptionEventEntity(
                    packageName = PACKAGES[appName] ?: appName,
                    surface = "FEED",
                    startedAtMillis = millis(date, "20:00"),
                    durationMillis = min * 60_000L,
                    planned = false,
                    context = "HOME",
                )
            }
            // Zásahy + XP podle pravidel gamifikace (Zavřít 50 nejvýš 5× denně, dřepy 20, kroky 10, půjčka 0).
            var closes = 0
            day.interventions.forEach { i ->
                val at = millis(date, i.time)
                val reward = if (i.outcome == XpSource.CLOSED) closes++ < GamificationRules.CLOSED_MAX_REWARDED_PER_DAY else true
                val gained = if (reward) MockDataGenerator.xpFor(i.outcome) else 0
                interventions += InterventionEntity(
                    atMillis = at,
                    packageName = PACKAGES[i.app] ?: i.app,
                    reason = "ENDLESS_SCROLL",
                    feeling = i.avoiding,
                    outcome = i.outcome,
                    xpGained = gained,
                )
                xp += XpEventEntity(date = key, atMillis = at, source = i.outcome, xp = gained)
            }
            if (day.extraSquats >= GamificationRules.SQUATS_PER_JOKER) {
                xp += XpEventEntity(date = key, atMillis = millis(date, "18:00"), source = XpSource.JOKER, xp = 0)
            }
            if (day.bossSurvived) {
                xp += XpEventEntity(date = key, atMillis = millis(date, "22:00"), source = XpSource.BOSS, xp = GamificationRules.XP_BOSS)
            }
            // Splněná smlouva: +100 XP a den do série / postavy.
            if (day.contractFulfilled) {
                xp += XpEventEntity(date = key, atMillis = millis(date, "23:00"), source = XpSource.CONTRACT, xp = GamificationRules.XP_CONTRACT)
            }
        }
        // Hodnocení videí (mapa hodnoty), stejný posun data; +5 XP nejvýš 10× denně.
        val ratings = MockDataGenerator.RATINGS.mapIndexed { index, r ->
            val date = MockDataGenerator.START.plusDays(r.day - 1L + shift)
            val at = millis(date, "19:00") + index * 60_000L
            xp += XpEventEntity(date = date.toString(), atMillis = at, source = XpSource.VIDEO_RATED, xp = GamificationRules.XP_VIDEO_RATED)
            VideoRatingEntity(atMillis = at, title = r.title, intent = r.intent.ifBlank { null }, rating = r.rating)
        }
        return Rows(consumption, interventions, xp, ratings)
    }

    suspend fun load(context: Context, zone: ZoneId = ZoneId.systemDefault()): Result {
        val db = NarsilDatabase.get(context.applicationContext)
        val today = DayKey.today(zone)
        val rows = rows(today, zone)
        // runInTransaction → Room po dokončení upozorní pozorovatele (Domov, Zrcadlo se překreslí).
        db.runInTransaction { clearStatements(today).forEach { db.openHelper.writableDatabase.execSQL(it) } }
        rows.consumption.forEach { db.consumptionDao().insert(it) }
        rows.interventions.forEach { db.interventionDao().insert(it) }
        rows.xp.forEach { db.xpDao().insert(it) }
        rows.ratings.forEach { db.videoRatingDao().insert(it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_LOADED, true).apply()
        return Result(MockDataGenerator.DAYS, rows.interventions.size, rows.ratings.size)
    }
}
