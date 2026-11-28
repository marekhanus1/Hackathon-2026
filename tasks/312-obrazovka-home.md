---
status: done        # todo | in-progress | review | done
owner: jan          # jméno člověka
agent: gemini       # claude | gemini | human
branch: jan/gemini/312-hlavni-obrazovka
pr: https://github.com/marekhanus1/Hackathon-2026/pull/11
---

# 312 – Hlavní obrazovka

## Cíl
Hlavní obrazovka: postava budoucího já, XP a úroveň, souboj s algoritmem (denní skóre), série se žolíky, stav dnešní smlouvy, zůstatek kroků/minut a dluh.

## Soubory
- app/src/main/java/cz/narsil/ui/screens/home/

## Hotovo když
Náhled s fake daty vypadá hotově.

## Poznámky
- Pro: Jan (Gemini Antigravity Pro).
- Fáze 1, proud C. Po 301, 302.
- Data jen přes ViewModel se `StateFlow<UiState>`, do napojení fake stavy. `@Preview` ve světlém i tmavém režimu. Texty česky, tón klidný a povzbuzující, nikdy trestající.
- Napojí Marek v 124.
