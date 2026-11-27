---
status: review
owner: marek
agent: claude
branch: marek/claude/102-core-rozhrani
pr: https://github.com/marekhanus1/Hackathon-2026/pull/8
---

# 102 – Rozhraní a modely v core

## Cíl
Všechna sdílená rozhraní a modely z tabulky „Sdílená rozhraní“ v docs/BRIEF.md (ContextRepository, ConsumptionEvent, InterventionTrigger, InterventionOutcome, MovementRepository, SquatCounter, GamificationEngine, ContractRepository, ContentMatcher, EchoVideoRepository) existují v cz.narsil.core, každé má fake v cz.narsil.core.fake a popis v docs/contracts.md. Hotovo, když všechna rozhraní existují, mají fake a projekt se zkompiluje.

## Soubory
app/src/main/java/cz/narsil/core/ (včetně core/fake/), app/src/test/java/cz/narsil/core/, docs/contracts.md

## Poznámky
Větev vychází z marek/claude/101-kostra (kostra ještě není v main); po sloučení 101 rebase na origin/main.
