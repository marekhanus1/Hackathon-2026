package cz.narsil.verify.demo

import cz.narsil.core.Proof

/**
 * Připravené důkazy pro demo (522) k úkolům z demo smlouvy (`FakeContractRepository.demoContract`).
 * Stejné texty jsou v `mockdata/dukazy/texty.json` (pro Štěpána a scénář 403). Fotky se fotí na místě
 * podle `mockdata/dukazy/README.md`.
 */
object DemoProofs {
    data class Example(val taskTitle: String, val target: String, val proof: Proof, val expected: Boolean, val note: String)

    val TEXT_EXAMPLES = listOf(
        Example(
            "Dopsat report", "",
            Proof.Text("Report o měření kroků je dopsaný a odeslaný vedoucímu v 16:40, má 4 strany a závěr s doporučeními."),
            expected = true, note = "konkrétní, věrohodné",
        ),
        Example(
            "Dopsat report", "",
            Proof.Text("jo udělal"),
            expected = false, note = "nic konkrétního",
        ),
        Example(
            "Lineární algebra", "kapitola 3, příklady 1–10",
            Proof.Text("Spočítal jsem příklady 1–10 z kapitoly 3, u 7 vyšel determinant −12, u 9 jsem musel použít Gaussovu eliminaci."),
            expected = true, note = "odpovídá cíli",
        ),
        Example(
            "Lineární algebra", "kapitola 3, příklady 1–10",
            Proof.Text("Dneska jsem koukal na seriál a uklízel."),
            expected = false, note = "jiný úkol",
        ),
    )
}
