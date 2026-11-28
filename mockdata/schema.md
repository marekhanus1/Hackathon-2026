# Schéma připravených dat (mockdata)

Navrhuje Marek (úkol 104), plní Štěpán (430), čte Jan (322 Zrcadlo a mapa hodnoty,
323 žebříček) a demo panel (123). Data pokrývají **21 dní** a vyprávějí příběh
zlepšení: minut feedu ubývá, „Zavřít“ přibývá, série roste.

## Společná pravidla

- Kódování UTF-8, texty česky. Klíče v `snake_case` anglicky (snadné mapování na Kotlin).
- Datum `YYYY-MM-DD`, čas `HH:MM`, délky v celých **minutách**.
- Den 1 = nejstarší, den 21 = „včera“ v demu. Datum se v aplikaci může přepočítat
  relativně k dnešku (`day_index` 1–21 je proto u dnů povinný).
- Povrchy podle `ConsumptionEvent`: `FEED`, `REELS`, `SHORTS`, `MESSAGES`, `VIDEO`.
- Výsledky podle `InterventionOutcome`: `CLOSED`, `PAID_STEPS`, `SQUATS`, `BORROWED`, `PLANNED_FREE`.
- Aplikace: `instagram`, `tiktok`, `youtube`, `firefox` (`firefox` = sledovaný obsah na webu,
  například YouTube nebo Instagram otevřený v mobilním Firefoxu).
- Čísla XP podle `docs/GAMIFICATION.md` (201); do té doby podle briefu
  (Zavřít 50, dřepy 20, kroky 10, půjčka 0, smlouva +100, hodnocení +5).
- Návrh: aplikace čte soubory jako assets – v `app/build.gradle.kts` přidat
  `sourceSets["main"].assets.srcDir("../mockdata")` (rozhodne Marek), pak
  `context.assets.open("mirror.json")`. Soubory v `mockdata/` jsou tak jediný zdroj.

## 1. Zrcadlo – `mockdata/mirror.json`

Statistiky po prvních dnech: denní spotřeba, zásahy a jejich výsledky, souboj
s algoritmem a pár vět „zrcadla“ (v reálné aplikaci by je psalo LLM).

| Pole | Typ | Popis |
| --- | --- | --- |
| `days[]` | pole, 21 prvků | Jeden den |
| `days[].day_index` | int 1–21 | Pořadí dne |
| `days[].date` | string | Datum |
| `days[].feed_minutes` | int | Minuty neplánovaného nekonečného obsahu doma |
| `days[].reclaimed_minutes` | int | Minuty „vzaté zpátky“ (souboj s algoritmem) |
| `days[].planned_minutes` | int | Plánovaný obsah ze smlouvy (zdarma) |
| `days[].by_app` | objekt aplikace → minuty | Rozpad `feed_minutes` |
| `days[].interventions[]` | pole | Zásahy: `time`, `app`, `surface`, `outcome`, `avoiding` (`nuda`/`unava`/`ukol`/`samota`/`null`) |
| `days[].steps` | int | Kroky za den |
| `days[].squats` | int | Dřepy za den |
| `days[].debt_minutes` | int | Dluh na konci dne |
| `days[].contract_fulfilled` | bool | Splněná večerní smlouva |
| `days[].battle_won` | bool | `reclaimed_minutes > feed_minutes` |
| `days[].xp_gained` | int | XP získané ten den |
| `critical_hour` | string `HH:MM` | Nejrizikovější hodina (boss) |
| `insights[]` | pole stringů | 3–5 vět Zrcadla, krátce a povzbudivě |

```json
{
  "critical_hour": "22:00",
  "insights": [
    "Nejčastěji utíkáš na TikTok kolem 22:00, když se ti nechce do úkolů.",
    "Za poslední týden sis vzal zpátky 6 hodin.",
    "Od feedu jsi 14× odešel sám."
  ],
  "days": [
    {
      "day_index": 1,
      "date": "2026-11-06",
      "feed_minutes": 142,
      "reclaimed_minutes": 10,
      "planned_minutes": 45,
      "by_app": { "instagram": 60, "tiktok": 70, "youtube": 12 },
      "interventions": [
        { "time": "21:47", "app": "tiktok", "surface": "FEED", "outcome": "BORROWED", "avoiding": "unava" },
        { "time": "22:30", "app": "instagram", "surface": "REELS", "outcome": "SQUATS", "avoiding": "ukol" }
      ],
      "steps": 4200,
      "squats": 20,
      "debt_minutes": 10,
      "contract_fulfilled": false,
      "battle_won": false,
      "xp_gained": 20
    }
  ]
}
```

