package cz.narsil.app.demo

import cz.narsil.app.EchoOverlayStarter
import cz.narsil.app.ServiceLocator
import cz.narsil.core.AvatarState
import cz.narsil.core.ComfortContext
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.core.fake.FakeGamificationEngine
import cz.narsil.core.fake.FakeMovementRepository
import cz.narsil.detection.accessibility.AccessibilityTriggerSource

/**
 * Akce demo panelu (123, SPEC 10) nad [ServiceLocator]. Ladicí funkce pohybu a gamifikace volá
 * přes fake (222 je dodá i skutečným modulům); když je modul skutečný a funkci nemá, vrátí false.
 */
object DemoActions {
    const val DEMO_THRESHOLD_SEC = 20

    private val sl get() = ServiceLocator

    suspend fun setContext(context: ComfortContext?) = sl.contextRepository.setManualOverride(context)

    suspend fun setShortThreshold(on: Boolean) =
        sl.settingsRepository.update { it.copy(demoMode = on, demoThresholdSec = if (on) DEMO_THRESHOLD_SEC else null) }

    suspend fun setContractToday(on: Boolean) = sl.settingsRepository.update { it.copy(demoContractToday = on) }

    suspend fun setAlwaysYes(on: Boolean) = sl.settingsRepository.update { it.copy(demoAlwaysYes = on) }

    fun addSteps(steps: Int = 1_000): Boolean =
        (sl.movementRepository as? FakeMovementRepository)?.let { it.addSteps(steps); true } ?: false

    fun addXp(xp: Int = 500): Boolean =
        (sl.gamificationEngine as? FakeGamificationEngine)?.let { it.addXp(xp); true } ?: false

    fun setAvatar(avatar: AvatarState): Boolean =
        (sl.gamificationEngine as? FakeGamificationEngine)?.let { it.setAvatar(avatar); true } ?: false

    suspend fun showEchoNow(): Boolean = EchoOverlayStarter.showNow()

    /**
     * Reset dne: ruční přepínač pryč, odemknutí zrušeno, relace v detekci od nuly,
     * fake pohyb/gamifikace/smlouva na výchozí demo hodnoty.
     */
    suspend fun resetDay() {
        sl.contextRepository.setManualOverride(null)
        sl.unlockTimer.clear()
        (sl.interventionTriggerSource as? AccessibilityTriggerSource)?.onOutcome(InterventionOutcome.CLOSED)
        (sl.movementRepository as? FakeMovementRepository)?.apply { setSteps(4_200); setDebt(0) }
        (sl.gamificationEngine as? FakeGamificationEngine)?.setState(FakeGamificationEngine.DEMO_STATE)
        (sl.contractRepository as? FakeContractRepository)?.save(FakeContractRepository.demoContract(java.time.LocalDate.now()))
    }

    /** Připraví celé demo jedním klepnutím: doma, práh 20 s, smlouva platí dnes, čistý den. */
    suspend fun prepareDemo() {
        resetDay()
        setContext(ComfortContext.HOME)
        setShortThreshold(true)
        setContractToday(true)
    }
}
