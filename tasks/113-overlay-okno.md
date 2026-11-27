---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/113-overlay-okno
pr:
---

# 113 – Překryvné okno Ozvěny

## Cíl
Překryvné okno přes `WindowManager` (`SYSTEM_ALERT_WINDOW`), Compose obsah s vlastním lifecycle ownerem (Lifecycle, ViewModelStore, SavedStateRegistry) a přehrání videa Ozvěny přes ExoPlayer (Media3). Vzhled dodává Jan (310); tady jen okno, přehrávač a místo, kam se jeho obrazovka vloží.

## Soubory
- app/src/main/java/cz/narsil/overlay/
- Navíc (odhad): oprávnění `SYSTEM_ALERT_WINDOW` v `app/src/main/AndroidManifest.xml`.

## Hotovo když
Okno se ukáže přes TikTok a přehraje video.

## Poznámky
- Pro: Marek (Claude Pro č. 1).
- Fáze 1, proud C, brána G2. Po G1.
- Video bere přes `EchoVideoRepository` (Šimon 212; do té doby fake).
- Redmi/MIUI: bez oprávnění k vyskakovacím oknům na pozadí se okno neukáže.
- Překryv: stejná složka jako 114 (stejný proud C, sekvenčně).
