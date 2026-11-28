package cz.narsil.overlay

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Video Ozvěny (113) přes ExoPlayer. Přehraje [uri] jednou se zvukem; při chybě nebo bez videa
 * ukáže [fallbackText] (SPEC 9: „Dnes jsi chtěl: …“). Vkládá se do `videoSlot` obrazovky Ozvěny (310).
 */
@OptIn(UnstableApi::class)
@Composable
fun EchoVideoPlayer(uri: String?, fallbackText: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Neexistující zdroj (např. demo video, které ještě není v res/raw) → rovnou záložní text.
    var failed by remember(uri) { mutableStateOf(uri == null || !isPlayable(context, uri)) }
    if (failed) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = fallbackText ?: "Čemu se teď vyhýbáš?",
                // Aero: bílý text se stínem v tmavém rámu videa.
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(Color(0xCC00578E), Offset(0f, 2f), 6f),
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        }
        player.addListener(listener)
        // Chyba mohla přijít dřív, než se posluchač připojil.
        if (player.playerError != null) failed = true
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = player
            }
        },
    )
}

/** Lze [uri] otevřít? (content://, file://, android.resource://). Chyba = ne. */
private fun isPlayable(context: Context, uri: String): Boolean =
    runCatching { context.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false }
        .getOrDefault(false)
