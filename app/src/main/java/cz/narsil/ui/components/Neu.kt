package cz.narsil.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.aeroSurface
import cz.narsil.ui.theme.neuInset

// Sdílené prvky ve stylu Frutiger Aero (narsil-frontend-v2). Jména a parametry zůstaly z konceptu E (334),
// aby obrazovky a ostatní moduly nemusely měnit volání.

/** true = text leží přímo na nebi (bílý se stínem), false = na skle (tmavě modrý). */
val LocalOnSky = compositionLocalOf { false }

/** Obsah přímo na pozadí: [NeuText] kreslí bíle se stínem. */
@Composable
fun OnSky(content: @Composable () -> Unit) = CompositionLocalProvider(LocalOnSky provides true, content = content)

/** Obsah na skle (zruší [OnSky] uvnitř panelu). */
@Composable
fun OnGlass(content: @Composable () -> Unit) = CompositionLocalProvider(LocalOnSky provides false, content = content)

/** Barva textu podle podkladu: barvy „na skle“ se na nebi převedou na světlé. */
@Composable
@ReadOnlyComposable
fun aeroTextColor(color: Color): Color = if (!LocalOnSky.current) color else when (color) {
    NeuColors.Text, NeuColors.Label -> NeuColors.OnSky
    NeuColors.Muted -> NeuColors.OnSkyMuted
    NeuColors.Accent -> NeuColors.OnSkyAccent
    NeuColors.Success -> NeuColors.OnSkySuccess
    NeuColors.Error -> NeuColors.OnSkyError
    else -> color
}

/** Text: systémové písmo, váhy a velikosti z návrhu; na nebi bílý se stínem (`text-shadow`). */
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
    val onSky = LocalOnSky.current
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = size,
            fontWeight = weight,
            color = aeroTextColor(color),
            lineHeight = lineHeight,
            letterSpacing = letterSpacing,
            textAlign = align ?: TextAlign.Unspecified,
            shadow = if (onSky) Shadow(NeuColors.SkyTextShadow, Offset(0f, 2f), 6f) else null,
        ),
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}

/** Malý nadpis sekce (`.section-label`). */
@Composable
fun NeuLabel(text: String, modifier: Modifier = Modifier, color: Color = NeuColors.Label) {
    NeuText(text, modifier, size = 13.sp, weight = FontWeight.Bold, color = color, letterSpacing = 0.1.sp)
}

/** Obrazovka: průhledná (pozadí kreslí [cz.narsil.ui.theme.AeroBackdrop]), okraje 18 dp, text na nebi. */
@Composable
fun NeuScreen(
    modifier: Modifier = Modifier,
    gap: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    OnSky {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
            content = content,
        )
    }
}

/** Skleněný panel (`.glass-panel`); obsah má tmavý text. */
@Composable
fun NeuPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    depth: NeuDepth = NeuDepth.Normal,
    padding: PaddingValues = PaddingValues(16.dp),
    gap: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    OnGlass {
        Column(
            modifier = modifier.neuInset(depth, RoundedCornerShape(radius)).padding(padding),
            verticalArrangement = Arrangement.spacedBy(gap),
            content = content,
        )
    }
}

/** Tenká modrá dělicí čára mezi řádky ve skle. */
@Composable
fun NeuDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(NeuColors.Divider))
}

/** Hlavička na nebi: volitelné tlačítko zpět, velký bílý nadpis (`h1`), podnadpis a prvek vpravo. */
@Composable
fun NeuHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backDescription: String = "Zpět",
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    OnSky {
        Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onBack != null) NeuIconButton(NeuIcons.Back, backDescription, onBack)
            Column(Modifier.weight(1f)) {
                NeuText(title, size = 26.sp, weight = FontWeight.Bold, maxLines = 1, letterSpacing = (-0.6).sp, lineHeight = 30.sp)
                if (subtitle != null) NeuText(subtitle, size = 13.sp, weight = FontWeight.Medium, color = NeuColors.Muted, lineHeight = 17.sp)
            }
            trailing?.invoke(this)
        }
    }
}

/** Modré skleněné tlačítko s bílou ikonou (`.aero-icon-button.compact`). */
@Composable
fun NeuIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier
            .size(46.dp)
            .aeroSurface(AeroStyle.BlueGlass, shape)
            .clip(shape)
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/** Lesk podle barvy textu: zelená pro běžnou akci, červená pro chybu, matné sklo pro neutrální. */
private fun glossFor(color: Color): Pair<AeroStyle, Color> = when (color) {
    NeuColors.Error -> AeroStyle.RedGloss to Color.White
    NeuColors.Muted -> AeroStyle.Frost to NeuColors.Muted
    else -> AeroStyle.GreenBubble to NeuColors.OnAccent
}

