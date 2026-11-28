package cz.narsil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors

/**
 * Lesklá pilulka (zóna, odměna, malá akce) ve stylu Frutiger Aero: výplň [containerColor] s bílým leskem
 * nahoře a bílým okrajem, text [contentColor].
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
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(lerp(containerColor, Color.White, 0.55f), containerColor)), shape)
            .border(1.dp, Color(0xE6FFFFFF), shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold,
                color = contentColor,
            ),
            maxLines = 1,
        )
    }
}

@Preview(name = "StatusPill – Aero", widthDp = 360, heightDp = 90)
@Composable
private fun StatusPillPreview() {
    NarsilTheme {
        AeroBackdrop {
            NeuPanel(Modifier.padding(16.dp)) {
                Row {
                    StatusPill("Doma · ochrana aktivní", Color(0xFFAAF7DE), NeuColors.OnAccent)
                    StatusPill("+20 min", Color(0xFF8FD8FF), NeuColors.Text, bold = true, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
