package cz.narsil.core.fake

import cz.narsil.core.ConsumptionEvent
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.InterventionTriggerSource
import cz.narsil.core.Surface
import cz.narsil.core.TriggerReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Fake: zásah se vyvolá ručně přes [fire] (demo panel, náhledy). */
class FakeInterventionTriggerSource : InterventionTriggerSource {
    private val _triggers = MutableSharedFlow<InterventionTrigger>(extraBufferCapacity = 8)
    override val triggers: Flow<InterventionTrigger> = _triggers.asSharedFlow()

    fun fire(trigger: InterventionTrigger = demoTrigger()) {
        _triggers.tryEmit(trigger)
    }

    companion object {
        /** TikTok, 6 minut ve feedu. */
        fun demoTrigger(now: Long = System.currentTimeMillis()) = InterventionTrigger(
            event = ConsumptionEvent(
                packageName = "com.zhiliaoapp.musically",
                surface = Surface.FEED,
                startedAtMillis = now - 6 * 60_000,
                durationMillis = 6 * 60_000,
            ),
            reason = TriggerReason.ENDLESS_SCROLL,
            atMillis = now,
        )
    }
}
