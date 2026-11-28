package cz.narsil.llm.matcher

import cz.narsil.core.ContentMatch
import java.text.Normalizer

/**
 * Záloha párování bez sítě (512): shoda, když kmen slova z plánu (≥ 4 znaky, bez koncovky)
 * začíná některé slovo názvu. Bez diakritiky a velikosti písmen, s hrubým českým skloňováním
 * („přednáška o Rustu“ ↔ „Rust přednášky 2026“). Krátká a výplňová slova se ignorují.
 */
object KeywordMatcher {
    private val STOP = setOf(
        "jeden", "jedna", "jedno", "dalsi", "nejaky", "nejaka", "video", "videa", "dil", "dilu", "serial", "serialu",
        "the", "and", "with", "from", "this", "that", "part", "full", "official",
    )

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    fun words(text: String): List<String> = normalize(text).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }

    /** Kmen: u dlouhých slov odříznout 2 znaky koncovky, nejméně 4 znaky. */
    fun stem(word: String): String = if (word.length <= 4) word else word.take(maxOf(4, word.length - 2))

    fun match(title: String, plan: List<String>): ContentMatch {
        val titleWords = words(title)
        for (item in plan) {
            val hit = words(item)
                .filter { it.length >= 4 && it !in STOP }
                .firstOrNull { w ->
                    val s = stem(w)
                    titleWords.any { it.startsWith(s) }
                }
            if (hit != null) return ContentMatch(true, "klíčové slovo „$hit“ z plánu „$item“")
        }
        return ContentMatch(false, "není v plánu (klíčová slova)")
    }
}
