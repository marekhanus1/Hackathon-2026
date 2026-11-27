---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 321 – Animace odměny a hodnocení videa

## Cíl
Animace odměny (XP) a nové úrovně; hodnocení videa 1–5 jako spodní panel (po YouTube videu, +5 XP), včetně záměru před sledováním.

## Soubory
- app/src/main/java/cz/narsil/ui/screens/reward/
- app/src/main/java/cz/narsil/ui/screens/rating/

## Hotovo když
Animace běží plynule na Redmi.

## Poznámky
- Pro: Jan (Gemini Antigravity Pro).
- Fáze 2, proud B.
- Data jen přes ViewModel se `StateFlow<UiState>`, do napojení fake stavy. `@Preview` ve světlém i tmavém režimu. Texty česky, tón klidný a povzbuzující, nikdy trestající.
- Animaci nové úrovně potřebuje závěr dema (230). Hodnocení spouští detekce YouTube (Marek 120).
