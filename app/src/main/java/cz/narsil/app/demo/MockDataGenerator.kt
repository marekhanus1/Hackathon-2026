package cz.narsil.app.demo

import cz.narsil.gamification.GamificationRules
import cz.narsil.gamification.XpSource
import java.time.LocalDate

/**
 * Generátor připravených dat přibalený v APK (138): stejný příběh 21 dní jako `mockdata/generuj.py`
 * (Štěpán, 430), takže „Nahrát mock data“ nepotřebuje žádné soubory. Deterministický.
 *
 * Den 1 = [start], den 21 = [start] + 20 dní. Loader data posune tak, aby poslední den byl včera.
 */
object MockDataGenerator {
    val START: LocalDate = LocalDate.of(2026, 11, 7)
    const val DAYS = 21

    data class Intervention(val time: String, val app: String, val surface: String, val outcome: String, val avoiding: String?)

    data class Day(
        val index: Int,
        val date: LocalDate,
        val feedMinutes: Int,
        val reclaimedMinutes: Int,
        val plannedMinutes: Int,
        val byApp: Map<String, Int>,
        val interventions: List<Intervention>,
        val steps: Int,
        val squats: Int,
        val extraSquats: Int,
        val contractFulfilled: Boolean,
        val bossSurvived: Boolean,
        val ratingsCount: Int,
        val xpGained: Int,
    ) {
        val battleWon: Boolean get() = reclaimedMinutes > feedMinutes
    }

    data class Rating(val day: Int, val channel: String, val title: String, val intent: String, val planned: Boolean, val watchedPercent: Int, val rating: Int)

    private val OUT = mapOf("C" to XpSource.CLOSED, "S" to XpSource.SQUATS, "P" to XpSource.PAID_STEPS, "B" to XpSource.BORROWED)
    private val APPS = mapOf("ig" to "instagram", "tt" to "tiktok", "yt" to "youtube", "ff" to "firefox")

    private val INTERVENTIONS = mapOf(
        1 to listOf("20:10 ig REELS P nuda", "21:47 tt FEED B unava", "22:30 ig REELS S ukol"),
        2 to listOf("19:40 tt FEED P nuda", "21:15 tt FEED B unava", "22:05 ff SHORTS B unava"),
        3 to listOf("20:30 ig REELS S ukol", "21:30 tt FEED P nuda"),
        4 to listOf("19:55 tt FEED B samota", "21:20 ig REELS B unava", "22:40 tt FEED P nuda"),
        5 to listOf("20:20 tt FEED C nuda", "21:35 ig REELS P unava", "22:50 tt FEED B unava"),
        6 to listOf("20:05 tt FEED S ukol", "21:10 ig REELS C nuda"),
        7 to listOf("19:30 ff VIDEO P nuda", "21:40 tt FEED B samota", "22:15 ig REELS C unava"),
        8 to listOf("20:45 tt FEED C ukol", "21:25 ig REELS S nuda"),
        9 to listOf("20:00 tt FEED C nuda", "21:05 tt FEED P unava", "22:20 ig REELS S ukol"),
        10 to listOf("19:50 ig REELS C ukol", "21:30 tt FEED C nuda"),
        11 to listOf("20:15 tt FEED C nuda", "21:45 ff SHORTS S unava"),
        12 to listOf("19:45 tt FEED P samota", "21:20 tt FEED C nuda", "22:35 ig REELS B unava"),
        13 to listOf("20:30 ig REELS C ukol", "21:15 tt FEED C nuda", "22:10 tt FEED S unava"),
        14 to listOf("20:05 tt FEED C nuda", "21:50 ig REELS P samota"),
        15 to listOf("19:40 tt FEED C ukol", "21:00 tt FEED C nuda", "22:25 yt SHORTS S unava"),
        16 to listOf("20:20 ig REELS C nuda", "21:35 tt FEED C ukol"),
        17 to listOf("20:10 tt FEED S unava", "21:25 tt FEED B unava", "22:45 ig REELS C nuda"),
        18 to listOf("20:00 tt FEED C ukol", "21:10 ig REELS C nuda", "22:00 tt FEED S unava"),
        19 to listOf("19:35 ig REELS C nuda", "21:20 tt FEED C ukol"),
        20 to listOf("20:25 tt FEED C nuda", "21:40 yt SHORTS C ukol"),
        21 to listOf("20:10 ig REELS C ukol", "21:30 tt FEED C nuda", "22:20 tt FEED C unava"),
    )
    private val FULFILLED = setOf(3, 6, 8, 10, 11, 13, 15, 16, 18, 19, 20, 21)
    private val EXTRA_SQUATS = mapOf(19 to 50)
    private val FEED = listOf(142, 135, 118, 128, 110, 95, 104, 88, 82, 70, 66, 72, 58, 61, 49, 45, 63, 41, 38, 36, 33)
    private val STEPS = listOf(
        4200, 3900, 6400, 4500, 5200, 6800, 4900, 6900, 5600, 7200, 7400, 5800, 7600, 6100,
        8100, 7900, 5300, 8400, 8600, 8800, 8900,
    )
    private val PLANNED = listOf(45, 30, 50, 20, 40, 55, 25, 60, 35, 50, 45, 30, 55, 40, 60, 50, 20, 55, 60, 45, 50)
    private val TASK_REWARDS = listOf(0, 0, 20, 0, 10, 30, 0, 40, 20, 40, 50, 20, 40, 30, 60, 50, 10, 60, 50, 50, 60)

