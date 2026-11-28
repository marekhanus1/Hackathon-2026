---
status: review
owner: marek
agent: claude
branch: marek/claude/137-retezeni-videi
pr: https://github.com/marekhanus1/Hackathon-2026/pull/96
---

# 137 – Řetězení neplánovaných videí (videoChainLimit)

## Cíl
SPEC 3: zásah `UNPLANNED_VIDEO_CHAIN` u N. neplánovaného videa po sobě (N = `Settings.videoChainLimit`,
výchozí 3), pauza > 10 min řetěz nuluje, plánované video (`ContentMatcher`) se nepočítá.
Přechody videí z 134 (`VideoPlayback.videoKey`), aplikace YouTube i Firefox. Zásah jen na hranici videa
(na začátku N. videa), nikdy uprostřed.

## Soubory
- app/src/main/java/cz/narsil/detection/ (youtube/VideoChainTracker, AccessibilityTriggerSource, čtení názvu)
- app/src/main/java/cz/narsil/app/ServiceLocator.kt (předání ContentMatcher a plánu)
- app/src/test/java/cz/narsil/detection/

## Poznámky
- Zadal koordinátor po sloučení #95. Část úkolu 120 (název videa, řetězení).
