---
status: todo
owner:
agent:
branch:
pr:
---

# 158 – Okno po „Zavřít“ přežije restart

## Cíl
Relace a zaplacený čas přežijí restart procesu (#122), okno 10 min po „Zavřít“ (#120, `relapseUntil`) zatím ne:
když systém Narsil zabije těsně po „Zavřít“, návrat do feedu by dostal novou toleranci. Uložit i okno.

## Soubory
- app/src/main/java/cz/narsil/detection/accessibility/ScrollSessionTracker.kt, SessionPersistence.kt, AccessibilityTriggerSource.kt
- app/src/test/java/cz/narsil/app/SurvivalTest.kt
