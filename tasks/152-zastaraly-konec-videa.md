---
status: review
owner: marek
agent: claude
branch: marek/claude/152-zastaraly-konec-videa
pr: https://github.com/marekhanus1/Hackathon-2026/pull/113
---

# 152 – Zastaralý end-screen při otevření videa

## Cíl
Po otevření videa (intent, znovu stejné video) zůstane ve stromu end-screen předchozího přehrání
(`ended=true` na „Navrhované video“). Brána konce videa (134) ani řetězení (137) ho nesmí brát jako konec:
konec platí, jen když se na tomhle videu předtím přečetlo přehrávání (`ended=false`).

## Soubory
- app/src/main/java/cz/narsil/detection/youtube/, detection/accessibility/ScrollSessionTracker.kt
- app/src/test/java/cz/narsil/detection/

## Poznámky
- Zadal koordinátor po testu na Nothingu (Me at the zoo, 10:55:09).
