---
status: review
owner: marek
agent: claude
branch: marek/claude/132-strava-pad
pr: https://github.com/marekhanus1/Hackathon-2026/pull/90
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
Příčina (analýza, telefon nebyl k dispozici): v Narsilu žádná nezachycená výjimka – `startActivity`
byl v `runCatching`. Bez klíčů ale „Připojit“ otevřelo `strava.com/oauth/mobile/authorize?client_id=`
s prázdným `client_id` v prohlížeči / aplikaci Strava (`FLAG_ACTIVITY_NEW_TASK`), takže uživatel
z Narsilu vypadl na chybovou stránku nebo se aplikace Strava hned zavřela. Návrat `narsil://strava`
navíc spouštěl druhou `MainActivity` v úloze prohlížeče (výchozí launchMode), „Zpět“ z ní opouštělo aplikaci.

Oprava: bez klíčů se nic nespouští (hláška „Strava není nastavená…“ + „Vyzkoušet demo“), demo
vrací ukázkový běh (id −1) a v nastavení je označené „Demo“; chybějící prohlížeč = hláška;
návrat s chybou (zrušeno, bez `activity:read`, neúplný, výměna selhala) = česká hláška;
`MainActivity` je `singleTask`, takže návrat jde do běžící instance. Cizí balíčky
(`integrations/strava`, `ui/screens/settings`) se svolením Marka.
