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
| `ContractRepository`, `DailyContract`, `PlanTask`, `TaskType`, `VerificationMethod`, `TaskStatus`, `PlanRules` | `ContractRepository.kt` | Šimon (212) | `FakeContractRepository` |
| `RewardService` | `RewardService.kt` | Šimon (221) | `FakeRewardService` |
| `ContentMatcher`, `ContentMatch` | `ContentMatcher.kt` | Člen 5 (512) | `FakeContentMatcher` |
| `EchoVideoRepository`, `EchoVideo` | `EchoVideoRepository.kt` | Šimon (212) | `FakeEchoVideoRepository` |
| `TaskVerifier`, `Proof`, `VerificationResult`, `LlmProvider` | `Verification.kt` | Člen 5 (501, 520) | `FakeTaskVerifier` |
| `StravaRepository`, `StravaActivity` | `StravaRepository.kt` | Člen 5 (511) | `FakeStravaRepository` |
| `SettingsRepository`, `Settings` | `SettingsRepository.kt` | Marek (105): `data.DataStoreSettingsRepository` | `FakeSettingsRepository` |
| `NarsilDatabase`, entity, DAO, `DayKey`, převody | `core/data/` | Marek (105) | – (Room) |

Pravidla a čísla (den od 4:00, měna, odměny, XP) jsou v `docs/SPEC.md`.

## ContextRepository (112)

Komfortní zóna. Zasahujeme jen při `HOME`; `UNKNOWN` = nezasahovat. Ruční přepínač má přednost před Wi-Fi.

```kotlin
enum class ComfortContext { HOME, AWAY, UNKNOWN }

interface ContextRepository {
    val context: StateFlow<ComfortContext>          // výsledný stav
    val manualOverride: StateFlow<ComfortContext?>  // null = automatika
    suspend fun setManualOverride(context: ComfortContext?) // platí do 4:00
    val currentWifiSsid: StateFlow<String?>          // null = žádná Wi-Fi / chybí poloha (125)
    suspend fun saveCurrentWifiAsHome(): Boolean      // „tohle je můj domov“ → Settings.homeSsid (125)
}
```

Implementace: `cz.narsil.detection.context.WifiContextRepository` (112). Obrazovka nastavení: 325.

Fake: `FakeContextRepository(detected = HOME, wifiSsid = "Doma")`, `setDetected(ctx)` simuluje změnu kontextu, `setWifi(ssid)` jinou Wi-Fi, `savedHome` = uložený domov.

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
    suspend fun addEarnedMinutes(minutes: Int)    // odměna za ověřený úkol (RewardService)
    suspend fun refresh()                         // znovu načíst kroky
}
```

`balanceMinutes` = kroky dnes / kurz + odměny za úkoly − dnes zaplacené minuty − přenesený dluh (SPEC 5). Dřepy se do zůstatku nepřičítají, odemykají hned.
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

Nikdy netrestá, XP se jen přičítají. Přesná čísla: `docs/SPEC.md` 8 a `docs/GAMIFICATION.md` (201).
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
    suspend fun onTaskVerified(task: PlanTask): Int           // +30, reclaimedMinutes += odměna
    suspend fun onVideoRated(rating: Int): Int
}
```

`badges` = id odznaků (seznam definuje 201/220). Denní skóre: `CLOSED` přidá získané minuty, placení a půjčka minuty feedu
(implementace může brát minuty feedu i z Room, 121).

Fake: `FakeGamificationEngine(DEMO_STATE)` (úroveň 3, série 4, 1 žolík), `addXp(500)`, `setAvatar()`, `setState()`.

## ContractRepository (212): smlouva a plán dne

