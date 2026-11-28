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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Nahraje připravená data Štěpána (430, `mockdata/`, schéma `mockdata/schema.md`) do sdílené databáze
 * (SPEC 10, demo panel): 21 dní spotřeby, zásahů a XP + hodnocení videí. Datum se posune tak, aby
 * poslední den byl včera. Jen jednou (příznak v prefs); znovu po „smazat data aplikace“.
 */
object MockDataLoader {
    private const val PREFS = "demo"
    private const val KEY_LOADED = "mock_loaded"

    private val PACKAGES = mapOf(
        "instagram" to "com.instagram.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
    )

    data class Result(val days: Int, val interventions: Int, val ratings: Int, val alreadyLoaded: Boolean = false)

    fun isLoaded(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_LOADED, false)

    suspend fun load(context: Context, zone: ZoneId = ZoneId.systemDefault()): Result {
        if (isLoaded(context)) return Result(0, 0, 0, alreadyLoaded = true)
        val app = context.applicationContext
        val db = NarsilDatabase.get(app)
        val json = Json { ignoreUnknownKeys = true }
        val mirror = json.parseToJsonElement(app.assets.open("mirror.json").bufferedReader().use { it.readText() }).jsonObject
        val days = mirror["days"]?.jsonArray.orEmpty().map { it.jsonObject }
        if (days.isEmpty()) return Result(0, 0, 0)

        // Posun: poslední den v datech = včera.
        val lastDate = days.maxOf { LocalDate.parse(it.str("date")) }
        val shift = ChronoUnit.DAYS.between(lastDate, DayKey.today(zone).minusDays(1))
        fun shifted(date: String) = LocalDate.parse(date).plusDays(shift)
        fun millis(date: LocalDate, time: String) = date.atTime(LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli()

        var interventions = 0
        for (day in days) {
            val date = shifted(day.str("date"))
            val key = date.toString()
            // Spotřeba po aplikacích (feed doma, neplánovaná) – souboj s algoritmem a Zrcadlo.
            day["by_app"]?.jsonObject?.forEach { (appName, minutes) ->
                val min = minutes.jsonPrimitive.intOrNull ?: return@forEach
                db.consumptionDao().insert(
                    ConsumptionEventEntity(
                        packageName = PACKAGES[appName] ?: appName,
                        surface = "FEED",
                        startedAtMillis = millis(date, "20:00"),
                        durationMillis = min * 60_000L,
                        planned = false,
                        context = "HOME",
                    ),
                )
            }
            // Zásahy + XP podle pravidel gamifikace (Zavřít 50, dřepy 20, kroky 10, půjčka 0).
            day["interventions"]?.jsonArray?.forEach { el ->
                val i = el.jsonObject
                val at = millis(date, i.str("time"))
                val outcome = i.str("outcome")
                val xp = when (outcome) {
                    XpSource.CLOSED -> GamificationRules.XP_CLOSED
                    XpSource.SQUATS -> GamificationRules.XP_SQUATS
                    XpSource.PAID_STEPS -> GamificationRules.XP_PAID_STEPS
                    else -> 0
                }
                db.interventionDao().insert(
                    InterventionEntity(
                        atMillis = at,
                        packageName = PACKAGES[i.str("app")] ?: i.str("app"),
                        reason = "ENDLESS_SCROLL",
                        feeling = i["avoiding"]?.jsonPrimitive?.contentOrNull,
                        outcome = outcome,
                        xpGained = xp,
                    ),
                )
                db.xpDao().insert(XpEventEntity(date = key, atMillis = at, source = outcome, xp = xp))
                interventions++
            }
            // Splněná smlouva: +100 XP a den do série / postavy.
            if (day["contract_fulfilled"]?.jsonPrimitive?.booleanOrNull == true) {
                db.xpDao().insert(
                    XpEventEntity(date = key, atMillis = millis(date, "23:00"), source = XpSource.CONTRACT, xp = GamificationRules.XP_CONTRACT),
                )
            }
        }

        // Hodnocení videí (mapa hodnoty), stejný posun data.
        var ratings = 0
        runCatching {
            val valueMap = json.parseToJsonElement(app.assets.open("value_map.json").bufferedReader().use { it.readText() }).jsonObject
            valueMap["ratings"]?.jsonArray?.forEachIndexed { index, el ->
                val r = el.jsonObject
                db.videoRatingDao().insert(
                    VideoRatingEntity(
                        atMillis = millis(shifted(r.str("date")), "19:00") + index * 60_000L,
                        title = r.str("title"),
                        intent = r["intent"]?.jsonPrimitive?.contentOrNull?.ifBlank { null },
                        rating = r["rating"]?.jsonPrimitive?.intOrNull ?: 3,
                    ),
                )
                ratings++
            }
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_LOADED, true).apply()
        return Result(days.size, interventions, ratings)
    }

    private fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    private fun JsonArray?.orEmpty(): List<kotlinx.serialization.json.JsonElement> = this ?: emptyList()
}
