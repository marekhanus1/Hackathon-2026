---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 231 – Stabilita dřepů v sále

## Cíl
Ověřit a doladit počítání dřepů v podmínkách sálu (světlo, vzdálenost, úhel kamery) na Nothing i Redmi, tolerantní prahy.

## Soubory
- app/src/main/java/cz/narsil/movement/squat/

## Hotovo když
3 různí lidé po 20 dřepů bez chyby.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 3, samostatná relace (navazuje na 210). Brána G4.
- Riziko z briefu: Pose Detection špatně počítá v sále – pointa dema.
