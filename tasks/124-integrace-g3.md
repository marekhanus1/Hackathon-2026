---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 124 – Skutečné moduly a obrazovky (G3)

## Cíl
V ServiceLocatoru vyměnit fake za skutečné moduly (pohyb, gamifikace, smlouva, LLM, Room) a napojit obrazovky od Jana (312, 313, 320–323) do navigace. Všechny funkce s MVP „Postavit“ běží spolu.

## Soubory
- app/src/main/java/cz/narsil/app/ (mimo `app/onboarding/` a `app/demo/`, dokud běží 122 a 123)

## Hotovo když
**G3:** všechny funkce Postavit běží spolu.

## Poznámky
- Pro: Marek (Claude Pro č. 1).
- Fáze 2, dělá orchestrátor sám. Brána G3 (feature freeze).
- Po 121, 122, 123, 202/220, 211, 212, 221 a obrazovkách Jana.
- Překryv: `app/` obsahuje složky 122 a 123 ze stejné fáze – viz výjimka v Soubory.
