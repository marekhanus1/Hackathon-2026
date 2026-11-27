package cz.narsil.gamification

import android.content.Context
import cz.narsil.core.ContractRepository
import cz.narsil.core.MovementRepository
import cz.narsil.core.data.DayKey
import cz.narsil.core.data.NarsilDatabase
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Splněná smlouva → [RulesGamificationEngine.onContractFulfilled] (+100 XP, série; SPEC 6).
 * Sleduje dnešní smlouvu a kroky; engine sám hlídá „jen jednou za den“.
 */
fun watchContractFulfillment(
    engine: RulesGamificationEngine,
    contracts: ContractRepository,
    movement: MovementRepository,
    scope: CoroutineScope,
): Job = combine(contracts.today, movement.state) { contract, steps -> contract?.isFulfilled(steps.stepsToday) == true }
    .distinctUntilChanged()
    .onEach { fulfilled -> if (fulfilled) runCatching { engine.onContractFulfilled() } }
    .launchIn(scope)

/**
 * Sestavení skutečné gamifikace pro `ServiceLocator` (124):
 * ```
 * gamificationEngine = if (useFakes) FakeGamificationEngine() else GamificationModule.create(appContext, appScope)
 * // po vytvoření smlouvy a pohybu:
 * GamificationModule.watchContract(engine, contractRepository, movementRepository, appScope)
 * ```
 */
object GamificationModule {

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun create(context: Context, scope: CoroutineScope, clock: Clock = Clock.systemDefaultZone()): RulesGamificationEngine {
        val db = NarsilDatabase.get(context)
        val feed = RoomFeedMinutes(db.consumptionDao(), clock)
        val engine = RulesGamificationEngine(
            store = RoomGamificationStore(db.xpDao(), db.badgeDao(), db.ledgerDao(), clock),
            clock = clock,
            feedMinutesFor = feed::forDate,
        )
        scope.launch { engine.refresh() }

        // Nové minuty feedu z detekce (121) → přepočítat souboj s algoritmem.
        flow {
            while (true) {
                emit(DayKey.of(clock.millis(), clock.zone))
                delay(60_000)
            }
        }.distinctUntilChanged()
            .flatMapLatest { day -> db.consumptionDao().observeFeedMinutesSince(DayKey.startMillis(day, clock.zone)) }
            .distinctUntilChanged()
            .debounce(2_000)
            .onEach { runCatching { engine.refresh() } }
            .launchIn(scope)

        // Boss (po 22:00) a přechod dne ve 4:00 se vyhodnotí i bez událostí.
        scope.launch {
            while (isActive) {
                delay(5 * 60_000L)
                runCatching { engine.refresh() }
            }
        }
        return engine
    }

    fun watchContract(
        engine: RulesGamificationEngine,
        contracts: ContractRepository,
        movement: MovementRepository,
        scope: CoroutineScope,
    ): Job = watchContractFulfillment(engine, contracts, movement, scope)
}
