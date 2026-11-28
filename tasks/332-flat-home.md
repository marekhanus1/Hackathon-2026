---
status: todo
owner:
agent:
branch:
pr:
---

# 332 – Flat redesign hlavní obrazovky

## Cíl
Zjednodušit hlavní obrazovku do rychle pochopitelného flat designu: jedna primární akce,
stručný přehled dne, kompaktní pohyb a postup a dvě jasné sekundární navigace.

## Soubory
- app/src/main/java/cz/narsil/ui/screens/home/HomeScreen.kt

## Hotovo když
Uživatel během několika sekund pozná stav ochrany, další doporučený krok a dnešní pokrok.
Obrazovka nepoužívá vizuálně těžké vnořené karty ani zbytečné dekorace a funguje ve světlém
i tmavém režimu.

## Poznámky
- Vyžádal Štěpán po auditu současného UI; implementuje Codex.
- Neměnit `HomeUiState`, navigační kontrakty, sdílené komponenty ani globální téma.
- Ověřit sestavení a existující unit testy pro home.
