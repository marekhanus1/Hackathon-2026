---
status: review
owner: marek
agent: claude
branch: marek/claude/149-obecna-detekce-siti
pr:
---

# 149 – Obecná detekce dalších sociálních sítí

## Cíl
X, Reddit, TikTok, Facebook, Threads, Pinterest, 9GAG a podobné bez pravidel pro každou zvlášť:
seznam známých balíčků + obecný klasifikátor nekonečného feedu podle chování (opakované vertikální
scrolly v hlavním seznamu / pageru), zprávy a chaty nikdy (pole pro psaní zprávy). V Nastavení přepínač
pro každou nainstalovanou známou síť + „Další aplikace s nekonečným feedem“ (pole na konci `Settings`).

## Soubory
- app/src/main/java/cz/narsil/detection/accessibility/
- app/src/main/java/cz/narsil/core/SettingsRepository.kt (TrackedApp, pole na konci), core/data/DataStoreSettingsRepository.kt
- app/src/main/java/cz/narsil/ui/screens/settings/ (přepínače, koordinátor povolil)
- app/src/main/AndroidManifest.xml (<queries> pro zjištění nainstalovaných sítí)
- app/src/test/java/cz/narsil/

## Poznámky
- Zadal koordinátor po testu na Nothingu (Marek má X místo TikToku).
