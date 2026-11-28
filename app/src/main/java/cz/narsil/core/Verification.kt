package cz.narsil.core

/** Poskytovatel LLM pro ověření (SPEC 7); přepínač v nastavení. */
enum class LlmProvider { CLAUDE, GEMINI, OPENAI }

/** Výsledek ověření: YES → VERIFIED, NO → REJECTED, LATER → VERIFY_LATER. */
enum class VerificationResult { YES, NO, LATER }

/** Co fotka ukazuje (541): výsledek úkolu, nebo prostředí (uklizený pokoj, rozložená podložka). */
enum class PhotoKind { TASK, ROOM }

/** Důkaz splnění úkolu. */
sealed interface Proof {
    /**
     * Fotka (content:// nebo file://); ověřovač ji zmenší na 512 px a spočítá otisk.
     * [kind] = co fotka ukazuje; výchozí TASK (zpětně kompatibilní s `Photo(uri)`).
     */
    data class Photo(val uri: String, val kind: PhotoKind = PhotoKind.TASK) : Proof

    /**
     * Vlastní popis splnění, nejvýš [PlanRules.MAX_TEXT_PROOF_CHARS] znaků. Od 541 aspoň 2 věty
     * (~60 znaků); LLM hodnotí konkrétnost a vrací krátkou zpětnou vazbu.
     */
    data class Text(val text: String) : Proof

    /** Ověřit dnešní aktivitou ze Stravy. */
    data object Strava : Proof
}

/**
 * Ověření úkolu (501, Člen 5). Nikdy nehází: chyba, timeout nebo nejasná odpověď = LATER.
 * Bez sítě nikdy YES. Opakovaná fotka (stejný otisk) = NO bez volání LLM.
 */
interface TaskVerifier {
    suspend fun verify(task: PlanTask, proof: Proof): VerificationResult
}
