package cz.narsil.app

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cz.narsil.app.nav.NarsilNavHost
import cz.narsil.integrations.strava.HttpStravaRepository
import cz.narsil.ui.theme.NarsilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleStravaRedirect(intent)
        enableEdgeToEdge()
        setContent {
            NarsilTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Edge-to-edge: obsah všech obrazovek mimo stavovou a navigační lištu (i výřez a klávesnici).
                    NarsilNavHost(Modifier.safeDrawingPadding())
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleStravaRedirect(intent)
    }

    /** narsil://strava?code=… z prohlížeče po přihlášení ke Stravě (511). */
    private fun handleStravaRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "narsil" || data.host != "strava") return
        val strava = ServiceLocator.stravaRepository as? HttpStravaRepository ?: return
        lifecycleScope.launch { runCatching { strava.handleRedirect(data.toString()) } }
    }
}
