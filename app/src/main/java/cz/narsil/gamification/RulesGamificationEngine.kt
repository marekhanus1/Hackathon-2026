package cz.narsil.gamification

import cz.narsil.core.AvatarState
import cz.narsil.core.GamificationEngine
import cz.narsil.core.GamificationState
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.PlanTask
import cz.narsil.core.data.DayKey
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Skutečný [GamificationEngine] (202) podle docs/GAMIFICATION.md. Čistý Kotlin:
 * stav se přepočítá z historie v [store] ([GamificationCalculator]), takže přežije restart,
 * když je [store] trvalé (Room, 220). Nic nehází – chyba úložiště jen nezapíše událost.
 *
 * @param feedMinutesFor minuty feedu za den z detekce (121); null = záloha z odemknutí
 */
class RulesGamificationEngine(
    private val store: GamificationStore = InMemoryGamificationStore(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val feedMinutesFor: suspend (LocalDate) -> Int? = { null },
) : GamificationEngine {

    private val mutex = Mutex()
    private var history: GamificationHistory? = null
    private var avatarOverride: AvatarState? = null
    private var extraSquatsToday = 0
    private var extraSquatsDay: LocalDate? = null

    private val _state = MutableStateFlow(GamificationState())
    override val state: StateFlow<GamificationState> = _state.asStateFlow()

    /** Načte historii, vyhodnotí bosse a přepočítá stav (při startu a po návratu do aplikace). */
    suspend fun refresh() = mutex.withLock {
        settle()
        publish()
    }

    /** Znovu načte historii z úložiště (po nahrání mock dat, 147) – zahodí mezipaměť i ladicí stav. */
    suspend fun reload() = mutex.withLock {
        history = null
        avatarOverride = null
        extraSquatsToday = 0
        extraSquatsDay = null
        settle()
        publish()
    }

    override suspend fun onOutcome(outcome: InterventionOutcome): Int = mutex.withLock {
        val h = history()
        val now = clock.millis()
        val today = today()
        val xp = when (outcome) {
            InterventionOutcome.CLOSED -> GamificationCalculator.closeXp(GamificationCalculator.effective(h.records), today, now)
            InterventionOutcome.SQUATS -> GamificationRules.XP_SQUATS
            InterventionOutcome.PAID_STEPS -> GamificationRules.XP_PAID_STEPS
            InterventionOutcome.BORROWED -> GamificationRules.XP_BORROWED
            InterventionOutcome.PLANNED_FREE -> 0
        }
        append(XpRecord(today, now, outcome.name, xp))
        settle()
        publish()
        xp
    }

    override suspend fun onContractFulfilled(): Int = mutex.withLock {
        val today = today()
        if (GamificationCalculator.hasContractXp(effectiveRecords(), today)) return@withLock 0
        append(XpRecord(today, clock.millis(), XpSource.CONTRACT, GamificationRules.XP_CONTRACT))
        settle()
        publish()
        GamificationRules.XP_CONTRACT
    }

    /** +30 XP a odměna úkolu do získaných minut. Jen jednou za úkol hlídá [cz.narsil.core.RewardService]. */
    override suspend fun onTaskVerified(task: PlanTask): Int = mutex.withLock {
        append(XpRecord(today(), clock.millis(), XpSource.TASK, GamificationRules.XP_TASK, minutes = task.rewardMinutes))
        settle()
        publish()
        GamificationRules.XP_TASK
    }

    override suspend fun onVideoRated(rating: Int): Int = mutex.withLock {
        val today = today()
        val xp = GamificationCalculator.videoRatedXp(effectiveRecords(), today)
        append(XpRecord(today, clock.millis(), XpSource.VIDEO_RATED, xp))
        publish()
        xp
    }

    /**
     * Dřepy navíc nad rámec odemknutí (obrazovka dřepů je počítá dál po 20). Každých 50 = žolík (max 3).
     * Vrací počet nově získaných žolíků. Rozpracované dřepy se drží jen v paměti do konce dne.
     */
    suspend fun onExtraSquats(count: Int): Int = mutex.withLock {
        if (count <= 0) return@withLock 0
        val today = today()
        if (extraSquatsDay != today) {
            extraSquatsDay = today
            extraSquatsToday = 0
        }
        extraSquatsToday += count
        var earned = 0
        while (extraSquatsToday >= GamificationRules.SQUATS_PER_JOKER) {
            extraSquatsToday -= GamificationRules.SQUATS_PER_JOKER
            append(XpRecord(today, clock.millis(), XpSource.JOKER, 0))
            earned++
        }
        if (earned > 0) {
            settle()
            publish()
        }
        earned
    }

    /** Ladění (222): přidá XP jako událost DEMO. */
    suspend fun addXp(xp: Int = 500) = mutex.withLock {
        if (xp <= 0) return@withLock
        append(XpRecord(today(), clock.millis(), XpSource.DEMO, xp))
        publish()
    }

    /** Ladění (222): vnutí stav postavy; null = zase podle pravidel. */
    suspend fun setAvatar(avatar: AvatarState?) = mutex.withLock {
        avatarOverride = avatar
        publish()
    }

    /**
     * Ladění (222): reset dne – dnešní XP, skóre souboje, žolíky a smlouva dneška se přestanou počítat
     * (zapíše se značka DAY_RESET, přežije restart). Zruší i vnucenou postavu. Odznaky zůstanou.
     */
    suspend fun resetToday() = mutex.withLock {
        append(XpRecord(today(), clock.millis(), XpSource.DAY_RESET, 0))
        avatarOverride = null
        extraSquatsToday = 0
        publish()
    }

    // --- vnitřek (volat pod mutexem) ---

    private suspend fun effectiveRecords(): List<XpRecord> = GamificationCalculator.effective(history().records)

    private fun today(): LocalDate = DayKey.of(clock.millis(), clock.zone)

    private suspend fun history(): GamificationHistory =
        history ?: (runCatching { store.load() }.getOrDefault(GamificationHistory())).also { history = it }

    private suspend fun append(record: XpRecord) {
        val h = history()
        history = h.copy(records = h.records + record)
        runCatching { store.append(record) }
    }

    /** Boss za včerejšek i dnešek a nové odznaky. */
    private suspend fun settle() {
        val now = clock.millis()
        val today = today()
        for (day in listOf(today.minusDays(1), today)) {
            if (GamificationCalculator.bossDue(effectiveRecords(), day, now, clock.zone)) {
                append(XpRecord(day, now, XpSource.BOSS, GamificationRules.XP_BOSS))
            }
        }
        val h = history()
        val newBadges = GamificationCalculator.earnedBadges(GamificationCalculator.effective(h.records), today) - h.badges.keys
        if (newBadges.isNotEmpty()) {
            history = h.copy(badges = h.badges + newBadges.associateWith { now })
            newBadges.forEach { runCatching { store.unlockBadge(it, now) } }
        }
    }

    private suspend fun publish() {
        val today = today()
        val h = history()
        val feed = mutableMapOf<LocalDate, Int?>()
        val days = h.records.map { it.date }.toSet() + today
        for (day in days) feed[day] = runCatching { feedMinutesFor(day) }.getOrNull()
        _state.value = GamificationCalculator.state(h, today, { feed[it] }, avatarOverride)
    }
}
