---
status: review
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
- `MirrorStats.applyHistory`: přežitý boss = dny s XP BOSS, okno pevně 21:00–22:00 (SPEC 8); mapa hodnoty z `video_ratings`
  (skupiny podle názvu bez čísla / záměru, past = průměr ≤ 2). Prázdná historie = připravená čísla, data bez hodnocení = prázdná mapa s vysvětlením.
- `ContentValueEntry.watchedMinutes` → `videoCount` (minuty sledování se neukládají).
