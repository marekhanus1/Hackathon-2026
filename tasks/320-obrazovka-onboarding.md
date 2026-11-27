---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 320 – Onboarding: oprávnění a domácí Wi-Fi

## Cíl
Obrazovky onboardingu: každé oprávnění vysvětlené jednou větou (Accessibility, překryvné okno, poloha kvůli Wi-Fi, kamera, Health Connect), nastavení domácí Wi-Fi („tohle je můj domov“), kroky pro MIUI (automatické spouštění, vyskakovací okna na pozadí).

## Soubory
- app/src/main/java/cz/narsil/ui/screens/onboarding/

## Hotovo když
Logiku napojí Marek (122).

## Poznámky
- Pro: Jan (Gemini Antigravity Pro).
- Fáze 2 (8–14 h), proud A.
- Data jen přes ViewModel se `StateFlow<UiState>`, do napojení fake stavy. `@Preview` ve světlém i tmavém režimu. Texty česky, tón klidný a povzbuzující, nikdy trestající.
