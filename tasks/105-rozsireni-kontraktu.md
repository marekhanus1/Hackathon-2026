---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/105-rozsireni-kontraktu
pr: 
---

# 105 – Rozšíření kontraktů, sdílená databáze, nastavení

## Cíl
Po sloučení PR #8 zapracovat `docs/SPEC.md` sekci 11 do kódu a dokumentace: rozšířit `ContractRepository` (úkoly plánu), `MovementRepository.addEarnedMinutes`, `GamificationEngine.onTaskVerified`, přidat `RewardService`, `TaskVerifier`, `Proof`, `VerificationResult`, `LlmProvider`, `StravaActivity`, `StravaRepository`, `SettingsRepository` a sdílenou databázi `NarsilDatabase` se všemi entitami a DAO. Ke každému novému rozhraní fake. Doplnit `docs/contracts.md`. Hotovo, když projekt zkompiluje, fake vrací ukázková data a unit testy fake projdou.

## Soubory
`app/src/main/java/cz/narsil/core/` (včetně `core/data/` a `core/fake/`), `app/src/test/java/cz/narsil/core/`, `docs/contracts.md`, po sloučení PR #2 také `tasks/203-*`, `tasks/212-*`, `tasks/221-*`, `tasks/313-*`, `tasks/320-*`, `docs/BRIEF.md`

## Poznámky
- Závisí na PR #8 (core) a PR #2 (úkoly). Dělat až po jejich sloučení.
- Po sloučení PR #2 upravit úkoly podle aktuálního briefu v Claude Docs:
  - 203: měna včetně odměn za úkoly plánu a vzorce ze SPEC sekce 5.
  - 212: `ContractRepository` s úkoly plánu dne (SPEC 11.1).
  - 221: místo `ContentMatcher` teď `RewardService` (Šimon); `ContentMatcher` přechází na úkol 512 (Člen 5).
  - 313: obrazovka smlouvy s úkoly po typech, odměnou a způsobem ověření.
  - 320: onboarding navíc s připojením Stravy a nahráním prvního videa.
- Znovu exportovat brief z Claude Docs do `docs/BRIEF.md` (přibyla záložka Člena 5).
