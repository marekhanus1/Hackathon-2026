---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/133-nastaveni-prehled
pr:
---

# 133 – Přehledné a podrobné nastavení

## Cíl
Všechny parametry chování Narsilu na jednom místě, v sekcích s krátkým vysvětlením, výchozí
hodnoty = nejlepší volba, „Obnovit výchozí“ po sekcích, citlivé změny (vypnutí ochrany,
zvýšení limitu) chráněné potvrzením se zpožděním. Demo panel (dlouhé podržení verze) zůstává.

## Soubory
- `app/src/main/java/cz/narsil/ui/screens/settings/` (Janův balíček, Marek povolil)
- `app/src/main/java/cz/narsil/core/SettingsRepository.kt`, `core/data/` (nastavení)
- `app/src/main/java/cz/narsil/app/nav/SettingsRoute.kt`
- `docs/contracts.md`, `docs/decisions.md`

## Poznámky