    val RATINGS: List<Rating> = listOf(
        Rating(8, "Rust za kafe", "Ownership za 20 minut", "pochopit borrow checker", true, 95, 5),
        Rating(8, "Drama Recap CZ", "Kdo s kým TENTOKRÁT", "", false, 90, 2),
        Rating(9, "Memes denně", "Když je pondělí #312", "", false, 100, 2),
        Rating(9, "Kuchyně za minutu", "Těstoviny za 60 s", "nápad na večeři", false, 80, 4),
        Rating(10, "Rust za kafe", "Lifetimes bez bolesti", "lifetimes", true, 88, 5),
        Rating(10, "Drama Recap CZ", "Rozchod roku?!", "", false, 85, 1),
        Rating(11, "Fyzika v kostce", "Proč je nebe modré", "", true, 92, 4),
        Rating(11, "Kočičí svět", "Kočka vs. okurka", "", false, 100, 3),
        Rating(12, "Drama Recap CZ", "Další skandál", "", false, 95, 1),
        Rating(12, "Memes denně", "Když je pondělí #318", "", false, 90, 2),
        Rating(12, "Rust za kafe", "Traity v praxi", "traity", true, 70, 4),
        Rating(13, "Kuchyně za minutu", "Polévka za 60 s", "", false, 75, 4),
        Rating(13, "Drama Recap CZ", "Kdo lže?", "", false, 90, 2),
        Rating(14, "Fyzika v kostce", "Černé díry za 15 minut", "na odpočinek", true, 96, 5),
        Rating(14, "Memes denně", "Když je pondělí #321", "", false, 85, 1),
        Rating(15, "Rust za kafe", "Async Rust", "async", true, 90, 5),
        Rating(15, "Kočičí svět", "Kočka a krabice", "", false, 60, 3),
        Rating(16, "Drama Recap CZ", "Shrnutí týdne", "", false, 88, 2),
        Rating(16, "Rust za kafe", "Chybové typy", "error handling", true, 85, 4),
        Rating(17, "Drama Recap CZ", "Konec přátelství?", "", false, 98, 1),
        Rating(17, "Memes denně", "Když je pondělí #330", "", false, 100, 2),
        Rating(17, "Drama Recap CZ", "Reakce na reakci", "", false, 92, 1),
        Rating(18, "Fyzika v kostce", "Kvantové tunelování", "", true, 90, 5),
        Rating(19, "Rust za kafe", "Makra pro začátečníky", "makra", true, 93, 5),
        Rating(19, "Kuchyně za minutu", "Rizoto za 60 s", "", false, 70, 3),
        Rating(20, "Fyzika v kostce", "Entropie lidsky", "", true, 88, 4),
        Rating(18, "Kuchyně za minutu", "Kari za 60 s", "", false, 65, 4),
        Rating(18, "Drama Recap CZ", "Kdo vyhrál hádku", "", false, 80, 2),
        Rating(18, "Rust za kafe", "Iterátory", "iterátory", true, 91, 5),
        Rating(19, "Fyzika v kostce", "Proč padají letadla? (nepadají)", "", true, 89, 4),
        Rating(19, "Memes denně", "Když je pondělí #335", "", false, 70, 2),
        Rating(19, "Kočičí svět", "Kočka na klávesnici", "", false, 55, 3),
        Rating(20, "Rust za kafe", "Unsafe Rust", "unsafe", true, 86, 4),
        Rating(20, "Kuchyně za minutu", "Palačinky za 60 s", "", false, 72, 4),
        Rating(20, "Drama Recap CZ", "Usmíření?", "", false, 75, 2),
        Rating(21, "Příroda zblízka", "Divoká příroda Šumavy – dokument", "dokument o přírodě", true, 97, 5),
        Rating(21, "Fyzika v kostce", "Teplota a pohyb molekul", "", true, 90, 5),
        Rating(21, "Memes denně", "Když je pondělí #340", "", false, 60, 2),
    )

