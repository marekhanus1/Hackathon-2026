package cz.narsil.app

import android.content.Context
import cz.narsil.contract.ContractModule
import cz.narsil.core.RewardService
import cz.narsil.core.fake.FakeRewardService
import cz.narsil.gamification.GamificationModule
import cz.narsil.gamification.reward.TaskRewardService
import cz.narsil.movement.MovementModule
import cz.narsil.core.ContentMatcher
import cz.narsil.core.StravaRepository
import cz.narsil.core.HistoryRepository
import cz.narsil.core.data.NarsilDatabase
import cz.narsil.core.data.RoomHistoryRepository
import cz.narsil.core.fake.FakeHistoryRepository
import cz.narsil.core.TaskVerifier
import cz.narsil.core.fake.FakeContentMatcher
import cz.narsil.core.fake.FakeStravaRepository
import cz.narsil.core.fake.FakeTaskVerifier
import cz.narsil.integrations.strava.HttpStravaRepository
import cz.narsil.llm.client.MultiProviderLlmClient
import cz.narsil.llm.matcher.LlmContentMatcher
import cz.narsil.verify.AndroidNetworkStatus
import cz.narsil.verify.TaskVerificationService
import cz.narsil.verify.VerificationModule
import cz.narsil.core.ContextRepository
import cz.narsil.core.ContractRepository
import cz.narsil.core.EchoVideoRepository
import cz.narsil.core.GamificationEngine
import cz.narsil.core.InterventionTriggerSource
import cz.narsil.core.MovementRepository
import cz.narsil.core.SettingsRepository
import cz.narsil.core.SquatCounter
import cz.narsil.core.Exercise
import cz.narsil.core.MovementRates
import cz.narsil.movement.pushup.CameraPushupCounter
import cz.narsil.core.data.DataStoreSettingsRepository
import cz.narsil.core.fake.FakeContextRepository
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.core.fake.FakeEchoVideoRepository
import cz.narsil.core.fake.FakeGamificationEngine
import cz.narsil.core.fake.FakeInterventionTriggerSource
import cz.narsil.core.fake.FakeMovementRepository
import cz.narsil.core.fake.FakeSquatCounter
import cz.narsil.detection.accessibility.AccessibilityTriggerSource
import cz.narsil.detection.context.WifiContextRepository
import cz.narsil.movement.squat.PoseSquatCounter
import cz.narsil.overlay.UnlockTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Jednoduchý ServiceLocator: jediné místo, kde se rozhoduje, zda aplikace používá
 * fake implementace (cz.narsil.core.fake) nebo skutečné moduly.
 *
 * Inicializuje ho [NarsilApp.onCreate]. Obrazovky si závislosti berou odsud
 * (např. `ServiceLocator.contextRepository`), nikdy nevytvářejí implementace samy.
 *
 * Přepínač [useFakes]:
 * - výchozí hodnota je [DEFAULT_USE_FAKES] (true, dokud nejsou skutečné moduly hotové),
 * - za běhu ho přepíná demo panel (úkol 123) přes [setUseFakes]; tím se zahodí
 *   už vytvořené instance a při dalším přístupu se vytvoří znovu.
 *
 * Vlastnosti doplní úkol 124 po sloučení rozhraní z úkolu 102 (cz.narsil.core):
 * - `contextRepository`    – ContextRepository (HOME/AWAY, Wi-Fi)
 * - `movementRepository`   – MovementRepository (kroky, Health Connect)
 * - `squatCounter`         – SquatCounter (ML Kit Pose, Šimon)
 * - `gamificationEngine`   – GamificationEngine (XP, úrovně, odznaky)
 * - `contractRepository`   – ContractRepository (Večerní smlouva)
 * - `contentMatcher`       – ContentMatcher (LLM, plánovaný obsah)
 * - `echoVideoRepository`  – EchoVideoRepository (videa pro Ozvěnu)
 *
 * Vzor pro každou vlastnost:
 * ```
 * val contextRepository: ContextRepository
 *     get() = cached("contextRepository") {
 *         if (useFakes) FakeContextRepository() else RealContextRepository(appContext)
 *     }
 * ```
 */
object ServiceLocator {

    /**
     * true = všechno fake (náhledy, demo bez telefonu). false = skutečné moduly, kde už existují
     * (detekce, kontext, nastavení, pohyb, dřepy, gamifikace, smlouva, odměny).
     */
    const val DEFAULT_USE_FAKES = false

    private lateinit var appContextRef: Context

    /** Aplikační kontext; dostupný až po [init]. */
    val appContext: Context
        get() = appContextRef

    @Volatile
    var useFakes: Boolean = DEFAULT_USE_FAKES
        private set

    private val instances = mutableMapOf<String, Any>()

    val isInitialized: Boolean
        get() = ::appContextRef.isInitialized

    /** Scope aplikace (běží po celou dobu procesu). */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun init(context: Context, useFakes: Boolean = DEFAULT_USE_FAKES) {
        appContextRef = context.applicationContext
        this.useFakes = useFakes
    }

    /** Přepne fake/skutečné moduly za běhu a zahodí vytvořené instance. */
    @Synchronized
    fun setUseFakes(value: Boolean) {
        if (useFakes == value) return
        useFakes = value
        instances.clear()
    }

