package cz.narsil.app

import android.app.Application

/** Vstupní bod aplikace. Inicializuje [ServiceLocator] a spojí detekci s Ozvěnou (115). */
class NarsilApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        EchoOverlayStarter.start()
    }
}
