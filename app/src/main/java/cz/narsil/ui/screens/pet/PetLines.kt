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

    private val greetingInputs = setOf(
        "ahoj",
        "cau",
        "cus",
        "nazdar",
        "dobre rano",
        "dobry den",
        "dobry vecer",
        "dobrou noc",
    )

    private val greetingReplies = listOf(
        "Ahoj! Jsem tady, kdybys chtěl na chvilku vypnout feed.",
        "Zdravím! Jak se ti dnes daří držet směr?",
        "Ahoj! Stačí jeden malý krok mimo displej a hned je líp.",
        "Ráda tě vidím. Co máš dnes v plánu?",
    )

    private val thanksReplies = listOf(
        "Není vůbec zač. Jsem tu s tebou kdykoli budeš potřebovat.",
        "Rádo se stalo! Držím ti palce do další chvíle klidu.",
        "Kdykoli. Každý malý krok bez feedu se počítá.",
    )

    private val boredFallbackReplies = listOf(
        "Zkus se na chvilku protáhnout nebo udělat 10 dřepů. Případně si dojdi pro sklenici vody.",
        "Dej telefonu na pět minut pauzu a koukni z okna. Často to stačí na vyčištění hlavy.",
        "Co zkusit krátkou procházku po pokoji nebo pustit si jednu oblíbenou písničku bez videa?",
    )

    private val tiredReplies = listOf(
        "Když je hlava unavená, scrollování ji jen vyčerpá víc. Odlož telefon a dopřej si spánek.",
        "Únava si říká o klid, ne o další obrazovku. Zkus na chvíli zavřít oči a vydechnout.",
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

    /** Normalizuje text pro klíčování v cache a lokální porovnání (bez diakritiky, lowercase). */
    fun normalizeKey(text: String): String =
        normalize(text).replace(Regex("\\s+"), " ").trim()

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

    /**
     * Okamžitá lokální odpověď s 0 API voláními pro krizi, krátké pozdravy, poděkování
     * a standardní rychlé volby. Pro otevřené dotazy vrací null (ty mohou jít do Gemini API).
     */
    fun tryLocalReply(userText: String, ctx: PetContext, turnIndex: Int = 0): String? {
        if (isCrisis(userText)) return CRISIS_REPLY
        val norm = normalizeKey(userText)
        if (norm.isEmpty()) return null

        if (norm.length <= 24 && (norm in greetingInputs || norm.startsWith("ahoj ") || norm.startsWith("cau "))) {
            return greetingReplies[floorMod(turnIndex, greetingReplies.size)]
        }
        if (norm.length <= 32 && (norm.contains("diky") || norm.contains("dekuju") || norm.contains("dekuji"))) {
            return thanksReplies[floorMod(turnIndex, thanksReplies.size)]
        }
        if (norm.contains("nuda") || norm.contains("nudim") || norm.contains("scroll")) {
            val tasks = ctx.plannedTasks.filter { it.isNotBlank() }
            if (tasks.isNotEmpty()) {
                val task = tasks[floorMod(turnIndex, tasks.size)]
                return "Co třeba: $task? Stačí pár minut a hned budeš mít lepší pocit."
            }
            return boredFallbackReplies[floorMod(turnIndex, boredFallbackReplies.size)]
        }
        if (norm.contains("unav") || norm.contains("spat") || norm.contains("ospal")) {
            return tiredReplies[floorMod(turnIndex, tiredReplies.size)]
        }
        if (norm.contains("jak se mi dari") || norm.contains("jak se dari") || norm.contains("skore")) {
            return "Dnes máš získaných ${ctx.reclaimedMinutes} min oproti ${ctx.feedMinutes} min feedu. Série je ${ctx.streakDays} dní a každé zavření se počítá."
        }
        return null
    }

    /** Offline odpověď sovy bez volání LLM. */
    fun reply(userText: String, ctx: PetContext, turnIndex: Int = 0): String =
        tryLocalReply(userText, ctx, turnIndex)
            ?: fallbackReplies[floorMod(userText.length + turnIndex, fallbackReplies.size)]

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
