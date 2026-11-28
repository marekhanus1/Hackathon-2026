---
status: review
owner: marek
agent: claude
branch: marek/claude/156-co-ti-video-dalo
pr: https://github.com/marekhanus1/Hackathon-2026/pull/121
---

# 156 – „Co ti to video dalo?“ (zpětná vazba k videím)

## Cíl
Po videu jedním ťuknutím zjistit, jestli mělo hodnotu. Z odpovědí vzniká skutečná mapa hodnoty
(Zrcadlo) a argument pro Ozvěnu. Dnes je panel hodnocení (321) hotový, ale nikde se neukazuje, nic
nezapisuje `video_ratings` ani nevolá `onVideoRated` a mapa hodnoty běží jen z mock dat.

**Kdy se zeptat**
- Po konci videa na YouTube (aplikace i Firefox; konec pozná `VideoEndGate` / end-screen), případně při
  odchodu z videa po ≥ 2 min sledování.
- Po vypršení zaplacených 10 min feedu: „Co ti dalo těch 10 minut?“
- Nejvýš 10× denně (stejně jako strop XP), nikdy po každém Reelu, nikdy mimo `HOME`.

**Otázka a odpovědi** (jedno ťuknutí, bez dvou kroků)

| Odpověď | `rating` |
| --- | --- |
| Naučil jsem se něco | 5 |
| Inspirace, nápad | 4 |
| Odpočinul jsem si | 3 |
| Nic, jen čas | 2 |
| Je mi hůř | 1 |

Záměr před videem se vypouští (obtěžuje, odpovídá na něj člověk v pokušení); místo něj se ukládá,
jestli bylo video plánované (`ContentMatcher`).

**Forma:** malý spodní panel přes YouTube (overlay jako Ozvěna), sám se zavře po ~8 s, nikdy neblokuje,
jde ignorovat. Za odpověď +5 XP (`onVideoRated`, strop 10× denně už platí).

**Data:** beze změny schématu – `VideoRatingEntity`: `title` z detekce, odpověď do `intent`, číslo do `rating`.

**Využití**
- Zrcadlo: mapa hodnoty ze skutečných dat („Kanály, které dokoukáš, ale nic ti nedají“).
- Ozvěna: když stejný pořad (název bez pořadového čísla, jako `MirrorStats.valueEntries`) dostal 2× „Nic,
  jen čas“, Ozvěna to připomene: „Minule ti tohle nedalo nic.“

## Soubory
- app/src/main/java/cz/narsil/detection/youtube/ (spouštěč po konci videa) – Marek
- app/src/main/java/cz/narsil/overlay/ (panel přes YouTube, zápis hodnocení, věta v Ozvěně) – Marek
- app/src/main/java/cz/narsil/ui/screens/rating/ (pět odpovědí místo záměru + 1–5) – Jan (321)
- docs/SPEC.md (nová sekce o zpětné vazbě), docs/decisions.md

## Hotovo když
Na Nothingu: video na YouTube doběhne → panel → „Nic, jen čas“ → +5 XP → Zrcadlo ukáže past →
další Ozvěna nad stejným pořadem ukáže větu o minulém videu.

## Poznámky
- Demo: jedno video na YouTube, konec, „Nic, jen čas“, +5 XP; pak Zrcadlo a Ozvěna s větou.
- Navazuje na 120 (detekce YouTube), 143 (Zrcadlo, mapa hodnoty), 321 (panel hodnocení).
