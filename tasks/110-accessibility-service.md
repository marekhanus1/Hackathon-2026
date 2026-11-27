---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/110-accessibility-service
pr:
---

# 110 – AccessibilityService: aplikace v popředí a URL

## Cíl
AccessibilityService, která sleduje `TYPE_WINDOW_STATE_CHANGED` a `TYPE_VIEW_SCROLLED`, zjistí aplikaci v popředí (Instagram, TikTok, YouTube, Chrome) a URL z adresního řádku Chrome. Pro ladění umí vypsat strom oken s ID prvků, ze kterých pak 111 pozná feed, Reels, Shorts a zprávy. Chatovací aplikace (Messenger, WhatsApp) se vůbec nesledují.

## Soubory
- app/src/main/java/cz/narsil/detection/accessibility/
- Navíc (odhad): registrace služby v `app/src/main/AndroidManifest.xml` a konfigurace `app/src/main/res/xml/accessibility_service_config.xml`.

## Hotovo když
V logu je vidět aktivní aplikace, URL a ID prvků obrazovky.

## Poznámky
- Pro: Marek (Claude Pro č. 1).
- Fáze 1 (1,5–8 h), proud A, brána G2. Po G1 (101–104).
- Android 13+ u APK zablokuje zapnutí Accessibility, dokud se v informacích o aplikaci nepovolí omezená nastavení – ověřit tady na Nothing.
- Marek ověřuje ID prvků na telefonu hned po dokončení.
- Překryv: stejná složka jako 111 (stejný proud A, 111 až po 110 – nepoběží paralelně). Manifest mění i 112 a 113 a `res/` patří Janovi: držet změny minimální (jen `<service>` a `xml/` konfigurace), při konfliktu řeší Marek.
