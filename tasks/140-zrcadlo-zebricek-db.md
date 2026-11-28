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
- Zrcadlo zůstává na `MirrorStats` z #99 (121), podle koordinátora ho tenhle úkol nemění.
- Odznaky: 5 odznaků gamifikace s postupem z historie (`HistoryRepository`, stejný filtr spotřeby jako `MirrorStats`
  + zavření, půjčky, žolíci z XP); party: kamarádi pevně (MVP bez serveru), „Ty“ = skutečné minuty za 7 dní.
