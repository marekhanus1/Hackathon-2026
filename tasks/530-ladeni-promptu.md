---
status: review
owner: simon
agent: claude
branch: simon/claude/530-ladeni-promptu
pr: https://github.com/marekhanus1/Hackathon-2026/pull/58
---

# 530 – Ladění promptu ověření

## Cíl
Vyladit prompt na 20 příkladech (10 ANO, 10 NE) u všech tří poskytovatelů. Hotovo, když je aspoň 18 z 20 správně.

## Soubory
`app/src/main/java/cz/narsil/verify/`, `docs/overeni-testy.md`

## Poznámky
-
- Sada a měření hotové, **neměřeno** – chybí klíče: `NARSIL_LIVE_EVAL=1 …_API_KEY=… ./gradlew testDebugUnitTest --tests '*PromptEvalTest*'`.