```kotlin
enum class TaskType(val defaultRewardMinutes: Int, val defaultMethod: VerificationMethod) {
    MOVEMENT(30, STRAVA), SCHOOL(20, PHOTO), WORK(20, PHOTO), OTHER(10, PHOTO)
}
enum class VerificationMethod { STRAVA, PHOTO, TEXT }
enum class TaskStatus { PENDING, VERIFIED, REJECTED, VERIFY_LATER }

data class PlanTask(
    val id: Long = 0, val type: TaskType, val title: String, val target: String = "",
    val rewardMinutes: Int = type.defaultRewardMinutes, val method: VerificationMethod = type.defaultMethod,
    val status: TaskStatus = TaskStatus.PENDING, val attempts: Int = 0,
    val proofUri: String? = null, val proofHash: String? = null,      // SHA-256 hex fotky
) { val canRetry: Boolean }                                           // ne VERIFIED a attempts < 3

data class DailyContract(
    val date: LocalDate, val stepGoal: Int,
    val plannedContent: List<String> = emptyList(), // volný text, např. "přednáška o Rustu"
    val videoUri: String? = null,                    // lokální content:// nebo file://
    val tasks: List<PlanTask> = emptyList(),
) {
    val allTasksVerified: Boolean
    fun isFulfilled(stepsToday: Int): Boolean        // kroky ≥ cíl a všechny úkoly VERIFIED
}

interface ContractRepository {
    val today: StateFlow<DailyContract?>       // null = nic nepodepsáno
    suspend fun save(contract: DailyContract)  // i s úkoly; obvykle date = zítřek (DayKey.contractDateFor)
    suspend fun forDate(date: LocalDate): DailyContract?
    suspend fun updateTask(task: PlanTask)     // stav, pokusy, důkaz podle id
}
```

Limity v `PlanRules`: výchozí cíl 6 000 kroků, ≤ 5 položek obsahu, ≤ 6 úkolů, odměna 5–60 min, 3 pokusy, text ≤ 300 znaků, video ≤ 15 s.
`isFulfilled` je funkce, protože kroky smlouva nezná (bere je z `MovementRepository.state`).

Fake: `FakeContractRepository(clock, withDemoContract = true)` – dnes `demoContract()`: 6 000 kroků, „přednáška o Rustu“, „jeden díl seriálu“, 4 úkoly (běh ověřený, ostatní čekají). Úkoly s `id = 0` dostanou id při `save`.

## RewardService (221)

```kotlin
interface RewardService { suspend fun reward(task: PlanTask) }
```

Po `VERIFIED`: `MovementRepository.addEarnedMinutes(task.rewardMinutes)` + `GamificationEngine.onTaskVerified(task)` (+30 XP). Každý úkol jen jednou.

Fake: `FakeRewardService(movement, gamification)`.

## TaskVerifier, Proof, LlmProvider (501, 520)

```kotlin
enum class LlmProvider { CLAUDE, GEMINI, OPENAI }
enum class VerificationResult { YES, NO, LATER }   // → VERIFIED / REJECTED / VERIFY_LATER
sealed interface Proof {
    data class Photo(val uri: String) : Proof
    data class Text(val text: String) : Proof
    data object Strava : Proof
}
interface TaskVerifier { suspend fun verify(task: PlanTask, proof: Proof): VerificationResult }
```

Nikdy nehází: chyba, timeout (5 s) nebo nejasná odpověď = `LATER`. Bez sítě nikdy `YES`. Opakovaná fotka (`ContractDao.isProofHashUsed`) = `NO` bez LLM.
Prompt a parsování: SPEC 7. Stav a pokusy úkolu zapisuje volající (520) přes `ContractRepository.updateTask`, při `YES` zavolá `RewardService`.

Fake: `FakeTaskVerifier(strava, delayMillis, nextResult)` – text ≥ 10 znaků ANO, fotka ANO (stejná URI podruhé NE), Strava podle `FakeStravaRepository`; `nextResult` vnutí výsledek.

## StravaRepository (511)

```kotlin
data class StravaActivity(val id: Long, val type: String, val startMillis: Long,
                          val movingTimeSec: Int, val distanceMeters: Float)
interface StravaRepository {
    val isConnected: StateFlow<Boolean>
    suspend fun todayActivities(): List<StravaActivity>   // od 4:00; chyba = prázdný seznam
}
```

Pohybový úkol splní aktivita typu `StravaRepository.MOVEMENT_TYPES` (Run, Ride, Walk, Workout) s `movingTimeSec ≥ MIN_MOVING_TIME_SEC` (15 min).

Fake: `FakeStravaRepository(connected = true)` – ranní běh 5,2 km / 28 min, `setConnected(false)`.

## SettingsRepository (105)

```kotlin
data class Settings(
    val homeSsid: String? = null, val llmProvider: LlmProvider = LlmProvider.CLAUDE,
    val demoMode: Boolean = false, val demoThresholdSec: Int? = null,
    val demoContractToday: Boolean = false, val demoAlwaysYes: Boolean = false,
)
interface SettingsRepository { val settings: StateFlow<Settings>; suspend fun update(change: (Settings) -> Settings) }
```