    /** Líně vytvoří a uloží instanci pod klíčem [key]; po [setUseFakes] se vytvoří znovu. */
    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T : Any> cached(key: String, create: () -> T): T =
        instances.getOrPut(key, create) as T

    // --- Moduly ---

    /** Nastavení: vždy DataStore (i demo přepínače musí přežít restart). */
    val settingsRepository: SettingsRepository
        get() = cached("settings") { DataStoreSettingsRepository(appContext) }

    val contextRepository: ContextRepository
        get() = cached("context") {
            if (useFakes) FakeContextRepository() else WifiContextRepository(appContext, settingsRepository, appScope)
        }

    val interventionTriggerSource: InterventionTriggerSource
        get() = cached("triggers") {
            if (useFakes) FakeInterventionTriggerSource()
            else AccessibilityTriggerSource(
                contextRepository, settingsRepository, appScope,
                // Řetězení videí (137): video z dnešního plánu se nepočítá.
                planCheck = { title ->
                    val plan = contractRepository.today.value?.plannedContent.orEmpty()
                    plan.isNotEmpty() && contentMatcher.isPlanned(title, plan).planned
                },
            )
        }

    // Pohyb, gamifikace a smlouva (Šimon, 2xx). Při useFakes ukázková data.

    val movementRepository: MovementRepository
        get() = cached("movement") {
            if (useFakes) FakeMovementRepository()
            else MovementModule.create(appContext, settings = settingsRepository).also { it.startAutoRefresh(appScope) }
        }

    val contractRepository: ContractRepository
        get() = cached("contract") {
            if (useFakes) FakeContractRepository() else ContractModule.contractRepository(appContext, appScope)
        }

    val gamificationEngine: GamificationEngine
        get() = cached("gamification") {
            if (useFakes) {
                FakeGamificationEngine()
            } else {
                GamificationModule.create(appContext, appScope).also {
                    // +100 XP a série za splněnou smlouvu (220).
                    GamificationModule.watchContract(it, contractRepository, movementRepository, appScope)
                }
            }
        }

    val echoVideoRepository: EchoVideoRepository
        get() = cached("echoVideo") {
            if (useFakes) FakeEchoVideoRepository() else ContractModule.echoVideoRepository(appContext)
        }

    /** Odměna za ověřený úkol (221); volá ověření (520). */
    val rewardService: RewardService
        get() = cached("reward") {
            if (useFakes) FakeRewardService(movementRepository, gamificationEngine)
            else TaskRewardService.create(appContext, movementRepository, gamificationEngine)
        }

    // Ověření úkolů, LLM a Strava (5xx). Klíče z BuildConfig; chybějící = poskytovatel vypnutý.

    /** Je video v plánu? (512) LLM podle `Settings.llmProvider`, bez sítě záloha přes klíčová slova. */
    val contentMatcher: ContentMatcher
        get() = cached("contentMatcher") {
            if (useFakes) FakeContentMatcher()
            else LlmContentMatcher(
                MultiProviderLlmClient({ settingsRepository.settings.value.llmProvider }),
                isOnline = AndroidNetworkStatus(appContext)::isOnline,
            )
        }

    /** Historie pro Zrcadlo a odznaky (140): jen čte sdílenou databázi. */
    val historyRepository: HistoryRepository
        get() = cached("history") {
            if (useFakes) FakeHistoryRepository() else RoomHistoryRepository(NarsilDatabase.get(appContext))
        }

    val stravaRepository: StravaRepository
        get() = cached("strava") { if (useFakes) FakeStravaRepository() else HttpStravaRepository.create(appContext) }

    /** Ověřovač úkolů (501/522) včetně demo přepínače „vždy ANO“ (DemoAwareTaskVerifier). */
    val taskVerifier: TaskVerifier
        get() = cached("taskVerifier") {
            if (useFakes) FakeTaskVerifier(stravaRepository)
            else VerificationModule.verifier(appContext, settingsRepository, stravaRepository)
        }

    /** Ověření + stav pro UI + odměna (520/521). Při návratu sítě zkusí VERIFY_LATER znovu. */
    val verification: TaskVerificationService
        get() = cached("verification") {
            VerificationModule.service(contractRepository, taskVerifier, rewardService).also {
                if (!useFakes) VerificationModule.networkTrigger(appContext, it, appScope).start()
            }
        }

    val squatCounter: SquatCounter
        get() = cached("squats") {
            if (useFakes) FakeSquatCounter(appScope, intervalMillis = 1_000) else PoseSquatCounter(appContext)
        }

    /** Kliky na podlaze (232): přední kamera, přiblížení a polohové senzory. */
    val pushupCounter: SquatCounter
        get() = cached("pushups") {
            if (useFakes) FakeSquatCounter(appScope, intervalMillis = 1_000, target = MovementRates.PUSHUPS_PER_UNLOCK)
            else CameraPushupCounter(appContext)
        }

    /** Počítadlo zvoleného cviku (232). */
    fun exerciseCounter(exercise: Exercise): SquatCounter =
        if (exercise == Exercise.PUSHUPS) pushupCounter else squatCounter

    val unlockTimer: UnlockTimer
        get() = cached("unlockTimer") { UnlockTimer() }
}
