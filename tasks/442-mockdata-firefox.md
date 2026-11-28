---
status: review
owner: stepan
agent: codex
branch: stepan/codex/442-mockdata-firefox
pr: https://github.com/marekhanus1/Hackathon-2026/pull/87
---

# 442 – Mock data: Firefox místo Chrome

## Cíl
Sjednotit připravená data s rozhodnutím „web se sleduje ve Firefoxu“ (docs/decisions.md, 2026-11-28). Aplikace teď zná klíč `firefox`, `chrome` už ne (MockDataLoader).

## Soubory
- mockdata/schema.md
- mockdata/generuj.py
- mockdata/*.json (jen přegenerované)

## Hotovo když
- `schema.md` uvádí aplikace `instagram`, `tiktok`, `youtube`, `firefox`,
- `generuj.py` nikde negeneruje `chrome` a pár událostí přidá jako `firefox` (YouTube nebo Instagram na webu), aby web v demu nebyl prázdný,
- JSON jsou přegenerované a `python3 mockdata/generuj.py` proběhne bez chyby.

## Poznámky
- Pro: Štěpán (GPT Plus). Malý úkol, vhodný na rozjezd.
- Formát ostatních polí neměň, čte ho Jan (322, 323) a demo panel (123).
