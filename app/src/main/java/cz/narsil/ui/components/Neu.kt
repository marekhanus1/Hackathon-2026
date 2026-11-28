package cz.narsil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.neuInset

// Sdílené prvky konceptu E (334): tmavý neumorfismus, kde je všechno vnořené.

/** Text konceptu E: písmo systémové (Manrope by byla nová závislost), váhy a velikosti z návrhu. */
@Composable
fun NeuText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    weight: FontWeight = FontWeight.Bold,
    color: Color = NeuColors.Text,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = size,
            fontWeight = weight,
            color = color,
            lineHeight = lineHeight,
            letterSpacing = letterSpacing,
            textAlign = align ?: TextAlign.Unspecified,
        ),
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}

/** Malý nadpis sekce velkými písmeny („POHYBOVÝ CÍL“). */
@Composable
fun NeuLabel(text: String, modifier: Modifier = Modifier, color: Color = NeuColors.Muted) {
    NeuText(text.uppercase(), modifier, size = 11.sp, weight = FontWeight.ExtraBold, color = color, letterSpacing = 0.9.sp)
}

/** Obrazovka: podklad, okraje 20/22 dp a svislé mezery mezi panely; nic se neposouvá. */
@Composable
fun NeuScreen(
    modifier: Modifier = Modifier,
    gap: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeuColors.Base)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/** Vnořený panel (`.in`). */
@Composable
fun NeuPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    depth: NeuDepth = NeuDepth.Normal,
    padding: PaddingValues = PaddingValues(16.dp),
    gap: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.neuInset(depth, RoundedCornerShape(radius)).padding(padding),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/** Tenká dělicí čára mezi řádky ve vaně. */
@Composable
fun NeuDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(NeuColors.Divider))
}

/** Hlavička: volitelné tlačítko zpět, nadpis, podnadpis a prvek vpravo. */
@Composable
fun NeuHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backDescription: String = "Zpět",
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onBack != null) NeuIconButton(NeuIcons.Back, backDescription, onBack)
        Column(Modifier.weight(1f)) {
            NeuText(title, size = 22.sp, weight = FontWeight.ExtraBold, maxLines = 1)
            if (subtitle != null) NeuText(subtitle, size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted)
        }
        trailing?.invoke(this)
    }
}

/** Kulaté mělce vnořené tlačítko s ikonou (zpět). */
@Composable
fun NeuIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .neuInset(NeuDepth.Shallow, CircleShape)
            .clip(CircleShape)
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = NeuColors.Text, modifier = Modifier.size(20.dp))
    }
}

/** Hluboce vnořené menší tlačítko s barevným textem („Ověřit“, „Pozastavit“). */
@Composable
fun NeuSmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeuColors.Accent,
    enabled: Boolean = true,
    height: Dp = 36.dp,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(height)
            .neuInset(NeuDepth.Deep, shape)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        NeuText(text, size = 12.sp, weight = FontWeight.ExtraBold, color = color, maxLines = 1)
    }
}

/** Hlavní akce: plné jantarové tlačítko ve vnořené liště (jediné „barevné“ místo obrazovky). */
@Composable
fun NeuPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = NeuColors.Accent,
    contentColor: Color = NeuColors.OnAccent,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.fillMaxWidth().neuInset(NeuDepth.Normal, RoundedCornerShape(24.dp)).padding(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(shape)
                .background(if (enabled) color else NeuColors.Deep)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            NeuText(
                text,
                size = 16.sp,
                weight = FontWeight.ExtraBold,
                color = if (enabled) contentColor else NeuColors.Muted,
                maxLines = 1,
            )
        }
    }
}

/**
 * Řada záložek ve vnořené liště; vybraná je zamáčknutá hlouběji ([NeuDepth.Deep]) s jantarovým textem.
 * Použití: proof mód, úkoly na ověření, pocity v Ozvěně, zóna v nastavení.
 */
@Composable
fun <T> NeuSegmented(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    outerRadius: Dp = 20.dp,
    innerRadius: Dp = 14.dp,
    outerDepth: NeuDepth = NeuDepth.Normal,
    selectedDepth: NeuDepth = NeuDepth.Deep,
    ring: Boolean = false,
    fontSize: TextUnit = 12.sp,
) {
    Row(
        modifier.fillMaxWidth().neuInset(outerDepth, RoundedCornerShape(outerRadius)).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSel = option == selected
            val shape = RoundedCornerShape(innerRadius)
            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .neuInset(if (isSel) selectedDepth else NeuDepth.Flat, shape)
                    .then(if (isSel && ring) Modifier.border(1.dp, NeuColors.Accent, shape) else Modifier)
                    .clip(shape)
                    .clickable(role = Role.Tab) { onSelect(option) }
                    .semantics { this.selected = isSel }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                NeuText(
                    label(option),
                    size = fontSize,
                    weight = FontWeight.ExtraBold,
                    color = if (isSel) NeuColors.Accent else NeuColors.Muted,
                    maxLines = 1,
                    align = TextAlign.Center,
                )
            }
        }
    }
}

/** Čtvereček se zkratkou („ŠK“, „OZ“) – hluboce vnořený. */
@Composable
fun NeuBadgeIcon(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    radius: Dp = 12.dp,
    color: Color = NeuColors.Accent,
    fontSize: TextUnit = 11.sp,
) {
    Box(modifier.size(size).neuInset(NeuDepth.Deep, RoundedCornerShape(radius)), contentAlignment = Alignment.Center) {
        NeuText(text, size = fontSize, weight = FontWeight.ExtraBold, color = color, maxLines = 1)
    }
}

/** Ukazatel postupu: hluboce vnořená drážka s výplní. */
@Composable
fun NeuProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = NeuColors.Accent,
    height: Dp = 10.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .neuInset(NeuDepth.Deep, RoundedCornerShape(height / 2))
            .padding(2.dp),
    ) {
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(RoundedCornerShape(height / 2)).background(color))
        }
    }
}

/** Jednořádkové / víceřádkové pole ve vnořené ploše. */
@Composable
fun NeuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    radius: Dp = 16.dp,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = TextStyle(color = NeuColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
        cursorBrush = SolidColor(NeuColors.Accent),
        modifier = modifier.neuInset(NeuDepth.Normal, RoundedCornerShape(radius)).padding(horizontal = 16.dp, vertical = 14.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) NeuText(placeholder, size = 14.sp, weight = FontWeight.Medium, color = NeuColors.Muted)
                inner()
            }
        },
    )
}

/** Řádek seznamu ve vaně: ikona, nadpis, podnadpis, prvek vpravo; volitelně klikací. */
@Composable
fun NeuListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = NeuColors.Muted,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleColor: Color = NeuColors.Text,
    verticalPadding: Dp = 10.dp,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            NeuText(title, size = 14.sp, weight = FontWeight.Bold, color = titleColor, maxLines = 1)
            if (!subtitle.isNullOrBlank()) NeuText(subtitle, size = 11.sp, weight = FontWeight.SemiBold, color = subtitleColor, maxLines = 2)
        }
        trailing?.invoke()
    }
}

/** Kolonka s číslem a popiskem („45 / min získáno“). */
@Composable
fun NeuStat(value: String, label: String, modifier: Modifier = Modifier, color: Color = NeuColors.Text) {
    Column(modifier.widthIn(min = 40.dp)) {
        NeuText(value, size = 16.sp, weight = FontWeight.ExtraBold, color = color)
        NeuText(label, size = 11.sp, weight = FontWeight.Medium, color = NeuColors.Muted)
    }
}
