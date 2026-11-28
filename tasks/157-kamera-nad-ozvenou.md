---
status: todo
owner:
agent:
branch:
pr:
---

# 157 – Dialog kamery nad Ozvěnou

## Cíl
PR #122 ptá kameru až při prvních dřepech přes `CameraPermissionActivity`. Okno Ozvěny je
`TYPE_ACCESSIBILITY_OVERLAY` a leží nad všemi aplikacemi, systémový dialog by se otevřel pod ním.
Okno dřepů se na dobu dialogu schová a po odpovědi (nebo po 60 s) se vrátí.

## Soubory
- app/src/main/java/cz/narsil/app/CameraPermissionActivity.kt, EchoOverlayStarter.kt
- app/src/main/java/cz/narsil/overlay/EchoOverlayController.kt
