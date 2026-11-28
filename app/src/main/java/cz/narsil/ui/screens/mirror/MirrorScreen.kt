package cz.narsil.ui.screens.mirror

import cz.narsil.ui.text.CzechPlural
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.components.AeroOrb
import cz.narsil.ui.components.NeuDivider
import cz.narsil.ui.components.NeuHeader
import cz.narsil.ui.components.NeuLabel
import cz.narsil.ui.components.NeuPanel
import cz.narsil.ui.components.NeuSegmented
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.components.OnGlass
import cz.narsil.ui.components.OnSky
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.aeroSurface

/** Zelená hodnota velkého čísla (`.hero-stat strong`). */
private val HeroGreen = Color(0xFF0C8731)
/** Modrá hodnota v mřížce (`.mirror-grid strong`). */
private val MetricBlue = Color(0xFF087FB0)
/** Dělicí čára mřížky (`.mirror-grid` gap). */
private val GridLineColor = Color(0x210078B1)

/**
 * Zrcadlo a mapa hodnoty obsahu za 21 dní (430) ve stylu Frutiger Aero (narsil-frontend-v2, `#screen-mirror`):
 * nadpis na nebi se zelenou koulí, velké číslo získaného času, mřížka čísel a týdenní trend ve skle.
 * Obrazovka je průhledná (pozadí kreslí AeroBackdrop), dok kreslí navigace.
 */
@Composable
fun MirrorScreen(
    state: MirrorUiState,
    onSelectTab: (MirrorTab) -> Unit,
    modifier: Modifier = Modifier
) {
    OnSky {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeuHeader(
                title = "Zrcadlo",
                subtitle = "Zrcadlo a hodnota obsahu za posledních 21 dní.",
                trailing = { AeroOrb("↗") },
            )

            NeuSegmented(
                options = MirrorTab.entries,
                selected = state.activeTab,
                label = { it.label },
                onSelect = onSelectTab,
            )

            when (state.activeTab) {
                MirrorTab.MIRROR -> MirrorOverviewContent(state = state)
                MirrorTab.VALUE_MAP -> ValueMapContent(state = state)
            }
        }
    }
}

@Composable
private fun MirrorOverviewContent(state: MirrorUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Velké číslo (`.hero-stat`)
        NeuPanel(Modifier.fillMaxWidth(), padding = PaddingValues(18.dp), gap = 4.dp) {
            val minutes = state.totalReclaimedMinutes
            NeuText(
                "Za 21 dní sis vzal zpět",
                Modifier.fillMaxWidth(),
                size = 13.sp,
                weight = FontWeight.Medium,
                color = NeuColors.Muted,
                align = TextAlign.Center,
            )
            NeuText(
                if (minutes >= 60) "${minutes / 60} h" else "$minutes min",
                Modifier.fillMaxWidth(),
                size = 44.sp,
                weight = FontWeight.Bold,
                color = HeroGreen,
                align = TextAlign.Center,
                lineHeight = 48.sp,
            )
            NeuText(
                if (minutes >= 60) CzechPlural.hoursAccusative(minutes / 60) + " · " + CzechPlural.minutesAccusative(minutes)
                else CzechPlural.minutesAccusative(minutes),
                Modifier.fillMaxWidth(),
                size = 13.sp,
                weight = FontWeight.Medium,
                color = NeuColors.Muted,
                align = TextAlign.Center,
            )
        }

        // Mřížka 2 × 2 (`.mirror-grid`)
        OnGlass {
            val shape = RoundedCornerShape(22.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .aeroSurface(AeroStyle.Glass, shape)
                    .clip(shape)
            ) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    MetricCell("−${state.feedDropPercent} %", "pasivního scrollování", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(GridLineColor))
                    MetricCell(CzechPlural.squats(state.averageDailySquats), "v průměru denně", Modifier.weight(1f))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(GridLineColor))
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    MetricCell(CzechPlural.days(state.bossSurvivedDays), "bez feedu (+150 XP za den)", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(GridLineColor))
                    MetricCell(state.criticalBossHour, "Boss kritické hodiny", Modifier.weight(1f))
                }
            }
        }

        NeuPanel(Modifier.fillMaxWidth(), padding = PaddingValues(15.dp), gap = 4.dp) {
            NeuText("Boss kritické hodiny · ${state.criticalBossHour}", size = 14.sp, weight = FontWeight.Bold)
            NeuText(
                "Největší pokušení přichází večer. Bez feedu přežito: ${CzechPlural.days(state.bossSurvivedDays)} (+150 XP za den).",
                size = 13.sp,
                weight = FontWeight.Medium,
                color = NeuColors.Muted,
                lineHeight = 18.sp,
            )
        }

        // Týdenní trend (`.chart-card`, `.bars`)
        NeuPanel(Modifier.fillMaxWidth(), padding = PaddingValues(15.dp), gap = 10.dp) {
            NeuLabel("Vývoj po týdnech (získaný čas vs. feed)")
            TrendBars(state.weeklyTrend)
            if (state.weeklyTrend.isNotEmpty()) NeuDivider()
            state.weeklyTrend.forEach { point ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    NeuText(point.dayLabel, size = 12.sp, weight = FontWeight.Bold, maxLines = 1)
                    NeuText(
                        "${point.reclaimedMinutes} min pro tebe · ${point.feedMinutes} min feed",
                        size = 11.sp,
                        weight = FontWeight.Medium,
                        color = NeuColors.Muted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Color(0x66F3FFFF)).padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        NeuText(value, size = 17.sp, weight = FontWeight.Bold, color = MetricBlue, maxLines = 1)
        NeuText(label, size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted, maxLines = 2, lineHeight = 15.sp)
    }
}

