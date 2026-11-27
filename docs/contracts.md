# Sdílená rozhraní

API kontrakty, datové modely, schéma DB. Zapiš sem **před** implementací, ať můžou ostatní stavět paralelně.
Mění jen Marek (AGENTS.md). Potřebuješ změnu? Zapiš ji do Poznámek svého tasku.

Kód: `app/src/main/java/cz/narsil/core/`. Ke každému rozhraní je fake v `cz.narsil.core.fake`
(demo data + metody pro ruční změnu stavu – pro UI náhledy, demo panel a práci, než je hotový skutečný modul).

Společné zásady:

- Stav = `StateFlow` (UI ho čte ve ViewModelu), akce = `suspend fun`.
- Rozhraní jsou čistý Kotlin bez Android tříd (výjimka: `LifecycleOwner` u `SquatCounter`). Video = URI jako `String`, datum = `java.time.LocalDate`.
- Implementace nehází výjimky do UI; při chybě vrací rozumnou výchozí hodnotu (null, false, záloha).

| Rozhraní / model | Soubor | Implementuje | Fake |
| --- | --- | --- | --- |
| `ContextRepository`, `ComfortContext` | `ContextRepository.kt` | Marek (112) | `FakeContextRepository` |
| `ConsumptionEvent`, `Surface` | `Consumption.kt` | Marek (111, 120) | – (data) |
| `InterventionTrigger`, `TriggerReason`, `InterventionTriggerSource` | `Intervention.kt` | Marek (111) | `FakeInterventionTriggerSource` |
| `InterventionOutcome` | `Intervention.kt` | Marek (114) | – (enum) |
| `MovementRepository`, `MovementState`, `MovementRates` | `MovementRepository.kt` | Šimon (203, 211) | `FakeMovementRepository` |
| `SquatCounter` | `SquatCounter.kt` | Šimon (210) | `FakeSquatCounter` |
| `GamificationEngine`, `GamificationState`, `DailyScore`, `AvatarState` | `GamificationEngine.kt` | Šimon (202, 220) | `FakeGamificationEngine` |
| `ContractRepository`, `DailyContract` | `ContractRepository.kt` | Šimon (212) | `FakeContractRepository` |
| `ContentMatcher`, `ContentMatch` | `ContentMatcher.kt` | Šimon (221) | `FakeContentMatcher` |
| `EchoVideoRepository`, `EchoVideo` | `EchoVideoRepository.kt` | Šimon (212) | `FakeEchoVideoRepository` |

## ContextRepository (112)

Komfortní zóna. Zasahujeme jen při `HOME`; `UNKNOWN` = nezasahovat. Ruční přepínač má přednost před Wi-Fi.

```kotlin
enum class ComfortContext { HOME, AWAY, UNKNOWN }

interface ContextRepository {
    val context: StateFlow<ComfortContext>          // výsledný stav
    val manualOverride: StateFlow<ComfortContext?>  // null = automatika
    suspend fun setManualOverride(context: ComfortContext?)
}
```

Fake: `FakeContextRepository(detected = HOME)`, `setDetected(ctx)` simuluje změnu Wi-Fi.

## ConsumptionEvent (111, 120)

Úsek spotřeby z detekce; ukládá se do Room (121).

```kotlin
enum class Surface { FEED, REELS, SHORTS, MESSAGES, VIDEO; val isEndless: Boolean } // true pro FEED/REELS/SHORTS
data class ConsumptionEvent(
    val packageName: String, val surface: Surface,
    val startedAtMillis: Long, val durationMillis: Long,
    val videoTitle: String? = null, val url: String? = null,
)
```

`MESSAGES` nikdy nespouští zásah a nepočítá se do rozpočtu.

## InterventionTrigger (111) a InterventionOutcome (114)

```kotlin
enum class TriggerReason { ENDLESS_SCROLL, UNPLANNED_VIDEO_CHAIN, DEMO }
data class InterventionTrigger(val event: ConsumptionEvent, val reason: TriggerReason, val atMillis: Long)

interface InterventionTriggerSource { val triggers: Flow<InterventionTrigger> } // hot, bez replay

enum class InterventionOutcome { CLOSED, PAID_STEPS, SQUATS, BORROWED, PLANNED_FREE }
```

Tok: detekce emituje `InterventionTrigger` → overlay ukáže Ozvěnu → volba se převede na `InterventionOutcome`
→ `MovementRepository` (`pay`/`borrow`/`recordSquats`) + `GamificationEngine.onOutcome()`.
`PLANNED_FREE` = plánovaný obsah, Ozvěna se neukázala (jen pro statistiky).

Fake: `FakeInterventionTriggerSource.fire(trigger = demoTrigger())` – TikTok, 6 min ve feedu.

## MovementRepository (203, 211)

Pohyb jako měna. Kurz v `MovementRates`: `STEPS_PER_MINUTE = 100` (1 000 kroků = 10 min),
`SQUATS_PER_UNLOCK = 20`, `MINUTES_PER_UNLOCK = 10`, `DEFAULT_DEBT_LIMIT_MINUTES = 30`.

```kotlin
data class MovementState(
    val stepsToday: Int = 0, val balanceMinutes: Int = 0,
    val debtMinutes: Int = 0, val debtLimitMinutes: Int = 30,
) { val isOverDebtLimit: Boolean }

interface MovementRepository {
    val state: StateFlow<MovementState>
    suspend fun pay(minutes: Int = 10): Boolean   // false = zůstatek nestačí, nic se nestrhne
    suspend fun borrow(minutes: Int = 10)         // vždy projde, zvýší dluh
    suspend fun recordSquats(count: Int): Int     // vrací získané minuty podle kurzu
    suspend fun refresh()                         // znovu načíst kroky
}
```

