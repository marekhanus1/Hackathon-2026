---
status: review
owner: marek
agent: claude
branch: marek/claude/147-mock-data-cisty-start
pr:
---

# 147 – Mock data vždy na čistou historii

## Cíl
„Nahrát mock data“ v demo panelu nejdřív smaže historii a pak nahraje, takže výsledek je vždy jako v testu
(480 / 500 XP, série 4, 1 žolík, postava FIT) a Domov se překreslí bez restartu.

## Soubory
- `app/demo/MockDataLoader.kt`, `app/demo/DemoActions.kt`, `app/demo/DemoPanelScreen.kt` (nápověda)
- `gamification/RulesGamificationEngine.kt` – nové `reload()` (Šimon, Marek povolil)
- test `MockDataReloadTest`

## Poznámky
- Maže xp_events, ledger, interventions, consumption_events, video_ratings, badges a smlouvy/úkoly před dneškem
  v jedné transakci Room (pozorovatelé se upozorní). Smlouva na dnešek a dál zůstane.
- Tlačítko jde zmáčknout opakovaně (příznak „jen jednou“ se už nekontroluje).
- `refresh()` enginu historii nenačítal znovu (mezipaměť) → `reload()`; pohyb `movementRepository.refresh()`.