/** Sloupce podílu získaného času (zelený lesk se zaobleným vrškem a bílým odleskem, `.bars i`). */
@Composable
private fun TrendBars(points: List<DailyTrendPoint>) {
    if (points.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEach { point ->
            val total = (point.reclaimedMinutes + point.feedMinutes).coerceAtLeast(1)
            val reclaimedShare = (point.reclaimedMinutes.toFloat() / total.toFloat()).coerceIn(0.1f, 0.9f)
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    val barShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().fillMaxHeight((reclaimedShare + 0.12f).coerceAtMost(1f)), verticalArrangement = Arrangement.Bottom) {
                        NeuText("${point.reclaimedMinutes} min", size = 11.sp, weight = FontWeight.Bold, color = NeuColors.Success, maxLines = 1)
                        Box(
                            Modifier
                                .padding(top = 3.dp)
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(barShape)
                                .background(Brush.verticalGradient(listOf(Color(0xFF92F064), Color(0xFF20AF43))))
                        ) {
                            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xB8FFFFFF)))
                        }
                    }
                }
                NeuText(point.dayLabel, Modifier.padding(top = 5.dp), size = 11.sp, weight = FontWeight.Medium, color = NeuColors.Muted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ValueMapContent(state: MirrorUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NeuText(
            text = "Který obsah ti dává energii a který tě jen vysává (podle tvých hodnocení 1–5).",
            size = 13.sp,
            weight = FontWeight.Medium,
            color = NeuColors.Muted,
            lineHeight = 18.sp,
        )

        if (state.valueEntries.isEmpty()) {
            NeuPanel(Modifier.fillMaxWidth(), padding = PaddingValues(15.dp)) {
                NeuText(
                    text = "Zatím žádná hodnocení. Po videu na YouTube ohodnoť, co ti dalo – tady se pak ukáže, co stojí za to.",
                    size = 13.sp,
                    weight = FontWeight.Medium,
                    color = NeuColors.Muted,
                    lineHeight = 18.sp,
                )
            }
        }
        state.valueEntries.forEach { entry ->
            NeuPanel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 15.dp, vertical = 13.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        NeuText(entry.channelOrFormat, size = 14.sp, weight = FontWeight.Bold, maxLines = 2)
                        NeuText(
                            text = if (entry.isLowValueTrap) {
                                "${entry.videoCount}× · díváš se, ale hodnotíš nízko"
                            } else {
                                if (entry.averageRating >= 4f) "${entry.videoCount}× · vysoká hodnota pro tebe" else "${entry.videoCount}× · střední hodnota"
                            },
                            size = 12.sp,
                            weight = FontWeight.Medium,
                            color = if (entry.isLowValueTrap) NeuColors.Error else NeuColors.Muted,
                        )
                    }
                    val pill = RoundedCornerShape(50)
                    Box(
                        Modifier
                            .aeroSurface(if (entry.isLowValueTrap) AeroStyle.RedGloss else AeroStyle.GreenGloss, pill)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        NeuText(
                            "${entry.averageRating} / 5",
                            size = 13.sp,
                            weight = FontWeight.Bold,
                            color = if (entry.isLowValueTrap) Color.White else NeuColors.OnAccent,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Zrcadlo", widthDp = 390, heightDp = 860)
@Composable
private fun MirrorLightPreview() {
    NarsilTheme(darkTheme = false) {
        AeroBackdrop {
            MirrorScreen(
                state = MirrorUiState(activeTab = MirrorTab.MIRROR),
                onSelectTab = {}
            )
        }
    }
}

@Preview(name = "Mapa hodnoty", widthDp = 390, heightDp = 860)
@Composable
private fun ValueMapPreview() {
    NarsilTheme(darkTheme = false) {
        AeroBackdrop {
            MirrorScreen(
                state = MirrorUiState(activeTab = MirrorTab.VALUE_MAP),
                onSelectTab = {}
            )
        }
    }
}
