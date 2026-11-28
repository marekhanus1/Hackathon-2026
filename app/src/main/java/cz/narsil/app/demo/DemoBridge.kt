package cz.narsil.app.demo

import android.util.Log
import cz.narsil.app.EchoOverlayStarter
import cz.narsil.app.ServiceLocator
import cz.narsil.core.ComfortContext
import cz.narsil.core.Exercise
import cz.narsil.core.data.NarsilDatabase
import cz.narsil.overlay.ExerciseText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Hlášení momentů dema (161) do logcatu: most na notebooku (scripts/demo-most.py) je čte živě
 * (`adb logcat -v raw -s NarsilDemo:I`) a posílá prezentaci. Jen čte stav z [ServiceLocator],
 * nic nemění; v provozu stojí jeden řádek logu za událost.
 */
object DemoBridge {
    const val TAG = "NarsilDemo"
    private const val ECHO_POLL_MILLIS = 400L
    private var started = false

    fun emit(event: DemoEvent) {
        Log.i(TAG, event.toJson())
    }

    fun where(c: ComfortContext): String = if (c == ComfortContext.HOME) "home" else "away"

    fun start() {
        if (started) return
        started = true
        val sl = ServiceLocator
        val scope = sl.appScope

        // Doma / venku (UNKNOWN = mimo domov, tak se chová i detekce).
        scope.launch {
            sl.contextRepository.context.map(::where).distinctUntilChanged().drop(1).collect { emit(DemoEvent.Context(it)) }
        }

        // Ozvěna: okno nemá Flow, stačí se ptát (0,4 s je pro prezentaci okamžitě).
        scope.launch {
            var showing = false
            while (true) {
                val now = EchoOverlayStarter.isEchoShowing()
                if (now != showing) emit(if (now) DemoEvent.EchoShown else DemoEvent.EchoHidden)
                showing = now
                delay(ECHO_POLL_MILLIS)
            }
        }

        // Dřepy i kliky: počet jen během běžící série (stop nechává count, ten už neposílat).
        listOf(Exercise.SQUATS, Exercise.PUSHUPS).map { it to sl.exerciseCounter(it) }.distinctBy { it.second }.forEach { (ex, counter) ->
            val target = ExerciseText.target(ex)
            scope.launch {
                counter.count.collect { n -> if (counter.isRunning.value) emit(DemoEvent.Squats(n, target)) }
            }
            scope.launch {
                counter.isRunning.drop(1).collect { running -> if (running) emit(DemoEvent.Squats(0, target)) }
            }
        }

        // Odevzdání / ověření úkolu smlouvy: změna stavu úkolu ve stejném dni.
        scope.launch {
            var lastDate: java.time.LocalDate? = null
            var last = emptyMap<Long, cz.narsil.core.TaskStatus>()
            sl.contractRepository.today.collect { c ->
                val now = c?.tasks.orEmpty().associate { it.id to it.status }
                if (c != null && c.date == lastDate) {
                    c.tasks.filter { last[it.id] != null && last[it.id] != it.status }
                        .forEach { emit(DemoEvent.Task(it.title, it.status.name)) }
                }
                lastDate = c?.date
                last = now
            }
        }

        // Odemčený feed: jen prodloužení do budoucna (clear = 0 se nehlásí).
        scope.launch {
            sl.unlockTimer.unlockedUntil.drop(1).collect {
                val min = remainingMinutes()
                if (min > 0) emit(DemoEvent.Unlocked(min))
            }
        }

        // XP a nová úroveň (první hodnota je načtený stav, ne změna).
        scope.launch {
            var last = sl.gamificationEngine.state.value
            sl.gamificationEngine.state.collect { s ->
                if (s.xp > last.xp) emit(DemoEvent.Xp(s.xp - last.xp, s.xp))
                if (s.level > last.level) emit(DemoEvent.Level(s.level))
                last = s
            }
        }

        // Ohodnocené video / odpověď po videu (156): přibyl řádek v tabulce hodnocení.
        scope.launch {
            runCatching {
                var lastId: Long? = null
                NarsilDatabase.get(sl.appContext).videoRatingDao().observeAll().collect { rows ->
                    val newest = rows.maxByOrNull { it.id }
                    val prev = lastId
                    if (prev != null && newest != null && newest.id > prev) emit(DemoEvent.Rated(newest.title))
                    lastId = newest?.id ?: 0L
                }
            }
        }
    }

    /** Celý stav najednou (příkaz `state`). */
    fun emitState() {
        val sl = ServiceLocator
        val g = sl.gamificationEngine.state.value
        emit(
            DemoEvent.State(
                where = where(sl.contextRepository.context.value),
                level = g.level,
                xp = g.xp,
                echo = EchoOverlayStarter.isEchoShowing(),
                unlockedMin = remainingMinutes(),
            ),
        )
    }

    private fun remainingMinutes(): Int = ((ServiceLocator.unlockTimer.remainingMillis() + 59_999) / 60_000).toInt()
}
