package cz.narsil.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme

/**
 * Unified pill badge used for zone status, rewards, and compact actions across screens.
 */
@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val spacing = NarsilTheme.spacing
    val label: @Composable () -> Unit = {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
        )
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor
        ) {
            label()
        }
    } else {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor
        ) {
            label()
        }
    }
}

@Preview(name = "StatusPill – Světlý", showBackground = true)
@Composable
private fun StatusPillLightPreview() {
    NarsilTheme(darkTheme = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StatusPill(
                text = "Doma · ochrana aktivní",
                containerColor = NarsilTheme.colors.echoContainer,
                contentColor = NarsilTheme.colors.onEchoContainer
            )
        }
    }
}

@Preview(
    name = "StatusPill – Tmavý",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun StatusPillDarkPreview() {
    NarsilTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StatusPill(
                text = "+20 min",
                containerColor = NarsilTheme.colors.positiveContainer,
                contentColor = NarsilTheme.colors.onPositiveContainer,
                bold = true
            )
        }
    }
}
