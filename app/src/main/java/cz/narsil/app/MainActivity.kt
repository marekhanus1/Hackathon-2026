package cz.narsil.app

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import cz.narsil.app.nav.NarsilNavHost
import cz.narsil.integrations.strava.HttpStravaRepository
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.NarsilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openSetupIfAsked(intent)
        handleStravaRedirect(intent)
        GuardService.start(this)
        // Nebe nahoře i tráva dole jsou tmavé dost na bílé ikony systémových lišt.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            NarsilTheme {
                AeroBackdrop {
                    // Edge-to-edge: obsah všech obrazovek mimo stavovou a navigační lištu (i výřez a klávesnici).
                    NarsilNavHost(Modifier.safeDrawingPadding())
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (openSetupIfAsked(intent)) recreate()
        handleStravaRedirect(intent)
    }

    /** Notifikace „Telefon vypnul Narsil“ (KillWatcher): otevřít průvodce s kroky pro Xiaomi. */
    private fun openSetupIfAsked(intent: Intent?): Boolean {
        if (intent?.getBooleanExtra(KillWatcher.EXTRA_OPEN_SETUP, false) != true) return false
        intent.removeExtra(KillWatcher.EXTRA_OPEN_SETUP)
        cz.narsil.app.onboarding.OnboardingPrefs.setDone(this, false)
        return true
    }

    /** narsil://strava?code=… z prohlížeče po přihlášení ke Stravě (511). */
    private fun handleStravaRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "narsil" || data.host != "strava") return
        val strava = ServiceLocator.stravaRepository as? HttpStravaRepository ?: return
        // Chyba návratu (zrušeno, bez oprávnění, síť) se ukáže jako hláška v nastavení (132).
        lifecycleScope.launch { runCatching { strava.handleRedirect(data.toString()) } }
        intent?.data = null // otočení obrazovky nezpracuje stejný kód podruhé
    }
}
