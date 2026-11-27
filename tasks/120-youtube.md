---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 120 – YouTube: název videa a řetězení

## Cíl
Z YouTube (aplikace i web v Chrome) číst název videa přes Accessibility, sledovat řetězení neplánovaných videí a u každého volat `ContentMatcher.isPlanned(název, plán)`. Plánované video projde zdarma (PLANNED_FREE), řetězení neplánovaných spustí zásah. Jedno video z odkazu z chatu nic nespustí.

## Soubory
- app/src/main/java/cz/narsil/detection/youtube/

## Hotovo když
Plánované video projde, třetí neplánované spustí zásah.

## Poznámky
- Pro: Marek (Claude Pro č. 1).
- Fáze 2 (8–14 h), proud A, brána G3.
- Staví na 110/111 (název videa z Accessibility). `ContentMatcher` dělá Šimon (221) paralelně – do té doby fake.
- Hodnocení videa 1–5 po zhlédnutí ukazuje UI Jana (321).
