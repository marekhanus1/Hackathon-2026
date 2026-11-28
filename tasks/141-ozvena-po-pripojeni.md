---
status: review
owner: marek
agent: claude
branch: marek/claude/141-ozvena-po-pripojeni
pr:
---

# 141 – Ozvěna přežije znovupřipojení služby přístupnosti

## Cíl
Okno Ozvěny (TYPE_ACCESSIBILITY_OVERLAY) zmizí se starou instancí služby a rozdělaná volba se ztratí.
Stav okna (zásah, pocit, obrazovka Ozvěna/cvik) držet mimo instanci služby (aplikační scope) a po
znovupřipojení okno obnovit. Pojistka: příliš starý rozdělaný zásah neobnovovat, detekce smí zasáhnout znovu.

## Soubory
- app/src/main/java/cz/narsil/overlay/ (EchoOverlayController, nový EchoSessionStore)
- app/src/main/java/cz/narsil/app/EchoOverlayStarter.kt
- app/src/test/java/cz/narsil/overlay/

## Poznámky
- Zadal koordinátor jako „138“; číslo 138 už měl jiný úkol, proto 141.
