---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 323 – Odznaky, žebříček party a postava

## Cíl
Obrazovka odznaků a žebříčku party (připravená data), vložení ilustrací postavy v 5 stavech a ikon odznaků od Štěpána (420) do `res/drawable/`; postava mění vzhled podle stavu z gamifikace.

## Soubory
- app/src/main/java/cz/narsil/ui/screens/badges/
- app/src/main/res/drawable/

## Hotovo když
Postava mění vzhled podle stavu.

## Poznámky
- Pro: Jan (Gemini Antigravity Pro).
- Fáze 2, proud D. Po 420 (grafika) a 430 (žebříček v `mockdata/leaderboard.json`).
- Data jen přes ViewModel se `StateFlow<UiState>`, do napojení fake stavy. `@Preview` ve světlém i tmavém režimu. Texty česky, tón klidný a povzbuzující, nikdy trestající.
- Překryv: `res/drawable/` můžou chtít i jiné obrazovky – v této fázi do něj zapisuje jen 323 (ostatní obrazovky si případné drawable domluví přes Poznámky).
