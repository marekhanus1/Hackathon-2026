---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/136-napojeni-nastaveni
pr:
---

# 136 – Napojení nastavení (133) do detekce a Ozvěny

## Cíl
Detekce čte `Settings.toleranceMillis()` (demo práh dál přednost), `isTracked(WatchedApp.name)`,
`isProtectionActive(now)` (při pauze ochrany žádný zásah ani ukazatel) a `videoChainLimit`.
Ozvěna čte `repsPerUnlock()` místo `MovementRates.SQUATS/PUSHUPS_PER_UNLOCK`.

## Soubory
- app/src/main/java/cz/narsil/detection/
- app/src/main/java/cz/narsil/overlay/
- app/src/main/java/cz/narsil/app/EchoOverlayStarter.kt
- app/src/test/java/cz/narsil/detection/, app/src/test/java/cz/narsil/overlay/

## Poznámky
- Zadal koordinátor po sloučení #93 (133).
