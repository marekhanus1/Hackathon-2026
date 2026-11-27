---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 430 – Připravená data za 21 dní

## Cíl
Připravená data za 21 dní podle `mockdata/schema.md`: Zrcadlo (denní spotřeba a statistiky), mapa hodnoty (hodnocení videí po kanálech a formátech), žebříček party. Data vyprávějí příběh zlepšení (méně minut feedu, víc „Zavřít“, rostoucí série).

## Soubory
- mockdata/*.json (`mirror.json`, `value_map.json`, `leaderboard.json`; `schema.md` neměnit – patří 104/Markovi)

## Hotovo když
JSON projde schématem a Jan ho použije (322).

## Poznámky
- Pro: Štěpán (Gemini zdarma). Gemini nemá přístup do repa – Štěpán výstup ukládá a commituje sám (prefix `[gemini]`).
- Chat D Data, 2–5 h. Po 104 (schéma). Přednost při nedostatku kvóty.
- Používají 322, 323 (žebříček) a demo panel 123 (načtení připravených dat).
- Čísla XP podle `docs/GAMIFICATION.md` (201), pokud už existuje.
