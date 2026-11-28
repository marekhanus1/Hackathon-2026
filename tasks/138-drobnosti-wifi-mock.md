---
status: review
owner: marek
agent: claude
branch: marek/claude/138-drobnosti-wifi-mock
pr: https://github.com/marekhanus1/Hackathon-2026/pull/98
---

# 138 – Drobnosti: tlačítko Wi-Fi v onboardingu, mock data bez souborů

## Cíl
Text tlačítka domácí Wi-Fi dává smysl ve všech stavech (bez Wi-Fi, uloženo, jiná síť). „Nahrát mock data“ v demo panelu funguje bez externích souborů: generátor dat v Kotlinu přibalený v APK (stejný příběh jako mockdata/generuj.py).

## Soubory
- `ui/screens/onboarding/` (Jan, Marek povolil), `app/onboarding/OnboardingRoute.kt`
- `app/demo/MockDataLoader.kt`, nový `app/demo/MockDataGenerator.kt`, testy

## Poznámky
- Tlačítko Wi-Fi: bez Wi-Fi/polohy „Najít moji Wi-Fi (povolit polohu)“, bez domova „Tohle je můj domov: X“,
  na domácí síti „Domov uložen: X“ (neaktivní), jinde „Uložit jako domov: Y“. Dřív „Aktualizovat na žádná Wi-Fi…“.
- `MockDataGenerator` = port `mockdata/generuj.py`; test porovnává s `mirror.json` a `value_map.json`.
  Loader teď vkládá i XP za bosse, hodnocení videí, žolíka a strop 5× Zavřít, takže XP sedí na DemoTuning (480/500).
