---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 130 – Opravy chyb z testů na telefonech

## Cíl
Opravy chyb nalezených při testech na Nothing a Redmi (Štěpán je zakládá jako úkoly). Jeden subagent na chybu; každá chyba může dostat vlastní task 13x (odhad: 132, 133, …), aby se daly zamknout.

## Soubory
- Podle chyby (vždy jen v balíčcích Marka: `core`, `detection`, `overlay`, `app`; konkrétní cesty zapsat do tasku chyby).

## Hotovo když
Scénář dema projde 3× za sebou.

## Poznámky
- Pro: Marek (Claude Pro č. 1).
- Fáze 3 (14–18 h), proudy A–C, brána G4. Po G3.
- Proudy Šimona pomáhají (viz poznámka u 230/231).
- Před spuštěním paralelních oprav ověřit, že se jejich soubory nepřekrývají.
