package cz.narsil.verify.demo

import cz.narsil.core.PhotoKind
import cz.narsil.core.Proof
import cz.narsil.verify.VerifyMode
import cz.narsil.verify.VerifyRules

/**
 * Připravené důkazy pro demo (522) k úkolům z demo smlouvy (`FakeContractRepository.demoContract`).
 * Stejné texty jsou v `mockdata/dukazy/texty.json` (pro Štěpána a scénář 403). Fotky se fotí na místě
 * podle `mockdata/dukazy/README.md`.
 *
 * Od 541 tři režimy: fotka úkolu, fotka prostředí ([ROOM_EXAMPLES]) a vlastní popis ([TEXT_EXAMPLES],
 * aspoň 2 věty). Odpovědi u [TEXT_EXAMPLES] a [ROOM_EXAMPLES] jsou skutečné odpovědi gemini-3.5-flash-lite
 * (28. 11. 2026) – hodí se do scénáře i jako ukázka zpětné vazby.
 */
object DemoProofs {
    data class Example(
        val taskTitle: String,
        val target: String,
        val proof: Proof,
        val expected: Boolean,
        val note: String,
        /** Ukázková odpověď LLM (zpětná vazba), prázdná = nezměřeno. */
        val sampleAnswer: String = "",
    )

    /** Zpětná vazba v demu „vždy ANO“ podle režimu – ať je vidět, jak vypadá skutečná odpověď. */
    fun feedbackFor(proof: Proof): String = when (VerifyRules.mode(proof)) {
        VerifyMode.TASK_PHOTO -> "Na fotce je vidět hotová práce na úkolu."
        VerifyMode.ROOM_PHOTO -> "Prostředí na fotce odpovídá splněnému úkolu."
        VerifyMode.TEXT -> "Popis je konkrétní – víme, co přesně jsi udělal a jak to dopadlo."
        null -> ""
    }

    val TEXT_EXAMPLES = listOf(
        Example(
            "Dopsat report", "",
            Proof.Text("Report o měření kroků je dopsaný a odeslaný vedoucímu v 16:40. Má 4 strany a závěr s doporučeními."),
            expected = true, note = "konkrétní, věrohodné",
        ),
        Example(
            "Dopsat report", "",
            Proof.Text("jo udělal"),
            expected = false, note = "nic konkrétního",
        ),
        Example(
            "Lineární algebra", "kapitola 3, příklady 1–10",
            Proof.Text("Spočítal jsem příklady 1–10 z kapitoly 3. U sedmičky vyšel determinant −12 a u devítky jsem musel použít Gaussovu eliminaci. Dvojku jsem si ověřil dosazením."),
            expected = true, note = "odpovídá cíli",
            sampleAnswer = "ANO – Skvěle jsi zmínil konkrétní výsledky a postupy u příkladů 2, 7 a 9.",
        ),
        Example(
            "Lineární algebra", "kapitola 3, příklady 1–10",
            Proof.Text("Dneska jsem dělal lineární algebru. Bylo to docela těžké, ale zvládl jsem to. Jsem rád."),
            expected = false, note = "obecné, nic konkrétního",
            sampleAnswer = "NE – Napiš konkrétně, které příklady z kapitoly 3 jsi vyřešil a jaký ti vyšel výsledek.",
        ),
        Example(
            "Lineární algebra", "kapitola 3, příklady 1–10",
            Proof.Text("Dneska jsem koukal na seriál a uklízel. Na příklady nezbyl čas."),
            expected = false, note = "jiný úkol",
        ),
    )

    /** Fotka prostředí: fotí se na místě (podložka na zemi, uklizený / neuklizený pokoj). */
    val ROOM_EXAMPLES = listOf(
        Example(
            "Cvičení doma", "20 minut jógy", Proof.Photo("demo://podlozka", PhotoKind.ROOM),
            expected = true, note = "rozložená podložka a pomůcky",
            sampleAnswer = "ANO – Na fotce je správně rozložená podložka a pomůcky na cvičení.",
        ),
        Example(
            "Uklidit pokoj", "", Proof.Photo("demo://neporadek", PhotoKind.ROOM),
            expected = false, note = "nepořádek",
            sampleAnswer = "NE – Na fotce je rozházený nepořádek, ne uklizený pokoj.",
        ),
        Example(
            "Uklidit pokoj", "", Proof.Photo("demo://televize", PhotoKind.ROOM),
            expected = false, note = "jen obrazovka se seriálem",
            sampleAnswer = "NE – Na fotce je obrazovka se seriálem, ne uklizený pokoj.",
        ),
    )
}
