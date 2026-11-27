---
status: review      # todo | in-progress | review | done
owner: jan          # jméno člověka
agent: gemini       # claude | gemini | human
branch: jan/gemini/313-vecerni-smlouva
pr: https://github.com/marekhanus1/Hackathon-2026/pull/12
---

# 313 – Večerní smlouva

## Cíl
Obrazovka večerní smlouvy (`docs/SPEC.md` 6): krokový cíl na zítra (výchozí 6 000), plánovaný obsah (nejvýš 5 položek textem), úkoly plánu (nejvýš 6) a video „jak se dnes cítím“ (≤ 15 s). U úkolu: typ (`TaskType`: pohyb, škola, práce, jiné), název, cíl, odměna v minutách (5–60, výchozí podle typu) a způsob ověření (`VerificationMethod`: Strava, fotka, text; výchozí podle typu). Celé do 2 minut.

## Soubory
- app/src/main/java/cz/narsil/ui/screens/contract/

## Hotovo když
Průchod všemi kroky v náhledu včetně přidání a úpravy úkolu.

## Poznámky
- Pro: Jan (Gemini Antigravity Pro).
- Fáze 1, proud D. Po 301, 302.
- Data jen přes ViewModel se `StateFlow<UiState>`, do napojení fake stavy. `@Preview` ve světlém i tmavém režimu. Texty česky, tón klidný a povzbuzující, nikdy trestající.
- Ukládání a nahrávání videa (CameraX) dělá Šimon v 212 – obrazovka má slot pro náhled kamery.
- Model: `DailyContract` + `PlanTask` z `cz.narsil.core`, limity v `PlanRules`, demo data `FakeContractRepository.demoContract()`.
