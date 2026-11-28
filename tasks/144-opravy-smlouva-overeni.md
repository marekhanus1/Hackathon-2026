---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/144-opravy-smlouva-overeni
pr:
---

# 144 – Opravy z testu na telefonu: ověření a smlouva

## Cíl
Chyby z testu buildu 3403d98: „Zkusit znovu“ po zamítnutí, sliby XP a „zítra“ ve smlouvě,
stav podepsané smlouvy na Domově, přední kamera pro video smlouvy.

## Soubory
- `app/nav/VerifyRoute.kt`, `app/nav/ContractRoute.kt`, `app/nav/ScreenRoutes.kt` (HomeRoute)
- `contract/ContractVideoRecorder.kt`
- `verify/TaskVerificationService.kt` – jen text zamítnutí
- `ui/screens/contract/`, `ui/screens/home/` (Jan, Marek povolil) – texty a stav smlouvy
- testy

## Poznámky
