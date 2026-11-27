package cz.narsil.contract

import android.content.Context
import cz.narsil.core.data.NarsilDatabase
import java.time.Clock
import kotlinx.coroutines.CoroutineScope

/**
 * Sestavení pro `ServiceLocator` (124):
 * ```
 * contractRepository  = if (useFakes) FakeContractRepository()  else ContractModule.contractRepository(appContext, appScope)
 * echoVideoRepository = if (useFakes) FakeEchoVideoRepository() else ContractModule.echoVideoRepository(appContext)
 * ```
 */
object ContractModule {
    fun contractRepository(context: Context, scope: CoroutineScope, clock: Clock = Clock.systemDefaultZone()) =
        RoomContractRepository(NarsilDatabase.get(context).contractDao(), scope, clock)

    fun echoVideoRepository(context: Context, clock: Clock = Clock.systemDefaultZone()) =
        RoomEchoVideoRepository(NarsilDatabase.get(context).contractDao(), PrefsOnboardingVideoStore(context), clock)
}
