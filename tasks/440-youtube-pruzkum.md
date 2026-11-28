---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | codex | human
branch:
pr:
---

# 440 – Průzkum: název videa a řetězení na YouTube

## Cíl
Podklady pro úkol 120: zjistit na Nothing Phone 3a, odkud Accessibility přečte **název videa** a jak se pozná **přechod na další video**. Zjišťuje se pro aplikaci YouTube (video, Shorts) i pro YouTube ve **Firefoxu** (m.youtube.com, viz docs/decisions.md). Výsledek je tabulka v `docs/youtube-ids.md`. Kód se nepíše.

## Soubory
- docs/youtube-ids.md (nový)

## Hotovo když
`docs/youtube-ids.md` obsahuje pro každý případ (aplikace: video, Shorts, autoplay dalšího videa; Firefox: /watch, /shorts, přechod na další video):
- `resource-id`, třídu a pole (`text` nebo `content-desc`), ve kterém je název videa,
- co se ve stromu změní při přechodu na další video (swipe v Shorts, autoplay, klik na doporučené),
- ukázku výřezu z dumpu (3–10 řádků XML), verzi aplikace YouTube a Firefoxu.

## Postup
1. Telefon přes USB, `adb devices` ho vidí. **Telefon se sdílí s Markem**, takže se předem domluv.
   Neinstaluj APK, nedělej `force-stop cz.narsil` ani `adb reconnect`/`kill-server`.
2. Otevři video a stáhni strom:
   `adb shell uiautomator dump /sdcard/d.xml && adb pull /sdcard/d.xml youtube-video.xml`
3. Totéž pro Shorts, po swipe na další Short a po autoplay. Ve Firefoxu nejdřív zavři nabídku „Přeložit tuto stránku?“ (Teď ne), jinak je v dumpu jen ta.
4. XML dej GPT a nech ho najít uzly s názvem videa. **Ověř sám**, že název v uzlu opravdu je.

## Poznámky
- Pro: Štěpán (GPT Plus). Výstup je dokument, nic nerozbije.
- Už víme (Firefox 160): URL je v `content-desc` prvku `ADDRESSBAR_URL_BOX`. Text uzlu `android.webkit.WebView` je název stránky („… - YouTube“). Ověř, jestli se při přechodu na další video mění.
- Surové XML dumpy necommituj (můžou obsahovat osobní údaje z účtu), do dokumentu dej jen výřezy.
