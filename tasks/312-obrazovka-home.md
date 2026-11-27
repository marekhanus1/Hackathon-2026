---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
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
