---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/148-hledani-nepocitat
pr:
---

# 148 – Výsledky vyhledávání se nepočítají

## Cíl
Stránka vyhledávání (YouTube aplikace `results?search_query=` i web, Instagram/X/Reddit/TikTok/Facebook search)
není nekonečný povrch ani video v řetězu a nespouští relaci. Počítá se až feed, Shorts/Reels nebo přehrávání.
Nový povrch `Surface.SEARCH`.

## Soubory
- app/src/main/java/cz/narsil/core/Consumption.kt (Surface.SEARCH), docs/contracts.md
- app/src/main/java/cz/narsil/detection/accessibility/
- app/src/test/java/cz/narsil/detection/

## Poznámky
- Zadal koordinátor po testu na Nothingu (Ozvěna po stránce hledání YouTube + jednom Shorts).
