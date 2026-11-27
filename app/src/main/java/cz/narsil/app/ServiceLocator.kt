package cz.narsil.app

import android.content.Context

/**
 * Jednoduchý ServiceLocator: jediné místo, kde se rozhoduje, zda aplikace používá
 * fake implementace (cz.narsil.core.fake) nebo skutečné moduly.
 *
 * Inicializuje ho [NarsilApp.onCreate]. Obrazovky si závislosti berou odsud
 * (např. `ServiceLocator.contextRepository`), nikdy nevytvářejí implementace samy.
 *
 * Přepínač [useFakes]:
 * - výchozí hodnota je [DEFAULT_USE_FAKES] (true, dokud nejsou skutečné moduly hotové),
 * - za běhu ho přepíná demo panel (úkol 123) přes [setUseFakes]; tím se zahodí
 *   už vytvořené instance a při dalším přístupu se vytvoří znovu.
 *
 * Vlastnosti doplní úkol 124 po sloučení rozhraní z úkolu 102 (cz.narsil.core):
 * - `contextRepository`    – ContextRepository (HOME/AWAY, Wi-Fi)
 * - `movementRepository`   – MovementRepository (kroky, Health Connect)
 * - `squatCounter`         – SquatCounter (ML Kit Pose, Šimon)
 * - `gamificationEngine`   – GamificationEngine (XP, úrovně, odznaky)
 * - `contractRepository`   – ContractRepository (Večerní smlouva)
 * - `contentMatcher`       – ContentMatcher (LLM, plánovaný obsah)
 * - `echoVideoRepository`  – EchoVideoRepository (videa pro Ozvěnu)
 *
 * Vzor pro každou vlastnost:
 * ```
 * val contextRepository: ContextRepository
 *     get() = cached("contextRepository") {
 *         if (useFakes) FakeContextRepository() else RealContextRepository(appContext)
 *     }
 * ```
 */
object ServiceLocator {

    /** Výchozí stav přepínače; po napojení skutečných modulů (124) změnit na false. */
    const val DEFAULT_USE_FAKES = true

    private lateinit var appContextRef: Context

    /** Aplikační kontext; dostupný až po [init]. */
    val appContext: Context
        get() = appContextRef

    @Volatile
    var useFakes: Boolean = DEFAULT_USE_FAKES
        private set

    private val instances = mutableMapOf<String, Any>()

    val isInitialized: Boolean
        get() = ::appContextRef.isInitialized

    fun init(context: Context, useFakes: Boolean = DEFAULT_USE_FAKES) {
        appContextRef = context.applicationContext
        this.useFakes = useFakes
    }

    /** Přepne fake/skutečné moduly za běhu a zahodí vytvořené instance. */
    @Synchronized
    fun setUseFakes(value: Boolean) {
        if (useFakes == value) return
        useFakes = value
        instances.clear()
    }

    /** Líně vytvoří a uloží instanci pod klíčem [key]; po [setUseFakes] se vytvoří znovu. */
    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T : Any> cached(key: String, create: () -> T): T =
        instances.getOrPut(key, create) as T
}
