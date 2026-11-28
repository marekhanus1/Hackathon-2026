---
status: review
owner: marek
agent: claude
branch: marek/claude/122-onboarding-logika
pr: https://github.com/marekhanus1/Hackathon-2026/pull/62
---

# 122 – Logika onboardingu oprávnění

## Cíl
Logika průvodce oprávněními: Accessibility (včetně omezených nastavení na Androidu 13+), překryvné okno, poloha kvůli Wi-Fi, kamera, Health Connect, notifikace, vypnutí optimalizace baterie; pro MIUI navíc automatické spouštění a vyskakovací okna na pozadí. Výběr domácí Wi-Fi. UI dodává Jan (320).

## Soubory
- app/src/main/java/cz/narsil/app/onboarding/

## Hotovo když
Na Redmi i Nothing jde vše zapnout podle průvodce.

## Poznámky
- Pro: Marek (Claude Pro č. 1).
- Fáze 2, proud C, brána G3. Napojuje obrazovku Jana 320.
- Překryv: 124 (orchestrátor) mění celé `app/` současně – 124 nesahá do `app/onboarding/` a `app/demo/`, dokud 122 a 123 nejsou sloučené.
