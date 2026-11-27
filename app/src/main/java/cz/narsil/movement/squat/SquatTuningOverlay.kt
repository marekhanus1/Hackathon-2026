package cz.narsil.movement.squat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Ladicí ukazatel pro sál (231): hloubka dřepu, úhel v koleni, počet a hláška, když nohy nejsou vidět.
 * Vložit přes náhled kamery (demo panel / obrazovka dřepů v demo režimu). Není to UI produktu.
 */
@Composable
fun SquatTuningOverlay(counter: PoseSquatCounter, modifier: Modifier = Modifier) {
    val count by counter.count.collectAsState()
    val depth by counter.depth.collectAsState()
    val angle by counter.kneeAngle.collectAsState()
    val visible by counter.personVisible.collectAsState()
    Column(
        modifier
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(8.dp),
    ) {
        Text("Dřepy: $count", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(
            "Hloubka: ${depth?.let { "%.2f".format(it) } ?: "–"}  ·  koleno: ${angle?.let { "%.0f°".format(it) } ?: "–"}",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
        )
        LinearProgressIndicator(
            progress = { (depth ?: 0.0).toFloat() },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        if (!visible) {
            Text(
                "Nevidím kyčle a kolena – ustup dál od telefonu nebo rozsviť",
                color = Color(0xFFFFC857),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
