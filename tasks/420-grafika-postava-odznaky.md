---
status: in-progress
owner: stepan
agent: codex
branch: stepan/codex/420-grafika-postava-odznaky
pr:
---

# 420 – Postava budoucího já a ikony odznaků

## Cíl
Ilustrace postavy budoucího já v 5 stavech (od unaveného po v kondici) a 5 ikon odznaků (např. „Odešel jsem sám 10×“, „Týden bez dluhu“, „1 000 dřepů“) ve stylu podle `docs/ilustrace.md` (Jan, 303).

## Soubory
- assets/postava/
- assets/odznaky/

## Hotovo když
Jan je může vložit (323).

## Poznámky
- Pro: Štěpán (Gemini zdarma). Gemini nemá přístup do repa – Štěpán výstup ukládá a commituje sám (prefix `[gemini]`).
- Chat C Grafika, 2–7 h. Po 303.
- Do aplikace (`app/src/main/res/drawable/`) je vkládá Jan v 323 – 420 do `app/` nesahá.
- Nejasnost: `assets/` = složka v kořeni repa (zdrojová grafika), ne `app/src/main/assets/`.
