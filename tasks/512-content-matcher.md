---
status: review
owner: simon
agent: claude
branch: simon/claude/512-content-matcher
pr: https://github.com/marekhanus1/Hackathon-2026/pull/57
---

# 512 – ContentMatcher přes LLM

## Cíl
Implementace `ContentMatcher` (dřív úkol 221): párování názvu videa s plánovaným obsahem přes klienta z 510, timeout 3 s, záloha přes klíčová slova. Do LLM jde jen název videa a plán. Hotovo, když „přednáška o Rustu“ v plánu spáruje podobný název videa a bez sítě funguje záloha.

## Soubory
`app/src/main/java/cz/narsil/llm/matcher/`

## Poznámky
-
