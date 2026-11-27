---
status: in-progress
owner: simon
agent: claude
branch: simon/claude/510-llm-klient
pr: 
---

# 510 – Klient LLM pro Claude, Gemini a OpenAI

## Cíl
Klient přes OkHttp pro tři poskytovatele (`LlmProvider`), timeout 5 s, teplota 0, krátký výstup, vždy nejmenší rychlý model, výběr poskytovatele ze `SettingsRepository`. Text i obrázek. Hotovo, když stejný úkol a důkaz dá u všech tří stejnou odpověď.

## Soubory
`app/src/main/java/cz/narsil/llm/client/`

## Poznámky
- API klíče z `local.properties` přes BuildConfig.
