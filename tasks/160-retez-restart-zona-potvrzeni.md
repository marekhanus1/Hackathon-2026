---
status: review
owner: marek
agent: claude
branch: marek/claude/160-retez-restart-zona-potvrzeni
pr: https://github.com/marekhanus1/Hackathon-2026/pull/127
---

# 160 – Řetěz videí přes restart, zóna a domov s potvrzením

## Cíl
1. Řetěz neplánovaných videí (137) přežije restart procesu jako relace (#122): počet, začátek řetězu,
   poslední video, odemčený čas. Dnes po zabití procesu počítá od nuly a během zaplaceného času může zasáhnout.
2. Uvolnění komfortní zóny jde přes potvrzení s odpočtem (133) jako „Venku“: „Doma“ → „Automaticky“, když
   automatika doma nevyjde, a uložení jiné Wi-Fi jako domova místo už uložené.

## Soubory
- app/src/main/java/cz/narsil/detection/youtube/VideoChainTracker.kt, ChainPersistence.kt (nový)
- app/src/main/java/cz/narsil/detection/accessibility/AccessibilityTriggerSource.kt (jen napojení; 158 mění jiné řádky)
- app/src/main/java/cz/narsil/app/ServiceLocator.kt (napojení)
- app/src/main/java/cz/narsil/ui/screens/settings/SettingsViewModel.kt (133)
- app/src/test/java/cz/narsil/… (testy)

## Poznámky
- SettingsViewModel je v ui/ (Jan), ale logika potvrzení je 133 (Marek).
- Aplikace YouTube: video rozkoukané při restartu se započte znovu (bez ID videa).
