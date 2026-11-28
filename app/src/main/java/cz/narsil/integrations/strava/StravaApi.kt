package cz.narsil.integrations.strava

import cz.narsil.core.StravaActivity
import java.net.URLEncoder
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** OAuth token Stravy. [expiresAtSec] = epoch sekundy (pole `expires_at`). */
data class StravaToken(val accessToken: String, val refreshToken: String, val expiresAtSec: Long) {
    fun isExpired(nowMillis: Long, marginSec: Long = 60): Boolean = nowMillis / 1000 >= expiresAtSec - marginSec
}

/** Přístupové údaje aplikace Stravy (registrace 502). Jen z `local.properties`, nikdy v gitu. */
data class StravaConfig(val clientId: String, val clientSecret: String, val redirectUri: String = REDIRECT_URI) {
    val isConfigured: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    companion object {
        /** Deep link (manifest: intent-filter `narsil://strava` na MainActivity – přidá Marek). */
        const val REDIRECT_URI = "narsil://strava"

        /** `STRAVA_CLIENT_ID` / `STRAVA_CLIENT_SECRET` v BuildConfig (přidá Marek do build.gradle.kts). */
        fun fromBuildConfig(): StravaConfig {
            fun field(name: String) = runCatching {
                Class.forName("cz.narsil.BuildConfig").getField(name).get(null) as? String
            }.getOrNull().orEmpty()
            return StravaConfig(field("STRAVA_CLIENT_ID"), field("STRAVA_CLIENT_SECRET"))
        }
    }
}

/** Strava API v3: adresy a parsování odpovědí (511). Čistý Kotlin. */
object StravaApi {
    const val AUTHORIZE_URL = "https://www.strava.com/oauth/mobile/authorize"
    const val TOKEN_URL = "https://www.strava.com/oauth/token"
    const val ACTIVITIES_URL = "https://www.strava.com/api/v3/athlete/activities"
    const val SCOPE = "activity:read"

    private val json = Json { ignoreUnknownKeys = true }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    fun authorizeUrl(config: StravaConfig): String =
        "$AUTHORIZE_URL?client_id=${enc(config.clientId)}&redirect_uri=${enc(config.redirectUri)}" +
            "&response_type=code&approval_prompt=auto&scope=${enc(SCOPE)}"

    fun exchangeForm(config: StravaConfig, code: String) = mapOf(
        "client_id" to config.clientId,
        "client_secret" to config.clientSecret,
        "code" to code,
        "grant_type" to "authorization_code",
    )

    fun refreshForm(config: StravaConfig, refreshToken: String) = mapOf(
        "client_id" to config.clientId,
        "client_secret" to config.clientSecret,
        "refresh_token" to refreshToken,
        "grant_type" to "refresh_token",
    )

    /** Aktivity od [afterEpochSec] (začátek dne ve 4:00), nejvýš 30. */
    fun activitiesUrl(afterEpochSec: Long): String = "$ACTIVITIES_URL?after=$afterEpochSec&per_page=30"

    /** Kód z návratu `narsil://strava?code=…&scope=…`; chyba nebo zamítnutí = null. */
    fun codeFromRedirect(uri: String): String? {
        val query = uri.substringAfter('?', "")
        if (query.isEmpty()) return null
        val params = query.split('&').mapNotNull {
            val (k, v) = it.split('=', limit = 2).let { p -> p[0] to p.getOrElse(1) { "" } }
            if (k.isEmpty()) null else k to java.net.URLDecoder.decode(v, "UTF-8")
        }.toMap()
        if (params.containsKey("error")) return null
        return params["code"]?.takeIf { it.isNotBlank() }
    }

    fun parseToken(body: String): StravaToken? = runCatching {
        val o = json.parseToJsonElement(body) as JsonObject
        StravaToken(
            accessToken = o.str("access_token") ?: return null,
            refreshToken = o.str("refresh_token") ?: return null,
            expiresAtSec = (o["expires_at"] as? JsonPrimitive)?.longOrNull ?: return null,
        )
    }.getOrNull()

    /** Seznam aktivit; `sport_type` se převede na základní typ ([baseType]), aby seděl na `MOVEMENT_TYPES`. */
    fun parseActivities(body: String): List<StravaActivity> = runCatching {
        (json.parseToJsonElement(body) as JsonArray).mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val id = (o["id"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null
            val start = o.str("start_date")?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: return@mapNotNull null
            StravaActivity(
                id = id,
                type = baseType(o.str("sport_type") ?: o.str("type").orEmpty()),
                startMillis = start,
                movingTimeSec = ((o["moving_time"] as? JsonPrimitive)?.longOrNull ?: 0L).toInt(),
                distanceMeters = ((o["distance"] as? JsonPrimitive)?.doubleOrNull ?: 0.0).toFloat(),
            )
        }
    }.getOrDefault(emptyList())

    /** Varianty sportů Stravy → Run / Ride / Walk / Workout (SPEC 7: běh, kolo, chůze, trénink). */
    fun baseType(sport: String): String = when (sport) {
        "Run", "TrailRun", "VirtualRun" -> "Run"
        "Ride", "MountainBikeRide", "GravelRide", "EBikeRide", "EMountainBikeRide", "VirtualRide", "Velomobile" -> "Ride"
        "Walk", "Hike" -> "Walk"
        "Workout", "WeightTraining", "Crossfit", "HighIntensityIntervalTraining", "Elliptical", "StairStepper",
        "Yoga", "Pilates", "RockClimbing", "Rowing", "VirtualRow" -> "Workout"
        else -> sport
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
