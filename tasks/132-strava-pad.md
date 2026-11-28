---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/132-strava-pad
pr:
---

# 132 – Strava: pád po prokliku, demo režim bez klíčů

## Cíl
„Připojit Stravu“ v nastavení nikdy neshodí ani neopustí aplikaci. Bez `STRAVA_CLIENT_ID/SECRET`
česká hláška „Strava není nastavená“ a demo režim (ukázková aktivita, jasně označená jako demo).
S klíči funkční OAuth a načtení aktivit. Ošetřený chybějící prohlížeč a návrat z OAuth s chybou.

## Soubory
- `app/src/main/java/cz/narsil/integrations/strava/` (cizí balíček, Marek povolil)
- `app/src/main/java/cz/narsil/ui/screens/settings/` (Janův balíček, Marek povolil – jen Strava karta)
- `app/src/main/java/cz/narsil/app/nav/SettingsRoute.kt`, `app/MainActivity.kt`, `AndroidManifest.xml`
- testy `app/src/test/java/cz/narsil/integrations/strava/`

## Poznámky