    fun xpFor(outcome: String): Int = when (outcome) {
        XpSource.CLOSED -> GamificationRules.XP_CLOSED
        XpSource.SQUATS -> GamificationRules.XP_SQUATS
        XpSource.PAID_STEPS -> GamificationRules.XP_PAID_STEPS
        else -> 0
    }

    private fun parse(entry: String): Intervention {
        val (time, app, surface, outcome, avoiding) = entry.split(' ')
        return Intervention(time, APPS.getValue(app), surface, OUT.getValue(outcome), avoiding.takeIf { it != "-" })
    }

    fun days(start: LocalDate = START): List<Day> = (1..DAYS).map { i ->
        val ints = INTERVENTIONS.getValue(i).map(::parse)
        val closes = ints.count { it.outcome == XpSource.CLOSED }
        val inBoss = ints.filter { it.time.substring(0, 2).toInt() == GamificationRules.BOSS_START_HOUR }
        val boss = inBoss.all { it.outcome == XpSource.CLOSED }
        val rated = RATINGS.count { it.day == i }
        val xp = ints.filter { it.outcome != XpSource.CLOSED }.sumOf { xpFor(it.outcome) } +
            GamificationRules.XP_CLOSED * minOf(GamificationRules.CLOSED_MAX_REWARDED_PER_DAY, closes) +
            (if (i in FULFILLED) GamificationRules.XP_CONTRACT else 0) +
            (if (boss) GamificationRules.XP_BOSS else 0) +
            GamificationRules.XP_VIDEO_RATED * minOf(GamificationRules.VIDEO_RATED_MAX_PER_DAY, rated)
        val feed = FEED[i - 1]
        // Rozpad minut feedu podle počtu zásahů v aplikaci (zbytek dostane poslední aplikace abecedně).
        val weights = ints.groupingBy { it.app }.eachCount()
        val apps = weights.keys.sorted()
        var left = feed
        val byApp = apps.mapIndexed { n, a ->
            val share = if (n == apps.lastIndex) left else Math.rint(feed.toDouble() * weights.getValue(a) / ints.size).toInt() // jako round() v Pythonu
            left -= share
            a to share
        }.toMap()
        val extra = EXTRA_SQUATS[i] ?: 0
        Day(
            index = i,
            date = start.plusDays(i - 1L),
            feedMinutes = feed,
            reclaimedMinutes = GamificationRules.RECLAIMED_MINUTES_PER_CLOSE * closes + TASK_REWARDS[i - 1],
            plannedMinutes = PLANNED[i - 1],
            byApp = byApp,
            interventions = ints,
            steps = STEPS[i - 1],
            squats = 20 * ints.count { it.outcome == XpSource.SQUATS } + extra,
            extraSquats = extra,
            contractFulfilled = i in FULFILLED,
            bossSurvived = boss,
            ratingsCount = rated,
            xpGained = xp,
        )
    }
}
