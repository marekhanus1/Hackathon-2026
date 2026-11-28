---
status: review
owner: marek
agent: claude
branch: marek/claude/133-nastaveni-prehled
pr: https://github.com/marekhanus1/Hackathon-2026/pull/93
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
- `movement/currency/CurrencyBook.kt`, `movement/MovementModule.kt`, `movement/PartnerNotifier.kt`, `app/ServiceLocator.kt`
  (Šimonův balíček, Marek povolil: limit dluhu a zprávy Parťákovi z nastavení)

## Poznámky
Nové pole `Settings` (viz docs/contracts.md): toleranceMinutes, videoChainLimit, squatsPerUnlock,
pushupsPerUnlock, debtLimitMinutes, trackedApps, protectionPausedUntilMillis, partnerAlerts; přepínač
pro `showScrollProgress` (135). Zapojené: limit dluhu a Parťák (pohyb). **Čeká na zapojení** v detekci
(toleranceMillis, videoChainLimit, isTracked, isProtectionActive) a v Ozvěně (repsPerUnlock) – soubory
patří session „Spouštění Ozvěny“, požadavek předán koordinátorovi. Staví na větvi 132 (PR #90).
