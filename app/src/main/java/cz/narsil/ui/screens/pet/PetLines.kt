package cz.narsil.ui.screens.pet

import java.text.Normalizer
import kotlin.math.floorMod

/** Offline zásoba klidných a povzbudivých hlášek sovy Narsilky (max 2 věty). */
object PetLines {

    private val wonDayGreetings = listOf(
        "Dneska vedeš nad algoritmem. Pěkná práce, jen tak dál!",
        "Získané minuty jsou dnes v plusu. Cítím, jak mám víc sil.",
        "Další vyhraný kousek pozornosti pro tebe. Mám z toho radost.",
        "Skvělý balanc! Dneska držíš směr pevně ve vlastních rukou.",
    )

    private val debtGreetings = listOf(
        "Dneska trochu na dluh, ale nic se neděje. Každý krok zpátky do klidu se počítá.",
        "Půjčené minuty jsou jen číslo, ne prohra. Zítra začneme s čistým štítem.",
        "Náročný den? Nevadí, odpočiň si a zkusíme jen jeden malý krůček.",
        "I s menším dluhem jsi pořád na dobré cestě. Netlač na sebe.",
    )

    private val streakGreetings = listOf(
        "Tvoje série už pěkně roste! Rytmus ti sedí.",
        "Několik dní v kuse držíš slovo sám sobě. To je opravdová síla.",
        "Série pokračuje a je to znát. Klidná hlava dělá divy.",
        "Krok za krokem buduješ pevný návyk. Jsem na tebe pyšná.",
    )

    private val generalGreetings = listOf(
        "Jsem tady, kdybys potřeboval na chvíli vypnout feed. Jak se dnes máš?",
        "Stačí jedno vědomé zavření okna a čas je zase tvůj.",
        "Dneska žádný spěch. Vyber si jednu drobnou věc, která ti udělá dobře.",
        "Klidný den! Když přijde chuť bezcílně scrollovat, zkus se na chvilku protáhnout.",
    )

    private val sleepyGreetings = listOf(
        "Už je pozdě, i sovy brzy zavírají oči. Dopřej si klidný spánek.",
        "V noci telefon nic dobrého nepřinese. Dobrou noc a zítra naviděnou.",
        "Čas odložit displej a nechat hlavu odpočinout. Sladké sny.",
    )

    private val crisisKeywords = listOf(
        "nechci zit",
        "zabit",
        "sebevra",
        "ublizit si",
        "nema to smysl",
        "depres",
        "uzkost",
    )

    private val fallbackReplies = listOf(
        "Rozumím. Zkus si teď vybrat jednu malou věc mimo displej, třeba sklenici vody.",
        "Díky, že mi to píšeš. I minuta klidu bez feedu ti udělá dobře.",
        "Jsem tu s tebou. Dej si pár pomalých nádechů a vyber si svůj další krok.",
    )

    private const val CRISIS_REPLY =
        "To zní hodně těžce a nemusíš na to být sám. Zkus prosím zavolat na Linku první psychické pomoci 116 123, jsou tam nonstop."

    /** Vybere deterministickou uvítací větu podle kontextu, nálady a indexu dne. */
    fun greeting(ctx: PetContext, mood: PetMood, dayIndex: Int): String {
        val pool = when {
            mood == PetMood.SPI -> sleepyGreetings
            ctx.reclaimedMinutes > ctx.feedMinutes -> wonDayGreetings
            ctx.debtMinutes > 0 -> debtGreetings
            ctx.streakDays >= 3 -> streakGreetings
            else -> generalGreetings
        }
        return pool[floorMod(dayIndex, pool.size)]
    }

    /** Detekuje krizová témata bez ohledu na velikost písmen a diakritiku. */
    fun isCrisis(text: String): Boolean {
        val normalized = normalize(text)
        return crisisKeywords.any { normalized.contains(it) }
    }

    /** Offline odpověď sovy bez volání LLM. */
    fun reply(userText: String, ctx: PetContext): String {
        if (isCrisis(userText)) return CRISIS_REPLY
        val norm = normalize(userText)
        return when {
            norm.contains("nuda") || norm.contains("nudim") || norm.contains("scroll") -> {
                val firstTask = ctx.plannedTasks.firstOrNull { it.isNotBlank() }
                if (firstTask != null) {
                    "Co třeba: $firstTask? Stačí pár minut a hned budeš mít lepší pocit."
                } else {
                    "Zkus se na chvilku protáhnout nebo udělat 10 dřepů. Případně si dojdi pro sklenici vody."
                }
            }
            norm.contains("unav") || norm.contains("spat") || norm.contains("ospal") -> {
                "Když je hlava unavená, scrollování ji jen vyčerpá víc. Odlož telefon a dopřej si spánek."
            }
            norm.contains("jak se mi dari") || norm.contains("jak se dari") || norm.contains("skore") -> {
                "Dnes máš získaných ${ctx.reclaimedMinutes} min oproti ${ctx.feedMinutes} min feedu. Série je ${ctx.streakDays} dní a každé zavření se počítá."
            }
            else -> fallbackReplies[floorMod(userText.length, fallbackReplies.size)]
        }
    }

    /** Ořízne odpověď na max 2 věty a 280 znaků, odstraní okolní uvozovky a přebytečné mezery. */
    fun shorten(text: String): String {
        val cleaned = text
            .trim()
            .trim('"', '\'', '„', '“', '”')
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isEmpty()) return ""

        var sentences = 0
        var cutIndex = cleaned.length
        for (i in cleaned.indices) {
            val ch = cleaned[i]
            if (ch == '.' || ch == '!' || ch == '?') {
                val atEndOrSpace = i == cleaned.lastIndex || cleaned[i + 1].isWhitespace()
                if (atEndOrSpace) {
                    sentences++
                    if (sentences >= 2) {
                        cutIndex = i + 1
                        break
                    }
                }
            }
        }
        val twoSentences = cleaned.substring(0, cutIndex).trim()
        if (twoSentences.length <= 280) return twoSentences

        val slice = twoSentences.substring(0, 279)
        val lastSpace = slice.lastIndexOf(' ')
        val truncated = if (lastSpace > 180) slice.substring(0, lastSpace) else slice
        return truncated.trimEnd('.', ',', ';', ':', ' ') + "…"
    }

    private fun normalize(input: String): String =
        Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()
}
