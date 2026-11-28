---
status: todo
owner:
agent:
branch:
pr:
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
