---
status: review
owner: marek
agent: claude
branch: marek/claude/103-servicelocator-navigace
pr: https://github.com/marekhanus1/Hackathon-2026/pull/4
---

# 103 – ServiceLocator a navigace

## Cíl
ServiceLocator s přepínačem fake/skutečné moduly a navigace (navigation-compose) mezi prázdnými obrazovkami: Domů, Večerní smlouva, Dřepy, Ozvěna (náhled), Zrcadlo, Odznaky, Onboarding, Demo panel. Hotovo, když aplikace naviguje mezi prázdnými obrazovkami.

## Soubory
app/src/main/java/cz/narsil/app/ (mimo onboarding/ a demo/ – ty mají 122 a 123 až později, tady jen zástupné obrazovky v app/nav/), app/src/main/AndroidManifest.xml

## Poznámky
Větev vychází z marek/claude/101-kostra (kostra ještě není v main); po sloučení 101 rebase na origin/main.
