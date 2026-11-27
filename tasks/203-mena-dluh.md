---
status: in-progress
owner: simon
agent: claude
branch: simon/claude/203-mena-dluh
pr:
---

# 203 – Měna: kurz pohybu a dluh

## Cíl
Čistá logika měny podle `docs/SPEC.md` sekce 5: `zůstatek = floor(kroky_dnes / 100) + Σ odměny ověřených úkolů dnes − dnes zaplacené minuty − přenesený dluh`. Kurz z `MovementRates` (1 000 kroků = 10 min, 20 dřepů = 10 min hned, do zůstatku se nepřičítají), odměny za úkoly plánu (`MovementRepository.addEarnedMinutes`), placení (nikdy pod 0), půjčka (limit 30 min, nad limit nejde a Parťák dostane zprávu), přenos dluhu do dalšího dne. Den začíná ve 4:00 (`cz.narsil.core.data.DayKey`). Unit testy.

## Soubory
- app/src/main/java/cz/narsil/movement/currency/
- app/src/test/java/cz/narsil/movement/currency/

## Hotovo když
Testy pokrývají placení, půjčku, odměnu za úkol a přenos dluhu.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 0, proud B. Paralelně s 202 (bez překryvu).
- Mimo repozitář do G1, pak přesun do balíčku. Použije ho 211 (`MovementRepository`).
- Záznamy minut ukládá 211 do sdílené `LedgerEntryEntity` / `LedgerDao` (`cz.narsil.core.data`, druhy v `LedgerKind`).
