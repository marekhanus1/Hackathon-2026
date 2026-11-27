---
status: review
owner: simon
agent: claude
branch: simon/claude/211-movement-repository
pr: https://github.com/marekhanus1/Hackathon-2026/pull/36
---

# 211 – MovementRepository: kroky a zůstatek

## Cíl
Skutečný `MovementRepository`: kroky dnes z Health Connect, záloha `TYPE_STEP_COUNTER`, zůstatek minut, `pay()`, `borrow()`, dluh (logika z 203).

## Soubory
- app/src/main/java/cz/narsil/movement/steps/
- app/src/main/java/cz/narsil/movement/ (jen soubory přímo v balíčku, ne podsložky squat/ a currency/)
- Navíc (odhad): oprávnění Health Connect a `ACTIVITY_RECOGNITION` v `app/src/main/AndroidManifest.xml`.

## Hotovo když
Kroky z telefonu se promítnou do zůstatku minut.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 1, proud B. Po 203.
- Překryv: Soubory `movement/` obsahují i `movement/squat/` (210, stejná fáze, paralelně) – proto 211 smí měnit jen soubory přímo v `movement/` a `movement/steps/`.
- Redmi: Health Connect pravděpodobně doinstalovat z Google Play, jinak záloha senzor kroků. Škrt č. 4 při skluzu: jen senzor kroků.
- Manifest: `<queries>` Health Connect + 2× `activity-alias` (rationale oprávnění). Oprávnění žádá onboarding přes `HealthConnectStepsSource.permissionContract()`.
