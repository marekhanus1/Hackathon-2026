---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/153-ozvena-jen-v-popredi
pr:
---

# 153 – Ozvěna nikdy nad jinou aplikací

## Cíl
Brána konce videa čekala, uživatel dal Home a Ozvěna · YouTube vyskočila nad launcherem (Nothing, 11:01–11:04).
Tik detekce nesmí opakovat zastaralý snímek sledované aplikace, když je v popředí jiná, a Ozvěna z detekce
se ukáže jen nad aplikací, ze které zásah přišel; jinak se odloží do návratu do feedu.

## Soubory
- app/src/main/java/cz/narsil/detection/accessibility/ (AccessibilityTriggerSource, NarsilAccessibilityService)
- app/src/main/java/cz/narsil/overlay/EchoOverlayController.kt, app/EchoOverlayStarter.kt, app/ServiceLocator.kt
- app/src/test/java/cz/narsil/
