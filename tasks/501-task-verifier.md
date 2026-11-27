---
status: in-progress
owner: simon
agent: claude
branch: simon/claude/501-task-verifier
pr: 
---

# 501 – TaskVerifier (čistý Kotlin)

## Cíl
`TaskVerifier` v čistém Kotlinu: sestavení promptu podle `docs/SPEC.md` sekce 7, parsování ANO/NE (bez diakritiky, velká písmena), zmenšení fotky na 512 px, SHA-256 otisk proti opakované fotce, unit testy s falešným HTTP. Hotovo, když testy pokrývají ANO, NE, nesmyslnou odpověď a timeout.

## Soubory
`app/src/main/java/cz/narsil/verify/`, `app/src/test/java/cz/narsil/verify/`

## Poznámky
- Fáze 0, do G1 mimo repozitář.
