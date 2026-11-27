package cz.narsil.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Palette: Reforged Twilight & Warm Ember
// Calm, encouraging, never-punishing tone for Narsil.

// Primary — Warm Forged Amber
val AmberLight = Color(0xFFC86D2D)
val OnAmberLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFFE5D0)
val OnAmberContainerLight = Color(0xFF4A2104)

val AmberDark = Color(0xFFF2A359)
val OnAmberDark = Color(0xFF451E00)
val AmberContainerDark = Color(0xFF663209)
val OnAmberContainerDark = Color(0xFFFFE5D0)

// Secondary — Calm Sage Green (positive choices like "Zavřít" & XP)
val SageLight = Color(0xFF2E7D5B)
val OnSageLight = Color(0xFFFFFFFF)
val SageContainerLight = Color(0xFFD2EFE0)
val OnSageContainerLight = Color(0xFF0A3825)

val SageDark = Color(0xFF5CC497)
val OnSageDark = Color(0xFF003823)
val SageContainerDark = Color(0xFF175139)
val OnSageContainerDark = Color(0xFFD2EFE0)

// Tertiary — Twilight Indigo (Ozvěna & evening reflection)
val TwilightLight = Color(0xFF5465B8)
val OnTwilightLight = Color(0xFFFFFFFF)
val TwilightContainerLight = Color(0xFFE0E5FA)
val OnTwilightContainerLight = Color(0xFF162052)

val TwilightDark = Color(0xFF9FA8DA)
val OnTwilightDark = Color(0xFF1D285E)
val TwilightContainerDark = Color(0xFF344180)
val OnTwilightContainerDark = Color(0xFFE0E5FA)

// Surfaces — Warm Alabaster (Light) & Deep Obsidian-Slate (Dark)
val AlabasterBackground = Color(0xFFFAF7F2)
val WarmSurfaceLight = Color(0xFFFFFFFF)
val WarmSurfaceVariantLight = Color(0xFFF2ECE1)
val WarmSurfaceContainerLight = Color(0xFFF5EFE6)
val WarmSurfaceContainerHighLight = Color(0xFFECE4D8)
val OnWarmLight = Color(0xFF1E1B18)
val OnWarmVariantLight = Color(0xFF5A5248)
val OutlineLight = Color(0xFFD7CEC0)
val OutlineVariantLight = Color(0xFFE8E0D4)

val ObsidianBackground = Color(0xFF121110)
val ObsidianSurfaceDark = Color(0xFF1C1A17)
val ObsidianSurfaceVariantDark = Color(0xFF26231F)
val ObsidianSurfaceContainerDark = Color(0xFF211E1A)
val ObsidianSurfaceContainerHighDark = Color(0xFF2E2A24)
val OnObsidianDark = Color(0xFFF3EFE9)
val OnObsidianVariantDark = Color(0xFFC1B7AA)
val OutlineDark = Color(0xFF474139)
val OutlineVariantDark = Color(0xFF332E28)

// Soft, non-punishing semantic alerts (never harsh alarm red)
val SoftErrorLight = Color(0xFFB8544B)
val OnSoftErrorLight = Color(0xFFFFFFFF)
val SoftErrorContainerLight = Color(0xFFFCE4E1)
val OnSoftErrorContainerLight = Color(0xFF4D1A15)

val SoftErrorDark = Color(0xFFE58C83)
val OnSoftErrorDark = Color(0xFF4F1713)
val SoftErrorContainerDark = Color(0xFF6B2B25)
val OnSoftErrorContainerDark = Color(0xFFFCE4E1)

val NarsilLightColorScheme = lightColorScheme(
    primary = AmberLight,
    onPrimary = OnAmberLight,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = OnAmberContainerLight,
    secondary = SageLight,
    onSecondary = OnSageLight,
    secondaryContainer = SageContainerLight,
    onSecondaryContainer = OnSageContainerLight,
    tertiary = TwilightLight,
    onTertiary = OnTwilightLight,
    tertiaryContainer = TwilightContainerLight,
    onTertiaryContainer = OnTwilightContainerLight,
    background = AlabasterBackground,
    onBackground = OnWarmLight,
    surface = WarmSurfaceLight,
    onSurface = OnWarmLight,
    surfaceVariant = WarmSurfaceVariantLight,
    onSurfaceVariant = OnWarmVariantLight,
    surfaceContainer = WarmSurfaceContainerLight,
    surfaceContainerHigh = WarmSurfaceContainerHighLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = SoftErrorLight,
    onError = OnSoftErrorLight,
    errorContainer = SoftErrorContainerLight,
    onErrorContainer = OnSoftErrorContainerLight
)

