---
status: in-progress
owner: stepan
agent: codex
branch: stepan/codex/332-flat-home
pr: https://github.com/marekhanus1/Hackathon-2026/pull/128
---

# 332 – Task-first Frutiger Aero hlavní obrazovka

## Cíl
Předělat hlavní obrazovku na task-first frontend ve stylu Frutiger Aero. Použít dodané
pozadí, zobrazit dnešní úkoly jako hlavní seznam a otevřít ověření kliknutím na konkrétní úkol.

## Soubory
- app/src/main/java/cz/narsil/ui/screens/home/HomeScreen.kt
- app/src/main/java/cz/narsil/ui/screens/home/HomeUiState.kt
- app/src/main/java/cz/narsil/app/nav/ScreenRoutes.kt
- app/src/main/java/cz/narsil/app/nav/NarsilRoute.kt
- app/src/main/java/cz/narsil/app/nav/NarsilNavHost.kt
- app/src/main/java/cz/narsil/app/nav/VerifyRoute.kt
- app/src/main/res/drawable-nodpi/frutiger_aero_background.png

## Hotovo když
Uživatel okamžitě vidí dnešní úkoly. Úkoly jsou v jednom seznamu pod sebou, celý řádek je
klikací a otevře ověření správného úkolu. Dodaný Frutiger Aero obrázek vyplňuje pozadí,
ale text zůstává čitelný díky jedné průsvitné ploše seznamu.

## Poznámky
- Vyžádal Štěpán po auditu současného UI; implementuje Codex.
- Rozsah byl po zpětné vazbě Štěpána rozšířen z flat návrhu na reálný task-first frontend.
- Neměnit sdílené komponenty ani globální téma.
- Ověřit sestavení a existující unit testy pro home.
- Lokální kompilace je zatím blokovaná chybějícím Android SDK (`sdk.dir` / `ANDROID_HOME`).
