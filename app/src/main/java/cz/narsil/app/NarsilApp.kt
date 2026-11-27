package cz.narsil.app

import android.app.Application

/** Vstupní bod aplikace. Inicializuje [ServiceLocator]. */
class NarsilApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
