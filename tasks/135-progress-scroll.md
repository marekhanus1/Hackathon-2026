---
status: review
owner: marek
agent: claude
branch: marek/claude/135-progress-scroll
pr:
---

# 135 – Ukazatel blížícího se limitu při scrollování

## Cíl
Tenký nenápadný pruh přes overlay (neblokuje dotyky), který se při scrollování ve sledované
aplikaci plní podle toho, jak blízko je limit. Barva graduje ke konci, při 100 % Ozvěna.
Skrytý mimo sledovanou aplikaci, při pauze a mimo domov. Přepínač v `Settings` (výchozí zapnuto).

## Soubory
- app/src/main/java/cz/narsil/overlay/ (nový ukazatel)
- app/src/main/java/cz/narsil/detection/accessibility/AccessibilityTriggerSource.kt (stav progresu)
- app/src/main/java/cz/narsil/core/SettingsRepository.kt (jen nové pole na konci `Settings`)
- app/src/main/java/cz/narsil/app/EchoOverlayStarter.kt (napojení)
- app/src/test/java/cz/narsil/

## Poznámky
- Větev založená na 134 (stejný pipeline spouštění Ozvěny).
- Přepínač v UI nastavení udělá session „Nastavení a Strava“.
