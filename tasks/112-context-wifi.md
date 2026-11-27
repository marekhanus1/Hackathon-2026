---
status: review
owner: marek
agent: claude
branch: marek/claude/112-context-wifi
pr: https://github.com/marekhanus1/Hackathon-2026/pull/25
---

# 112 – ContextRepository přes domácí Wi-Fi

## Cíl
Skutečná implementace `ContextRepository`: stav HOME / AWAY / UNKNOWN jako `Flow` podle názvu domácí Wi-Fi (WifiManager; od Androidu 10 vyžaduje oprávnění k poloze). Uložení „tohle je můj domov“ a ruční přepínač „jsem doma“ jako záloha.

## Soubory
- app/src/main/java/cz/narsil/detection/context/
- Navíc (odhad): oprávnění (`ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`) v `app/src/main/AndroidManifest.xml`.

## Hotovo když
Stav se změní po připojení k domácí síti na obou telefonech.

## Poznámky
- Pro: Marek (Claude Pro č. 1).
- Fáze 1, proud B, brána G2. Po G1.
- Marek ověřuje Wi-Fi na Nothing i Redmi hned po dokončení.
- Manifest mění i 110 a 113 – změny minimální, konflikty řeší Marek.
