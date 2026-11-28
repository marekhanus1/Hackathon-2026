package cz.narsil.app.demo

/**
 * Moment dema (161), který most na notebooku (scripts/demo-most.py) pošle prezentaci.
 * Na řádek logcatu s tagem [DemoBridge.TAG] jde jako jeden JSON objekt: `{"e":"squats","n":12}`.
 * Seznam událostí a jejich polí je i v pitch/snimky/README.md.
 */
sealed class DemoEvent(val name: String) {
    /** Kontext komfortní zóny: `home` | `away`. */
    data class Context(val where: String) : DemoEvent("context")

    /** Ozvěna se ukázala. */
    data object EchoShown : DemoEvent("echo")

    /** Ozvěna zmizela (volba, zavření). */
    data object EchoHidden : DemoEvent("echo_end")

    /** Kamera počítá cvik ([n] od začátku série, cíl [target]). */
    data class Squats(val n: Int, val target: Int = 20) : DemoEvent("squats")

    /** Úkol smlouvy změnil stav (odevzdání / ověření): `PENDING|VERIFIED|REJECTED|VERIFY_LATER`. */
    data class Task(val title: String, val status: String) : DemoEvent("task")

    /** Feed odemčený, [min] minut zbývá. */
    data class Unlocked(val min: Int) : DemoEvent("unlocked")

    /** Nová úroveň. */
    data class Level(val level: Int) : DemoEvent("level")

    /** Přibyly XP ([delta]), celkem [xp]. */
    data class Xp(val delta: Int, val xp: Int) : DemoEvent("xp")

    /** Ohodnocené video / odpověď „Co ti to video dalo?“. */
    data class Rated(val title: String?) : DemoEvent("rated")

    /** Odpověď na příkaz `state`: celý stav najednou, aby se prezentace srovnala i po svém restartu. */
    data class State(val where: String, val level: Int, val xp: Int, val echo: Boolean, val unlockedMin: Int) : DemoEvent("state")

    /** Příkaz z mostu proveden ([ok] = povedl se). */
    data class Ack(val cmd: String, val ok: Boolean) : DemoEvent("ack")

    fun toJson(): String {
        val fields = buildList {
            add("e" to str(name))
            when (this@DemoEvent) {
                is Context -> add("where" to str(where))
                EchoShown, EchoHidden -> Unit
                is Squats -> { add("n" to "$n"); add("target" to "$target") }
                is Task -> { add("title" to str(title)); add("status" to str(status)) }
                is Unlocked -> add("min" to "$min")
                is Level -> add("level" to "$level")
                is Xp -> { add("delta" to "$delta"); add("xp" to "$xp") }
                is Rated -> add("title" to (title?.let(::str) ?: "null"))
                is State -> {
                    add("where" to str(where)); add("level" to "$level"); add("xp" to "$xp")
                    add("echo" to "$echo"); add("min" to "$unlockedMin")
                }
                is Ack -> { add("cmd" to str(cmd)); add("ok" to "$ok") }
            }
        }
        return fields.joinToString(",", "{", "}") { (k, v) -> "\"$k\":$v" }
    }

    private companion object {
        fun str(s: String): String = buildString {
            append('"')
            for (c in s) when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c < ' ' -> append(' ')
                else -> append(c)
            }
            append('"')
        }
    }
}
