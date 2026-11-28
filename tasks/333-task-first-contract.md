---
status: in-progress
owner: stepan
agent: codex
branch: stepan/codex/333-task-first-contract
pr:
---

# 333 – Task-first obrazovka smlouvy

## Cíl
Udělat z denních úkolů hlavní obsah kroku smlouvy: přidané úkoly zobrazit nahoře a výrazně,
rychlé přidání hned pod nimi a návrhy ponechat až jako vedlejší pomoc.

## Soubory
- app/src/main/java/cz/narsil/ui/screens/contract/ContractScreen.kt

## Hotovo když
Uživatel na první pohled vidí, co chce splnit, kolik úkolů má a jakou odměnu každý přinese.
Návrhové chipy a technické vysvětlení nepřebíjejí samotný todo list.

## Poznámky
- Vyžádal Štěpán po testu aplikace na Markově telefonu.
- Neměnit datové modely, ukládání smlouvy ani navigační kontrakty.
- Ověřit světlý i tmavý preview a sestavení, pokud je dostupné Android SDK.
