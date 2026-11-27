# Pravidla pro AI agenty (Claude Code, Gemini CLI, …)

Tento soubor je **jediný zdroj pravdy** pro všechny agenty. `CLAUDE.md` a `GEMINI.md`
na něj jen odkazují — pravidla měň tady.

Na projektu pracuje víc lidí a víc modelů **současně**. Cílem je, aby si nikdo
nepřepisoval práci a merge byl nudný.

## Projekt

> TODO: doplnit, až bude jasné, co stavíme (cíl, stack, jak spustit, jak testovat).

## Zlatá pravidla

1. **Nikdy nepracuj přímo na `main`.** Každý úkol = vlastní větev + vlastní worktree
   (`scripts/start-task.sh`). Na `main` se commituje jen claim/uvolnění úkolu v `tasks/`.
2. **Než začneš, vezmi si úkol** v `tasks/` (viz níže). Pracuj jen na souborech,
   které k úkolu patří (sekce `Soubory` v tasku). Pokud potřebuješ sáhnout jinam,
   napiš to do tasku a dej vědět člověku.
3. **Malé, časté commity** s prefixem agenta: `[claude] přidán endpoint /login`,
   `[gemini] oprava parsování CSV`.
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
