---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 203 – Měna: kurz pohybu a dluh

## Cíl
Čistá logika měny: kurz 1 000 kroků = 10 min, 20 dřepů = 10 min neplánovaného obsahu, placení, půjčka (dluh), přenos dluhu do zítřejšího cíle, limit dluhu (nad limit → Parťák). Unit testy.

## Soubory
- app/src/main/java/cz/narsil/movement/currency/
- app/src/test/java/cz/narsil/movement/currency/

## Hotovo když
Testy pokrývají placení, půjčku a přenos dluhu.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 0, proud B. Paralelně s 202 (bez překryvu).
- Mimo repozitář do G1, pak přesun do balíčku. Použije ho 211 (`MovementRepository`).