`balanceMinutes` = kroky dnes / kurz − dnes zaplacené minuty. Dřepy se do zůstatku nepřičítají, odemykají hned.
Splácení dluhu a přenos do zítřka řeší 203 uvnitř implementace.

Fake: `FakeMovementRepository(stepsToday = 4_200)`, `setSteps()`, `addSteps(1_000)`, `setDebt()`.

## SquatCounter (210)

```kotlin
interface SquatCounter {
    val count: StateFlow<Int>          // dřepy od posledního start()
    val isRunning: StateFlow<Boolean>
    fun start(lifecycleOwner: LifecycleOwner, previewSurfaceProvider: Any? = null) // vynuluje count
    fun stop()
}
```

**Rozhodnutí – náhled kamery:** core nezávisí na CameraX. UI si vytvoří `PreviewView` (v `AndroidView`)
a předá `previewView.surfaceProvider` jako `Any?`; implementace (210) ho přetypuje na
`androidx.camera.core.Preview.SurfaceProvider` a naváže Preview + ImageAnalysis na `lifecycleOwner`.
`null` = počítat bez náhledu. Důvod: UI vlastní layout náhledu, `SquatCounter` vlastní kameru a ML Kit,
fake i unit testy se bez CameraX obejdou. `LifecycleOwner` je z `androidx.lifecycle` (ne z CameraX) a CameraX ho stejně potřebuje.
Cíl (20) bere volající z `MovementRates.SQUATS_PER_UNLOCK`. `start`/`stop` volat na hlavním vlákně.

Fake: `FakeSquatCounter(scope, intervalMillis = 1_200, target = 20)` – se `scope` přidává dřepy sám, bez něj ručně `addSquat()`; `start()` bez parametrů pro náhledy a testy.

## GamificationEngine (202, 220)

Nikdy netrestá, XP se jen přičítají. Přesná čísla: `docs/GAMIFICATION.md` (201).
Výchozí návrh: zavřít 50, dřepy 20, kroky 10, půjčka 0, smlouva +100, hodnocení +5, úroveň každých 500 XP.

```kotlin
enum class AvatarState { EXHAUSTED, TIRED, OK, FIT, THRIVING }
data class DailyScore(val reclaimedMinutes: Int = 0, val feedMinutes: Int = 0) { val isWon: Boolean }
data class GamificationState(
    val xp: Int = 0, val level: Int = 1, val xpInLevel: Int = 0, val xpPerLevel: Int = 500,
    val streakDays: Int = 0, val jokers: Int = 0, val avatar: AvatarState = AvatarState.OK,
    val today: DailyScore = DailyScore(), val wonDays: Int = 0, val badges: List<String> = emptyList(),
)

interface GamificationEngine {
    val state: StateFlow<GamificationState>
    suspend fun onOutcome(outcome: InterventionOutcome): Int  // vrací přidané XP
    suspend fun onContractFulfilled(): Int
    suspend fun onVideoRated(rating: Int): Int
}
```

`badges` = id odznaků (seznam definuje 201/220). Denní skóre: `CLOSED` přidá získané minuty, placení a půjčka minuty feedu
(implementace může brát minuty feedu i z Room, 121).

Fake: `FakeGamificationEngine(DEMO_STATE)` (úroveň 3, série 4, 1 žolík), `addXp(500)`, `setAvatar()`, `setState()`.

## ContractRepository (212)

```kotlin
data class DailyContract(
    val date: LocalDate, val stepGoal: Int,
    val plannedContent: List<String> = emptyList(), // volný text, např. "přednáška o Rustu"
    val videoUri: String? = null,                    // lokální content:// nebo file://
)

interface ContractRepository {
    val today: StateFlow<DailyContract?>       // null = včera nic nepodepsáno
    suspend fun save(contract: DailyContract)  // obvykle date = zítřek
    suspend fun forDate(date: LocalDate): DailyContract?
}
```

Fake: `FakeContractRepository(clock, withDemoContract = true)` – dnes 6 000 kroků, „přednáška o Rustu“, „jeden díl seriálu“.

## ContentMatcher (221)

```kotlin
data class ContentMatch(val planned: Boolean, val reason: String = "") // = JSON {planned, reason}
interface ContentMatcher { suspend fun isPlanned(title: String, plan: List<String>): ContentMatch }
```

Nikdy nehází: Claude API s timeoutem 3 s, pak záloha přes klíčová slova. Do LLM jde jen název videa a plán.

Fake: `FakeContentMatcher` – shoda, když název obsahuje slovo (≥ 4 znaky) z položky plánu.

## EchoVideoRepository (212)

```kotlin
data class EchoVideo(val uri: String, val recordedOn: LocalDate)
interface EchoVideoRepository { suspend fun pickEchoVideo(): EchoVideo? } // null = jen otázka bez videa
```

Typicky včerejší video ze smlouvy, jinak nejnovější. URI přehraje ExoPlayer v overlay (113).

Fake: `FakeEchoVideoRepository(video)`, výchozí `android.resource://cz.narsil/raw/echo_demo` (soubor zatím neexistuje, dodá 4xx/302).