val NarsilDarkColorScheme = darkColorScheme(
    primary = AmberDark,
    onPrimary = OnAmberDark,
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = OnAmberContainerDark,
    secondary = SageDark,
    onSecondary = OnSageDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = TwilightDark,
    onTertiary = OnTwilightDark,
    tertiaryContainer = TwilightContainerDark,
    onTertiaryContainer = OnTwilightContainerDark,
    background = ObsidianBackground,
    onBackground = OnObsidianDark,
    surface = ObsidianSurfaceDark,
    onSurface = OnObsidianDark,
    surfaceVariant = ObsidianSurfaceVariantDark,
    onSurfaceVariant = OnObsidianVariantDark,
    surfaceContainer = ObsidianSurfaceContainerDark,
    surfaceContainerHigh = ObsidianSurfaceContainerHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = SoftErrorDark,
    onError = OnSoftErrorDark,
    errorContainer = SoftErrorContainerDark,
    onErrorContainer = OnSoftErrorContainerDark
)

/**
 * Semantic colors tailored to Narsil's core mechanics:
 * Ozvěna, positive choices, movement ember, gentle debt, and XP progression.
 */
@Immutable
data class NarsilExtendedColors(
    val positive: Color,
    val onPositive: Color,
    val positiveContainer: Color,
    val onPositiveContainer: Color,
    val ember: Color,
    val onEmber: Color,
    val emberContainer: Color,
    val onEmberContainer: Color,
    val echo: Color,
    val onEcho: Color,
    val echoContainer: Color,
    val onEchoContainer: Color,
    val debt: Color,
    val onDebt: Color,
    val debtContainer: Color,
    val onDebtContainer: Color,
    val algorithm: Color,
    val algorithmContainer: Color,
    val xpTrack: Color,
    val xpFill: Color,
    val goldAccent: Color
)

val LightNarsilExtendedColors = NarsilExtendedColors(
    positive = SageLight,
    onPositive = OnSageLight,
    positiveContainer = SageContainerLight,
    onPositiveContainer = OnSageContainerLight,
    ember = AmberLight,
    onEmber = OnAmberLight,
    emberContainer = AmberContainerLight,
    onEmberContainer = OnAmberContainerLight,
    echo = TwilightLight,
    onEcho = OnTwilightLight,
    echoContainer = TwilightContainerLight,
    onEchoContainer = OnTwilightContainerLight,
    debt = Color(0xFFC27B38),
    onDebt = Color(0xFFFFFFFF),
    debtContainer = Color(0xFFFBE9D5),
    onDebtContainer = Color(0xFF4D2B08),
    algorithm = Color(0xFF7B6B8D),
    algorithmContainer = Color(0xFFEDE8F3),
    xpTrack = Color(0xFFE4DDD1),
    xpFill = SageLight,
    goldAccent = Color(0xFFD49B27)
)

val DarkNarsilExtendedColors = NarsilExtendedColors(
    positive = SageDark,
    onPositive = OnSageDark,
    positiveContainer = SageContainerDark,
    onPositiveContainer = OnSageContainerDark,
    ember = AmberDark,
    onEmber = OnAmberDark,
    emberContainer = AmberContainerDark,
    onEmberContainer = OnAmberContainerDark,
    echo = TwilightDark,
    onEcho = OnTwilightDark,
    echoContainer = TwilightContainerDark,
    onEchoContainer = OnTwilightContainerDark,
    debt = Color(0xFFE5A96D),
    onDebt = Color(0xFF432605),
    debtContainer = Color(0xFF5A3916),
    onDebtContainer = Color(0xFFFBE9D5),
    algorithm = Color(0xFFB5A5C9),
    algorithmContainer = Color(0xFF322A3D),
    xpTrack = Color(0xFF332E28),
    xpFill = SageDark,
    goldAccent = Color(0xFFF2C14E)
)

val LocalNarsilColors = staticCompositionLocalOf { LightNarsilExtendedColors }
