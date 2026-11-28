---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/161-demo-most-prezentace
pr:
---

# 161 – Demo most: telefon ↔ prezentace

## Cíl
Živé demo propojené s prezentací (pitch/narsil-pitch.html, PR #112), aby se během pitche nemuselo nic klikat v demo panelu:
1. **Aplikace hlásí momenty dema** do logcatu (tag `NarsilDemo`, jeden JSON na řádek): kontext doma/venku,
   Ozvěna ukázaná/zavřená, dřepy (počet), odemčený čas, nová úroveň, XP, ohodnocené video.
2. **Aplikace přijímá příkazy dema** přes `adb shell am broadcast -a cz.narsil.DEMO` (chráněno oprávněním DUMP
   jako ostatní ladicí receivery): připravit demo, doma/venku/auto, Ozvěna hned, +XP, reset dne, stav.
3. **Most na notebooku** `scripts/demo-most.py` (jen stdlib Pythonu): servíruje pitch/ přes http://localhost,
   čte živě logcat a posílá události prezentaci (SSE), příkazy z prezentace posílá do telefonu přes adb,
   volitelně drží scrcpy ve smyčce (po výpadku USB ho nahodí).
4. **Prezentace** na slidu Demo: scény se posouvají samy podle událostí z telefonu (klik prezentérem dál funguje),
   velké počítadlo dřepů vedle živého telefonu, oslava nové úrovně, při vstupu na scénu „Venku“/„Doma“ pošle
   telefonu příkaz. Bez mostu se prezentace chová jako dřív.

## Soubory
- app/src/main/java/cz/narsil/app/demo/DemoBridge.kt, DemoCommandReceiver.kt (nové)
- app/src/main/java/cz/narsil/app/EchoOverlayStarter.kt (jen dotaz „ukazuje se Ozvěna“)
- app/src/main/java/cz/narsil/app/NarsilApp.kt nebo ServiceLocator.kt (spuštění hlášení)
- app/src/main/AndroidManifest.xml (receiver)
- app/src/test/java/cz/narsil/app/demo/ (testy formátu událostí)
- scripts/demo-most.py (nový)
- pitch/narsil-pitch.html, pitch/snimky/README.md, pitch/demo.md (sekce Propojení s prezentací)

## Poznámky
- Větev stojí na marek/claude/151-pitch-lotr (PR #112) – sloučit až po něm.
- Pitch je řada 4xx (Štěpán); úprava na žádost Marka.
