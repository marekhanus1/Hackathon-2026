---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 221 – ContentMatcher přes Claude API

## Cíl
`ContentMatcher.isPlanned(název, plán)`: jedno volání Claude API (OkHttp), odpověď JSON `{planned, reason}`, timeout 3 s, pak záloha přes klíčová slova. Do LLM jde jen název videa a plán.

## Soubory
- app/src/main/java/cz/narsil/llm/
- app/src/test/java/cz/narsil/llm/ (odhad – test zálohy přes klíčová slova)

## Hotovo když
„Přednáška o Rustu“ v plánu spáruje s podobným názvem videa; bez sítě funguje záloha.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 2, proud B. Používá ho Marek v 120 (YouTube).
- API klíč jen v `local.properties` (není v gitu). Načtení do BuildConfig vyžaduje změnu `app/build.gradle.kts` (Marek) – pokud tam ještě není, napsat do Poznámek a dát vědět Markovi. Oprávnění `INTERNET` v manifestu ověřit.
- Škrt č. 3 při skluzu: jen klíčová slova.
