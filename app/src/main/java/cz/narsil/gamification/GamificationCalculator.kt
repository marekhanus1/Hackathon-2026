package cz.narsil.gamification

import cz.narsil.core.AvatarState
import cz.narsil.core.DailyScore
import cz.narsil.core.GamificationState
import java.time.LocalDate
import java.time.ZoneId

/**
 * Čistá pravidla gamifikace (docs/GAMIFICATION.md). Bez stavu a bez Androidu:
 * vše se počítá z historie [XpRecord] – po restartu stačí načíst události.
 */
object GamificationCalculator {

    /**
     * Historie po resetech dne (222): u dne s [XpSource.DAY_RESET] se zahodí události do posledního resetu
     * včetně značky samotné. Ostatní funkce dostávají už očištěné záznamy.
     */
    fun effective(records: List<XpRecord>): List<XpRecord> {
        // Podle pořadí v historii (ne podle času) – události ve stejné milisekundě jako reset rozhodne pořadí zápisu.
        val lastResetIndex = mutableMapOf<java.time.LocalDate, Int>()
        records.forEachIndexed { i, r -> if (r.source == XpSource.DAY_RESET) lastResetIndex[r.date] = i }
        if (lastResetIndex.isEmpty()) return records
        return records.filterIndexed { i, r ->
            r.source != XpSource.DAY_RESET && i > (lastResetIndex[r.date] ?: -1)
        }
    }

    /** XP za CLOSED: 50, nejvýš 5× denně a s odstupem ≥ 10 min, jinak 0 (SPEC 4). */
    fun closeXp(records: List<XpRecord>, today: LocalDate, nowMillis: Long): Int {
        val rewarded = records.filter { it.date == today && it.source == XpSource.CLOSED && it.xp > 0 }
        if (rewarded.size >= GamificationRules.CLOSED_MAX_REWARDED_PER_DAY) return 0
        val last = rewarded.maxOfOrNull { it.atMillis }
        if (last != null && nowMillis - last < GamificationRules.CLOSED_MIN_GAP_MILLIS) return 0
        return GamificationRules.XP_CLOSED
    }

    /** XP za ohodnocené video: 5, nejvýš 10× denně. */
    fun videoRatedXp(records: List<XpRecord>, today: LocalDate): Int {
        val rewarded = records.count { it.date == today && it.source == XpSource.VIDEO_RATED && it.xp > 0 }
        return if (rewarded >= GamificationRules.VIDEO_RATED_MAX_PER_DAY) 0 else GamificationRules.XP_VIDEO_RATED
    }

    fun hasContractXp(records: List<XpRecord>, date: LocalDate): Boolean =
        records.any { it.date == date && it.source == XpSource.CONTRACT }

    private fun firstDay(records: List<XpRecord>): LocalDate? = records.minOfOrNull { it.date }

    /**
     * Série a žolíky. Uzavřené dny (před [today]): splněno → série +1; nesplněno → žolík, jinak série 0.
     * Dnešek sérii jen prodlouží (když je už splněný), nikdy nepřeruší.
     */
    fun streakAndJokers(records: List<XpRecord>, today: LocalDate): Pair<Int, Int> {
        val first = firstDay(records) ?: return 0 to 0
        val fulfilled = records.filter { it.source == XpSource.CONTRACT }.map { it.date }.toSet()
        val jokersEarned = records.filter { it.source == XpSource.JOKER }.groupingBy { it.date }.eachCount()
        var streak = 0
        var jokers = 0
        var day = first
        while (day.isBefore(today)) {
            jokers = (jokers + (jokersEarned[day] ?: 0)).coerceAtMost(GamificationRules.MAX_JOKERS)
            when {
                day in fulfilled -> streak++
                jokers > 0 -> jokers--
                else -> streak = 0
            }
            day = day.plusDays(1)
        }
        jokers = (jokers + (jokersEarned[today] ?: 0)).coerceAtMost(GamificationRules.MAX_JOKERS)
        if (today in fulfilled) streak++
        return streak to jokers
    }

    /**
     * Postava podle dní se splněnou smlouvou za posledních 7 dní (dnešek + 6).
     * Nikdy netrestat: kdo má méně než 7 dní historie, nespadne pod OK.
     */
    fun avatar(records: List<XpRecord>, today: LocalDate): AvatarState {
        val window = GamificationRules.AVATAR_WINDOW_DAYS.toLong()
        val from = today.minusDays(window - 1)
        val fulfilledDays = records
            .filter { it.source == XpSource.CONTRACT && !it.date.isBefore(from) && !it.date.isAfter(today) }
            .map { it.date }.toSet().size
        val computed = GamificationRules.avatarFor(fulfilledDays)
        val first = firstDay(records) ?: today
        val historyDays = java.time.temporal.ChronoUnit.DAYS.between(first, today) + 1
        return if (historyDays < window && computed < AvatarState.OK) AvatarState.OK else computed
    }

