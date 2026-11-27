---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/111-klasifikace-povrchu
pr:
---

# 111 – Klasifikace povrchů a heuristika scrollování

## Cíl
Z událostí 110 rozlišit povrch: FEED, REELS, SHORTS, MESSAGES, VIDEO (`ConsumptionEvent`). Heuristika scrollování s tolerancí 5 minut (na demu 20 s); po jejím překročení vyšle `InterventionTrigger`. V nejistotě se povrch bere jako zprávy – raději zásah propásnout než otravovat. Odkaz z chatu = jedno video bez zásahu, zásah až při navazujících.

## Soubory
- app/src/main/java/cz/narsil/detection/accessibility/
- app/src/test/java/cz/narsil/detection/accessibility/ (unit testy heuristiky, odhad)

## Hotovo když
Zprávy v Instagramu nikdy nespustí zásah, feed ano.

## Poznámky
- Pro: Marek (Claude Pro č. 1).
- Fáze 1, proud A, po 110 (stejná složka – sekvenčně, ne paralelně). Brána G2.
- Používá `ConsumptionEvent` a `InterventionTrigger` z core (102).
- Práh (5 min / 20 s) musí jít přepnout z demo panelu (123).