## 2. Mapa hodnoty – `mockdata/value_map.json`

Hodnocení videí (záměr před, 1–5 po) a z nich souhrn po kanálech a formátech.
Pointa: kanály, které dokoukávám, ale hodnotím nízko.

| Pole | Typ | Popis |
| --- | --- | --- |
| `ratings[]` | pole | Jednotlivá zhlédnutí |
| `ratings[].date` | string | Datum |
| `ratings[].title` | string | Název videa |
| `ratings[].channel` | string | Kanál |
| `ratings[].format` | `SHORT` / `LONG` | Formát |
| `ratings[].intent` | string | Záměr před sledováním (může být `""`) |
| `ratings[].planned` | bool | Spárováno s plánem ze smlouvy |
| `ratings[].watched_percent` | int 0–100 | Kolik dokoukáno |
| `ratings[].rating` | int 1–5 | Hodnocení po zhlédnutí |
| `channels[]` | pole | Souhrn za kanál: `channel`, `format`, `views`, `minutes`, `avg_watched_percent`, `avg_rating` |
| `channels[].verdict` | `KEEP` / `TRAP` / `NEUTRAL` | `TRAP` = dokoukávám (≥ 70 %), ale hodnotím ≤ 2 |

```json
{
  "ratings": [
    {
      "date": "2026-11-20",
      "title": "Rust ownership za 20 minut",
      "channel": "Let's Get Rusty",
      "format": "LONG",
      "intent": "pochopit borrow checker",
      "planned": true,
      "watched_percent": 95,
      "rating": 5
    }
  ],
  "channels": [
    { "channel": "Let's Get Rusty", "format": "LONG", "views": 6, "minutes": 118, "avg_watched_percent": 90, "avg_rating": 4.7, "verdict": "KEEP" },
    { "channel": "Drama Recap CZ", "format": "SHORT", "views": 41, "minutes": 96, "avg_watched_percent": 88, "avg_rating": 1.6, "verdict": "TRAP" }
  ]
}
```

## 3. Žebříček party – `mockdata/leaderboard.json`

Simulovaná výzva party: společný cíl kroků a žebříček získaného času.
Uživatel je v datech označený `is_me: true`.

| Pole | Typ | Popis |
| --- | --- | --- |
| `party_name` | string | Název party |
| `challenge.title` | string | Název výzvy |
| `challenge.steps_goal` | int | Společný cíl kroků |
| `challenge.steps_done` | int | Splněno dohromady |
| `challenge.ends` | string | Datum konce |
| `members[]` | pole | Členové seřazení podle `reclaimed_minutes` sestupně |
| `members[].name` | string | Přezdívka (vymyšlená, ne skutečné osoby) |
| `members[].is_me` | bool | Uživatel aplikace |
| `members[].reclaimed_minutes` | int | Získaný čas za týden |
| `members[].steps` | int | Příspěvek k cíli |
| `members[].streak_days` | int | Aktuální série |
| `members[].level` | int | Úroveň |
| `members[].avatar_state` | int 1–5 | Stav postavy (1 unavený … 5 v kondici) |

```json
{
  "party_name": "Kovárna",
  "challenge": {
    "title": "100 000 kroků za týden",
    "steps_goal": 100000,
    "steps_done": 73400,
    "ends": "2026-11-30"
  },
  "members": [
    { "name": "Ty", "is_me": true, "reclaimed_minutes": 380, "steps": 28100, "streak_days": 6, "level": 4, "avatar_state": 4 },
    { "name": "Bára", "is_me": false, "reclaimed_minutes": 310, "steps": 24800, "streak_days": 9, "level": 5, "avatar_state": 5 },
    { "name": "Ondra", "is_me": false, "reclaimed_minutes": 150, "steps": 20500, "streak_days": 2, "level": 2, "avatar_state": 2 }
  ]
}
```
