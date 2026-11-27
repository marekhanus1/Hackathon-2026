package cz.narsil.core

/** Výsledek párování; odpovídá JSON z Claude API `{planned, reason}`. */
data class ContentMatch(
    val planned: Boolean,
    val reason: String = "",
)

/** Je video v plánu? Implementuje 221 (Claude API, timeout 3 s, záloha klíčová slova). */
interface ContentMatcher {
    /** Nikdy nehází; při chybě použije zálohu. */
    suspend fun isPlanned(title: String, plan: List<String>): ContentMatch
}
