package cz.narsil.core.fake

import cz.narsil.core.ContentMatch
import cz.narsil.core.ContentMatcher

/** Fake: shoda, když název obsahuje některé slovo (≥ 4 znaky, bez poslední hlásky) z položky plánu. Bez sítě. */
class FakeContentMatcher : ContentMatcher {
    override suspend fun isPlanned(title: String, plan: List<String>): ContentMatch {
        val t = title.lowercase()
        for (item in plan) {
            // dropLast(1): hrubé skloňování („Rustu“ → „rust“)
            val word = item.lowercase().split(NON_LETTER).firstOrNull { it.length >= 4 && it.dropLast(1) in t }
            if (word != null) return ContentMatch(true, "„$word“ z plánu „$item“")
        }
        return ContentMatch(false, "Není v plánu")
    }

    private companion object {
        val NON_LETTER = Regex("[^\\p{L}\\p{N}]+")
    }
}
