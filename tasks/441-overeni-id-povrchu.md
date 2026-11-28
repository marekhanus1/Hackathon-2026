---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | codex | human
branch:
pr:
---

# 441 – Průzkum: ověření ID obrazovek Instagram, TikTok, YouTube

## Cíl
Ověřit, že fragmenty ID v `SurfaceRules` (app/src/main/java/cz/narsil/detection/accessibility/SurfaceClassifier.kt) sedí na aktuálních verzích aplikací na Nothingu. Pro každou obrazovku zapsat, která ID ve stromu jsou a jestli je pravidlo pozná. Kód se nemění, návrhy oprav jdou do dokumentu.

## Soubory
- docs/surface-ids.md (nový)

## Hotovo když
`docs/surface-ids.md` má tabulku: aplikace × obrazovka (feed, Reels/Shorts, zprávy, profil, hledání) → nalezená ID, očekávaný povrch (FEED, REELS, SHORTS, MESSAGES, VIDEO), jestli ho dnešní pravidlo pozná (ano/ne) a navrhovaný fragment, pokud ne. Plus verze aplikací.

## Postup
- Dump: `adb shell uiautomator dump /sdcard/d.xml && adb pull /sdcard/d.xml`, nebo log služby `adb logcat -s NarsilA11y NarsilA11yTree` (vypíše aplikaci, povrch a ID při změně okna).
- Porovnat s `SurfaceRules`: INSTAGRAM_*, TIKTOK, YOUTUBE.
- Stejná pravidla pro telefon jako v 440: sdílí se s Markem, neinstalovat, nedělat force-stop ani reconnect.

## Poznámky
- Pro: Štěpán (GPT Plus). Navazuje na 440, dá se dělat v jednom sezení.
- Chaty (Messenger, WhatsApp, …) se nesledují, ty vynech.
- Instagram už je ověřený (komentář v kódu, 11/2026), stačí rychlá kontrola. Hlavní jsou TikTok (obfuskovaná ID) a YouTube.
