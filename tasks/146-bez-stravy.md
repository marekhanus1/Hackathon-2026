---
status: review
owner: marek
agent: claude
branch: marek/claude/146-bez-stravy
pr:
---

# 146 – Bez Stravy

## Cíl
Marek rozhodl: Strava se nepoužije (API vyžaduje předplatné). Odstranit ji z UI, pohybový úkol jen fotkou,
fotkou prostředí nebo popisem. Kód `integrations/strava` a rozhraní zůstávají.

## Soubory
- `core/ContractRepository.kt` (TaskType.MOVEMENT → PHOTO), `app/ServiceLocator.kt`, `app/nav/VerifyRoute.kt`
- `ui/screens/settings`, `ui/screens/contract` (Jan, Marek povolil) – texty a karta
- `docs/decisions.md`, test `PlanFakesTest`

## Poznámky
- Stávající úkoly v DB s method STRAVA se ověřují normálně fotkou/popisem (route už Stravu neposílá).
- Onboarding Stravu nezmiňoval.
