---
status: in-progress
owner: simon
agent: claude
branch: simon/claude/202-gamification-engine
pr:
---

# 202 – GamificationEngine v čistém Kotlinu

## Cíl
Implementace `GamificationEngine` z core bez závislosti na Androidu: `onOutcome()` → XP, stav (XP, úroveň, série, žolíky, stav postavy, denní skóre souboje) podle `docs/GAMIFICATION.md` (201). Unit testy.

## Soubory
- app/src/main/java/cz/narsil/gamification/
- app/src/test/java/cz/narsil/gamification/

## Hotovo když
Testy pokrývají XP, úrovně, série a žolíky.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 0, proud A, po 201. Paralelně s 203 (různé složky – bez překryvu).
- Kostra ještě neexistuje: vzniká mimo repozitář, po G1 ji orchestrátor přesune do balíčku. Rozhraní `GamificationEngine` dodá 102 – pokud se liší, přizpůsobit implementaci, rozhraní neměnit.
