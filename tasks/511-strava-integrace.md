---
status: todo
owner: 
agent: 
branch: 
pr: 
---

# 511 – Strava OAuth a dnešní aktivity

## Cíl
`StravaRepository`: OAuth přes Custom Tab a deep link, uložení tokenu, stažení dnešních aktivit (od 4:00) a ověření úkolu `MOVEMENT` podle `docs/SPEC.md` sekce 7. Hotovo, když běh nahraný na Stravu se v aplikaci ukáže jako splněný úkol.

## Soubory
`app/src/main/java/cz/narsil/integrations/strava/`

## Poznámky
- Deep link v `AndroidManifest.xml` patří Markovi (`app/`), požádat ho v Poznámkách.
