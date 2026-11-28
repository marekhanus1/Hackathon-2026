---
status: done        # todo | in-progress | review | done
owner: jan          # jméno člověka
agent: gemini       # claude | gemini | human
branch: jan/gemini/322-zrcadlo-a-mapa-hodnoty
pr: https://github.com/marekhanus1/Hackathon-2026/pull/15
---

# 322 – Zrcadlo a mapa hodnoty

## Cíl
Obrazovky Zrcadla (statistiky po prvních dnech) a mapy hodnoty obsahu (kanály a formáty, které dokoukávám, ale hodnotím nízko) nad připravenými daty za 21 dní.

## Soubory
- app/src/main/java/cz/narsil/ui/screens/mirror/

## Hotovo když
Obrazovky čtou data od Štěpána (430).

## Poznámky
- Pro: Jan (Gemini Antigravity Pro).
- Fáze 2, proud C. Po 430 (formát v `mockdata/schema.md`, úkol 104).
- Data jen přes ViewModel se `StateFlow<UiState>`, do napojení fake stavy. `@Preview` ve světlém i tmavém režimu. Texty česky, tón klidný a povzbuzující, nikdy trestající.
- Nejasnost: jak se JSON z `mockdata/` dostane do aplikace – návrh v `mockdata/schema.md` (assets přes `app/build.gradle.kts`, rozhodne Marek). Do té doby fake stav sestavený podle příkladů ze schématu.
- Škrt č. 2 při skluzu: mapa hodnoty jako statický obrázek.