/** Menší lesklé tlačítko („Ověřit“, „Pozastavit“). */
@Composable
fun NeuSmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeuColors.Accent,
    enabled: Boolean = true,
    height: Dp = 36.dp,
) {
    val shape = RoundedCornerShape(13.dp)
    val (style, textColor) = glossFor(color)
    OnGlass {
        Box(
            modifier
                .height(height)
                .alpha(if (enabled) 1f else 0.5f)
                .aeroSurface(style, shape)
                .clip(shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            NeuText(text, size = 13.sp, weight = FontWeight.Bold, color = textColor, maxLines = 1)
        }
    }
}

/** Hlavní akce: lesklé zelené tlačítko (`.primary-aero`); chybová barva = červený lesk. */
@Composable
fun NeuPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = NeuColors.Accent,
    contentColor: Color = NeuColors.OnAccent,
) {
    val shape = RoundedCornerShape(17.dp)
    val style = when (color) {
        NeuColors.Error -> AeroStyle.RedGloss
        NeuColors.Muted -> AeroStyle.Frost
        else -> AeroStyle.GreenGloss
    }
    val textColor = when {
        !enabled -> NeuColors.Muted
        style == AeroStyle.RedGloss -> Color.White
        contentColor == NeuColors.OnAccent || contentColor == NeuColors.OnSuccess -> NeuColors.OnAccent
        else -> contentColor
    }
    OnGlass {
        Box(
            modifier
                .fillMaxWidth()
                .height(54.dp)
                .aeroSurface(if (enabled) style else AeroStyle.Frost, shape)
                .clip(shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            NeuText(text, size = 16.sp, weight = FontWeight.Bold, color = textColor, maxLines = 1)
        }
    }
}

/**
 * Řada záložek ve skle (`.proof-tabs`); vybraná je bílo-zelená bublina.
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
    outerRadius: Dp = 18.dp,
    innerRadius: Dp = 13.dp,
    outerDepth: NeuDepth = NeuDepth.Normal,
    selectedDepth: NeuDepth = NeuDepth.Deep,
    ring: Boolean = false,
    fontSize: TextUnit = 12.sp,
) {
    OnGlass {
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
                        .clip(shape)
                        .clickable(role = Role.Tab) { onSelect(option) }
                        .semantics { this.selected = isSel }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    NeuText(
                        label(option),
                        size = fontSize,
                        weight = FontWeight.Bold,
                        color = if (isSel) NeuColors.OnAccent else NeuColors.Label,
                        maxLines = 1,
                        align = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Zelená ikona odznaku (`.badge-icon`) se zkratkou („ŠK“, „OZ“) bílým písmem. */
@Composable
fun NeuBadgeIcon(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    radius: Dp = 12.dp,
    color: Color = NeuColors.Accent,
    fontSize: TextUnit = 11.sp,
) {
    val style = when (color) {
        NeuColors.Muted -> AeroStyle.Frost
        NeuColors.Error -> AeroStyle.RedGloss
        else -> AeroStyle.GreenOrb
    }
    Box(modifier.size(size).aeroSurface(style, RoundedCornerShape(radius)), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = if (style == AeroStyle.Frost) NeuColors.Muted else Color.White,
                shadow = if (style == AeroStyle.Frost) null else Shadow(Color(0xAA08722A), Offset(0f, 2f), 4f),
            ),
            maxLines = 1,
        )
    }
}

/** Ukazatel postupu (`.progress`): průsvitná drážka se zeleným leskem. */
@Composable
fun NeuProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = NeuColors.Accent,
    height: Dp = 12.dp,
) {
    val fill = when (color) {
        NeuColors.Error -> listOf(Color(0xFFFFB4A6), Color(0xFFD9432E))
        NeuColors.Muted -> listOf(Color(0xFFB9D3DC), Color(0xFF7FA3B0))
        NeuColors.Accent, NeuColors.Success -> listOf(Color(0xFFADF87A), Color(0xFF22AF42))
        else -> listOf(color.copy(alpha = 0.6f), color)
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .aeroSurface(AeroStyle.Track, RoundedCornerShape(height / 2)),
    ) {
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(f)
                    .clip(RoundedCornerShape(height / 2))
                    .background(Brush.verticalGradient(fill)),
            ) {
                Box(Modifier.fillMaxWidth().height(height * 0.35f).background(Color(0x99FFFFFF), RoundedCornerShape(height / 2)))
            }
        }
    }
}

/** Jednořádkové / víceřádkové pole (`.add-task input`, `.proof-textarea`). */
@Composable
fun NeuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    radius: Dp = 14.dp,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = TextStyle(color = NeuColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
        cursorBrush = SolidColor(NeuColors.Accent),
        modifier = modifier.aeroSurface(AeroStyle.Frost, RoundedCornerShape(radius)).padding(horizontal = 14.dp, vertical = 13.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) OnGlass { NeuText(placeholder, size = 14.sp, weight = FontWeight.Medium, color = NeuColors.Muted) }
                inner()
            }
        },
    )
}

/** Řádek seznamu ve skle (`.task-row`): ikona, nadpis, podnadpis, prvek vpravo; volitelně klikací. */
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
            NeuText(title, size = 15.sp, weight = FontWeight.Bold, color = titleColor, maxLines = 1)
            if (!subtitle.isNullOrBlank()) NeuText(subtitle, size = 12.sp, weight = FontWeight.Medium, color = subtitleColor, maxLines = 2)
        }
        trailing?.invoke()
    }
}

/** Kolonka s číslem a popiskem (`.stats div`). */
@Composable
fun NeuStat(value: String, label: String, modifier: Modifier = Modifier, color: Color = NeuColors.Text) {
    Column(modifier.widthIn(min = 40.dp)) {
        NeuText(value, size = 17.sp, weight = FontWeight.Bold, color = if (color == NeuColors.Text) NeuColors.Accent else color)
        NeuText(label, size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted)
    }
}

/** Zelená koule s textem (`.task-count`, `.aero-orb`). */
@Composable
fun AeroOrb(text: String, modifier: Modifier = Modifier, size: Dp = 52.dp, fontSize: TextUnit = 16.sp) {
    Box(modifier.size(size).aeroSurface(AeroStyle.GreenOrb, CircleShape), contentAlignment = Alignment.Center) {
        Text(text, style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold, color = Color(0xFF075821)), maxLines = 1)
    }
}

/** Modrá skleněná koule s ikonou (`.verify-orb`). */
@Composable
fun AeroBlueOrb(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Box(modifier.size(size).aeroSurface(AeroStyle.BlueOrb, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.38f))
    }
}