Skutečná implementace: `cz.narsil.core.data.DataStoreSettingsRepository(context)` (DataStore „settings“). Fake: `FakeSettingsRepository`.

## Sdílená databáze `NarsilDatabase` (105)

`cz.narsil.core.data`, instance `NarsilDatabase.get(context)`, verze 1, `fallbackToDestructiveMigration()`, schéma v `app/schemas/`.
Enumy jako text (`name`), datum jako `LocalDate.toString()`. Entity a DAO mění jen Marek – chybí dotaz → Poznámky v tasku.

| Entita (tabulka) | Sloupce | DAO | Používá |
| --- | --- | --- | --- |
| `ContractEntity` (`contracts`) | `date` PK, `stepGoal`, `plannedContent` (JSON), `videoUri`, `createdAtMillis` | `ContractDao` | 212 |
| `PlanTaskEntity` (`plan_tasks`) | `id` PK auto, `date`, `type`, `title`, `target`, `rewardMinutes`, `method`, `status`, `attempts`, `proofUri`, `proofHash`, `verifiedAtMillis` | `ContractDao` | 212, 520 |
| `ConsumptionEventEntity` (`consumption_events`) | `id`, `packageName`, `surface`, `startedAtMillis`, `durationMillis`, `videoTitle`, `url`, `planned`, `context` | `ConsumptionDao` | 121 |
| `InterventionEntity` (`interventions`) | `id`, `atMillis`, `packageName`, `reason`, `feeling`, `outcome`, `xpGained` | `InterventionDao` | 114, 121 |
| `LedgerEntryEntity` (`ledger`) | `id`, `date`, `atMillis`, `kind` (`LedgerKind`: TASK_REWARD, PAID, BORROW, DEBT_CARRY), `minutes` | `LedgerDao` | 203, 211 |
| `XpEventEntity` (`xp_events`) | `id`, `date`, `atMillis`, `source`, `xp` | `XpDao` | 202, 220 |
| `BadgeEntity` (`badges`) | `id` PK, `unlockedAtMillis` | `BadgeDao` | 220 |
| `VideoRatingEntity` (`video_ratings`) | `id`, `atMillis`, `title`, `intent`, `rating` | `VideoRatingDao` | 120, 321 |

Důležité dotazy: `ContractDao.replaceContract(contract, tasks)`, `observeContract/observeTasks(date)`, `latestWithVideoBefore(date)`, `tasksToVerifyLater()`, `isProofHashUsed(hash, exceptTaskId)`;
`ConsumptionDao.observeFeedMinutesSince(millis)`; `InterventionDao.lastWithOutcome/countWithOutcomeSince`; `LedgerDao.sumForDate(date, kind)`; `XpDao.observeTotal()`, `countForDate(date, source)`; `BadgeDao.insert` (IGNORE); `VideoRatingDao.countSince`.

Pomocníci: `DayKey` (den od 4:00: `of(millis)`, `today()`, `startMillis(date)`, `contractDateFor(millis, contractToday)`),
`Mappers.kt` (`ContractEntity.toDomain(tasks)`, `DailyContract.toEntity()`, `PlanTask.toEntity(date)`, `PlanTaskEntity.toDomain()`, `enumOr`).

## ContentMatcher (512)

```kotlin
data class ContentMatch(val planned: Boolean, val reason: String = "") // = JSON {planned, reason}
interface ContentMatcher { suspend fun isPlanned(title: String, plan: List<String>): ContentMatch }
```

Nikdy nehází: LLM s timeoutem 3 s, pak záloha přes klíčová slova. Implementuje Člen 5 (512). Do LLM jde jen název videa a plán.

Fake: `FakeContentMatcher` – shoda, když název obsahuje slovo (≥ 4 znaky) z položky plánu.

## EchoVideoRepository (212)

```kotlin
data class EchoVideo(val uri: String, val recordedOn: LocalDate)
interface EchoVideoRepository { suspend fun pickEchoVideo(): EchoVideo? } // null = jen otázka bez videa
```

Nejnovější video ze smlouvy staršího dne (`ContractDao.latestWithVideoBefore`), jinak video z onboardingu (SPEC 9). URI přehraje ExoPlayer v overlay (113).

Fake: `FakeEchoVideoRepository(video)`, výchozí `android.resource://cz.narsil/raw/echo_demo` (soubor zatím neexistuje, dodá 4xx/302).
