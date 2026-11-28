package cz.narsil.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Nastavení aplikace (DataStore, ne Room).
 *
 * @param homeSsid Wi-Fi uložená jako domov (112)
 * @param demoThresholdSec práh zásahu v demo režimu (20 s), null = výchozí 5 min
 * @param demoContractToday smlouva platí hned (pro dnešek)
 * @param demoAlwaysYes ověření úkolů vždy ANO (jen demo panel)
 *
 * Pole od [toleranceMinutes] dál jsou z obrazovky Nastavení (133), výchozí hodnoty = SPEC.
 * Čtenáři: detekce [toleranceMillis], [videoChainLimit], [isTracked], [isProtectionActive];
 * Ozvěna [repsPerUnlock]; pohyb [debtLimitMinutes], [partnerAlerts].
 */
data class Settings(
    val homeSsid: String? = null,
    val llmProvider: LlmProvider = LlmProvider.GEMINI,
    val demoMode: Boolean = false,
    val demoThresholdSec: Int? = null,
    val demoContractToday: Boolean = false,
    val demoAlwaysYes: Boolean = false,
    /** Cvik v Ozvěně a na obrazovce pohybu (232): dřepy, nebo kliky na podlaze. */
    val exercise: Exercise = Exercise.SQUATS,
    /** Tenký ukazatel blížícího se limitu při scrollování (135). */
    val showScrollProgress: Boolean = true,
    // --- 133: parametry chování v obrazovce Nastavení. Výchozí = SPEC, uvolnění chrání potvrzení. ---
    /** Tolerance: Ozvěna po tolika minutách nekonečného feedu doma (SPEC 3). Demo práh má přednost. */
    val toleranceMinutes: Int = SettingsDefaults.TOLERANCE_MINUTES,
    /** Řetězení: Ozvěna u tolikátého neplánovaného videa po sobě (SPEC 3). */
    val videoChainLimit: Int = SettingsDefaults.VIDEO_CHAIN_LIMIT,
    /** Dřepů na odemčení [MovementRates.MINUTES_PER_UNLOCK] minut. */
    val squatsPerUnlock: Int = MovementRates.SQUATS_PER_UNLOCK,
    /** Kliků na odemčení [MovementRates.MINUTES_PER_UNLOCK] minut. */
    val pushupsPerUnlock: Int = MovementRates.PUSHUPS_PER_UNLOCK,
    /** Limit dluhu v minutách (SPEC 5). */
    val debtLimitMinutes: Int = MovementRates.DEFAULT_DEBT_LIMIT_MINUTES,
    /** Sledované aplikace; co tu není, Narsil nesleduje. */
    val trackedApps: Set<TrackedApp> = TrackedApp.entries.toSet(),
    /** Ochrana pozastavená do (epoch ms, nejdéle do 4:00); null = běží. */
    val protectionPausedUntilMillis: Long? = null,
    /** Zpráva Parťákovi při dluhu nad limit a při pozastavení ochrany. */
    val partnerAlerts: Boolean = true,
    /** „Další aplikace s nekonečným feedem“ (149): neznámé aplikace podle chování scrollu. Výchozí vypnuto. */
    val trackOtherFeedApps: Boolean = false,
) {
    /** Práh zásahu u nekonečného feedu: demo panel (20 s), jinak [toleranceMinutes]. */
    fun toleranceMillis(): Long = demoThresholdSec?.let { it * 1_000L } ?: toleranceMinutes * 60_000L

    /** Zasahuje Narsil teď? false = uživatel ochranu pozastavil (do [protectionPausedUntilMillis]). */
    fun isProtectionActive(nowMillis: Long): Boolean = protectionPausedUntilMillis?.let { nowMillis >= it } ?: true

    /**
     * Sleduje se aplikace? [appName] = název `WatchedApp` z detekce (INSTAGRAM, …, X, REDDIT, …);
     * „OTHER“ = ostatní aplikace podle [trackOtherFeedApps] (149).
     */
    fun isTracked(appName: String): Boolean =
        if (appName == OTHER_APPS) trackOtherFeedApps else trackedApps.any { it.name == appName }

    /** Opakování na odemčení pro [exercise]. */
    fun repsPerUnlock(exercise: Exercise = this.exercise): Int = when (exercise) {
        Exercise.SQUATS -> squatsPerUnlock
        Exercise.PUSHUPS -> pushupsPerUnlock
    }
}

/** Název `WatchedApp.OTHER` pro [Settings.isTracked] (149). */
const val OTHER_APPS = "OTHER"

/**
 * Sledovatelné aplikace (133). Názvy odpovídají `cz.narsil.detection.accessibility.WatchedApp`.
 * Od X dál (149) obecná detekce feedu podle chování; v Nastavení jen ty nainstalované.
 */
enum class TrackedApp { INSTAGRAM, TIKTOK, YOUTUBE, FIREFOX, X, REDDIT, FACEBOOK, THREADS, PINTEREST, NINEGAG, TUMBLR, LINKEDIN, BLUESKY }

/** Výchozí hodnoty a povolené volby nastavení (133). Výchozí hodnota je vždy ve volbách. */
object SettingsDefaults {
    const val TOLERANCE_MINUTES = 5
    const val VIDEO_CHAIN_LIMIT = 3
    val TOLERANCE_OPTIONS = listOf(3, 5, 10, 15)
    val VIDEO_CHAIN_OPTIONS = listOf(2, 3, 4, 5)
    val SQUATS_OPTIONS = listOf(10, 15, 20, 30, 40)
    val PUSHUPS_OPTIONS = listOf(5, 10, 15, 20)
    val DEBT_LIMIT_OPTIONS = listOf(0, 15, 30, 45, 60)
}

/** Cvik, kterým se odemyká čas (232). Počet na odemčení viz [MovementRates]. */
enum class Exercise { SQUATS, PUSHUPS }

/** Nastavení (105). Implementace `cz.narsil.core.data.DataStoreSettingsRepository`. */
interface SettingsRepository {
    val settings: StateFlow<Settings>

    /** Atomická změna, např. `update { it.copy(demoMode = true) }`. */
    suspend fun update(change: (Settings) -> Settings)
}
