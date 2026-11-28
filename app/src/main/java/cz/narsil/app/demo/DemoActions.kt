package cz.narsil.app.demo

import cz.narsil.app.EchoOverlayStarter
import cz.narsil.app.ServiceLocator
import cz.narsil.core.AvatarState
import cz.narsil.core.ComfortContext
import cz.narsil.core.Exercise
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.LlmProvider
import cz.narsil.core.data.DayKey
import cz.narsil.llm.client.BuildConfigLlmKeys
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.gamification.RulesGamificationEngine
import cz.narsil.gamification.debug.GamificationDebug
import cz.narsil.movement.debug.MovementDebug
import cz.narsil.detection.accessibility.AccessibilityTriggerSource

/** Akce demo panelu (123, SPEC 10) nad [ServiceLocator]. */
object DemoActions {
    const val DEMO_THRESHOLD_SEC = 20

    private val sl get() = ServiceLocator

    suspend fun setContext(context: ComfortContext?) = sl.contextRepository.setManualOverride(context)

    suspend fun setShortThreshold(on: Boolean) =
        sl.settingsRepository.update { it.copy(demoMode = on, demoThresholdSec = if (on) DEMO_THRESHOLD_SEC else null) }

    suspend fun setContractToday(on: Boolean) = sl.settingsRepository.update { it.copy(demoContractToday = on) }

    suspend fun setExercise(exercise: Exercise) = sl.settingsRepository.update { it.copy(exercise = exercise) }

    suspend fun setLlmProvider(provider: LlmProvider) = sl.settingsRepository.update { it.copy(llmProvider = provider) }

    /** Je pro [provider] v BuildConfig klíč? Jen ano/ne, hodnota se nikdy nezobrazuje. */
    fun hasKey(provider: LlmProvider): Boolean = !BuildConfigLlmKeys.keyFor(provider).isNullOrBlank()

    suspend fun setAlwaysYes(on: Boolean) = sl.settingsRepository.update { it.copy(demoAlwaysYes = on) }

    // Ladicí funkce Šimona (222): fungují s fake i se skutečnými moduly.
    suspend fun addSteps(steps: Int = 1_000): Boolean = MovementDebug.addSteps(sl.movementRepository, steps)

    suspend fun addXp(xp: Int = 500): Boolean = GamificationDebug.addXp(sl.gamificationEngine, xp)

    suspend fun setAvatar(avatar: AvatarState): Boolean = GamificationDebug.setAvatar(sl.gamificationEngine, avatar)

    /** Nahraje mockdata/ do databáze (SPEC 10) a přepočítá gamifikaci. Vrací popis výsledku. */
    suspend fun loadMockData(): String {
        val r = runCatching { MockDataLoader.load(sl.appContext) }.getOrElse { return "Mock data nejdou načíst: ${it.message}" }
        // Historie se vyměnila celá → gamifikace i pohyb (ledger) znovu z databáze, Domov se překreslí bez restartu.
        (sl.gamificationEngine as? RulesGamificationEngine)?.let { runCatching { it.reload() } }
        runCatching { sl.movementRepository.refresh() }
        return "Nahráno: ${r.days} dní, ${r.interventions} zásahů, ${r.ratings} hodnocení"
    }

    suspend fun showEchoNow(): Boolean = EchoOverlayStarter.showNow()

    /**
     * Reset dne: ruční přepínač pryč, odemknutí zrušeno, relace v detekci od nuly,
     * pohyb a gamifikace přes ladicí funkce (222), fake smlouva na demo.
     */
    suspend fun resetDay() {
        sl.contextRepository.setManualOverride(null)
        sl.unlockTimer.clear()
        (sl.interventionTriggerSource as? AccessibilityTriggerSource)?.onOutcome(InterventionOutcome.CLOSED)
        MovementDebug.resetDay(sl.movementRepository)
        GamificationDebug.resetDay(sl.gamificationEngine)
        (sl.contractRepository as? FakeContractRepository)?.save(FakeContractRepository.demoContract(DayKey.today()))
    }

    /**
     * Dnešní smlouva pro demo: když žádná není nebo nemá úkoly, uloží demo smlouvu (6 000 kroků,
     * plán, 4 úkoly – běh ověřený, ostatní k ověření). Platí i pro skutečnou smlouvu v Room.
     */
    suspend fun ensureDemoContract() {
        val today = DayKey.today()
        val current = runCatching { sl.contractRepository.forDate(today) }.getOrNull()
        if (current != null && current.tasks.isNotEmpty()) return
        val demo = FakeContractRepository.demoContract(today)
        runCatching { sl.contractRepository.save(demo.copy(videoUri = current?.videoUri ?: demo.videoUri)) }
    }

    /** Připraví celé demo jedním klepnutím: doma, práh 20 s, smlouva platí dnes s úkoly, čistý den. */
    suspend fun prepareDemo() {
        resetDay()
        // XP těsně pod úrovní: 20 dřepů v demu spustí novou úroveň (230).
        GamificationDebug.prepareDemo(sl.gamificationEngine)
        ensureDemoContract()
        setContext(ComfortContext.HOME)
        setShortThreshold(true)
        setContractToday(true)
    }
}
