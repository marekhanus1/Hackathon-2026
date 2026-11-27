package cz.narsil.core.fake

import cz.narsil.core.ContentMatch
import cz.narsil.core.ContentMatcher

/** Fake: shoda, když název obsahuje některé slovo (≥ 4 znaky) z položky plánu. Bez sítě. */
class FakeContentMatcher : ContentMatcher {
    override suspend fun isPlanned(title: String, plan: List<String>): ContentMatch {
        val t = title.lowercase()
        for (item in plan) {
            val word = item.lowercase().split(Regex("\\W+")).firstOrNull { it.length >= 4 && it in t }
            if (word != null) return ContentMatch(true, "„$word“ z plánu „$item“")
        }
        return ContentMatch(false, "Není v plánu")
    }
}
