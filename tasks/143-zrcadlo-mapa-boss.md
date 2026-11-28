---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/143-zrcadlo-mapa-boss
pr:
---

# 143 – Zrcadlo: mapa hodnoty a boss z databáze

## Cíl
Zrcadlo bere mapu hodnoty z `video_ratings` a bosse (přežité dny, kritická hodina) z databáze místo
ukázkových dat. Navazuje na `MirrorStats` (121) a `HistoryRepository` (140). S prázdnou Room zůstane záloha.

## Soubory
- `app/nav/MirrorStats.kt`, `app/nav/ScreenRoutes.kt` (MirrorRoute), testy
- `ui/screens/mirror/` (Jan, Marek povolil) – jen text položky mapy hodnoty

## Poznámky
