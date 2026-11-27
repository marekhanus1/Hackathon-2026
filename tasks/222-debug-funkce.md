---
status: in-progress
owner: simon
agent: claude
branch: simon/claude/222-debug-funkce
pr:
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
