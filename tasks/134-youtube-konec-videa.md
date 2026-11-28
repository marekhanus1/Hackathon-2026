---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/134-youtube-konec-videa
pr:
---

# 134 – YouTube: Ozvěna až na konci videa

## Cíl
Když limit vyprší během sledování videa (YouTube ve Firefoxu, i aplikace YouTube), Ozvěna počká
na konec videa nebo na přepnutí na jiné video (jiné `v=`, Shorts, návrat na feed). Pojistka:
strop čekání (docs/decisions.md). Testy nad umělými snímky.

## Soubory
- app/src/main/java/cz/narsil/detection/accessibility/
- app/src/main/java/cz/narsil/detection/youtube/ (nové)
- app/src/test/java/cz/narsil/detection/
- docs/decisions.md (jeden řádek)

## Poznámky
- Zadal koordinátor (session Marka), Marek rozhodl: Ozvěna nesmí vyskočit uprostřed videa.
