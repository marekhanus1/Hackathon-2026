---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/121-room-udalosti
pr:
---

# 121 – Room: události spotřeby a denní souhrny

## Cíl
Room tabulky pro události spotřeby (`ConsumptionEvent`, výsledky intervencí) a denní souhrny pro souboj s algoritmem: minuty feedu a minuty získané zpátky. Data pro hlavní obrazovku a gamifikaci.

## Soubory
- app/src/main/java/cz/narsil/core/data/
- app/src/test/java/cz/narsil/core/data/ (odhad)

## Hotovo když
Denní minuty feedu a získané minuty jdou přečíst.

## Poznámky
- Pro: Marek (Claude Pro č. 1).
- Fáze 2, proud B, brána G3.
- Nejasnost: Šimon v 212 dělá `ContractRepository` v Room. Domluvit, kdo vlastní třídu databáze (`AppDatabase`) – návrh: databáze v `core/data/` (tento úkol), 212 přidá jen své entity/DAO v `contract/`; pokud 212 skončí dřív a databázi založí sám, 121 ji přesune sem. Zapsat do docs/decisions.md.
