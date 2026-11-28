package cz.narsil.movement

import cz.narsil.core.MovementRepository
import cz.narsil.core.MovementState
import cz.narsil.core.data.DayKey
import cz.narsil.movement.currency.CurrencyBook
import cz.narsil.movement.currency.Currency
import cz.narsil.movement.steps.StepsSource
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Skutečný [MovementRepository] (211): kroky z [steps] (Health Connect, záloha senzor),
 * měna a dluh z [book] (203, Room ledger). Nic nehází; bez kroků platí poslední známá hodnota.
 *
 * @param onPartnerAlert prosba nad denní limit → zpráva Parťákovi (MVP ukázková notifikace)
 * @param onFriendGift Parťák dal minuty (163) → ukázková notifikace s jeho odpovědí
 */
class StepsMovementRepository(
    private val steps: StepsSource,
    private val book: CurrencyBook,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val onFriendGift: (minutes: Int) -> Unit = {},
    private val onPartnerAlert: (debtMinutes: Int) -> Unit = {},
) : MovementRepository {

    private val _state = MutableStateFlow(MovementState(debtLimitMinutes = book.debtLimitMinutes))
    override val state: StateFlow<MovementState> = _state.asStateFlow()

    private var lastSteps = 0
    private var demoSteps = 0
    private var demoDay: LocalDate? = null
    private var autoRefresh: Job? = null

    /** Dřepy celkem od spuštění (statistiky). */
    var squatsTotal = 0
        private set

    override suspend fun refresh() {
        val read = runCatching { steps.stepsToday() }.getOrNull()
        if (read != null) lastSteps = read
        publish()
    }

    override suspend fun pay(minutes: Int): Boolean {
        val ok = book.pay(stepsNow(), minutes)
        publish()
        return ok
    }

    /** Minuty od Parťáka do denního limitu (SPEC 5, 163). Nad limit se nic nepřidá a Parťák dostane zprávu. */
    override suspend fun borrow(minutes: Int) {
        val result = book.borrow(minutes)
        if (result.partnerAlert) runCatching { onPartnerAlert(result.debtMinutes + minutes) }
        if (result.accepted) runCatching { onFriendGift(minutes) }
        publish()
    }

    override suspend fun recordSquats(count: Int): Int {
        if (count > 0) squatsTotal += count
        return Currency.squatMinutes(count)
    }

    override suspend fun addEarnedMinutes(minutes: Int) {
        book.addEarnedMinutes(minutes)
        publish()
    }

    /** Ladění (222): přidá kroky jen pro dnešek (nepíše do Health Connect). */
    suspend fun addSteps(steps: Int = 1_000) {
        rollDemoDay()
        demoSteps += steps
        publish()
    }

    /**
     * Ladění (222): reset dne. Minuty dneška od nuly, demo kroky pryč; když je zadáno [steps],
     * kroky dneška se nastaví na tuto hodnotu (posun proti skutečným krokům telefonu).
     */
    suspend fun resetDay(steps: Int? = null) {
        book.resetToday()
        rollDemoDay()
        demoSteps = if (steps == null) 0 else steps - lastSteps
        publish()
    }

    /** Pravidelné načítání kroků (výchozí 1 min). Opakované volání předchozí smyčku nahradí. */
    fun startAutoRefresh(scope: CoroutineScope, intervalMillis: Long = 60_000) {
        autoRefresh?.cancel()
        autoRefresh = scope.launch {
            while (isActive) {
                refresh()
                delay(intervalMillis)
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefresh?.cancel()
        autoRefresh = null
    }

    private fun rollDemoDay() {
        val today = DayKey.of(clock.millis(), clock.zone)
        if (demoDay != today) {
            demoDay = today
            demoSteps = 0
        }
    }

    private fun stepsNow(): Int {
        rollDemoDay()
        return (lastSteps + demoSteps).coerceAtLeast(0)
    }

    private suspend fun publish() {
        _state.value = runCatching { book.state(stepsNow()) }
            .getOrElse { _state.value.copy(stepsToday = stepsNow()) }
    }
}
