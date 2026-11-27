---
status: review
owner: simon
agent: claude
branch: simon/claude/231-stabilita-drepu
pr: https://github.com/marekhanus1/Hackathon-2026/pull/43
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
- Hotovo v kódu: počítání čelem ke kameře (zkrácení stehna), bez kotníků, tolerantní prahy (`SquatThresholds`, přísné = `STRICT`), ladicí `SquatTuningOverlay`. Zbývá ruční zkouška: 3 lidé × 20 dřepů na Nothing i Redmi v sále, případně doladit `downDepth`/`upDepth`.