    /**
     * Souboj s algoritmem za den [date]. [feedMinutes] = minuty feedu z detekce (121);
     * null = záloha 10 min za každé odemknutí.
     */
    fun dailyScore(records: List<XpRecord>, date: LocalDate, feedMinutes: Int? = null): DailyScore {
        val day = records.filter { it.date == date }
        val closes = day.count { it.source == XpSource.CLOSED }
        val taskMinutes = day.filter { it.source == XpSource.TASK }.sumOf { it.minutes }
        val reclaimed = closes * GamificationRules.RECLAIMED_MINUTES_PER_CLOSE + taskMinutes
        val feed = feedMinutes ?: (day.count { it.source in XpSource.UNLOCKS } * GamificationRules.FEED_MINUTES_PER_UNLOCK)
        return DailyScore(reclaimedMinutes = reclaimed, feedMinutes = feed.coerceAtLeast(0))
    }

    /** Vyhrané dny od prvního dne historie po dnešek (dnešek, jakmile je vyhraný). */
    fun wonDays(records: List<XpRecord>, today: LocalDate, feedMinutesFor: (LocalDate) -> Int?): Int {
        val first = firstDay(records) ?: return 0
        var won = 0
        var day = first
        while (!day.isAfter(today)) {
            if (dailyScore(records, day, feedMinutesFor(day)).isWon) won++
            day = day.plusDays(1)
        }
        return won
    }

    /**
     * Má se za den [date] připsat boss? Ano, když už je po 22:00, boss za ten den ještě není,
     * existuje historie (den není před instalací) a v kritické hodině nebyl zásah, nebo každý skončil CLOSED.
     */
    fun bossDue(records: List<XpRecord>, date: LocalDate, nowMillis: Long, zone: ZoneId): Boolean {
        val first = firstDay(records) ?: return false
        if (date.isBefore(first)) return false
        if (records.any { it.date == date && it.source == XpSource.BOSS }) return false
        val start = date.atTime(GamificationRules.BOSS_START_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        val end = date.atTime(GamificationRules.BOSS_END_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        if (nowMillis < end) return false
        return records
            .filter { it.source in XpSource.INTERVENTIONS && it.atMillis in start until end }
            .all { it.source == XpSource.CLOSED }
    }

    /** Dřepy celkem: 20 za odemknutí dřepy + 50 za každý žolík. */
    fun totalSquats(records: List<XpRecord>): Int =
        records.count { it.source == XpSource.SQUATS } * cz.narsil.core.MovementRates.SQUATS_PER_UNLOCK +
            records.count { it.source == XpSource.JOKER } * GamificationRules.SQUATS_PER_JOKER

    /** Nejdelší řada dní bez půjčky (od prvního dne historie po dnešek). */
    fun longestNoDebtRun(records: List<XpRecord>, today: LocalDate): Int {
        val first = firstDay(records) ?: return 0
        val borrowDays = records.filter { it.source == XpSource.BORROWED }.map { it.date }.toSet()
        var best = 0
        var run = 0
        var day = first
        while (!day.isAfter(today)) {
            run = if (day in borrowDays) 0 else run + 1
            best = maxOf(best, run)
            day = day.plusDays(1)
        }
        return best
    }

    /** Odznaky, na které má historie nárok (docs/GAMIFICATION.md sekce 8). */
    fun earnedBadges(records: List<XpRecord>, today: LocalDate): Set<String> {
        val closes = records.count { it.source == XpSource.CLOSED }
        val (streak, _) = streakAndJokers(records, today)
        return buildSet {
            if (closes >= 1) add(Badges.FIRST_CLOSE)
            if (closes >= 10) add(Badges.CLOSE_10)
            if (longestNoDebtRun(records, today) >= GamificationRules.NO_DEBT_DAYS) add(Badges.NO_DEBT_WEEK)
            if (totalSquats(records) >= GamificationRules.SQUATS_BADGE_TOTAL) add(Badges.SQUATS_1000)
            if (streak >= GamificationRules.STREAK_BADGE_DAYS) add(Badges.PLAN_STREAK_3)
        }
    }

    /** Celý stav pro UI. [avatarOverride] = vnucená postava z demo panelu (222). */
    fun state(
        history: GamificationHistory,
        today: LocalDate,
        feedMinutesFor: (LocalDate) -> Int? = { null },
        avatarOverride: AvatarState? = null,
    ): GamificationState {
        val records = effective(history.records)
        val xp = records.sumOf { it.xp }.coerceAtLeast(0)
        val (streak, jokers) = streakAndJokers(records, today)
        return GamificationState(
            xp = xp,
            level = GamificationRules.level(xp),
            xpInLevel = GamificationRules.xpInLevel(xp),
            xpPerLevel = GamificationRules.XP_PER_LEVEL,
            streakDays = streak,
            jokers = jokers,
            avatar = avatarOverride ?: avatar(records, today),
            today = dailyScore(records, today, feedMinutesFor(today)),
            wonDays = wonDays(records, today, feedMinutesFor),
            badges = history.badges.entries.sortedBy { it.value }.map { it.key },
        )
    }
}
