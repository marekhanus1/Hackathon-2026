package cz.narsil.core.data

import android.util.Log
import cz.narsil.core.ConsumptionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Úseky spotřeby z detekce → Room (121). Detekce drží jen probíhající relaci (`AccessibilityTriggerSource.current`,
 * null = pauza / mimo feed / mimo domov). Úsek = souvislý čas na jednom povrchu jedné aplikace; pauza se nepočítá.
 * Řádek se založí hned a délka se průběžně dopisuje, takže souboj s algoritmem a Zrcadlo vidí minuty živě.
 */
class ConsumptionSegmenter(private val flushEveryMillis: Long = FLUSH_EVERY_MILLIS) {
    /** Co zapsat: [Open] = nový řádek, [Extend] = nová délka posledního otevřeného řádku. */
    sealed interface Action {
        data class Open(val entity: ConsumptionEventEntity) : Action
        data class Extend(val durationMillis: Long) : Action
    }

    private class Open(val pkg: String, val surface: String, val sessionStart: Long, val start: Long, var end: Long, var flushedEnd: Long)

    private var open: Open? = null

    /** Relace a konec posledního uzavřeného úseku (navázání při přepnutí, pozná návrat po pauze). */
    private var lastSessionStart: Long? = null
    private var lastEnd: Long = 0

    fun onEvent(e: ConsumptionEvent?): List<Action> {
        val o = open
        if (e == null) return close()
        val end = e.startedAtMillis + e.durationMillis
        if (o != null && o.pkg == e.packageName && o.surface == e.surface.name && o.sessionStart == e.startedAtMillis) {
            if (end <= o.end) return emptyList()
            o.end = end
            if (end - o.flushedEnd < flushEveryMillis) return emptyList()
            o.flushedEnd = end
            return listOf(Action.Extend(end - o.start))
        }
        // Přepnutí povrchu / aplikace ve stejné relaci: navázat na konec předchozího úseku.
        // Návrat po krátké pauze (relace žije dál): počítat až od návratu, pauza se nepočítá.
        // Nová relace: od jejího začátku.
        val switched = o != null && o.sessionStart == e.startedAtMillis
        val resumed = o == null && lastSessionStart == e.startedAtMillis
        val actions = close().toMutableList()
        val start = when {
            switched -> lastEnd.coerceAtMost(end)
            resumed -> end
            else -> e.startedAtMillis
        }
        open = Open(e.packageName, e.surface.name, e.startedAtMillis, start, end, end)
        actions += Action.Open(
            ConsumptionEventEntity(
                packageName = e.packageName,
                surface = e.surface.name,
                startedAtMillis = start,
                durationMillis = end - start,
                videoTitle = e.videoTitle,
                url = e.url,
                // Detekce hlásí relaci jen doma (SPEC 3), jinde se nic nepočítá.
                context = "HOME",
            ),
        )
        return actions
    }

    private fun close(): List<Action> {
        val o = open ?: return emptyList()
        open = null
        lastSessionStart = o.sessionStart
        lastEnd = o.end
        return if (o.end > o.flushedEnd) listOf(Action.Extend(o.end - o.start)) else emptyList()
    }

    companion object {
        /** Jak často dopisovat délku probíhajícího úseku (zápis do Room, přepočet souboje). */
        const val FLUSH_EVERY_MILLIS = 30_000L
    }
}

/** Zapisuje [ConsumptionSegmenter] do [dao]. Spustit jednou v aplikačním scope. */
class ConsumptionRecorder(
    private val dao: ConsumptionDao,
    private val segmenter: ConsumptionSegmenter = ConsumptionSegmenter(),
) {
    private var openId: Long? = null

    fun start(events: StateFlow<ConsumptionEvent?>, scope: CoroutineScope) {
        scope.launch { events.collect { apply(segmenter.onEvent(it)) } }
    }

    internal suspend fun apply(actions: List<ConsumptionSegmenter.Action>) {
        for (a in actions) {
            runCatching {
                when (a) {
                    is ConsumptionSegmenter.Action.Open -> openId = dao.insert(a.entity)
                    is ConsumptionSegmenter.Action.Extend -> openId?.let { dao.updateDuration(it, a.durationMillis) }
                }
            }.onFailure { Log.w("NarsilData", "zápis spotřeby selhal", it) }
        }
    }
}
