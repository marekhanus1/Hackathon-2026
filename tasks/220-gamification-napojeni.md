---
status: review
owner: simon
agent: claude
branch: simon/claude/220-gamification-napojeni
pr: https://github.com/marekhanus1/Hackathon-2026/pull/38
---

# 220 – Gamifikace napojená na výsledky

## Cíl
Napojit `GamificationEngine` na `InterventionOutcome` a události: XP, úrovně, série se žolíky, 3–5 odznaků, stav postavy (5 stavů podle posledních 7 dní), denní skóre souboje s algoritmem, boss v pevné hodině (+150 XP). Perzistence stavu.

## Soubory
- app/src/main/java/cz/narsil/gamification/ (mimo `gamification/debug/` – patří 222)
- app/src/test/java/cz/narsil/gamification/

## Hotovo když
Každá volba v Ozvěně změní XP a denní skóre.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 2 (8–14 h), proud A. Po 202. Brána G3.
- Denní minuty feedu a získané minuty bere z Room souhrnů Marka (121).
- Překryv: `gamification/` obsahuje `gamification/debug/` z 222 (stejná fáze, paralelně) – 220 do `debug/` nesahá.
- Pro Marka: chybí `XpDao.all()` (teď se historie načítá po dnech za 90 dní). Dřepy nad 20 posílat do `RulesGamificationEngine.onExtraSquats(n)` (žolíky).
