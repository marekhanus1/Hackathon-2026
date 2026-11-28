---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/159-ozvena-neukazana-pujcka
pr:
---

# 159 – Neukázaná Ozvěna a Půjčit nad limitem

## Cíl
1. Když se Ozvěna neukáže (chybí oprávnění k překrytí), detekce o tom ví a nezůstane ve stavu
   „čeká na volbu“: jinak v relaci už nezasáhne, dokud uživatel 2 min nepřestane scrollovat.
2. Tlačítko „Půjčit si 10 min“ je neaktivní, když by dluh přesáhl limit (SPEC 4: „nad limitem dluhu nejde“),
   s vysvětlením. Dnes je vždy aktivní, nad limitem okno jen zmizí a Ozvěna přijde znovu.

## Soubory
- app/src/main/java/cz/narsil/overlay/EchoOverlayController.kt, EchoStateBuilder.kt
- app/src/main/java/cz/narsil/ui/screens/echo/EchoUiState.kt, EchoScreen.kt (Jan: jen pole `canBorrow`
  ze SPEC 11.6 a `enabled` u tlačítka)
- app/src/test/java/cz/narsil/overlay/ (testy)

## Poznámky
