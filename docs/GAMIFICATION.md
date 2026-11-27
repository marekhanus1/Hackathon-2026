# Narsil – pravidla gamifikace (201)

Přesná pravidla pro `GamificationEngine` (202, 220), mock data (430) a obrazovky Jana (312, 321–323).
Čísla vycházejí z briefu a `docs/SPEC.md` sekce 4, 5 a 8; při rozporu platí SPEC. Ladí je úkol 230.
Konstanty v kódu: `cz.narsil.gamification.GamificationRules`.

**Zásada: nikdy netrestat.** XP se jen přičítají. Dluh, relaps, placení ani nesplněný den XP neberou,
jen nepřidávají. Jediné, co se může „ztratit“, je série – a tu chrání žolíky.

## 1. Den

- Den začíná ve **4:00** (`DayKey`). Všechny denní limity a skóre se počítají za tento den.
- Hodina bosse se bere v místním čase (21:00 patří ještě do dne, který začal ve 4:00).

## 2. XP za rozhodnutí

| Zdroj (`XpEventEntity.source`) | XP | Podmínka / strop |
| --- | --- | --- |
| `CLOSED` – Zavřít v Ozvěně | **50** | nejvýš **5× denně** a s odstupem **≥ 10 min** od posledního odměněného zavření; jinak 0 XP (volba se i tak zapíše) |
| `SQUATS` – 20 dřepů | **20** | bez stropu (každé odemknutí stojí 20 dřepů) |
| `PAID_STEPS` – zaplatit kroky | **10** | bez stropu (platba je omezená zůstatkem) |
| `BORROWED` – půjčka | **0** | zapíše se kvůli odznaku „Týden bez dluhu“ |
| `PLANNED_FREE` – plánovaný obsah | **0** | není volba, jen statistika |
| `TASK` – ověřený úkol plánu | **30** | jednou za úkol (hlídá `RewardService`, 221) |
| `CONTRACT` – splněná smlouva | **100** | jednou za den; splněno = kroky ≥ cíl a všechny úkoly `VERIFIED` |
| `VIDEO_RATED` – ohodnocené video | **5** | nejvýš **10× denně** |
| `BOSS` – přežitá kritická hodina | **150** | jednou za den, viz sekce 7 |
| `DEMO` – demo panel | libovolně | jen ladění (222) |

Pořadí odměn odpovídá cíli: nejvíc dostane ten, kdo od feedu odejde sám (Zavřít 50 > dřepy 20 > kroky 10 > půjčka 0).

## 3. Úrovně

- `level = 1 + floor(xp / 500)`, `xpInLevel = xp mod 500`, `xpPerLevel = 500`.
- Úroveň se nikdy nesníží (XP jen přibývají).
- Nová úroveň = animace (Jan, 321). Úroveň odemyká vzhled postavy (ilustrace 420).

## 4. Série a žolíky

- **Série** (`streakDays`) = počet dní po sobě se splněnou smlouvou (`CONTRACT`).
- Vyhodnocuje se vždy uzavřený den (včerejšek a starší). Dnešek sérii nikdy nepřeruší – jen ji může prodloužit,
  když je smlouva dnes už splněná.
- Uzavřený den **bez** splnění:
  - je-li v zásobě žolík → spotřebuje se jeden, série zůstane (neprodlouží se),
  - jinak série = 0.
- **Žolík** (`jokers`): za každých **50 dřepů navíc** během dne (nad rámec 20 dřepů na odemknutí).
  Nejvýš **3** v zásobě; další se nepřidávají. Zapisuje se jako událost `JOKER` (0 XP).
- Počítá se od prvního dne s jakoukoli událostí; dny před instalací sérii neruší.

## 5. Postava – budoucí já (`AvatarState`)

Podle počtu dní se splněnou smlouvou za posledních 7 dní (dnešek + 6 předchozích):

| Dní se splněnou smlouvou | Stav |
| --- | --- |
| 0–1 | `EXHAUSTED` |
| 2 | `TIRED` |
| 3–4 | `OK` |
| 5–6 | `FIT` |
| 7 | `THRIVING` |

- Výjimka „nikdy netrestat“: během prvních 7 dní používání se postava nedrží pod `OK`, dokud má uživatel
  méně než 7 dní historie (nový uživatel nezačíná vyčerpaný).
- Demo panel může stav vnutit (`setAvatar`, 222); vnucení platí do resetu.

## 6. Souboj s algoritmem (`DailyScore`)

- `feedMinutes` = minuty neplánovaného nekonečného obsahu doma za dnešek (`ConsumptionDao.observeFeedMinutesSince`, 121).
  Dokud detekce nedodává data, záloha: 10 min za každé odemknutí (`PAID_STEPS`, `SQUATS`, `BORROWED`).
- `reclaimedMinutes` = počet `CLOSED` dnes × **15** + Σ odměn ověřených úkolů dnes (minuty).
- Den **vyhrává** vyšší číslo: `isWon = reclaimedMinutes > feedMinutes`. Remíza = algoritmus.
- `wonDays` = počet vyhraných dní celkem (bod za den). Dnešek se započítá, jakmile je vyhraný.
- 15 min je odhad ušetřené relace (vyladí 230).

## 7. Boss kritické hodiny

- Kritická hodina je v MVP pevně **21:00–22:00** (v budoucnu nejrizikovější hodina z dat).
- Po 22:00 se vyhodnotí: když v té hodině nebyl žádný zásah, nebo každý zásah skončil `CLOSED` → **+150 XP** (`BOSS`).
- Zaplacení, dřepy nebo půjčka v kritické hodině = boss tentokrát vyhrál (nic se nebere, jen +150 nepřijde).
- Vyhodnocuje se líně: při další události nebo `refresh()` po 22:00 téhož dne.

## 8. Odznaky (MVP)

| Id | Název v UI | Podmínka |
| --- | --- | --- |
| `FIRST_CLOSE` | První odchod | 1× `CLOSED` |
| `CLOSE_10` | Odešel jsem sám 10× | 10× `CLOSED` celkem |
| `NO_DEBT_WEEK` | Týden bez dluhu | 7 dní po sobě bez `BORROWED` (od prvního dne používání) |
| `SQUATS_1000` | 1 000 dřepů | dřepy celkem ≥ 1 000 (20 za každé `SQUATS` + 50 za každý `JOKER`) |
| `PLAN_STREAK_3` | Tři dny podle plánu | série ≥ 3 |

Odznak se odemkne jednou a už nezmizí (`BadgeEntity`, insert IGNORE).

## 9. Výzvy party (simulace)

- Společný týdenní cíl kroků party (výchozí 4 × 6 000 × 7 = 168 000) a žebříček získaného času (`reclaimedMinutes` za týden).
- V MVP jen připravená data (430, Štěpán) a obrazovka žebříčku (323, Jan); engine je nepočítá.

## 10. Co engine vystavuje

`GamificationState`: `xp`, `level`, `xpInLevel`, `xpPerLevel`, `streakDays`, `jokers`, `avatar`, `today` (`DailyScore`),
`wonDays`, `badges` (id ze sekce 8). Metody viz `docs/contracts.md` – `onOutcome`, `onContractFulfilled`,
`onTaskVerified`, `onVideoRated`; ladění (222): `addXp`, `setAvatar`.
