---
status: review
owner: marek
agent: claude
branch: marek/claude/140-zrcadlo-zebricek-db
pr: https://github.com/marekhanus1/Hackathon-2026/pull/102
---

# 140 – Zrcadlo a žebříček ze skutečné databáze

## Cíl
Janovo Zrcadlo a žebříček čtou skutečná data přes repozitáře v core místo fake dat.

## Soubory
- `ui/screens/mirror/`, `ui/screens/badges/` (Jan, Marek povolil), `app/nav/`, nové čtecí rozhraní `core/HistoryRepository.kt`, `core/data/RoomHistoryRepository.kt`, `core/fake/FakeHistoryRepository.kt`, `app/ServiceLocator.kt`, `docs/contracts.md`

## Poznámky
- Zrcadlo: vzato zpátky, pokles feedu (1. vs. poslední týden), dřepy denně, nejrizikovější hodina, přežitý boss,
  týdenní trend a mapa hodnoty (skupiny podle názvu videa / záměru, past = průměr ≤ 2) – vše z databáze.
- Odznaky: 5 odznaků gamifikace s postupem; party: kamarádi pevně (MVP bez serveru), „Ty“ = skutečné minuty za 7 dní.
- Minuty feedu z událostí spotřeby (121); dokud nejsou, záloha 10 min za odemknutí. Nic v `detection` jsem neměnil.
