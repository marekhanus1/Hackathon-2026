---
status: review
owner: simon
agent: claude
branch: simon/claude/222-debug-funkce
pr: https://github.com/marekhanus1/Hackathon-2026/pull/40
---

# 222 – Ladící funkce pro demo panel

## Cíl
Vystavit ladící funkce pro demo panel: +1 000 kroků, +500 XP, přepnutí stavu postavy (případně reset dne). Tlačítka napojí Marek v 123.

## Soubory
- app/src/main/java/cz/narsil/movement/debug/
- app/src/main/java/cz/narsil/gamification/debug/

## Hotovo když
Funkce jsou vystavené a popsané v `docs/contracts.md` přes Poznámky.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 2, proud C. Potřebuje ho 123 (demo panel).
- `docs/contracts.md` mění jen Marek: popis funkcí (signatury) napsat sem do Poznámek, Marek je přenese.
- Překryv: `gamification/debug/` je uvnitř `gamification/` z 220 (stejná fáze) – 222 mění jen složky `debug/`.
- Signatury pro demo panel (123): `MovementDebug.addSteps(repo, steps = 1_000): Boolean`, `GamificationDebug.addXp(engine, xp = 500): Boolean`, `GamificationDebug.setAvatar(engine, avatar: AvatarState?): Boolean`, `GamificationDebug.nextAvatar(engine): AvatarState` (balíčky `cz.narsil.movement.debug`, `cz.narsil.gamification.debug`; fake i skutečné implementace). Reset dne: `MovementDebug.resetDay(repo, steps = 4_200)`, `GamificationDebug.resetDay(engine)` (značka DAY_RESET, bez mazání z DB); demo: `GamificationDebug.prepareDemo(engine)` (230).
