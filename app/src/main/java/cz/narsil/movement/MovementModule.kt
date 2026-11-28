package cz.narsil.movement

import android.content.Context
import cz.narsil.core.SettingsRepository
import cz.narsil.core.data.NarsilDatabase
import cz.narsil.movement.currency.CurrencyBook
import cz.narsil.movement.steps.CombinedStepsSource
import cz.narsil.movement.steps.HealthConnectStepsSource
import cz.narsil.movement.steps.SensorStepsSource
import java.time.Clock

/**
 * Sestavení skutečného pohybu pro `ServiceLocator` (124):
 * ```
 * val movementRepository: MovementRepository
 *     get() = cached("movementRepository") {
 *         if (useFakes) FakeMovementRepository() else MovementModule.create(appContext).also { it.startAutoRefresh(appScope) }
 *     }
 * ```
 */
object MovementModule {
    /**
     * @param settings nastavení (133): denní minuty od Parťáka a zprávy Parťákovi; null = výchozí hodnoty SPEC
     */
    fun create(
        context: Context,
        clock: Clock = Clock.systemDefaultZone(),
        settings: SettingsRepository? = null,
    ): StepsMovementRepository {
        val app = context.applicationContext
        val steps = CombinedStepsSource(HealthConnectStepsSource(app, clock), SensorStepsSource(app, clock))
        val book = CurrencyBook(
            RoomLedgerStore(NarsilDatabase.get(app).ledgerDao()),
            clock,
            debtLimitProvider = settings?.let { s -> { s.settings.value.debtLimitMinutes } },
        )
        val partner = PartnerNotifier(app)
        return StepsMovementRepository(
            steps, book, clock,
            onPartnerAlert = { debt ->
                if (settings?.settings?.value?.partnerAlerts != false) partner.notifyDebtOverLimit(debt)
            },
            onFriendGift = { minutes -> partner.notifyFriendGift(minutes) },
        )
    }
}
