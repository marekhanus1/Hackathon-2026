---
status: todo        # todo | in-progress | review | done
owner:
agent:
branch:
pr:
---

# 163 – Parťák místo půjčky do zítřka

## Cíl
Volba „Půjčit si 10 min“ v Ozvěně se mění na „Poprosit Parťáka o 10 min“. Minuty se už
nepůjčují z dalšího dne (žádný přenos dluhu), místo toho Narsil „napíše kamarádovi“ a ten
(simulovaně, bez serveru) za chvíli odpoví a 10 min přidá. Denní limit z Nastavení
(dřív limit dluhu) říká, kolik minut ti Parťák za den dá.

## Soubory
- movement/currency (bez přenosu dluhu), movement/PartnerNotifier, MovementModule
- overlay/EchoOverlayController, EchoOutcomeHandler
- ui/screens/echo, home, settings, badges, components/ChoiceCard (jen texty)
- docs/SPEC.md (4, 5), docs/decisions.md, testy

## Poznámky
Zadal Marek 28. 11. 2026. Interně zůstává `InterventionOutcome.BORROWED` a ledger BORROW
(bez migrace DB), mění se jen význam a texty.
