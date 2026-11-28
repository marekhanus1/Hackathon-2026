---
status: review
owner: marek
agent: claude
branch: marek/claude/144-opravy-smlouva-overeni
pr: https://github.com/marekhanus1/Hackathon-2026/pull/104
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
- Retry: `verifyStatus()` ve VerifyRoute, `retrying` se resetuje s novým pokusem (`rememberSaveable(task.id, task.attempts)`).
- `onContractFulfilled` už volá `GamificationModule.watchContract` (ServiceLocator), jen texty neslibují XP při podpisu.
- Přední kamera: neověřeno na telefonu.
