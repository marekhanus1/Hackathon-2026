---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/138-drobnosti-wifi-mock
pr:
---

# 138 – Drobnosti: tlačítko Wi-Fi v onboardingu, mock data bez souborů

## Cíl
Text tlačítka domácí Wi-Fi dává smysl ve všech stavech (bez Wi-Fi, uloženo, jiná síť). „Nahrát mock data“ v demo panelu funguje bez externích souborů: generátor dat v Kotlinu přibalený v APK (stejný příběh jako mockdata/generuj.py).

## Soubory
- `ui/screens/onboarding/` (Jan, Marek povolil), `app/onboarding/OnboardingRoute.kt`
- `app/demo/MockDataLoader.kt`, nový `app/demo/MockDataGenerator.kt`, testy

## Poznámky
