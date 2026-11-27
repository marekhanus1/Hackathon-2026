---
status: review
owner: simon
agent: claude
branch: simon/claude/230-ladeni-xp
pr: https://github.com/marekhanus1/Hackathon-2026/pull/42
---

# 230 – Vyladit čísla XP a kurz

## Cíl
Vyladit čísla XP a kurz pohybu tak, aby 2minutové demo (viz demo scénář v briefu a 403) skončilo animací nové úrovně (animace od Jana, 321).

## Soubory
- app/src/main/java/cz/narsil/gamification/
- docs/GAMIFICATION.md (odhad – aktualizovat čísla)

## Hotovo když
Scénář dema končí animací nové úrovně.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 3 (14–18 h), proud A. Brána G4. Po G3 (feature freeze: jen ladění čísel, žádné nové funkce).
- Zbylé proudy pomáhají Markovi s opravami (130).
- Demo startuje na 1 480 XP (úroveň 3, 480/500): 20 dřepů → úroveň 4 + animace. Pro Marka (123): `DemoActions.prepareDemo()` má volat `GamificationDebug.prepareDemo(engine)` a `MovementDebug.resetDay(repo)` (fake `DEMO_STATE` 1 340 XP úroveň nespustí). Animaci ukázat, když `level` po volbě > před ní (`DemoTuning.levelsUp`).
