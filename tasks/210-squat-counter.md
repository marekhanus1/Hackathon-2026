---
status: in-progress
owner: simon
agent: claude
branch: simon/claude/210-squat-counter
pr:
---

# 210 – SquatCounter: dřepy přes kameru

## Cíl
**Priorita – Marek ho napojuje v 7 h (115).** Implementace `SquatCounter`: CameraX a ML Kit Pose Detection, úhel kyčel–koleno–kotník, dole pod 100°, nahoře nad 160°, tlumení zákmitů. Vystavuje počet jako `Flow<Int>` a náhled kamery pro obrazovku Jana (311).

## Soubory
- app/src/main/java/cz/narsil/movement/squat/
- Navíc (odhad): oprávnění `CAMERA` v `app/src/main/AndroidManifest.xml` (změna minimální, konflikt řeší Marek).

## Hotovo když
Na Nothing počítá správně 9 z 10 pokusů, na Redmi stíhá v reálném čase.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 1 (1,5–8 h), proud A, samostatná relace řízená člověkem. Potřeba pro bránu G2.
- Šimon mezitím dělá dřepy před kamerou a ladí prahy. Pokračuje v 231.
- Náhled kamery: UI obrazovky dřepů je Janovo (311) – 210 dodá jen composable/View s preview, ne layout obrazovky.
