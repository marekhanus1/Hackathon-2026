---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 114 – Volby v okně a časovač odemknutí

## Cíl
Volby v okně Ozvěny (Zavřít, Zaplatit kroky, 20 dřepů, Půjčit si) převést na `InterventionOutcome` (CLOSED, PAID_STEPS, SQUATS, BORROWED, PLANNED_FREE) a předat dál (MovementRepository, GamificationEngine). Časovač odemknutí: v zaplaceném čase se nezasahuje (1 000 kroků = 10 min, 20 dřepů = 10 min).

## Soubory
- app/src/main/java/cz/narsil/overlay/

## Hotovo když
Každá volba okno zavře nebo odemkne na správnou dobu.

## Poznámky
- Pro: Marek (Claude Pro č. 1).
- Fáze 1, proud C, po 113 (stejná složka – sekvenčně). Brána G2.
- Kurz bere z MovementRepository (Šimon 203/211), do té doby fake.
