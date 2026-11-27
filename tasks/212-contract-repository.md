---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 212 – ContractRepository a večerní video

## Cíl
`ContractRepository` v Room (dnešní smlouva: cíl kroků, plánovaný obsah, video), nahrávání 15s večerního videa přes CameraX (uložení jen lokálně) a `EchoVideoRepository` (výběr videa pro Ozvěnu – včerejší video).

## Soubory
- app/src/main/java/cz/narsil/contract/

## Hotovo když
Smlouva se uloží a druhý den se její video přehraje.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 1, proud C.
- UI smlouvy dělá Jan (313); 212 dodá logiku a ViewModel/stav, ne vzhled.
- Nejasnost: třída Room databáze – Marek v 121 (fáze 2) dělá `core/data/`. Pokud 212 potřebuje databázi dřív, založí jen své entity a DAO v `contract/` a domluví s Markem, kde bude `AppDatabase` (zapsat do docs/decisions.md).
- Video pro Ozvěnu přehrává overlay Marka (113).
