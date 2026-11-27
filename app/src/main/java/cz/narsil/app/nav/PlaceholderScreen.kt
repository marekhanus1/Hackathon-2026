package cz.narsil.app.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Zástupná obrazovka: nadpis a tlačítka na ostatní obrazovky, ať jde aplikace proklikat. */
@Composable
fun PlaceholderScreen(
    route: NarsilRoute,
    onNavigate: (NarsilRoute) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(route.title, style = MaterialTheme.typography.headlineMedium)
        Text("Zástupná obrazovka", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        NarsilRoute.all.filter { it != route }.forEach { target ->
            Button(onClick = { onNavigate(target) }, modifier = Modifier.fillMaxWidth()) {
                Text(target.title)
            }
        }
        if (onBack != null) {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Zpět")
            }
        }
    }
}
