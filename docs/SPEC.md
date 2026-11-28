# Narsil – pravidla chování (SPEC)

Zdroj pravdy pro pravidla a čísla. Rozhraní a kód: `docs/contracts.md` a `cz.narsil.core`.
Mění Marek po dohodě s týmem, každou změnu zapsat jedním řádkem do `docs/decisions.md`.
Názvy typů odpovídají kódu v `cz.narsil.core` (PR #8); co je nové, je v sekci 11 a implementuje úkol 105.

## 1. Den a čas

- Den začíná ve **4:00** místního času. `dayKey` = `LocalDate` z času posunutého o −4 h.
- Večerní smlouva uložená mezi 17:00 a 3:59 platí pro **následující** den, jinak pro dnešní (jde ji vyplnit i ráno).
- Demo režim: přepínač „smlouva platí hned“ uloží plán na dnešní den.

## 2. Komfortní zóna (`ComfortContext`)

- `HOME`: připojeno k Wi-Fi uložené jako domov, nebo ruční přepínač „jsem doma“ (platí do konce dne).
- `AWAY`: jiná nebo žádná Wi-Fi a přepínač vypnutý.
- `UNKNOWN`: chybí oprávnění (poloha, Wi-Fi). Chová se jako `AWAY`, tedy nezasahovat.

## 3. Detekce a zásah

Povrchy (`Surface`): `FEED`, `REELS`, `SHORTS` jsou nekonečný obsah, `VIDEO` je jedno video, `MESSAGES` zprávy, `SEARCH` výsledky vyhledávání (nepočítají se a relaci nezačnou, 148). V nejistotě `MESSAGES`.

- **Relace** = souvislý čas na nekonečném povrchu. Přepnutí mezi Instagramem a TikTokem relaci nepřeruší. Pauza delší než 2 min mimo nekonečný povrch relaci ukončí.
- **Tolerance:** zásah (`ENDLESS_SCROLL`), když relace trvá ≥ **5 min** (demo 20 s) a kontext je `HOME`.
- **Řetězení videí:** zásah (`UNPLANNED_VIDEO_CHAIN`) u **3.** neplánovaného videa po sobě. Pauza > 10 min řetěz nuluje. Plánované video (`ContentMatcher`) se nepočítá a zásah nespustí (`PLANNED_FREE` jen do statistik).
- Během zaplaceného času se nezasahuje. Po jeho vypršení přijde zásah hned, bez nové tolerance, pokud relace pokračuje.
- Po volbě „Zavřít“ začíná nová relace od nuly. Návrat na nekonečný povrch do **10 min** po „Zavřít“ ale zasáhne hned, bez nové tolerance (155): „Zavřít“ není 5 minut zdarma.
- `MESSAGES`, `AWAY` a `UNKNOWN` nikdy nezasahují a nepočítají se do minut feedu.

## 4. Volby v Ozvěně (`InterventionOutcome`)

| Volba | Efekt | XP |
| --- | --- | --- |
| `CLOSED` | Cílová aplikace se pošle do pozadí (návrat na plochu) | 50 až po potvrzení: 10 min bez dalšího zásahu (155); nejvýš 5× denně a s odstupem ≥ 10 min, jinak 0 |
| `PAID_STEPS` | Strhne 10 min ze zůstatku; když nestačí, tlačítko je neaktivní | 10 |
| `SQUATS` | 20 dřepů odemkne 10 min, do zůstatku se nepřičítají | 20 |
| `BORROWED` | Odemkne 10 min, dluh +10; nad limitem dluhu nejde | 0 |
| `PLANNED_FREE` | Není volba, plánovaný obsah prošel bez Ozvěny | 0 |

Před volbou je otázka „Čemu se teď vyhýbáš?“ (nuda, únava, úkol, samota, jiné). Odpověď je nepovinná a ukládá se k zásahu.

## 5. Měna: minuty neplánovaného obsahu

```
zůstatek = floor(kroky_dnes / 100) + Σ odměny ověřených úkolů dnes − dnes zaplacené minuty − přenesený dluh
```

- Kurz z `MovementRates`: 1 000 kroků = 10 min, 20 dřepů = 10 min hned.
- Kroky se počítají za celý den od 4:00, i ty ušlé před zásahem.
- Zůstatek nikdy neklesne pod 0 platbou (`pay` vrátí `false`).
- **Dluh:** limit 30 min. Na konci dne se dluh odečte od zůstatku dalšího dne (přenesený dluh) a vynuluje. Nad limit nejde půjčit a Parťák dostane zprávu (v MVP ukázková notifikace).

## 6. Plán dne (večerní smlouva)

Smlouva = krokový cíl (výchozí 6 000) + plánovaný obsah (nejvýš 5 položek textem) + úkoly (nejvýš 6) + večerní video (≤ 15 s).

| Typ úkolu (`TaskType`) | Výchozí ověření | Odměna (min, rozsah 5–60) |
| --- | --- | --- |
| `MOVEMENT` | `STRAVA`, záloha `PHOTO` | 30 |
| `SCHOOL` | `PHOTO` nebo `TEXT` | 20 |
| `WORK` | `PHOTO` nebo `TEXT` | 20 |
| `OTHER` | `PHOTO` nebo `TEXT` | 10 |

- Stavy úkolu: `PENDING` → `VERIFIED` / `REJECTED` / `VERIFY_LATER`.
- `REJECTED` jde zkusit znovu nejvýš 2× (celkem 3 pokusy), pak zůstane `REJECTED`.
- Krokový cíl se ověřuje automaticky (kroky ≥ cíl), odměnu nemá, kroky už jsou v měně.
- **Smlouva splněna** = krokový cíl splněn a všechny úkoly `VERIFIED` → +100 XP a den se počítá do série.

## 7. Ověření úkolů

**LLM (PHOTO, TEXT):**

- Důkaz: fotka zmenšená na delší stranu 512 px (JPEG, kvalita 70), nebo text ≤ 300 znaků.
- Prompt:

  ```
  Jsi kontrolor splněných úkolů. Úkol: "{název}" ({cíl}).
  Rozhodni, zda přiložený důkaz věrohodně ukazuje, že úkol byl splněn.
  Odpověz jediným slovem: ANO nebo NE.
  ```

- Parametry: teplota 0, výstup nejvýš ~5 tokenů, timeout 5 s, nejmenší rychlý model poskytovatele.
- Poskytovatel (`LlmProvider`): `GEMINI` (výchozí, bezplatná úroveň API, model `gemini-3.5-flash-lite`, `thinkingLevel: minimal`), `CLAUDE`, `OPENAI`, přepínač v nastavení (zatím demo panel).
- Parsování: odpověď oříznout, převést na velká písmena a bez diakritiky. Začíná `ANO` → `VERIFIED`, `NE` → `REJECTED`. Cokoli jiného, chyba nebo timeout → `VERIFY_LATER`: znovu při další síti, nejvýš 3 pokusy, pak tlačítko „ověřit znovu“.
- **Opakovaná fotka:** SHA-256 otisk souboru. Už použitý otisk (u jakéhokoli úkolu) → `REJECTED` bez volání LLM.
- Bez sítě nikdy automaticky neschvalovat.

**Režimy důkazu (541):** fotka úkolu, fotka prostředí (uklizený pokoj, rozložená podložka – LLM porovná
s popisem úkolu) a vlastní popis (2–3 věty, aspoň 60 znaků). Prompt rozšiřuje text výše o režim a o
odpověď na jednom řádku `ANO – věta` / `NE – věta` (věta = zpětná vazba česky, např. co popsat
konkrétněji); parsování prvního slova zůstává. Popis se nejdřív kontroluje místně (prázdný, krátký,
opakovaný, opsané zadání, stejný text u jiného úkolu → `REJECTED` bez LLM se zpětnou vazbou).
Výstup nejvýš 96 tokenů. Chyba sítě nebo modelu se v UI ukazuje česky srozumitelně (ne surová chyba).

**Strava (MOVEMENT):** úkol je ověřený, když dnes od 4:00 existuje aktivita typu běh, kolo, chůze nebo trénink s pohybovým časem ≥ 15 min. Když Strava není připojená nebo aktivita chybí, uživatel může dát snímek aktivity (`PHOTO`).

**Odměna:** `VERIFIED` → `RewardService`: + odměna minut do zůstatku a +30 XP.

## 8. Gamifikace

| Zdroj | XP |
| --- | --- |
| `CLOSED` | 50 po potvrzení (záznam `CLOSE_CONFIRMED`, strop viz sekce 4) |
| `SQUATS` | 20 |
| `PAID_STEPS` | 10 |
| `BORROWED` | 0 |
| Ověřený úkol | 30 |
| Smlouva splněna | 100 |
| Ohodnocené video | 5, nejvýš 10× denně |
| Boss přežit | 150 |

- Úroveň = 1 + floor(XP / 500).
- **Série** = dny po sobě se splněnou smlouvou. Den bez splnění spotřebuje žolík, když je, jinak série = 0.
- **Žolík:** za 50 dřepů navíc během dne (nad rámec odemykání), nejvýš 3 v zásobě.
- **Postava (`AvatarState`)** podle počtu dní se splněnou smlouvou za posledních 7: 0–1 `EXHAUSTED`, 2 `TIRED`, 3–4 `OK`, 5–6 `FIT`, 7 `THRIVING`.
- **Potvrzené zavření (155):** `CLOSED` se zapíše s 0 XP. Když do 10 min nepřijde další zásah (návrat do feedu = Ozvěna hned = zásah), připíše se při nejbližším přepočtu (start aplikace, návrat do ní, další volba) záznam `CLOSE_CONFIRMED` s XP. Staré `CLOSED` s XP > 0 (mock data) platí jako potvrzené. Odznaky `FIRST_CLOSE`/`CLOSE_10`, souboj a boss počítají jen potvrzená zavření.
- **Souboj s algoritmem (`DailyScore`):** `feedMinutes` = minuty neplánovaného nekonečného obsahu doma. `reclaimedMinutes` = počet potvrzených `CLOSED` × 15 + odměny ověřených úkolů. Den vyhrává vyšší číslo (15 min je odhad ušetřené relace, vyladí 230).
- **Boss:** kritická hodina pevně 21:00–22:00 (MVP). Když v ní není zásah, nebo každý skončí potvrzeným `CLOSED`, +150 XP.
- **Odznaky (MVP):** `FIRST_CLOSE`, `CLOSE_10`, `NO_DEBT_WEEK`, `SQUATS_1000`, `PLAN_STREAK_3`.
- Nic nikdy XP nebere.

## 9. Ozvěna: co se ukáže

1. Nejnovější večerní video ze smlouvy staršího dne.
2. Jinak video z onboardingu (nahrává se při prvním spuštění).
3. Jinak text: „Dnes jsi chtěl: {první úkol dnešního plánu}“. `EchoVideoRepository` vrací `null` a text složí UI ze smlouvy.

Popisek pod videem: věta z odpovědí „Co ti to video dalo?“ (9a), jinak „Dnes jsi chtěl: …“, jinak délka relace.

## 9a. Co ti to video dalo? (156)

- **Kdy:** doma, po dosledovaném videu na YouTube (aplikace i Firefox): konec videa po ≥ 1 min sledování,
  nebo odchod z videa po ≥ 2 min. A po vypršení zaplaceného času feedu („Co ti daly ty minuty?“), když
  zrovna neběží další Ozvěna. Nejvýš 10× denně a s odstupem ≥ 3 min; přes Ozvěnu nikdy (Ozvěna panel zavře).
- **Forma:** spodní panel přes aplikaci, nebere fokus, sám zmizí po 20 s, „Teď ne“ ho zavře.
- **Odpovědi** (jedno ťuknutí) → `video_ratings.rating`: Naučil jsem se něco 5, Inspirace, nápad 4,
  Odpočinul jsem si 3, Nic, jen čas 2, Je mi hůř 1. Text odpovědi jde do `intent`, název videa do `title`
  (nečitelný název z aplikace YouTube = „Video na YouTube“, zaplacený feed = „Feed · {aplikace}“). +5 XP (SPEC 8).
- **Ozvěna:** stejný pořad (název bez pořadového čísla) nebo feed stejné aplikace naposledy dostal ≤ 2 →
  „Minule ti „…“ nedalo nic.“ (2× a víc za sebou: „Už 2× za sebou…“). Jinak dnešní odpovědi ≤ 2 stejného
  druhu (videa / feed) → „Dnes ti už 2 videa nedala nic.“ / „Dnes ti feed už 2× nedal nic.“
- **Zrcadlo:** mapa hodnoty seskupí opakované pořady podle názvu, ostatní videa podle odpovědi.

## 10. Demo režim (demo panel, 123)

Přepínače a akce: vynutit `HOME`/`AWAY`, práh 20 s, smlouva platí hned, reset dne, nahrát `mockdata/`, +1 000 kroků, +500 XP, stav postavy, ověření „vždy ANO“. Poslední je viditelně označené a jen v demo panelu.

O škrtech na branách rozhoduje Marek, Štěpán hlídá čas a připomíná seznam škrtů.

## 11. Rozšíření rozhraní (implementuje 105)

Doplnění k `docs/contracts.md` z PR #8. Zásady zůstávají: stav = `StateFlow`, akce = `suspend fun`, čistý Kotlin, nic nehází do UI.

### 11.1 Plán dne: rozšíření `ContractRepository` (Šimon, 212)

```kotlin
enum class TaskType { MOVEMENT, SCHOOL, WORK, OTHER }
enum class VerificationMethod { STRAVA, PHOTO, TEXT }
enum class TaskStatus { PENDING, VERIFIED, REJECTED, VERIFY_LATER }

data class PlanTask(
    val id: Long = 0, val type: TaskType, val title: String, val target: String = "",
    val rewardMinutes: Int, val method: VerificationMethod,
    val status: TaskStatus = TaskStatus.PENDING, val attempts: Int = 0,
    val proofUri: String? = null, val proofHash: String? = null,
)

data class DailyContract(              // stávající + tasks
    val date: LocalDate, val stepGoal: Int,
    val plannedContent: List<String> = emptyList(),
    val videoUri: String? = null,
    val tasks: List<PlanTask> = emptyList(),
) { fun isFulfilled(stepsToday: Int): Boolean } // sekce 6; kroky smlouva nezná (rozhodnutí 105)

interface ContractRepository {          // stávající + updateTask
    val today: StateFlow<DailyContract?>
    suspend fun save(contract: DailyContract)
    suspend fun forDate(date: LocalDate): DailyContract?
    suspend fun updateTask(task: PlanTask)
}
```

### 11.2 Odměny (Šimon, 221)

```kotlin
interface MovementRepository {          // stávající + addEarnedMinutes
    // ...
    suspend fun addEarnedMinutes(minutes: Int)   // odměna za úkol, zvýší zůstatek
}
interface GamificationEngine {          // stávající + onTaskVerified
    // ...
    suspend fun onTaskVerified(task: PlanTask): Int
}
interface RewardService { suspend fun reward(task: PlanTask) } // minuty + XP
```

### 11.3 Ověření, Strava, LLM (Člen 5, 501–522)

```kotlin
enum class LlmProvider { CLAUDE, GEMINI, OPENAI }
enum class VerificationResult { YES, NO, LATER }
sealed interface Proof {
    data class Photo(val uri: String) : Proof
    data class Text(val text: String) : Proof
    data object Strava : Proof
}
interface TaskVerifier { suspend fun verify(task: PlanTask, proof: Proof): VerificationResult }

data class StravaActivity(val id: Long, val type: String, val startMillis: Long,
                          val movingTimeSec: Int, val distanceMeters: Float)
interface StravaRepository {
    val isConnected: StateFlow<Boolean>
    suspend fun todayActivities(): List<StravaActivity>
}
```

`ContentMatcher` zůstává beze změny, implementuje ho Člen 5 (512) místo Šimona.

### 11.4 Nastavení (Marek, 105)

```kotlin
data class Settings(
    val homeSsid: String? = null, val llmProvider: LlmProvider = LlmProvider.GEMINI,
    val demoMode: Boolean = false, val demoThresholdSec: Int? = null,
    val demoContractToday: Boolean = false, val demoAlwaysYes: Boolean = false,
)
interface SettingsRepository { val settings: StateFlow<Settings>; suspend fun update(change: (Settings) -> Settings) }
```

Ukládá se do DataStore, ne do Room.

### 11.5 Sdílená databáze `NarsilDatabase` (Marek, 105)

Jedna databáze Room v `cz.narsil.core.data`, verze 1, `fallbackToDestructiveMigration()`.
Všechny entity a DAO napíše 105 předem. Ostatní je používají jen ve svých repozitářích. Nová entita nebo dotaz → Poznámky v tasku, přidá Marek.
Enumy se ukládají jako text (název hodnoty), datum jako `LocalDate.toString()`.

| Entita | Sloupce | Používá |
| --- | --- | --- |
| `ContractEntity` | `date` (PK), `stepGoal`, `plannedContent` (JSON pole), `videoUri`, `createdAtMillis` | Šimon (212) |
| `PlanTaskEntity` | `id` (PK auto), `date` (index), `type`, `title`, `target`, `rewardMinutes`, `method`, `status`, `attempts`, `proofUri`, `proofHash` (index), `verifiedAtMillis` | Šimon (212), Člen 5 (520) |
| `ConsumptionEventEntity` | `id`, `packageName`, `surface`, `startedAtMillis`, `durationMillis`, `videoTitle`, `url`, `planned`, `context` | Marek (121) |
| `InterventionEntity` | `id`, `atMillis`, `packageName`, `reason`, `feeling`, `outcome`, `xpGained` | Marek (114, 121) |
| `LedgerEntryEntity` | `id`, `date`, `atMillis`, `kind` (`TASK_REWARD`, `PAID`, `BORROW`, `DEBT_CARRY`), `minutes` | Šimon (203, 211) |
| `XpEventEntity` | `id`, `date`, `atMillis`, `source`, `xp` | Šimon (202, 220) |
| `BadgeEntity` | `id` (PK), `unlockedAtMillis` | Šimon (220) |
| `VideoRatingEntity` | `id`, `atMillis`, `title`, `intent`, `rating` | Marek (120), Jan (321) |

DAO: `ContractDao`, `ConsumptionDao`, `InterventionDao`, `LedgerDao`, `XpDao`, `BadgeDao`, `VideoRatingDao`. Každé má základní `insert`, `observe…` (`Flow`) a dotazy podle `date`.

### 11.6 UI sloty (Jan 310, 311; hostí Marek 113 a 124)

```kotlin
// cz.narsil.ui.screens.echo
data class EchoUiState(
    val videoUri: String?, val fallbackText: String?, val sessionMinutes: Int,
    val balanceMinutes: Int, val canPay: Boolean, val canBorrow: Boolean, val closeXp: Int,
)
@Composable fun EchoScreen(
    state: EchoUiState,
    videoSlot: @Composable () -> Unit,          // ExoPlayer dodá overlay (113)
    onFeeling: (String) -> Unit,                // nuda, únava, úkol, samota, jiné
    onChoice: (InterventionOutcome) -> Unit,
)

// cz.narsil.ui.screens.squat
@Composable fun SquatScreen(
    count: Int, target: Int,
    onPreviewReady: (surfaceProvider: Any) -> Unit, // PreviewView vytvoří obrazovka, předá SquatCounter.start()
    onCancel: () -> Unit,
)
```

- ViewModely píše Jan v `ui/screens/<obrazovka>/`. Konstruktor bere jen rozhraní z `cz.narsil.core`, instance dodá `ServiceLocator`.
- Ozvěna ViewModel nemá, stav sestaví overlay (113) a obrazovku vloží do okna.
- Ladicí akce pro demo panel (222) jsou metody na fake a skutečných implementacích (`addSteps`, `addXp`, `setAvatar`). Demo panel (123) je volá přes `ServiceLocator`.
