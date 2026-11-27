---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 311 – Obrazovka dřepů

## Cíl
Obrazovka dřepů: náhled kamery, velké počítadlo, zpětná vazba po každém dřepu (20 dřepů = 10 min). Náhled kamery dodá Šimon (210) – obrazovka má pro něj slot.

## Soubory
- app/src/main/java/cz/narsil/ui/screens/squat/

## Hotovo když
Hotovo do 6 h.

## Poznámky
- Pro: Jan (Gemini Antigravity Pro).
- Fáze 1, proud B, přednost (potřeba pro G2). Po 301, 302.
- Data jen přes ViewModel se `StateFlow<UiState>`, do napojení fake stavy. `@Preview` ve světlém i tmavém režimu. Texty česky, tón klidný a povzbuzující, nikdy trestající.
- Počet dřepů přijde jako `Flow<Int>` ze `SquatCounter` (fake z core.fake).
