---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 313 – Večerní smlouva

## Cíl
Obrazovka večerní smlouvy ve 3 krocích: pohybový cíl na zítra (kroky, minuty aktivity nebo dřepy), plánovaný obsah na zítra (text), 15s video „jak se dnes cítím“. Celé do 2 minut.

## Soubory
- app/src/main/java/cz/narsil/ui/screens/contract/

## Hotovo když
Průchod všemi kroky v náhledu.

## Poznámky
- Pro: Jan (Gemini Antigravity Pro).
- Fáze 1, proud D. Po 301, 302.
- Data jen přes ViewModel se `StateFlow<UiState>`, do napojení fake stavy. `@Preview` ve světlém i tmavém režimu. Texty česky, tón klidný a povzbuzující, nikdy trestající.
- Ukládání a nahrávání videa (CameraX) dělá Šimon v 212 – obrazovka má slot pro náhled kamery.
