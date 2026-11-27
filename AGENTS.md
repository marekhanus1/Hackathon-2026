# Pravidla pro AI agenty (Claude Code, Gemini, Codex, …)

Tento soubor je **jediný zdroj pravdy** pro všechny agenty. `CLAUDE.md` a `GEMINI.md`
na něj jen odkazují — pravidla měň tady.

Na projektu pracuje víc lidí a víc modelů **současně**. Cílem je, aby si nikdo
nepřepisoval práci a merge byl nudný.

## Projekt

Narsil: Android aplikace proti prokrastinaci na sociálních sítích (hackathon, 20 h).
Zadání a plán: docs/BRIEF.md. Pravidla chování a čísla: docs/SPEC.md.
Rozhraní, UI sloty a sdílená databáze: docs/contracts.md (rozšíření v docs/SPEC.md, sekce 11).
Priorita: funkční demo, ne dokonalý kód.

- Stack: Kotlin, Jetpack Compose, jeden modul app, minSdk 29, Room, CameraX,
  ML Kit Pose Detection, Health Connect, Media3, OkHttp.
- Build: ./gradlew assembleDebug · Testy: ./gradlew testDebugUnitTest
- Instalace: ./gradlew installDebug (demo Nothing Phone 3a, záloha Redmi Note 11)
- Balíčky a vlastníci (Soubory v tasku jen z vlastních balíčků):
  - cz.narsil.core (vč. databáze), detection, overlay, app: Marek / Claude (1xx)
  - cz.narsil.movement, gamification, contract: Šimon / Claude (2xx)
  - cz.narsil.ui a res/: Jan / Antigravity (3xx); každá obrazovka ve vlastní
    složce ui/screens/<obrazovka>/, sdílené komponenty jen v úkolu 302
  - mockdata/, pitch/, assets/: Štěpán / Gemini (4xx)
  - cz.narsil.verify, integrations, llm: Člen 5 / Codex (5xx)
- Rozhraní v cz.narsil.core, schéma databáze a docs/contracts.md mění jen Marek.
- Blokuje tě cizí modul: použij fake z cz.narsil.core.fake a pokračuj.
- Nové závislosti nepřidávej, vše je v gradle/libs.versions.toml.
- API klíče (Claude, Gemini, OpenAI, Strava) jen v local.properties, nikdy v gitu.
- Texty v UI česky. Hlavní mechanika se v UI jmenuje Ozvěna.

## Zlatá pravidla

1. **Nikdy nepracuj přímo na `main`.** Každý úkol = vlastní větev + vlastní worktree
   (`scripts/start-task.sh`). Na `main` se commituje jen claim/uvolnění úkolu v `tasks/`.
2. **Než začneš, vezmi si úkol** v `tasks/` (viz níže). Pracuj jen na souborech,
   které k úkolu patří (sekce `Soubory` v tasku). Pokud potřebuješ sáhnout jinam,
   napiš to do tasku a dej vědět člověku.
3. **Malé, časté commity** s prefixem agenta: `[claude] přidán endpoint /login`,
   `[gemini] oprava parsování CSV`, `[codex] klient Strava`.
4. **Před PR se rebasni na `origin/main`** (`git fetch && git rebase origin/main`)
   a ověř, že projekt jde spustit / testy projdou.
5. **Nemaž a nepřepisuj cizí kód** bez zmínky v PR. Nedělej force-push na cizí větve.
6. **Žádné tajné klíče v gitu.** Používej `.env` (je v `.gitignore`), vzor dej do `.env.example`.
7. **Sdílená rozhraní** (API kontrakty, datové modely, schéma DB) zapisuj do `docs/contracts.md`
   dřív, než je implementuješ — ostatní agenti podle nich staví paralelně.
8. Rozhodnutí, která ovlivní ostatní, zapiš jedním řádkem do `docs/decisions.md`.

## Úkoly (`tasks/`)

Každý úkol je jeden soubor `tasks/NNN-kratky-nazev.md` (šablona `tasks/_TEMPLATE.md`).
Jeden soubor na úkol = žádné merge konflikty na společném seznamu.

Stavy: `todo` → `in-progress` → `review` → `done`.

**Claim (zámek):** `scripts/claim-task.sh <soubor> <jméno> <agent>` nastaví
`owner`/`agent`/`status: in-progress`, commitne a pushne na `main`. Push je zámek:
kdo pushne první, má úkol. Když push selže kvůli konfliktu, úkol už má někdo jiný —
vyber jiný.

## Větve a worktree

- Název větve: `<jméno>/<agent>/<NNN-kratky-nazev>`, např. `marek/claude/003-login`.
- Worktree leží vedle repa: `../hackathon_2026-wt/<větev s / → ->`.
- Každý agent běží ve **svém** worktree → dva agenti na jednom stroji se nepřepisují.

## Hotovo = 

- kód je na větvi pushnutý a je otevřené PR do `main`,
- task má `status: review` a odkaz na PR,
- v PR popisu: co se změnilo, jak to otestovat, který model to psal.
