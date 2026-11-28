---
status: done
owner: marek
agent: claude
branch: marek/claude/155-zavrit-bez-farmeni
pr: https://github.com/marekhanus1/Hackathon-2026/pull/120
---

# 155 – „Zavřít“ bez farmení

## Cíl
„Zavřít“ v Ozvěně nesmí dávat scrollování zadarmo ani XP za opakované zavření a znovuotevření.
- Návrat do nekonečného feedu do 10 min po „Zavřít“ = Ozvěna hned, bez nové tolerance.
- XP za „Zavřít“ (50) se připíše až po 10 min bez dalšího zásahu (CLOSE_CONFIRMED); návrat = bez XP.
- Odznaky FIRST_CLOSE / CLOSE_10, souboj s algoritmem (15 min za zavření) a boss počítají jen potvrzená zavření.
- Obrazovka odměny po „Zavřít“ ukáže čekající XP s podmínkou.

## Soubory
- app/src/main/java/cz/narsil/detection/accessibility/ScrollSessionTracker.kt, AccessibilityTriggerSource.kt
- app/src/main/java/cz/narsil/gamification/GamificationRules.kt, GamificationCalculator.kt, RulesGamificationEngine.kt (Šimonův balíček – zmínit v PR)
- app/src/main/java/cz/narsil/overlay/EchoOutcomeHandler.kt, EchoOverlayController.kt, RewardBuilder.kt
- app/src/main/java/cz/narsil/core/Intervention.kt, GamificationEngine.kt (konstanta, dokumentace)
- testy k výše uvedenému, docs/SPEC.md (4, 8), docs/decisions.md

## Poznámky
Potvrzení se počítá z historie (žádný časovač): CLOSED bez dalšího zásahu do 10 min → při nejbližším
settle() (start aplikace, návrat do ní, další volba) záznam CLOSE_CONFIRMED +50 XP. Staré záznamy CLOSED s XP > 0
(mock data) platí jako potvrzené.
