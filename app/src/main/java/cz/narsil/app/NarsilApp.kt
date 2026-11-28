package cz.narsil.app

import android.app.Application

/** Vstupní bod aplikace. Inicializuje [ServiceLocator] a spojí detekci s Ozvěnou (115). */
class NarsilApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // ML Kit si pouští vlastní proces (cz.narsil:mlkit_acceleration_mini_benchmark), který taky vytvoří
        // Application. Tam nic nespouštět: druhá kopie Wi-Fi, Ozvěny a hlídací služby by se prala s hlavní.
        if (getProcessName() != packageName) return
        ServiceLocator.init(this)
        EchoOverlayStarter.start()
        // Proces mohl vypnout systém (MIUI): jednou za čas říct, co nastavit, a obnovit hlídací službu.
        KillWatcher.check(this)
        GuardService.start(this)
    }
}
