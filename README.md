# Hackathon 2026

## Narsil – TL;DR

Android aplikace proti prokrastinaci na sociálních sítích (hackathon „antiprokrastinační zbraň“, 20 h).
Nic neblokuje, přesvědčuje. Název podle zlomeného a znovu ukovaného meče: relaps není konec.

- **Kdy zasahuje:** jen doma (domácí Wi-Fi) a jen u nekonečného obsahu (feed, Reels, TikTok, řetězení videí na YouTube) po 5 minutách. Venku, ve zprávách a při krátkém pohledu mlčí.
- **Ozvěna:** v pokušení pustí tvoje vlastní večerní video a zeptá se „Čemu se teď vyhýbáš?“.
- **Pohyb jako měna:** plánovaný obsah z večerní smlouvy je zdarma, neplánovaný se platí kroky nebo 20 dřepy před kamerou. Jde si i půjčit.
- **Gamifikace:** nejvíc XP za „Zavřít“, postava budoucího já, souboj s algoritmem, série se žolíky. Nikdy netrestá.
- **Parťák:** když aplikaci smažeš, nedozví se to server, ale kamarád.
- **Stack:** Kotlin, Jetpack Compose, AccessibilityService, ML Kit Pose Detection, Health Connect, Claude API.
- **Tým:** Marek (Claude, jádro a integrace), Šimon (Claude, pohyb a gamifikace), Jan (Antigravity, UI), Štěpán (Gemini, pitch a data).
- **Cíl harmonogramu:** v 8. hodině funkční průchod TikTok → Ozvěna → dřepy na telefonu.

Celý brief, harmonogram a plány agentů: [docs/BRIEF.md](docs/BRIEF.md). Pravidla pro agenty: [AGENTS.md](AGENTS.md).

## Workspace
Kolaborační workspace: víc lidí × víc AI agentů (Claude Code, Gemini CLI) na jednom repu zároveň.

## Jak to funguje

```
main ── tasks/*.md (kdo co dělá) ── AGENTS.md (pravidla pro všechny modely)
 ├─ worktree marek/claude/002-api      ← agent 1
 ├─ worktree marek/gemini/003-ui       ← agent 2 (stejný člověk, jiný model)
 └─ worktree petr/claude/004-db        ← jiný člen týmu
```

- **AGENTS.md** – společná pravidla. `CLAUDE.md` i `GEMINI.md` ho importují, oba modely tak čtou totéž.
- **tasks/** – jeden soubor na úkol. Push claim commitu na `main` funguje jako zámek.
- **worktree na úkol** – každý agent má vlastní složku a větev, nepřepisují si soubory.
- **docs/contracts.md** – rozhraní domluvená předem, ať se dá stavět paralelně.

## Workflow

```bash
git pull
scripts/claim-task.sh tasks/002-api.md marek claude    # zamkne úkol
scripts/start-task.sh marek/claude/002-api             # vytvoří worktree
cd ../hackathon_2026-wt/marek-claude-002-api && claude # nebo gemini
# … práce …
scripts/finish-task.sh                                 # rebase + push + odkaz na PR
```

Nový úkol: zkopíruj `tasks/_TEMPLATE.md` na `tasks/NNN-nazev.md`, commit + push na `main`.

Po merge PR: `git worktree remove ../hackathon_2026-wt/<složka>`.
