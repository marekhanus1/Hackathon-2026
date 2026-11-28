---
status: review
owner: marek
agent: claude
branch: marek/claude/148-ukazatel-scrollu
pr: https://github.com/marekhanus1/Hackathon-2026/pull/108
---

# 148 – Viditelný ukazatel limitu scrollování

## Cíl
Ukazatel limitu (135) byl na Nothingu skoro neviditelný (3 dp přes stavový řádek). Teď 7 dp pruh pod stavovým
řádkem, výrazné barvy, zaoblené konce, tmavý obrys, puls při 50/80 %, štítek „Ozvěna za 0:15“ v posledních 20 %.
Pořád neblokující.

## Soubory
- `overlay/ScrollProgressOverlay.kt`, `overlay/ScrollProgressColors.kt`, nové `overlay/ScrollProgressStyle.kt`
- `app/EchoOverlayStarter.kt` – předání zbývajícího času
- testy `ScrollProgressStyleTest`, `ScrollProgressColorsTest`

## Poznámky
- Detekce beze změny; zbývající čas = práh (`Settings.toleranceMillis`) × zbývající procenta, mezi aktualizacemi odpočítává overlay.
