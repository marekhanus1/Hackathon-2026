package cz.narsil.app

import android.app.Application

/** Vstupní bod aplikace. Inicializuje [ServiceLocator] a spojí detekci s Ozvěnou (115). */
class NarsilApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        EchoOverlayStarter.start()
        // 161: momenty dema do logcatu pro most k prezentaci.
        cz.narsil.app.demo.DemoBridge.start()
        // Proces mohl vypnout systém (MIUI): jednou za čas říct, co nastavit, a obnovit hlídací službu.
        KillWatcher.check(this)
        GuardService.start(this)
    }
}
