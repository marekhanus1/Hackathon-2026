package cz.narsil.app

import android.content.Context
import cz.narsil.contract.ContractModule
import cz.narsil.core.RewardService
import cz.narsil.core.fake.FakeRewardService
import cz.narsil.gamification.GamificationModule
import cz.narsil.gamification.reward.TaskRewardService
import cz.narsil.movement.MovementModule
import cz.narsil.core.ContextRepository
import cz.narsil.core.ContractRepository
import cz.narsil.core.EchoVideoRepository
import cz.narsil.core.GamificationEngine
import cz.narsil.core.InterventionTriggerSource
import cz.narsil.core.MovementRepository
import cz.narsil.core.SettingsRepository
import cz.narsil.core.SquatCounter
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
            else AccessibilityTriggerSource(contextRepository, settingsRepository, appScope)
        }

    // Pohyb, gamifikace a smlouva (Šimon, 2xx). Při useFakes ukázková data.

    val movementRepository: MovementRepository
        get() = cached("movement") {
            if (useFakes) FakeMovementRepository()
            else MovementModule.create(appContext).also { it.startAutoRefresh(appScope) }
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

    val squatCounter: SquatCounter
        get() = cached("squats") {
            if (useFakes) FakeSquatCounter(appScope, intervalMillis = 1_000) else PoseSquatCounter(appContext)
        }

    val unlockTimer: UnlockTimer
        get() = cached("unlockTimer") { UnlockTimer() }
}
