---
status: review
owner: marek
agent: claude
branch: marek/claude/145-overeni-vysledek-dluh
pr: https://github.com/marekhanus1/Hackathon-2026/pull/105
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
- Ověření: zobrazený úkol se drží (`pickVerifyTask`), karta „Ověřeno“ ukáže „+N min · +30 XP“ a tlačítko „Další úkol: …“.
- Důvod ověření po restartu: `VerificationReasonStore` (SharedPreferences `verify_reasons`), bez změny schématu.
- Dluh: `MovementState.carriedDebtMinutes` (nový nepovinný parametr, výchozí 0) = přenesený dluh nepokrytý kroky/odměnami.
