---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/540-gemini-model
pr:
---

# 540 – Oprava modelu Gemini

## Cíl
Ověření úkolů přes Gemini hlásilo chybu: `gemini-2.5-flash-lite` vrací novým uživatelům 404
(„no longer available to new users“). Přejít na dostupný model a ověřit skutečným dotazem.

## Soubory
`app/src/main/java/cz/narsil/llm/client/` (Šimonův/Člen 5 balíček – se svolením Marka),
`app/src/test/java/cz/narsil/llm/client/LlmClientTest.kt`, `docs/SPEC.md` (jen název modelu),
`docs/overeni-testy.md`, `docs/decisions.md`

## Poznámky
- `LlmDefaults.GEMINI_MODEL` = `gemini-3.5-flash-lite` (ověřeno curlem s klíčem 28. 11. 2026,
  odpověď do ~1 s; `gemini-flash-lite-latest` funguje taky, ale nepinuje verzi).
- Thinking model: `generationConfig.thinkingConfig.thinkingLevel = "minimal"`; `thinkingBudget: 0`
  API odmítá (400). `MAX_TOKENS` 16 → 32 jako rezerva.
- Parser Gemini přeskakuje části `thought: true`; části s `thoughtSignature` nesou text.
- Claude `claude-haiku-5-5`: `thinking: disabled` + effort `low` je platné (disabled jde jen do
  effort `high`), `temperature` se neposílá – beze změny. OpenAI `gpt-4.1-nano` beze změny
  (bez klíče, neověřeno).
