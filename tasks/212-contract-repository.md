---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 212 – ContractRepository a večerní video

## Cíl
`ContractRepository` v Room podle `docs/SPEC.md` 11.1 (dnešní smlouva: cíl kroků, plánovaný obsah, video a úkoly plánu dne `PlanTask` včetně `updateTask`), nahrávání 15s večerního videa přes CameraX (uložení jen lokálně) a `EchoVideoRepository` (výběr videa pro Ozvěnu – včerejší video).

## Soubory
- app/src/main/java/cz/narsil/contract/

## Hotovo když
Smlouva i s úkoly se uloží (večer na zítřek, viz `DayKey.contractDateFor`), `updateTask` mění stav úkolu a druhý den se video smlouvy přehraje.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 1, proud C.
- UI smlouvy dělá Jan (313); 212 dodá logiku a ViewModel/stav, ne vzhled.
- Databáze je hotová (105): `NarsilDatabase.get(context).contractDao()`, entity `ContractEntity` + `PlanTaskEntity`, převody v `cz.narsil.core.data.Mappers.kt`. Vlastní entity nezakládat; chybí dotaz → Poznámky, přidá Marek.
- `EchoVideoRepository`: `ContractDao.latestWithVideoBefore(dnes)`, jinak video z onboardingu (SPEC 9).
- Video pro Ozvěnu přehrává overlay Marka (113).
