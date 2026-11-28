package cz.narsil.ui.screens.badges

import cz.narsil.ui.text.CzechPlural
import cz.narsil.core.GamificationState
import cz.narsil.core.History
import cz.narsil.ui.components.AvatarState

/**
 * Odznaky, souhrn postavy a žebříček party ze skutečných dat (140). Čistý Kotlin.
 *
 * Odznaky = pět odznaků gamifikace (docs/GAMIFICATION.md 8) s postupem z historie. Party je v MVP
 * ukázková (bez serveru): kamarádi mají pevná čísla z `mockdata/leaderboard.json`, „Ty“ je skutečný.
 */
object BadgesMapper {
    data class Friend(val name: String, val reclaimedMinutes: Int, val steps: Int)

    /** Ukázková parta (Kovárna) – MVP bez serveru. */
    val FRIENDS = listOf(
        Friend("Bára", 610, 61_200),
        Friend("Kuba", 410, 47_300),
        Friend("Ondra", 240, 38_900),
    )
    const val PARTY_GOAL_STEPS = 200_000

    fun map(game: GamificationState, history: History, avatar: AvatarState, stepsToday: Int): BadgesUiState {
        val days = history.days
        val closes = days.sumOf { it.closes }
        val squats = days.sumOf { it.squats }
        val noDebtRun = longestNoDebtRun(history)
        val last7 = days.takeLast(7)
        fun badge(id: String, title: String, description: String, progress: Int, target: Int) = BadgeItem(
            id = id,
            title = title,
            description = description,
            progress = progress.coerceIn(0, target),
            target = target,
            isUnlocked = id in game.badges || progress >= target,
        )
        val badges = listOf(
            badge("FIRST_CLOSE", "První odchod", "Poprvé zavřít feed při Ozvěně bez placení.", closes, 1),
            badge("CLOSE_10", "Odešel jsem sám 10×", "Zavřít feed při Ozvěně bez placení pohybem.", closes, 10),
            badge("NO_DEBT_WEEK", "Týden bez dluhu", "Sedm dní po sobě bez půjčených minut.", noDebtRun, 7),
            badge("SQUATS_1000", "1 000 dřepů", "Celkem tisíc dřepů spočítaných kamerou.", squats, 1_000),
            badge("PLAN_STREAK_3", "Tři smlouvy v řadě", "Tři dny po sobě splněná večerní smlouva.", game.streakDays, 3),
        ).sortedByDescending { it.isUnlocked }
        val myWeek = last7.sumOf { it.reclaimedMinutes }
        val members = (FRIENDS.map { it.name to it.reclaimedMinutes } + ("Ty" to myWeek))
            .sortedByDescending { it.second }
            .mapIndexed { i, (name, minutes) -> PartyMemberEntry(i + 1, name, minutes, isCurrentUser = name == "Ty") }
        return BadgesUiState(
            currentAvatarState = avatar,
            avatarSummary7Days = "Posledních 7 dní: ${CzechPlural.wins(last7.count { it.battleWon && it.hasData })} · " +
                "${CzechPlural.squats(last7.sumOf { it.squats })} · série ${CzechPlural.days(game.streakDays)}",
            badges = badges,
            partyGoalSteps = PARTY_GOAL_STEPS,
            partyCurrentSteps = FRIENDS.sumOf { it.steps } + stepsToday.coerceAtLeast(0),
            leaderboard = members,
        )
    }

    /** Nejdelší řada dní s daty bez půjčky. */
    fun longestNoDebtRun(history: History): Int {
        var best = 0
        var run = 0
        history.days.filter { it.hasData }.forEach { day ->
            run = if (day.borrows > 0) 0 else run + 1
            best = maxOf(best, run)
        }
        return best
    }
}
