---
status: review
owner: marek
agent: claude
branch: marek/claude/139-smlouva-video-ukoly
pr: https://github.com/marekhanus1/Hackathon-2026/pull/100
---

# 139 – Smlouva a onboarding: vlastní video a úkoly

## Cíl
Nahrání videa ve večerní smlouvě a v onboardingu, přidávání úkolů do smlouvy; Ozvěna přehraje vlastní video (SPEC 9).

## Soubory
- `contract/` (Šimon), `ui/screens/contract/`, `ui/screens/onboarding/` (Jan), `app/nav/` – Marek povolil

## Poznámky
- Smlouva má 4 kroky: cíl, plánovaný obsah, **úkoly** (typ Pohyb/Škola/Práce/Jiné, odměna podle SPEC 6, max 6), video.
- Video přes `ContractVideoRecorder` (přední kamera, ≤ 15 s, jen v telefonu), uložení přes `ContractSigner`
  (17:00–3:59 na zítřek, demo „platí hned“ na dnešek). Existující smlouva se předvyplní, video a ověřené úkoly zůstanou.
- Onboarding: nepovinná karta „Video pro Ozvěnu“ → `PrefsOnboardingVideoStore` (záloha Ozvěny, SPEC 9).
- `ContractRoute` přesunutá do `app/nav/ContractRoute.kt`, náhled kamery `app/nav/RecorderPreview.kt`.
- Staví na 138 (PR #98) kvůli společnému `OnboardingScreen`.
