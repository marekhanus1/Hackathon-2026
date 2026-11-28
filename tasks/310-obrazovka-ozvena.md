---
status: done        # todo | in-progress | review | done
owner: jan          # jméno člověka
agent: gemini       # claude | gemini | human
branch: jan/gemini/310-ozvena-overlay
pr: https://github.com/marekhanus1/Hackathon-2026/pull/17
---

# 310 – Obrazovka Ozvěny (překryvné okno)

## Cíl
Vzhled překryvného okna Ozvěny: plocha pro video, otázka „Čemu se teď vyhýbáš?“ s volbami (nuda, únava, úkol, samota), 4 karty voleb (Zavřít, Zaplatit kroky, 20 dřepů, Půjčit si). Zavřít je vizuálně nejatraktivnější. Video přehrává kód Marka (113) – obrazovka dodává jen vzhled a místo pro přehrávač (slot).

## Soubory
- app/src/main/java/cz/narsil/ui/screens/echo/

## Hotovo když
Hotovo do 6 h, Marek ho může vložit do okna.

## Poznámky
- Pro: Jan (Gemini Antigravity Pro).
- Fáze 1 (1,5–8 h), proud A, přednost (potřeba pro G2). Po 301, 302.
- Data jen přes ViewModel se `StateFlow<UiState>`, do napojení fake stavy. `@Preview` ve světlém i tmavém režimu. Texty česky, tón klidný a povzbuzující, nikdy trestající.
- Volby mapovat na `InterventionOutcome` z core (102); napojí Marek v 113/114/115.
