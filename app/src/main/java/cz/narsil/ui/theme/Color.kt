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
val OnAmberDark = Color(0xFF3A1A00)
val AmberContainerDark = Color(0xFF663209)
val OnAmberContainerDark = Color(0xFFFFE5D0)

// Secondary — Calm Sage Green (positive choices like "Zavřít" & XP)
val SageLight = Color(0xFF2E7D5B)
val OnSageLight = Color(0xFFFFFFFF)
val SageContainerLight = Color(0xFFD2EFE0)
val OnSageContainerLight = Color(0xFF0A3825)

val SageDark = Color(0xFF5CC497)
val OnSageDark = Color(0xFF10241B)
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

val ObsidianBackground = Color(0xFF272B34) // koncept E (334)
val ObsidianSurfaceDark = Color(0xFF23272F)
val ObsidianSurfaceVariantDark = Color(0xFF20232A)
val ObsidianSurfaceContainerDark = Color(0xFF23272F)
val ObsidianSurfaceContainerHighDark = Color(0xFF2E333D)
val OnObsidianDark = Color(0xFFE9ECF2)
val OnObsidianVariantDark = Color(0xFFA3AAB8)
val OutlineDark = Color(0xFF4A505C)
val OutlineVariantDark = Color(0xFF30353F)

// Soft, non-punishing semantic alerts (never harsh alarm red)
val SoftErrorLight = Color(0xFFB8544B)
val OnSoftErrorLight = Color(0xFFFFFFFF)
val SoftErrorContainerLight = Color(0xFFFCE4E1)
val OnSoftErrorContainerLight = Color(0xFF4D1A15)

val SoftErrorDark = Color(0xFFF59A8B)
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
    surfaceContainerHighest = ObsidianSurfaceContainerHighDark,
    surfaceContainerLow = ObsidianSurfaceContainerDark,
    surfaceContainerLowest = ObsidianSurfaceVariantDark,
    surfaceBright = ObsidianSurfaceContainerHighDark,
    surfaceDim = ObsidianSurfaceVariantDark,
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
    xpTrack = Color(0xFF20232A),
    xpFill = SageDark,
    goldAccent = Color(0xFFF2C14E)
)

val LocalNarsilColors = staticCompositionLocalOf { LightNarsilExtendedColors }

// Frutiger Aero (narsil-frontend-v2): zelený lesk, modré nebe, tmavě modrý text na skle.
val AeroColorScheme = lightColorScheme(
    primary = Color(0xFF1E9E3E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3F8BC),
    onPrimaryContainer = Color(0xFF064F20),
    secondary = Color(0xFF0A7FC0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDEFFF),
    onSecondaryContainer = Color(0xFF063B5C),
    tertiary = Color(0xFF2B6CC4),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCEBFF),
    onTertiaryContainer = Color(0xFF0B2E5C),
    background = Color(0xFF159DE9),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xF2FFFFFF),
    onSurface = Color(0xFF063B5C),
    surfaceVariant = Color(0xFFE2F6FF),
    onSurfaceVariant = Color(0xFF3F6B78),
    surfaceContainer = Color(0xF0F2FCFF),
    surfaceContainerHigh = Color(0xFFE6F7FF),
    surfaceContainerHighest = Color(0xFFDDF3FF),
    surfaceContainerLow = Color(0xF5F7FDFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFD5EEFA),
    outline = Color(0xFF8CC3DC),
    outlineVariant = Color(0xFFC4E4F2),
    error = Color(0xFFC2412F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE1DA),
    onErrorContainer = Color(0xFF5A1409),
)

val AeroNarsilExtendedColors = NarsilExtendedColors(
    positive = Color(0xFF168231),
    onPositive = Color(0xFFFFFFFF),
    positiveContainer = Color(0xFFD3F8BC),
    onPositiveContainer = Color(0xFF064F20),
    ember = Color(0xFF0A7FC0),
    onEmber = Color(0xFFFFFFFF),
    emberContainer = Color(0xFFCDEFFF),
    onEmberContainer = Color(0xFF063B5C),
    echo = Color(0xFF2B6CC4),
    onEcho = Color(0xFFFFFFFF),
    echoContainer = Color(0xFFDCEBFF),
    onEchoContainer = Color(0xFF0B2E5C),
    debt = Color(0xFFD2861A),
    onDebt = Color(0xFFFFFFFF),
    debtContainer = Color(0xFFFFF0CC),
    onDebtContainer = Color(0xFF4D2B08),
    algorithm = Color(0xFF6E5BA8),
    algorithmContainer = Color(0xFFECE6FA),
    xpTrack = Color(0x24005B96),
    xpFill = Color(0xFF36BB45),
    goldAccent = Color(0xFFE0A500),
)
