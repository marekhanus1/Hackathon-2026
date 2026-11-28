---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/541-zpusoby-overeni
pr:
---

# 541 – Nové způsoby ověření splnění úkolu

## Cíl
Uživatel si při ověření vybere způsob: fotka úkolu, fotka prostředí (uklizený pokoj, rozložená
podložka) nebo vlastní popis (2–3 věty; podpora psaní a kritického myšlení). LLM vrací strukturovanou
odpověď `ANO/NE – věta` se zpětnou vazbou česky; výpadek sítě / chyba modelu se zobrazí srozumitelně.

## Soubory
- `core/Verification.kt` – `PhotoKind`, `Proof.Photo(uri, kind = TASK)` (zpětně kompatibilní), `docs/contracts.md`
- `verify/` (Šimon / Člen 5 – se svolením Marka): `VerifyRules`, `LlmTaskVerifier`,
  `TaskVerificationService` (`ProofCodec`, `VerificationUiEvent.feedback`), `demo/`, `PromptEvaluation`
- `ui/screens/verify/` (Jan – se svolením Marka): třetí režim, nápověda, počítadlo minimální délky
- `app/nav/VerifyRoute.kt`
- testy `verify/VerifyModesTest.kt`, `verify/LiveModesTest.kt` (živý, mimo CI)
- `docs/SPEC.md` (7), `docs/decisions.md`, `docs/overeni-testy.md`

## Poznámky
- Navazuje na 540 (model Gemini). Bez 540 vrací Gemini 404 → ověření dá „ověří se později“.
- Živě (Gemini 3.5 Flash Lite, 28. 11. 2026): 3 režimy 7/7, textová sada 530 viz `docs/overeni-testy.md`.
