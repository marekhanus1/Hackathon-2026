---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/145-overeni-vysledek-dluh
pr:
---

# 145 – Výsledek ověření, délka videa, dluh na Domově

## Cíl
Druhé kolo z testu na telefonu: po ověření ukázat výsledek a odměnu (ne hned další úkol), zachovat
důvod zamítnutí po restartu, správná délka uloženého videa ve smlouvě, dluh z `DEBT_CARRY` na Domově.

## Soubory
- `app/nav/VerifyRoute.kt`, `app/nav/ContractRoute.kt`, `app/nav/ScreenRoutes.kt`
- `verify/` (uložení důvodu), `movement/` (výpočet dluhu, pokud je chyba tam)
- `ui/screens/verify`, `ui/screens/contract` (Jan, Marek povolil)
- testy

## Poznámky
