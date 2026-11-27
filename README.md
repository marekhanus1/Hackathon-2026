# Hackathon 2026

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
# Hackathon-2026
