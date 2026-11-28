package cz.narsil.app.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cz.narsil.app.ServiceLocator
import cz.narsil.core.ComfortContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Příkazy dema z mostu na notebooku (161): `adb shell am broadcast -a cz.narsil.DEMO -p cz.narsil --es cmd <cmd>`.
 * Chráněno oprávněním DUMP (má ho jen adb shell), stejně jako ostatní ladicí receivery.
 * Výsledek jde zpět jako událost `ack` (a u `state` celý stav) přes [DemoBridge].
 *
 * Příkazy: `prepare` (Připravit demo), `home` | `away` | `auto`, `echo` (Ozvěna hned),
 * `xp` (+500 XP), `steps` (+1 000 kroků), `reset` (Reset dne), `state`.
 */
class DemoCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cmd = intent.getStringExtra("cmd") ?: "state"
        val pending = goAsync()
        ServiceLocator.appScope.launch {
            try {
                val ok = runCatching { run(cmd) }.getOrDefault(false)
                DemoBridge.emit(DemoEvent.Ack(cmd, ok))
                // Kontext a gamifikace se propíšou asynchronně – stav poslat až po nich (jinak by byl starý).
                delay(STATE_DELAY_MILLIS)
                DemoBridge.emitState()
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun run(cmd: String): Boolean = when (cmd) {
        "prepare" -> { DemoActions.prepareDemo(); true }
        "home" -> { DemoActions.setContext(ComfortContext.HOME); true }
        "away" -> { DemoActions.setContext(ComfortContext.AWAY); true }
        "auto" -> { DemoActions.setContext(null); true }
        "echo" -> DemoActions.showEchoNow()
        "xp" -> DemoActions.addXp()
        "steps" -> DemoActions.addSteps()
        "reset" -> { DemoActions.resetDay(); true }
        "state" -> true
        else -> false
    }

    private companion object {
        const val STATE_DELAY_MILLIS = 600L
    }
}
